import java.io.*;
import java.net.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server implements Runnable{
    List<Thread> ths = new ArrayList<>();

    public static void sendWantFile(DatagramSocket socket, InetAddress adress, int port) throws IOException{
        byte[] wantFile = new byte[]{Integer.valueOf(5).byteValue()};
        DatagramPacket wantFilePacket = new DatagramPacket(wantFile, wantFile.length, adress, port);
        socket.send(wantFilePacket);
        LoggerUtil.getLogger().info("S || Enviamos confirmacao de que queremo ficheiro a " + adress + " pela porta " + port);
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
                LoggerUtil.getLogger().info("S || Pacote Recebido | IP: " + receivePacket.getAddress() + " | Port: " + receivePacket.getPort());
                
                //Servidor confirma se mensagem é do tipo 1
                if(received_m.getType() != 1){
                   LoggerUtil.getLogger().warning("S || Mensagem nao reconhecida! Tipo: " + received_m.getType());
                   break;
                }

                //Servidor verificar se tem o ficheiro em memoria
                Byte file_sizeB = received_m.getData()[0];
                int file_size = file_sizeB.intValue();
                String file_path =  new String(Arrays.copyOfRange(received_m.getData(),1,file_size+1));

                LoggerUtil.getLogger().info("S || Questionado se queremos o seguinte ficheiro: " + file_path);

                if(!Main.hasFile(file_path)){ //Computador do Servidor não tem o ficheiro recebido
                    LoggerUtil.getLogger().info("S || Queremos o ficheiro: " + file_path);
                    
                    File f = new File(Main.changeFilePath(file_path));
                    try{                     
                        try{
                            f.getParentFile().mkdirs(); //Caso tenha subpastas até chegar ao ficheiro
                        }
                        catch(NullPointerException e)
                        {
                        }
                        f.createNewFile(); //Criamos o ficheiro em memória
                        Main.addFile(f);
                    }
                    catch(IOException e){
                        //Enviar pacote de erro a criar ficheiro
                        LoggerUtil.getLogger().warning("S || Erro a criar ficheiro");
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
                    LoggerUtil.getLogger().info("S || Nao queremos o ficheiro: " + file_path);
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
                    LoggerUtil.getLogger().severe("S || " + e.getMessage());
                }
            }
            serverSocket.close();
        } catch (Exception e) {
            LoggerUtil.getLogger().severe("S || " + e.getMessage());
        }
    }
}