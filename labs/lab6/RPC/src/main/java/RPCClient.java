import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;


// AutoCloseable -> allow resources to be automatically released or closed when they're no longer needed,
// such as when exiting a try-with-resources block or when an object is garbage collected.
public class RPCClient implements AutoCloseable {

  private Connection connection;
  private Channel channel;
  private String requestQueueName = "rpc_queue";
  static final int RPC_TIMEOUT_MS = 5000;

  public RPCClient() throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");

    connection = factory.newConnection();
    channel = connection.createChannel();
  }

  public static void main(String[] argv) {
    try (RPCClient fibonacciRpc = new RPCClient()) {
      for (int i = 0; i < 32; i++) {
        String i_str = Integer.toString(i);
        System.out.println(" [x] Requesting fib(" + i_str + ")");
        String response = fibonacciRpc.call(i_str);
        System.out.println("fib(" + i_str + ") is " + response);
      }
    } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  // perform the actual RPC call
  public String call(String message)
      throws IOException, InterruptedException, ExecutionException, TimeoutException {

    // generate a unique correlation OD (corrId) for the request
    final String corrId = UUID.randomUUID().toString();

    // a temp queue for receiving the response
    String replyQueueName = channel.queueDeclare().getQueue();

    // set up message properties
    AMQP.BasicProperties props = new AMQP.BasicProperties
        .Builder()
        .correlationId(corrId)
        .replyTo(replyQueueName)
        .build();

    // publish the request message to the request queue
    channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

    // Create a CompletableFuture to hold the response
    final CompletableFuture<String> response = new CompletableFuture<>();

    // Listen for the response on the temporary queue
    String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
      // Check if the received correlation ID matches the expected one
      if (delivery.getProperties().getCorrelationId().equals(corrId)) {
        response.complete(new String(delivery.getBody(), "UTF-8")); // Complete the CompletableFuture with the response
      }
    }, consumerTag -> {
    });

    // Get the response from the CompletableFuture

    try {
      return response.get(RPC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new TimeoutException("RPC call time out");
    } finally {
      channel.basicCancel(ctag);
    }

  }

  // Method to close the connection to RabbitMQ
  public void close() throws IOException {
    connection.close();
  }
}