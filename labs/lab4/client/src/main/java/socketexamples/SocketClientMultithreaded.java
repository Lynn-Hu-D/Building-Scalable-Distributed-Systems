package socketexamples;

/**
 * Skeleton socket client.
 * Accepts host/port on command line or defaults to localhost/12031
 * Then (should) starts MAX_Threads and waits for them all to terminate before terminating main()
 * @author Ian Gorton
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketClientMultithreaded {

  static CyclicBarrier barrier;
  static final int MAX_CLIENTS = 2000;

  public static void main(String[] args)  {

    long startTime = System.currentTimeMillis();
    String hostName;
    int port;

    if (args.length == 2) {
      hostName = args[0];
      port = Integer.parseInt(args[1]);
    } else {
      hostName= null;
      port = 12031;  // default port in SocketServer
    }
    barrier = new CyclicBarrier(MAX_CLIENTS);

    List<SocketClientThread> threads = new ArrayList<>();

    // TO DO create and start MAX_THREADS SocketClientThread
    for (int i = 1; i <= MAX_CLIENTS; i++) {
      SocketClientThread thread = new SocketClientThread(hostName, port, i, barrier);
      thread.start();
      threads.add(thread);
    }

    System.out.println("Terminating ....");

    // TO DO wait for all threads to complete
    for (SocketClientThread thread : threads) {
      try {
        thread.join(); // Wait for the thread to finish
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    System.out.println("All threads have completed. Terminating....");
    long endTime = System.currentTimeMillis();
    System.out.println("Wall Time is: " + (endTime - startTime) + " ms");

  }

}