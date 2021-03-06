/*
Author Luke Stowe
Author Ashley Deane
 */


//import java.awt.*;

import com.pixelmed.display.SingleImagePanel;
import com.pixelmed.display.SourceImage;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;


class Reader {
    public static void main(String[] argv) throws Exception {

        String inputString = promptForFile();
        //Read from an input stream
        InputStream is = new BufferedInputStream(new FileInputStream(inputString));//input image
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
        String outputString = getOutputFile(inputString);//get input file directory
        BufferedOutputStream o = new BufferedOutputStream(new FileOutputStream(outputString + "Output.dcm"));
        DataOutputStream out = new DataOutputStream(o);
        int count = 0;
        try {
            while (in.available() > 0)// end of file + count size
            {
                bytes[count] = in.readByte();//read in bytes
                count++;
            }
            if (Collections.indexOfSubList(Arrays.asList(bytes), Arrays.asList(endOfhead2)) + endOfhead2.length > 12) {//check which type of dicom file
                pixelStart = Collections.indexOfSubList(Arrays.asList(bytes), Arrays.asList(endOfhead2)) + endOfhead2.length;
                pSize = 520;
            } else {
                pixelStart = Collections.indexOfSubList(Arrays.asList(bytes), Arrays.asList(endOfhead1)) + endOfhead1.length;
                pSize = 512;
            }

            Pixel[][] pixelData = new Pixel[pSize][pSize];//take in pixel objects


            positionToWrite = pixelStart;  //position to write back to


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


            /**
             * Getting Zeros, with slopes to points
             * This will lead to finding the streaks
             * As Zeros are air effected by the streak
             */
            //Zeros with slope from first metal Object
            List<Point> zerosM1 = new LinkedList<Point>();

            //Zeros with slope from Second metal Object
            List<Point> zerosM2 = new LinkedList<Point>();

            Point p;
            //Adding all Zeros to both arrays
            for (int i = 1; i < 519; i++) {
                for (int j = 1; j < pSize; j++) {
                    p = new Point(i, j);
                    if (pixelData[i][j].getPixelValue() == 0) {//store 0 values
                        Point p1 = new Point(i, j);
                        int c1 = i;
                        if (pixelData[p.getY() + 1][p.getX()].getPixelValue() != 0) {
                            while (pixelData[p1.getY() - 1][p1.getX()].getPixelValue() == 0 && c1 > 0) {//avoid running off image out of bounds fix
                                p1 = new Point(c1--, j);
                            }
                            p = p.midpoint(p1);

                            p.getSlope(medians[1][0], medians[1][1]);//left

                            zerosM1.add(p);

                            p.getSlope(medians[0][0], medians[0][1]);//right
                            zerosM2.add(p);
                        }

                    }

                }
            }


            //left metal

            Point metal1 = new Point(medians[1][0], medians[1][1]);

            //Check which zero tends to which streak
            for (int i = 0; i < zerosM1.size(); i++) {

                Point p1 = zerosM1.get(i);
                Point mid = p1.midpoint(metal1);

                if (!checkRange(pixelData[mid.getY()][mid.getX()])) {
                    zerosM1.remove(p1);
                }


            }

            Point metal2 = new Point(medians[0][0], medians[0][1]);

            //Check which zero tends to which streak
            for (int i = 0; i < zerosM2.size(); i++) {

                Point p1 = zerosM2.get(i);
                Point mid = p1.midpoint(metal2);

                if (!checkRange(pixelData[mid.getY()][mid.getX()])) {
                    zerosM2.remove(p1);
                }


            }


            List<Slice> middleEdge = new LinkedList<Slice>();

            Point pix2;
            int lineOccuranceSize = 0;

            /**
             * Streak ID and profiling
             * -----
             */
            int max = 0;
            for (Point pix : zerosM1) {

                for (int i = 0; i < pSize; i++) {

                    for (int k = 0; k < pSize; k++) {

                        pix2 = new Point(i, k);
                        //check to see if pixel is on same line as zero and check if it is an offset value
                        if (pix.equationLine(pix2)) {
                            if (pixelData[i][k].getPixelValue() >= 500 && pixelData[i][k].getPixelValue() <= 960 && pix.returnSlope() > -1.5 && pix.returnSlope() < 1.5)//tissue fix
                            {
                                int moverC = i;
                                Point mover = new Point(i, k);
                                mover.getSlope(pix.getY(), pix.getX());
                                mover.returnPerpendicular();
                                //mover perpendicular upwards in the array to find boundary
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
                                if (s1 > -0.05 && s1 < 0.05) {//means infinity so x will not change
                                    x = mover.getX();
                                } else {
                                    x = mover.getPerpY(mover.getY() - 20);
                                    if (x < 1) {
                                        x = 1;
                                    }
                                }
                                Point outUpper;
                                if (mover.getY() - 20 < 1) {
                                    outUpper = new Point(1, x);//bounds checking
                                } else {
                                    outUpper = new Point(mover.getY() - 20, x);
                                }
                                Point upper = new Point(mover.getY(), mover.getX());
                                moverC++;
                                mover = new Point(i, k);
                                //mover perpendicular downwards to find lower boundary
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
                                //create a slice which is made up of midpoint of boundaries and boundary points
                                Slice mid = new Slice(temp.getY(), temp.getX());
                                s1 = mover.returnSlope();
                                if (s1 > -0.05 && s1 < 0.05) {
                                    x = mover.getX();
                                } else {
                                    x = mover.getPerpY(mover.getY() + 20);
                                    if (x > 519) {//bounds
                                        x = 519;
                                    }
                                }
                                Point outLower;
                                if (mover.getY() + 20 > 519) {
                                    outLower = new Point(519, x);//bounds
                                } else {
                                    outLower = new Point(mover.getY() + 20, x);
                                }
                                mid.getSlope(medians[1][0], medians[1][1]);
                                //set the points in the Slice
                                mid.setLimit(upper, lower, outUpper, outLower);//link mid with its limit points
                                int upperPix = pixelData[upper.getX()][upper.getY()].getPixelValue();
                                int midPix = pixelData[mid.getY()][mid.getX()].getPixelValue();
                                int lowerPix = pixelData[lower.getY()][lower.getX()].getPixelValue();
                                int outerLowerPix = pixelData[outLower.getY()][outLower.getX()].getPixelValue();
                                int outerUpperPix = pixelData[outUpper.getY()][outUpper.getX()].getPixelValue();
                                mid.setAvg(midPix, upperPix, lowerPix, outerUpperPix, outerLowerPix);
                                if (upper.distance(lower) > 4 && !middleEdge.contains(mid)) {

                                    //avoid random dots streak must have width

                                    if (containsSlope(middleEdge, mid.returnSlope())) {
                                        lineOccuranceSize++;
                                    }

                                    //store all these slices
                                    middleEdge.add(mid);
                                }
                                moverC = upper.getY();
                                mover = new Point(upper.getY(), upper.getX());

                                int testC = 0;
                                //push each boundary out 5 to make sure the white streaks are fixed
                                while (testC < 5) {
                                    moverC--;

                                    float sl = mover.returnSlope();
                                    if (sl > -0.05 && sl < 0.05) {
                                        mover = new Point(moverC, mover.getX());
                                    } else {
                                        x = mover.getPerpY(moverC);//bounds checking

                                        if (x < 0)
                                            x = 1;
                                        if (x > 519) {
                                            x = 519;
                                        }
                                        mover = new Point(moverC, x);

                                    }

                                    testC++;


                                }
                                upper = mover;
                                moverC = lower.getY();
                                mover = new Point(lower.getY(), lower.getX());

                                testC = 0;
                                //push each boundary out 5 to make sure the white streaks are fixed
                                while (testC < 5) {
                                    moverC++;
                                    float sl = mover.returnSlope();
                                    if (sl > -0.05 && sl < 0.05) {
                                        mover = new Point(moverC, mover.getX());
                                    } else {
                                        x = mover.getPerpY(moverC);
                                        if (x > 519) {
                                            x = 519;
                                        }
                                        if (x < 1) {
                                            x = 1;
                                        }
                                        mover = new Point(moverC, x);


                                    }

                                    testC++;
                                }
                                lower = mover;
                                mid.setLimit(upper, lower, outUpper, outLower);//link mid with its limit points
                                upperPix = pixelData[upper.getX()][upper.getY()].getPixelValue();
                                midPix = pixelData[mid.getY()][mid.getX()].getPixelValue();
                                lowerPix = pixelData[lower.getY()][lower.getX()].getPixelValue();
                                outerLowerPix = pixelData[outLower.getY()][outLower.getX()].getPixelValue();
                                outerUpperPix = pixelData[outUpper.getY()][outUpper.getX()].getPixelValue();
                                mid.setAvg(midPix, upperPix, lowerPix, outerUpperPix, outerLowerPix);


                                if (upper.distance(lower) > max)
                                    max = upper.distance(lower);
                            }
                        }
                    }
                }
            }
            max = 0;
            for (Point pix : zerosM2) {

                for (int i = 0; i < pSize; i++) {

                    for (int k = 0; k < pSize; k++) {

                        pix2 = new Point(i, k);
                        //check to see if pixel is on same line as zero and check if it is an offset value
                        if (pix.equationLine(pix2)) {
                            if (pixelData[i][k].getPixelValue() >= 500 && pixelData[i][k].getPixelValue() <= 960 && pix.returnSlope() > -1.5 && pix.returnSlope() < 1.5)//tissue fix
                            {
                                int moverC = i;
                                Point mover = new Point(i, k);
                                mover.getSlope(pix.getY(), pix.getX());
                                mover.returnPerpendicular();
                                //mover perpendicular upwards in the array to find boundary
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
                                if (s1 > -0.05 && s1 < 0.05) {//means infinity so x will not change
                                    x = mover.getX();
                                } else {
                                    x = mover.getPerpY(mover.getY() - 20);
                                    if (x < 1) {
                                        x = 1;
                                    }
                                }
                                Point outUpper;
                                if (mover.getY() - 20 < 1) {
                                    outUpper = new Point(1, x);//bounds checking
                                } else {
                                    outUpper = new Point(mover.getY() - 20, x);
                                }
                                Point upper = new Point(mover.getY(), mover.getX());
                                moverC++;
                                mover = new Point(i, k);
                                //mover perpendicular downwards to find lower boundary
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
                                //create a slice which is made up of midpoint of boundaries and boundary points
                                Slice mid = new Slice(temp.getY(), temp.getX());
                                s1 = mover.returnSlope();
                                if (s1 > -0.05 && s1 < 0.05) {
                                    x = mover.getX();
                                } else {
                                    x = mover.getPerpY(mover.getY() + 20);
                                    if (x > 519) {//bounds
                                        x = 519;
                                    }
                                }
                                Point outLower;
                                if (mover.getY() + 20 > 519) {
                                    outLower = new Point(519, x);//bounds
                                } else {
                                    outLower = new Point(mover.getY() + 20, x);
                                }
                                mid.getSlope(medians[1][0], medians[1][1]);
                                //set the points in the Slice
                                mid.setLimit(upper, lower, outUpper, outLower);//link mid with its limit points
                                int upperPix = pixelData[upper.getX()][upper.getY()].getPixelValue();
                                int midPix = pixelData[mid.getY()][mid.getX()].getPixelValue();
                                int lowerPix = pixelData[lower.getY()][lower.getX()].getPixelValue();
                                int outerLowerPix = pixelData[outLower.getY()][outLower.getX()].getPixelValue();
                                int outerUpperPix = pixelData[outUpper.getY()][outUpper.getX()].getPixelValue();
                                mid.setAvg(midPix, upperPix, lowerPix, outerUpperPix, outerLowerPix);
                                if (upper.distance(lower) > 4 && !middleEdge.contains(mid)) {

                                    //avoid random dots streak must have width

                                    if (containsSlope(middleEdge, mid.returnSlope())) {
                                        lineOccuranceSize++;
                                    }

                                    //store all these slices
                                    middleEdge.add(mid);
                                }
                                moverC = upper.getY();
                                mover = new Point(upper.getY(), upper.getX());

                                int testC = 0;
                                //push each boundary out 5 to make sure the white streaks are fixed
                                while (testC < 5) {
                                    moverC--;

                                    float sl = mover.returnSlope();
                                    if (sl > -0.05 && sl < 0.05) {
                                        mover = new Point(moverC, mover.getX());
                                    } else {
                                        x = mover.getPerpY(moverC);//bounds checking

                                        if (x < 0)
                                            x = 1;
                                        if (x > 519) {
                                            x = 519;
                                        }
                                        mover = new Point(moverC, x);

                                    }

                                    testC++;


                                }
                                upper = mover;
                                moverC = lower.getY();
                                mover = new Point(lower.getY(), lower.getX());

                                testC = 0;
                                //push each boundary out 5 to make sure the white streaks are fixed
                                while (testC < 5) {
                                    moverC++;
                                    float sl = mover.returnSlope();
                                    if (sl > -0.05 && sl < 0.05) {
                                        mover = new Point(moverC, mover.getX());
                                    } else {
                                        x = mover.getPerpY(moverC);
                                        if (x > 519) {
                                            x = 519;
                                        }
                                        if (x < 1) {
                                            x = 1;
                                        }
                                        mover = new Point(moverC, x);


                                    }

                                    testC++;
                                }
                                lower = mover;
                                mid.setLimit(upper, lower, outUpper, outLower);//link mid with its limit points
                                upperPix = pixelData[upper.getX()][upper.getY()].getPixelValue();
                                midPix = pixelData[mid.getY()][mid.getX()].getPixelValue();
                                lowerPix = pixelData[lower.getY()][lower.getX()].getPixelValue();
                                outerLowerPix = pixelData[outLower.getY()][outLower.getX()].getPixelValue();
                                outerUpperPix = pixelData[outUpper.getY()][outUpper.getX()].getPixelValue();
                                mid.setAvg(midPix, upperPix, lowerPix, outerUpperPix, outerLowerPix);


                                if (upper.distance(lower) > max)
                                    max = upper.distance(lower);
                            }
                        }
                    }
                }
            }


            /**
             * End
             */

            //set up slope key data structure for correction
            float[][] lineProperties = new float[lineOccuranceSize][(max * 2) + 3];

            int countFill = 1;
            //fill the array with all the key slope values
            for (Slice sl : middleEdge) {

                int flag2 = 0;
                for (int i = 0; i < countFill; i++) {
                    if (lineProperties[i][0] == sl.returnSlope1()) {
                        flag2 = 1;
                        break;
                    }
                }
                if (flag2 != 1) {

                    lineProperties[countFill - 1][0] = sl.returnSlope1();
                    countFill++;
                }
            }
            System.out.println("count fill" + countFill);

            /**
             * Storing of streak profiles
             */
            for (Slice sl : middleEdge) {

                int counter = 1;
                for (int i = 0; i < countFill; i++) {
                    if (lineProperties[i][0] == sl.returnSlope1()) {//check if at the key value in the array and add data here
                        Slice temp = sl;

                        if (sl.returnOutAvg() == 341) {//set a flag
                            break;
                        } else {
                            lineProperties[i][counter] += sl.returnOutAvg();//set first value of out tissue average
                            counter++;
                            lineProperties[i][counter]++;
                            counter++;

                            while (temp.getY() <= sl.getLowY()) {

                                if (pixelData[temp.getY()][temp.getX()].getPixelValue() < 1300 && pixelData[temp.getY()][temp.getX()].getPixelValue() > 100)
                                    lineProperties[i][counter] += pixelData[temp.getY()][temp.getX()].getPixelValue();//put in each point value heading towards boundary


                                //System.out.println("Adding " + pixelData[temp.getY()][temp.getX()].getPixelValue()+"temp"+ temp.getY()+" ----" + temp.getX()+" "+sl.returnSlope());

                                lineProperties[i][counter + 1]++;//increment the count to be used later to average values

                                float s1 = sl.returnSlope();
                                if (s1 > -0.05 && s1 < 0.05) {
                                    temp = new Slice(temp.getY() + 1, temp.getX());
                                } else {
                                    temp.returnPerpendicular();
                                    int x = temp.getPerpY(temp.getY() + 1);//bounds checking
                                    //System.out.println(x+"<---------");
                                    if (x > 519) {
                                        x = 519;
                                    }

                                    temp = new Slice(temp.getY() + 1, x);

                                }
                                counter += 2;

                            }

                        }
                    }
                }
            }
           //divide all values by respected counts
            for (int i = 0; i < countFill; i++) {

                for (int j = 1; j < max; j += 2) {
                    if (lineProperties[i][j + 1] != 0) {

                        lineProperties[i][j] = lineProperties[i][j] / lineProperties[i][j + 1];
                    }//inner avg
                    else
                        break;

                }

            }

            /**
             * Correction Process here
             */
            for (Slice cl : middleEdge) {

                int counter = 5;
                for (int i = 0; i < countFill; i++) {

                    if (cl.returnSlope1() == lineProperties[i][0]) {//check if at key value if the array


                        Point tempDown = cl;
                        Point tempUp = cl;

                        while (tempDown.getY() <= cl.getLowY() - 1) {//run through  points from mid to boundary along slice


                            float s1 = cl.returnSlope();
                            if (s1 > -0.05 && s1 < 0.05) {
                                tempDown = new Slice(tempDown.getY() + 1, tempDown.getX());
                                tempUp = new Slice(tempUp.getY() - 1, tempUp.getX());
                            } else {
                                int x = tempDown.getPerpY(tempDown.getY() + 1);//bounds checking
                                int y = tempUp.getPerpY(tempUp.getY() - 1);//bounds checking
                                if (x > 519) {
                                    x = 519;
                                }
                                if (y < 1) {
                                    y = 1;
                                }
                                tempDown = new Slice(tempDown.getY() + 1, x);
                                tempUp = new Slice(tempUp.getY() - 1, x);


                            }
                            //correct values = original+(outside tissue - avg at this step)
                            int up = pixelData[tempUp.getY()][tempUp.getX()].getPixelValue() + (int) (lineProperties[i][1] - lineProperties[i][counter]);
                            int down = pixelData[tempDown.getY()][tempDown.getX()].getPixelValue() + (int) (lineProperties[i][1] - lineProperties[i][counter]);
                            if (pixelData[tempUp.getY()][tempUp.getX()].getPixelValue() > 100 && up > 300) {

                                pixelData[tempUp.getY()][tempUp.getX()].setPixelValue(up);
                                if (down > 300)
                                    pixelData[tempDown.getY()][tempDown.getX()].setPixelValue(down);
                            }
                            counter += 2;
                        }
                        tempUp = cl;
                        //fix each middle value as step moves away from this before correction is done
                        int midPix = pixelData[tempUp.getY()][tempUp.getX()].getPixelValue() + (int) (lineProperties[i][1] - lineProperties[i][3]);
                        if (pixelData[tempUp.getY()][tempUp.getX()].getPixelValue() > 100 && midPix > 300) {

                            pixelData[tempUp.getY()][tempUp.getX()].setPixelValue(midPix);
                        }
                    }


                }
            }

            /**
             * Writing back altered image
             */
            for (int i = 0; i < pSize; i++) {
                for (int j = 0; j < pSize; j++) {

                    Byte fb = (byte) (pixelData[i][j].getPixelValue() & 0xFF);
                    Byte sb = (byte) ((pixelData[i][j].getPixelValue() >> 8) & 0xFF);
                    bytes[positionToWrite] = fb;
                    bytes[positionToWrite + 1] = sb;
                    positionToWrite += 2;

                }

            }
            System.out.println(bytes[300] + "  " + bytes[300 + 1]);
            for (int i = 0; i < count; i++) {
                //write to new  image
                out.writeByte(bytes[i]);
            }

            // close stuff and flush to update file system
            out.close();

            // GUI elements
            JFrame frame = new JFrame();
            JPanel left = new JPanel();
            JPanel right = new JPanel();
            frame.setLayout(new GridLayout(0, 2));
            JLabel label = new JLabel();
            JLabel label1 = new JLabel();
            label.setText("Original Image");
            label1.setText("Corrected Image");
            Border b = BorderFactory.createLineBorder(Color.BLACK, 1, true);
            label.setBorder(b);
            label1.setBorder(b);
            label.setBackground(Color.red);
            label.setOpaque(true);
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));


