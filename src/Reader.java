
import java.io.*;

class Reader {
    public static void main(String[] argv) throws Exception {
        //Read from an input stream
        InputStream is = new BufferedInputStream(new FileInputStream("input.dcm"));
        DataInputStream in = new DataInputStream(is);
        byte [] bytes = new byte[20001];



        //handle writing back to a file
        BufferedOutputStream o =new BufferedOutputStream(new FileOutputStream("output.dcm"));
        DataOutputStream out = new DataOutputStream(o);

        try
        {
            for(int i=0;i<2001;i++)
            {
                bytes [i] = in.readByte();

            }
            System.out.println((char)bytes[128]);



            //test to change byte value
            bytes[128]=73;
            for(int i=0;i<2001;i++)
            {
                //write to new improved image
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