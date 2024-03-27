
import io.swagger.client.ApiException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class SkiersClient {
//  static final String basePath = "http://44.237.113.102:8080/QS100";
//  static final String basePath = "http://localhost:8080";
static final String basePath = "http://my-server-lb-1948541718.us-west-2.elb.amazonaws.com/QS200";


  static final int REQUESTS_COUNT_PER_THREAD = 1000;
  private static final int ORIGINAL_THREAD_COUNT = 32;
  static final int NEW_THREAD_COUNT = 100;
  static final int TOTAL_REQUEST_COUNT = 200000;
  static final int RETRIES = 5;
  static final int PERCENTILE = 99;
  static final String CSV_FILE_PATH = "requestMetrics.csv";
  static AtomicInteger unsuccessfulPostRequests;
  static ExecutorService originalPostPool;
  static ExecutorService newlyCreatedPool;
  static BlockingQueue<LiftDataGenerator> liftRideQueue;
  static int perThread;
  static CountDownLatch completion;
  static List<List<PostMetric>> metrics = new LinkedList<>();
  static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  static long wallTime ;
  static long startTime;

  public static void main(String[] args) throws ApiException, InterruptedException, IOException {
    startTime = System.currentTimeMillis();
    unsuccessfulPostRequests = new AtomicInteger();
    for (int i=0; i< ORIGINAL_THREAD_COUNT+NEW_THREAD_COUNT; i++) {
      metrics.add(new LinkedList<>());
    }

    // create a fixed pool for 32 threads & a flex pool for newly created threads
    originalPostPool = Executors.newFixedThreadPool(ORIGINAL_THREAD_COUNT);
    newlyCreatedPool = Executors.newCachedThreadPool();

    // a thread for liftRide generator
    liftRideQueue = new LinkedBlockingQueue<>();
    Thread liftGeneratorThread = new Thread(new LiftRideGeneratorThread(liftRideQueue));
    liftGeneratorThread.start();

    completion = new CountDownLatch(1);
    int threadId = 0;
    // create 32 initial consume threads
    for (int i = 0; i < ORIGINAL_THREAD_COUNT; i++) {
      originalPostPool.submit(new SinglePostRequestThread(REQUESTS_COUNT_PER_THREAD, threadId));
      threadId++;
    }

    // wait for any thread finish its 1000 requests
    completion.await();
    perThread = (TOTAL_REQUEST_COUNT - ORIGINAL_THREAD_COUNT * REQUESTS_COUNT_PER_THREAD) / NEW_THREAD_COUNT + 1;
    for (int i = 0; i < NEW_THREAD_COUNT; i++) {
      newlyCreatedPool.submit(new SinglePostRequestThread(perThread, threadId));
      threadId++;
    }

    //  termination
    liftGeneratorThread.join();
    originalPostPool.shutdown();
    newlyCreatedPool.shutdown();
    originalPostPool.awaitTermination(300, TimeUnit.SECONDS);
    newlyCreatedPool.awaitTermination(300, TimeUnit.SECONDS);

    // cal the time and throughput
    long endTime = System.currentTimeMillis();
    wallTime = endTime - startTime;

    List<PostMetric> flattenedMetrics = metrics.stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());

    System.out.println("Number of Threads used: " + (ORIGINAL_THREAD_COUNT + NEW_THREAD_COUNT));
    System.out.println("Successful Threads: " + flattenedMetrics.size());
    System.out.println("Total time: " + wallTime + " ms");
    System.out.println("");


    calculateMetrics(flattenedMetrics);
//    writeToFile(flattenedMetrics);
//    plotThroughput(flattenedMetrics);
  }

  private static void calculateMetrics(List<PostMetric> metrics) {
    // cal mean
    Long meanResponseTime = calMeanLatency(metrics);

    // cal median
    Long medianResponseTime = calMedianLatency(metrics);

    // p99
    Long p99ResponseTime = calP99Latency(metrics);

    // min & max
    long[] extremes = calExtremeLatency(metrics);
    Long minResponseTime = extremes[0];
    Long maxResponseTime = extremes[1];

    System.out.println("Mean Response Time: " + meanResponseTime + " ms");
    System.out.println("Median Response Time: " + medianResponseTime + " ms");
    System.out.println("Throughput: " + (metrics.size() / (wallTime / 1000) )+ " requests per second");
    System.out.println("p99 Response Time: " + p99ResponseTime + " ms");
    System.out.println("Min Response Time: " + minResponseTime + " ms");
    System.out.println("Max Response Time: " + maxResponseTime + " ms");
  }


  private static long calMeanLatency(List<PostMetric> metrics) {
    long latencies = 0;
    for (PostMetric metric : metrics) {
      latencies += metric.getLatency();
    }
    return latencies / metrics.size();
  }

  private static long calMedianLatency(List<PostMetric> metrics) {
    List<PostMetric> sortedMetrics = sortMetrics(metrics);
    return sortedMetrics.get(metrics.size() / 2).getLatency();
  }

  private static long calP99Latency(List<PostMetric> metrics) {
    List<PostMetric> sortedMetrics = sortMetrics(metrics);

    int index = (int) (PERCENTILE / 100.0 * metrics.size());
    return sortedMetrics.get(index - 1).getLatency();
  }
  private static List<PostMetric> sortMetrics(List<PostMetric> metrics) {
    List<PostMetric> sortedMetrics = new ArrayList<>(metrics);
    Comparator<PostMetric> latencyComparator = new Comparator<PostMetric>() {
      @Override
      public int compare(PostMetric metric1, PostMetric metric2) {
        // Compare based on latency
        return Double.compare(metric1.getLatency(), metric2.getLatency());
      }
    };
    Collections.sort(sortedMetrics, latencyComparator);
    return  sortedMetrics;
  }

  private static long[] calExtremeLatency(List<PostMetric> metrics) {
    long min = Long.MAX_VALUE;
    long max = Long.MIN_VALUE;

    for (PostMetric metric :metrics) {
      long latency = metric.getLatency();
      if (latency < min) {
        min = latency;
      }

      if (latency > max) {
        max = latency;
      }
    }
    return new long[]{min, max};
  }


  private static void writeToFile(List<PostMetric> metrics) throws IOException {

    List<PostMetric> sortedMetrics = sortByStartTime(metrics);

    BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE_PATH));
    writer.append(String.format("%-25s %-10s %-10s %-5s%n", "StartTime", "ResponseType", "Latency(ms)", "ResponseCode"));

    for (int i = 0; i < metrics.size(); i++) {
      String curTime = dateFormat.format(new Date(sortedMetrics.get(i).getStart()));
      try  {
        String formattedLine = String.format("%-25s %-10s %-10d %-5d%n", curTime, "POST", sortedMetrics.get(i).getLatency(), 201);
        writer.append(formattedLine);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

  }

  private static void plotThroughput(List<PostMetric> metrics) throws IOException {
    List<PostMetric> sortedMetrics = sortByEndTime(metrics);
    String plotPath = "ThroughputPlot.csv";

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(plotPath))) {
      writer.append(String.format("%-75s %-25s%n", "Current Time", "Throughput"));

      sortedMetrics.stream()
          .map(metric -> {
            long curTime = metric.getEnd();
            double throughput = (curTime - startTime == 0) ? 0 : (sortedMetrics.indexOf(metric) + 1) / ((curTime - startTime) / 1000.0);
            return String.format("%-75s %-25f%n", dateFormat.format(new Date(curTime)), throughput);
          })
          .forEach(line -> {
            try {
              writer.write(line);
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
    }
  }

  private static List<PostMetric> sortByStartTime(List<PostMetric> metrics) {
    List<PostMetric> sortedMetrics = new ArrayList<>(metrics);
    Comparator<PostMetric> latencyComparator = new Comparator<PostMetric>() {
      @Override
      public int compare(PostMetric metric1, PostMetric metric2) {
        // Compare based on starttime
        return Double.compare(metric1.getStart(), metric2.getStart());
      }
    };
    Collections.sort(sortedMetrics, latencyComparator);
    return  sortedMetrics;
  }

  private static List<PostMetric> sortByEndTime(List<PostMetric> metrics) {
    List<PostMetric> sortedMetrics = new ArrayList<>(metrics);
    Comparator<PostMetric> latencyComparator = new Comparator<PostMetric>() {
      @Override
      public int compare(PostMetric metric1, PostMetric metric2) {
        // Compare based on endtime
        return Double.compare(metric1.getEnd(), metric2.getEnd());
      }
    };
    Collections.sort(sortedMetrics, latencyComparator);
    return  sortedMetrics;
  }

}

