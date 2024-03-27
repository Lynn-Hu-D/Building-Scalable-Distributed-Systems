package Consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChannelPool {

  private final BlockingQueue<Channel> channelPool;
  private final Connection connection;

  public ChannelPool(int poolSize, ConnectionFactory factory) throws Exception {
    this.channelPool = new LinkedBlockingQueue<>(poolSize);
    this.connection = factory.newConnection(); // Establish a single connection

    // Initialize the pool with pre-created channels
    for (int i = 0; i < poolSize; i++) {
      channelPool.put(connection.createChannel());
    }
  }

  public Channel borrowChannel() throws InterruptedException {
    return channelPool.take(); // This will block if no channels are available
  }

  public void returnChannel(Channel channel) throws InterruptedException, IOException {
    if (channel != null && channel.isOpen()) {
      channelPool.put(channel); // Return the channel to the pool

    } else {
      // Replace closed channel with a new one
      channelPool.put(connection.createChannel());
    }
  }

  public void close() throws Exception {
    for (Channel channel : channelPool) {
      if (channel.isOpen()) {
        channel.close();
      }
    }
    if (connection.isOpen()) {
      connection.close();
    }
  }
}
