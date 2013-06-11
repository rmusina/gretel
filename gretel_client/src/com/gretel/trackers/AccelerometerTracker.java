package com.gretel.trackers;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class AccelerometerTracker {

	private Quadrangle referenceQuad;
	
	private float referenceAzimuthAngle;

	private float referencePitchAngle;

	private float referenceRollAngle;
	
	public AccelerometerTracker() {
	}
	
	public AccelerometerTracker(Quadrangle rectangle,
								float referenceAzimuthAngle, 
								float referencePitchAngle,
								float referenceRollAngle) {
		this.referenceQuad = rectangle;
		this.referenceAzimuthAngle = referenceAzimuthAngle;
		this.referencePitchAngle = referencePitchAngle;
		this.referenceRollAngle = referenceRollAngle;
	}

	public void setReferenceQuad(Quadrangle referenceQuad) {
		this.referenceQuad = referenceQuad;
	}

	public void setReferenceAzimuthAngle(float referenceAzimuthAngle) {
		this.referenceAzimuthAngle = referenceAzimuthAngle;
	}

	public void setReferencePitchAngle(float referencePitchAngle) {
		this.referencePitchAngle = referencePitchAngle;
	}

	public void setReferenceRollAngle(float referenceRollAngle) {
		this.referenceRollAngle = referenceRollAngle;
	}
	
	public void setTarget(Quadrangle rectangle) {
		this.referenceQuad = rectangle;
	}
	
	public Quadrangle track(float[] angleChange) {
		Mat angleChangeMat = new Mat(1, 3, CvType.CV_32FC1);
		
		angleChangeMat.put(0, 0, angleChange[0]);
		angleChangeMat.put(0, 1, angleChange[1]);
		angleChangeMat.put(0, 2, angleChange[2]);
		
		/*angleChangeMat.put(1, 0, angleChange[3]);
		angleChangeMat.put(1, 1, angleChange[4]);
		angleChangeMat.put(1, 2, angleChange[5]);
		
		angleChangeMat.put(2, 0, angleChange[6]);
		angleChangeMat.put(2, 1, angleChange[7]);
		angleChangeMat.put(2, 2, angleChange[8]);*/
		
		Mat rodriguesMat = new Mat(3, 3, CvType.CV_32FC1);
		Calib3d.Rodrigues(angleChangeMat, rodriguesMat);
		
		Mat transformedQuad = new Mat(4, 1, CvType.CV_32FC2);
		Core.perspectiveTransform(this.referenceQuad.asPerspectiveTransformMatrix(), transformedQuad, rodriguesMat);
		
		return new Quadrangle(transformedQuad);
	}

}
