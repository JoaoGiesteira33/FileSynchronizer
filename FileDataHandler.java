import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class FileDataHandler implements Runnable{
    private File f;
    private InetAddress ip; //Do cliente de onde estamos a receber o ficheiro
    private int port; //Do cliente de onde estamos a receber o ficheiro
    private DatagramSocket socket;

    public FileDataHandler(File f, InetAddress ip, int port){
        this.f = f;
        this.ip = ip;
        this.port = port;
        try{
            this.socket = new DatagramSocket();
        }
        catch(SocketException e){
            LoggerUtil.getLogger().severe(" S || " + e.getMessage());
        }
    }

    private void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        byte[] ack = new byte[4];
        Message ackMessage = new Message(3,foundLast);
        ack = ackMessage.getBytes();
        DatagramPacket acknowledgement = new DatagramPacket(ack, ack.length, address, port);
        socket.send(acknowledgement);
        LoggerUtil.getLogger().info("S || Enviar ACK a " + address + ", sequence number: " + foundLast);
    }

    public void run(){
        try{
            //Confirmamos ao Cliente que queremos o ficheiro
            Server.sendWantFile(socket, this.ip, this.port);
            FileOutputStream outToFile = new FileOutputStream(this.f); 
            boolean flag; // Fim do ficheiro
            int sequenceNumber = 0; 
            int foundLast = 0;
            
            while (true) {
                byte[] message = new byte[2054]; // Mensagem a receber, tamanho maximo
            
                // Receber pacote
                DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
                socket.receive(receivedPacket);
                message = receivedPacket.getData();
                Message received_m = new Message(message);

                // Obter n??mero de sequ??ncia para verificar
                sequenceNumber = received_m.getPacketNumber();

                // Verificar se chegamos ao fim do ficheiro
                flag = received_m.isLastPacket();

                // Est?? correto se numero de sequ??ncia for mais 1 que o visto anteriormente
                if (sequenceNumber == (foundLast + 1)) {
                    // Atualizar ??ltimo n??mero de sequ??ncia recebido
                    foundLast = sequenceNumber;

                    // Data do ficheiro, apenas a partir do 6 byte ?? que come??a
                    int fileDataSize = received_m.fileDataSize();
                    byte[] fileByteArray = new byte[fileDataSize];
                    System.arraycopy(message, 6, fileByteArray, 0, fileDataSize);
                    
                    // Escrever dados para o ficheiro
                    outToFile.write(fileByteArray);
                    LoggerUtil.getLogger().info("S || Recebemos sequence number: " + foundLast);

                    // Enviar ACK
                    this.sendAck(foundLast, socket, receivedPacket.getAddress(), receivedPacket.getPort());
                } else {
                    LoggerUtil.getLogger().warning("S || A espera de sequence number: " + (foundLast + 1) + " mas recebemos " + sequenceNumber + ". DISCARDING");
                    // Reenviar ack
                    this.sendAck(foundLast, socket, receivedPacket.getAddress(), receivedPacket.getPort());
                }
                // Se for ??ltimo pacote do ficheiro podemos fechar
                if (flag) {
                    outToFile.close();
                    this.socket.close();
                    break;
                }
            }
        }catch(FileNotFoundException e){ //Erro a escrever no ficheiro
            LoggerUtil.getLogger().severe("S || " + e.getMessage());
        } catch(IOException e){
            LoggerUtil.getLogger().warning("S || " + e.getMessage());
        }
    }
}