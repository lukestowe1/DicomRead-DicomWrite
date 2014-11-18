
import java.io.*;

class Reader {
    public static void main(String[] argv) throws Exception {
        //Read from an input stream
        InputStream is = new BufferedInputStream(new FileInputStream("im0.dcm"));
        DataInputStream in = new DataInputStream(is);
        byte [] bytes = new byte[20001];



        //handle writing back to a file
        BufferedOutputStream o =new BufferedOutputStream(new FileOutputStream("im2.dcm"));
        DataOutputStream out = new DataOutputStream(o);

        try
        {
            while(in.available()> 0x0)//convert to hex for bytes maybe???????
            {
                bytes [i] = in.readByte();

            }
            System.out.println((char)bytes[128]);



            //test to change byte value from D to I
            bytes[128]=73;
            System.out.println((char)bytes[128]);
            for(int i=0;i<2001;i++)
            {
                //write to new  image
                out.writeByte(bytes[i]);


            }

            // close stuff
            out.close();
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }



    }
}