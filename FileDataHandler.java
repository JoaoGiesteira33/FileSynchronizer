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
            boolean flag; // Have we reached end of file
            int sequenceNumber = 0; // Order of sequences
            int foundLast = 0; // The las sequence found
            
            while (true) {
                byte[] message = new byte[261]; // Where the data from the received datagram is stored
                byte[] fileByteArray = new byte[256]; // Where we store the data to be writen to the file

                // Receive packet and retrieve the data
                DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
                socket.receive(receivedPacket);
                message = receivedPacket.getData(); // Data to be written to the file
                Message received_m = new Message(message);

                // Retrieve sequence number
                sequenceNumber = received_m.getPacketNumber();
                // Check if we reached last datagram (end of file) ATENCAO APARENTEMENTE PODE TER QUE SE MUDAR ISTO
                flag = received_m.isLastPacket();

                // If sequence number is the last seen + 1, then it is correct
                // We get the data from the message and write the ack that it has been received correctly
                if (sequenceNumber == (foundLast + 1)) {

                    // set the last sequence number to be the one we just received
                    foundLast = sequenceNumber;

                    // Data do ficheiro, apenas a partir do 5 byte é que começa
                    System.arraycopy(message, 5, fileByteArray, 0, 255);

                    // Write the retrieved data to the file and print received data sequence number
                    outToFile.write(fileByteArray);
                    LoggerUtil.getLogger().info("Received: Sequence number:" + foundLast);

                    // Send acknowledgement
                    Server.sendAck(foundLast, socket, ip, port);
                } else {
                    LoggerUtil.getLogger().warning("Expected sequence number: " + (foundLast + 1) + " but received " + sequenceNumber + ". DISCARDING");
                    // Re send the acknowledgement
                    Server.sendAck(foundLast, socket, ip, port);
                }
                // Check for last datagram
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