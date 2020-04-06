package Peer;

import Channels.*;
import Common.Logs;
import Message.Message;
import SubProtocols.*;
import static Common.Constants.*;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer implements PeerInterface{
    private final String version;
    private int id;
    private final String[] serviceAccessPoint;
    private int usedMemory = INITIAL_MAX_MEMORY;
    private int maxMemory = INITIAL_MAX_MEMORY;

    public static String FILE_STORAGE_PATH = "storage";
    public String REPLICATION_DEGREE_INFO_PATH;
    public String DISK_INFO_PATH ;
    public String STORED_CHUNK_HISTORY_PATH;

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

    /**
     * String : "fileId_chuckNo"   |   String : "repDegree_desiredRepDegree"
     * The necessity of the ConcurrentHashMap comes from the fact that HashMap is not thread-safe, HashTable does not
     * permit concurrency when accessing data (single-lock) and ConcurrentHashMap is more efficient for threaded applications
     */
    private ConcurrentHashMap<String, String> repDegreeInfo = new ConcurrentHashMap<>();

    /**
     * String : "senderId_fileId_chuckNo_size"   |   String : "senderId"
     * senderId from the key: senderId that send STORED message
     * senderId from the value: senderId that receives STORED message
     */
    private ConcurrentHashMap<String, String> storedChunkHistory = new ConcurrentHashMap<>();


    /**
     * ConcurrentHashMap used in state() to store all backup information
     */
    private ConcurrentHashMap<String, Backup> backupInfo = new ConcurrentHashMap<>();


    public Peer(String version, String id, String[] serviceAccessPoint, String[] mcAddress, String[] mdbAddress, String[] mdrAddresss) throws IOException {
        this.version = version;
        this.id = Integer.parseInt(id);
        this.serviceAccessPoint = serviceAccessPoint;

        REPLICATION_DEGREE_INFO_PATH = FILE_STORAGE_PATH + "/" + this.id + "/replicationDegreeInfo.properties";
        DISK_INFO_PATH = FILE_STORAGE_PATH + "/" + this.id + "/diskInfo.properties";
        STORED_CHUNK_HISTORY_PATH = FILE_STORAGE_PATH + "/" + this.id + "/storedChunkHistory.properties";

        this.setupFiles();
        this.readProperties();
        this.setupChannels(mcAddress, mdbAddress, mdrAddresss);
        this.setupExecutors();
        this.createProtocols();
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
        readMap(REPLICATION_DEGREE_INFO_PATH, this.repDegreeInfo);
        readMap(STORED_CHUNK_HISTORY_PATH, this.storedChunkHistory);

        // Get disk space info
        File diskInfo = new File(DISK_INFO_PATH);

        if (!diskInfo.exists()) {
            this.setMaxMemory(INITIAL_MAX_MEMORY);
            this.setUsedMemory(0);
            return;
        }

        Properties diskProperties = new Properties();
        try {
            diskProperties.load(new FileInputStream(DISK_INFO_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.usedMemory = Integer.parseInt(diskProperties.getProperty("used"));
        this.maxMemory = Integer.parseInt(diskProperties.getProperty("max"));
    }

    /**
     * Create all 3 channels and starts listening to them
     *
     * @param mcAddress : address of the control chanel
     * @param mdbAddress : address of the backup chanel
     * @param mdrAddress : address of the restore chanel
     */
    private void setupChannels(String[] mcAddress, String[] mdbAddress, String[] mdrAddress) throws IOException {
        this.controlChannel = new MC(this, mcAddress[0], Integer.parseInt(mcAddress[1]));
        this.backupChannel = new MDB(this, mdbAddress[0], Integer.parseInt(mdbAddress[1]));
        this.restoreChannel = new MDR(this, mdrAddress[0], Integer.parseInt(mdrAddress[1]));

        new Thread(this.controlChannel).start();
        new Thread(this.backupChannel).start();
        new Thread(this.restoreChannel).start();
    }

    /**
     * Comment...
     */
    private void setupExecutors(){
        this.senderExecutor = Executors.newFixedThreadPool(5);
        this.deliverExecutor = Executors.newFixedThreadPool(11);
        this.receiverExecutor = Executors.newFixedThreadPool(10);
    }

    /**
     * Create all 4 protocols
     */
    private void createProtocols() {
        this.backup = new Backup(this);
        this.delete = new Delete(this);
        this.restore = new Restore(this);
        this.reclaim = new SpaceReclaim(this);
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
    public void saveMap(String path, ConcurrentHashMap map) {
        Properties properties = new Properties();
        properties.putAll(map);
        try {
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
        this.saveMap(REPLICATION_DEGREE_INFO_PATH, this.repDegreeInfo);
        this.saveMap(STORED_CHUNK_HISTORY_PATH, this.storedChunkHistory);
    }

    /**
     * Client interface for executing backup protocol
     * @param pathName : path name of the file to be backed up
     * @param replicationDegree : desired replication degree for the file
     */
    public void backup(String pathName, int replicationDegree) {
        if(replicationDegree > MAX_REPLICATION_DEGREE) {
            Logs.logError("Maximum replication Degree reached!");
            return;
        }

        this.backup = new Backup(this, pathName, replicationDegree);
        this.backup.startPutChunkProcedure();

        // Stores backup protocol to be used in state()
        this.backupInfo.put(this.backup.getFileId(), this.backup);
    }

    public void restore(String pathname) {
        this.restore = new Restore(this, pathname);
        this.restore.startGetChunkProcedure();
    }

    /**
     * The client shall specify the file to delete by its pathname
     * @param pathname name of the file to delete -> should be in the peer's files directories
     */
    public void delete(String pathname) {
        this.delete = new Delete(this, pathname);
        this.delete.startDeleteProcedure();
    }

    /**
     * The client shall specify the maximum disk space in KBytes that can be used for storing chunks.
     * Thus, if maxDiskSpace = 0 => the peer shall reclaim all disk space previously allocated
     * @param maxDiskSpace
     */
    public void reclaim(int maxDiskSpace) {
        this.reclaim = new SpaceReclaim(this, maxDiskSpace);
        this.reclaim.startSpaceReclaimProcedure();
    }

    /** Returns desired/current replication degree for a pair (fileId, chuckNo) */
    public String getRepDegreeInfo(String fileId, String chunkNo, boolean getCurrent) {
        String chunkId = fileId + "_" + chunkNo;
        int index;
        if(getCurrent) index = 0;
        else index = 1;

        if(this.repDegreeInfo.get(chunkId) != null) {
            return this.repDegreeInfo.get(chunkId).split("_")[index];
        }
        else {
            return null;
        }
    }

    /**
     * This function increments the replication degree of the chunk with id = fileId + "_" + chunkNo
     * only if this is the first time that the sender of a message as sent the STORED message
     *
     * @param message PUTCHUNK message that came from the peer that wants this peer to store the chunk
     */
    public void updateRepDegreeInfo(Message message, boolean increment) {

        String fileId = message.getHeader().getFileId();
        String chunkNo = message.getHeader().getChuckNo();
        String chunkId = fileId + "_" + chunkNo;
        String senderId = message.getHeader().getSenderId();
        String storedMessageHistoryId = senderId + "_" + chunkId;

        if(!increment) {
            String repDegInf = this.repDegreeInfo.get(chunkId);
            Integer newValue = Integer.parseInt(repDegInf.split("_")[0]) - 1;
            this.repDegreeInfo.put(chunkId, newValue + "_" + repDegInf.split("_")[1]);
            this.storedChunkHistory.remove(storedMessageHistoryId);
            this.saveProperties();
            return;
        }

        if(this.storedChunkHistory.get(storedMessageHistoryId) == null && this.repDegreeInfo.get(chunkId) != null){
            this.storedChunkHistory.put(storedMessageHistoryId, senderId);
            String currentRepDegree = Integer.toString(Integer.parseInt(this.getRepDegreeInfo(fileId, chunkNo, true)) + 1);
            String desiredRepDegree = this.getRepDegreeInfo(fileId, chunkNo, false);
            this.repDegreeInfo.put(chunkId, currentRepDegree + "_" + desiredRepDegree);
            this.saveProperties();
        }
    }

    public void initiateRepDegreeInfo(Message message){
        String fileId = message.getHeader().getFileId();
        String senderId = message.getHeader().getSenderId();
        String chunkNo = message.getHeader().getChuckNo();
        String desiredRepDegree = message.getHeader().getReplicationDeg();

        if(this.repDegreeInfo.get(fileId + "_" + chunkNo) == null){
            String currentRepDegree;

            if(this.id == Integer.parseInt(senderId))
                currentRepDegree = "0";
            else {
                currentRepDegree = "1";
                this.storedChunkHistory.put(this.id + "_" + fileId + "_" + chunkNo, senderId);
            }

            this.repDegreeInfo.put(fileId + "_" + chunkNo, currentRepDegree + "_" + desiredRepDegree);
            saveProperties();
        }
    }

    public void state() {
        for(Map.Entry<String, Backup> entry : this.backupInfo.entrySet()) {
            String fileId = entry.getKey();
            Backup backup = entry.getValue();

            System.out.println("File pathname: " + Peer.FILE_STORAGE_PATH + backup.getPeer().getId() + "/" + backup.getPeer().getId()  + "/" + fileId);
            System.out.println("Backup service id of the file: " + backup.getPeer().getId());
            System.out.println("Desired replication degree: " + backup.getDesiredRepDeg());

            System.out.println("Information about each file chunk:");
            for(Map.Entry<String, String> chunk : this.repDegreeInfo.entrySet()) {
                String chunkId = chunk.getKey();
                String repDegInfo = chunk.getValue();

                System.out.println("Id: " + backup.getFileId() + chunkId.split("_")[1]);
                System.out.println("Perceived replication degree: " + repDegInfo.split("_")[0]);
                System.out.println("Desired replication degree: " + repDegInfo.split("_")[1]);
            }
        }

        System.out.println("Information about each stored chunk:");
        for(Map.Entry<String, String> entry : this.storedChunkHistory.entrySet()) {
            String fileId = entry.getKey().split("_")[1] + entry.getKey().split("_")[2];
            System.out.println("Chunk id: " + entry.getKey().split("_")[1]);
            //System.out.println("Chunk size: " + ); -> criar uma nova mensgaem igual a storedmas com o tamanho??
            System.out.println("Perceived replication degree: " + entry.getValue().split("_")[0]);
        }

        System.out.println("Information about peer storage capacity");
        System.out.println("Maximum amount of disk space that can be used to store chunks: " + this.getUsedMemory() + " KBytes");
        System.out.println("Amount of storage used to backup the chunks" + this.getUsedMemory() * 1000 + " KBytes");
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof Peer)) return true;

        Peer c = (Peer)o;
        return c.getId() == this.id;
    }

    public int getAvailableStorage() {
        return this.maxMemory -  this.usedMemory;
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
        return this.backup;
    }

    public Delete getDelete() {
        return this.delete;
    }

    public Restore getRestore() {
        return this.restore;
    }

    public SpaceReclaim getSpaceReclaim() {
        return this.reclaim;
    }

    public ConcurrentHashMap<String, String> getRepDegreeInfo() {
        return this.repDegreeInfo;
    }

    public ConcurrentHashMap<String, String> getStoredChunkHistory() {
        return this.storedChunkHistory;
    }

    public int getMaxMemory() {
        return this.maxMemory;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;

        // Save disk space info
        Properties diskProperties = new Properties();
        diskProperties.setProperty("used", Integer.toString(this.usedMemory));
        diskProperties.setProperty("max", Integer.toString(this.maxMemory));

        try {
            diskProperties.store(new FileOutputStream(DISK_INFO_PATH), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getUsedMemory() {
        return this.usedMemory;
    }

    public void setUsedMemory(int usedMemory) {
        this.usedMemory = usedMemory;

        // Save disk space info
        Properties diskProperties = new Properties();
        diskProperties.setProperty("used", Integer.toString(this.usedMemory));
        diskProperties.setProperty("max", Integer.toString(this.maxMemory));

        try {
            diskProperties.store(new FileOutputStream(DISK_INFO_PATH), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ExecutorService getSenderExecutor() {
        return senderExecutor;
    }

    public ExecutorService getDeliverExecutor() {
        return deliverExecutor;
    }

    public ExecutorService getReceiverExecutor() {
        return receiverExecutor;
    }

    public static void main(String[] args) throws IOException {
        String[] serviceAccessPoint = {"sda", "sad"};
        String[] mcAddress = {"224.0.0.0", "4445"};
        String[] mdbAddress = {"224.0.0.1", "4446"};
        //String[] mdbAddress = {"224.0.0.1", "4446"};
        String[] mdrAddress = {"224.0.0.2", "4447"};

        if(args[0].equals("1")) {
            FILE_STORAGE_PATH = FILE_STORAGE_PATH + '1';
            Peer peer1 = new Peer("1", "1", serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);
            peer1.backup( FILE_STORAGE_PATH + "/1/" + "Teste.txt", 2);
            //peer1.delete( FILE_STORAGE_PATH + "/1/" + "Teste.txt");
        }
        else if(args[0].equals("2")) {
            FILE_STORAGE_PATH = FILE_STORAGE_PATH + '2';
            Peer peer2 = new Peer("1", "2", serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);
            peer2.reclaim(0);
        }
        else if(args[0].equals("3")){
            FILE_STORAGE_PATH = FILE_STORAGE_PATH + '3';
            Peer peer3 = new Peer("1", "3", serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);
            peer3.reclaim(130);
        }
        else {
            FILE_STORAGE_PATH = FILE_STORAGE_PATH + '3';
            Peer peer4 = new Peer("1", "3", serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);
        }
    }
}