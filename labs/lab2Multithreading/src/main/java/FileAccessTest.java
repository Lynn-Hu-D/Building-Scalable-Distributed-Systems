import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class FileAccessTest {

  private static final int NUM_THREAD = 500;
  private static final int ITERATION = 1000;
  public static void main(String[] args) throws IOException, InterruptedException {
    /*
      Approach 1 Time: 455 ms
      Approach 2 Time: 186 ms
      Approach 3 Time: 330 ms

      2. Can you design a solution in which only one thread writes to the file
      while the threads are generating the strings.
      3. How would you modify any of your solutions to ensure the data is written to the file
      in ascending timestamp order?
     */

    // write every string to the file immediately after it's generated in the loop in each thread
    long startTime1 = System.currentTimeMillis();
    writeImmediately("output_approach1.txt");
    long endTime1 = System.currentTimeMillis();
    System.out.println("Approach 1 Time: " + (endTime1 - startTime1) + " ms");

    // write all the strings from one thread after they are generated and just before a thread terminates
    long startTime2 = System.currentTimeMillis();
    writeByThread("output_approach2.txt");
    long endTime2 = System.currentTimeMillis();
    System.out.println("Approach 2 Time: " + (endTime2 - startTime2) + " ms");


    /*
    Store all the strings from all threads in a shared collection,
    and write this to a file from your main() thread after all threads are completed
     */
    long startTime3 = System.currentTimeMillis();
    writeAtLast("output_approach3.txt");
    long endTime3 = System.currentTimeMillis();
    System.out.println("Approach 3 Time: " + (endTime3 - startTime3) + " ms");


  }

    private static void writeImmediately(String fileName) throws IOException {
    Thread[] threads = new Thread[NUM_THREAD];

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
      for (int i = 0; i < NUM_THREAD; i++) {
        threads[i] = new Thread(() -> {
          for (int j = 0; j < ITERATION; j++) {
            String line = System.currentTimeMillis() + ", " + Thread.currentThread().getId() + ", " + j;

            try {
              writer.write(line);
              writer.newLine();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });
        threads[i].start();
      }
      // Wait for all threads to finish
      for (Thread thread : threads) {
        thread.join();
      }

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
    private static void writeByThread(String fileName) throws IOException, InterruptedException {
      // Use CountDownLatch to wait for all threads to finish
      CountDownLatch latch = new CountDownLatch(NUM_THREAD);

      for (int i = 0; i < NUM_THREAD; i++) {
        int threadId = i;
        new Thread(() -> {
          StringBuilder sb = new StringBuilder();
          for (int j = 0; j < ITERATION; j++) {
            String line = System.currentTimeMillis() + ", " + Thread.currentThread().getId() + ", " + j;
            sb.append(line).append(System.lineSeparator());
          }

         try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))){
           writer.write(sb.toString());
         } catch (IOException e) {
           throw new RuntimeException(e);
         }
        latch.countDown();

        }).start();
      }
      latch.await();
    }

    private static void writeAtLast(String fileName) throws InterruptedException {
      CountDownLatch latch = new CountDownLatch(NUM_THREAD);
      List<String> sharedCollection = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < NUM_THREAD; i++) {
          int threadId = i;
          new Thread(() -> {
            for (int j = 0; j < ITERATION; j++) {
              String line = System.currentTimeMillis() + ", " + Thread.currentThread().getId() + ", " + j;
              sharedCollection.add(line);
            }
            latch.countDown();

          }).start();
        }

        latch.await();

//      // Sort the shared collection based on timestamp
//      sharedCollection.sort(Comparator.comparingLong(line -> Long.parseLong(line.split(",")[0])));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
          // Write the contents of the shared collection to the file
          for (String line : sharedCollection) {
            writer.write(line);
            writer.newLine();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
  }
}
