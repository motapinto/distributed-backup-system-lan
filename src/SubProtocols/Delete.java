package SubProtocols;

import java.io.File;
import java.util.Map;

import Message.Message;
import Peer.Peer;
import Message.Dispatcher;
import static Common.Constants.*;
import Common.Utilities;

public class Delete {

    private Peer peer;
    private String pathName;
    private String fileId;

    /**
     * Creates delete protocol
     *
     * @param peer : peer that creates delete protocol
     */
    public Delete(Peer peer) {
        this.peer = peer;
    }

    /**
     * Responsible for deleting a file
     *
     * @param peer          : peer listening to the multicast
     * @param pathname      : pathname to the file
     */
    public Delete(Peer peer, String pathname) {
        this.peer = peer;
        this.pathName = pathname;
    }

    /**
     * Starts the restore protocol
     */
    public void startDeleteProcedure() {
        this.setFileId();
        if(!this.peer.getVersion().equals("1.0"))
            this.addStoredPeers();
        this.sendDeleteMessage();
        this.deleteFile(this.fileId);
    }

    /**
     * Encodes fileId and stores it
     */
    public void setFileId() {
        File file = new File(this.pathName);
        this.fileId = Utilities.hashAndEncode(file.getName() + file.lastModified() + file.length());
    }

    /**
     * Adds to the map all the peers that have to delete the file id
     */
    public void addStoredPeers() {
        Map<String, String> deleteHistory = this.peer.getDeleteHistory();

        this.peer.getStoredChunkHistory().forEach((key, value) -> {
            if(key.split("_")[1].equals(this.fileId) && key.split("_")[0].equals(value))
                if(!deleteHistory.containsKey(this.fileId))
                    this.peer.addDeleteHistory(this.fileId + "_" + value, value);
        });
    }

    /**
     * Checks if a peer that did not listen to a DELETE message of a file id
     * needs to delete some chunks regarding that file Id (Enchancement)
     * @param id
     */
    // info = fileId + "_" + peer id that needs to delete
    public void checkIfPeerNeedsToDelete(String id) {
        if(this.peer.getDeleteHistory().containsValue(id)) {
            this.peer.getDeleteHistory().forEach((key, value) -> {
                if(value.equals(id)) {
                    String fileId = key.split("_")[0];
                    Message request = new Message(DELETE, "1.1", Integer.toString(this.peer.getId()), fileId);
                    for(int i = 0; i < MESSAGE_RETRIES; i++) {
                        Dispatcher dispatcher = new Dispatcher(this.peer, request, this.peer.getControlChannel());
                        this.peer.getSenderExecutor().submit(dispatcher);
                        try {
                            Thread.sleep((long) (MAX_DELAY * Math.random()));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    /**
     * Sends a DELETE message to all peers
     */
    public void sendDeleteMessage() {
        Message request = new Message(DELETE, this.peer.getVersion(), Integer.toString(this.peer.getId()), this.fileId);
        // 3 tries to make sure the message gets to all peers
        for(int i = 0; i < MESSAGE_RETRIES; i++) {
            Dispatcher dispatcher = new Dispatcher(this.peer, request, this.peer.getControlChannel());
            this.peer.getSenderExecutor().submit(dispatcher);
            try {
                Thread.sleep((long) (MAX_DELAY * Math.random()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a DELETEACK message to all peers
     */
    public void sendDeleteAckMessage(String fileId, String destId) {
        Message ack = new Message(DELETEACK, this.peer.getVersion(), Integer.toString(this.peer.getId()), fileId, destId, true);

        for(int i = 0; i < MESSAGE_RETRIES; i++) {
            Dispatcher dispatcher = new Dispatcher(this.peer, ack, this.peer.getControlChannel());
            this.peer.getSenderExecutor().submit(dispatcher);
            try {
                Thread.sleep((long) (MAX_DELAY * Math.random()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes all chunks from a file if original copy or removes entirely the original copy of the file
     * depending on the received peer
     */
    public void deleteFile(String fileId) {

        Map<String, String> repDegreeInfo = peer.getRepDegreeInfo();
        Map<String, String> storedHistory = peer.getStoredChunkHistory();

        for(Map.Entry<String, String> entry : repDegreeInfo.entrySet()) {
            String key = entry.getKey();
            if(key.split("_")[0].equals(fileId)) {
                this.peer.removeRepDegreeInfo(key);
            }
        }

        File file;
        /* String(KEY) : "senderId_fileId_chuckNo"   |   String(VALUE) : "senderId" */
        for(Map.Entry<String, String> entry : storedHistory.entrySet()) {
            String key = entry.getKey();
            if(key.split("_")[1].equals(fileId)) {
                file =  new File(this.peer.FILE_STORAGE_PATH + "/" + fileId + "/" + key.split("_")[2]);

                try {
                    this.peer.getMutex().acquire();
                    if(file.exists()) {
                        this.peer.setUsedMemory(-1 * (int) file.length());
                        file.delete();
                    }
                    this.peer.removeStoredChunkHistory(key);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    this.peer.getMutex().release();
                }
            }
        }

        File folder = new File(this.peer.FILE_STORAGE_PATH + "/" + fileId);
        folder.delete();
        this.peer.getInitiatorBackupInfo().remove(fileId);
    }
}