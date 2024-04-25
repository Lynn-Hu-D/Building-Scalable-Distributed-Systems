import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketClientThread extends Thread {
  private long clientID;
  private String hostName;
  private int port;
  private CyclicBarrier synk;
  private Socket socket;

  private final static int NUM_ITERATIONS = 1000;

  public SocketClientThread(String hostName, int port, CyclicBarrier barrier) {
    this.hostName = hostName;
    this.port = port;
    synk = barrier;
  }

  @Override
  public void run() {
    clientID = Thread.currentThread().getId();
    try {
      socket = new Socket(hostName, port); // Create the socket outside the loop
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      for (int i = 0; i < NUM_ITERATIONS; i++) {
        out.println("Client ID is " + clientID);
        System.out.println(in.readLine());
      }

      // Close the socket after sending all requests
      socket.close();
    } catch (UnknownHostException e) {
      System.err.println("Don't know about host " + hostName);
    } catch (IOException e) {
      System.err.println("Couldn't get I/O for the connection to " + hostName);
    }

    try {
      System.out.println("Thread waiting at barrier");
      synk.await();
    } catch (InterruptedException | BrokenBarrierException ex) {
      Logger.getLogger(SocketClientThread.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
