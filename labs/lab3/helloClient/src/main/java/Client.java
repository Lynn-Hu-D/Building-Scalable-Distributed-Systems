import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class Client {
  private static String url = "http://localhost:8080/hello";
  private static final int NUM_THREADS = 100;

  public static void main(String[] args) {

    // create an array to hold the thread references
    Thread[] threads = new Thread[NUM_THREADS];

    // Record start time
    long startTime = System.currentTimeMillis();

    // start the threads
    for (int i = 0; i < NUM_THREADS; i++) {
      threads[i] = new Thread(() ->{

        // Create an instance of HttpClient.
        HttpClient client = HttpClients.createDefault();

        // Create a method instance.
        HttpGet method = new HttpGet(url);

        // Set custom request configuration with retry handler
        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(5000)  // Set socket timeout in milliseconds
            .setConnectTimeout(5000) // Set connection timeout in milliseconds
            .setConnectionRequestTimeout(5000) // Set connection request timeout in milliseconds
            .setStaleConnectionCheckEnabled(true)
            .build();

        method.setConfig(requestConfig);

        try {
          // Execute the method.
          HttpResponse response = client.execute(method);

          // Get the status code.
          int statusCode = response.getStatusLine().getStatusCode();

          if (statusCode == HttpStatus.SC_OK) {
            // Read the response body.
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);

            // Deal with the response.
            // Use caution: ensure correct character encoding and check if it's not binary data
            System.out.println(responseBody);
          } else {
            System.err.println("Method failed: " + response.getStatusLine());
          }

        } catch (IOException e) {
          System.err.println("Fatal transport error: " + e.getMessage());
          e.printStackTrace();
        } finally {
          // Release the connection.
          method.releaseConnection();
        }
      });

      threads[i].start();
    }

    // wait all threads to compete
    for (Thread thread: threads) {
      try{
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    // Record end time
    long endTime = System.currentTimeMillis();

    // Calculate and print the time taken
    long timeTaken = endTime - startTime;
    System.out.println("Time taken: " + timeTaken + " ms");

}
}
