import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class LiftRideGeneratorThread implements Runnable {

  private final BlockingQueue<LiftDataGenerator> liftRideQueue;
  private Set<String> generatedCombinations = new HashSet<>();

  public LiftRideGeneratorThread(BlockingQueue<LiftDataGenerator> liftRideQueue) {
    this.liftRideQueue = liftRideQueue;
  }


  @Override
  public void run() {
    try {
      while (generatedCombinations.size() < SkiersClient.TOTAL_REQUEST_COUNT && !Thread.currentThread().isInterrupted()) {
        LiftDataGenerator newGenerator = new LiftDataGenerator(); // Assume this generates a new unique combination.
        String combination = newGenerator.getCombination(); // A method to get a unique string representation of the combination.

        if (!generatedCombinations.contains(combination)) {
          liftRideQueue.put(newGenerator);
          generatedCombinations.add(combination);
        }
      }
    } catch (InterruptedException e) {
      // handle the error
      Thread.currentThread().interrupt();
      System.err.println("LiftRideGeneratorThread interrupted: " + e.getMessage());
    }
  }

}
