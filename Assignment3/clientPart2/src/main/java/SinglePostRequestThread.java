import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;


public class SinglePostRequestThread implements Runnable {
  private int requestsPerThread;
  private long startTime;
  private long endTime;
  private int threadId;

  public SinglePostRequestThread(int requestsPerThread, int threadId) {
    this.requestsPerThread = requestsPerThread;
    this.threadId = threadId;
  }


  @Override
  public void run()  {
    ApiClient client = new ApiClient().setBasePath(SkiersClient.basePath);
    SkiersApi apiInstance = new SkiersApi(client);
    postRequest(apiInstance);
    SkiersClient.completion.countDown();
  }


  private void postRequest(SkiersApi apiInstance) {
    // 1000 per thread
    for (int i = 0; i < this.requestsPerThread; i++) {
      int retry = 0;
//      if (SkiersClient.successfulPostRequests.get() >= SkiersClient.TOTAL_REQUEST_COUNT) break;
      // retry at most 5 times
      startTime = System.currentTimeMillis();
      while (retry < SkiersClient.RETRIES ) {
        retry++;
        try {
          LiftDataGenerator liftData = SkiersClient.liftRideQueue.remove();
          ApiResponse<Void> response = apiInstance.writeNewLiftRideWithHttpInfo(
              liftData.getLiftRide(),
              liftData.getResortID(),
              liftData.getSeasonID(),
              liftData.getDayID(),
              liftData.getSkierID());
        /*
        check the response status
        if success-> increment successful_post_request
        fail -> retry + 1
         */
          int statusCode = response.getStatusCode();
          if (statusCode == 201) {

            System.out.println("sent");
//            SkiersClient.successfulPostRequests.incrementAndGet();
            endTime = System.currentTimeMillis();

            // store data fro later processing
            PostMetric metric = new PostMetric(startTime, endTime, endTime - startTime);
            SkiersClient.metrics.get(threadId).add(metric);
            break;
          } else if (statusCode >= 400 && statusCode < 600){
            if (retry >= SkiersClient.RETRIES) {
              SkiersClient.unsuccessfulPostRequests.incrementAndGet();
              break;
            }
          }
        } catch (ApiException e) {
          // Log ApiException details
          e.printStackTrace();
          if (retry >= SkiersClient.RETRIES) {
            SkiersClient.unsuccessfulPostRequests.incrementAndGet();
            throw new RuntimeException("Maximum retries exceeded", e);
          }
        }
      }
    }

  }

}


