package Consumer;

import static Consumer.Config.QUEUE_NAME;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;

public class ConsumerThread implements Runnable {
  private Connection connection;
  private Channel channel;
  private ChannelPool channelPool;

  public ConsumerThread(ChannelPool channelPool) {
    this.channelPool = channelPool;
  }

  @Override
  public void run() {
    try {
      channel = this.channelPool.borrowChannel();
      channel.queueDeclare(QUEUE_NAME, false, false, false, null);

      // Define callback for handling incoming messages
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        // Process the message (update HashMap)
        processMessage(message);
      };

      channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
    } catch( IOException e) {
    throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        channelPool.returnChannel(channel);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void processMessage(String message) {
    // Parse the message and update the skierRidesMap
    String[] parts = message.split(",");
    if (parts.length == 3) {
      int skierId = Integer.parseInt(parts[0]);
      int liftID = Integer.parseInt(parts[1]);
      int time = Integer.parseInt(parts[2]);

      LiftRideConsumer.skierRidesMap.put(skierId, new LiftRide(liftID, time));
    }
  }

}
