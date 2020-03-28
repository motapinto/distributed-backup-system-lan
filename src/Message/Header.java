package Message;

import static Common.Constants.CRLF;

public class Header {
    private String chunkNo;
    private String version;
    private String messageType;
    private String senderId;
    private String fileId;

    private String replicationDeg;


    /**
     * Message header for PUTCHUNKS messages
     * <MessageType> <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF>
     *
     * @param MessageType    indicates message type
     * @param Version        indicates the version of the peer that sends the message
     * @param SenderId       indicates the sender id
     * @param FileId         indicates the file id
     * @param ChunkNo        indicate the chunk number
     * @param ReplicationDeg indicates the desired replication degree
     */
    public Header(String MessageType, String Version, String SenderId, String FileId, String ChunkNo, String ReplicationDeg) {

        this.messageType = MessageType.trim();
        this.version = Version.trim();
        this.senderId = SenderId.trim();
        this.fileId = FileId.trim();
        this.chunkNo = ChunkNo.trim();
        this.replicationDeg = ReplicationDeg.trim();

    }

    /**
     * Message header for CHUNK, GETCHUNK and STORED messages
     * <MessageType> <Version> <SenderId> <FileId> <ChunkNo> <CRLF>
     *
     * @param MessageType indicates message type
     * @param Version     indicates the version of the peer that sends the message
     * @param SenderId    indicates the sender id
     * @param FileId      indicates the file id
     * @param ChunkNo     indicate the chunk number
     */
    public Header(String MessageType, String Version, String SenderId, String FileId, String ChunkNo) {

        this.messageType = MessageType.trim();
        this.version = Version.trim();
        this.senderId = SenderId.trim();
        this.fileId = FileId.trim();
        this.chunkNo = ChunkNo.trim();

    }

    /**
     * Message header for Delete messages
     * <MessageType> <Version> <SenderId> <FileId> <ChunkNo> <CRLF>
     *
     * @param MessageType indicates message type
     * @param Version     indicates the version of the peer that sends the message
     * @param SenderId    indicates the sender id
     * @param FileId      indicates the file id
     */
    public Header(String MessageType, String Version, String SenderId, String FileId) {

        this.messageType = MessageType.trim();
        this.version = Version.trim();
        this.senderId = SenderId.trim();
        this.fileId = FileId.trim();

    }

    /**
     * Message header for Alive messages
     * <MessageType> <Version> <SenderId> <FileId> <ChunkNo> <CRLF>
     *
     * @param MessageType indicates message type
     * @param Version     indicates the version of the peer that sends the message
     * @param SenderId    indicates the sender id
     */
    public Header(String MessageType, String Version, String SenderId) {

        this.messageType = MessageType.trim();
        this.version = Version.trim();
        this.senderId = SenderId.trim();

    }


    public Header() { }

    @Override
    public String toString() {
        String header = this.version + " " + this.messageType;

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

        header += CRLF;

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

    public void setVersion(String version) {
        this.version = version;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setChuckNo(String  chunkNo) {
        this.chunkNo =  chunkNo;
    }

    public void setReplicationDeg(String replicationDeg) {
        this.replicationDeg = replicationDeg;
    }
}
