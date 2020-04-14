package Message;

import static Common.Constants.CRLF;

public class Header {
    private String chunkNo;
    private String ip;
    private String port;
    private final String version;
    private final String messageType;
    private String senderId;
    private String fileId;
    private String replicationDeg;
    private String destId;

    /**
     * Message header for PUTCHUNK messages
     * <Version> <MessageType> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF>
     *
     * @param messageType    : indicates message type
     * @param version        : indicates the version of the peer that sends the message
     * @param senderId       : indicates the sender id
     * @param fileId         : indicates the file id
     * @param chunkNo        : indicate the chunk number
     * @param replicationDeg : indicates the desired replication degree
     */
    public Header(String version, String messageType, String senderId, String fileId, String chunkNo, String replicationDeg) {
        this.messageType = messageType.trim();
        this.version = version.trim();
        this.senderId = senderId.trim();
        this.fileId = fileId.trim();
        this.chunkNo = chunkNo.trim();
        this.replicationDeg = replicationDeg.trim();
    }

    /**
     * Message header for CHUNK, GETCHUNK and STORED messages
     * <Version> <MessageType> <SenderId> <FileId> <ChunkNo> <CRLF>
     *
     * @param messageType : indicates message type
     * @param version     : indicates the version of the peer that sends the message
     * @param senderId    : indicates the sender id
     * @param fileId      : indicates the file id
     * @param chunkNo     : indicate the chunk number
     */
    public Header(String version, String messageType, String senderId, String fileId, String chunkNo) {
        this.version = version.trim();
        this.messageType = messageType.trim();
        this.senderId = senderId.trim();
        this.fileId = fileId.trim();
        this.chunkNo = chunkNo.trim();
    }

    /**
     * Message header for DELETE messages
     * <Version> <MessageType> <SenderId> <FileId> <CRLF>
     *
     * @param messageType : indicates message type
     * @param version     : indicates the version of the peer that sends the message
     * @param senderId    : indicates the sender id
     * @param fileId      : indicates the file id
     */
    public Header(String version, String messageType, String senderId, String fileId) {
        this.version = version.trim();
        this.messageType = messageType.trim();
        this.senderId = senderId.trim();
        this.fileId = fileId.trim();
    }

    public Header(String MessageType, String Version, String SenderId, String FileId, String ChunkNo, String ip, String port) {
        this.messageType = MessageType.trim();
        this.version = Version.trim();
        this.senderId = SenderId.trim();
        this.fileId = FileId.trim();
        this.chunkNo = ChunkNo.trim();
        this.ip = ip.trim();
        this.port = port.trim();
    }



    /**
     * Message header for DELETEACK messages
     * <Version> <MessageType> <SenderId> <FileId> <DestId> <CRLF>
     *
     * @param messageType : indicates message type
     * @param senderId    : indicates the sender id
     * @param fileId      : indicates the file id
     * @param destId      : indicates the destination peer id
     */
    public Header(String version, String messageType, String senderId, String fileId, String destId, boolean isACK) {
        this.version = version.trim();
        this.messageType = messageType.trim();
        this.version = version.trim();
        this.senderId = senderId.trim();
        this.fileId = fileId.trim();
        this.destId = destId;
    }

    @Override
    public String toString() {
        String header = new String();

        header += this.version + this.messageType;

        if(this.senderId != null) {
            header += " " + this.senderId;
        }

        if(this.fileId != null) {
            header += " " + this.fileId;
        }

        if(this. chunkNo != null) {
            header += " " + this. chunkNo;
        }

        if(this.replicationDeg != null) {
            header += " " + this.replicationDeg;
        }

        if(this.destId != null) {
            header += " " + this.destId;
        }

        header += " " + CRLF + CRLF;

        return header;
    }

    public String getVersion() {
        return version;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getFileId() {
        return fileId;
    }

    public String getChuckNo() {
        return  chunkNo;
    }

    public String getReplicationDeg() {
        return replicationDeg;
    }


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }


    public String getDestId() {
        return destId;
    }
}
