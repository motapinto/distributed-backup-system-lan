package Message;

import java.net.DatagramPacket;
import static Common.Constants.*;

public class Message {

    private Header header;
    byte[] body;

    /**
     * Message constructor for PUTCHUNKS messages
     * <MessageType> <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF>
     *
     * @param messageType       indicates message type
     * @param version           indicates the version of the peer that sends the message
     * @param senderId          indicates the sender id
     * @param fileId            indicates the file id
     * @param chunkNo           indicate the chunk number
     * @param replicationDegree indicates the desired replication degree
     */
    public Message(String messageType, String version, String senderId, String fileId, String chunkNo, String replicationDegree) {
        header = new Header(messageType, version, senderId, fileId, chunkNo, replicationDegree);

    }

    /**
     * Message header for CHUNK, GETCHUNK and STORED messages
     * <MessageType> <Version> <SenderId> <FileId> <ChunkNo><CRLF>
     *
     * @param messageType indicates message type
     * @param version     indicates the version of the peer that sends the message
     * @param senderId    indicates the sender id
     * @param fileId      indicates the file id
     * @param chunkNo     indicate the chunk number
     */
    public Message(String messageType, String version, String senderId, String fileId, String chunkNo) {
        header = new Header(messageType, version, senderId, fileId, chunkNo);
    }

    /**
     * Message header for DELETE messages
     * <MessageType> <Version> <SenderId> <FileId> <CRLF>
     *
     * @param messageType indicates message type
     * @param version     indicates the version of the peer that sends the message
     * @param senderId    indicates the sender id
     * @param fileId      indicates the file id
     */
    public Message(String messageType, String version, String senderId, String fileId) {
        header = new Header(messageType, version, senderId, fileId);
    }

    //ALIVE
    public Message(String messageType, String version, String senderId) {
        header = new Header(messageType, version, senderId);
    }


    /**
     * Constructs Message object with a DatagramPacket
     * @param packet : DatagramPacket containing message
     */
    public Message(DatagramPacket packet) {
        this.header = new Header();
        this.body = null;
        this.parseMessage(packet.toString().getBytes());
    }

    /**
     * Constructs Message object from an array of bytes
     * @param message : message bytes array
     */
    public Message(byte[] message) {
        this.header = new Header();
        this.body = null;
        this.parseMessage(message);
    }

    private void parseMessage(byte[] bytes)  {
        String[] message = (new String(bytes)).split(CRLF + CRLF);


        //  matches one or many whitespaces and replaces them with one whitespace
        message[0].replaceAll("\\s+", " ");
        String[] header = message[0].split(" ");

        switch(header[1]) {
            case PUTCHUNK:
                this.header.setVersion(header[0]);
                this.header.setMessageType(header[1]);
                this.header.setSenderId(header[2]);
                this.header.setFileId(header[3]);
                this.header.setChuckNo(header[4]);
                this.header.setReplicationDeg(header[5]);
                this.body = message[1].getBytes();
                break;

            case CHUNK:
                this.header.setVersion(header[0]);
                this.header.setMessageType(header[1]);
                this.header.setSenderId(header[2]);
                this.header.setFileId(header[3]);
                this.header.setChuckNo(header[4]);
                this.body = message[1].getBytes();
                break;

            case GETCHUNK:
            case REMOVED:
            case STORED:
                this.header.setVersion(header[0]);
                this.header.setMessageType(header[1]);
                this.header.setSenderId(header[2]);
                this.header.setFileId(header[3]);
                this.header.setChuckNo(header[4]);
                break;

            case DELETE:
                this.header.setVersion(header[0]);
                this.header.setMessageType(header[1]);
                this.header.setSenderId(header[2]);
                this.header.setFileId(header[3]);
                break;

            default:
                break;
        }
    }

    @Override
    public String toString() {
        String header = this.header.toString();
        return(this.body == null) ? header : header + CRLF + CRLF + this.body;
    }

    public byte[] toBytes() {
        String header = this.header.toString();
        return((this.body == null) ? header : header + CRLF + CRLF + this.body).getBytes();
    }

    public Header getHeader() {
        return this.header;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
