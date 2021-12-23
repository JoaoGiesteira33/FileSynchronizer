import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static String folderToSync;
    public static List<File> filesToSync;
    public static List<File> addedFiles = new ArrayList<>();
    public static String password;

    public static String getFolder(){
        return Main.folderToSync;
    }

    public static String changeFilePath(String filePath){
        String aux = filePath.substring(filePath.indexOf("/")+1);
        return (Main.getFolder() + "/" + aux);
    }

    public static void addFile(File f){
        Main.addedFiles.add(f);
    }

    public static void updateFiles(){
        Main.addedFiles = new ArrayList<>();
        Main.filesToSync = Main.files_to_sync(Main.getFolder());
    }

    public static boolean hasFile(String filePath){
        String aux = changeFilePath(filePath);
        //Verificar se existe
        for(File f : Main.filesToSync){
            if(f.getPath().equals(aux))
                return true;
        }
        for(File f : Main.addedFiles){
            if(f.getPath().equals(aux))
                return true;
        }
        return false;
    }

    public static List<File> files_to_sync(String folder) {
        File directory = new File(folder);
        List<File> resultList = new ArrayList<>();
        // Obter todos os ficheiros dentro da diretoria
        File[] fList = directory.listFiles();
        resultList.addAll(Arrays.asList(fList));
        for (File file : fList) {
            if (file.isFile()) {
                LoggerUtil.getLogger().info("Ficheiro a sinconizar=" + file.getPath());
            } else if (file.isDirectory()) { // Chamada recursiva caso seja um folder
                resultList.addAll(files_to_sync(file.getPath()));
            }
        }
        return resultList.stream().filter(f -> !f.isDirectory()).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            LoggerUtil.getLogger().warning("Argumentos insuficientes");
            return;
        }
        folderToSync = args[0];
        LoggerUtil.getLogger().info("Pasta a sincronizar=" + folderToSync);

        password = args[1];

        List<String> ips = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            ips.add(args[i]);
            LoggerUtil.getLogger().info("IP" + i + "=" + args[i]);
        }

        Main.filesToSync = files_to_sync(Main.getFolder());

        Server myServer = new Server();
        Client myClient = new Client(ips);
        TCPserver myTCPserver = new TCPserver(8080,ips);
        
        Thread t1 = new Thread(myServer);
        Thread t2 = new Thread(myClient);
        Thread t3 = new Thread(myTCPserver);
        
        t1.start();
        t2.start();
        t3.start();
        
        try{
            t1.join();
            t2.join();
            t3.join();
        }
        catch(InterruptedException e){
            LoggerUtil.getLogger().severe(e.getMessage());
        } 
        
    }
}
