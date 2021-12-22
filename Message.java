import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Message{
    private byte type;
    private byte[] data;

    public Message(int type, byte[] data)
    {
        this.type = Integer.valueOf(type).byteValue();
        this.data = data;
    }

    //WriteFileRequest Constructor
    public Message(int type, int filePathSize, byte[] filePath, int passwordSize, byte[] password){
        this.type = Integer.valueOf(type).byteValue();
        byte[] filePathSizeArr = new byte[]{Integer.valueOf(filePathSize).byteValue()};
        byte[] passwordSizeArr = new byte[]{Integer.valueOf(passwordSize).byteValue()};
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try{
            outputStream.write( filePathSizeArr );
            outputStream.write( filePath );
            outputStream.write( passwordSizeArr );
            outputStream.write( password );
        }
        catch(IOException e){
            LoggerUtil.getLogger().severe(e.getMessage());
        }
        this.data = outputStream.toByteArray();
    }

    //Data Constructor
    public Message(int type, int packetNumber,int file_size, byte[] fileData){
        this.type = Integer.valueOf(type).byteValue();
        byte packetNumberArr[] = new byte[3];
        packetNumberArr[0] = (byte) ((packetNumber & 0x00FF0000) >> 16);
        packetNumberArr[1] = (byte) ((packetNumber& 0x0000FF00) >> 8);
        packetNumberArr[2] = (byte) ((packetNumber& 0x000000FF) >> 0);
        byte[] file_size_arr = new byte[1];
        file_size_arr[0] =  (byte) ((file_size & 0x000000FF));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try{
            outputStream.write( packetNumberArr );
            outputStream.write( file_size_arr );
            outputStream.write( fileData );
        }
        catch(IOException e){
            LoggerUtil.getLogger().severe(e.getMessage());
        }

        this.data = outputStream.toByteArray();
    }

    //Ack Constructor
    public Message(int type, int packetNumber){
        this.type = Integer.valueOf(type).byteValue();
        this.data = new byte[3];
        this.data[0] = (byte) ((packetNumber & 0x00FF0000) >> 16);
        this.data[1] = (byte) ((packetNumber & 0x0000FF00) >> 8);
        this.data[2] = (byte) ((packetNumber & 0x000000FF) >> 0);
    }

    public Message(byte[] byte_arr){
        this.type = byte_arr[0];
        this.data = Arrays.copyOfRange(byte_arr, 1, byte_arr.length); 
    }

    public int getType(){
        Byte b = this.type;
        return b.intValue();
    }

    public byte[] getData(){
        return this.data;
    }

    public byte[] getBytes(){
        int size = 1 + this.data.length;
        byte[] res = new byte[size];
        res[0] = this.type;
        for(int i = 1 ; i < size ; i++){
            res[i] = this.data[i-1];
        }
        return res;
    }

    public int getPacketNumber(){
        if((this.getType() != 2) && (this.getType() != 3))
            return 0;
        return  this.byteToInt(this.data, 3);
    }

    public boolean isLastPacket(){
        return(Byte.toUnsignedInt(this.data[3]) < 255 && this.getType() == 2);
    }

    public void printData(){
        System.out.println(Arrays.toString(this.data));
    }

    public int fileDataSize(){
        if(this.getType() != 2)
            return 0;
        return Byte.toUnsignedInt(this.data[3]);
    }

    private int byteToInt(byte[] bytes, int length) {
        int val = 0;
        if(length>4) throw new RuntimeException("Demasiado Grande para guardar num int!");
        for (int i = 0; i < length; i++) {
            val=val<<8;
            val=val|(bytes[i] & 0xFF);
        }
        return val;
    }
}