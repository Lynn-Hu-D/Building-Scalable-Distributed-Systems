import io.swagger.client.ApiException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SkiersClient {
  static final String basePath = "http://44.237.113.102:8080/server";

  static final int REQUESTS_COUNT_PER_THREAD = 1000;
  private static final int INITIAL_THREAD_COUNT = 32;
  static final int NEW_THREAD_COUNT = 400;
  static final int TOTAL_REQUEST_COUNT = 200000;
  static final int RETRIES = 5;
  static AtomicInteger unsuccessfulPostRequests = new AtomicInteger();
  static AtomicInteger successfulPostRequests = new AtomicInteger();
  static ExecutorService initialThreadPool;
  static ExecutorService newlyCreatedPool;
  static BlockingQueue<LiftDataGenerator> liftRideQueue;
  static int perThread;
  static CountDownLatch completion;
  static List<List<Integer>> metrics = new LinkedList<>();
  static long wallTime ;


  public static void main(String[] args) throws ApiException, InterruptedException, IOException {
    long startTime = System.currentTimeMillis();

    // create a fixed pool for 32 threads & a cached pool for newly created threads
    initialThreadPool = Executors.newFixedThreadPool(INITIAL_THREAD_COUNT);
    newlyCreatedPool = Executors.newCachedThreadPool();

    completion = new CountDownLatch(1);

    // a thread for liftRide generator
    liftRideQueue = new LinkedBlockingQueue<>(TOTAL_REQUEST_COUNT);
    newlyCreatedPool.submit(new Thread(new LiftRideGeneratorThread(liftRideQueue)));


    // create 32 initial consume threads
    for (int i = 0; i < INITIAL_THREAD_COUNT; i++) {
      initialThreadPool.submit(new SinglePostRequestThread(REQUESTS_COUNT_PER_THREAD, i));
    }

    // wait for any thread finish its 1000 requests
    completion.await();
    perThread = (TOTAL_REQUEST_COUNT - INITIAL_THREAD_COUNT * REQUESTS_COUNT_PER_THREAD) / NEW_THREAD_COUNT + 5;
    for (int i = 0; i < NEW_THREAD_COUNT; i++) {
      newlyCreatedPool.submit(new SinglePostRequestThread(perThread, INITIAL_THREAD_COUNT + i));
    }

    //  termination
    initialThreadPool.shutdown();
    newlyCreatedPool.shutdown();
    initialThreadPool.awaitTermination(8000, TimeUnit.SECONDS);
    newlyCreatedPool.awaitTermination(8000, TimeUnit.SECONDS);

    // cal the time and throughput
    long endTime = System.currentTimeMillis();
    wallTime = endTime - startTime;


    System.out.println("Phase1 Thread count: 32");
    System.out.println("Phase2 Thread count: " + NEW_THREAD_COUNT);
    System.out.println("");
    System.out.println("Number of successful requests sent: " + successfulPostRequests);
    System.out.println("Number of unsuccessful requests: " + unsuccessfulPostRequests);
    System.out.println("Total runtime: " + wallTime + " ms");
    System.out.println("Throughput: " + successfulPostRequests.get() / (wallTime / 1000) + " requests per second");
  }

}

