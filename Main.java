import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    private static String folderToSync;
    private static List<File> filesToSync;

    public static String getFolder(){
        return Main.folderToSync;
    }

    public static void addFile(File f){
        Main.filesToSync.add(f);
    }

    public static void updateFiles(){
        Main.filesToSync = Main.files_to_sync(Main.getFolder());
    }

    //Ignorar nome da Pasta, pois pode mudar de cliente para cliente
    public static boolean hasFile(String filePath){
        //Cortar nome da Pasta
        String aux = filePath.substring(filePath.indexOf("/")+1);
        //Adicionar pasta atual
        String aux2 = Main.getFolder() + "/" + aux;
        //Verificar se existe
        for(File f : Main.filesToSync){
            if(f.getPath().equals(aux2))
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
        return resultList;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            LoggerUtil.getLogger().warning("Argumentos insuficientes");
            return;
        }
        folderToSync = args[0];
        LoggerUtil.getLogger().info("Pasta a sincronizar=" + folder_to_sync);

        List<String> ips = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            ips.add(args[i]);
            LoggerUtil.getLogger().info("IP" + i + "=" + args[i]);
        }

        Main.filesToSync = files_to_sync(Main.getFolder());

        
        Server myServer = new Server();
        Client myClient = new Client(Main.filesToSync,ips);
        
        Thread t1 = new Thread(myServer);
        Thread t2 = new Thread(myClient);
        
        t1.start();
        t2.start();
        
        try{
            t1.join();
            t2.join();
        }
        catch(InterruptedException e){
            LoggerUtil.getLogger().severe(e.getMessage());
        }   
    }
}