package com.gretel.trackers;

import com.gretel.trakers.objects.Quadrangle;
import com.gretel.trakers.objects.TrackableImage;
import com.gretel.trakers.objects.TrackableObject;
import com.gretel.trakers.objects.TrackingTarget;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class WallTracker extends Tracker {

    private MatOfPoint2f getWallsConvexHull(Mat grayscaleImage) {
        Mat thresholdImg = grayscaleImage.clone();
        Imgproc.threshold(grayscaleImage, thresholdImg, 127, 255, 0);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresholdImg, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 500;
        MatOfPoint2f maxContour = new MatOfPoint2f();

        for (int i = 0; i < contours.size(); i++) {
            double currArea = Imgproc.contourArea(contours.get(i));

            if (currArea > maxArea) {
                MatOfInt hull = new MatOfInt();
                Imgproc.convexHull(contours.get(i), hull);

                if (hull.rows() < 4) {
                    continue;
                }

                Point[] arcPoints = new Point[hull.rows()];

                for (int j = 0; j < hull.rows(); j++) {
                    arcPoints[j] = contours.get(i).toList().get(hull.toList().get(j));
                }

                MatOfPoint2f contour = new MatOfPoint2f(arcPoints);
                Imgproc.approxPolyDP(contour, contour, 0.1 * Imgproc.arcLength(contour, true), true);

                hull.release();
                maxContour.release();

                maxContour = contour;
                maxArea = currArea;
            }

            contours.get(i).release();
        }

        hierarchy.release();
        thresholdImg.release();

        return maxContour;
    }

    @Override
    public TrackableObject track(Mat image, TrackingTarget trackingTarget) {
        MatOfPoint2f wallPlane = getWallsConvexHull(image);
        TrackableImage trackedImage = (TrackableImage)trackingTarget.getTrackedObject();

        if (wallPlane.rows() == 4) {
            Quadrangle quad = trackedImage.getBoundingQuad();

            Quadrangle wallQuad = new Quadrangle(wallPlane, true);
            Mat transMatrix = Imgproc.getPerspectiveTransform(
                quad.asPerspectiveTransformMatrix(),
                wallQuad.asPerspectiveTransformMatrix());

            return trackedImage.perspectiveTransform(transMatrix);
        }

        return trackedImage;
    }
}
