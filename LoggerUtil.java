import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerUtil {

	public static final String LOGGERNAME = "log";

	static {
		Logger.getLogger(LOGGERNAME).setUseParentHandlers(false);
		try {
			FileHandler fh = new FileHandler(LOGGERNAME);
			fh.setLevel(Level.INFO);
            fh.setFormatter(new SimpleFormatter());
			Logger.getLogger(LOGGERNAME).addHandler(fh);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void setHandlersLevel(Level level) {
		Handler[] handlers = Logger.getLogger(LOGGERNAME).getHandlers();
		for (Handler h : handlers)
			h.setLevel(level);

		Logger.getLogger(LOGGERNAME).setLevel(level);
	}

	public static Logger getLogger() {
		return Logger.getLogger(LOGGERNAME);
	}
}