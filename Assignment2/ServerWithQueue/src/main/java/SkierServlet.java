



import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

@WebServlet(name = "SkierServlet", value = "/skiers/*")
public class SkierServlet extends HttpServlet {
    private static final Integer LOWER_BOUND = 1;
    private static final Integer MAX_SKIER_ID = 100000;
    private static final Integer MAX_RESORT_ID = 10;
    private static final Integer MAX_LIFT_ID = 40;
    private static final Integer SEASON_ID = 2024;
    private static final Integer DAY_ID = 1;
    private static final int MAX_TIME = 360;
    static final int POOL_SIZE = 500;


    private static final ResponseMsg MISSING_PARAMS = new ResponseMsg("{\"error\": \"Missing parameters\"}");
    private static final ResponseMsg INVALID_PAYLOAD = new ResponseMsg("{\"error\": \"Invalid payload\"}");
    private static final ResponseMsg SUCCESS_MESSAGE = new ResponseMsg("{\"LiftRide successfully posted\"}");
    private static final ResponseMsg INVALID_PATH = new ResponseMsg("{\"error\": \"Invalid URL path\"}");


    static ChannelPool channelPool;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public void init() throws ServletException {
        super.init();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(QueueConfig.RABBITMQ_HOST);
        factory.setPort(QueueConfig.RABBITMQ_PORT);
        factory.setUsername(QueueConfig.RABBITMQ_USERNAME);
        factory.setPassword(QueueConfig.RABBITMQ_PASSWORD);

        try {
            System.out.println("ChannelPool is creating....");
            SkierServlet.channelPool = new ChannelPool(SkierServlet.POOL_SIZE, factory);
            System.out.println("A ChannelPool is successfully created!");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void destroy() {
        super.destroy(); // Always call super.destroy()
        try {
            if (channelPool != null) {
                channelPool.close(); // This method should close all channel connections properly
                System.out.println("ChannelPool and all connections are successfully closed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("An error occurred while closing ChannelPool: " + e.getMessage());
        }
    }


    private void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, InterruptedException {
        response.setContentType("application/json");
        String urlPath = request.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            sendResponseMsg(response, HttpServletResponse.SC_NOT_FOUND, MISSING_PARAMS);
            return;
        }

        String[] urlParts = urlPath.split("/");
        // validate url path and return the response status code
        if (!isValidURL(urlParts)) {
            sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, INVALID_PATH);
            return;
        }


        // process request body
        StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        } catch (IOException e) {
            // Handle IOException
            e.printStackTrace();
            sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, INVALID_PAYLOAD);
            return;
        }
        String payload = requestBody.toString();

        LiftRide liftRide;
        try {
            liftRide = objectMapper.readValue(payload, LiftRide.class);
            // validate payload
            if (liftRide.getLiftID() < LOWER_BOUND || liftRide.getLiftID() > MAX_LIFT_ID ||
                liftRide.getTime() < LOWER_BOUND || liftRide.getTime() > MAX_TIME) {
                sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, INVALID_PAYLOAD);
                return ;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, INVALID_PAYLOAD);
            return;
        }

        String skierID = urlParts[7];
        String formattedMessage = skierID + "," + liftRide.getLiftID() + "," + liftRide.getTime();  // "skierID, liftID, Time"
        // send to the RabbitMQ
        // declare the queue
        Channel channel = channelPool.borrowChannel();
        channel.queueDeclare(QueueConfig.QUEUE_NAME,false, false, false, null);
        channel.basicPublish("", QueueConfig.QUEUE_NAME, null,formattedMessage.getBytes());
        System.out.println("Message published to RabbitMQ: " + formattedMessage);
        sendResponseMsg(response, HttpServletResponse.SC_CREATED, SUCCESS_MESSAGE);
        if (channel != null) {
            channelPool.returnChannel(channel);
        }
    }


    private void sendResponseMsg(HttpServletResponse response, int statusCode, ResponseMsg msg)
        throws IOException {
        response.setStatus(statusCode);
        response.getWriter().write(msg.getMessage());
    }

    private boolean isValidURL(String[] urlPaths) {
        // urlPath  = "/1/seasons/2019/days/1/skiers/123"
        // urlParts = [, 1, seasons, 2019, days, 1, skiers, 123]
        if (urlPaths.length != 8
            || !urlPaths[2].equals("seasons")
            || !urlPaths[4].equals("days")
            || !urlPaths[6].equals("skiers")) {
            return false;
        }

        // Validate range
        try {
            int resortID = Integer.parseInt(urlPaths[1]); // resortID
            int seasonID = Integer.parseInt(urlPaths[3]); // seasonID
            int dayID = Integer.parseInt(urlPaths[5]); // dayID
            int skierID = Integer.parseInt(urlPaths[7]); // skierID

            if (seasonID != SEASON_ID ||
                resortID < LOWER_BOUND || resortID > MAX_RESORT_ID ||
                skierID < LOWER_BOUND || skierID > MAX_SKIER_ID ||
                dayID != DAY_ID) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
    

}
