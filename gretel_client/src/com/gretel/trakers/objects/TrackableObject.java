package com.gretel.trakers.objects;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

public interface TrackableObject {
	void draw(Mat imageFrame, Scalar color);
	boolean isShapeValid();
	Mat getTrackingMask(Mat image);
	TrackableObject perspectiveTransform(Mat transformMatrix);
}
