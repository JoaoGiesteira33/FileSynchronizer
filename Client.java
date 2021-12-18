import java.io.*;
import java.net.*;

public class Client implements Runnable {
    String ipA;

    public Client(String ipA) {
        this.ipA = ipA;
    }

    public void run() {
        try {
            System.out.println("Ola 1.1");
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            DatagramSocket clientSocket = new DatagramSocket(); // creates a door for the client process
            InetAddress ip = InetAddress.getByName(ipA); // address of destination

            byte[] sendData = new byte[1024]; // hold send data the client receives
            byte[] receiveData = new byte[1024]; // hold received data the client receives

            String sentence;
            int counter = 1;

            while ((sentence = inFromUser.readLine()) != null) {
                sendData = sentence.getBytes();
                System.out.println("Packet " + counter + " was sent.");
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                        ip, 8989);
                clientSocket.send(sendPacket);
                counter++;
            }

            inFromUser.close();
            sendData = "&&&".getBytes();

            DatagramPacket sendPacketEnd = new DatagramPacket(sendData, sendData.length,
                    ip, 8989);
            clientSocket.send(sendPacketEnd);

            DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);
            clientSocket.receive(receivePacket);
            String modifiedSentence = new String(receivePacket.getData());
            System.out.println("FROM SERVER: "
                    + modifiedSentence);
            clientSocket.close();
            System.out.println("Ola 2.1");
        } catch (SocketException ex) {
            LoggerUtil.getLogger().severe(ex.getMessage());
        } catch (UnknownHostException ex) {
            LoggerUtil.getLogger().severe(ex.getMessage());
        } catch (IOException ex) {
            LoggerUtil.getLogger().severe(ex.getMessage());
        }
    }
}