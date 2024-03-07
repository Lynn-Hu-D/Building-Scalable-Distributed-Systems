package Consumer;

public class Config {
  protected static final String QUEUE_NAME = "liftRideQueue";
  protected static final String RABBITMQ_HOST = "44.235.243.252";
  //  private static final String RABBITMQ_HOST = "localhost";
  protected static final int RABBITMQ_PORT = 5672;
  protected static final String RABBITMQ_USERNAME = "guest";
  protected static final String RABBITMQ_PASSWORD = "guest";
  protected  static final int THREAD_COUNT = 500;
  protected static final int POOL_SIZE = 600;
  protected static final int TOTAL_NUM_MESSAGE = 200000;

}
