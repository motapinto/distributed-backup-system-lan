package SubProtocols;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import Message.Message;
import Message.Dispatcher;
import Peer.Peer;

import static Common.Constants.*;
import static Common.Constants.PUTCHUNK_RETRIES;

public class SpaceReclaim {

    private Peer peer;
    private int sizeToReclaim;

    /**
     * Creates restore protocol
     *
     * @param peer : peer that creates delete protocol
     */
    public SpaceReclaim(Peer peer) {
        this.peer = peer;
        this.sizeToReclaim = 0;
    }

    /**
     * Responsible for reclaiming a file
     *
     * @param peer         : peer listening to the multicast
     * @param maxDiskSpace : maximum size that peer allocates for storing chunks in KB
     */
    public SpaceReclaim(Peer peer, int maxDiskSpace) {
        this.peer = peer;

        if(this.peer.getUsedMemory() > (maxDiskSpace * 1000))
            this.sizeToReclaim = this.peer.getUsedMemory() - (maxDiskSpace * 1000);
        else
            this.sizeToReclaim = 0;

        this.peer.setMaxMemory(maxDiskSpace);
    }

    /**
     * Starts the restore protocol
     */
    public void startSpaceReclaimProcedure() {
        if(this.sizeToReclaim > 0)
            this.delete(true);

        if(this.sizeToReclaim > 0)
            this.delete(false);
    }

    /**
     * Initiates the delete of a chunk
     * @param firstTask : if true -> delete chunks with currRepDeg > desRepDeg
     *                    if false -> delete any chunk
     */
    public void delete(boolean firstTask) {
        ConcurrentHashMap<String, String> repDegreeInfo = this.peer.getRepDegreeInfo();
        ConcurrentHashMap<String, String> storedHistory = this.peer.getStoredChunkHistory();

        /* String(KEY) : "senderId_fileId_chuckNo"   |   String(VALUE) : "senderId" */
        for(Map.Entry<String, String> entry : storedHistory.entrySet()) {
            String rawKey = entry.getKey();

            if(this.sizeToReclaim <= 0) return;

            // chunkId = fileId + "_" + chunkNo
            String chunkId = rawKey.split("_")[1] + "_" + rawKey.split("_")[2];

            // senderId_fileId_chunkNo
            int peerStorer = Integer.parseInt(rawKey.split("_")[0]);

            String currRepDeg = repDegreeInfo.get(chunkId).split("_")[0];
            String desRepDeg = repDegreeInfo.get(chunkId).split("_")[1];

            // if current replication degree is greater than the desired replication degree
            if(firstTask && peerStorer == this.peer.getId() && (Integer.parseInt(currRepDeg) > Integer.parseInt(desRepDeg)))
                this.deleteChunk(chunkId, entry.getValue());
            else if(!firstTask && peerStorer == this.peer.getId())
                this.deleteChunk(chunkId, entry.getValue());
        }
    }

    /**
     * Deletes a chunk with specified chunkId
     * @param chunkId : fileId + "_" + chunkNo
     */
    public void deleteChunk(String chunkId, String peerOriginal) {
        String fileId = chunkId.split("_")[0];
        String chunkNo = chunkId.split("_")[1];
        String pathName = Peer.FILE_STORAGE_PATH + "/" + peerOriginal + "/" + fileId + "/" + chunkNo;

        int chunkSize = 0;
        File file = new File(pathName);
        if(file.exists()) {
            chunkSize = (int) file.length();
            file.delete();
            if(file.getParentFile().length() == 0)
                file.getParentFile().delete();
        } else {
            file.getParentFile().delete();
        }

        this.sendRemovedMessage(chunkNo, fileId);

        for(Map.Entry<String, String> entry : this.peer.getStoredChunkHistory().entrySet()) {
            String key = entry.getKey();
            if(this.peer.getId() == Integer.parseInt(key.split("_")[0]))
                if(key.split("_")[1].equals(fileId) && key.split("_")[2].equals(chunkNo))
                    this.peer.getStoredChunkHistory().remove(key);

        }

        this.peer.setUsedMemory(this.peer.getUsedMemory() - chunkSize);
        this.sizeToReclaim -= chunkSize;
    }

    /**
     * Sends a REMOVED message to all peers
     */
    public void sendRemovedMessage(String chuckNo, String fileId) {
        Message reply = new Message(REMOVED, this.peer.getVersion(), Integer.toString(this.peer.getId()), fileId, chuckNo);

        Dispatcher dispatcher = new Dispatcher(this.peer, reply, this.peer.getControlChannel());
        this.peer.getSenderExecutor().submit(dispatcher);

        this.peer.updateRepDegreeInfo(reply, false);
    }

    /**
     * Updates replication degree of the deleted chunks and if the current replication degree
     * is less than the desired one, ot will start the backup protocol
     * @param message : received message from Dispatcher with the REMOVED message
     */
    public void updateChunkRepDegree(Message message) {
        this.peer.updateRepDegreeInfo(message, false);

        String currRepDegree = this.peer.getRepDegreeInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo(), true);
        String desRepDegree = this.peer.getRepDegreeInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo(), false);
        String chunkId = message.getHeader().getFileId() + "_" + message.getHeader().getChuckNo();

        System.out.println("curr: " + currRepDegree);
        System.out.println("desired: " + desRepDegree);
        System.out.println("chunk id: " + chunkId);

        ConcurrentHashMap<String, String> storedHistory = this.peer.getStoredChunkHistory();

        // if it has the chunk stored and the currRepDegree is less than the desRepDegree
        if(storedHistory.get(this.peer.getId() + "_" + chunkId) != null &&
                Integer.parseInt(currRepDegree) < Integer.parseInt(desRepDegree)) {
            this.startBackup(message, Integer.parseInt(desRepDegree));
        }
    }

    /**
     * Starts again the backup protocol for a chunk if the current replication degree
     * is less than the desired one
     * @param message
     * @param desiredRepDegree
     */
    public void startBackup(Message message, int desiredRepDegree) {
        try {
            Thread.sleep((long) (Math.random() * MAX_DELAY));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //IF DURING THIS WAIT.....



        ConcurrentHashMap<String, String> storedHistory = this.peer.getStoredChunkHistory();
        String chunkId = message.getHeader().getFileId() + "_" + message.getHeader().getChuckNo();
        String originalFileSender;

        if(storedHistory.get(this.peer.getId() + "_" + chunkId) != null)
            originalFileSender = storedHistory.get(this.peer.getId() + "_" + chunkId);
        else
            return;

        String pathName = Peer.FILE_STORAGE_PATH + "/" + originalFileSender + "/" +
                message.getHeader().getFileId() + "/" + message.getHeader().getChuckNo();

        File file = new File(pathName);
        InputStream inputFile = null;
        byte[] chunk = null;

        try {
            inputFile = new FileInputStream(file.getAbsolutePath());
            chunk = inputFile.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //!ALEERT!
        Backup backup = new Backup(this.peer, pathName, desiredRepDegree);
        backup.setSenderId(Integer.parseInt(originalFileSender));
        backup.sendPutChunkMessage(chunk, Integer.parseInt(message.getHeader().getChuckNo()), message.getHeader().getFileId());
    }
}


