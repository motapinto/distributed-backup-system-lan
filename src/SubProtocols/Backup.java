package SubProtocols;

import Common.Logs;
import Peer.Peer;
import Message.*;
import Common.Utilities;
import static Common.Constants.*;

import java.io.*;

public class Backup {
    private Peer peer;
    private int desiredRepDeg;
    private String pathName;
    private String fileId;
    private int senderId;

    /**
     * Creates backup protocol
     *
     * @param peer : peer that creates backup protocol
     */
    public Backup(Peer peer){
        this.peer = peer;
        this.senderId = this.peer.getId();
        this.pathName = null;
    }

    /**
     * Responsible for backing up a file
     *
     * @param peer          : peer listening to the multicast
     * @param desiredRepDeg : desired replication degree of the file
     */
    public Backup(Peer peer, String pathName, int desiredRepDeg){
        this.peer = peer;
        this.senderId = this.peer.getId();
        this.desiredRepDeg = desiredRepDeg;
        this.pathName = pathName;
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

        int numNecessaryChunks = (int)Math.ceil((double)file.length() / MAX_CHUNK_SIZE);

        if(numNecessaryChunks > MAX_NUM_CHUNKS) {
            Logs.logError("File can only have ...");
            return;
        }

        byte[] chunk;
        int bytesRead = 0;
        for (int chuckNo = 0; chuckNo < numNecessaryChunks; chuckNo++) {
            chunk = new byte[MAX_CHUNK_SIZE];
            try {
                assert inputFile != null;
                bytesRead = inputFile.read(chunk);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(chuckNo == numNecessaryChunks - 1 && bytesRead < MAX_CHUNK_SIZE) {
                byte[] tmp = new byte[bytesRead];
                System.arraycopy(chunk, 0, tmp, 0, bytesRead);
                this.sendPutChunkMessage(tmp, chuckNo, this.fileId);
            }
            else
                this.sendPutChunkMessage(chunk, chuckNo, this.fileId);
        }

        try {
            assert inputFile != null;
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
        Message request = new Message(PUTCHUNK, this.peer.getVersion(), Integer.toString(this.senderId),
                fileId, Integer.toString(chunkNo), Integer.toString(this.desiredRepDeg), chunk);

        this.peer.initiateRepDegreeInfo(request);
        Dispatcher dispatcher = new Dispatcher(this.peer, request, this.peer.getBackupChannel());

        int sleepTime = 1000;
        String repDegString = this.peer.getRepDegreeInfo(request.getHeader().getFileId(), Integer.toString(chunkNo), true);
        int repDeg =  Integer.parseInt(repDegString);

        for(int tries = 1; repDeg < this.desiredRepDeg && tries <= PUTCHUNK_RETRIES; tries++, sleepTime *= 2) {
            System.out.println("tries: " + tries);
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

        if(this.peer.getInitiatorBackupInfo().get(message.getHeader().getFileId()) != null)
            return;

        this.peer.initiateRepDegreeInfo(message);
        this.sendStoredMessage(message);

        String pathName = this.peer.FILE_STORAGE_PATH + "/" + message.getHeader().getFileId()
                + "/" + message.getHeader().getChuckNo();

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
        this.peer.setUsedMemory(message.getBody().length);
    }

    /**
     * Sends STORED message for a chunk
     * @param message : message with request
     */
    public void sendStoredMessage(Message message) {
        try {
            Thread.sleep((long)Math.random() * MAX_DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Message reply = new Message(STORED, this.peer.getVersion(), Integer.toString(this.peer.getId()),
                message.getHeader().getFileId(), message.getHeader().getChuckNo());

        Dispatcher dispatcher = new Dispatcher(this.peer, reply, this.peer.getControlChannel());
        this.peer.getSenderExecutor().submit(dispatcher);
    }

    public String getFileId() {
        return this.fileId;
    }

    public int getDesiredRepDeg() {
        return this.desiredRepDeg;
    }

    public Peer getPeer() {
        return this.peer;
    }
}