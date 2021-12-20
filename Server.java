import java.io.*;
import java.net.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server implements Runnable{
    List<Thread> ths = new ArrayList<>();

    private static void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // send acknowledgement
        byte[] ack = new byte[4];
        Message ackMessage = new Message(3,foundLast);
        ack = ackMessage.getBytes();
        // the datagram packet to be sent
        DatagramPacket acknowledgement = new DatagramPacket(ack, ack.length, address, port);
        socket.send(acknowledgement);
        LoggerUtil.getLogger().info("Sent ack: Sequence Number = " + foundLast);
    }

    public void run(){
         try {
            DatagramSocket serverSocket = new DatagramSocket(8888);
            byte[] receiveData = new byte[1024];

            while (true) {
                //Servidor recebe nome de um ficheiro de algum cliente
                DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                serverSocket.receive(receivePacket);
                Message received_m = new Message(receivePacket.getData());
                LoggerUtil.getLogger().info("Pacote Recebido | IP: " + receivePacket.getAddress() + " | Port: " + receivePacket.getPort());
                //Servidor confirma se mensagem é do tipo 1
                if(received_m.getType() != 1){
                   LoggerUtil.getLogger().warning("Mensagem nao reconhecida! Tipo: " + received_m.getType());
                   break;
                }
                //Servidor verificar se tem o ficheiro em memoria
                String file_path = new String(received_m.getData());
                LoggerUtil.getLogger().info("Write Request for new file: " + file_path);

                if(!Main.hasFile(file_path)){
                    LoggerUtil.getLogger().info("Request Accepted for file: " + file_path);
                    File f = new File(file_path);
                    try{                     
                        f.createNewFile();
                        Main.addFile(f);
                    }
                    catch(IOException e){
                        LoggerUtil.getLogger().warning("Erro a criar ficheiro");
                        //Enviar pacote com erro a criar ficheiro
                        break;
                    }
                    //Depois de criar ficheiro agora sim abrir o handler com nova thread para transferencia do ficheiro
                    FileDataHandler fdh = new FileDataHandler(f,receivePacket.getAddress(),receivePacket.getPort());
                    Thread t = new Thread(fdh);
                    ths.add(t);
                    t.start();
                }
                else{
                    LoggerUtil.getLogger().info("Request Declined for file: " + file_path);
                    //Responder com Erro tipo 1
                }
            }
            for(Thread t : this.ths){
                try{
                    t.join();
                }
                catch(InterruptedException e){
                    LoggerUtil.getLogger().severe(e.getMessage());
                }
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(e.getMessage());
        }
    }
}