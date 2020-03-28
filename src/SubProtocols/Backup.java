package SubProtocols;

import Common.Logs;
import Common.Utilities;
import Peer.Peer;
import Message.*;
import java.io.*;

import static Common.Constants.*;

public class Backup {
    private Peer peer;
    private int desiredRepDeg;
    private String pathName;
    private String fileId;

    /**
     * Responsible for backing up a file
     *
     * @param peer          : peer listening to the multicast
     * @param desiredRepDeg : desired replication degree of the file
     */
    public Backup(Peer peer, String pathName, int desiredRepDeg){
        this.peer = peer;
        this.desiredRepDeg = desiredRepDeg;
        this.pathName = pathName;
    }

    /**
     * Creates backup protocol
     *
     * @param peer : peer that creates backup protocol
     */
    public Backup(Peer peer){
        this.peer = peer;
        this.pathName = null;
    }

    /**
     * Stores the chunk and sends a STORED message, if the peer has enough memory and does not have that chunk
     *
     * @param message : PUTCHUNK message
     */
    public void startStoredProcedure(Message message) {

        if(this.peer.getAvailableStorage() < message.getBody().length) return;

        this.sendStoredMessage(message);

        String pathName = Peer.FILE_STORAGE_PATH + "/" + message.getHeader().getSenderId() + "/" +
                message.getHeader().getFileId() + "/" + message.getHeader().getChuckNo();

        File out = new File(pathName);

        // If the chunk is already stored then it does not make anything else
        if(out.exists()) return;

        // If the directory Storage/SenderId/FileId does not exist creates it
        if(!out.getParentFile().exists()) out.getParentFile().mkdirs();

        // Writes chunk into file
        try {
            out.createNewFile();

            FileOutputStream fos = new FileOutputStream(pathName);
            fos.write(message.getBody());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Updates current system memory of the peer
        this.peer.setCurrentSystemMemory(this.peer.getCurrentSystemMemory() + message.getBody().length);

        // Updates replication degree of the chunk
        this.peer.setRepDegreeInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo(), Integer.parseInt(message.getHeader().getReplicationDeg()));
    }

    /**
     * Sends STORED message for a chunk
     * @param message : message with request
     */
    public void sendStoredMessage(Message message) {
        try {
            Thread.sleep((long)Math.random() * 400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Message reply = new Message(STORED, this.peer.getVersion(), Integer.toString(this.peer.getId()),
                message.getHeader().getFileId(), message.getHeader().getChuckNo());

        Dispatcher dispatcher = new Dispatcher(this.peer, reply, this.peer.getControlChannel());
        this.peer.getSenderExecutor().submit(dispatcher);
    }

    /**
     * Splits a file into chunks and for each chunk send a PUTCHUNK message
     */
    public void startPutchunkProcedure() throws IOException {

        File file = new File(this.pathName);
        this.fileId = Utilities.hashAndEncode(file.getName() + file.lastModified() + file.length());

        InputStream inputFile = new FileInputStream(file.getAbsolutePath());

        int numNecessaryChunks = (int)Math.ceil(file.length() / MAX_CHUNK_SIZE);
        if(numNecessaryChunks > MAX_NUM_CHUNKS) {
            Logs.logError("File can only have  ");
            return;
        }

        for (int chuckNo = 0; chuckNo < numNecessaryChunks; chuckNo++) {
            byte[] chuck = inputFile.readNBytes(MAX_CHUNK_SIZE);
            this.sendPutChunkMessage(chuck, chuckNo, fileId);
        }

        inputFile.close();
    }

    /**
     * Sends PUTCHUNK message for a chunk
     * @param chunk   : received files to separate into chunks
     * @param chunkNo : number of the received chunk
     * @param fileId  : file id of the file
     */
    public void sendPutChunkMessage(byte[] chunk, int chunkNo, String fileId) {
        Message request = new Message(PUTCHUNK, this.peer.getVersion(), Integer.toString(this.peer.getId()), fileId, Integer.toString(chunkNo), Integer.toString(this.desiredRepDeg));
        request.setBody(chunk);

        Dispatcher dispatcher = new Dispatcher(this.peer, request, this.peer.getBackupChannel());

        int repDeg = 0;
        int tries = 1;
        int sleepTime = 1000;

        while (repDeg < this.desiredRepDeg && tries <= 5) {
            repDeg = this.peer.getRepDegreeInfo(Integer.toString(this.peer.getId()), Integer.toString(chunkNo), true);

            if(repDeg < this.desiredRepDeg) {
                this.peer.getSenderExecutor().submit(dispatcher);
            }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            tries ++;
            sleepTime *= 2;
        }
    }

    public String getFileId() {
        return fileId;
    }
}
