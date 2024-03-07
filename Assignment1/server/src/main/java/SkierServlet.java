



import com.fasterxml.jackson.databind.ObjectMapper;
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


    private static final ResponseMsg MISSING_PARAMS = new ResponseMsg("{\"error\": \"Missing parameters\"}");
    private static final ResponseMsg INVALID_PAYLOAD = new ResponseMsg("{\"error\": \"Invalid payload\"}");
    private static final ResponseMsg SUCCESS_MESSAGE = new ResponseMsg("{\"LiftRide successfully posted\"}");
    private static final ResponseMsg INVALID_PATH = new ResponseMsg("{\"error\": \"Invalid URL path\"}");

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        sendResponseMsg(response, HttpServletResponse.SC_CREATED, SUCCESS_MESSAGE);
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
