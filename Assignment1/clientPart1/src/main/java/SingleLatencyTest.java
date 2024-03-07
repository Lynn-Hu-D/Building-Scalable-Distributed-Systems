import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;

public class SingleLatencyTest {
  private static long requestCount = 1000;
  public static void main(String[] args) {
    long startTime = System.currentTimeMillis();
    ApiClient client = new ApiClient().setBasePath(SkiersClient.basePath);
    SkiersApi apiInstance = new SkiersApi(client);

    for (int i = 0; i < requestCount; i++) {
      try {
        LiftDataGenerator liftData = new LiftDataGenerator();
        ApiResponse<Void> response = apiInstance.writeNewLiftRideWithHttpInfo(
            liftData.getLiftRide(),
            liftData.getResortID(),
            liftData.getSeasonID(),
            liftData.getDayID(),
            liftData.getSkierID());
      } catch (ApiException e) {
        // Log ApiException details
        System.err.println("ApiException occurred:");
        System.err.println("  HTTP Status Code: " + e.getCode());
        System.err.println("  Response Body: " + e.getResponseBody());
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
    long endTime = System.currentTimeMillis();
    long latency = ((endTime - startTime)  / requestCount);
    System.out.println("Latency for single thread: " + latency + " ms");
  }
}
