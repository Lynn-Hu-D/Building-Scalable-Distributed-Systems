import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class TestTwo {
  private static final Integer ELEMENT = 10000000;
  private static final Integer NUM_THREADS = 100;
  public static void main(String[] args) throws InterruptedException {

    /*
    Hashtable time: 23 ms
    HashMap time: 13 ms
    ConcurrentHashMap time: 44 ms
     */
    singleThread("Hashtable", new Hashtable<>());
    singleThread("HashMap", new HashMap<>());
    singleThread("ConcurrentHashMap", new ConcurrentHashMap<>());


    /*
    Hashtable (Multi-threaded) time: 72 ms
    HashMap (Multi-threaded) time: 50 ms
    ConcurrentHashMap (Multi-threaded) time: 139 ms
     */
    multiThreaded("Hashtable", new Hashtable<>());
    multiThreaded("HashMap", Collections.synchronizedMap(new HashMap<>()));
    multiThreaded("ConcurrentHashMap", new ConcurrentHashMap<>());


  }

  private static void singleThread(String type, Map<Integer, Integer> map) {
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < ELEMENT; i++) {
      map.put(i, i*i);
    }

    long endTime = System.currentTimeMillis();
    System.out.println(type + " time: " + (endTime - startTime) + " ms");
  }

  private static void multiThreaded(String type, Map<Integer, Integer> map) {
    long startTime = System.currentTimeMillis();

    Thread[] threads = new Thread[NUM_THREADS];

    for (int i = 0; i < NUM_THREADS; i++) {
      final int threadId = i;
      threads[i] = new Thread(() -> {
        for (int j = 0; j < ELEMENT / NUM_THREADS; j++) {
          map.put(threadId * (ELEMENT / NUM_THREADS) + j, threadId);
        }
      });
      threads[i].start();
    }

    // Wait for all threads to finish
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    long endTime = System.currentTimeMillis();
    System.out.println(type + " (Multi-threaded) time: " + (endTime - startTime) + " ms");
  }


}
