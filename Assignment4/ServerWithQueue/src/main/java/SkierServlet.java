
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;


@WebServlet(name = "SkierServlet", urlPatterns = {"/skiers/*", "/resorts/*"})
public class SkierServlet extends HttpServlet {
    private static final Integer LOWER_BOUND = 1;
    private static final Integer MAX_SKIER_ID = 100000;
    private static final Integer MAX_RESORT_ID = 10;
    private static final Integer MAX_LIFT_ID = 40;
    private static final Integer SEASON_ID = 2024;
    private static final Integer DAY_ID = 1;
    private static final int MAX_TIME = 360;
    static final int POOL_SIZE = 300;
    static final int RESORT_ID = 1;
    static final String TABLE_NAME = "SkiersData";



    private static final ResponseMsg MISSING_PARAMS = new ResponseMsg("{\"error\": \"Missing parameters\"}");
    private static final ResponseMsg INVALID_PAYLOAD = new ResponseMsg("{\"error\": \"Invalid payload\"}");
    private static final ResponseMsg SUCCESS_MESSAGE = new ResponseMsg("{\"LiftRide successfully posted\"}");
    private static final ResponseMsg INVALID_PATH = new ResponseMsg("{\"error\": \"Invalid URL path\"}");


    static ChannelPool channelPool;
    private final ObjectMapper objectMapper = new ObjectMapper();
    protected static  DynamoDbClient dynamoDbClient;



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

