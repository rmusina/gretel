package com.gretel.trakers.objects;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public class TrackingTarget {
	private MatOfKeyPoint features;
	
	private Mat featuresDescription;
	
	private TrackableObject trackedObject;
	
	public TrackingTarget(MatOfKeyPoint features,
						  Mat featuresDescription, 
						  TrackableObject trackedObject) {
		this.features = features;
		this.featuresDescription = featuresDescription;
		this.trackedObject = trackedObject;
	}

	public MatOfKeyPoint getFeatures() {
		return features;
	}

	public void setFeatures(MatOfKeyPoint features) {
		this.features = features;
	}

	public Mat getFeaturesDescription() {
		return featuresDescription;
	}

	public void setFeaturesDescription(Mat featureDescription) {
		this.featuresDescription = featureDescription;
	}

	public TrackableObject getTrackedObject() {
		return trackedObject;
	}

	public void setTrackedObject(TrackableObject trackedObject) {
		this.trackedObject = trackedObject;
	}
}
