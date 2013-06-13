package com.gretel.imageprocessors;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;

public class BitmapConverter {
	
	public Bitmap fromMat(Mat image) {
		Mat destFrame = new Mat(image.rows(), image.cols(), CvType.CV_8UC4);
		Imgproc.cvtColor(image, destFrame, Imgproc.COLOR_RGB2BGRA);
		
		Bitmap bitmapImage = Bitmap.createBitmap(destFrame.cols(), destFrame.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(destFrame, bitmapImage);
		
		destFrame.release();
		
		return bitmapImage;
	}
	
	public Mat toMat(Bitmap bitmapImage) {
		Mat destFrame = new Mat(bitmapImage.getHeight(), bitmapImage.getWidth(), CvType.CV_8UC4);
		Utils.bitmapToMat(bitmapImage, destFrame);
		
		return destFrame;
	}
}
