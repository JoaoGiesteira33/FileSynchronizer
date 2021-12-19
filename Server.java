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
                DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                serverSocket.receive(receivePacket);
                
                Message received_m = new Message(receivePacket.getData());

                //Servidor confirma se mensagem é do tipo 1
                if(received_m.getType() != 1){
                   LoggerUtil.getLogger().warning("Mensagem nao reconhecida! Tipo: " + received_m.getType());
                   break;
                }
                //Servidor verificar se tem o ficheiro em memoria
                String file_path = new String(received_m.getData());
                LoggerUtil.getLogger().info("Write Request for new file: " + file_path);
                if(Main.hasFile(file_path)){
                    LoggerUtil.getLogger().info("Request Accepted for file: " + file_path);
                }
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