package com.gretel.trakers.objects;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class TrackableImage implements TrackableObject {

    private Quadrangle boundingQuad;

    private Mat image;

    public TrackableImage(Quadrangle boundingQuad) {
        this.boundingQuad = boundingQuad;
    }

    public TrackableImage(Mat image) {
        this.image = image;
        this.boundingQuad = new Quadrangle(
                                new Point(0, 0),
                                new Point(image.height(), image.width()));
    }

    public TrackableImage(Mat image, Quadrangle boundingQuad) {
        this.image = image;
        this.boundingQuad = boundingQuad;
    }

    public Quadrangle getBoundingQuad() {
        return boundingQuad;
    }

    public void setBoundingQuad(Quadrangle boundingQuad) {
        this.boundingQuad = boundingQuad;
    }

    public Mat getImage() {
        return image;
    }

    public void setImage(Mat image) {
        this.image = image;
    }

    @Override
    public void draw(Mat imageFrame, Scalar color) {
        if (image == null) {
            Core.polylines(imageFrame, this.boundingQuad.asPolyLineList(), true, color, 3);
        } else {
            Quadrangle defaultQuad = new Quadrangle(new Point(0, 0), this.image.size());
            Mat transMatrix = Imgproc.getPerspectiveTransform(
                                            defaultQuad.asPerspectiveTransformMatrix(),
                                            boundingQuad.asPerspectiveTransformMatrix());

            Mat mask = Mat.zeros(imageFrame.size(), imageFrame.type());
            Rect roi = new Rect(defaultQuad.getPoint1(), this.image.size());
            image.copyTo(mask.submat(roi));
            Imgproc.warpPerspective(mask, mask, transMatrix, mask.size(), Imgproc.CV_WARP_FILL_OUTLIERS);
            Core.add(imageFrame, mask, imageFrame);
        }
    }

    @Override
    public boolean isShapeValid() {
        return boundingQuad.isShapeValid();
    }

    @Override
    public Mat getTrackingMask(Mat image) {
        return boundingQuad.getTrackingMask(image);
    }

    @Override
    public TrackableObject perspectiveTransform(Mat transMatrix) {
        Quadrangle transQuad = (Quadrangle)this.boundingQuad.perspectiveTransform(transMatrix);
        return new TrackableImage(this.image, transQuad);
    }
}
