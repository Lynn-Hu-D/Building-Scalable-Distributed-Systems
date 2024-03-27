package Consumer;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public class DBConnection {
  protected static  DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
      .region(Region.US_WEST_2) // Specify your AWS Region here
      .build();
  protected static  final String TABLE_NAME = "SkiersData";

public static void main(String[] args) {
  createTable();
}

  public static void createTable() {
    CreateTableRequest request = CreateTableRequest.builder()
        .tableName(TABLE_NAME)
        .keySchema(
            KeySchemaElement.builder()
                .attributeName("SkierSeasonId")
                .keyType(KeyType.HASH) // Partition key
                .build(),
            KeySchemaElement.builder()
                .attributeName("DayLiftTime") // Sort key
                .keyType(KeyType.RANGE)
                .build())
        .attributeDefinitions(
            AttributeDefinition.builder()
                .attributeName("SkierSeasonId")
                .attributeType(ScalarAttributeType.S)
                .build(),
            AttributeDefinition.builder()
                .attributeName("DayLiftTime")
                .attributeType(ScalarAttributeType.S)
                .build(),
            AttributeDefinition.builder()
                .attributeName("ResortId")
                .attributeType(ScalarAttributeType.N)
                .build(),
            // Removed Time and LiftId from here as they are not used in any key
            AttributeDefinition.builder()
                .attributeName("DaySkier") // Assuming it's a string based on concatenation
                .attributeType(ScalarAttributeType.S)
                .build())
        .globalSecondaryIndexes(
            GlobalSecondaryIndex.builder()
                .indexName("ResortDayIndex")
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName("ResortId")
                        .keyType(KeyType.HASH)
                        .build(),
                    KeySchemaElement.builder()
                        .attributeName("DaySkier")
                        .keyType(KeyType.RANGE)
                        .build())
                .projection(Projection.builder()
                    .projectionType(ProjectionType.ALL)
                    .build())
                .build())
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .build();


    try {
      dynamoDbClient.createTable(request);
      System.out.println("DBConnection created successfully: " + TABLE_NAME);
    } catch (DynamoDbException e) {
      System.err.println("DBConnection creation failed: " + e.getMessage());
    }
  }
}