        dynamoDbClient = DynamoDbClient.builder()
            .region(Region.US_WEST_2)
            .build();
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("doGet is called ....");
        String urlPath = request.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            sendResponseMsg(response, HttpServletResponse.SC_NOT_FOUND, new ResponseMsg("Data not found"));
            return;
        }

        String[] urlParts = urlPath.split("/");

        System.out.println(urlParts.length);
        try {
            if (urlParts.length == 7) {
                processResortSeasonDaySkiers(request, response, urlParts);
            } else if (urlParts.length == 8) {
                processSkierDayActivities(request, response, urlParts);
            } else if (urlPath.matches("/\\d+/vertical")) {
                processSkierVertical(request, response, urlParts);
            } else {
                sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, new ResponseMsg("Invalid inputs supplied"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponseMsg(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                new ResponseMsg("{\"error\": \"Error processing request\"}"));
        }
    }

    // get number of unique skiers at resort/season/day
    private void processResortSeasonDaySkiers(HttpServletRequest request,
        HttpServletResponse response, String[] urlParts) throws IOException {
        // URL pattern: /resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
        System.out.println("url size  " + urlParts.length);

        int resortId = Integer.parseInt(urlParts[1]);
        int seasonId = Integer.parseInt(urlParts[3]);
        int dayId = Integer.parseInt(urlParts[5]);

        if ( !urlParts[2].equals("seasons") || !urlParts[4].equals("day") || !urlParts[6].equals("skiers")) {
            sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, INVALID_PATH);
        } else if (resortId != RESORT_ID || seasonId != SEASON_ID || (dayId != 1 && dayId != 2 && dayId != 3)) {
            sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, new ResponseMsg("Invalid Resort ID supplied"));
        } else {
            String resortSeasonDayId = resortId + "#" + seasonId + "#" + dayId;
            try {
                QueryRequest queryRequest = QueryRequest.builder()
                    .tableName("TABLE_NAME")
                    .keyConditionExpression("ResortSeasonDayId = :rsdId")
                    .expressionAttributeValues(Map.of(":rsdId", AttributeValue.builder().s(resortSeasonDayId).build()))
                    .select(Select.SPECIFIC_ATTRIBUTES)
                    .projectionExpression("SkierId")
                    .build();

                QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
                Set<String> uniqueSkiers = queryResponse.items().stream()
                    .map(item -> item.get("SkierId").s())
                    .collect(Collectors.toSet());

                int uniqueSkierCount = uniqueSkiers.size();
                sendResponseMsg(response, HttpServletResponse.SC_OK, new ResponseMsg("{\"uniqueSkierCount\": " + uniqueSkierCount + "}"));
            } catch (DynamoDbException e) {
                e.printStackTrace();
                sendResponseMsg(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, new ResponseMsg("{\"error\": \"Error querying database\"}"));
            }
        }
    }


    // get the total vertical for the skier for the specified ski day
    private void processSkierDayActivities(HttpServletRequest request, HttpServletResponse response,
        String[] urlParts) throws IOException {
        // Assuming URL pattern: /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
        int resortId = Integer.parseInt(urlParts[1]);
        int seasonId = Integer.parseInt(urlParts[3]);
        int dayId = Integer.parseInt(urlParts[5]);
        int skierId = Integer.parseInt(urlParts[7]);

        if (!urlParts[2].equals("seasons") || !urlParts[4].equals("days") || !urlParts[6].equals("skiers")) {
            sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, INVALID_PATH);
        } else if (resortId != RESORT_ID || seasonId != SEASON_ID || (dayId != 1 && dayId != 2 && dayId != 3) ||
            skierId < LOWER_BOUND || skierId > MAX_SKIER_ID){
            sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, INVALID_PAYLOAD);
        } else {
            String resortSeasonDayId = resortId + "#" + seasonId + "#" + dayId;
            QueryRequest queryRequest = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("ResortSeasonDayId = :rsdId AND SkierId = :skierId")
                .expressionAttributeValues(Map.of(
                    ":rsdId", AttributeValue.builder().s(resortSeasonDayId).build(),
                    ":skierId", AttributeValue.builder().n(String.valueOf(skierId)).build()
                ))
                .build();

            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

            int totalVertical = queryResponse.items().stream()
                .mapToInt(item -> Integer.parseInt(item.get("vertical").n()))
                .sum();

            sendResponseMsg(response, HttpServletResponse.SC_OK, new ResponseMsg("{\"totalVertical\": " + totalVertical + "}"));
        }

    }

    // get the total vertical for the skier the specified resort. If no season is specified, return all seasons
    private void processSkierVertical(HttpServletRequest request, HttpServletResponse response,
        String[] urlParts) throws IOException {
        // Assuming URL pattern: /skiers/{skierID}/vertical
        int skierId = Integer.parseInt(urlParts[1]);
        // Get resort and season from the query parameters
        String resortParam = request.getParameter("resort");
        String seasonParam = request.getParameter("season");  // This is optional

        if (!urlParts[2].equals("vertical")) {
            sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, INVALID_PATH);
        } else if (skierId < LOWER_BOUND || skierId > MAX_SKIER_ID || resortParam == null || Integer.parseInt(resortParam) != RESORT_ID) {
            sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, INVALID_PAYLOAD);
        } else {
            int resortId = Integer.parseInt(resortParam);
            int seasonId = seasonParam != null ? Integer.parseInt(seasonParam) : -1; // If season is not specified, use -1 or handle accordingly

            // Building the key condition expression
            String keyConditionExpression;
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

            // Common part of key
            expressionAttributeValues.put(":skierId", AttributeValue.builder().n(String.valueOf(skierId)).build());

            if (seasonId > 0) {
                // Query for a specific season
                String resortSeasonDayId = resortId + "#" + seasonId;
                keyConditionExpression = "begins_with(ResortSeasonDayId, :rsdId) AND SkierId = :skierId";
                expressionAttributeValues.put(":rsdId", AttributeValue.builder().s(resortSeasonDayId).build());
            } else {
                // Query across all seasons for the resort
                keyConditionExpression = "begins_with(ResortSeasonDayId, :resortId) AND SkierId = :skierId";
                expressionAttributeValues.put(":resortId", AttributeValue.builder().s(resortId + "#").build());
            }

            try {
                QueryRequest queryRequest = QueryRequest.builder()
                    .tableName("TABLE_NAME")
                    .keyConditionExpression(keyConditionExpression)
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

                QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
                int totalVertical = queryResponse.items().stream()
                    .mapToInt(item -> Integer.parseInt(item.get("vertical").n()))
                    .sum();

                sendResponseMsg(response, HttpServletResponse.SC_OK, new ResponseMsg("{\"totalVertical\": " + totalVertical + "}"));
            } catch (DynamoDbException e) {
                e.printStackTrace();
                sendResponseMsg(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, new ResponseMsg("{\"error\": \"Error querying database\"}"));
            }
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
        throws IOException, InterruptedException {
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

        int resortId = Integer.parseInt(urlParts[1]); // resortID
        int seasonId = Integer.parseInt(urlParts[3]); // seasonID
        int dayId = Integer.parseInt(urlParts[5]); // dayID
        int skierId = Integer.parseInt(urlParts[7]); // skierID

        String formattedMessage = skierId + "," + resortId + "," + seasonId +"," + dayId +"," + liftRide.getTime() +"," + liftRide.getLiftID();

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
                resortID != RESORT_ID ||
                skierID < LOWER_BOUND || skierID > MAX_SKIER_ID ||
                (dayID != DAY_ID || dayID != 2 || dayID != 3)) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }


}
