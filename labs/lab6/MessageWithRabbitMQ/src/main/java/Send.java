
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
public class Send {

  private final static String QUEUE_NAME = "hello";
  public static void main(String[] args) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");

    // try catch to auto close the channel and connection
    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {

        // Declaring a queue is idempotent - it will only be created if it doesn't exist already.
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        String message = "Hello World!";

        // The message content is a byte array, so you can encode whatever you like there.
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
    }
  }

}
