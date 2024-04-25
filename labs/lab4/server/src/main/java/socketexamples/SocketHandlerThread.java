package socketexamples;


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
  private  BufferedReader in;
  private PrintWriter out;

  SocketHandlerThread(Socket s, ActiveCount threads) throws IOException {
    clientSocket = s;
    threadCount = threads;
    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
  }


  public void run() {
    threadCount.incrementCount();
    System.out.println("Accepted Client: Address - "
        + clientSocket.getInetAddress().getHostName());

    try {
      // read a line of text from the input stream of the clientSocket
      String clientID = in.readLine();
      System.out.println("Client ID is : " + clientID);

      // send a message to the client containing the active server thread count
      out.println("Active Server Thread Count = " + Integer.toString( threadCount.getCount() ));

      // flushes the output stream, ensuring that any buffered data is written to the underlying output stream immediately.
      out.flush();

      System.out.println("Reply sent");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      threadCount.decrementCount();
      System.out.println("Thread exiting");

      out.close();
      try {
        in.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

  }

}