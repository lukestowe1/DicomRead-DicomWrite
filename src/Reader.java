/*
Author Luke Stowe
Author Ashley Deane
 */


//import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;


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

        int pSize;//size of pixel array either 512/520 will get set further down
        int positionToWrite;//position in bytes to start writing all the new pixels out to
        int pixelStart;//start position of pixels in terms of bytes


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


            /**
             * Get Metal Point in image
             * And convert bytes to pixels
             */
            int[][] metalHalves = new int[100][2];//store middle values of any metal object detected in the image
            int count2 = 0;
            int arrayCount = 0;
            int flag = 1;

            for (int i = 0; i < pSize; i++) {
                for (int j = 0; j < pSize * 2; j += 2) { //increment by two so don't merge wrong bytes
                    Pixel p = new Pixel(bytes[pixelStart + 1], bytes[pixelStart]);//change every byte to Pixel
                    pixelData[i][j / 2] = p;

                    pixelStart += 2;
                    if (p.getPixelValue() > 4000) {//detect for metal object
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
                    }
                }
            }
            //Sort different metal objects by location
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
            }
            //get centre Point of each metal object
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
            /**
             * End
             */

            //Printing centre points of Metal objects
            System.out.println(" "+medians[1][1]+" "+medians[1][0]);


            /**
             * Getting Zeros, with slopes to points
             * This will lead to finding the streaks
             * As Zeros are air effected by the streak
             */
            //Zeros with slope from first metal Object
            List<Point> zerosM1 = new LinkedList<Point>();

            //Zeros with slope from Second metal Object
            List<Point> zerosM2 = new ArrayList<Point>();

            Point p;
            //Adding all Zeros to both arrays
            for(int i = 1 ;i < 390;i++)
            {
                for(int j = 1; j<pSize;j++)
                {
                    p = new Point(i,j);
                    if(pixelData[i][j].getPixelValue()==0)
                    {
                        Point p1 = new Point(i,j);
                        int c1=i;
                        if(pixelData[p.getY()+1][p.getX()].getPixelValue()!=0) {
                            while (pixelData[p1.getY() - 1][p1.getX()].getPixelValue() == 0 && c1 > 0) {//avoid running off image out of bounds fix
                                p1 = new Point(c1--, j);
                            }
                            p = p.midpoint(p1);

                            p.getSlope(medians[1][0], medians[1][1]);//left
                            //Debugging
                            //pixelData[p.getY()][p.getX()].setPixelValue(-1);
                            zerosM1.add(p);

                            p.getSlope(medians[0][0], medians[0][1]);//right
                            zerosM2.add(p);
                        }

                    }

                }
            }

            /*Collections.sort(zerosM1, new Comparator<Point>() {
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
            });*/

            //left metal
            //System.out.println(zerosM1.size());
            Point metal1 = new Point(medians[1][0],medians[1][1]);
            //System.out.println("First metal point y :"+metal1.getY()+" x: "+metal1.getX());

            for(int i =0;i<zerosM1.size();i++)
            {

                Point p1 = zerosM1.get(i);
                Point mid = p1.midpoint(metal1);
                //pixelData[p1.getY()][p1.getX()].setPixelValue(-10);
                if(!checkRange(pixelData[mid.getY()][mid.getX()])){
                    zerosM1.remove(p1);
                }
                /*else
                {
                    pixelData[p1.getY()][p1.getX()].setPixelValue(-1);
                }*/
            }
            //System.out.println("new size: "+zerosM1.size());

            //System.out.println(zerosM2.size());
            Point metal2 = new Point(medians[0][0],medians[0][1]);
            //System.out.println("Second metal point y :"+metal2.getY()+" x: "+metal2.getX());

            for(int i =0;i<zerosM2.size();i++)
            {

                Point p1 = zerosM2.get(i);
                Point mid = p1.midpoint(metal2);

                if(!checkRange(pixelData[mid.getY()][mid.getX()])){
                    zerosM2.remove(p1);
                }
                /*else
                {
                    pixelData[p1.getY()][p1.getX()].setPixelValue(-1);
                }*/
            }


            //testing stuff
            /*
            for(Point p3:zerosM1)
            {
                pixelData[p3.getY()][p3.getX()].setPixelValue(-1);
            }
            for(Point p3:zerosM2)
            {
                pixelData[p3.getY()][p3.getX()].setPixelValue(700000);
            }*/
           /*for(int i = 0; i< 300; i++)
            {
                for(int j = 0; j <pSize; j++ )
                {
                    System.out.printf("%5d ", pixelData[i][j].getPixelValue());
                }
                System.out.println();
            }*/

            /*Collections.sort(zerosM2, new Comparator<Point>() {
                @Override
                public int compare(Point  p1, Point  p2)
                {
                    if(p1.returnSlope()>p2.returnSlope())
                        return 1;
                    else
                        return -1;
                }
            });*/

            System.out.println(zerosM2.size());
            /*System.out.println("-------"+medians[1][1]+ "------"+medians[1][0]);
            float t = getSlope(medians[1][0],medians[1][1],240,99);
            System.out.println(t);*/



            //Queue<Point> streak =  flood(pixelData,p);
            List<Point> middleEdge = new LinkedList<Point>();
            //queuePrint(zerosM2);//prink streak coordinates
            //Point pix = new Point(180,88);
            //pix.getSlope(medians[1][0],medians[1][1]);
            Point pix2;

            /**
             * Streak ID and profiling
             * ----- To be moved to own method
             */
            for(Point pix: zerosM1) {

                for (int i = 0; i < pSize; i++) {

                    for (int k = 0; k < pSize; k++) {

                            pix2 = new Point(i, k);

                            if (pix.equationLine(pix2, pix.returnSlope())) {
                                if (pixelData[i][k].getPixelValue() >= 500 && pixelData[i][k].getPixelValue() <= 960 && pix.returnSlope() > -1.5 && pix.returnSlope() < 1.5)//tissue fix
                                {
                                    int moverC = i;
                                    Point mover = new Point(i, k);
                                    mover.getSlope(pix.getY(), pix.getX());
                                    mover.returnPerpendicular();
                                    while (pixelData[mover.getY()][mover.getX()].getPixelValue() >= 500 && pixelData[mover.getY()][mover.getX()].getPixelValue() <= 990) {
                                        moverC--;
                                        float sl = mover.returnSlope();
                                        //System.out.println(mover.getY()+"---------"+mover.getX()+"----------"+mover.returnSlope()+"-------"+mover.returnPerpendicular());
                                        if (sl > -0.05 && sl < 0.05) {
                                            mover = new Point(moverC, mover.getX());
                                        } else {
                                            int x = mover.getPerpY(moverC);
                                            if (x > 519) {
                                                x = 519;
                                            }
                                            mover = new Point(moverC, x);

                                        }


                                        //System.out.println(mover.getY()+"---------"+mover.getX());
                                    }
                                    float s1 = mover.returnSlope();
                                    int x;
                                    if(s1 > -0.05 && s1 < 0.05){
                                        x = mover.getX();
                                    }
                                    else {
                                        x = mover.getPerpY(mover.getY() - 10);
                                        if (x < 1) {
                                            //System.out.println("X:------" + mover.getX() + " Y--------- " + mover.getY() + "Slope:-----" + mover.returnSlope());
                                            x = 1;
                                        }
                                    }
                                    Point outUpper;
                                    if(mover.getY() - 10 < 1) {
                                        outUpper = new Point(1, x);
                                    }
                                    else{
                                        outUpper = new Point(mover.getY()-10,x);
                                    }
                                    Point upper = new Point(mover.getY(), mover.getX());
                                    moverC++;
                                    mover = new Point(i, k);
                                    while (pixelData[mover.getY()][mover.getX()].getPixelValue() >= 500 && pixelData[mover.getY()][mover.getX()].getPixelValue() <= 990) {
                                        moverC++;
                                                    //System.out.println(mover.getY()+"---------"+mover.getX()+"----------"+mover.returnSlope()+"-------"+mover.returnPerpendicular());
                                        float sl = mover.returnSlope();
                                                    //System.out.println(mover.getY()+"---------"+mover.getX()+"----------"+mover.returnSlope()+"-------"+mover.returnPerpendicular());
                                        if (sl > -0.05 && sl < 0.05) {
                                            mover = new Point(moverC, mover.getX());
                                        } else {
                                            x = mover.getPerpY(moverC);
                                            if (x > 519) {
                                                x = 519;
                                            }
                                            mover = new Point(moverC, x);

                                        }

                                    }
                                    Point lower = new Point(mover.getY(), mover.getX());
                                    Point mid = lower.midpoint(upper);
                                    s1 = mover.returnSlope();
                                    if(s1 > -0.05 && s1 < 0.05){
                                        x = mover.getX();
                                    }
                                    else {
                                        x = mover.getPerpY(mover.getY() + 10);
                                        if (x > 519) {
                                            //System.out.println("X:------"+mover.getX()+" Y--------- "+mover.getY()+"Slope:-----"+mover.returnSlope());
                                            x = 519;
                                        }
                                    }
                                    Point outLower;
                                    if(mover.getY()+10 > 519) {
                                        outLower = new Point(519, x);
                                    }
                                    else{
                                        outLower = new Point(mover.getY() + 10, x);
                                    }
                                    mid.getSlope(medians[1][0], medians[1][1]);
                                    mid.setLimit(upper,lower,outUpper,outLower);

                                    if(upper.distance(lower) > 4 && !middleEdge.contains(mid))
                                        middleEdge.add(mid);
                                }
                            }
                        }
                }
            }

            /**
             * End
             */
        /*
            List<Point> independentSlopes = new ArrayList<Point>();

            //Debug Print
            System.out.println("Size--------------" + middleEdge.size());

            for(Point p4 : middleEdge)
            {
                //pixelData[p4.getY()][p4.getX()].setPixelValue(3000);
                if(!independentSlopes.contains(p4))
                {
                    //pixelData[p4.getY()][p4.getX()].setPixelValue(3000);
                    independentSlopes.add(p4);
                }
            }

            //Debug Print
            System.out.println(count5+"Independent slopes : ---------"+independentSlopes.size());


            Collections.sort(independentSlopes, new Comparator<Point>() {
                @Override
                public int compare(Point  p1, Point  p2)
                {
                    if(p1.returnSlope()>p2.returnSlope())
                        return 1;
                    else
                        return -1;
                }
            });

            //Debug Print
            for(Point pix: independentSlopes) {
                System.out.println("x:  " + pix.getX() + " Y:   " + pix.getY() + "  Slope:   " + pix.returnSlope());

            }

            Point slice;
            List<Point> slices = new LinkedList<Point>();

            for(Point pix: independentSlopes) {

                            slice = new Point(pix.getY(),pix.getX());

                                if(pixelData[slice.getY()][slice.getX()].getPixelValue()>500 && pixelData[slice.getY()][slice.getX()].getPixelValue() <960)//we are now at light bit of streak maybe one too far
                                {

                                    if (pixelData[slice.getY()][slice.getX()].getPixelValue() < 1300) {
                                        Point mover = new Point(slice.getY(),slice.getX());
                                        int moverC = slice.getY();
                                        while (pixelData[mover.getY()][mover.getX()].getPixelValue() >= 500 && pixelData[mover.getY()][mover.getX()].getPixelValue() <= 990) {
                                            moverC--;
                                            //System.out.println(mover.getY()+"---------"+mover.getX()+"----------"+mover.returnSlope()+"-------"+mover.returnPerpendicular());
                                            float sl = mover.returnSlope();
                                            //System.out.println(mover.getY()+"---------"+mover.getX()+"----------"+mover.returnSlope()+"-------"+mover.returnPerpendicular());
                                            if (sl > -0.05 && sl < 0.05) {
                                                mover = new Point(moverC, mover.getX());
                                            } else {
                                                int x = mover.getPerpY(moverC);
                                                if (x > 519) {
                                                    x = 519;
                                                }
                                                mover = new Point(moverC, x);
                                            }
                                        }


                                        float s1 = pix.returnSlope();
                                        int x;
                                        if(s1 > -0.05 && s1 < 0.05){
                                            x = mover.getX();
                                        }
                                        else {
                                            x = mover.getPerpY(mover.getY() - 10);
                                            if (x < 1) {
                                                x = 1;
                                            }
                                        }
                                        Point outUpper;
                                        if(mover.getY()-10 < 1) {
                                            outUpper = new Point(1, x);
                                        }
                                        else{
                                            outUpper = new Point(mover.getY()-10,x);
                                        }
                                        Point upper = new Point(mover.getY(), mover.getX());
                                        moverC++;
                                        mover = new Point(slice.getY(),slice.getX());
                                        while (pixelData[mover.getY()][mover.getX()].getPixelValue() >= 500 && pixelData[mover.getY()][mover.getX()].getPixelValue() <= 990) {
                                            moverC++;
                                            //System.out.println(mover.getY()+"---------"+mover.getX()+"----------"+mover.returnSlope()+"-------"+mover.returnPerpendicular());
                                            float sl = mover.returnSlope();
                                            //System.out.println(mover.getY()+"---------"+mover.getX()+"----------"+mover.returnSlope()+"-------"+mover.returnPerpendicular());
                                            if (sl > -0.05 && sl < 0.05) {
                                                mover = new Point(moverC, mover.getX());
                                            } else {
                                                x = mover.getPerpY(moverC);
                                                if (x > 519) {
                                                    x = 519;
                                                }
                                                mover = new Point(moverC, x);
                                            }
                                        }
                                        s1 = pix.returnSlope();
                                        if(s1 > -0.05 && s1 < 0.05){
                                            x = mover.getX();
                                        }
                                        else {
                                            x = mover.getPerpY(mover.getY() + 10);
                                            if (x > 519) {
                                                x = 519;
                                            }
                                        }
                                        Point outLower;
                                        if(mover.getY()+10 > 519) {
                                            outLower = new Point(519, x);
                                        }
                                        else{
                                            outLower = new Point(mover.getY() + 10, x);
                                        }
                                        Point lower = new Point(mover.getY(), mover.getX());

                                        slice.setLimit(upper, lower, outUpper, outLower);
                                        slices.add(slice);
                                    }

                                   /*while(count<distance)distance between upper and lower or check not equal to lower or one before lower not sure
                                   {
                                       //get some profile U shaped with pix being the middle
                                   }
                                }
                            }
            */

            //alter Pixels to illustrate streaks
            int gh=0;
            int gh1=0;

            int midgh=0;
            int midgh1=0;
            for(Point p5: middleEdge)
            {

                if(p5.returnSlope()>-0.03 && p5.returnSlope()<0.03)
                {
                    //pixelData[p5.getY()][p5.getX()].setPixelValue(3000);
                    //pixelData[p5.returnLower().getY()][p5.returnLower().getX()].setPixelValue(3000);


                    if( pixelData[p5.returnUpper().getY()][p5.returnUpper().getX()].getPixelValue()>1100 &&pixelData[p5.returnUpper().getY()][p5.returnUpper().getX()].getPixelValue()<1300)
                    {
                        gh+=pixelData[p5.returnUpper().getY()][p5.returnUpper().getX()].getPixelValue();
                        gh1++;
                    }
                    gh=0;
                    gh1=0;

                    if(pixelData[p5.getY()][p5.getX()].getPixelValue()>0 &&pixelData[p5.getY()][p5.getX()].getPixelValue()<1300)
                    {
                        midgh+=pixelData[p5.getY()][p5.getX()].getPixelValue();
                        midgh1++;
                    }



                }
                /*pixelData[p5.getY()][p5.getX()].setPixelValue(3000);
                pixelData[p5.returnUpper().getY()][p5.returnUpper().getX()].setPixelValue(3000);
                pixelData[p5.returnLower().getY()][p5.returnLower().getX()].setPixelValue(3000);
                pixelData[p5.returnOutLower().getY()][p5.returnOutLower().getX()].setPixelValue(3000);
                pixelData[p5.returnOutUpper().getY()][p5.returnOutUpper().getX()].setPixelValue(3000);
                */
            }

            for(Point p5: middleEdge)
            {

                if(p5.returnSlope()>-0.03 && p5.returnSlope()<0.03)
                {
                    //pixelData[p5.getY()][p5.getX()].setPixelValue(3000);
                    //pixelData[p5.returnLower().getY()][p5.returnLower().getX()].setPixelValue(3000);




                    if(pixelData[p5.getY()][p5.getX()].getPixelValue()>0 &&pixelData[p5.getY()][p5.getX()].getPixelValue()<1300)
                    {
                        pixelData[p5.getY()][p5.getX()].setPixelValue(pixelData[p5.getY()][p5.getX()].getPixelValue()+(1048-(midgh/midgh1)));
                    }



                }
                /*pixelData[p5.getY()][p5.getX()].setPixelValue(3000);
                pixelData[p5.returnUpper().getY()][p5.returnUpper().getX()].setPixelValue(3000);
                pixelData[p5.returnLower().getY()][p5.returnLower().getX()].setPixelValue(3000);
                pixelData[p5.returnOutLower().getY()][p5.returnOutLower().getX()].setPixelValue(3000);
                pixelData[p5.returnOutUpper().getY()][p5.returnOutUpper().getX()].setPixelValue(3000);
                */
            }
            System.out.println("Streak Average"+midgh/midgh1);




            /**
             * Writing back altered image
             */
            for(int i = 0; i< pSize; i++)
            {
                for(int j = 0; j <pSize; j++ )
                {
                    Byte fb = (byte)(pixelData[i][j].getPixelValue()&0xFF);
                    Byte sb = (byte)((pixelData[i][j].getPixelValue()>>8)&0xFF);
                    bytes[positionToWrite]=fb;
                    bytes[positionToWrite+1]=sb;
                    positionToWrite+=2;

                }

            }
            System.out.println(bytes[300]+"  "+bytes[300+1]);
            for (int i = 0; i < count; i++) {
                //write to new  image
                out.writeByte(bytes[i]);
            }

            // close stuff
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         * end
         */
    }


    private static int[] profiles(Point p){
        int [] profiles = new int[20];
        Point up, low;
        int upY = p.;
        up = new Point(p.returnUpper().getY(), p.returnUpper().getY());
        low = new Point(p.returnLower().getY(), p.returnLower().getY());
        while ( up != low){

        }

        return profiles;
    }
    //print queue
    private static void queuePrint(List<Point> streak){
        System.out.println("Here");
        for(Point s : streak){

            System.out.println(s.returnSlope()+" y: "+s.getY()+" x: "+s.getX());
        }
    }
    private static boolean checkRange(Pixel p) {
        //soft tissue(760-860)
        return (p.getPixelValue() >= 760 && p.getPixelValue() <= 900) || p.getPixelValue() > 4000 || p.getPixelValue() == 0;
    }
   /* private static void fixPixel(int i, int k,Pixel[][] pixelData)
    {
        int c1=i;
        if(pixelData[p.getY()+1][p.getX()].getPixelValue()!=0) {
            while (pixelData[p1.getY() - 1][p1.getX()].getPixelValue() == 0) {
                p1 = new Point(c1--, j);
            }
            pixelData[p.getY()][p.getX()].setPixelValue(-10);
            pixelData[p1.getY()][p1.getX()].setPixelValue(-10);
            p = p.midpoint(p1);

                pixelData[p.getY()][p.getX()].setPixelValue(-1);
                float t = p.getSlope(medians[1][0], medians[1][1]);
                if(p.getX()==109)
                {
                    System.out.println(t);
                }
                zerosM1.add(p);

                t = p.getSlope(medians[1][0], medians[1][1]);

                zerosM2.add(p);
            }
    }*/
}
