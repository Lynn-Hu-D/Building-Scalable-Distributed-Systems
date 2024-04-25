public class Counter {
  private int count = 0;

  synchronized public void increment() {
    for (int i = 0; i < 10; i++) {
      count++;
    }
  }

  public int getCount() {
    return this.count;
  }
}
