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

    /* */
    public static final int MAX_DELAY = 400;
    public static final int MAX_MEMORY = 8000000; // 8MB
    public static final int PUTCHUNK_RETRIES = 5;
    public static final int MAX_CHUNK_SIZE = 64000; // 64 KB
    public static final int MAX_PACKET_SIZE = 64500; // 64 KB
    public static final int MAX_REPLICATION_DEGREE = 9;
    public static final int MAX_NUM_CHUNKS = 1000000;

}
