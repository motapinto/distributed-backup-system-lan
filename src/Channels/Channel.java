package Channels;

import Message.Message;
import Peer.Peer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Channel {
    private Peer peer;
    private int port;
    private InetAddress address;
    private MulticastSocket multicastSocket;
    private Thread thread;

    // connects to multicast channel
    Channel(Peer peer, String address, int port) throws IOException {
        this.peer = peer;
        this.address = InetAddress.getByName(address);
        this.port = port;
        this.multicastSocket = new MulticastSocket(this.port);
        this.multicastSocket.setTimeToLive(1);
        this.multicastSocket.joinGroup(this.address);
    }

    // Receive messages
    public void receiveMessage() throws IOException {
        byte[] buf = new byte[64500];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        this.multicastSocket.receive(packet);

    }

    public void sendMessage(byte[] message) throws IOException {
        DatagramPacket packet = new DatagramPacket(message, message.length, this.address, this.port);
        this.multicastSocket.send(packet);
    }

    public void sendMessage(Message message) throws IOException {
        DatagramPacket packet = new DatagramPacket(message.to, message.length, this.address, this.port);
        this.multicastSocket.send(packet);
    }

    // Peer connected to the channel
    public Peer getPeer() {
        return this.peer;
    }


    public void closeSocket() {
        this.multicastSocket.close();
    }
}
