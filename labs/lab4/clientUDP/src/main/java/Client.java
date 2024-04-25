import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
  private DatagramSocket datagramSocket;
  private InetAddress inetAddress;
  private byte[] buffer;

  public Client(DatagramSocket datagramSocket, InetAddress inetAddress) {
    this.datagramSocket = datagramSocket;
    this.inetAddress = inetAddress;
  }

  public void sendAndReceive() throws IOException {


    while (true) {

      try {
        for (int i = 0; i < 1000; i++) {
          String messageToSend = "Message " + i;
          buffer = messageToSend.getBytes();
          DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, inetAddress, 1234);

          // blocking: sending message & wait response
          datagramSocket.send(datagramPacket);

          // receive response
          datagramSocket.receive(datagramPacket);
          String messageFromServer = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
          System.out.println("The server says you said: " + messageFromServer);
      }
      } catch (IOException e) {
        e.printStackTrace();
        break;
      }

    }
  }

}
