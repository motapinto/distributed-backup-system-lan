package Peer;

import Channels.*;



public class Peer {


    /* Maximum storage allowed by a peer */
    public static final int MAX_DELAY = 400;
    public static final int MAX_MEMORY = 8000000 // 8MB
    public static final int PUTCHUNK_RETRIES = 5;
    public static final int MAX_CHUNK_SIZE = 64000; // 64 Kb
    public static final int MAX_REPLICATION_DEGREE = 9;
    public static final int MAX_NUM_CHUNKS = 1000000;
    public static final String FILE_STORAGE_PATH = '../../storage'

    private String version;
    private int id;
    private String[] serviceAccessPoint;
    private Channel controlChannel;
    private Channel backupChannel;
    private Channel restoreChannel;
    private int currentSystemMemory;

    public Peer(String version, String id, String[] serviceAccessPoint, String[] mcAddress, String[] mdbAddress, String[] mdrAddresss) {
        this.version = version;
        this.id = Integer.parseInt(id);
        this.serviceAccessPoint = serviceAccessPoint;
        this.currentSystemMemory = 0;

        setupChannels(mcAddress, mdbAddress, mdrAddresss);
    }

    public void setupChannels(String[] mcAddress, String[] mdbAddress, String[] mdrAddresss){


        this.controlChannel = new MC(this, mcAddress[0], mcAddress[1]);
        this.backupChannel = new MDB(this, mdbAddress[0], mdbAddress[1]);
        this.restoreChannel = new MDR(this, mdrAddresss[0], mdrAddresss[1]);




    }


    public void backup(){


    }

    public void restore(){


    }


    public void delete(){


    }

    public void reclaim(){

    }


}
