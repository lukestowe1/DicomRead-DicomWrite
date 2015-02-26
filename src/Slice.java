import java.math.BigDecimal;

/**
 * Created by Ashley on 25/02/2015.
 */
public class Slice extends Point {


    private Point upper;
    private Point outUpper;
    private Point lower;
    private Point outLower;
    private int midPix=0;
    private int upperPix=0;
    private int lowerPix=0;
    private int borderAvg=0;
    private int outerAvg=0;
    private int outerUpperPix=0;
    private int outerLowerPix=0;
    private int borderLesAvg = 0;

    Slice(int y1, int x1) {
        super(y1, x1);
    }

    Point returnUpper()
    {
        return upper;
    }
    int getUpY(){
        return upper.getY();
    }
    int getUpX(){
        return upper.getX();
    }
    Point returnLower() { return lower; }
    int getLowY(){
        return lower.getY();
    }
    int getLowX(){
        return lower.getX();
    }


    Point returnOutUpper()
    {
        return outUpper;
    }
    void setAvg(int mid,int upper, int lower , int outerUpper , int outerLower)
    {
        midPix=mid;
        upperPix=upper;
        lowerPix=lower;
        borderAvg=(upper+lower)/2;
        outerUpperPix=outerUpper;
        outerLowerPix=outerLower;
        outerAvg=(outerUpper+outerLower)/2;

    }

    float returnSlope1()
    {
        BigDecimal bd = new BigDecimal(Float.toString(this.returnSlope()));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
        //return (float)Math.round(this.returnSlope()*100.0f)/100.0f;
    }


    int returnBorderAvg()
    {
        return borderAvg;
    }
    int returnMidPix()
    {
        return midPix;
    }
    int returnOutAvg()
    {
        return outerAvg;
    }




    int getOutUpY(){
        return outUpper.getY();
    }
    int getOutUpX(){
        return outUpper.getX();
    }
    Point returnOutLower()
    {
        return outLower;
    }
    int getOutLowY(){
        return outLower.getY();
    }
    int getOutLowX(){
        return outLower.getX();
    }
    void setLimit(Point upper, Point lower, Point outUpper, Point outLower)
    {
        this.upper=upper;
        this.lower=lower;
        this.outLower=outLower;
        this.outUpper=outUpper;
    }

}
