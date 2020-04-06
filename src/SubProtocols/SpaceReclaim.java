package SubProtocols;

import Message.Message;
import Message.Dispatcher;
import Peer.Peer;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static Common.Constants.MAX_CHUNK_SIZE;
import static Common.Constants.REMOVED;

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
     * @param peer          : peer listening to the multicast
     * @param sizeToReclaim : size that peer intends to reclaim
     */
    public SpaceReclaim(Peer peer, int sizeToReclaim) {
        this.peer = peer;
        this.sizeToReclaim = sizeToReclaim;
    }

    /**
     * Starts the restore protocol
     */
    public void startSpaceReclaimProcedure() {

        if(this.sizeToReclaim > this.peer.getCurrentSystemMemory()) {
            this.sizeToReclaim = this.peer.getCurrentSystemMemory();
        }

        this.peer.setCurrentSystemMemory(this.peer.getCurrentSystemMemory() - this.sizeToReclaim);

        if(this.sizeToReclaim > 0)
            this.delete(true);

        if(this.sizeToReclaim > 0)
            this.delete(false);
    }

    /**
     *
     * @param firstTask : if true -> delete chunks with currRepDeg > desRepDeg
     *                    if false -> delete any chunk
     */
    public void delete(boolean firstTask) {
        ConcurrentHashMap<String, String> repDegreeInfo = peer.getRepDegreeInfo();
        ConcurrentHashMap<String, String> storedHistory = peer.getStoredChunkHistory();

        /* String(KEY) : "senderId_fileId_chuckNo"   |   String(VALUE) : "senderId" */
        for(Map.Entry<String, String> entry : storedHistory.entrySet()) {
            String rawKey = entry.getKey();

            // chunkId = fileId + "_" + chunkNo
            String chunkId = rawKey.split("_")[1] + "_" + rawKey.split("_")[2];
            int peerStorer = Integer.parseInt(rawKey.split("_")[0]);

            this.peer.printMap(repDegreeInfo);
            String currRepDeg = repDegreeInfo.get(chunkId).split("_")[0];
            String desRepDeg = repDegreeInfo.get(chunkId).split("_")[1];

            // if current replication degree is greater than the desired replication degree
            if(firstTask && peerStorer == this.peer.getId() && (Integer.parseInt(currRepDeg) > Integer.parseInt(desRepDeg)) && this.sizeToReclaim > 0)
                this.deleteChunk(chunkId, entry.getValue());
            else if(!firstTask && this.sizeToReclaim > 0)
                this.deleteChunk(chunkId, entry.getValue());
        }
    }

    /**
     *
     * @param chunkId : fileId + "_" + chunkNo
     */
    public void deleteChunk(String chunkId, String peerOriginal) {
        String fileId = chunkId.split("_")[0];
        String chunkNo = chunkId.split("_")[1];
        String pathName = Peer.FILE_STORAGE_PATH + "/" + peerOriginal + "/" + fileId + "/" + chunkNo;

        System.out.println("chunkId: " + chunkId + " peerOriginal:" + peerOriginal+ " space to reclaim: " + this.sizeToReclaim );

        File file = new File(pathName);
        int chunkSize = 0;
        if(file.exists()) {
            chunkSize = (int) file.length();
            file.delete();
        }

        Message reply = this.sendRemovedMessage(chunkNo, fileId);
        this.peer.updateRepDegreeInfo(reply, false);
        this.peer.setCurrentSystemMemory(this.peer.getCurrentSystemMemory() - chunkSize);
        this.sizeToReclaim -= chunkSize;
    }

    /**
     * Sends a REMOVED message to all peers
     */
    public Message sendRemovedMessage(String chuckNo, String fileId) {
        Message reply = new Message(REMOVED, this.peer.getVersion(), Integer.toString(this.peer.getId()), fileId, chuckNo);

        Dispatcher dispatcher = new Dispatcher(this.peer, reply, this.peer.getControlChannel());
        this.peer.getSenderExecutor().submit(dispatcher);

        return reply;
    }

    /**
     *
     * @param message
     */
    public void updateChunkRepDegree(Message message) {
        this.peer.updateRepDegreeInfo(message, false);

        String currRepDegree = this.peer.getRepDegreeInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo(), true);
        String desRepDegree = this.peer.getRepDegreeInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo(), false);
        String chunkId = message.getHeader().getFileId() + "_" + message.getHeader().getChuckNo();

        ConcurrentHashMap<String, String> storedHistory = this.peer.getStoredChunkHistory();

        /* String(KEY) : "senderId_fileId_chuckNo"   |   String(VALUE) : "senderId" */
        for(Map.Entry<String, String> entry : storedHistory.entrySet()) {

            String rawKey = entry.getKey();
            int peerStorer = Integer.parseInt(rawKey.split("_")[0]);
            String entryChunkId = rawKey.split("_")[1] + rawKey.split("_")[2];

            // if it has the chunk stored and the currRepDegree is less than the desRepDegree
            if(peerStorer == this.peer.getId() && entryChunkId.equals(chunkId) && Integer.parseInt(currRepDegree) < Integer.parseInt(desRepDegree))
                this.startBackup(message, Integer.parseInt(desRepDegree));
        }
    }

    public void startBackup(Message message, int desiredRepDegree) {
        try {
            Thread.sleep((long) (Math.random()*400));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String pathName = Peer.FILE_STORAGE_PATH + "/" + message.getHeader().getSenderId() + "/" +
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

        this.peer.getBackup().setDesiredRepDeg(desiredRepDegree);
        this.peer.getBackup().setFileId(message.getHeader().getFileId());
        this.peer.getBackup().sendPutChunkMessage(chunk, Integer.parseInt(message.getHeader().getChuckNo()), message.getHeader().getFileId());
    }
}


