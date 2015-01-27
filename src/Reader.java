/*
Author Luke Stowe
Author Ashley Deane
 */


import java.io.*;
import java.util.Arrays;
import java.util.Collections;


class Reader {
    public static void main(String[] argv) throws Exception {
        //Read from an input stream
        InputStream is = new BufferedInputStream(new FileInputStream("Phantom_Artifact.dcm"));
        DataInputStream in = new DataInputStream(is);
        Byte [] bytes = new Byte[600000];
        Byte [] endOfhead1 = {(byte)0xe0,(byte)0x7f,(byte)0x10,(byte)0x00,(byte)0x4f,
                (byte)0x57,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x08,(byte)0x00};//512
        Byte [] endOfhead2 = {(byte)0xe0,(byte)0x7f,(byte)0x10,(byte)0x00,(byte)0x4f,
         (byte)0x57,(byte)0x00,(byte)0x00,(byte)0x80,(byte)0x40,(byte)0x08,(byte)0x00};//520

        int pSize = 520;
        int positionToWrite=0;
        int pixelStart=0;


        //handle writing back to a file
        BufferedOutputStream o =new BufferedOutputStream(new FileOutputStream("Output.dcm"));
        DataOutputStream out = new DataOutputStream(o);
        int count = 0;
        try {
            while (in.available() > 0)// end of file + count size
            {
                bytes[count] = in.readByte();
                count++;
            }
            if (Collections.indexOfSubList(Arrays.asList(bytes), Arrays.asList(endOfhead2)) + endOfhead2.length > 12) {
                pixelStart = Collections.indexOfSubList(Arrays.asList(bytes), Arrays.asList(endOfhead2)) + endOfhead2.length;
                pSize = 520;
            } else {
                pixelStart = Collections.indexOfSubList(Arrays.asList(bytes), Arrays.asList(endOfhead1)) + endOfhead1.length;
                pSize = 512;
            }

            Pixel[][] pixelData = new Pixel[pSize][pSize];
            positionToWrite = pixelStart;  //position to write back to
            System.out.println("Pixel values start at : " + pixelStart);

            int [][]metalHalves = new int [100][2];
            int count2 =0;
            int arrayCount=0;
            int flag=1;
            //int[][] metal = new int[pSize][pSize];
            for (int i = 0; i < pSize; i++) {
                for (int j = 0; j < pSize * 2; j += 2) { //increment by two so dont merge wrong bytes
                    Pixel p = new Pixel(bytes[pixelStart + 1], bytes[pixelStart]);
                    pixelData[i][j / 2] = p;
                    pixelStart += 2;
                    if (p.getPixelValue() > 4000) {
                        //metal[i][j/2]=1;
                        count2++;
                        flag=0;
                    }
                    else
                    {
                        if(flag==0)
                        {
                            metalHalves[arrayCount][1]=(j/2)-(count2/2);
                            metalHalves[arrayCount][0]=i;
                            count2=0;
                            arrayCount++;
                            flag=1;
                        }
                        //metal[i][j/2]=0;

                    }
                }
            }
            for(int i = 0; i<30;i++)
            {
                for(int j = 0 ; j<1;j++)
                {
                    System.out.println(metalHalves[i][j]+" "+metalHalves[i][j+1]);

                }
            }




            for(int i=0;i<50000;i++)
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