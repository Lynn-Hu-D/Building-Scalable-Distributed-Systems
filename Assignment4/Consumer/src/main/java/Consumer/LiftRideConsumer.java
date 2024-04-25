package Consumer;

import static Consumer.Config.POOL_SIZE;
import static Consumer.Config.RABBITMQ_HOST;
import static Consumer.Config.RABBITMQ_PASSWORD;
import static Consumer.Config.RABBITMQ_PORT;
import static Consumer.Config.RABBITMQ_USERNAME;
import static Consumer.Config.THREAD_COUNT;
import static Consumer.DBConnection.TABLE_NAME;
import static Consumer.DBConnection.dynamoDbClient;

import com.rabbitmq.client.ConnectionFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;


public class LiftRideConsumer {
  private  static ExecutorService executorService;
  protected static final int MAX_RETRIES = 5;


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
  }


}


