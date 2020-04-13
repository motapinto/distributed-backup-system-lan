package SubProtocols;

import Common.Utilities;
import Peer.Peer;
import Message.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Restore {

    private Peer peer;
    private String pathName;
    private int numberOfChunks = 0;
    private int numberDistinctChunksReceived = 0;
    private String fileId;



    private Boolean tcpConnectionActive = false;

    //Map to store the chunkId = fileId + "_" + chunkNo and they key the bytes of that chunk
    private ConcurrentHashMap<String, byte[]> chunks = new ConcurrentHashMap<>();
    private Socket enhancedSocket;
    private OutputStream outStream;
    private DataOutputStream dataOutStream;



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
        File file = new File(this.pathName);
        long newLastModified = file.lastModified();
        this.fileId = Utilities.hashAndEncode(file.getName() + file.lastModified() + file.length());

        this.restoreDone = false;
        this.numberOfChunks = 0;


        Map<String, String> repDegreeInfo = peer.getRepDegreeInfo();
        Map<String, String> storedHistory = peer.getStoredChunkHistory();

        repDegreeInfo.forEach((key, value) -> {
            if(key.split("_")[0].equals(fileId)) {
                this.numberOfChunks++;
            }
        });


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
                this.numberOfChunks--;

            }
        });

        System.out.println("Number of chunks necessary : " + this.numberOfChunks);

        if (this.peer.getVersion().equals("1.1") && this.numberDistinctChunksReceived < this.numberOfChunks) {
            Runnable enhancedRestore = new EnhacementRestore(this);
            this.peer.getSenderExecutor().submit(enhancedRestore);
        }


        while(this.numberDistinctChunksReceived < this.numberOfChunks){
            this.getChunksProcedure();
            try {
                Thread.sleep(3000);
                System.out.println("After sleep");
                System.out.println("Number of distinct chunks received = " + this.numberDistinctChunksReceived);
                System.out.println("Number of chunks necessary  = " + this.numberOfChunks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        reconstructFile();
        file.setLastModified(newLastModified);
        this.restoreDone = true;
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

    public void getChunksProcedure(){

        Dispatcher dispatcher;
        Message message;
        System.out.println(this.numberOfChunks);

        for(int i = 0; i <  this.numberOfChunks; i++){
           if(this.chunks.get(this.fileId + "_" + i) == null){
               message = new Message("GETCHUNK", this.peer.getVersion(), Integer.toString(this.peer.getId()), this.fileId, Integer.toString(i));
               dispatcher = new Dispatcher(this.peer, message, this.peer.getControlChannel());
               this.peer.getSenderExecutor().submit(dispatcher);
           }
        }
    }

    public void startChunkProcedure(Message message){
        String fileId = message.getHeader().getFileId();
        String chunkNo = message.getHeader().getChuckNo();
        Message chunkMessage;

        if(this.peer.getStoredChunkHistory().get(this.peer.getId() + "_" + fileId + "_" + chunkNo) != null){
            try {
                Thread.sleep((long) (Math.random() * 400));

                if(!this.peer.hasChunkBeenSent(fileId, chunkNo)) {

                    Path fileLocation = Paths.get(this.peer.FILE_STORAGE_PATH + "/" + fileId + "/" + chunkNo);
                    byte[] data = new byte[0];

                    try {
                        data = Files.readAllBytes(fileLocation);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    chunkMessage = new Message("CHUNK", this.peer.getVersion(), Integer.toString(this.peer.getId()), fileId, chunkNo);
                    chunkMessage.setBody(data);

                    if(message.getHeader().getVersion().equals("1.1") && this.peer.getVersion().equals("1.1")) {
                        if (!this.tcpConnectionActive) {
                            try {
                                InetAddress localhost = InetAddress.getLocalHost();
                                this.setServerSocketConnection(localhost, this.peer.getRestoreChannel().getPort());
                                this.tcpConnectionActive = true;
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }

                        }

                        try {
                            while (enhancedSocket.getInputStream().available() != 0) {
                                System.out.println("Waiting for socket to be empty");
                            }
                            try {
                                this.dataOutStream.writeInt(chunkMessage.toBytes().length);
                                System.out.println(chunkMessage.toBytes());
                                this.dataOutStream.write(chunkMessage.toBytes());
                            }
                            catch (Exception e){
                                System.out.println(e.getMessage());
                            }
                            System.out.println("SENT CHUNK " + message.getHeader().getChuckNo());

                        } catch (IOException e) {
                            System.out.println("Error");
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
        else {
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
            peer.addSentChunkInfo(message.getHeader().getFileId(), message.getHeader().getChuckNo());
        }
    }

    /**
     * connects to the TCP socket to be able to send the chunks necessary
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

    public class EnhacementRestore implements Runnable {

        private Restore restore;
        private ServerSocket listener;

        public EnhacementRestore(Restore restore) {
            this.restore = restore;
        }

        public void run() {
            System.out.println("Connecting to socket");
            try {
                this.listener = new ServerSocket(this.restore.getPeer().getRestoreChannel().getPort());
                System.out.println("Connected to socket");
                while (true){
                    Runnable requestHandler = new RequestHandler(this.listener.accept(), this.restore);
                    this.restore.peer.getReceiverExecutor().submit(requestHandler);
                    System.out.println("RECEIVED CHUNK");
                }
            }
            catch (IOException e) {
                try {
                    this.listener.close();
                    this.restore.setTcpConnectionActive(false);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }
    }

    public class RequestHandler implements Runnable {

        private Socket socket;
        private Restore restore;

        public RequestHandler(Socket socket, Restore restore) {
            this.socket = socket;
            this.restore = restore;
        }

        public void run() {

            while (!this.restore.isRestoreDone()) {
                System.out.println("entrei no handler");
                try {
                    System.out.println("estou parado aqui");
                    InputStream in = this.socket.getInputStream();
                    DataInputStream dis = new DataInputStream(in);
                    int len = dis.readInt();
                    byte[] data = new byte[len];
                    if (len > 0) {
                        dis.readFully(data);
                    }
                    System.out.println("ESTOU AQUI CARAMBA ESTÁ A FUNCIONAR ACABEI DE RECEBER CHUNK");
                    Message requestMessage = new Message(data);
                    this.restore.saveChunkProcedure(requestMessage);
                }
                catch (Exception e){
                    e.printStackTrace();
                    e.printStackTrace();
                }


            }
            try {
                System.out.println("aqui");
                this.socket.close();
                System.out.println("CLOSED SOCKET");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public boolean isRestoreDone() {
        return restoreDone;
    }

    public void setRestoreDone(boolean restoreDone) {
        this.restoreDone = restoreDone;
    }

    public Boolean getTcpConnectionActive() {
        return tcpConnectionActive;
    }

    public void setTcpConnectionActive(Boolean tcpConnectionActive) {
        this.tcpConnectionActive = tcpConnectionActive;
    }

    public Peer getPeer() {
        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }


}
