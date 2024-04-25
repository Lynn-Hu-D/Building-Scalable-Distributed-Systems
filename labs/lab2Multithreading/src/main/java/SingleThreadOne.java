import java.util.ArrayList;
import java.util.Vector;

public class SingleThreadOne {

  public static void main(String[] args) throws InterruptedException {
    int numThreads = 100000;
    // Vector is synchronized
    long vectorStartTime = System.currentTimeMillis();

    Vector<Integer> vector = new Vector<>();
    for (int i = 0; i < numThreads; i++) {
      vector.add(i);
    }

    long vectorEndTime = System.currentTimeMillis();
    System.out.println("Vector time: " + (vectorEndTime - vectorStartTime) + " ms");



    // ArrayList is  not
    long arrayListStartTime = System.currentTimeMillis();
    ArrayList<Integer> arrayList = new ArrayList<>();

    for (int i = 0; i < numThreads; i++) {
      arrayList.add(i);
    }

    long arrayListEndTime = System.currentTimeMillis();
    System.out.println("ArrayList time: " + (arrayListEndTime - arrayListStartTime) + " ms");


    /*
    Vector time: 6 ms
    ArrayList time: 6 ms
     */

  }

}
