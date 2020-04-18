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


        // stores the initiator peer to use in conjunction with the reclaim protocol
        this.peer.getInitiatorBackupInfo().put(this.fileId, "1");
        this.peer.saveMap(this.peer.INITIATOR_BACKUP_INFO_PATH, this.peer.getInitiatorBackupInfo());

        InputStream inputFile = null;
        try {
            inputFile = new FileInputStream(file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int numNecessaryChunks = (int)Math.ceil((double)file.length() / MAX_CHUNK_SIZE);

        if(numNecessaryChunks > MAX_NUM_CHUNKS) {
            Logs.logError("File can only have a maximum of" + MAX_NUM_CHUNKS + " chunks");
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
     *
     * @param chunk   : received files to separate into chunks
     * @param chunkNo : number of the received chunk
     * @param fileId  : file id of the file
     */
    public void sendPutChunkMessage(byte[] chunk, int chunkNo, String fileId) {
        Message request = new Message(PUTCHUNK, this.peer.getVersion(), Integer.toString(this.senderId),
                fileId, Integer.toString(chunkNo), Integer.toString(this.desiredRepDeg), chunk);

        int sleepTime = 1000;
        String repDegString = this.peer.getRepDegreeInfo(request.getHeader().getFileId(), Integer.toString(chunkNo), true);

        if(repDegString == null) {
            if(!this.peer.getRepDegreeInfo().containsKey(fileId + "_" + chunkNo))
                this.peer.getRepDegreeInfo().put(fileId + "_" + chunkNo, "0_" + this.desiredRepDeg);

            repDegString = this.peer.getRepDegreeInfo(request.getHeader().getFileId(), Integer.toString(chunkNo), true);
        }

        int repDeg =  Integer.parseInt(repDegString);

        Dispatcher dispatcher = new Dispatcher(this.peer, request, this.peer.getBackupChannel());

        for(int tries = 1; repDeg < this.desiredRepDeg && tries <= MESSAGE_RETRIES; tries++, sleepTime *= 2) {
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
     * @param message : PUTCHUNK received message
     */
    public void startStoredProcedure(Message message) {
        if(this.peer.getAvailableStorage() < message.getBody().length)
            return;

        if(this.peer.getInitiatorBackupInfo().get(message.getHeader().getFileId()) != null)
            return;

        if(!this.peer.getVersion().equals("1.0")) {
            try {
                Thread.sleep(MAX_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String currRepDeg = this.peer.getRepDegreeInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo(), true);
            if(currRepDeg != null){
                String desRepDeg = this.peer.getRepDegreeInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo(), false);
                if (Integer.parseInt(currRepDeg) >= Integer.parseInt(desRepDeg))
                    return;
            }
        } else {
            try {
                Thread.sleep((long) Math.random() * MAX_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String chunkId = message.getHeader().getFileId() + "_" + message.getHeader().getChuckNo();

        if(!this.peer.getRepDegreeInfo().containsKey(chunkId)){
            this.peer.getRepDegreeInfo().put(chunkId, "1_" + message.getHeader().getReplicationDeg());
        } else {
            String currentRepDeg = this.peer.getRepDegreeInfo().get(chunkId).split("_")[0];
            this.peer.getRepDegreeInfo().put(chunkId, currentRepDeg + "_" + message.getHeader().getReplicationDeg());
        }

        this.peer.saveMap(this.peer.REPLICATION_DEGREE_INFO_PATH, this.peer.getRepDegreeInfo());
        this.peer.getStoredChunkHistory().put(this.peer.getId() + "_" + chunkId, message.getHeader().getSenderId());
        this.peer.saveMap(this.peer.STORED_CHUNK_HISTORY_PATH, this.peer.getStoredChunkHistory());

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
     *
     * @param message : message with request
     */
    public void sendStoredMessage(Message message) {
        try {
            Thread.sleep((long) (MAX_DELAY * Math.random()));
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

    public String getPathname() {
        return this.pathName;
    }
}