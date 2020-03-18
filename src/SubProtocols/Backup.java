package SubProtocols;

import Peer.Peer;
import Message.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Backup {
    /* private String fileId;
    private int replicationDeg;
    private int chunkNumber;
    private bytes[] body;
    private String senderId;*/
    private Peer parentPeer;
    private Message msg;

    public Backup(Peer peer, Message msg){

        this.parentPeer = peer;
        this.msg = msg;
    }

    public boolean putChunk(Message msg) throws IOException {

        /* Não sei se temos que verificar que o senderID aqui ou se verifcamos quando recebemos a messagem no canal */
        /* verificar se o chunk já está stored no peer*/
        /* caso não esteja verificar se existe espaço suficiiente para guardar o ficheiro*/
        /* store do chunk no diretório */
        /* em caso de sucesso criar a mensagem STORED para enviar para o canal MC para o peer que envio o PUTCHUNK*/

        int chuckSize = msg.getBody().getBytes().length;
        if(!getChunk(msg) && this.parentPeer.getAvailableStorage() >= chuckSize) {
            // Checks if ChuckNo is already stored
            File out = new File(this.parentPeer.FILE_STORAGE_PATH + "/" + msg.getHeader().getSenderId() + "/" + msg.getHeader().getFileId() + "/" + msg.getHeader().getChuckNo() + ".txt");
            if(out.exists()) return false;
            if(!out.getParentFile().exists()) out.getParentFile().mkdirs();

            out.createNewFile();

            FileWriter writer = new FileWriter(out);
            writer.write(msg.getBody());
            writer.close();

            this.parentPeer.setCurrentSystemMemory(this.parentPeer.getCurrentSystemMemory() + msg.getBody().length());

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

    public boolean getChunk(Message msg){

        /*Message message = new Message();*/


    }

}
