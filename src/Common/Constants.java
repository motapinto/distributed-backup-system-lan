package Common;

public class Constants {

    /* Parsing messages delimiter */
    public static final byte CR = 0xD;
    public static final byte LF = 0xA;
    public static final String CRLF = "\r\n";

    /* Message type */
    public static final String PUTCHUNK = "PUTCHUNK";
    public static final String CHUNK = "CHUNK";
    public static final String GETCHUNK = "GETCHUNK";
    public static final String REMOVED = "REMOVED";
    public static final String STORED = "STORED";
    public static final String DELETE = "DELETE";

}
