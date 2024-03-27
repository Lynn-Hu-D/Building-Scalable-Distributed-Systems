package Consumer;

import static Consumer.Config.QUEUE_NAME;
import static Consumer.DBConnection.TABLE_NAME;
import static Consumer.DBConnection.dynamoDbClient;
import static Consumer.LiftRideConsumer.MAX_RETRIES;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;


public class ConsumerThread implements Runnable {
  private Channel channel;
  private ChannelPool channelPool;
  private  final int BATCH_SIZE = 25; // DynamoDB max batch size
  private List<WriteRequest> batch = new ArrayList<>();

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
      if (!batch.isEmpty()) {
        flushBatch();
      }
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
    if (parts.length == 6) {
      int skierId = Integer.parseInt(parts[0]); // skierId
      int resortId = Integer.parseInt(parts[1]); // resortId
      int seasonId = Integer.parseInt(parts[2]); // seasonId
      int dayId = Integer.parseInt(parts[3]); // dayId
      int time = Integer.parseInt(parts[4]); // time
      int liftId = Integer.parseInt(parts[5]); // liftId

      // Composite keys
      String skierSeasonId = skierId + "#" + seasonId;
      String dayLiftTime = dayId + "#" + liftId + "#" + time;
      String daySkierId= dayId + "#" + skierId;

      Map<String, AttributeValue> item = new HashMap<>();
      item.put("SkierSeasonId", AttributeValue.builder().s(skierSeasonId).build());
      item.put("DayLiftTime", AttributeValue.builder().s(dayLiftTime).build());
      item.put("ResortId", AttributeValue.builder().n(String.valueOf(resortId)).build());
      item.put("DaySkier", AttributeValue.builder().s(daySkierId).build());

      addToBatch(item);
      System.out.println("item is successfully added");
    }
  }

  private synchronized void addToBatch(Map<String, AttributeValue> item) {
    batch.add(WriteRequest.builder().putRequest(builder -> builder.item(item)).build());
    System.out.println(batch.size());
    if (batch.size() >= BATCH_SIZE) {
      System.out.println("batch size: " + batch.size());
      flushBatch();
    }
  }

  private void flushBatch() {
    if (batch.isEmpty()) {
      return;
    }

    int retryCount = 0;
    Map<String, List<WriteRequest>> unprocessedItems = Map.of(TABLE_NAME, new ArrayList<>(batch));

    do {
      try {
        BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
            .requestItems(unprocessedItems)
            .build();

        var response = dynamoDbClient.batchWriteItem(batchWriteItemRequest);
        unprocessedItems = response.unprocessedItems();

        if (unprocessedItems.isEmpty() || unprocessedItems.get(TABLE_NAME).isEmpty()) {
          System.out.println("Successfully wrote batch to table " + TABLE_NAME);
          break; // Break if all items are processed
        }

        // Prepare the list of unprocessed items for the next retry
        retryCount++;
        System.out.println("Retrying unprocessed items. Attempt #" + retryCount);
        Thread.sleep((long) (Math.pow(2, retryCount) * 100L)); // Exponential backoff

      } catch (DynamoDbException | InterruptedException e) {
        System.err.println("Error during batch write, retry " + retryCount + ": " + e.getMessage());
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
          break; // Exit if the thread was interrupted
        }
        retryCount++;
      }
    } while (retryCount <= MAX_RETRIES);

    if (retryCount > MAX_RETRIES) {
      System.err.println("Failed to write batch after " + MAX_RETRIES + " retries.");
      // Consider further actions for unprocessed items, such as logging or moving to a dead-letter queue
    }

    batch.clear(); // Clear the initial batch after all attempts
  }


//  private void flushBatch() {
//    if (!batch.isEmpty()) {
//      final int MAX_RETRIES = 5;
//      int retryCount = 0;
//      BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
//          .requestItems(Map.of(TABLE_NAME, new ArrayList<>(batch)))
//          .build();
//      Map<String, List<WriteRequest>> unprocessedItems = null;
//
//      do {
//        try {
//          var response = retryCount == 0 ? dynamoDbClient.batchWriteItem(batchWriteItemRequest) :
//              dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder().requestItems(unprocessedItems).build());
//          unprocessedItems = response.unprocessedItems();
//
//          if (!unprocessedItems.isEmpty()) {
//            retryCount++;
//            // Exponential backoff
//            Thread.sleep((long) (Math.pow(2, retryCount) * 100L));
//          } else {
//            System.out.println("Successfully wrote batch to table " + TABLE_NAME);
//            return;
//          }
//        } catch (DynamoDbException | InterruptedException e) {
//          System.err.println("Error during batch write, retry " + retryCount + ": " + e.getMessage());
//          retryCount++;
//          // Handle InterruptedException for the sleep method
//          Thread.currentThread().interrupt();
//        }
//      } while (retryCount <= MAX_RETRIES);
//
//      // Handle the case where retries exceeded the maximum allowed attempts
//      System.err.println("Failed to write batch after " + MAX_RETRIES + " retries. Consider further actions like logging or moving to a dead-letter queue.");
//    }
//      batch.clear();
//    }
}
