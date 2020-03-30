package Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
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
    public Message(String messageType, String version, String senderId, String fileId, String chunkNo, String replicationDegree) {
        this.header = new Header(messageType, version, senderId, fileId, chunkNo, replicationDegree);
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

        this.parseMessage(packet.getData());
    }

    /**
     * Constructs Message object from an array of bytes
     *
     * @param message : message bytes array
     */
    public Message(byte[] message) {
        this.parseMessage(message);
    }

    /**
     * Constructs a Message object with an array of bytes
     *
     * @param bytes : array of bytes
     */
    private void parseMessage(byte[] bytes)  {

        String[] message = (new String(bytes)).split(CRLF + CRLF);

        ByteArrayInputStream bodyInputStream = new ByteArrayInputStream(bytes);
        bodyInputStream.skip(message[0].length() + 4);
        byte[] body;

        //  matches one or many whitespaces and replaces them with one whitespace
        message[0].replaceAll("\\s+", " ");
        System.out.print(message[0]);
        String[] header = message[0].split(" ");

        switch(header[1]) {
            case PUTCHUNK:
                this.header = new Header(header[1], header[0], header[2], header[3], header[4], header[5]);
                body = new byte[bytes.length - this.header.toString().length() - 4];

                try {
                    bodyInputStream.read(body);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                StringBuilder builder = new StringBuilder();
                for(byte byteC :body){
                    builder.append(String.format("%02X", byteC));
                }
                System.out.println(builder.toString());
                break;

            case CHUNK:
                this.header = new Header(header[1], header[0], header[2], header[3], header[4]);
                body = new byte[bytes.length - this.header.toString().length() - 4];

                try {
                    bodyInputStream.read(body);
                } catch (IOException e) {
                    e.printStackTrace();
                }

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
        return(this.body == null) ? header : header + CRLF + CRLF + printBodyHex();
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

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(this.header.toString().getBytes());
            if(this.body != null)

                outputStream.write(' ');
                outputStream.write(CR);
                outputStream.write(LF);
                outputStream.write(CR);
                outputStream.write(LF);
                outputStream.write(this.body);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return outputStream.toByteArray();

    }

    public Header getHeader() {
        return this.header;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}


