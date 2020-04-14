package SubProtocols;

import Common.Utilities;
import Peer.Peer;
import Message.*;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Restore {


    private Peer peer;
    private String pathName;

    //Number of chunks necessary to restore the file
    private int numberOfChunks = 0;
    //Number of distinct chunks received
    private int numberDistinctChunksReceived = 0;

    private String fileId;

    //Server socket and other objects used by the peer to receive the CHUNK messages from other peers with chunks he desires
    private ServerSocket listener;
    private Boolean tcpConnectionActive = false;


    //Map to store the chunkId = fileId + "_" + chunkNo and they key the bytes of that chunk
    private ConcurrentHashMap<String, byte[]> chunks = new ConcurrentHashMap<>();
    
    //Socket  used by other peers to send the CHUNK messsage in response to a GETCHUNK message
    private Socket enhancedSocket;
    private OutputStream outStream;
    private DataOutputStream dataOutStream;


    //primitive used to represent when the restore procedure is completed
    private boolean restoreDone = false;

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

    /**
     * Starts the restore procedure, sending the GETCHUNK message to the channel
     */
    public void startRestoreFileProcedure(){

        this.restoreDone = false;
        File file = new File(this.pathName);
        long newLastModified = file.lastModified();
        this.fileId = Utilities.hashAndEncode(file.getName() + file.lastModified() + file.length());


        Map<String, String> repDegreeInfo = peer.getRepDegreeInfo();
        Map<String, String> storedHistory = peer.getStoredChunkHistory();

        // counting the number of necessary chunks to restore the file
        this.numberOfChunks = 0;
        repDegreeInfo.forEach((key, value) -> {
            if(key.split("_")[0].equals(fileId)) {
                this.numberOfChunks++;
            }
        });

        // seeing if the peer we are executing already has chunks from that file and if so save them in the chunks map
        storedHistory.forEach((key, value) -> {
            if(key.split("_")[1].equals(fileId) && key.split("_")[0].equals(Integer.toString(this.peer.getId()))){
                String chunkNo = key.split("_")[2];
                Path fileLocation = Paths.get(this.peer.FILE_STORAGE_PATH + "/" + this.fileId + "/" + chunkNo);
                byte[] data = new byte[0];

                try {
                    data = Files.readAllBytes(fileLocation);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                this.chunks.put(this.fileId + "_" + chunkNo, data);
                this.numberDistinctChunksReceived++;

            }
        });

        if (this.peer.getVersion().equals("1.1") && this.numberDistinctChunksReceived < this.numberOfChunks) {
            Runnable enhancedRestore = new EnhancementRestore(this);
            this.peer.getSenderExecutor().submit(enhancedRestore);
        }

        while(this.numberDistinctChunksReceived < this.numberOfChunks){
            this.getChunksProcedure();
            try {
                Thread.sleep(3000);
                System.out.println("After sleep");
                System.out.println("Received : " + this.numberDistinctChunksReceived);
                System.out.println("Necessary : " + this.numberOfChunks);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(numberOfChunks != 0) {
            reconstructFile();
            file.setLastModified(newLastModified);
        }
        this.restoreDone = true;

        if(this.peer.getVersion().equals("1.1")) {
            try {
                this.listener.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Function used to create and rebuild the file from the chunks saved in the chunks map
     */
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

    /**
     * Sends the GETCHUNK messages to the control channel for the chunks still necessary
     */
    public void getChunksProcedure(){

        Dispatcher dispatcher;
        Message message;
        for(int i = 0; i <  this.numberOfChunks; i++){
           if(this.chunks.get(this.fileId + "_" + i) == null){
               System.out.println("NEED CHUNKNO " + i);
               message = new Message("GETCHUNK", this.peer.getVersion(), Integer.toString(this.peer.getId()), this.fileId, Integer.toString(i));
               dispatcher = new Dispatcher(this.peer, message, this.peer.getControlChannel());
               this.peer.getSenderExecutor().submit(dispatcher);
           }
        }
    }

    /**
     * Handles the request from a peer who wishes to restore a chunk, checks if it has stored the chunk and if so
     * sends a message with that chunk
     * @param message GETCHUNK message received from the MDC channel
     */
    public void startChunkProcedure(Message message){
        String fileId = message.getHeader().getFileId();
        String chunkNo = message.getHeader().getChuckNo();
        Message chunkMessage;

        if(this.peer.getStoredChunkHistory().get(this.peer.getId() + "_" + fileId + "_" + chunkNo) != null){
            try {
                Thread.sleep((long) (Math.random() * 400));

                if(!this.peer.hasChunkBeenSent(fileId, chunkNo)) {

                    byte[] data = this.getFileData(this.peer.FILE_STORAGE_PATH + "/" + fileId + "/" + chunkNo);

                    chunkMessage = new Message("CHUNK", this.peer.getVersion(), Integer.toString(this.peer.getId()), fileId, chunkNo);
                    chunkMessage.setBody(data);

                    if(message.getHeader().getVersion().equals("1.1") && this.peer.getVersion().equals("1.1")) {
                        if (!this.tcpConnectionActive) {
                            this.setServerSocketConnection(message.getIp(), this.peer.getRestoreChannel().getPort());
                            this.tcpConnectionActive = true;
                        }

                        try {
                            while (enhancedSocket.getInputStream().available() != 0) {
                            }
                            try { System.out.println("SENDING CHUNK" + chunkMessage.getHeader().getChuckNo());
                                this.dataOutStream.writeInt(chunkMessage.toBytes().length);
                                this.dataOutStream.write(chunkMessage.toBytes());
                                System.out.println("SENT CHUNK" + chunkMessage.getHeader().getChuckNo());
                            }
                            catch (Exception e){
                                System.out.println(e.getMessage());
                            }


                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }

                    else{
                        Dispatcher dispatcher = new Dispatcher(this.peer, chunkMessage, this.peer.getRestoreChannel());
                        this.peer.getReceiverExecutor().submit(dispatcher);
                    }

                }
                this.peer.removeChunkFromSentChunks(message.getHeader().getFileId(), message.getHeader().getChuckNo());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        // closing the socket used to send the CHUNK message
        if(message.getHeader().getVersion().equals("1.1") && this.peer.getVersion().equals("1.1")) {
            try {
                this.enhancedSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.tcpConnectionActive = false;
        }
    }

    /**
     * Gets the bytes of a certian file
     * @param pathName of the file
     * @return the file in a byte array
     */
    public byte[] getFileData(String pathName){

        Path fileLocation = Paths.get(pathName);
        byte[] data = new byte[0];

        try {
            data = Files.readAllBytes(fileLocation);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * Saves the chunk received in the CHUNK message
     * @param message CHUNK message received from the MDR channel
     */
    public void saveChunkProcedure(Message message) {

        if ((message.getHeader().getFileId().equals(this.fileId))){
            if (chunks.get(message.getHeader().getFileId() + "_" + message.getHeader().getChuckNo()) == null) {
                chunks.put(message.getHeader().getFileId() + "_" + message.getHeader().getChuckNo(), message.getBody());
                this.numberDistinctChunksReceived++;
            }
        } else {
            peer.addSentChunkInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo());
        }
    }

    /**
     * connects to the TCP socket to be able to send the CHUNK message to the peer that is currently restoring the file
     */
    public void setServerSocketConnection(InetAddress ip, int port) {

        try {
            this.enhancedSocket = new Socket(ip, port);
            this.outStream = this.enhancedSocket.getOutputStream();
            this.dataOutStream = new DataOutputStream(this.outStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * EnhancementRestore class is used to wait for a connection to the serverSocket then accept it and create a request
     * handler to receive the CHUNK message
     */
    public class EnhancementRestore implements Runnable {

        private Restore restore;

        public EnhancementRestore(Restore restore) {
            this.restore = restore;
        }

        public void run() {
            try {
                listener = new ServerSocket(this.restore.getPeer().getRestoreChannel().getPort());
                while (true){
                    Runnable requestHandler = new RequestHandler(listener.accept(), this.restore);
                    this.restore.peer.getReceiverExecutor().submit(requestHandler);
                    System.out.println("OPEN SOCKET");
                }
            }
            catch (IOException e) {
                try {
                    listener.close();
                    this.restore.setTcpConnectionActive(false);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * RequestHandler class is used to receive the CHUNK messages after the connection has been established
     */
    public class RequestHandler implements Runnable {

        private Socket socket;
        private Restore restore;

        public RequestHandler(Socket socket, Restore restore) {
            this.socket = socket;
            this.restore = restore;
        }

        public void run() {

            while(!this.restore.isRestoreDone()) {

                try {
                    InputStream in = this.socket.getInputStream();
                    DataInputStream dis = new DataInputStream(in);
                    try {

                        int len = dis.readInt();
                        byte[] data = new byte[len];
                        if (len > 0) {
                            dis.readFully(data);
                        }


                        Message requestMessage = new Message(data);
                        this.restore.saveChunkProcedure(requestMessage);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }



                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                System.out.println("CLOSED SOCKET");
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isRestoreDone(){
        return restoreDone;
    }

    public void setTcpConnectionActive(Boolean tcpConnectionActive){
        this.tcpConnectionActive = tcpConnectionActive;
    }

    public Peer getPeer(){
        return peer;
    }

    public void setPeer(Peer peer){
        this.peer = peer;
    }
}
