import java.io.*;
import java.net.*;

public class Server implements Runnable{
    public void run(){
         try {
            DatagramSocket serverSocket = new DatagramSocket(8888);
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];
            while (true) {
                DatagramPacket receivePacket
                        = new DatagramPacket(receiveData,
                                receiveData.length);
                serverSocket.receive(receivePacket);
                
                String sentence = new String(
                        receivePacket.getData());
                
                if(receivePacket.getData() != "&&&".getBytes())
                    System.out.println(receivePacket.getData());
                
                InetAddress ip
                        = receivePacket.getAddress();
                int port = receivePacket.getPort();
                String capitalizedSentence
                        = sentence.toUpperCase();
                sendData = sentence.getBytes();
                DatagramPacket sendPacket
                        = new DatagramPacket(sendData,
                                sendData.length, ip, port);
                serverSocket.send(sendPacket);
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(e.getMessage());
        }
    }
}