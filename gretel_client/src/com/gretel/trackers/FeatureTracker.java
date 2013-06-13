package com.gretel.trackers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;

import com.gretel.trakers.objects.TrackableObject;
import com.gretel.trakers.objects.TrackingTarget;

public class FeatureTracker {
	private final int MIN_MATCH_COUNT = 10;
		
	private final double HOMOGRAPHY_THRESHOLD = 3.0;
	
	private FeatureDetector detector = FeatureDetector.create(FeatureDetector.BRISK);
	
	private DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.FREAK);
	
	private DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
		
	private List<TrackingTarget> trackingTargets = new ArrayList<TrackingTarget>();
	
	public void loadTrackingSettings(File detectorSettingsFile, File descriptorSettingsFile) {
		if (detectorSettingsFile != null) {
			this.detector.read(detectorSettingsFile.getAbsolutePath());
			this.descriptor.read(descriptorSettingsFile.getAbsolutePath());
		}
	}
	
	public MatOfKeyPoint detectFeatures(Mat image) {
		return detectFeatures(image, Mat.ones(image.rows(), image.cols(), CvType.CV_8UC1));
	}
	
	public MatOfKeyPoint detectFeatures(Mat image, Mat mask){
		MatOfKeyPoint keypointsMat = new MatOfKeyPoint();		
		this.detector.detect(image, keypointsMat, mask);		
		return keypointsMat;
	}
	
	public Mat describeFeatures(Mat image, MatOfKeyPoint keypointsMat) {
		Mat descriptors = new Mat();		
		this.descriptor.compute(image, keypointsMat, descriptors);		
		return descriptors;
	}
	
	public List<DMatch> getMatches(Mat image, Mat queryDescriptors, MatOfKeyPoint queryKeyPoints) {
		List<MatOfDMatch> rawMatches = new ArrayList<MatOfDMatch>();		
		this.matcher.knnMatch(queryDescriptors, rawMatches, 2);
		
		List<DMatch> matches = new ArrayList<DMatch>(rawMatches.size());
				
		for (MatOfDMatch matchMat : rawMatches) {
			DMatch[] match = matchMat.toArray();
			
			if (match.length == 2 && match[0].distance < 150 && match[0].distance < match[1].distance * 0.75) {
				matches.add(match[0]);
			}
		}
		
		return matches;
	}

	public void addTarget(Mat image, TrackableObject trackableObject) {
		Mat trackingMask = trackableObject.getTrakingMask(image);		
		MatOfKeyPoint features = this.detectFeatures(image, trackingMask);
		Mat featuresDescription = this.describeFeatures(image, features);
		
		this.trackingTargets.add(new TrackingTarget(features, featuresDescription, trackableObject));
		this.matcher.add(Arrays.asList(featuresDescription));
	}
	
	public void clearTargets() {
		this.matcher.clear();
		this.trackingTargets.clear();
	}

	public TrackableObject track(Mat image, TrackingTarget trackingTarget) {
		MatOfKeyPoint frameFeatures = this.detectFeatures(image);
		Mat frameFeaturesDescription = this.describeFeatures(image, frameFeatures);
		List<DMatch> matches = this.getMatches(image, frameFeaturesDescription, frameFeatures);
		
		if (matches.size() < this.MIN_MATCH_COUNT) {
			frameFeatures.release();
			frameFeaturesDescription.release();
			
			return null;
		}
		
		KeyPoint[] trainingKeyPoints = trackingTarget.getFeatures().toArray();
		KeyPoint[] frameKeyPoints = frameFeatures.toArray(); 
		
		List<Point> trainingPoints = new ArrayList<Point>(matches.size());
		List<Point> framePoints = new ArrayList<Point>(matches.size());
		
		for (DMatch match : matches) {
			trainingPoints.add(trainingKeyPoints[match.trainIdx].pt);
			framePoints.add(frameKeyPoints[match.queryIdx].pt);
		}
		
		MatOfPoint2f trainingPoints2f = new MatOfPoint2f();
		trainingPoints2f.fromList(trainingPoints);
		
		MatOfPoint2f framePoints2f = new MatOfPoint2f();
		framePoints2f.fromList(framePoints);
		
		Mat status = new Mat();
		Mat homography = Calib3d.findHomography(trainingPoints2f, framePoints2f, Calib3d.RANSAC, this.HOMOGRAPHY_THRESHOLD, status);
		
		trainingPoints2f.release();
		framePoints2f.release();
		frameFeatures.release();
		frameFeaturesDescription.release();
		
		TrackableObject projectedObject = null;
		if (Core.sumElems(status).val[0] >= this.MIN_MATCH_COUNT) {
			projectedObject = trackingTarget.getTrackedObject().perspectiveTransform(homography);
		}

		status.release();
		homography.release();

		return projectedObject;
	}
	
	public List<TrackableObject> trackAll(Mat image) {
		List<TrackableObject> trackedObjects = new ArrayList<TrackableObject>();
		
		for (TrackingTarget target : this.trackingTargets) {
			TrackableObject trackedObject = this.track(image, target);
			
			if (trackedObject != null) {
				trackedObjects.add(trackedObject);
			}
		}
		
		return trackedObjects;
	}
}
