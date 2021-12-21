import java.io.*;
import java.net.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server implements Runnable{
    List<Thread> ths = new ArrayList<>();

    public static void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        byte[] ack = new byte[4];
        Message ackMessage = new Message(3,foundLast);
        ack = ackMessage.getBytes();
        DatagramPacket acknowledgement = new DatagramPacket(ack, ack.length, address, port);
        socket.send(acknowledgement);
        LoggerUtil.getLogger().info("Sent ack: Sequence Number = " + foundLast);
    }

    public void run(){
         try {
             //Abrimos servidor na porta 8888
            DatagramSocket serverSocket = new DatagramSocket(8888);
            byte[] receiveData = new byte[128]; //Sabemos que vamos receber um pacote deste tamanho como primeira mensagem

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
                Byte file_sizeB = received_m.getData()[0];
                int file_size = file_sizeB.intValue();
                System.out.println("GOING TO PRINT DATA-----------------------");
                received_m.printData();
                //UNICA COISA Q PODE ESTAR A FALHAR, TANTO QUANTO SABEMOS
                String file_path =  new String(Arrays.copyOfRange(received_m.getData(),1,file_size+1));
                System.out.println("FILE PATH SIZE: " + file_path.length());                
                System.out.println("FILE PATH: " + file_path);

                LoggerUtil.getLogger().info("Write Request for new file: " + file_path);

                if(!Main.hasFile(file_path)){ //Computador do Servidor não tem o ficheiro recebido
                    LoggerUtil.getLogger().info("Request Accepted for file: " + file_path);
                    
                    File f = new File(Main.changeFilePath(file_path));
                    try{                     
                        f.createNewFile();
                        Main.addFile(f);
                    }
                    catch(IOException e){
                        //Enviar pacote de erro a criar ficheiro
                        LoggerUtil.getLogger().warning("Erro a criar ficheiro");
                        byte[] error = new byte[]{Integer.valueOf(4).byteValue(),Integer.valueOf(2).byteValue()};
                        DatagramPacket errorPacket = new DatagramPacket(error, error.length,receivePacket.getAddress(),receivePacket.getPort());
                        serverSocket.send(errorPacket);
                        break;
                    }

                    //Depois de criar ficheiro abrir thread para fazer transferência do mesmo
                    FileDataHandler fdh = new FileDataHandler(f,receivePacket.getAddress(),receivePacket.getPort());
                    Thread t = new Thread(fdh);
                    ths.add(t);
                    t.start();
                }
                else{
                    LoggerUtil.getLogger().info("Request Declined for file: " + file_path);
                    //Enviar pacote de erro de ficheiro já existente
                    byte[] errorAlreadyExists = new byte[]{Integer.valueOf(4).byteValue(),Integer.valueOf(1).byteValue()};
                    DatagramPacket errorAlreadyExistsPacket = new DatagramPacket(errorAlreadyExists, errorAlreadyExists.length,receivePacket.getAddress(),receivePacket.getPort());
                    serverSocket.send(errorAlreadyExistsPacket);
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