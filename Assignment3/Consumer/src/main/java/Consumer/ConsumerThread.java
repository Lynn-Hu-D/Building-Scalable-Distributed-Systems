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
import software.amazon.awssdk.services.dynamodb.endpoints.internal.Value.Int;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;

import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
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
    System.out.println("consuer starts/.....");
    try {
      channel = this.channelPool.borrowChannel();
      channel.queueDeclare(QUEUE_NAME, false, false, false, null);

      // Define callback for handling incoming messages
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        // Process the message (update HashMap)
        System.out.println("start process message..........");
        processMessage(message);
      };
      channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
    } catch( IOException e) {
    throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      System.out.println("go into finally  " + batch.size());
      if (!batch.isEmpty()) {
        System.out.println("flush the final batch.....  ");
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

    System.out.println("finish working/.....");
  }
  private void processMessage(String message) {
    System.out.println(" process..........");
    // Parse the message and update the skierRidesMap
    String[] parts = message.split(",");
    if (parts.length == 6) {
      int skierId = Integer.parseInt(parts[0]); // skierId
      int resortId = Integer.parseInt(parts[1]); // resortId
      int seasonId = Integer.parseInt(parts[2]); // seasonId
      int dayId = Integer.parseInt(parts[3]); // dayId
      int time = Integer.parseInt(parts[4]); // time
      int liftId = Integer.parseInt(parts[5]); // liftId

      int vertical = liftId * 10; // vertical

      // Composite keys
      String resortSeasonDayLiftId = resortId + "#" + seasonId + "#" + dayId +"#" + liftId;
      Map<String, AttributeValue> item = new HashMap<>();
      item.put("ResortSeasonDayLiftId", AttributeValue.builder().s(resortSeasonDayLiftId).build());
      item.put("SkierId", AttributeValue.builder().n(String.valueOf(skierId)).build());
      item.put("Vertical", AttributeValue.builder().n(String.valueOf(vertical)).build());
      item.put("Time", AttributeValue.builder().n(String.valueOf(time)).build());

      System.out.println("start updating ....");

      // update uniqueSkiers
      update(resortId + "#" + seasonId + "#" + dayId, -1, "ADD UniqueSkiers :val", 1);
      System.out.println("uniqueskiers success");

      // update total vertical
      // 1#2024 skierID totalVertical ->  get the total vertical for the skier for specified seasons at the specified resort
      if(!update(resortId + "#" + seasonId, skierId, "ADD TotalVertical :val", vertical)){
        writeTotalVertical(resortId+"#"+seasonId, skierId, vertical, "TotalVertical");
        System.out.println("totalvertical success");
      }

      // update day vertical
      if(!update(resortId + "#" + seasonId + "#" + dayId, skierId, "ADD DayVertical :val", vertical)){
        writeTotalVertical(resortId + "#" + seasonId + "#" + dayId, skierId, vertical, "DayVertical");
        System.out.println("dayvertical success");
      }

      // update vertical
      // 1#2024#1 skierId Vertical -> get the total vertical for the skier for the specified ski day
      if (!update(resortSeasonDayLiftId, skierId, "ADD Vertical :val", vertical)) {
        addToBatch(item);
        System.out.println("item is successfully added");
      }

    }
  }

  private void writeTotalVertical(String resortSeasonDayLiftId, int skierId, int vertical, String attributeName) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put("ResortSeasonDayLiftId", AttributeValue.builder().s(resortSeasonDayLiftId).build());
    item.put("SkierId", AttributeValue.builder().n(String.valueOf(skierId)).build());
    item.put(attributeName, AttributeValue.builder().n(String.valueOf(vertical)).build());

    System.out.println("////");
    PutItemRequest putItemRequest = PutItemRequest.builder()
        .tableName(TABLE_NAME) // The name of the table
        .item(item)
        .build();
    dynamoDbClient.putItem(putItemRequest);

    System.out.println(".....");
  }


  private boolean update(String partitionKey, int sortKey, String expression, int val) {
    Map<String, AttributeValue> key = new HashMap<>();
    key.put("ResortSeasonDayLiftId", AttributeValue.builder().s(partitionKey).build());
    key.put("SkierId", AttributeValue.builder().n(String.valueOf(sortKey)).build());

    System.out.println("1111");

    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(":val", AttributeValue.builder().n(String.valueOf(val)).build());

    System.out.println("2222");
    UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
        .tableName(TABLE_NAME) // Replace with your table name
        .key(key)
        .updateExpression(expression)
        .expressionAttributeValues(expressionAttributeValues)
        .conditionExpression("attribute_exists(ResortSeasonDayLiftId) and attribute_exists(SkierId)")
        .build();
    System.out.println("33333");
// Execute the update
    try {
      dynamoDbClient.updateItem(updateItemRequest);
      System.out.println("updated successfully.");
      return true;
    } catch (DynamoDbException e) {
      return false;
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

}
