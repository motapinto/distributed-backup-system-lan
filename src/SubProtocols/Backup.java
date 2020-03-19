package SubProtocols;

import Peer.Peer;
import Message.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Backup {
    private String fileId;
    private int replicationDeg;
    private int chunkNumber;
    private bytes[] body;
    private String senderId;
    private Peer peer;

    //private Message msg;

    /**
     * Responsible for backing up a file
     *
     * @param peer : peer listening to the multicast
     */
    public Backup(Peer peer, Message msg){
        this.peer = peer;
        this.msg = msg;
    }

    /**
     * Creates backup protocol
     *
     * @param peer : peer that creates backup protocol
     */
    public Backup(Peer peer){
        this.peer = peer;
    }

    //notas para o ze: verificamos o sendirId no dispacher run
    public boolean putChunk(Message msg) throws IOException {

        /* caso não esteja verificar se existe espaço suficiiente para guardar o ficheiro*/
        /* store do chunk no diretório */
        /* em caso de sucesso criar a mensagem STORED para enviar para o canal MC para o peer que envio o PUTCHUNK*/

        int chuckSize = msg.getBody().getBytes().length;
        if(!getChunk(msg) && this.peer.getAvailableStorage() >= chuckSize) {
            // Checks if ChuckNo is already stored on the peer
            File out = new File(this.peer.FILE_STORAGE_PATH + "/" + msg.getHeader().getSenderId() + "/" + msg.getHeader().getFileId() + "/" + msg.getHeader().getChuckNo() + ".txt");
            if(out.exists()) return false;

            // If the directory Storage/SenderId/FileId does not exist creates it
            if(!out.getParentFile().exists()) out.getParentFile().mkdirs();

            out.createNewFile();

            FileWriter writer = new FileWriter(out);
            writer.write(msg.getBody());
            writer.close();

            this.peer.setCurrentSystemMemory(this.peer.getCurrentSystemMemory() + msg.getBody().length());

            // what to do with replication degree???
            // increase replication degree and save in non volatile memory



            File dir = new File("/Users/pankaj");
            File notExists = new File("/Users/pankaj/notafile");

            System.out.println("/Users/pankaj/source.txt is file?"+file.isFile());
            System.out.println("/Users/pankaj/source.txt is directory?"+file.isDirectory());

            System.out.println("/Users/pankaj is file?"+dir.isFile());
            System.out.println("/Users/pankaj is directory?"+dir.isDirectory());

            System.out.println("/Users/pankaj/notafile is file?"+notExists.isFile());
            System.out.println("/Users/pankaj/notafile is directory?"+notExists.isDirectory());
        }

        
    }

    public void sendChunk(byte[] chunk, int chuckNo) {
        Header requestHeader = new Header();
        requestHeader.setVersion(this.peer.getVersion());
        requestHeader.setSenderId(Integer.toString(this.peer.getId()));
        requestHeader.setFileId(this.fileId);
        requestHeader.setChuckNo(Integer.toString(chuckNo));
        requestHeader.setReplicationDeg(Integer.toString(this.replicationDeg));

        Message request = new Message(requestHeader);
        request.setBody(chunk);

        Dispatcher dispatcher = new Dispatcher();
        ??dispatcher.send?
    }

    public boolean getChunk(Message msg){

        /*Message message = new Message();*/


    }

}
