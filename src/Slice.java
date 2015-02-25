/**
 * Created by Ashley on 25/02/2015.
 */
public class Slice extends Point {


    private Point upper;
    private Point outUpper;
    private Point lower;
    private Point outLower;

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
