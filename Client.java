import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Client implements Runnable {
    List<InetAddress> ips;

    public Client(List<String> ipsString) {
        this.ips = new ArrayList<>();
        try{
            for(String ip : ipsString){
                this.ips.add(InetAddress.getByName(ip));
            }
        }catch(UnknownHostException e){
            LoggerUtil.getLogger().severe(e.getMessage());
        }
    }

    public void run() {
        try {
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            DatagramSocket clientSocket = new DatagramSocket(); // creates a door for the client process
            
            byte[] sendData = new byte[1024]; // hold send data the client receives
            byte[] receiveData = new byte[1024]; // hold received data the client receives

            String sentence;
            int counter = 1;

            while (!(sentence = inFromUser.readLine()).equals(".")) {
                sendData = sentence.getBytes();
                System.out.println("Packet " + counter + " was sent.");
                for(InetAddress i : this.ips){
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                    i, 8888);
                    clientSocket.send(sendPacket);
                }
                counter++;
            }

            inFromUser.close();
            System.out.println("Sending &&& to terminate!");
            sendData = "&&&".getBytes();

            for(InetAddress i : this.ips){
                DatagramPacket sendPacketEnd = new DatagramPacket(sendData, sendData.length,
                i, 8888);
                clientSocket.send(sendPacketEnd);
            }

            DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);
            clientSocket.receive(receivePacket);
            String modifiedSentence = new String(receivePacket.getData());
            System.out.println("FROM SERVER: "
                    + modifiedSentence);
            clientSocket.close();
        } catch (SocketException ex) {
            LoggerUtil.getLogger().severe(ex.getMessage());
        } catch (UnknownHostException ex) {
            LoggerUtil.getLogger().severe(ex.getMessage());
        } catch (IOException ex) {
            LoggerUtil.getLogger().severe(ex.getMessage());
        }
    }
}