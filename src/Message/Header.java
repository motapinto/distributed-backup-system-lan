package Message;

import static Common.Constants.CRLF;

public class Header {
    private String version;
    private String messageType;
    private String senderId;
    private String fileId;
    private String chuckNo;
    private String replicationDeg;

    public Header() { super(); }

    @Override
    public String toString() {
        String header = this.version + " " + this.messageType;

        if(this.senderId != null) {
            header += " " + this.senderId;
        }
        if(this.fileId != null) {
            header += " " + this.fileId;
        }
        if(this.chuckNo != null) {
            header += " " + this.chuckNo;
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
        return chuckNo;
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

    public void setChuckNo(String chuckNo) {
        this.chuckNo = chuckNo;
    }

    public void setReplicationDeg(String replicationDeg) {
        this.replicationDeg = replicationDeg;
    }
}
