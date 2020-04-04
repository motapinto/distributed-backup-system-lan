package SubProtocols;

import Peer.Peer;
import Message.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

public class Restore {

    private Peer peer;
    private String pathName;
    private int numberOfChunks = 0;
    private int numberDistinctChunksReceived = 0;
    private String fileId;

    //Map to store the chunkId = fileId + "_" + chunkNo and they key the bytes of that chunk
    private Map<String, byte[]> chunks = new ConcurrentHashMap<>();

    /**
     * Responsible for restoring a file
     *
     * @param peer          : peer listening to the multicast
     * @param pathname      : pathname to the file
     */
    public Restore(Peer peer, String pathname) {
        this.peer = peer;
        this.pathName = pathname;
    }

    /**
     * Creates restore protocol
     *
     * @param peer : peer that creates delete protocol
     */
    public Restore(Peer peer) {
        this.peer = peer;
    }


    public void startRestoreFileProcedure(String fileId){


        this.fileId = fileId;
        System.out.println("passei daqui 1");
        while(true){
            if(this.peer.getRepDegreeInfo().get(this.fileId + "_" + numberOfChunks) != null){
                numberOfChunks++;
            }
            else
                break;
        }
        System.out.println("passei daqui 2");
        this.GetChunksProcedure();

        System.out.println("passei daqui 3");
        while(numberDistinctChunksReceived < numberOfChunks){
            System.out.println("passei daqui 4");
            try {
                Thread.sleep(3000);
                if(numberDistinctChunksReceived < numberOfChunks)
                    this.GetChunksProcedure();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("passei daqui 5");
        reconstructFile();
    }

    public void reconstructFile(){
        File outputFile = new File(this.pathName);

        try (FileOutputStream outputStream = new FileOutputStream(outputFile); ) {

            for(int i = 0; i < this.numberDistinctChunksReceived; i++){
                outputStream.write(this.chunks.get(this.fileId + "_" + Integer.toString(i)));  //write the bytes
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void GetChunksProcedure(){

        int numberOfChunk = 0;

        Dispatcher dispatcher;
        Message message;
        while(numberOfChunks < this.numberOfChunks){
            message = new Message("GETCHUNK", Integer.toString(1), Integer.toString(this.peer.getId()), this.fileId, Integer.toString(numberOfChunk));
            dispatcher = new Dispatcher(this.peer, message, this.peer.getControlChannel());
            this.peer.getSenderExecutor().submit(dispatcher);
            numberOfChunk++;
        }

    }

    public void startChunkProcedure(Message message){
        String fileId = message.getHeader().getFileId();
        String chunkNo = message.getHeader().getChuckNo();
        Message chunkMessage;
        String originalSenderPeerId;

        if(this.peer.getStoredChunkHistory().get(this.peer.getId() + "_" + fileId + "_" + chunkNo) != null){
            originalSenderPeerId = this.peer.getStoredChunkHistory().get(this.peer.getId() + "_" + fileId + "_" + chunkNo);
            try {
                Thread.sleep((long) (Math.random() * 400));

                if(!this.peer.hasChunkBeenSent(fileId, chunkNo)) {

                    Path fileLocation = Paths.get(this.peer.FILE_STORAGE_PATH + "/" + originalSenderPeerId + "/" + fileId + "/" + chunkNo);
                    byte[] data = new byte[0];

                    try {
                        data = Files.readAllBytes(fileLocation);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    chunkMessage = new Message("CHUNK", Integer.toString(1), Integer.toString(this.peer.getId()), fileId, chunkNo);
                    chunkMessage.setBody(data);

                    Dispatcher dispatcher = new Dispatcher(this.peer, chunkMessage, this.peer.getRestoreChannel());
                    this.peer.getReceiverExecutor().submit(dispatcher);
                }
                peer.removeChunkFromSentChunks(message.getHeader().getFileId(), message.getHeader().getChuckNo());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }


    public void saveChunkProcedure(Message message) {

        if (peer.getRestore() != null) {
            if (chunks.get(message.getHeader().getFileId() + "_" + message.getHeader().getChuckNo()) == null) {
                chunks.put(message.getHeader().getFileId() + "_" + message.getHeader().getChuckNo(), message.getBody());
                numberDistinctChunksReceived++;
            }

        } else {
            peer.addSentChunkInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo());
        }
    }
}
