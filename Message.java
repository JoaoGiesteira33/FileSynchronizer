import java.util.Arrays;

public class Message{
    private byte type;
    private byte[] data;

    public Message(int type, byte[] data)
    {
        this.type = Integer.valueOf(type).byteValue();
        this.data = data;
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
}