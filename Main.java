import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
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
        String folder_to_sync = args[0];
        LoggerUtil.getLogger().info("Pasta a sincronizar=" + folder_to_sync);

        List<String> ips = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            ips.add(args[i]);
            LoggerUtil.getLogger().info("IP" + i + "=" + args[i]);
        }

        List<File> files_to_sync = files_to_sync(folder_to_sync);
        
        Server myServer = new Server();
        Client myClient = new Client(files_to_sync,ips);
        
        Thread t1 = new Thread(myServer);
        Thread t2 = new Thread(myClient);
        
        t1.start();
        t2.start();
        
        System.out.println("Ola");

        try{
            t1.join();
            t1.join();
            t2.join();
        }
        catch(InterruptedException e){
            LoggerUtil.getLogger().severe(e.getMessage());
        }
         
    }
}