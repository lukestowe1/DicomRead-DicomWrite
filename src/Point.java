/**
 * Created by lukestowe on 28/01/2015.
 */
public class Point {
    private int y;
    private int x;
    private float slope;
    Point(int x1, int x2)
    {
        y=x1;
        x=x2;
    }
    int getX()
    {
        return x;
    }
    int getY()
    {
        return y;
    }
    float returnSlope()
    {
        return slope;
    }
    float returnPerpendicular(){
        return 0-(1/slope);
    }
    float getSlope(int y1, int x1){
        double x = x1 - this.x;
        double y = y1 - this.y;

        if( x == 0)
        {
            slope = 500f;
            return slope;
        }

        else
        {
            slope=(float)(y/x)+0f;
            return slope;
        }

    }
    Point midpoint(Point p1)
    {
        int x = (this.x+p1.x)/2;
        int y = (this.y+p1.y)/2;
        return new Point(y,x);
    }
    @Override
    public boolean equals(Object p1){
        Point p = (Point)p1;
        return this.slope == p.slope;
    }
    public boolean equationLine(Point p, float slope)
    {
        int m =(this.y-p.y)-(this.x-p.x);
        return m==this.slope;
    }



}
