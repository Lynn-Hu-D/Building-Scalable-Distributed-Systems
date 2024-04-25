import com.rabbitmq.client.*;

public class RPCServer {

  // Name of the queue used for RPC requests
  private static final String RPC_QUEUE_NAME = "rpc_queue";

  // Fibonacci function to calculate Fibonacci numbers recursively
  private static int fib(int n) {
    if (n == 0) return 0;
    if (n == 1) return 1;
    return fib(n - 1) + fib(n - 2);
  }

  public static void main(String[] argv) throws Exception {
    // Establishing connection to RabbitMQ server
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    // Declaring the queue for RPC requests
    channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
    // Purging the queue to clear any existing messages
    channel.queuePurge(RPC_QUEUE_NAME);

    // Setting the maximum number of unacknowledged messages that can be
    // delivered to this RPC server
    channel.basicQos(1);

    System.out.println(" [x] Awaiting RPC requests");

    // Callback to handle incoming RPC requests
    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      // Creating properties for the response message
      AMQP.BasicProperties replyProps = new AMQP.BasicProperties
          .Builder()
          .correlationId(delivery.getProperties().getCorrelationId())
          .build();

      String response = "";
      try {
        // Extracting the request message (number) from the delivery
        String message = new String(delivery.getBody(), "UTF-8");
        int n = Integer.parseInt(message);

        // Calculating Fibonacci number for the given input
        System.out.println(" [.] fib(" + message + ")");
        response += fib(n);
      } catch (RuntimeException e) {
        // Handling any runtime exceptions that might occur during processing
        System.out.println(" [.] " + e);
      } finally {
        // Publishing the response to the replyTo queue specified in the request
        channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
        // Acknowledging the message delivery
        // acknowledges the message delivery to RabbitMQ.
        // It informs RabbitMQ that the message with the given delivery tag has been successfully processed by the server
        // and can be removed from the queue.
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      }
    };

    // Start consuming messages from the RPC queue
    channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {}));
  }
}
