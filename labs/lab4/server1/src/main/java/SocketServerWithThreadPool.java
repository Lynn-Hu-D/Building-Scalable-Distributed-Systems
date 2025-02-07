

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class SocketServerWithThreadPool {
  public static void main(String[] args) throws Exception {
    // create socket listener
    try (ServerSocket listener = new ServerSocket(12031)){
      // create object to count active threads
      ActiveCount threadCount = new ActiveCount();
      System.out.println("Server started .....");
      // create thread pool and accept connections
      Executor pool = Executors.newFixedThreadPool(50);
      while (true) {
        Socket clientSocket = listener.accept();
        pool.execute(new SocketHandlerRunnable(clientSocket, threadCount));
      }
    }
  }


}