package socketexamples;

/**
 *
 * @author Ian Gorton
 * Basic socket server that implements a thread-per-connection model:
 * 1) starts and listens for connections on port 12031
 * 2) When a connection received, spawn a thread to handle connection
 *
 */



import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class SocketServerWithThreadPool {
  public static void main(String[] args) throws Exception {
    // Define the size of the thread pool
    int poolSize = 2000;

    // Create the thread pool
    ExecutorService threadPool = Executors.newFixedThreadPool(poolSize);

    try  (ServerSocket m_ServerSocket = new ServerSocket(12031)){

      // create object o count active threads
      ActiveCount threadCount = new ActiveCount();
      System.out.println("Server started .....");

      while (true) {
        // accept connection and start thread
        Socket clientSocket = m_ServerSocket.accept();
        SocketHandlerThread server = new SocketHandlerThread (clientSocket, threadCount);
        threadPool.submit(server);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      // Shutdown the thread pool when the server terminates
      threadPool.shutdown();
    }
  }
}
