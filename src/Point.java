/**
 * Created by lukestowe on 28/01/2015.
 */
public class Point {
    private int y;
    private int x;
    private float slope;
    private float perpSlope;

    Point(int y1, int x1) {
        y = y1;
        x = x1;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    float returnSlope() {
        return slope;
    }

    float returnPerpendicular() {// get and send perpedicular slope
        float f = 1;

        perpSlope = 0 - (f / slope);
        //perpSlope = Math.round(perpSlope*100.0f)/100.0f;
        if (slope == 500) {
            perpSlope = 0;
        }

        return perpSlope;
    }

    float getSlope(int y1, int x1) {//calculate slope
        double x = x1 - this.x;
        double y = y1 - this.y;

        if (x == 0) {
            slope = 500f;

            return slope;
        } else {
            slope = (float) (y / x) + 0f;

            return slope;
        }

    }

    Point midpoint(Point p1) { // calculate and return a midpoint Point
        int x = (this.x + p1.x) / 2;
        int y = (this.y + p1.y) / 2;
        return new Point(y, x);
    }

    public boolean equalsSlopes(Object p1) {
        Point p = (Point) p1;
        return this.slope == p.slope;
    }

    @Override
    public boolean equals(Object p1) {//override equals for contains method for lists to avoid duplicates
        Point p = (Point) p1;
        return (this.x == p.x && this.y == p.y);
    }

    public boolean equationLine(Point p) {//equation of line
        double x = this.x - p.x;
        double y = this.y - p.y;
        float m = (float) (y / x) + 0f;

        return m <= this.slope + 0.03f && m >= this.slope - 0.03f;//0.03
    }

    public boolean equationLine2(Point p) {//Used for debugging
        double x = this.x - p.x;
        double y = this.y - p.y;
        float m = (float) (y / x) + 0f;

        //System.out.println(m+" "+this.slope);
        return m == this.slope;

    }

    public int getPerpY(int y1) {//calculate new y coordinate when moving perpdenicular
        double y = y1 - this.y;
        return (int) (this.x + (y / this.perpSlope));
    }

    public int distance(Point p) {
        return (int) Math.sqrt(((this.x - p.x) * (this.x - p.x)) + ((this.y - p.y) * (this.y - p.y)));
    }

}
