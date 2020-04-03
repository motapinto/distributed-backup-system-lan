package Channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import Common.Logs;
import Message.Dispatcher;
import Peer.Peer;
import static Common.Constants.MAX_PACKET_SIZE;

public class Channel implements Runnable {
    protected Peer peer;
    protected int port;
    protected String address;

    protected MulticastSocket multicastSocket;

    /**
     * Class responsible for the comunication with the multicast
     *
     * @param peer : peer listening to the multicast
     * @param address : multicast address
     * @param port : multicast port
     * @throws IOException
     */
    Channel(Peer peer, String address, int port) throws IOException {
        this.peer = peer;
        this.address = address;
        this.port = port;
        this.multicastSocket = new MulticastSocket(this.port);
        this.multicastSocket.setTimeToLive(1);

        InetAddress ipAddress = InetAddress.getByName(address);
        this.multicastSocket.joinGroup(ipAddress);
    }

    /**
     * Receives packets from the multicast socket
     *
     * @return DatagramPacket : received packet
     * @throws IOException
     */
    public DatagramPacket receive() throws IOException {
        byte[] buf = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        this.multicastSocket.receive(packet);
        return packet;
    }

    /**
     * Runs the Channel thread that handles received packets
     */
    @Override
    public void run() {
        while (true) {
            try {
                DatagramPacket received = this.receive();
                Dispatcher handler = new Dispatcher(this.peer, received);
                peer.getReceiverExecutor().submit(handler);

            } catch (IOException e) {
                Logs.logError("Error handling peer" + e);
            }
        }
    }

    /**
     * Returns the peer connected to the channel
     */
    public Peer getPeer() {
        return this.peer;
    }

    /**
     * Returns the channel port
     */
    public int getPort() { return port; }

    /**
     * Returns the channel address
     */
    public String getAddress() { return address; }
}