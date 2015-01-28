/*
Author Luke Stowe
Author Ashley Deane
 */


import java.io.*;
import java.util.*;


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

                            metalHalves[arrayCount][1] = (j / 2) - (count2 / 2);
                            metalHalves[arrayCount][0] = i;
                            arrayCount++;


                            count2 = 0;

                            flag = 1;//reset the flag
                        }
                        //metal[i][j/2]=0;

                    }
                }
            }
            //first sort
            Arrays.sort(metalHalves, new Comparator<int[]>() {
                @Override
                public int compare(final int[] entry1, final int[] entry2) {
                    final int x1 = entry1[1];
                    final int x2 = entry2[1];
                    if (x1 < x2)
                        return 1;
                    else if (x1 == x2)
                        return 0;
                    else
                        return -1;

                }
            });


            //second sort metal
            int[][] finalMetal = new int[4][20];
            finalMetal[0][0] = metalHalves[0][1];
            finalMetal[0][1] = metalHalves[0][0];
            for (int i = 2, j = 1, k = 0; k < 3 && j < 100; i++, j++) {
                if (metalHalves[j][1] != 0 && (metalHalves[j][1] >= finalMetal[k][0] - 5 && metalHalves[j][1] <= finalMetal[k][0] + 5)) {
                    finalMetal[k][i] = metalHalves[j][0];
                } else if (metalHalves[j][1] != 0) {
                    k++;
                    j++;
                    i = 1;
                    finalMetal[k][0] = metalHalves[j][1];
                    finalMetal[k][1] = metalHalves[j][0];
                }

            }//get single coordinate for each metal
            int[][] medians = new int[4][2];
            for (int i = 0; i < 4; i++) {
                int pos = 0;
                int countMedian = 0;
                for (int k = 1; k < 20; k++) {
                    int x = finalMetal[i][k];
                    pos += x;
                    if (x != 0) {

                        countMedian++;
                    }

                }
                if (countMedian != 0) {
                    medians[i][0] = pos / countMedian;
                    medians[i][1] = finalMetal[i][0];
                }

            }


            Point p = new Point(medians[2][0], medians[2][1]);//y,x

            //flood fill line detection


            Queue<Point> streak =  flood(pixelData,p);


            queuePrint(streak);//prink streak coordinates


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


    private static Queue flood(Pixel pixelData[][],Point p)
    {
        Point metal = p;
        Queue<Point> streak = new LinkedList<Point>();
        Queue <Point>coord=new LinkedList<Point>();
        coord.add(metal);
        streak.add(metal);
        while(coord.peek()!=null){
            Point p1 = coord.remove();
            if(checkRange(pixelData[p1.getY()][p1.getX()+1])){//right
                p1 = new Point(p1.getY(),p1.getX()+1);
                pixelData[p1.getY()][p1.getX()].setPixelValue(0);
                coord.add(p1);//push
                streak.add(p1);
            }
            if(checkRange(pixelData[p1.getY()][p1.getX()-1]))//left
            {
                p1 = new Point(p1.getY(),p1.getX()-1);
                pixelData[p1.getY()][p1.getX()].setPixelValue(0);
                coord.add(p1);//push
                streak.add(p1);
            }
            if(checkRange(pixelData[p1.getY()][p1.getX()]))//up
            {
                p1 = new Point(p1.getY()+1,p1.getX());
                pixelData[p1.getY()+1][p1.getX()].setPixelValue(0);
                coord.add(p1);//push
                streak.add(p1);
            }
            if(checkRange(pixelData[p1.getY()-1][p1.getX()])) //down
            {
                p1 = new Point(p1.getY()-1,p1.getX());
                pixelData[p1.getY()][p1.getX()].setPixelValue(0);
                coord.add(p1);
                streak.add(p1);
            }

        }
        return streak;
    }
    private static float getSlope(int y1, int x1, int y2, int x2){
        return (y1 - y2)/(x1 - x2);
    }
    //check for flood fill
    private static boolean checkRange(Pixel p) {
        //bone(860-900)
        //soft tissue(760-860)
        if (p.getPixelValue() >= 760 && p.getPixelValue() <= 900) {
            return true;
        } else if (p.getPixelValue() > 4000) {//metal
            return true;
        } else {
            return false;
        }
    }
    //print queue
    private static void queuePrint(Queue<Point> streak){
        System.out.println("Here");
        for(Point s : streak){
            System.out.println(s.getY() + "," + s.getX());
        }
    }


}
