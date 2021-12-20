import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client implements Runnable {
    List<File> files;
    List<InetAddress> ips;

    public Client(List<File> files, List<String> ipsString) {
        this.files = new ArrayList<>(files);
        this.ips = new ArrayList<>();
        try{
            for(String ip : ipsString){
                this.ips.add(InetAddress.getByName(ip));
            }
        }catch(UnknownHostException e){
            LoggerUtil.getLogger().severe(e.getMessage());
        }
    }

    private static byte[] readFileToByteArray(File file) {
        FileInputStream fis = null;
        byte[] bArray = new byte[(int) file.length()];
        try {
            fis = new FileInputStream(file);
            fis.read(bArray);
            fis.close();
        } catch (IOException ioExp) {
            LoggerUtil.getLogger().severe(ioExp.getMessage());
        }
        return bArray;
    }

    private void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int port) throws IOException {
        byte[] sendData = new byte[260];
        System.out.println("Sending file");
        int sequenceNumber = 0; // For order
        boolean flag; // To see if we got to the end of the file
        int ackSequence = 0; // To see if the datagram was received correctly

        for (int i = 0; i < fileByteArray.length; i = i + 256) {
            sequenceNumber += 1;

            // Create message
            Message m = new Message(2,sequenceNumber,Arrays.copyOfRange(fileByteArray,i,i+255));


            if ((i + 256) >= fileByteArray.length) { // Have we reached the end of file?
                flag = true;
            } else {
                flag = false;
            }

            sendData = m.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port); // The data to be sent
            socket.send(sendPacket); // Sending the data
            LoggerUtil.getLogger().info("Sent: Sequence number = " + sequenceNumber);
            boolean ackRec; // Was the datagram received?

            while (true) {
                byte[] ack = new byte[4]; // Create another packet for datagram ackknowledgement
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

                try {
                    socket.setSoTimeout(50); // Waiting for the server to send the ack
                    socket.receive(ackpack);
                    Message received_m = new Message(ackpack.getData());
                    ackSequence = received_m.getPacketNumber(); // Figuring the sequence number
                    ackRec = true; // We received the ack
                } catch (SocketTimeoutException e) {
                    System.out.println("Socket timed out waiting for ack");
                    ackRec = false; // We did not receive an ack
                }

                // If the package was received correctly next packet can be sent
                if ((ackSequence == sequenceNumber) && (ackRec)) {
                    LoggerUtil.getLogger().info("Ack received: Sequence Number = " + ackSequence);
                    break;
                } // Package was not received, so we resend it
                else {
                    socket.send(sendPacket);
                    LoggerUtil.getLogger().warning("Resending: Sequence Number = " + sequenceNumber);
                }
            }
        }
    }

    public void run() {
        try {
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            DatagramSocket clientSocket = new DatagramSocket(); // creates a door for the client process
            
            byte[] sendData = new byte[1024]; // hold send data the client receives
            byte[] receiveData = new byte[1024]; // hold received data the client receives

            String sentence;
            int counter = 1;

            //Enviar ficheiros deste computador, um a um, para todos os ips
            //Se os pcs a receber estes ficheiros nao o tiverem vao pedir transferencia

            for(File f : this.files){
                String file_path = f.getPath();
                Message send_m = new Message(1,file_path.getBytes());
                sendData = send_m.getBytes();
                for(InetAddress i : this.ips){
                    //Envio da mensagem
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, i, 8888);
                    clientSocket.send(sendPacket);
                    LoggerUtil.getLogger().info("Pacote enviado | IP: " + i + " | Port: 8888 | File:" + file_path);
                    //Esperar por resposta
                    //Podemos ter que mexer nos setSoTimeout em situacoes como esta !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);
                    clientSocket.receive(receivePacket);
                    Message receive_m = new Message(receivePacket.getData());
                    //Confirmar se resposta é afirmativa/negativa
                    if(receive_m.getType() == 3){ //IF ACK
                        LoggerUtil.getLogger().info("Iniciar transferencia do ficheiro " + file_path);
                        byte[] fileByteArray = readFileToByteArray(f); // Array de bytes do ficheiro
                        // Envio do ficheiro
                        sendFile(clientSocket, fileByteArray, receivePacket.getAddress(), receivePacket.getPort()); 
                    }
                    else if(receive_m.getType() == 4) //IF ERR
                    {

                    }
                }
            }
            clientSocket.close();
        } catch (SocketException ex) {
            LoggerUtil.getLogger().severe(ex.getMessage());
        } catch (UnknownHostException ex) {
            LoggerUtil.getLogger().severe(ex.getMessage());
        } catch (IOException ex) {
            LoggerUtil.getLogger().severe(ex.getMessage());
        }
    }
}