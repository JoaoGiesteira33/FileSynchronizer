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
            LoggerUtil.getLogger().severe(e.getMessage());
        }
    }

    public void run(){
        try{
            //Antes de tudo enviar ACK para cliente do outro lado saber que queremos o ficheiro e começar a transferencia
            Server.sendAck(0, socket, this.ip, this.port);
            LoggerUtil.getLogger().info("Sent ACK | Receiving file");
            FileOutputStream outToFile = new FileOutputStream(this.f); 
            boolean flag; // Fim do ficheiro
            int sequenceNumber = 0; 
            int foundLast = 0;
            
            while (true) {
                byte[] message = new byte[261]; // Mensagem a receber, tamanho maximo
                byte[] fileByteArray = new byte[256]; // Data do ficheiro, tamanho maximo

                // Receber pacote
                DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
                socket.receive(receivedPacket);
                message = receivedPacket.getData(); // Data to be written to the file
                Message received_m = new Message(message);

                // Obter número de sequência para verificar
                sequenceNumber = received_m.getPacketNumber();
                // Verificar se chegamos ao fim do ficheiro
                flag = received_m.isLastPacket();

                // Está correto se numero de sequncia for mais 1 que o visto anteriormente
                if (sequenceNumber == (foundLast + 1)) {
                    // Atualizar último número de sequência recebido
                    foundLast = sequenceNumber;

                    // Data do ficheiro, apenas a partir do 5 byte é que começa
                    //MUDAR CÓPIA DO FICHEIRO FAZENDO USO DO TAMANHO
                    System.arraycopy(message, 5, fileByteArray, 0, 255);

                    // Escrever dados para o ficheiro
                    outToFile.write(fileByteArray);
                    LoggerUtil.getLogger().info("Received: Sequence number:" + foundLast);

                    // Enviar ACK
                    Server.sendAck(foundLast, socket, ip, port);
                } else {
                    LoggerUtil.getLogger().warning("Expected sequence number: " + (foundLast + 1) + " but received " + sequenceNumber + ". DISCARDING");
                    // Reenviar ack
                    Server.sendAck(foundLast, socket, ip, port);
                }
                // Se for último pacote do ficheiro podemos fechar
                if (flag) {
                    outToFile.close();
                    break;
                }
            }
        }catch(FileNotFoundException e){
            LoggerUtil.getLogger().severe(e.getMessage());
            //SEND ERROR PACKAGE
        } catch(IOException e){
            LoggerUtil.getLogger().warning(e.getMessage());
        }
    }
}