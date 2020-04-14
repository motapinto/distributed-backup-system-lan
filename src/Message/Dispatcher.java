package Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import Common.Logs;
import Peer.Peer;
import Channels.Channel;
import static Common.Constants.*;

public class Dispatcher implements Runnable{

    private Peer peer;
    private Message message;
    private MessageType type;
    private String address;
    private int port;

    enum MessageType {
        SENDER,
        RECEIVER
    }

    /**
     * Receives a DatagramPacket from the a Channel to be handled and dispatched
     * Role - Receiver
     *
     * @param peer : peer that receives packet
     * @param packet : packet received from the channel
     */
    public Dispatcher(Peer peer, DatagramPacket packet) {
        this.peer = peer;
        this.message = new Message(packet);
        this.type = MessageType.RECEIVER;
    }

    /**
     * Receives a Message to be handled and dispatched
     * Role - Sender
     *
     * @param peer : peer that receives packet
     * @param message : packet received from the channel
     * @param channel : packet received from the channel
     */
    public Dispatcher(Peer peer, Message message, Channel channel) {
        this.peer = peer;
        this.message = message;
        this.type = MessageType.SENDER;
        this.address = channel.getAddress();
        this.port = channel.getPort();
    }

    @Override
    public void run() {
        switch (this.type) {
            case RECEIVER:
                this.receiveMessageFromChannel();
                break;
            case SENDER:
                this.sendMessageToChannel();
                break;
            default:
                break;
        }
    }

    /**
     * Function responsible to receiving and handling requests
     */
    public void receiveMessageFromChannel() {
        if(Integer.parseInt(this.message.getHeader().getSenderId()) == this.peer.getId()) return;
        this.peer.getDelete().checkIfPeerNeedsToDelete(this.message.getHeader().getSenderId());

        switch (this.message.getHeader().getMessageType()) {
            case PUTCHUNK:
                this.peer.getSpaceReclaim().storePutChunk(message);
                this.peer.getBackup().startStoredProcedure(message);
                break;
            case STORED:
                this.peer.updateRepDegreeInfo(message, true);
                if(!this.message.getHeader().getVersion().equals("1.0"))
                    this.peer.removeDeleteHistory(message);
                break;
            case GETCHUNK:
            case ENRESTORE:
                this.peer.getRestore().startChunkProcedure(message);
                break;
            case DELETE:
                this.peer.getDelete().deleteFile(this.message.getHeader().getFileId());
                if(!this.message.getHeader().getVersion().equals("1.0"))
                    this.peer.getDelete().sendDeleteAckMessage(message.getHeader().getFileId(), message.getHeader().getSenderId());
            case DELETEACK:
                System.out.println("heyy");
                System.out.println(this.message.getHeader());
                if(Integer.parseInt(this.message.getHeader().getDestId()) == this.peer.getId())
                    this.peer.removeDeleteHistory(message);
                break;
            case REMOVED:
                this.peer.getSpaceReclaim().updateChunkRepDegree(this.message);
                break;
            case CHUNK:
                this.peer.getRestore().saveChunkProcedure(message);
                break;
        }
    }

    /**
     * Function responsible for delivering a message to a channel
     */
    public void sendMessageToChannel() {
        DatagramPacket packet;
        DatagramSocket socket;

        try {
            socket = new DatagramSocket();
            byte[] buf = this.message.toBytes();
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(this.address), this.port);
            socket.send(packet);
        } catch (IOException e) {
            Logs.logError("Error while sending a message to a channel");
            e.printStackTrace();
        }
    }
}