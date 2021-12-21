import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client implements Runnable {
    List<InetAddress> ips;

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
        byte[] sendData = new byte[261]; //Tamanho máximo de um pacote
        LoggerUtil.getLogger().info("Sending file");
        int sequenceNumber = 0; // Para ordenar envio de pacotes
        int ackSequence = 0; // Verificar se o pacote foi enviado corretamente

        for (int i = 0; i < fileByteArray.length; i = i + 256) {
            sequenceNumber++;

            // Cria uma mensagem, que muda se o ficheiro já chegou ao fim
            Message m;
            if ((i + 255) >= fileByteArray.length) { // Chegamos ao fim do ficheiro
                //CUIDADO NESTA PASSO, MUITO POTENCIAL PARA DAR ERRO
                m = new Message(2, sequenceNumber, fileByteArray.length - i, Arrays.copyOfRange(fileByteArray,i,i+(fileByteArray.length-i-1)));
            } else { //Ainda não chegamos ao fim 
                m = new Message(2,sequenceNumber,256, Arrays.copyOfRange(fileByteArray,i,i+255));
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
    }

    public void run() {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            
            byte[] sendData = new byte[128]; // Data a enviar

            for(File f : Main.filesToSync){
                
                //Criação de uma mensagem com o nome do ficheiro (TIPO 1)
                String file_path = f.getPath();
                Message send_m = new Message(1,f.getPath().length(),file_path.getBytes());
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
                            LoggerUtil.getLogger().info("C || Socket timed out waiting for answer");
                            gotAnswer = false; // We did not receive an ack
                        }
        
                        // Pacote foi recebido e servidor confirmou
                        if (gotAnswer){
                            answerIP = answerPacket.getAddress();
                            answerPort = answerPacket.getPort();
                            LoggerUtil.getLogger().info("C || Answer Received");
                            break;
                        } // Pacote não foi recebido, vamos reenviar
                        else {
                            clientSocket.send(sendPacket);
                            LoggerUtil.getLogger().warning("C || Resending File Request: " + f.getPath());
                        }
                    }

                    //Verificar tipo de resposta do servidor
                    if(answerMessage.getType() == 3){ //Servidor deseja ficheiro
                        LoggerUtil.getLogger().info("C || Iniciar transferencia do ficheiro " + file_path);
                        byte[] fileByteArray = readFileToByteArray(f); // Array de bytes do ficheiro
                        sendFile(clientSocket, fileByteArray, answerIP, answerPort); //Envio de ficheiro
                    }
                    else if(answerMessage.getType() == 4) //Servidor não deseja ficheiro
                    {
                        LoggerUtil.getLogger().info("C || Servidor em " + i + " não deseja o ficheiro " + f.getPath());
                    }
                    else{
                        LoggerUtil.getLogger().warning("C || Mensagem não reconhecida!");
                    }
                }
            }
            clientSocket.close();
        } catch (SocketException ex) {
            LoggerUtil.getLogger().severe("C || " + ex.getMessage());
        } catch (UnknownHostException ex) {
            LoggerUtil.getLogger().severe("C || " + ex.getMessage());
        } catch (IOException ex) {
            LoggerUtil.getLogger().severe("C || " + ex.getMessage());
        }
    }
}