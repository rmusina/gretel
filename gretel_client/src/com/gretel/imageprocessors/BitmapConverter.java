package com.gretel.imageprocessors;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class BitmapConverter {
	
	private Bitmap fromMat(Mat destFrame) {
		Bitmap bitmapImage = Bitmap.createBitmap(destFrame.cols(), destFrame.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(destFrame, bitmapImage);
		
		destFrame.release();
		
		return bitmapImage;
	}
	
	public Mat toMat(Bitmap bitmapImage) {
		Mat destFrame = new Mat(bitmapImage.getHeight(), bitmapImage.getWidth(), CvType.CV_8UC3);
		Utils.bitmapToMat(bitmapImage, destFrame);
		
		return destFrame;
	}
	
	public Bitmap fromRgbaMat(Mat image) {
		Mat destFrame = image.clone();
		Imgproc.cvtColor(image, destFrame, Imgproc.COLOR_RGBA2RGB);
		
		return fromMat(destFrame);
	}
	
	public Bitmap fromDescriptorMat(Mat featuresDesc) {
		Mat destFrame = featuresDesc.clone();		
		return fromMat(destFrame);
	}
}
