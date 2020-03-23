package Message;

import Peer.Peer;
import Channels.Channel;
import static Common.Constants.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Dispatcher implements Runnable{
    private Peer peer;
    private Message message;

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
        this.message.setType(Message.MessageType.RECEIVER);
    }

    /**
     * Receives a Message to be handled and dispatched
     * Role - Sender
     *
     * @param peer : peer that receives packet
     * @param message : packet received from the channel
     */
    public Dispatcher(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
        this.message.setType(Message.MessageType.SENDER);
    }

    @Override
    public void run() {
        switch (this.message.getType()) {
            case RECEIVER:
                this.receiveMessage();
                break;

            case SENDER:
                this.sendMessage();
                break;

            default:
                break;
        }
    }

    /**
     * Function responsible to sending messages
     */
    public void sendMessage() {
        switch (message.getHeader().getMessageType()) {
            case PUTCHUNK:
                peer.getBackup().sendPutChunkMessage(message);
                break;
            case STORED:
                peer.getBackup().sendStoredMessage(message);
                break;
            case GETCHUNK:
                break;
            case DELETE:
                break;
            case REMOVED:
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
                this.peer.getBackup().storeChunk(message);
                break;
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
     * @param channel : channel that will receive the message
     */
    public void sendMessageToChannel(Message message, Channel channel) {
        DatagramPacket packet;
        DatagramSocket socket;

        try {
            socket = new DatagramSocket();
            byte[] buf = message.toBytes();
            packet = new DatagramPacket(buf, buf.length, channel.getAddress(), channel.getPort());
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
