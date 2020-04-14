package Common;

public class Constants {

    /* Messages delimiter */
    public static final String CRLF = "\r\n";

    /* Message type */
    public static final String PUTCHUNK = "PUTCHUNK";
    public static final String CHUNK = "CHUNK";
    public static final String GETCHUNK = "GETCHUNK";
    public static final String REMOVED = "REMOVED";
    public static final String STORED = "STORED";
    public static final String DELETE = "DELETE";
    public static final String ENRESTORE = "ENRESTORE";
    public static final String DELETEACK = "DELETEACK";

    /* Enhancements versions */
    public static final String BACKUP_V2 = "1.1";
    public static final String DELETE_V2 = "1.2";
    public static final String RESTORE_V2 = "1.3";

    /* Common variables */
    public static final int MAX_DELAY = 400; // 400 micro seconds
    public static final int INITIAL_MAX_MEMORY = 100000000; // 100MB
    public static final int MESSAGE_RETRIES = 5;
    public static final int MAX_CHUNK_SIZE = 64000; // 64 KB
    public static final int MAX_PACKET_SIZE = 64500; // 64 KB
    public static final int MAX_REPLICATION_DEGREE = 9;
    public static final int MAX_NUM_CHUNKS = 1000000; // 1 million
}