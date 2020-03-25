package Peer;

import Channels.*;
import Common.Logs;
import Common.Utilities;
import Message.*;
import SubProtocols.*;

import static Common.Constants.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Peer implements PeerInterface{
    public static final String FILE_STORAGE_PATH = "../../storage";
    public static final int MAX_SIZE = 64000000;

    private String version;
    private int id;
    private String[] serviceAccessPoint;
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

    /**
     * String : fileId_chuckNo   |   String : replication degree
     * The necessity of the ConcurrentHashMap comes from the fact that HashMap is not thread-safe, HashTable does not
     * permit concurrency when accessing data (single-lock) and ConcurrentHashMap is more efficient for threaded applications
     */
    private ConcurrentHashMap<String, String> repDegreeInfo = new ConcurrentHashMap<>();

    public Peer(String version, String id, String[] serviceAccessPoint, String[] mcAddress, String[] mdbAddress, String[] mdrAddresss) throws IOException {
        this.version = version;
        this.id = Integer.parseInt(id);
        this.serviceAccessPoint = serviceAccessPoint;
        this.currentSystemMemory = 0; // ?

        this.setupFiles();
        this.readProperties();
        this.setupChannels(mcAddress, mdbAddress, mdrAddresss);
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

        // esta verificacao deve ser feita no inicio
        if(replicationDegree > MAX_REPLICATION_DEGREE) {
            Logs.logError("Maximum replication Degree reached!");
            return;
        }

        String fileName = Paths.get(pathName).getFileName().toString();
        this.backup = new Backup(this, fileName, replicationDegree);

        this.backup.splitFileIntoChunks(pathName);
    }

    public void restore(){

    }

    public void delete(){

    }

    public void reclaim(){

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

    public void increaseRepDegreeInfo(Message message) {
        return;
    }

    public void decreaseRepDegreeInfo(Message message) {

    }

    public int getCurrentSystemMemory() {
        return currentSystemMemory;
    }

    public void setCurrentSystemMemory(int currentSystemMemory) {
        this.currentSystemMemory = currentSystemMemory;
    }
}
