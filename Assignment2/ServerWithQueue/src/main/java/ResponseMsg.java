public class ResponseMsg {
  private String message;

  // Default constructor for Jackson serialization
  public ResponseMsg() {
  }


  public ResponseMsg(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

}
