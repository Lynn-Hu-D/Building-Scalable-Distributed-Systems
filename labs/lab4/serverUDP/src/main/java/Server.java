import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import jogamp.common.util.locks.SingletonInstanceServerSocket;

public class Server {
  private DatagramSocket datagramSocket;
  private byte[] buffer = new byte[256];


  public Server(DatagramSocket datagramSocket) {
    this.datagramSocket = datagramSocket;
  }

  public void receiveThenSend() throws IOException {
    while (true) {
      try {
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
        // blocking method: wait to receive message
        datagramSocket.receive(datagramPacket);
        InetAddress inetAddress = datagramPacket.getAddress();
        int port = datagramPacket.getPort();
        String messageFromClient = new String(datagramPacket.getData(), 0,
            datagramPacket.getLength());
        System.out.println("Message from client: " + messageFromClient);

        // send back to client
        datagramPacket = new DatagramPacket(buffer, buffer.length, inetAddress, port);
        datagramSocket.send(datagramPacket);

      } catch (IOException e) {
        e.printStackTrace();
        break;
      }
    }
  }

  public static void main (String[] args) throws IOException {
    DatagramSocket datagramSocket = new DatagramSocket(1234);
    Server server = new Server(datagramSocket);
    server.receiveThenSend();
  }
}
