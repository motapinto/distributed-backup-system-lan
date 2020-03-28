package Peer;

import Channels.*;
import Common.Logs;
import SubProtocols.*;
import static Common.Constants.*;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer implements PeerInterface{
    public static final String FILE_STORAGE_PATH = "../../storage";
    public static final int MAX_SIZE = 64000000;

    private final String version;
    private final int id;
    private final String[] serviceAccessPoint;
    private int currentSystemMemory;

    /** Channels */
    private Channel controlChannel;
    private Channel backupChannel;
    private Channel restoreChannel;

    /** Protocols */
    private Backup backup;
    private Delete delete;
    private Restore restore;
    private SpaceReclaim reclaim;

    /** Executors */
    private ExecutorService senderExecutor;
    private ExecutorService deliverExecutor;
    private ExecutorService receiverExecutor;

    /** Sring: fileId    |    Backup: backup protocol for a file with fileId */
    private ConcurrentHashMap<String, Backup> backupProtocolMap = new ConcurrentHashMap<>();

    /**
     * String : "fileId_chuckNo"   |   String : "repDegree_desiredRepDegree"
     * The necessity of the ConcurrentHashMap comes from the fact that HashMap is not thread-safe, HashTable does not
     * permit concurrency when accessing data (single-lock) and ConcurrentHashMap is more efficient for threaded applications
     */
    private ConcurrentHashMap<String, String> repDegreeInfo = new ConcurrentHashMap<>();

    /**
     * String : "senderId_fileId_chuckNo"   |   String : "senderId"
     */
    private ConcurrentHashMap<String, String> storedChunkHistory = new ConcurrentHashMap<>();


    public Peer(String version, String id, String[] serviceAccessPoint, String[] mcAddress, String[] mdbAddress, String[] mdrAddresss) throws IOException {
        this.version = version;
        this.id = Integer.parseInt(id);
        this.serviceAccessPoint = serviceAccessPoint;
        this.currentSystemMemory = 0; // ?

        this.setupFiles();
        this.readProperties();
        this.setupChannels(mcAddress, mdbAddress, mdrAddresss);
        this.setupExecutors();
        this.createProtocols();
        this.saveProperties();
    }

    /**
     * Create the directory Storage/PeerId
     */
    private void setupFiles() {
        File dir = new File(FILE_STORAGE_PATH + "/" + this.id);
        if(!dir.exists()) dir.mkdirs();
    }

    /**
     * Reads all properties files
     */
    private void readProperties() {
        // Get chunks replication degree info
        String repDegPath = FILE_STORAGE_PATH + "/" + this.id + "/replicationDegreeInfo.properties";
        readMap(repDegPath, this.repDegreeInfo);

        // Get disk space info
        String diskPath = FILE_STORAGE_PATH + "/" + this.id + "/diskInfo.properties";
        File diskInfo = new File(diskPath);

        if (diskInfo.exists()) {
            Properties diskProperties = new Properties();
            try {
                diskProperties.load(new FileInputStream(diskPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.currentSystemMemory = Integer.parseInt(diskProperties.getProperty("used"));
        }
    }


    private void setupExecutors(){
        this.senderExecutor = Executors.newFixedThreadPool(5);
        this.deliverExecutor = Executors.newFixedThreadPool(11);
        this.receiverExecutor = Executors.newFixedThreadPool(10);
    }

    /**
     * Create all 3 channels and starts listening to them
     */
    private void setupChannels(String[] mcAddress, String[] mdbAddress, String[] mdrAddress) throws IOException {
        // Creates channels
        this.controlChannel = new MC(this, mcAddress[0], Integer.parseInt(mcAddress[1]));
        this.backupChannel = new MDB(this, mdbAddress[0], Integer.parseInt(mdbAddress[1]));
        this.restoreChannel = new MDR(this, mdrAddress[0], Integer.parseInt(mdrAddress[1]));

        // Starts each channel thread and starts listening
        new Thread(this.controlChannel).start();
        new Thread(this.backupChannel).start();
        new Thread(this.restoreChannel).start();
    }

    /**
     * Create all 4 protocols
     */
    private void createProtocols() {
        this.backup = new Backup(this);
        /*this.delete = new Delete();
        this.restore = new Restore();
        this.reclaim = new SpaceReclaim();*/
    }

    /**
     * Reads all properties files and stores it on a map
     * A .properties file is a simple collection of KEY-VALUE pairs that can be parsed by the java.util.Properties class.
     * https://mkyong.com/java/java-properties-file-examples/
     */
    private void readMap(String path, ConcurrentHashMap map) {
        File in = new File(path);
        if(!in.exists()) return;

        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(path));
            properties.forEach((key, value) -> map.put(key, value));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves all maps into properties files
     * A .properties file is a simple collection of KEY-VALUE pairs that can be parsed by the java.util.Properties class.
     * https://mkyong.com/java/java-properties-file-examples/
     */
    private void saveMap(String path, ConcurrentHashMap map) {
        File out = new File(path);
        if(!out.exists()) return;

        try {
            Properties properties = new Properties();
            properties.putAll(map);
            properties.store(new FileOutputStream(path), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads all properties files regarding chunks replication degree
     */
    private void saveProperties() {
        // Save chunks replication degree info
        String repDegPath = FILE_STORAGE_PATH + "/" + this.id + "/replicationDegreeInfo.properties";
        saveMap(repDegPath, this.repDegreeInfo);

        // Save disk space info
        String diskPath = FILE_STORAGE_PATH + "/" + this.id + "/diskInfo.properties";
        File diskInfo = new File(diskPath);

        if(!diskInfo.exists()) return;

        try {
            Properties diskProperties = new Properties();
            diskProperties.setProperty("used", Integer.toString(this.currentSystemMemory));
            diskProperties.store(new FileOutputStream(diskPath), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void backup(String pathName, int replicationDegree) throws IOException {


        if(replicationDegree > MAX_REPLICATION_DEGREE) {
            Logs.logError("Maximum replication Degree reached!");
            return;
        }

        this.backup = new Backup(this, pathName, replicationDegree);

        // finish all the pending tasks related with PUTCHUNK


        this.backup.startPutchunkProcedure();

        this.backupProtocolMap.put(backup.getFileId(), backup);
    }

    public void restore(String pathname) {}

    public void delete(String pathname) {

    }

    public void reclaim(int maxDiskSpace) {

    }

    /** Returns desired/current replication degree for a pair (fileId, chuckNo) */
    public int getRepDegreeInfo(String fileId, String chunkNo, boolean getCurrent) {
        String id = fileId + "_" + chunkNo;

        int index;
        if(getCurrent) index = 0;
        else index = 1;

        if(this.repDegreeInfo.get(id) != null) {
            return Integer.parseInt(this.repDegreeInfo.get(id).split("_")[index]);
        } else {
            return -1;
        }
    }

    /**
     * This function increments the replication degree of the chunk with id = fileId + "_" + chunkNo
     * only if this is the first time that the sender of a message as sent the STORED message
     *
     * @param senderId : sender id
     * @param fileId   : file id
     * @param chunkNo  : number of the chunk
     */
    public void incrementRepDegreeInfo(String senderId, String fileId, String chunkNo) {
        String id = fileId + "_" + chunkNo;
        if(this.storedChunkHistory.get(senderId + "_" + id) == null) {
            this.storedChunkHistory.put(senderId + "_" + id, senderId);

            if(this.repDegreeInfo.get(id) != null) {
                int currentRepDegree = getRepDegreeInfo(fileId, chunkNo, true) + 1;
                int desiredRepDegree = getRepDegreeInfo(fileId, chunkNo, false);
                this.repDegreeInfo.put(id, currentRepDegree + "_" + desiredRepDegree);
            }
        }
    }

    public void setRepDegreeInfo(String fileId, String chunkNo, int desiredRepDegree) {
        String id = fileId + "_" + chunkNo;
        this.repDegreeInfo.put(id, "1_" + desiredRepDegree);
    }

    public int getAvailableStorage() {
        return MAX_SIZE -  this.currentSystemMemory;
    }

    public String getVersion() {
        return version;
    }

    public int getId() {
        return id;
    }

    public String[] getServiceAccessPoint() {
        return serviceAccessPoint;
    }

    public Channel getControlChannel() {
        return controlChannel;
    }

    public Channel getBackupChannel() {
        return backupChannel;
    }

    public Channel getRestoreChannel() {
        return restoreChannel;
    }

    public Backup getBackup() {
        return backup;
    }

    public ConcurrentHashMap<String, String> getRepDegreeInfo() {
        return repDegreeInfo;
    }

    public int getCurrentSystemMemory() {
        return currentSystemMemory;
    }

    public void setCurrentSystemMemory(int currentSystemMemory) {
        this.currentSystemMemory = currentSystemMemory;
    }

    public ExecutorService getSenderExecutor() {
        return senderExecutor;
    }

    public void setSenderExecutor(ExecutorService senderExecutor) {
        this.senderExecutor = senderExecutor;
    }

    public ExecutorService getDeliverExecutor() {
        return deliverExecutor;
    }

    public void setDeliverExecutor(ExecutorService deliverExecutor) {
        this.deliverExecutor = deliverExecutor;
    }

    public ExecutorService getReceiverExecutor() {
        return receiverExecutor;
    }

    public void setReceiverExecutor(ExecutorService receiverExecutor) {
        this.receiverExecutor = receiverExecutor;
    }

    public static void main(String[] args) throws IOException {
        String[] serviceAccessPoint = {"sda", "sad"};
        String[] mcAddress = {"localhost", "45"};
        String[] mdbAddress = {"localhost", "46"};
        String[] mdrAddress = {"localhost", "48"};
        Peer peer1 = new Peer("1", "1", serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);
        Peer peer2 = new Peer("1", "2", serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);

        peer1.backup("C:\\Users\\Martim\\Desktop\\feup-LBAW\\Theory\\01-intro.pdf", 1);
    }
}
