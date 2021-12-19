import java.io.*;
import java.net.*;
import java.util.ArrayList;
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
                    //Esperar por resposta
                    //Podemos ter que mexer nos setSoTimeout em situacoes como esta !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);
                    clientSocket.receive(receivePacket);
                    Message receive_m = new Message(receivePacket.getData());
                    //Confirmar se resposta é afirmativa/negativa
                    if(receive_m.getType() == 3){ //IF ACK
                        //START SENDING FILE TO M.GETPORT
                    }
                }
            }

            /*
            while (!(sentence = inFromUser.readLine()).equals(".")) {
                sendData = sentence.getBytes();
                System.out.println("Packet " + counter + " was sent.");
                for(InetAddress i : this.ips){
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                    i, 8888);
                    clientSocket.send(sendPacket);
                }
                counter++;
            }
            */

            inFromUser.close();
            System.out.println("Sending &&& to terminate!");
            sendData = "&&&".getBytes();

            for(InetAddress i : this.ips){
                DatagramPacket sendPacketEnd = new DatagramPacket(sendData, sendData.length,
                i, 8888);
                clientSocket.send(sendPacketEnd);
            }

            DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);
            clientSocket.receive(receivePacket);
            String modifiedSentence = new String(receivePacket.getData());
            System.out.println("FROM SERVER: "
                    + modifiedSentence);
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