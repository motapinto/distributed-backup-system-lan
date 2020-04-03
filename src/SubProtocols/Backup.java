package SubProtocols;

import Common.Logs;
import Common.Utilities;
import Peer.Peer;
import Message.*;
import java.io.*;
import java.sql.SQLOutput;

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
        if(out.exists()){
            return;
        }
        // If the directory Storage/SenderId/FileId does not exist creates it
        if(!out.getParentFile().exists()) out.getParentFile().mkdirs();

        // Writes chunk into file
        try {
            out.createNewFile();

            FileOutputStream fos = new FileOutputStream(pathName);
            System.out.println(message.getBody().length);
            fos.write(message.getBody());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



        // Updates current system memory of the peer
        this.peer.setCurrentSystemMemory(this.peer.getCurrentSystemMemory() + message.getBody().length);

        // Updates replication degree of the chunk - DOES NOT WORK
        System.out.println("PEER2: before setRepDegreeInfo");
        this.peer.incrementRepDegreeInfo(message, false);
        System.out.println("PEER2: after setRepDegreeInfo");
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

        System.out.println((double)file.length() /MAX_CHUNK_SIZE);
        int numNecessaryChunks =(int)Math.ceil((double)file.length() / MAX_CHUNK_SIZE);
        System.out.println(file.length());
        System.out.println("necessary chunks :" + numNecessaryChunks);
        if(numNecessaryChunks > MAX_NUM_CHUNKS) {
            Logs.logError("File can only have  ");
            return;
        }
        byte[] chunk;
        int bytesRead;
        for (int chuckNo = 0; chuckNo < numNecessaryChunks; chuckNo++) {
            chunk = new byte[MAX_CHUNK_SIZE];
            bytesRead = inputFile.read(chunk);

            if(chuckNo == numNecessaryChunks - 1 && bytesRead < MAX_CHUNK_SIZE) {
                byte[] tmp = new byte[bytesRead];
                System.out.println("BYTES READ LAST CHUNK" + bytesRead);
                System.arraycopy(chunk, 0, tmp, 0, bytesRead);
                this.sendPutChunkMessage(tmp, chuckNo, this.fileId);
            }
            else
                this.sendPutChunkMessage(chunk, chuckNo, this.fileId);

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
        Message request = new Message(PUTCHUNK, this.peer.getVersion(), Integer.toString(this.peer.getId()),
                fileId, Integer.toString(chunkNo), Integer.toString(this.desiredRepDeg), chunk);

        this.peer.incrementRepDegreeInfo(request, true);
        Dispatcher dispatcher = new Dispatcher(this.peer, request, this.peer.getBackupChannel());

        int repDeg = 0;
        int tries = 1;
        int sleepTime = 1000;
        String repDegString;

        // while (repDeg < this.desiredRepDeg && tries <= 5) {

        // TESTING
        while(repDeg < this.desiredRepDeg){

            this.peer.getSenderExecutor().submit(dispatcher);

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            repDegString = this.peer.getRepDegreeInfo(request.getHeader().getFileId(), Integer.toString(chunkNo), true);
            if(repDegString != null){
                repDeg =  Integer.parseInt(repDegString);
            }
            else{
                System.out.println("Error looking in map for chunk");
            }


            tries ++;
            sleepTime *= 2;
        }
    }

    public String getFileId() {
        return fileId;
    }
}