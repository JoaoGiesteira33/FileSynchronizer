import java.io.File;
import java.net.InetAddress;

public class FileDataHandler implements Runnable{
    private File f;
    private InetAddress ip; //Do cliente de onde estamos a receber o ficheiro
    private int port; //Do cliente de onde estamos a receber o ficheiro

    public FileDataHandler(File f, InetAddress ip, int port){
        this.ip = ip;
        this.port = port;
    }

    public void run(){
        
    }
}