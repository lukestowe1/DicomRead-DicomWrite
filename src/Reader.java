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
            System.out.println(" "+medians[1][1]+" "+medians[1][0]);
            List<Point> zerosM1 = new LinkedList<Point>();
            List<Point> zerosM2 = new ArrayList<Point>();
            Point p = new Point(0,0);
            for(int i = 0 ;i < 390;i++)
            {
                for(int j = 0; j<pSize;j++)
                {
                    p=new Point(i,j);
                    if(pixelData[i][j].getPixelValue()==0)
                    {
                        float t = p.getSlope(medians[1][0], medians[1][1]);

                        if(!zerosM1.contains(p))
                            zerosM1.add(p);

                        t = p.getSlope(medians[1][0], medians[1][1]);

                        if(!zerosM2.contains(p))
                            zerosM2.add(p);

                    }

                }
            }

            Collections.sort(zerosM1, new Comparator<Point>() {
                @Override
                public int compare(Point  p1, Point  p2)
                {
                    if(p1.returnSlope()>p2.returnSlope())
                        return 1;
                    else
                        return -1;
                }
            });
            Collections.sort(zerosM2, new Comparator<Point>() {
                @Override
                public int compare(Point  p1, Point  p2)
                {
                    if(p1.returnSlope()>p2.returnSlope())
                        return 1;
                    else
                        return -1;
                }
            });

            //left metal
            System.out.println(zerosM1.size());
            Point metal1 = new Point(medians[1][0],medians[1][1]);
            for(int i =0;i<zerosM1.size();i++)
            {
                int checker=0;
                Point p1 = zerosM1.get(i);
                Point mid = p1.midpoint(metal1);
                while(checkRange(pixelData[mid.getY()][mid.getX()])==true && checker !=6)
                {
                    mid = mid.midpoint(metal1);
                    checker++;

                }
                if(checker !=6){
                    zerosM1.remove(p1);
                }
            }
            System.out.println("new size: "+zerosM1.size());

            System.out.println(zerosM2.size());
            Point metal2 = new Point(medians[0][0],medians[0][1]);
            for(int i =0;i<zerosM2.size();i++)
            {
                int checker=0;
                Point p1 = zerosM2.get(i);
                Point mid = p1.midpoint(metal2);
                while(checkRange(pixelData[mid.getY()][mid.getX()])==true && checker !=6)
                {
                    mid = mid.midpoint(metal2);
                    checker++;

                }
                if(checker != 6 ){
                    zerosM2.remove(p1);
                }
            }
            System.out.println(zerosM2.size());
            /*System.out.println("-------"+medians[1][1]+ "------"+medians[1][0]);
            float t = getSlope(medians[1][0],medians[1][1],240,99);
            System.out.println(t);*/



            //Queue<Point> streak =  flood(pixelData,p);
            //queuePrint(zerosM1);//prink streak coordinates


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


    //print queue
    private static void queuePrint(List<Point> streak){
        System.out.println("Here");
        for(Point s : streak){
            System.out.println(s.returnSlope());
        }
    }
    private static boolean checkRange(Pixel p) {
        //soft tissue(760-860)
        if (p.getPixelValue() >= 760 && p.getPixelValue() <= 900) {
            return true;
        } else if (p.getPixelValue() > 4000) {
            return true;
        } else if (p.getPixelValue() == 0) {//metal
            return true;
        } else {
            return false;
        }
    }



}
