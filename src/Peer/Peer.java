package Peer;

import Channels.*;
import Common.Logs;
import Message.Message;
import SubProtocols.*;
import static Common.Constants.*;

import java.io.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Peer extends UnicastRemoteObject implements PeerInterface {
    private final String version;
    private int id;
    private final String serviceAccessPoint;

    public String FILE_STORAGE_PATH;
    public String REPLICATION_DEGREE_INFO_PATH;
    public String DISK_INFO_PATH ;
    public String STORED_CHUNK_HISTORY_PATH;
    public String INITIATOR_BACKUP_INFO_PATH;

    private Semaphore mutex = new Semaphore(1);

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
    private ExecutorService receiverExecutor;

    /**
     * Holds information regarding if the chunk has been sent
     * String is a par of fileId+chunkNo
     * Boolean holds the current replication degree
     */
    private Map<String, Boolean> sentChunks = new ConcurrentHashMap<>();

    /**
     * String : "fileId_chuckNo"   |   String : "repDegree_desiredRepDegree"
     * The necessity of the ConcurrentHashMap comes from the fact that HashMap is not thread-safe, HashTable does not
     * permit concurrency when accessing data (single-lock) and ConcurrentHashMap is more efficient for threaded applications
     */
    private Map<String, String> repDegreeInfo = new ConcurrentHashMap<>();

    /**
     * String : "senderId_fileId_chuckNo_size"   |   String : "senderId"
     * if senderId(key) == senderId(value) -> received STORED message
     * else {
     *      senderId from the key: senderId that send STORED message
     *      senderId from the value: senderId that receives STORED message
     *  }
     */
    private Map<String, String> storedChunkHistory = new ConcurrentHashMap<>();

    /**
     * ConcurrentHashMap used in state() to store all backup information
     */
    private Map<String, Backup> backupInfo = new ConcurrentHashMap<>();

    /**
     * ConcurrentHashMap used to store the initiator of the backup
     */
    private Map<String, String> initiatorBackupInfo = new ConcurrentHashMap<>();

    
    /**
     * ConcurrentHashMap used to store the memory info
     */
    private Map<String, String> memoryInfo = new ConcurrentHashMap<>();
    public Peer(String version, String id, String serviceAccessPoint, String[] mcAddress, String[] mdbAddress, String[] mdrAddress) throws IOException {
        super();

        this.version = version;
        this.id = Integer.parseInt(id);
        this.serviceAccessPoint = serviceAccessPoint;

        FILE_STORAGE_PATH = "storage" + this.id;
        REPLICATION_DEGREE_INFO_PATH = FILE_STORAGE_PATH + "/replicationDegreeInfo.properties";
        DISK_INFO_PATH = FILE_STORAGE_PATH + "/diskInfo.properties";
        STORED_CHUNK_HISTORY_PATH = FILE_STORAGE_PATH + "/storedChunkHistory.properties";
        INITIATOR_BACKUP_INFO_PATH = FILE_STORAGE_PATH + "/initiatorBackupInfo.properties";

        this.setupFiles();
        this.readProperties();
        this.setupChannels(mcAddress, mdbAddress, mdrAddress);
        this.setupExecutors();
        this.createProtocols();
    }

    /**
     * Create the directory Storage/PeerId
     */
    private void setupFiles() {
        File dir = new File(FILE_STORAGE_PATH);
        if(!dir.exists()) {
            boolean mkdirs = dir.mkdirs();
            if(!mkdirs)
                System.out.println("Error creating directory storage" + this.id);
        }
    }

    /**
     * Reads all properties files
     */
    private void readProperties() {
        readMap(REPLICATION_DEGREE_INFO_PATH, this.repDegreeInfo);
        readMap(STORED_CHUNK_HISTORY_PATH, this.storedChunkHistory);
        readMap(INITIATOR_BACKUP_INFO_PATH, this.initiatorBackupInfo);


        if(!readMap(DISK_INFO_PATH, this.memoryInfo)){
            this.memoryInfo.put("used", "0");
            this.memoryInfo.put("max", Integer.toString(INITIAL_MAX_MEMORY));
            saveMap(DISK_INFO_PATH, this.memoryInfo);
        }
    }

    /**
     * Create all 3 channels and starts listening to them
     *
     * @param mcAddress : address of the control chanel
     * @param mdbAddress : address of the backup chanel
     * @param mdrAddress : address of the restore chanel
     */
    private void setupChannels(String[] mcAddress, String[] mdbAddress, String[] mdrAddress) {
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
    private boolean readMap(String path, Map map) {
        File in = new File(path);
        if(!in.exists()) return false;

        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(path));
            properties.forEach((key, value) -> map.put(key, value));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Saves all maps into properties files
     * A .properties file is a simple collection of KEY-VALUE pairs that can be parsed by the java.util.Properties class.
     * https://mkyong.com/java/java-properties-file-examples/
     */
    public void saveMap(String path, Map map) {
        Properties properties = new Properties();

        map.forEach((key, value) -> {

            properties.put(key, value);

        });



        //properties.putAll(map);

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            properties.store(fileOutputStream, null);
            fileOutputStream.close();
           // properties.forEach((k, v) -> System.out.println("Key : " + k + ", Value : " + v));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Reads all properties files regarding chunks replication degree
     */
    private void saveProperties() {
        // Save chunks replication degree info
        this.saveMap(STORED_CHUNK_HISTORY_PATH, this.storedChunkHistory);
        this.saveMap(REPLICATION_DEGREE_INFO_PATH, this.repDegreeInfo);
      //
    }

    /**
     * Client interface for executing backup protocol
     * @param pathName : path name of the file to be backed up
     * @param replicationDegree : desired replication degree for the file
     */
    public void backup(String pathName, int replicationDegree) {

        System.out.println(pathName);
        if(replicationDegree > MAX_REPLICATION_DEGREE) {
            Logs.logError("Maximum replication Degree reached!");
            return;
        }

        this.backup = new Backup(this, pathName, replicationDegree);
        this.backup.startPutChunkProcedure();

        // stores the initiator peer to use in conjunction with the reclaim protocol
        this.initiatorBackupInfo.put(this.backup.getFileId(), "1");
        this.saveMap(INITIATOR_BACKUP_INFO_PATH, this.initiatorBackupInfo);

        // Stores backup protocol to be used in state()
        this.backupInfo.put(this.backup.getFileId(), this.backup);
    }

    public void restore(String pathname) {
        this.restore = new Restore(this, pathname);
        this.restore.startRestoreFileProcedure();
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


    public void addSentChunkInfo(String fileId, String chunkNo) {
        this.sentChunks.put(fileId + chunkNo, true);
    }


    public boolean hasChunkBeenSent(String fileId, String chunkNo){
        return this.sentChunks.get(fileId + "_" + chunkNo) != null;
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

        if(this.storedChunkHistory.get(storedMessageHistoryId) == null) {
            if (this.repDegreeInfo.get(chunkId) != null) {
                this.storedChunkHistory.put(storedMessageHistoryId, senderId);
                System.out.println(chunkId + "=" + this.repDegreeInfo.get(chunkId));
                this.repDegreeInfo.compute(chunkId, (key, value) -> (Integer.parseInt(value.split("_")[0]) + 1) + "_" + value.split("_")[1]);
                System.out.println(chunkId + "=" + this.repDegreeInfo.get(chunkId));
                this.saveProperties();
            } else {
                this.initiateRepDegreeInfo(message);
            }
        }
    }

    public void initiateRepDegreeInfo(Message message){
        String fileId = message.getHeader().getFileId();
        String senderId = message.getHeader().getSenderId();
        String chunkNo = message.getHeader().getChuckNo();
        String desiredRepDegree = message.getHeader().getReplicationDeg();
        String currentRepDegree;

        if(this.id == Integer.parseInt(senderId)) {
            currentRepDegree = "0";
        }

        else {
            currentRepDegree = "1";
            this.storedChunkHistory.put(this.id + "_" + fileId + "_" + chunkNo, senderId);
        }

        this.repDegreeInfo.put(fileId + "_" + chunkNo, currentRepDegree + "_" + desiredRepDegree);
        saveProperties();

    }

    public void state() {
        for(Map.Entry<String, Backup> entry : this.backupInfo.entrySet()) {
            String fileId = entry.getKey();
            Backup backup = entry.getValue();

            System.out.println("File pathname: " + this.FILE_STORAGE_PATH + "/" + fileId);
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

    public void printMapBytes(Map<String, byte[]> map){
        for (String key : map.keySet()) {
            System.out.println(key);
        }
    }

    public void printMapString(Map<String, String> map){
        for (String key : map.keySet()) {
            System.out.println(key + "-->" + map.get(key));
        }
    }


    public void removeChunkFromSentChunks(String fileId, String chuckNo) {
        this.sentChunks.remove(fileId + "_" + chuckNo);
    }

    public String getVersion() {
        return version;
    }

    public int getId() {
        return id;
    }

    public String getServiceAccessPoint() {
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

    public Map<String, String> getRepDegreeInfo() {
        return this.repDegreeInfo;
    }

    public Map<String, String> getStoredChunkHistory() {
        return this.storedChunkHistory;
    }

    public Map<String, String> getInitiatorBackupInfo() {
        return this.initiatorBackupInfo;
    }


    public int getMaxMemory() {
        return Integer.parseInt(this.memoryInfo.get("max"));
    }

    public void setMaxMemory(int maxMemory) {
        this.memoryInfo.put("max", Integer.toString(maxMemory));
        saveMap(DISK_INFO_PATH, this.memoryInfo);
    }

    public int getUsedMemory() {
        return Integer.parseInt(this.memoryInfo.get("used"));
    }

    public void setUsedMemory(int memoryUsedByChunk){
        this.memoryInfo.compute("used", (key, value) -> Integer.toString(Integer.parseInt(value) + memoryUsedByChunk));
        saveMap(DISK_INFO_PATH, this.memoryInfo);
    }

    public int getAvailableStorage() {
        return this.getMaxMemory() -  this.getUsedMemory();
    }


    public ExecutorService getSenderExecutor() {
        return this.senderExecutor;
    }

    public ExecutorService getReceiverExecutor() {
        return this.receiverExecutor;
    }

    public Semaphore getMutex() {
        return this.mutex;
    }

    public static void main(String[] args) throws IOException {
        String serviceAccessPoint = "sda";
        String[] mcAddress = {"224.0.0.0", "4445"};
        String[] mdbAddress = {"224.0.0.1", "4446"};
        //String[] mdbAddress = {"224.0.0.1", "4446"};
        String[] mdrAddress = {"224.0.0.2", "4447"};

        if(args[0].equals("1")) {
            Peer peer1 = new Peer("1", "1", serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);
            //peer1.backup( peer1.FILE_STORAGE_PATH + "/Teste.txt", 1);
            peer1.delete( peer1.FILE_STORAGE_PATH + "/Teste.txt");
        }
        else if(args[0].equals("2")) {
            Peer peer2 = new Peer("1", "2", serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);
            //peer2.reclaim(0);
        }
        else if(args[0].equals("3")){
            Peer peer3 = new Peer("1", "3", serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);
            //peer3.delete(peer3.FILE_STORAGE_PATH + "/Teste.txt");
        }
        else {
            Peer peer4 = new Peer("1", "4", serviceAccessPoint, mcAddress, mdbAddress, mdrAddress);
        }
    }

}