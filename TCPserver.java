import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket; 
import java.net.Socket; 
import java.util.ArrayList;
import java.util.List;

public class TCPserver implements Runnable{
    private ServerSocket ss;
    private int port;
    private String message;
    private boolean onOff;

    public TCPserver(int port,List<String> ips) {
        this.port = port;
        try {
            this.ss = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.onOff = true;  
        StringBuilder sb = new StringBuilder();
        sb.append("Sincronizando para os seguintes utilizadores: \n");
        for(String ip : ips){
            sb.append(ip + "\n");
        }
        sb.append("Sincronizando os seguintes ficheiros: \n");
        for(File f : Main.filesToSync)
        {
            sb.append(f.getPath() + "\n");
        }
        this.message = sb.toString();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerSocket getSocket() {
        return ss;
    }

    public void setSocket(ServerSocket ss) {
        this.ss = ss;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void addMessage(String m){ String old = message; message = old+"<br>"+m;}
    public boolean isOnOff() {
        return onOff;
    }

    public void setOnOff(boolean onOff) {
        this.onOff = onOff;
    }

    public static String writeMessage(String body) {
        String html = "<html><head><title>FFsync</title></head><body><h1>" + body + "</body></html>";
        final String CRLF = "\r\n"; //13, 10
        return "HTTP/1.1 200 OK" + CRLF +//status Line : http_version response_code response_message
                "Content-Length: " + html.getBytes().length + CRLF + //header
                CRLF +
                html +
                CRLF;
    }

    public void run() {
        while (onOff) {
            try {
                Socket socket = ss.accept();
                OutputStream os = socket.getOutputStream();
                String response = writeMessage(message);
                os.write(response.getBytes());
                os.flush();
                os.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}