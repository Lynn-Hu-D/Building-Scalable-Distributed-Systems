import java.util.concurrent.CountDownLatch;

public class MultiThreadedProgram {

  public static void main(String[] args) throws InterruptedException {
    int numThreads = 100000;

    Counter counter = new Counter();
    CountDownLatch completed = new CountDownLatch(numThreads);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < numThreads; i++) {
      IncrementThread thread = new IncrementThread(counter, completed);
      thread.start();
    }

    completed.await();

    long endTime = System.currentTimeMillis();

    System.out.println("Counter value: " + counter.getCount());
    System.out.println("Total duration: " + (endTime - startTime) + " milliseconds");

    /*
    The duration is not fixed

    Counter value: 10000
    Total duration: 85 milliseconds

    Counter value: 100000
    Total duration: 782 milliseconds

    Counter value: 1000000
    Total duration: 6029 milliseconds

     */


  }

}
