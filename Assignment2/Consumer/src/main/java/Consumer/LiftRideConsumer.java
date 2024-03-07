package Consumer;

import static Consumer.Config.POOL_SIZE;
import static Consumer.Config.RABBITMQ_HOST;
import static Consumer.Config.RABBITMQ_PASSWORD;
import static Consumer.Config.RABBITMQ_PORT;
import static Consumer.Config.RABBITMQ_USERNAME;
import static Consumer.Config.THREAD_COUNT;
import static Consumer.Config.TOTAL_NUM_MESSAGE;
import static java.lang.Thread.sleep;

import com.rabbitmq.client.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiftRideConsumer {
  protected  static Map<Integer, Object> skierRidesMap = new ConcurrentHashMap<>();
  private  static ExecutorService executorService;


  public static void main(String[] args) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(RABBITMQ_HOST);
    factory.setPort(RABBITMQ_PORT);
    factory.setUsername(RABBITMQ_USERNAME);
    factory.setPassword(RABBITMQ_PASSWORD);

    ChannelPool channelPool = new ChannelPool(POOL_SIZE, factory);
    executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    for (int i = 0; i < THREAD_COUNT; i++) {
      executorService.submit(new ConsumerThread(channelPool));
    }


    executorService.shutdown();
    while (!executorService.isTerminated()) {
      sleep(1000);
    }

    if (skierRidesMap.size() == TOTAL_NUM_MESSAGE) {
      channelPool.close();
    }

  }
}
