package Message;

import Peer.Peer;
import Channels.Channel;
import static Common.Constants.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Dispatcher implements Runnable{
    private Peer peer;
    private Message message;
    private MessageType type;
    private String address;
    private int port;

    enum MessageType{
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
                this.receiveMessage();
                break;

            case SENDER:
                this.sendMessageToChannel(this.message);
                break;

            default:
                break;
        }
    }

    /**
     * Function responsible to receiving requests and does the handling
     */
    public void receiveMessage() {
        switch (this.message.getHeader().getMessageType()) {
            case PUTCHUNK:
                this.peer.getBackup().startStoredProcedure(message);
                break;
            case STORED:
                this.peer.incrementRepDegreeInfo(this.message.getHeader().getSenderId(), this.message.getHeader().getFileId(), this.message.getHeader().getChuckNo());
            case GETCHUNK:
                break;
            case DELETE:
                break;
            case REMOVED:
                break;
        }
    }

    /**
     * Delivers a message to a channel
     *
     * @param message : message to be sent
     */
    public void sendMessageToChannel(Message message) {
        DatagramPacket packet;
        DatagramSocket socket;

        try {
            socket = new DatagramSocket();
            byte[] buf = message.toBytes();
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(this.address), this.port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}