            SourceImage img = new SourceImage(outputString + "Output.dcm");//Pixel Med
            SingleImagePanel single = new SingleImagePanel(img);//Pixel Med
            label1.setBackground(Color.green);
            label1.setOpaque(true);
            left.add(label1);

            left.add(single);//PixelMed integrating with java swing


            SourceImage img2 = new SourceImage(inputString);
            SingleImagePanel single2 = new SingleImagePanel(img2);

            left.setBackground(Color.white);
            right.setBackground(Color.white);
            right.add(label);
            right.add(single2);

            frame.setTitle("Beam Hardening Solutions " + (char) 174);
            frame.add(left);
            frame.add(right);

            frame.setSize(img.getWidth() * 2, img.getHeight());
            frame.setVisible(true);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         * end
         */
    }

    //avoid duplications
    private static boolean containsSlope(List<Slice> t, float fl) {
        for (Slice t1 : t) {
            if (t1.returnSlope() == fl)
                return false;
        }
        return true;
    }
    //check for offset
    private static boolean checkRange(Pixel p) {
        //soft tissue(760-860)
        return (p.getPixelValue() >= 760 && p.getPixelValue() <= 900) || p.getPixelValue() > 4000 || p.getPixelValue() == 0;
    }
    //File chooser that is implement at start of run
    private static String promptForFile() {
        JFrame f = new JFrame();
        f.setTitle("Beam Hardening Solutions " + (char) 153);

        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Dicom Files", "dcm");
        fc.setFileFilter(filter);
        int returnVal = fc.showOpenDialog(f);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile().getAbsolutePath();
        } else {
            return null;
        }
    }
    //set directory of output file
    private static String getOutputFile(String input) {
        String output;
        int i;
        for (i = input.length() - 1; i >= 0; i--) {

            if (input.charAt(i) == '/') {
                break;
            }

        }
        output = input.substring(0, i + 1);
        System.out.println("" + output);
        return output;
    }
}
