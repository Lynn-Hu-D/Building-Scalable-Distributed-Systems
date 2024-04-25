
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * Simple thread to handle a socket request
 * Author: Ian Gorton
 */
public class SocketHandlerRunnable implements Runnable {
  private final Socket clientSocket;
  private boolean running = true;

  private final ActiveCount threadCount;

  SocketHandlerRunnable(Socket s, ActiveCount threads) {
    clientSocket = s;
    threadCount = threads;
  }

  @Override
  public void run() {
    threadCount.incrementCount();
    System.out.println("Accepted Client: Address - "
        + clientSocket.getInetAddress().getHostName());
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

      String clientID;
      while ((clientID = in.readLine()) != null) {
        System.out.println(clientID);

        // send a message to the client containing the active server thread count
        out.println("Active Server Thread Count = " + Integer.toString( threadCount.getCount() ));
        out.flush();

        System.out.println("Reply sent");
      }


    } catch (IOException e) {
      threadCount.decrementCount();
    } finally {
      threadCount.decrementCount();
      try { clientSocket.close(); } catch (IOException e) {}
      System.out.println("Thread exiting");
    }
  }

}