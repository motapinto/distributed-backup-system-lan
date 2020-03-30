package Common;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logs {
    private static PrintStream logFile = System.err;
    private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    private static String getStandardHeader() {
        Date date = new Date();
        return "" + Thread.currentThread().getId() + ", " + dateFormat.format(date) + ": ";
    }

    synchronized public static void logError(String msg) {

        logFile.println("ERR  @ " + getStandardHeader() + msg);
        logFile.flush();
    }

    synchronized public static void logWarning(String msg) {

        logFile.println("WARN @ " + getStandardHeader() + msg);
        logFile.flush();
    }

    synchronized public static void log(String msg) {

        logFile.println("LOG  @ " + getStandardHeader() + msg);
        logFile.flush();
    }

    synchronized public static void setLogFile(String filepath) throws FileNotFoundException {
        Logs.logFile = new PrintStream(new FileOutputStream(filepath, true));
    }
}