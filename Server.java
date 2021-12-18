import java.io.*;
import java.net.*;

public class Server implements Runnable{
    public void run(){
         try {
            DatagramSocket serverSocket = new DatagramSocket(8888);
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];
            while (true) {
                System.out.println("Ola 1");
                DatagramPacket receivePacket
                        = new DatagramPacket(receiveData,
                                receiveData.length);
                serverSocket.receive(receivePacket);
                
                String sentence = new String(
                        receivePacket.getData());
                
                if(receivePacket.getData() != "&&&".getBytes()){
                    System.out.println("Received DATA!!!");
                    System.out.println(receivePacket.getData().toString());
                }
                
                InetAddress ip
                        = receivePacket.getAddress();
                int port = receivePacket.getPort();
                String capitalizedSentence
                        = sentence.toUpperCase();
                sendData = capitalizedSentence.getBytes();
                DatagramPacket sendPacket
                        = new DatagramPacket(sendData,
                                sendData.length, ip, port);
                serverSocket.send(sendPacket);
                System.out.println("Ola 2");
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(e.getMessage());
        }
    }
}