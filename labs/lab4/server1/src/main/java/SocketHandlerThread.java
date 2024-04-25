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
class SocketHandlerThread extends Thread {
  private final Socket clientSocket;
  private boolean running = true;
  private final ActiveCount threadCount;

  SocketHandlerThread(Socket s, ActiveCount threads) throws IOException {
    clientSocket = s;
    threadCount = threads;

  }


  public void run() {
    threadCount.incrementCount();
    System.out.println("Accepted Client: Address - "
        + clientSocket.getInetAddress().getHostName());
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

      // read a line of text from the input stream of the clientSocket
      String clientID = in.readLine();
      System.out.println("Client ID is : " + clientID);

      // send a message to the client containing the active server thread count
      out.println("Active Server Thread Count = " + Integer.toString( threadCount.getCount() ));
      out.flush();

      System.out.println("Reply sent");
      } catch (IOException ex) {
      throw new RuntimeException(ex);
      } finally {
      threadCount.decrementCount();
      try {
        clientSocket.close();
      } catch (IOException e) {
      e.printStackTrace();}
    }
      System.out.println("Thread exiting");
    }

}