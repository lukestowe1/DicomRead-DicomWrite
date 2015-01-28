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
        InputStream is = new BufferedInputStream(new FileInputStream("Phantom_Artifact.dcm"));//input image
        DataInputStream in = new DataInputStream(is);
        Byte[] bytes = new Byte[600000];//array to store all bytes of file
        Byte[] endOfhead1 = {(byte) 0xe0, (byte) 0x7f, (byte) 0x10, (byte) 0x00, (byte) 0x4f,
                (byte) 0x57, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00};//End of head array for 512.dcm file
        Byte[] endOfhead2 = {(byte) 0xe0, (byte) 0x7f, (byte) 0x10, (byte) 0x00, (byte) 0x4f,
                (byte) 0x57, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x40, (byte) 0x08, (byte) 0x00};//End of head array for 520.dcm file

        int pSize = 520;//size of pixel array either 512/520 will get set further down
        int positionToWrite = 0;//position in bytes to start writing all the new pixels out to
        int pixelStart = 0;//start position of pixels in terms of bytes


        //handle writing back to a file
        BufferedOutputStream o = new BufferedOutputStream(new FileOutputStream("Output.dcm"));
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

            Pixel[][] pixelData = new Pixel[pSize][pSize];//take in pixel objects
            positionToWrite = pixelStart;  //position to write back to
            System.out.println("Pixel values start at : " + pixelStart);

            int[][] metalHalves = new int[100][2];//store middle values of any metal object detected in the image
            int count2 = 0;
            int arrayCount = 0;
            int flag = 1;
            int flagFar=0;

            //int[][] metal = new int[pSize][pSize];
            for (int i = 0; i < pSize; i++) {
                for (int j = 0; j < pSize * 2; j += 2) { //increment by two so dont merge wrong bytes
                    Pixel p = new Pixel(bytes[pixelStart + 1], bytes[pixelStart]);//change every byte to Pixel
                    pixelData[i][j / 2] = p;
                    pixelStart += 2;
                    if (p.getPixelValue() > 4000) {//detect for metal object
                        //metal[i][j/2]=1;
                        count2++;
                        flag = 0;
                    } else {
                        if (flag == 0)//after metal has been detected calculate horizontal middle value
                        {
                            int k = (j/2) - (count2/2);

                            if(arrayCount!=0 && (k < (metalHalves[0][1]-15) || k > (metalHalves[0][1]+15))) {

                                metalHalves[arrayCount+20][1] = (j / 2) - (count2 / 2);
                                metalHalves[arrayCount+20][0] = i;
                                flagFar++;
                                if(flagFar<1)
                                {
                                    arrayCount++;
                                }
                                else
                                {
                                    flagFar=0;
                                }

                            }
                            else
                            {
                                metalHalves[arrayCount][1] = (j / 2) - (count2 / 2);
                                metalHalves[arrayCount][0] = i;
                                arrayCount++;

                            }
                            count2 = 0;

                            flag = 1;//reset the flag
                        }
                        //metal[i][j/2]=0;

                    }
                }
            }
            for (int i = 0; i < 60; i++)//test print out middle values
            {
                for (int j = 0; j < 1; j++) {
                    System.out.println(metalHalves[i][j] + " " + metalHalves[i][j + 1]);

                }
            }


            for (int i = 0; i < count; i++) {
                //write to new  image
                out.writeByte(bytes[i]);
            }


            // close stuff
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}