


import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

@WebServlet(name = "SkierServlet", value = "/skiers/*")
public class SkierServlet extends HttpServlet {
    private static final Integer MIN_DAY_ID = 1;
    private static final Integer MAX_DAY_ID = 366;
    private static final Integer SEASON_ID_LENGTH = 4;

    private static final ResponseMsg MISSING_PARAMS = new ResponseMsg("{\"error\": \"Missing parameters\"}");
    private static final ResponseMsg SUCCESS_MESSAGE = new ResponseMsg("It works!");
    private static final ResponseMsg INVALID_PATH = new ResponseMsg("{\"error\": \"Invalid URL path\"}");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        if (processRequest(request, response)){
            sendResponseMsg(response, HttpServletResponse.SC_OK, SUCCESS_MESSAGE);
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        boolean isValid = processRequest(request, response);
        if (isValid) {
            String dummyData = "{\"LiftRide successfully posted\"}";
            sendResponseMsg(response, HttpServletResponse.SC_CREATED, new ResponseMsg(dummyData));
        }

    }

    private boolean processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.setContentType("application/json");
        String urlPath = request.getPathInfo();


        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            sendResponseMsg(response, HttpServletResponse.SC_NOT_FOUND, MISSING_PARAMS);
            return false;
        }

        String[] urlParts = urlPath.split("/");

        // validate url path and return the response status code
        if (!isUrlValid(urlParts)) {
            sendResponseMsg(response, HttpServletResponse.SC_BAD_REQUEST, INVALID_PATH);
            return false;
        }
        return true;
    }

    private void sendResponseMsg(HttpServletResponse response, int statusCode, ResponseMsg msg)
        throws IOException {
        response.setStatus(statusCode);
        response.getWriter().write(msg.getMessage());
    }
    private boolean isUrlValid(String[] urlPaths) {
        // urlPath  = "/1/seasons/2019/days/1/skiers/123"
        // urlParts = [, 1, seasons, 2019, days, 1, skiers, 123]
        if (urlPaths.length != 8
            || !urlPaths[2].equals("seasons")
            || !urlPaths[4].equals("days")
            || !urlPaths[6].equals("skiers")) {

            return false;
        }

        // Validate numeric parameters:
        try {
            Integer.parseInt(urlPaths[1]); // resortID
            Integer.parseInt(urlPaths[7]); // skierID
        } catch (NumberFormatException e) {
            return false;
        }

        // validate dayID
        try {
            int dayID = Integer.parseInt(urlPaths[5]);    // dayID

            // Validate dayID within the specified range
            if (dayID < MIN_DAY_ID || dayID > MAX_DAY_ID || urlPaths[3].length() != SEASON_ID_LENGTH) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }


}
