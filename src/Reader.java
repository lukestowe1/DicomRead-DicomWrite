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

            }


           /*for(int i = 0; i< 300; i++)
            {
                for(int j = 0; j <pSize; j++ )
                {
                    System.out.printf("%5d ", pixelData[i][j].getPixelValue());
                }
                System.out.println();
            }*/

            System.out.println(zerosM2.size());





            List<Slice> middleEdge = new LinkedList<Slice>();
            //queuePrint(zerosM2);//prink streak coordinates
            Point pix2;
            int lineOccuranceSize=0;

            /**
             * Streak ID and profiling
             * ----- To be moved to own method
             */
            for(Point pix: zerosM1) {

                for (int i = 0; i < pSize; i++) {

                    for (int k = 0; k < pSize; k++) {

                            pix2 = new Point(i, k);

                            if (pix.equationLine(pix2)) {
                                if (pixelData[i][k].getPixelValue() >= 500 && pixelData[i][k].getPixelValue() <= 960 && pix.returnSlope() > -1.5 && pix.returnSlope() < 1.5)//tissue fix
                                {
                                    int moverC = i;
                                    Point mover = new Point(i, k);
                                    mover.getSlope(pix.getY(), pix.getX());
                                    mover.returnPerpendicular();
                                    while (pixelData[mover.getY()][mover.getX()].getPixelValue() >= 500 && pixelData[mover.getY()][mover.getX()].getPixelValue() <= 990) {
                                        moverC--;
                                        float sl = mover.returnSlope();
                                        if (sl > -0.05 && sl < 0.05) {
                                            mover = new Point(moverC, mover.getX());
                                        } else {
                                            int x = mover.getPerpY(moverC);//bounds checking
                                            if (x > 519) {
                                                x = 519;
                                            }
                                            mover = new Point(moverC, x);

                                        }


                                    }
                                    float s1 = mover.returnSlope();
                                    int x;
                                    if(s1 > -0.05 && s1 < 0.05){//means infinity so x will not change
                                        x = mover.getX();
                                    }
                                    else {
                                        x = mover.getPerpY(mover.getY() - 10);
                                        if (x < 1) {
                                            x = 1;
                                        }
                                    }
                                    Point outUpper;
                                    if(mover.getY() - 10 < 1) {
                                        outUpper = new Point(1, x);//bounds checking
                                    }
                                    else{
                                        outUpper = new Point(mover.getY()-10,x);
                                    }
                                    Point upper = new Point(mover.getY(), mover.getX());
                                    moverC++;
                                    mover = new Point(i, k);
                                    while (pixelData[mover.getY()][mover.getX()].getPixelValue() >= 500 && pixelData[mover.getY()][mover.getX()].getPixelValue() <= 990) {
                                        moverC++;
                                        float sl = mover.returnSlope();
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
                                    Point temp = lower.midpoint(upper);
                                    Slice mid = new Slice(temp.getY(),temp.getX());
                                    s1 = mover.returnSlope();
                                    if(s1 > -0.05 && s1 < 0.05){
                                        x = mover.getX();
                                    }
                                    else {
                                        x = mover.getPerpY(mover.getY() + 10);
                                        if (x > 519) {//bounds
                                            x = 519;
                                        }
                                    }
                                    Point outLower;
                                    if(mover.getY()+10 > 519) {
                                        outLower = new Point(519, x);//bounds
                                    }
                                    else{
                                        outLower = new Point(mover.getY() + 10, x);
                                    }
                                    mid.getSlope(medians[1][0], medians[1][1]);
                                    mid.setLimit(upper,lower,outUpper,outLower);//link mid with its limit points
                                    int upperPix = pixelData[upper.getX()][upper.getY()].getPixelValue();
                                    int midPix = pixelData[mid.getX()][mid.getY()].getPixelValue();
                                    int lowerPix = pixelData[lower.getX()][lower.getY()].getPixelValue();
                                    int outerLowerPix = pixelData[outLower.getX()][outLower.getY()].getPixelValue();
                                    int outerUpperPix = pixelData[outUpper.getX()][outUpper.getY()].getPixelValue();
                                    mid.setAvg(midPix,upperPix,lowerPix,outerUpperPix,outerLowerPix);
                                    if(upper.distance(lower) > 4 && !middleEdge.contains(mid)) {

                                        //avoid random dots streak must have width

                                        if (containsSlope(middleEdge, mid.returnSlope())) {
                                            lineOccuranceSize++;
                                        }


                                        middleEdge.add(mid);
                                    }
                                }
                            }
                        }
                }
            }

            /**
             * End
             */
                System.out.println("------- line size"+ lineOccuranceSize + "----originals : "+middleEdge.size());
            //alter Pixels to illustrate streaks
            /*int gh=0;
            int gh1=0;

            int midgh=0;
            int midgh1=0;
            for(Slice p5: middleEdge)
            {

                if(p5.returnSlope()>-0.03 && p5.returnSlope()<0.03)
                {

                    if( pixelData[p5.getUpY()][p5.getUpX()].getPixelValue()>1100 &&pixelData[p5.getUpY()][p5.getUpX()].getPixelValue()<1300)
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
            }

            for(Point p5: middleEdge)//test set data to average
            {

                if(p5.returnSlope()>-0.03 && p5.returnSlope()<0.03)
                {

                    if(pixelData[p5.getY()][p5.getX()].getPixelValue()>0 &&pixelData[p5.getY()][p5.getX()].getPixelValue()<1300)
                    {
                        pixelData[p5.getY()][p5.getX()].setPixelValue(pixelData[p5.getY()][p5.getX()].getPixelValue()+(1048-(midgh/midgh1)));
                    }



                }

            }
            System.out.println("Streak Average"+midgh/midgh1);*/


            double[][] lineProperties = new double[lineOccuranceSize][5];//

            int countFill=1;
            for(Slice sl : middleEdge) {

                    int flag2 = 0;
                    for (int i = 0; i < countFill; i++) {
                        if (lineProperties[i][0] == sl.returnSlope()) {
                            if(sl.returnBorderAvg()<1300)
                                lineProperties[i][1] += sl.returnBorderAvg();
                            if(sl.returnMidPix()<1300)
                                lineProperties[i][2] += sl.returnMidPix();
                            if(sl.returnOutAvg()<1300)
                                lineProperties[i][3] += sl.returnOutAvg();
                            lineProperties[i][4]++;
                            flag2 = 1;
                            break;
                        }


                    }
                    if (flag2 != 1) {

                        lineProperties[countFill - 1][0] = (double) sl.returnSlope();
                        if (sl.returnBorderAvg()<1300)
                            lineProperties[countFill - 1][1] = sl.returnBorderAvg();//mught have to split
                        if (sl.returnBorderAvg()<1300)
                            lineProperties[countFill - 1][2] = sl.returnMidPix();
                        if (sl.returnBorderAvg()<1300)
                            lineProperties[countFill - 1][3] = sl.returnOutAvg();
                        lineProperties[countFill - 1][4]++;
                        countFill++;
                    }
            }

            for(int i = 0 ; i < lineOccuranceSize;i++)
            {
                System.out.println("slope "+lineProperties[i][0]+" number of slices at this slope: "+lineProperties[i][4]);
            }

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


    private static int[] profiles(Slice p){
        int [] profiles = new int[20];
        Slice up, low;
        int upY = p.getUpY();
        up = new Slice(p.getUpY(), p.getUpX());
        low = new Slice(p.getLowY(), p.getLowX());
        while ( up != low){

        }

        return profiles;
    }
    //print queue

    private static boolean containsSlope(List<Slice> t , float fl)
    {
        for(Slice t1: t)
        {
            if(t1.returnSlope()==fl)
                return false;
        }
        return true;
    }
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
}
