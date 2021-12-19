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

    public byte[] getBytes(){
        int size = 1 + this.data.length;
        byte[] res = new byte[size];
        res[0] = this.type;
        for(int i = 1 ; i < size ; i++){
            res[i] = this.data[i-1];
        }
        return res;
    }

    public boolean type2Positive(){
        boolean b = (this.data[1] != 0);
        return (b && (this.getType() == 2));
    }

    public boolean type2Negative(){
        boolean b = (this.data[1] == 0);
        return (b && (this.getType() == 2));
    }
}