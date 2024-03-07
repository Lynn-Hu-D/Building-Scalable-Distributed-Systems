import java.util.Random;
import io.swagger.client.model.LiftRide;

public class LiftDataGenerator {
  private static final int MAX_SKIER_ID = 100000;
  private static final int MAX_RESORT_ID = 10;
  private static final int MAX_LIFT_ID = 40;
  private static final String SEASON_ID = "2024";
  private static final String DAY_ID = "1";
  private static final int MAX_TIME = 360;


  private LiftRide liftRide;
  private Integer resortID;
  private String seasonID;
  private String dayID;
  private Integer skierID;

  private Random random;


  public LiftDataGenerator() {
     this.random = new Random();

    // liftRide
    LiftRide newRide = new LiftRide();
    newRide.setTime(random.nextInt(MAX_TIME) + 1);
    newRide.setLiftID(random.nextInt(MAX_LIFT_ID) + 1);
    setLiftRide(newRide);

    setResortID(random.nextInt(MAX_RESORT_ID) + 1);
    setSeasonID(SEASON_ID);
    setDayID(DAY_ID);
    setSkierID(random.nextInt(MAX_SKIER_ID) + 1);

  }


  public LiftRide getLiftRide() {
    return liftRide;
  }

  public void setLiftRide(LiftRide liftRide) {
    this.liftRide = liftRide;
  }

  public Integer getResortID() {
    return resortID;
  }

  public void setResortID(Integer resortID) {
    this.resortID = resortID;
  }

  public String getSeasonID() {
    return seasonID;
  }

  public void setSeasonID(String seasonID) {
    this.seasonID = seasonID;
  }

  public String getDayID() {
    return dayID;
  }

  public void setDayID(String dayID) {
    this.dayID = dayID;
  }

  public Integer getSkierID() {
    return skierID;
  }

  public void setSkierID(Integer skierID) {
    this.skierID = skierID;
  }


}
