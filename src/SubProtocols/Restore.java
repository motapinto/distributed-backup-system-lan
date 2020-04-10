package SubProtocols;

import Common.Utilities;
import Peer.Peer;
import Message.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private ConcurrentHashMap<String, byte[]> chunks = new ConcurrentHashMap<>();

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


    public void startRestoreFileProcedure(){


        File file = new File(this.pathName);
        long newLastModified = file.lastModified();
        this.fileId = Utilities.hashAndEncode(file.getName() + file.lastModified() + file.length());


        this.numberOfChunks = 0;
        int number = 0;
        Path fileLocation;
        byte[] data;


        while(true){
            if(this.peer.getRepDegreeInfo().get(this.fileId + "_" + number) != null){
                this.numberOfChunks++;
            }
            else
                break;
            number++;
        }

        int numberOfChunksAux = this.numberOfChunks;

        for(int i = 0; i < numberOfChunksAux; i++){
            if(this.peer.getStoredChunkHistory().get(this.peer.getId() + "_" + this.fileId + "_" + i) != null){
                fileLocation = Paths.get(Peer.FILE_STORAGE_PATH + "/" + this.fileId + "/" + i);
                data = new byte[0];

                try {
                    data = Files.readAllBytes(fileLocation);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                this.chunks.put(this.fileId + "_" + i, data);
                this.numberOfChunks--;
            }
        }

        System.out.println("Number of chunks necessary : " + this.numberOfChunks);
        this.GetChunksProcedure();


        while(this.numberDistinctChunksReceived < this.numberOfChunks){
            System.out.println("Number of distinct chunks = " + this.numberDistinctChunksReceived);
            System.out.println("Number of chunks received = " + this.numberOfChunks);
            try {
                Thread.sleep(3000);
                System.out.println("After sleep");
                System.out.println("Number of distinct chunks = " + this.numberDistinctChunksReceived);
                System.out.println("Number of chunks received = " + this.numberOfChunks);
                if(this.numberDistinctChunksReceived < this.numberOfChunks)
                    this.GetChunksProcedure();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        reconstructFile();
        file.setLastModified(newLastModified);
        this.peer.printMapBytes(this.chunks);
    }

    public void reconstructFile(){
        File outputFile = new File(this.pathName);

        try (FileOutputStream outputStream = new FileOutputStream(outputFile); ) {

            for(int i = 0; i < this.numberDistinctChunksReceived; i++){
                outputStream.write(this.chunks.get(this.fileId + "_" + i));  //write the bytes
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    public void GetChunksProcedure(){

        int numberOfChunk = 0;

        Dispatcher dispatcher;
        Message message;
        System.out.println(this.numberOfChunks);

        for(int i = 0; i <  this.numberOfChunks; i++){
           if(this.chunks.get(this.fileId + "_" + i) == null){
               message = new Message("GETCHUNK", Integer.toString(1), Integer.toString(this.peer.getId()), this.fileId, Integer.toString(i));
               dispatcher = new Dispatcher(this.peer, message, this.peer.getControlChannel());
               this.peer.getSenderExecutor().submit(dispatcher);
           }
        }
    }

    public void startChunkProcedure(Message message){
        String fileId = message.getHeader().getFileId();
        String chunkNo = message.getHeader().getChuckNo();
        Message chunkMessage;
        String originalSenderPeerId;


        if(this.peer.getStoredChunkHistory().get(this.peer.getId() + "_" + fileId + "_" + chunkNo) != null){
            try {
                Thread.sleep((long) (Math.random() * 400));

                if(!this.peer.hasChunkBeenSent(fileId, chunkNo)) {

                    Path fileLocation = Paths.get(Peer.FILE_STORAGE_PATH + "/" + fileId + "/" + chunkNo);
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
                this.peer.removeChunkFromSentChunks(message.getHeader().getFileId(), message.getHeader().getChuckNo());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        else{
            System.out.println("received request for a chunk that i don't have");
        }
    }


    public void saveChunkProcedure(Message message) {

        if ((message.getHeader().getFileId().equals(this.fileId))){
            if (chunks.get(message.getHeader().getFileId() + "_" + message.getHeader().getChuckNo()) == null) {
                chunks.put(message.getHeader().getFileId() + "_" + message.getHeader().getChuckNo(), message.getBody());
                this.numberDistinctChunksReceived++;
            }

        } else {
            System.out.println("entrei aqqui");
            peer.addSentChunkInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo());
        }
    }
}
