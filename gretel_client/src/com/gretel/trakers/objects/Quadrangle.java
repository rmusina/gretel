package com.gretel.trakers.objects;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.os.Parcel;
import android.os.Parcelable;

public class Quadrangle implements Parcelable, TrackableObject {

	/*
	 * p4      p1
	 * 
	 * p3      p2
	 */
		
	private Point point1;

	private Point point2;
	
	private Point point3;
	
	private Point point4;
	
	public Quadrangle() {
		this.point1 = new Point();
		this.point2 = new Point();
		this.point3 = new Point();
		this.point4 = new Point();
	}
	
	public static final Parcelable.Creator<Quadrangle> CREATOR = new Parcelable.Creator<Quadrangle>() {
		public Quadrangle createFromParcel(Parcel in) {
		    return new Quadrangle(in);
		}
		
		public Quadrangle[] newArray(int size) {
		    return new Quadrangle[size];
		}
	};
	
	public Quadrangle(Parcel in) {
		this.point1 = new Point(in.readDouble(), in.readDouble());
		this.point2 = new Point(in.readDouble(), in.readDouble());
		this.point3 = new Point(in.readDouble(), in.readDouble());
		this.point4 = new Point(in.readDouble(), in.readDouble());
	}
	
	public Quadrangle(Point point1, Point point3) {
		this.point1 = point1;
		this.point3 = point3;
		
		this.point2 = new Point(point3.x, point1.y);
		this.point4 = new Point(point1.x, point3.y);
	}
	
	public Quadrangle(Point point1, Point point2, Point point3, Point point4) {
		this.point1 = point1;
		this.point2 = point2;
		this.point3 = point3;
		this.point4 = point4;
	}
	
	public Quadrangle(Mat rectangleCorners) {
		this.point1 = new Point((int)rectangleCorners.get(0, 0)[0], (int)rectangleCorners.get(0, 0)[1]);
		this.point2 = new Point((int)rectangleCorners.get(1, 0)[0], (int)rectangleCorners.get(1, 0)[1]);
		this.point3 = new Point((int)rectangleCorners.get(2, 0)[0], (int)rectangleCorners.get(2, 0)[1]);
		this.point4 = new Point((int)rectangleCorners.get(3, 0)[0], (int)rectangleCorners.get(3, 0)[1]);
	}
	
	public Point getPoint1() {
		return point1;
	}

	public void setPoint1(Point point1) {
		this.point1 = point1;
	}

	public Point getPoint2() {
		return point2;
	}

	public void setPoint2(Point point2) {
		this.point2 = point2;
	}

	public Point getPoint3() {
		return point3;
	}

	public void setPoint3(Point point3) {
		this.point3 = point3;
	}

	public Point getPoint4() {
		return point4;
	}

	public void setPoint4(Point point4) {
		this.point4 = point4;
	}

	public Mat asPerspectiveTransformMatrix() {
		Mat rectangleCorners = new Mat(4, 1, CvType.CV_32FC2);
		
		rectangleCorners.put(0, 0, new double[] { this.point1.x, this.point1.y });
		rectangleCorners.put(1, 0, new double[] { this.point2.x, this.point2.y });
		rectangleCorners.put(2, 0, new double[] { this.point3.x, this.point3.y });
		rectangleCorners.put(3, 0, new double[] { this.point4.x, this.point4.y });
		
		return rectangleCorners;
	}

	public List<MatOfPoint> asPolyLineList() {
		List<MatOfPoint> polyLinePoints = new ArrayList<MatOfPoint>(4);
		MatOfPoint points = new MatOfPoint();
		
		points.fromArray(this.point1, this.point2, this.point3, this.point4);
		polyLinePoints.add(points);
		
		return polyLinePoints;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeDouble(point1.x); out.writeDouble(point1.y);
		out.writeDouble(point2.x); out.writeDouble(point2.y);
		out.writeDouble(point3.x); out.writeDouble(point3.y);
		out.writeDouble(point4.x); out.writeDouble(point4.y);
	}

	@Override
	public Mat getTrakingMask(Mat image) {
		int rows = image.rows();
		int cols = image.cols();
		
		int xMin = (int)this.point1.x; int yMin = (int)this.point1.y;
		int xMax = (int)this.point3.x; int yMax = (int)this.point3.y;
		
		Mat mask = new Mat(rows, cols, CvType.CV_8UC1);
		
		for (int x = xMin; x <= xMax; x++) {
			for (int y = yMin; y <= yMax; y++) {
				mask.put(y, x, 1.0);
			}
		}
		
		return mask;
	}

	@Override
	public TrackableObject perspectiveTransform(Mat transformMatrix) {
		Mat frameQuad = new Mat(4, 1, CvType.CV_32FC2);
		Core.perspectiveTransform(this.asPerspectiveTransformMatrix(), frameQuad, transformMatrix);		
		Quadrangle projectedQuad = new Quadrangle(frameQuad);
		frameQuad.release();
		
		return projectedQuad;
	}

	@Override
	public boolean isShapeValid() {
		/*if (this.point1 == this.point2 || 
			this.point1 == this.point3 || 
			this.point1 == this.point4) {
			return false;
		}
		
		if (this.point2 == this.point3 || 
			this.point2 == this.point4) {
			return false;
		}
		
		if (this.point3 == this.point4) {
			return false;
		}
		
		if (this.point1.x > this.point2.x || 
			this.point1.x > this.point3.x ||
			this.point4.x > this.point2.x || 
			this.point4.x > this.point3.x) {
			return false;
		}
		
		if (this.point1.y > this.point4.y || 
			this.point2.y > this.point3.y) {
			return false;
		}*/
		
		return true;
	}

	@Override
	public void draw(Mat imageFrame, Scalar color) {
		Core.polylines(imageFrame, this.asPolyLineList(), true, color, 3);
	}
}
