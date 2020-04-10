package SubProtocols;

import java.io.File;
import java.util.Map;

import Common.Utilities;
import Message.Message;
import Peer.Peer;
import Message.Dispatcher;
import static Common.Constants.DELETE;
import static Common.Constants.MAX_DELAY;

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
        this.sendDeleteMessage();
        this.deleteFile(this.peer.getId(), this.fileId);
    }

    /**
     * Encodes fileId and stores it
     */
    public void setFileId() {
        File file = new File(this.pathName);
        this.fileId = Utilities.hashAndEncode(file.getName() + file.lastModified() + file.length());
    }

    /**
     * Sends a DELETE message to all peers
     */
    public void sendDeleteMessage() {
        Message request = new Message(DELETE, this.peer.getVersion(), Integer.toString(this.peer.getId()), this.fileId);
        // 5 tries to make sure the message gets to all peers
        for(int i = 0; i < 3; i++) {

            Dispatcher dispatcher = new Dispatcher(this.peer, request, this.peer.getControlChannel());
            this.peer.getSenderExecutor().submit(dispatcher);
            try {
                Thread.sleep(MAX_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes all chunks from a file if original copy or removes entirely the original copy of the file
     * depending on the received peer
     */
    public void deleteFile(int peerId, String fileId) {

        Map<String, String> repDegreeInfo = peer.getRepDegreeInfo();
        Map<String, String> storedHistory = peer.getStoredChunkHistory();

        for(Map.Entry<String, String> entry : repDegreeInfo.entrySet()) {
            String key = entry.getKey();
            if(key.split("_")[0].equals(fileId)) {
                repDegreeInfo.remove(key);
                peer.saveMap(peer.REPLICATION_DEGREE_INFO_PATH, repDegreeInfo);
            }
        }

        File file;
        /* String(KEY) : "senderId_fileId_chuckNo"   |   String(VALUE) : "senderId" */
        for(Map.Entry<String, String> entry : storedHistory.entrySet()) {
            String key = entry.getKey();
            if(key.split("_")[1].equals(fileId)) {
                file =  new File(this.peer.FILE_STORAGE_PATH + "/" + key.split("_")[1] + "/" + key.split("_")[2]);

                try {
                    this.peer.getMutex().acquire();
                    if(file.exists()) {
                        this.peer.setUsedMemory(-1 * (int) file.length());
                        System.out.println("used memory: " + this.peer.getUsedMemory() + " -- size of the chunk: " + file.length());
                        file.delete();
                        storedHistory.remove(key);
                        peer.saveMap(peer.STORED_CHUNK_HISTORY_PATH, storedHistory);
                    }
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
