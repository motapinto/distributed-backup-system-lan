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

    /**
     * Responsible for backing up a file
     *
     * @param peer : peer listening to the multicast
     * @param desiredRepDeg : desired replication degree of the file
     */
    public Backup(Peer peer, int desiredRepDeg){
        this.peer = peer;
        this.desiredRepDeg = desiredRepDeg;
    }

    /**
     * Creates backup protocol
     *
     * @param peer : peer that creates backup protocol
     */
    public Backup(Peer peer){
        this.peer = peer;
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
     * @param pathName : path name of the file to split into chunks
     */
    public void splitFileIntoChunks(String pathName) throws IOException {
        File file = new File(pathName);
        String fileId = Utilities.hashAndEncode(file.getName() + file.lastModified() + file.length());

        InputStream inputFile = new FileInputStream(file.getAbsolutePath());

        int numDivs = (int)Math.ceil(file.length() / MAX_CHUNK_SIZE);
        if(numDivs > MAX_NUM_CHUNKS) {
            Logs.logError("File can only have  ");
            return;
        }

        for (int chuckNo = 0; chuckNo < numDivs; chuckNo ++) {
            byte[] chuck = inputFile.readNBytes(MAX_CHUNK_SIZE);
            sendPutChunkMessage(chuck, chuckNo, fileId);
        }
    }

    /**
     * Sends PUTCHUNK message for a chunk
     * @param chunk : received files to separate into chunks
     * @param chunkNo : number of the received chunk
     */
    public void sendPutChunkMessage(byte[] chunk, int chunkNo, String fileId) {
        Header requestHeader = new Header();
        requestHeader.setVersion(this.peer.getVersion());
        requestHeader.setSenderId(Integer.toString(this.peer.getId()));
        requestHeader.setFileId(fileId);
        requestHeader.setChuckNo(Integer.toString(chunkNo));
        requestHeader.setReplicationDeg(Integer.toString(this.desiredRepDeg));

        Message request = new Message(requestHeader);
        request.setBody(chunk);

        Dispatcher dispatcher = new Dispatcher(this.peer, request);
        dispatcher.run();
    }
}
