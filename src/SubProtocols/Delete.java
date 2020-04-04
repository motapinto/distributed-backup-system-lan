package SubProtocols;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import Common.Utilities;
import Message.Message;
import Peer.Peer;
import Message.Dispatcher;
import static Common.Constants.DELETE;

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
        for(int i = 0; i < 5; i++) {
            Dispatcher dispatcher = new Dispatcher(this.peer, request, this.peer.getControlChannel());
            this.peer.getSenderExecutor().submit(dispatcher);
        }
    }

    /**
     * Deletes all chunks from a file if original copy or removes entirely the original copy of the file
     * depending on the received peer
     */
    public void deleteFile(int peerId, String fileId) {

        ConcurrentHashMap<String, String> repDegreeInfo = peer.getRepDegreeInfo();
        ConcurrentHashMap<String, String> storedHistory = peer.getStoredChunkHistory();

        /* String(KEY) : "fileId_chuckNo"   |   String(VALUE) : "repDegree_desiredRepDegree" */
        for(Map.Entry<String, String> entry : repDegreeInfo.entrySet()) {
            String key = entry.getKey();
            if(key.split("_")[0].equals(fileId)) {
                repDegreeInfo.remove(key);
                peer.saveMap(peer.REPLICATION_DEGREE_INFO_PATH, repDegreeInfo);
            }
        }

        /* String(KEY) : "senderId_fileId_chuckNo"   |   String(VALUE) : "senderId" */
        for(Map.Entry<String, String> entry : storedHistory.entrySet()) {
            String key = entry.getKey();
            if(key.split("_")[1].equals(fileId)) {
                storedHistory.remove(key);
                peer.saveMap(peer.STORED_CHUNK_HISTORY_PATH, storedHistory);
            }
        }

        if(this.peer.getId() != peerId) {
            File folder = new File(Peer.FILE_STORAGE_PATH + "/" + peerId + "/" + fileId);

            File[] files = folder.listFiles();
            for(File file : files) {
                System.out.println("peer: " +  this.peer.getId() + "  current memory: " + this.peer.getCurrentSystemMemory() + "  chunk size " + (int)file.length());
                this.peer.setCurrentSystemMemory(this.peer.getCurrentSystemMemory() - (int)file.length());
                file.delete();
                System.out.println("length: " + file.length());
            }
            folder.delete();
        }
    }
}
