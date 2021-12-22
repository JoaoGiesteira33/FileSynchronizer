import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class FileDataSender implements Runnable{
    private DatagramSocket socket;
    private byte[] fileByteArray;
    private InetAddress address;
    private int port;

    public FileDataSender(byte[] fileByteArray, InetAddress address, int port){
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            LoggerUtil.getLogger().warning("C || " + e.getMessage());
        }
        this.fileByteArray = fileByteArray;
        this.address = address;
        this.port = port;
    }

    public void run(){
        try{
        byte[] sendData = new byte[260]; //Tamanho máximo de um pacote
        LoggerUtil.getLogger().info("Sending file");
        int sequenceNumber = 0; // Para ordenar envio de pacotes
        int ackSequence = 0; // Verificar se o pacote foi enviado corretamente
        boolean flag; //Ultimo pacote

        for (int i = 0; i < fileByteArray.length; i += 255) {
            sequenceNumber++;

            // Cria uma mensagem, que muda se o ficheiro já chegou ao fim
            Message m;
            if ((i + 255) >= fileByteArray.length) { // Chegamos ao fim do ficheiro
                flag = true;
            } else { //Ainda não chegamos ao fim 
                flag = false;
            }
            if(flag){
                m = new Message(2, sequenceNumber, fileByteArray.length - i, Arrays.copyOfRange(fileByteArray,i,i+254));
            }
            else{
                m = new Message(2,sequenceNumber,255, Arrays.copyOfRange(fileByteArray,i,i+254));
            }

            //Enviar pacote com parte do ficheiro
            sendData = m.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);
            LoggerUtil.getLogger().info("C || Sent: Sequence number = " + sequenceNumber);
            boolean ackRec; // Recebemos um ack

            while (true) {
                byte[] ack = new byte[4]; // Receber ACK (TIPO 3)
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

                try {
                    socket.setSoTimeout(50); // Esperar que o servidor envie um ACK
                    socket.receive(ackpack);
                    Message received_m = new Message(ackpack.getData());
                    ackSequence = received_m.getPacketNumber(); //Número de pacote no ACK
                    ackRec = true; // Recebemos um ACK
                } catch (SocketTimeoutException e) {
                    System.out.println("C || Socket timed out waiting for ack");
                    ackRec = false; // Não recebemos um ACK
                }

                // Recebemos o ACK correto, podemos enviar próxima parte do ficheiro
                if ((ackSequence == sequenceNumber) && (ackRec)) {
                    LoggerUtil.getLogger().info("C || Ack received: Sequence Number = " + ackSequence);
                    break;
                } // Pacote não foi recebido, por isso reenviamos
                else {
                    socket.send(sendPacket);
                    LoggerUtil.getLogger().warning("C || Resending: Sequence Number = " + sequenceNumber);
                }
            }
        }
    }catch(Exception e){
        LoggerUtil.getLogger().severe("C || " + e.getMessage());
    }
}
}