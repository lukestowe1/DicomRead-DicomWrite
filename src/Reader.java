/*
Author Luke Stowe
Tag Along Ashley Robert Deane
 */


import java.io.*;
import java.util.Arrays;
import java.util.Collections;


class Reader {
    public static void main(String[] argv) throws Exception {
        //Read from an input stream
        InputStream is = new BufferedInputStream(new FileInputStream("im0.dcm"));
        DataInputStream in = new DataInputStream(is);
        Byte [] bytes = new Byte[600000];
        Byte [] endOfhead1 = {(byte)0xe0,(byte)0x7f,(byte)0x10,(byte)0x00,(byte)0x4f,
                (byte)0x57,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x08,(byte)0x00};//512
        //Byte [] endOfhead2 = {(byte)0xe0,(byte)0x7f,(byte)0x10,(byte)0x00,(byte)0x4f,
        // (byte)0x57,(byte)0x00,(byte)0x00,(byte)0x80,(byte)0x40,(byte)0x08,(byte)0x00};//520


        //handle writing back to a file
        BufferedOutputStream o =new BufferedOutputStream(new FileOutputStream("im2.dcm"));
        DataOutputStream out = new DataOutputStream(o);
        int count = 0;
        try
        {
            while(in.available() > 0)// end of file + count size
            {
                bytes [count] = in.readByte();
                count++;
            }
            int pixelStart=Collections.indexOfSubList(Arrays.asList(bytes),Arrays.asList(endOfhead1))+endOfhead1.length;
            System.out.println("Pixel values start at : "+pixelStart);

            short firstPix = (short) ((bytes[3241] << 8) & 0x3F | (bytes[3240]  ));
            System.out.println("First pixel: "+ firstPix);
            //test to change byte value from D to I
            bytes[128]=73;

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