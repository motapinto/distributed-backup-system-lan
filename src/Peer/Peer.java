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

    public String FILE_STORAGE_PATH;
    public String REPLICATION_DEGREE_INFO_PATH;
    public String DISK_INFO_PATH ;
    public String STORED_CHUNK_HISTORY_PATH;
    public String INITIATOR_BACKUP_INFO_PATH;
    public String DELETE_ENHANCEMENT_INFO_PATH;

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
     * Holds information regarding each chuck id and the corresponding current and desired replication degree
     * String(value) is a pair of fileId_chuckNo
     * String(key) is a pair of repDegree_desiredRepDegree
     */
    private Map<String, String> repDegreeInfo = new ConcurrentHashMap<>();

    /**
     * MUDAR ESTE COMENTARIOOOOOOOOOOOOooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
     * String : "senderId_fileId_chuckNo_size"   |   String : "senderId"
     * if senderId(key) == senderId(value) -> received STORED message
     * else {
     *      senderId from the key: senderId that send STORED message
     *      senderId from the value: senderId that receives STORED message
     *  }
     */
    private Map<String, String> storedChunkHistory = new ConcurrentHashMap<>();

    /**
     * ConcurrentHashMap used in state() to store all backup information (only saves current state)
     */
    private Map<String, Backup> backupInfo = new ConcurrentHashMap<>();

    /**
     * ConcurrentHashMap used to store the initiator of the backup (need to be stored in non-volatile memory)
     */
    private Map<String, String> initiatorBackupInfo = new ConcurrentHashMap<>();

    /**
     * ConcurrentHashMap used to store the memory info
     */
    private Map<String, String> memoryInfo = new ConcurrentHashMap<>();

    /**
     * ConcurrentHashMap used to store delete messages
     */
    private Map<String, String> deleteHistory = new ConcurrentHashMap<>();

    public Peer(String version, String id, String[] mcAddress, String[] mdbAddress, String[] mdrAddress) throws IOException {
        super();

        this.version = version;
        this.id = Integer.parseInt(id);

        FILE_STORAGE_PATH = "storage" + this.id;
        REPLICATION_DEGREE_INFO_PATH = FILE_STORAGE_PATH + "/replicationDegreeInfo.properties";
        DISK_INFO_PATH = FILE_STORAGE_PATH + "/diskInfo.properties";
        STORED_CHUNK_HISTORY_PATH = FILE_STORAGE_PATH + "/storedChunkHistory.properties";
        INITIATOR_BACKUP_INFO_PATH = FILE_STORAGE_PATH + "/initiatorBackupInfo.properties";
        DELETE_ENHANCEMENT_INFO_PATH = FILE_STORAGE_PATH + "/deleteHistory.properties";

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
                Logs.logError("Error creating directory storage for peer " + this.id);
        }
    }

    /**
     * Reads all properties files
     * A .properties file is a simple collection of KEY-VALUE pairs that can be parsed by the java.util.Properties class.
     */
    private void readProperties() {
        readMap(STORED_CHUNK_HISTORY_PATH, this.storedChunkHistory);
        this.constructRepDegreeInfo();
        readMap(INITIATOR_BACKUP_INFO_PATH, this.initiatorBackupInfo);
        readMap(DELETE_ENHANCEMENT_INFO_PATH, this.deleteHistory);

        if(!readMap(DISK_INFO_PATH, this.memoryInfo)){
            this.memoryInfo.put("used", "0");
            this.memoryInfo.put("max", Integer.toString(INITIAL_MAX_MEMORY));
            this.saveMap(DISK_INFO_PATH, this.memoryInfo);
        }
    }

    public void constructRepDegreeInfo(){
        this.storedChunkHistory.forEach((key, value) -> {
            String fileId = key.split("_")[1];
            String chunkNo = key.split("_")[2];
            String chunkId = fileId + chunkNo;

            if(this.repDegreeInfo.containsKey(chunkId)){

            }
            else{

            }
        });


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

    private void setupExecutors() {
        this.senderExecutor = Executors.newFixedThreadPool(5);
        this.receiverExecutor = Executors.newFixedThreadPool(10);
    }

    private void createProtocols() {
        this.backup = new Backup(this);
        this.delete = new Delete(this);
        this.restore = new Restore(this);
        this.reclaim = new SpaceReclaim(this);
    }

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

    public void saveMap(String path, Map map) {
        Properties properties = new Properties();

        map.forEach((key, value) -> {
            properties.put(key, value);
        });

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            properties.store(fileOutputStream, null);
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        // stores the initiator peer to use in conjunction with the reclaim protocol
        this.initiatorBackupInfo.put(this.backup.getFileId(), "1");
        this.saveMap(INITIATOR_BACKUP_INFO_PATH, this.initiatorBackupInfo);

        // Stores backup protocol to be used in state()
        this.backupInfo.put(this.backup.getFileId(), this.backup);
    }

    /**
     * The client shall specify the file to restore by its pathname
     * @param pathname pathname of the file to restore
     */
    public void restore(String pathname) {
        this.restore = new Restore(this, pathname);
        this.restore.startRestoreFileProcedure();
    }

    /**
     * The client shall specify the file to delete by its pathname
     * @param pathname pathname of the file to delete
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

        if(this.repDegreeInfo.containsKey(chunkId))
            return this.repDegreeInfo.get(chunkId).split("_")[index];
        else
            return null;
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
     * or STORED message received from other p+errs who have stored the chunk
     */
    public void updateRepDegreeInfo(Message message, boolean increment) {

        String fileId = message.getHeader().getFileId();
        String chunkNo = message.getHeader().getChuckNo();
        String chunkId = fileId + "_" + chunkNo;
        String senderId = message.getHeader().getSenderId();
        String storedMessageHistoryId = senderId + "_" + chunkId;

        if(!increment){
            String repDegInf = this.repDegreeInfo.get(chunkId);
            Integer newValue = Integer.parseInt(repDegInf.split("_")[0]) - 1;
            this.repDegreeInfo.put(chunkId, newValue + "_" + repDegInf.split("_")[1]);
            this.storedChunkHistory.remove(storedMessageHistoryId);
            this.saveMap(STORED_CHUNK_HISTORY_PATH, this.storedChunkHistory);
            this.saveMap(REPLICATION_DEGREE_INFO_PATH, this.repDegreeInfo);
            return;
        }

        if(this.storedChunkHistory.get(storedMessageHistoryId) == null) {

            this.storedChunkHistory.put(storedMessageHistoryId, senderId);




            /* if (this.repDegreeInfo.get(chunkId) != null) {
                this.storedChunkHistory.put(storedMessageHistoryId, senderId);
                if(message.getHeader().getMessageType().equals(PUTCHUNK)){
                    this.repDegreeInfo.compute(chunkId, (key, value) -> (Integer.parseInt(value.split("_")[0]) + 1) + "_" + message.getHeader().getReplicationDeg());
                }
                else{
                    this.repDegreeInfo.compute(chunkId, (key, value) -> (Integer.parseInt(value.split("_")[0]) + 1) + "_" + value.split("_")[1]);
        }
                this.saveMap(REPLICATION_DEGREE_INFO_PATH, this.repDegreeInfo);
                this.saveMap(STORED_CHUNK_HISTORY_PATH, this.storedChunkHistory);
            } else {
                this.initiateRepDegreeInfo(message);
            }*/
        }
    }

    public void initiateRepDegreeInfo(Message message){
        String fileId = message.getHeader().getFileId();
        String senderId = message.getHeader().getSenderId();
        String chunkNo = message.getHeader().getChuckNo();
        String desiredRepDegree = message.getHeader().getReplicationDeg();
        String currentRepDegree;

        if(this.id == Integer.parseInt(senderId))
            currentRepDegree = "0";

        else {
            currentRepDegree = "1";
            this.storedChunkHistory.put(this.id + "_" + fileId + "_" + chunkNo, senderId);
            this.saveMap(STORED_CHUNK_HISTORY_PATH, this.storedChunkHistory);
        }

        if(desiredRepDegree == null)
            desiredRepDegree = currentRepDegree;

        this.repDegreeInfo.put(fileId + "_" + chunkNo, currentRepDegree + "_" + desiredRepDegree);
        this.saveMap(REPLICATION_DEGREE_INFO_PATH, this.repDegreeInfo);
    }

    public void state() {
        System.out.println("\nInformation about each file whose backup it has initiated: ");
        if(this.backupInfo.size() == 0) System.out.println("\tNone\n");

        for(Map.Entry<String, Backup> entry : this.backupInfo.entrySet()) {
            String fileId = entry.getKey();
            Backup backup = entry.getValue();

            System.out.println("\tFile pathname: " + backup.getPathname());
            System.out.println("\tBackup service id of the file: " + backup.getPeer().getId());
            System.out.println("\tDesired replication degree: " + backup.getDesiredRepDeg());

            System.out.println("\nInformation about each file chunk:");
            for(Map.Entry<String, String> chunk : this.repDegreeInfo.entrySet()) {
                String chunkId = chunk.getKey();
                String repDegInfo = chunk.getValue();

                if(!chunkId.split("_")[0].equals(fileId)) continue;

                System.out.println("\tID: \t\t\t\t\t\t\t\t" + chunkId);
                System.out.println("\tPerceived replication degree: \t\t" + repDegInfo.split("_")[0]);
                System.out.println("\tDesired replication degree: \t\t" + repDegInfo.split("_")[1]);
            }
        }

        System.out.println("\nInformation about each stored chunk: ");
        boolean hasPrinted = false;
        for(Map.Entry<String, String> entry : this.storedChunkHistory.entrySet()) {
            if(!entry.getKey().split("_")[0].equals(Integer.toString(this.id))) continue;
            if(!hasPrinted) hasPrinted = true;
            String chunkId = entry.getKey().split("_")[1] + "_"+  entry.getKey().split("_")[2];
            System.out.println("\tChunk id: \t\t\t\t\t\t" + chunkId);
            System.out.println("\tChunk size: \t\t\t\t\t" + this.getChunkSize(chunkId));
            System.out.println("\tPerceived replication degree: \t" + entry.getValue().split("_")[0] + "\n");
        }
        if(!hasPrinted) System.out.println("\tNone\n");

        System.out.println("\nInformation about peer storage capacity:");
        System.out.println("\tMaximum amount of disk space that can be used to store chunks: " + this.getMaxMemory() + " KBytes");
        System.out.println("\tAmount of storage used to backup the chunks: " + this.getUsedMemory() / 1000 + " KBytes");
    }

    public long getChunkSize(String chunkId) {
        File file = new File(FILE_STORAGE_PATH + "/" + chunkId.split("_")[0] + "/" + chunkId.split("_")[1]);
        return file.length();
    }

    /* Function for enhancement regarding DELETE protocol */
    public void addDeleteHistory(String info, String peerToDelete) {
        this.deleteHistory.put(info, peerToDelete);
        this.saveMap(DELETE_ENHANCEMENT_INFO_PATH, this.deleteHistory);
    }

    /* Function for enhancement regarding DELETE protocol */
    public void removeDeleteHistory(Message message) {
        if(this.deleteHistory.containsKey(message.getHeader().getFileId() + "_" + message.getHeader().getSenderId())) {
            this.deleteHistory.remove(message.getHeader().getFileId() + "_" + message.getHeader().getSenderId());
            this.saveMap(DELETE_ENHANCEMENT_INFO_PATH, this.deleteHistory);
        }
    }

    /* Function for enhancement regarding DELETE protocol */
    public Map<String, String> getDeleteHistory() {
        return this.deleteHistory;
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

    public void removeRepDegreeInfo(String key) {
        if(this.repDegreeInfo.containsKey(key)) {
            this.repDegreeInfo.remove(key);
            this.saveMap(this.REPLICATION_DEGREE_INFO_PATH, this.repDegreeInfo);
        }
    }

    public Map<String, String> getStoredChunkHistory() {
        return this.storedChunkHistory;
    }

    public void removeStoredChunkHistory(String key) {
        if(this.storedChunkHistory.containsKey(key)) {
            this.storedChunkHistory.remove(key);
            this.saveMap(this.STORED_CHUNK_HISTORY_PATH, this.storedChunkHistory);
        }
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
}