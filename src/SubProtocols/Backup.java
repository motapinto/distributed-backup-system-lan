package SubProtocols;

import Peer.Peer;
import Message.*;
import Common.*;
import Common.Utilities;
import static Common.Constants.*;

import java.io.*;

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
     * Splits a file into chunks and for each chunk send a PUTCHUNK message
     */
    public void startPutChunkProcedure() {
        File file = new File(this.pathName);
        this.fileId = Utilities.hashAndEncode(file.getName() + file.lastModified() + file.length());

        InputStream inputFile = null;
        try {
            inputFile = new FileInputStream(file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int numNecessaryChunks =(int)Math.ceil((double)file.length() / MAX_CHUNK_SIZE);

        if(numNecessaryChunks > MAX_NUM_CHUNKS) {
            Logs.logError("File can only have a maximum of 1 million chunks");
            return;
        }

        byte[] chunk = new byte[MAX_CHUNK_SIZE];

        for (int chuckNo = 0; chuckNo < numNecessaryChunks; chuckNo++) {

            try {
                inputFile.readNBytes(chunk, 0, MAX_CHUNK_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.sendPutChunkMessage(chunk, chuckNo, this.fileId);
        }

        try {
            inputFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends PUTCHUNK message for a chunk
     * @param chunk   : received files to separate into chunks
     * @param chunkNo : number of the received chunk
     * @param fileId  : file id of the file
     */
    public void sendPutChunkMessage(byte[] chunk, int chunkNo, String fileId) {
        Message request = new Message(PUTCHUNK, this.peer.getVersion(), Integer.toString(this.peer.getId()),
                fileId, Integer.toString(chunkNo), Integer.toString(this.desiredRepDeg), chunk);

        this.peer.incrementRepDegreeInfo(request);
        Dispatcher dispatcher = new Dispatcher(this.peer, request, this.peer.getBackupChannel());

        int sleepTime = 1000;
        String repDegString = this.peer.getRepDegreeInfo(request.getHeader().getFileId(), Integer.toString(chunkNo), true);
        int repDeg =  Integer.parseInt(repDegString);

        for(int tries = 1; repDeg < this.desiredRepDeg && tries <= 5; tries++, sleepTime *= 2) {

            this.peer.getSenderExecutor().submit(dispatcher);

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            repDegString = this.peer.getRepDegreeInfo(request.getHeader().getFileId(), Integer.toString(chunkNo), true);
            repDeg =  Integer.parseInt(repDegString);
        }
    }

    /**
     * Stores the chunk and sends a STORED message, if the peer has enough memory and does not have that chunk
     *
     * @param message : PUTCHUNK message
     */
    public void startStoredProcedure(Message message) {

        if(this.peer.getAvailableStorage() < message.getBody().length) return;

        this.peer.incrementRepDegreeInfo(message);
        this.sendStoredMessage(message);

        String pathName = Peer.FILE_STORAGE_PATH + "/" + message.getHeader().getSenderId() + "/" +
                message.getHeader().getFileId() + "/" + message.getHeader().getChuckNo();

        File out = new File(pathName);

        // If the chunk is already stored then it does not make anything else
        if(out.exists())
            return;

        if(!out.getParentFile().exists())
            out.getParentFile().mkdirs();

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
}