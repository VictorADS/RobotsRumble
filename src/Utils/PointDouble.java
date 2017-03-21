package Utils;

import java.awt.Point;

public class PointDouble  {
	public double x;
	public double y;
	
	public PointDouble(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	public void setLocation(double d, double e) {
		this.x = d;
		this.y = e;
	}
	public double distance(Point e){
		return Math.hypot(x - e.x, y - e.y);
	}
	
}
