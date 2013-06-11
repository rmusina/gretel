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

public class FeatureTracker {
	private final int MIN_MATCH_COUNT = 10;
		
	private final double HOMOGRAPHY_THRESHOLD = 3.0;
	
	private FeatureDetector detector = FeatureDetector.create(FeatureDetector.BRISK);
	
	private DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.FREAK);
	
	private DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
		
	private MatOfKeyPoint trainingFeatures = new MatOfKeyPoint();
	
	private Quadrangle rectangle;
	
	public void loadTrackingSettings(File detectorSettingsFile, File descriptorSettingsFile) {
		if (detectorSettingsFile != null) {
			this.detector.read(detectorSettingsFile.getAbsolutePath());
			this.descriptor.read(descriptorSettingsFile.getAbsolutePath());
		}
	}
		
	public static Mat getTrakingMask(Mat image, Quadrangle rectangle) {
		int rows = image.rows();
		int cols = image.cols();
		
		int xMin = (int)rectangle.getPoint1().x; int yMin = (int)rectangle.getPoint1().y;
		int xMax = (int)rectangle.getPoint3().x; int yMax = (int)rectangle.getPoint3().y;
		
		Mat mask = new Mat(rows, cols, CvType.CV_8UC1);
		
		for (int x = xMin; x <= xMax; x++) {
			for (int y = yMin; y <= yMax; y++) {
				mask.put(y, x, 1.0);
			}
		}
		
		return mask;
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

	public void setTarget(Mat image, Quadrangle rectangle) {
		Mat trackingMask = getTrakingMask(image, rectangle);		
		MatOfKeyPoint features = detectFeatures(image, trackingMask);
		Mat featureDescription = describeFeatures(image, features);
		
		this.matcher.clear();
		this.matcher.add(Arrays.asList(featureDescription));
		this.trainingFeatures = features;
		this.rectangle = rectangle;
	}

	public Quadrangle track(Mat image) {
		MatOfKeyPoint frameFeatures = this.detectFeatures(image);
		Mat frameFeaturesDescription = this.describeFeatures(image, frameFeatures);
		List<DMatch> matches = this.getMatches(image, frameFeaturesDescription, frameFeatures);
		
		if (matches.size() < this.MIN_MATCH_COUNT) {
			frameFeatures.release();
			frameFeaturesDescription.release();
			
			return null;
		}
		
		KeyPoint[] trainingKeyPoints = this.trainingFeatures.toArray();
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

		/*double[][] h = new double[3][3];
		
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				h[i][j] = homography.get(i, j)[0];
			}
		}*/
		
		trainingPoints2f.release();
		framePoints2f.release();
		frameFeatures.release();
		frameFeaturesDescription.release();
		
		if (Core.sumElems(homography).val[0] < this.MIN_MATCH_COUNT) {
			status.release();
			homography.release();
			
			return null;
		}

		Mat frameQuad = new Mat(4, 1, CvType.CV_32FC2);
		Core.perspectiveTransform(this.rectangle.asPerspectiveTransformMatrix(), frameQuad, homography);		
		Quadrangle projectedQuad = new Quadrangle(frameQuad);
		
		frameQuad.release();
		status.release();
		homography.release();

		return projectedQuad;
	}
}
