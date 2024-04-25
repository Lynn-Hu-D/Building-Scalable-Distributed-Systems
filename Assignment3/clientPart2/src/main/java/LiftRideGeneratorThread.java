import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class LiftRideGeneratorThread implements Runnable {

  private final BlockingQueue<LiftDataGenerator> liftRideQueue;

  public LiftRideGeneratorThread(BlockingQueue<LiftDataGenerator> liftRideQueue) {
    this.liftRideQueue = liftRideQueue;
  }


  @Override
  public void run() {
    try {
        LiftDataGenerator newGenerator = new LiftDataGenerator(); // Assume this generates a new unique combination.
        liftRideQueue.put(newGenerator);

    } catch (InterruptedException e) {
      // handle the error
      Thread.currentThread().interrupt();
      System.err.println("LiftRideGeneratorThread interrupted: " + e.getMessage());
    }
  }

}
