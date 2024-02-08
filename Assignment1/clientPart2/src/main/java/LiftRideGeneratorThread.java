import java.util.concurrent.BlockingQueue;

public class LiftRideGeneratorThread implements Runnable {

  private final BlockingQueue<LiftDataGenerator> liftRideQueue;

  public LiftRideGeneratorThread(BlockingQueue<LiftDataGenerator> liftRideQueue) {
    this.liftRideQueue = liftRideQueue;
  }


  @Override
  public void run() {
    try {
      for (int i = 0; i < SkiersClient.TOTAL_REQUEST_COUNT && !Thread.currentThread().isInterrupted(); i++) {
        liftRideQueue.put(new LiftDataGenerator());
      }
    } catch (InterruptedException e) {
      // handle the error
      Thread.currentThread().interrupt();
      System.err.println("LiftRideGeneratorThread interrupted: " + e.getMessage());
    }
  }

}
