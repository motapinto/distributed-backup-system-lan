package SubProtocols;

import Message.Message;
import Message.Dispatcher;
import Peer.Peer;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public void startSpaceReclaimProcedure() {

        if(this.sizeToReclaim > this.peer.getCurrentSystemMemory()) {
            this.sizeToReclaim = this.peer.getCurrentSystemMemory();
        }

        this.peer.setCurrentSystemMemory(this.peer.getCurrentSystemMemory() - this.sizeToReclaim);

        this.delete(true);
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
            String chunkId = rawKey.split("_")[1] + rawKey.split("_")[2];
            int peerStorer = Integer.parseInt(rawKey.split("_")[0]);

            String currRepDeg = repDegreeInfo.get(chunkId).split("_")[0];
            String desRepDeg = repDegreeInfo.get(chunkId).split("_")[1];

            // if current replication degree is greater than the desired replication degree
            if(firstTask && peerStorer == this.peer.getId() && (Integer.parseInt(currRepDeg) > Integer.parseInt(desRepDeg)))
                this.deleteChunk(chunkId, entry.getValue());
            else if(!firstTask)
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

        File file = new File(pathName);
        if(file.exists()) {
            int chunkSize = (int) file.length();
            file.delete();
        }

        this.sendRemovedMessage(chunkNo, fileId);
    }

    /**
     * Sends a REMOVED message to all peers
     */
    public void sendRemovedMessage(String chuckNo, String fileId) {
        Message reply = new Message(REMOVED, this.peer.getVersion(), Integer.toString(this.peer.getId()), fileId, chuckNo);

        Dispatcher dispatcher = new Dispatcher(this.peer, reply, this.peer.getControlChannel());
        this.peer.getSenderExecutor().submit(dispatcher);
    }


}


