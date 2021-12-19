import java.io.*;
import java.net.*;
import java.sql.Array;
import java.util.Arrays;

public class Server implements Runnable{
    public void run(){
         try {
            DatagramSocket serverSocket = new DatagramSocket(8888);
            byte[] receiveData = new byte[1024];

            while (true) {
                //Servidor recebe nome de um ficheiro de algum cliente
                DatagramPacket receivePacket
                        = new DatagramPacket(receiveData,
                                receiveData.length);
                serverSocket.receive(receivePacket);
                
                Message received_m = new Message(receivePacket.getData());

                //Servidor verificar se tem o ficheiro em memoria

                //FFSync.hasFile(fileName) ou algo do genero
                //Pode ser uma classe estatica com tudo estatico

                //if(queremosficheiro)
                //FileDataHandler fdh = new fdh(aquiVaiOFile,receivePacket.getAddress(),receivePacket.getPort());
                //Thread t = new Thread(fdh);
                //t.start();
                //else
                //responder q n queremos ficheiro

                /*
                String sentence = new String(
                        receivePacket.getData());
                
                if(receivePacket.getData() != "&&&".getBytes()){
                    System.out.println("Received DATA!!!");
                    System.out.println(receivePacket.getData());
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
                */
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(e.getMessage());
        }
    }
}