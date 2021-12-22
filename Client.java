import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client implements Runnable {
    private List<InetAddress> ips;
    private List<Thread> ths;

    public Client(List<String> ipsString) {
        this.ips = new ArrayList<>();
        this.ths = new ArrayList<>();
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

    public void run() {
        while(true){
            this.ths = new ArrayList<>();
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
                            LoggerUtil.getLogger().info("C || Socket timed out a espera da resposta");
                            gotAnswer = false; // We did not receive an ack
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

                        FileDataSender fds = new FileDataSender(fileByteArray, answerIP, answerPort);
                        Thread t = new Thread(fds);
                        this.ths.add(t);
                        //t.start();
/*
                        //Informação para tempo de transferência e débito final
                        long startTime = System.nanoTime();
                        long totalUpload = f.getTotalSpace() * 8; //bits

                        sendFile(clientSocket, fileByteArray, answerIP, answerPort); //Envio de ficheiro

                        long transferTime = ((System.nanoTime() - startTime) / 1000000000);
                        float bitsPerSec = (float)totalUpload / transferTime;
                        System.out.println("C || F: " + f.getPath() + " | bps: " + bitsPerSec);
                        System.out.println("C || F: " + f.getPath() + " | Time of transfer: " + transferTime + " secs");
                        */
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
            for(Thread t : this.ths){
                t.start();           
            }
            for(Thread t : this.ths){
                try {
                    t.join();
                } catch (InterruptedException e) {
                    LoggerUtil.getLogger().severe("C || " + e.getMessage());
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
        Main.updateFiles();
    }
    }
}