package Message;

import Common.Logs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

import static Common.Constants.*;

public class Message {

    private Header header;
    byte[] body;


    /**
     * Constructs Message for PUTCHUNKS messages
     * <MessageType> <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF>
     *
     * @param messageType       indicates message type
     * @param version           indicates the version of the peer that sends the message
     * @param senderId          indicates the sender id
     * @param fileId            indicates the file id
     * @param chunkNo           indicate the chunk number
     * @param replicationDegree indicates the desired replication degree
     */
    public Message(String messageType, String version, String senderId, String fileId, String chunkNo, String replicationDegree, byte[] body) {
        this.header = new Header(messageType, version, senderId, fileId, chunkNo, replicationDegree);
        this.body = body;
    }

    /**
     * Constructs Message for CHUNK, GETCHUNK and STORED messages
     * <MessageType> <Version> <SenderId> <FileId> <ChunkNo><CRLF>
     *
     * @param messageType indicates message type
     * @param version     indicates the version of the peer that sends the message
     * @param senderId    indicates the sender id
     * @param fileId      indicates the file id
     * @param chunkNo     indicate the chunk number
     */
    public Message(String messageType, String version, String senderId, String fileId, String chunkNo) {
        this.header = new Header(messageType, version, senderId, fileId, chunkNo);
    }

    /**
     * Constructs Message for DELETE messages
     * <MessageType> <Version> <SenderId> <FileId> <CRLF>
     *
     * @param messageType indicates message type
     * @param version     indicates the version of the peer that sends the message
     * @param senderId    indicates the sender id
     * @param fileId      indicates the file id
     */
    public Message(String messageType, String version, String senderId, String fileId) {
        this.header = new Header(messageType, version, senderId, fileId);
    }

    /**
     * Constructs Message object from a DatagramPacket
     *
     * @param packet : DatagramPacket containing message
     */
    public Message(DatagramPacket packet) {
        try {
            this.parseMessage(packet.getData(), packet.getLength());
        } catch (IOException e) {
            Logs.logError("Error parsing message");
            e.printStackTrace();
        }
    }

    /**
     * Constructs Message object from an array of bytes
     *
     * @param message : message bytes array
     */
    public Message(byte[] message) {
        try {
            this.parseMessage(message, message.length);
        } catch (IOException e) {
            Logs.logError("Error parsing message");
            e.printStackTrace();
        }
    }

    /**
     * Constructs a Message object with an array of bytes
     *
     * @param bytes : array of bytes
     */
    private void parseMessage(byte[] bytes, int packetLength) throws IOException {
        String[] message = (new String(bytes)).split(CRLF + CRLF);
        int headerSize = message[0].length();

        //  matches one or many whitespaces and replaces them with one whitespace
        message[0].replaceAll("\\s+", " ");
        String[] header = message[0].split(" ");

        switch(header[1]) {
            case PUTCHUNK:
                this.header = new Header(header[1], header[0], header[2], header[3], header[4], header[5]);
                this.body = new byte[packetLength - this.header.toString().length()];
                ByteArrayInputStream putchunkInputStream = new ByteArrayInputStream(bytes);
                putchunkInputStream.skip(headerSize + 4);
                putchunkInputStream.read(this.body);
                break;

            case CHUNK:
                this.header = new Header(header[1], header[0], header[2], header[3], header[4]);
                this.body = new byte[packetLength - this.header.toString().length()];
                ByteArrayInputStream chunkInputStream = new ByteArrayInputStream(bytes);
                chunkInputStream.skip(headerSize + 4);
                chunkInputStream.read(this.body);

                break;

            case GETCHUNK:
            case REMOVED:
            case STORED:
                this.header = new Header(header[1], header[0], header[2], header[3], header[4]);
                break;

            case DELETE:
                this.header = new Header(header[1], header[0], header[2], header[3]);
                break;

            default:
                break;
        }
    }

    @Override
    public String toString() {
        String header = this.header.toString();
        String body = new String(this.body);
        return(this.body == null) ? header : header + body;
    }

    public String printBodyHex(){
        if(this.body != null) {
            StringBuilder builder = new StringBuilder();
            for (byte byteC : this.body) {
                builder.append(String.format("%02X", byteC));
            }
            return builder.toString();
        }
        return null;
    }

    public byte[] toBytes() {
        byte[] messageHeaderBytes = this.header.toString().getBytes();
        byte[] messageBytes;

        if(this.header.getMessageType().equals("PUTCHUNK") || this.header.getMessageType().equals("CHUNK")){
            messageBytes = new byte[messageHeaderBytes.length + this.body.length];
            System.arraycopy(messageHeaderBytes, 0, messageBytes, 0, messageHeaderBytes.length);
            System.arraycopy(this.body, 0, messageBytes, messageHeaderBytes.length, this.body.length);
        } else {
            messageBytes = new byte[messageHeaderBytes.length];
            System.arraycopy(messageHeaderBytes, 0, messageBytes, 0, messageHeaderBytes.length);
        }
        return messageBytes;
    }

    public Header getHeader() {
        return this.header;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setBody(byte[] data) {
        this.body = data;
    }
}