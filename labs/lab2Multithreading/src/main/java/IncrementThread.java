import java.util.concurrent.CountDownLatch;

public class IncrementThread extends Thread{
  private Counter counter;
  private CountDownLatch latch;

  public IncrementThread(Counter counter, CountDownLatch latch) {
    this.counter = counter;
    this.latch = latch;
  }

  @Override
  public void run() {
    counter.increment();
    latch.countDown(); // Signal that the thread has completed
  }
}
