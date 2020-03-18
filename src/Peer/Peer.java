package Peer;

import Channels.*;
import SubProtocols.Backup;

import static Common.Constants.*;


public class Peer implements PeerInterface{
    public static final String FILE_STORAGE_PATH = "../../storage";

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
        Backup backup = new Backup();
        backup....()

    }

    public void restore(){


    }


    public void delete(){


    }

    public void reclaim(){

    }

    public int getAvailableStorage() {
        return MAX_CHUNK_SIZE -  this.currentSystemMemory;
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

    public int getCurrentSystemMemory() {
        return currentSystemMemory;
    }

    public void setCurrentSystemMemory(int currentSystemMemory) {
        this.currentSystemMemory = currentSystemMemory;
    }
}
