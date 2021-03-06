import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client implements Runnable {
    private List<InetAddress> ips;

    public Client(List<String> ipsString) {
        this.ips = new ArrayList<>();
        try{
            for(String ip : ipsString){
                this.ips.add(InetAddress.getByName(ip));
            }
        }catch(UnknownHostException e){
            LoggerUtil.getLogger().severe(e.getMessage());
        }
    }

    //Transforma ficheiro em array de bytes para fazer a transferência
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

    public void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int port){
        try{
            System.out.println("Starting new transfer");
            //Informação para tempo de transferência e débito final
            long startTime = System.nanoTime();
            long totalUpload = fileByteArray.length * 8; //bits

        byte[] sendData = new byte[2054]; //Tamanho máximo de um pacote
        LoggerUtil.getLogger().info("Sending file");
        int sequenceNumber = 0; // Para ordenar envio de pacotes
        int ackSequence = 0; // Verificar se o pacote foi enviado corretamente
        boolean flag; //Ultimo pacote

        for (int i = 0; i < fileByteArray.length; i += 2048) {
            sequenceNumber++;

            // Cria uma mensagem, que muda se o ficheiro já chegou ao fim
            Message m;
            if ((i + 2048) >= fileByteArray.length) { // Chegamos ao fim do ficheiro
                flag = true;
            } else { //Ainda não chegamos ao fim 
                flag = false;
            }
            if(flag){
                m = new Message(2, sequenceNumber, fileByteArray.length - i, Arrays.copyOfRange(fileByteArray,i,i+2047));
            }
            else{
                m = new Message(2,sequenceNumber,2048, Arrays.copyOfRange(fileByteArray,i,i+2047));
            }

            //Enviar pacote com parte do ficheiro
            sendData = m.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);
            LoggerUtil.getLogger().info("C || Enviado sequence number = " + sequenceNumber);
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
                    LoggerUtil.getLogger().info("C || Ack recebido, sequence number = " + ackSequence);
                    break;
                } else { //Pacote não recebido, por isso reenviamos
                    socket.send(sendPacket);
                    LoggerUtil.getLogger().warning("C || Reenviando, sequence number = " + sequenceNumber);
                }
            }
        }
        long transferTime = ((System.nanoTime() - startTime) / 1000000000);
        float bitsPerSec = 0;
        if(transferTime != 0){
            bitsPerSec = (float)totalUpload / transferTime;
        }

        System.out.println("C || bps: " + bitsPerSec);
        System.out.println("C || Time of transfer: " + transferTime + " secs");
    }catch(Exception e){
        LoggerUtil.getLogger().severe("C || " + e.getMessage());
    }
    }

    public void run() {
        try{
            DatagramSocket clientSocket = new DatagramSocket();
            byte[] sendData = new byte[128]; // Data a enviar
            
            while(true){
                for(File f : Main.filesToSync){
                
                //Criação de uma mensagem com o nome do ficheiro (TIPO 1)
                String file_path = f.getPath();
                Message send_m = new Message(1,f.getPath().length(),file_path.getBytes(),Main.password.length(),Main.password.getBytes());
                sendData = send_m.getBytes();

                for(InetAddress i : this.ips){
                    
                    //Envio da mensagem para todos os ips conhecidos
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, i, 8888);
                    clientSocket.send(sendPacket);
                    LoggerUtil.getLogger().info("C || Pacote enviado | IP: " + i + " | Port: 8888 | File:" + file_path);
                    boolean gotAnswer;
                    Message answerMessage = new Message(4,1); // Inicializada como Erro (ficheiro não desejado)
                    InetAddress answerIP;
                    int answerPort;

                    //Verificão de mensagem recebida
                    while (true) {
                        byte[] answer = new byte[4]; //Mensagem ou vai ter 4 bytes (TIPO 3) ou 2 bytes (TIPO 4)
                        DatagramPacket answerPacket = new DatagramPacket(answer, answer.length);
                        try {
                            clientSocket.setSoTimeout(50); //Esperar por resposta do servidor
                            clientSocket.receive(answerPacket);
                            answerMessage = new Message(answerPacket.getData()); //Criação de mensagem com data obtida
                            gotAnswer = true;
                        } catch (SocketTimeoutException e) {
                            LoggerUtil.getLogger().info("C || Socket timed out a espera da resposta");
                            gotAnswer = false; // Não recebemos ACK
                        }
        
                        // Pacote foi recebido e servidor confirmou
                        if (gotAnswer){
                            answerIP = answerPacket.getAddress();
                            answerPort = answerPacket.getPort();
                            LoggerUtil.getLogger().info("C || Resposta recebida, verificando...");
                            break;
                        } // Pacote não foi recebido, vamos reenviar
                        else {
                            clientSocket.send(sendPacket);
                            LoggerUtil.getLogger().warning("C || Pacote nao recebido, reenviando: " + f.getPath());
                        }
                    }

                    //Verificar tipo de resposta do servidor
                    if(answerMessage.getType() == 5){ //Servidor deseja ficheiro
                        LoggerUtil.getLogger().info("C || Resposta positiva, iniciar transferencia de: " + file_path);
                        byte[] fileByteArray = readFileToByteArray(f); // Array de bytes do ficheiro

                        sendFile(clientSocket, fileByteArray, answerIP, answerPort);
                    }
                    else if(answerMessage.getType() == 4) //Servidor não deseja ficheiro
                    {
                        LoggerUtil.getLogger().info("C || Servidor em " + i + " não deseja o ficheiro " + f.getPath());
                    }
                    else{
                        LoggerUtil.getLogger().warning("C || Mensagem não reconhecida!");
                        break;
                    }
                }
            }
            Main.updateFiles();
    }
        } catch (SocketException ex) {
            LoggerUtil.getLogger().severe("C || " + ex.getMessage());
        } catch (UnknownHostException ex) {
            LoggerUtil.getLogger().severe("C || " + ex.getMessage());
        } catch (IOException ex) {
            LoggerUtil.getLogger().severe("C || " + ex.getMessage());
        }
        
    }
}
