package SubProtocols;

import Common.Logs;
import Common.Utilities;
import Peer.Peer;
import Message.*;
import java.io.*;
import java.nio.file.Paths;

import static Common.Constants.*;

public class Backup {
    private Peer peer;
    private int desiredRepDeg;
    private String pathName;
    private String fileName;
    private int lastChunkNo;
    private String fileId;

    /**
     * Responsible for backing up a file
     *
     * @param peer : peer listening to the multicast
     * @param desiredRepDeg : desired replication degree of the file
     */
    public Backup(Peer peer, String pathName, int desiredRepDeg){
        this.peer = peer;
        this.desiredRepDeg = desiredRepDeg;
        this.pathName = pathName;
        this.fileName = Paths.get(pathName).getFileName().toString();
        this.lastChunkNo = 0;
    }

    /**
     * Creates backup protocol
     *
     * @param peer : peer that creates backup protocol
     */
    public Backup(Peer peer){
        this.peer = peer;
        this.pathName = null;
        this.fileName = null;
        this.lastChunkNo = 0;
    }


    /**
     * Stores the chunk and sends a STORED message, if the peer has enough memory and does not have that chunk
     */
    public void storeChunk(Message message) throws IOException {

        if(this.peer.getAvailableStorage() < message.getBody().length) return;

        sendStoredMessage(message);

        String pathName = Peer.FILE_STORAGE_PATH + "/" + message.getHeader().getSenderId() + "/" +
                message.getHeader().getFileId() + "/" + message.getHeader().getChuckNo();

        File out = new File(pathName);

        // If the chunk is already stored then it does not make anything else
        if(out.exists()) return;

        // If the directory Storage/SenderId/FileId does not exist creates it
        if(!out.getParentFile().exists()) out.getParentFile().mkdirs();

        out.createNewFile();

        FileWriter writer = new FileWriter(out);
        FileOutputStream fos = new FileOutputStream(pathName);
        fos.write(message.getBody());

        // Updates current system memory of the peer
        this.peer.setCurrentSystemMemory(this.peer.getCurrentSystemMemory() + message.getBody().length);

        // Updates replication degree of the chunk
        this.peer.increaseRepDegreeInfo(message);

        fos.close();
    }

    /**
     * Sends STORED message for a chunk
     * @param message : message with request
     */
    public void sendStoredMessage(Message message) {
        Header replyHeader = new Header();
        replyHeader.setVersion(this.peer.getVersion());
        replyHeader.setMessageType(STORED);
        replyHeader.setSenderId(Integer.toString(this.peer.getId()));
        replyHeader.setFileId(message.getHeader().getFileId());
        replyHeader.setChuckNo((message.getHeader().getChuckNo()));

        Message reply = new Message(replyHeader);

        Dispatcher dispatcher = new Dispatcher(this.peer, reply);
        dispatcher.run();
    }

    /**
     * Splits a file into chunks
     */
    public void splitFileIntoChunks() throws IOException {

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
            sendPutChunkMessage(chuck, chuckNo, fileId);
            this.lastChunkNo++;
        }

        inputFile.close();
    }

    /**
     * Sends PUTCHUNK message for a chunk
     * @param chunk : received files to separate into chunks
     * @param chunkNo : number of the received chunk
     */
    public void sendPutChunkMessage(byte[] chunk, int chunkNo, String fileId) {

        Message request = new Message(PUTCHUNK, this.peer.getVersion(), Integer.toString(this.peer.getId()), fileId, Integer.toString(chunkNo), Integer.toString(this.desiredRepDeg));
        request.setBody(chunk);

        Dispatcher dispatcher = new Dispatcher(this.peer, request);
        this.peer.getSenderExecutor().submit(dispatcher);
    }

    public String getFileId() {
        return fileId;
    }
}
