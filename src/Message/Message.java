package Message;

import java.net.DatagramPacket;
import static Common.Constants.*;

public class Message {
    private Header header;
    byte[] body;

    public Message(byte[] message) {
        this.header = new Header();
        this.body = null;
        this.parseMessage(message);
    }

    public Message(DatagramPacket packet) {
        this.header = new Header();
        this.body = null;
        this.parseMessage(packet.toString().getBytes());
    }

    public Message(Header header) {
        this.header = header;
        this.body = null;
    }

    private void parseMessage(byte[] bytes)  {
        String[] message = (new String(bytes)).split(CRLF + CRLF);
        byte[] body = message[1].getBytes();

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
                this.body = body;
                break;

            case CHUNK:
                this.header.setVersion(header[0]);
                this.header.setMessageType(header[1]);
                this.header.setSenderId(header[2]);
                this.header.setFileId(header[3]);
                this.header.setChuckNo(header[4]);
                this.body = body;
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
