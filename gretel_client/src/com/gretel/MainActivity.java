package com.gretel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;

import com.gretel.trackers.AccelerometerTracker;
import com.gretel.trackers.FeatureTracker;
import com.gretel.trackers.Quadrangle;
import com.gretel.trackers.TrackingListener;
import com.gretel.utils.BitmapConverter;
import com.gretel.utils.BitmapStorage;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener, SensorEventListener, TrackingListener  {
	
	private final Scalar RECTANGLE_COLOR = new Scalar(255);
	
	private final int TRACKING_FREQUENCY = 5;
		
	private final String DETECTOR_SETTINGS_FILE_NAME = "detector_settings.yml";
	
	private final String DESCRIPTOR_SETTINGS_FILE_NAME = "descriptor_settings.yml";

    public final static String BOUNDING_RECTANGLE = "com.gretel.BOUNDING_RECTANGLE";

	public static final String DRAWING_SURFACE_FILE_NAME = "com.gretel.DRAWING_SURFACE";
	
	private CameraBridgeViewBase openCvCameraView;
	
	private Mat rgbaFrame;
	
	private Mat grayscaleFrame;
	
	private int framesSinceLastTracking = 0;
	
	private Quadrangle rectangle = new Quadrangle();
	
	private SensorManager sensorManager;
	
	private Sensor accelerometer;
	
	private Sensor geomagnetic;
	
	private float[] magneticValues;
	
	private float[] accelerometerValues;

	private float azimuthAngle;

	private float pitchAngle;

	private float rollAngle;
	
	private float[] rotationMatrix = new float[9];
	
	private float[] referenceRotationMatrix; 
	
	private boolean drawRectangle = false;

	private boolean trackFeatures = false;
	
	private FeatureTracker featureTracker;
	
	private AccelerometerTracker accTracker;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	            {	            	
	            	openCvCameraView.setOnTouchListener(MainActivity.this);
	            	openCvCameraView.enableView();
	            	
					featureTracker = new FeatureTracker();
					File detectorSettingsFile = writeSettingsFile(DETECTOR_SETTINGS_FILE_NAME, R.raw.detector_settings);
					File descriptorSettingsFile = writeSettingsFile(DESCRIPTOR_SETTINGS_FILE_NAME, R.raw.descriptor_settings);
					
					featureTracker.loadTrackingSettings(detectorSettingsFile, descriptorSettingsFile);
					
					removeSettingsFile(detectorSettingsFile);			
					removeSettingsFile(descriptorSettingsFile);
	            } break;
	            default:
	            {
	                super.onManagerConnected(status);
	            } break;
	        }
	    }
	    
	    //hack to write resource file to disk, and read it like a regular file from the tracker object
	    private File writeSettingsFile(String fileName, int resourceId){
	    	try {
	            InputStream is = getResources().openRawResource(resourceId);
	            File settingsDir = getDir("detector", Context.MODE_PRIVATE);
	            File settingsFile = new File(settingsDir, fileName);
	            FileOutputStream os = new FileOutputStream(settingsFile);

	            byte[] buffer = new byte[4096];
	            int bytesRead;
	            while ((bytesRead = is.read(buffer)) != -1) {
	                os.write(buffer, 0, bytesRead);
	            }
	            
	            is.close();
	            os.close();
	            
	            return settingsFile;
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    	
	    	return null;
	    }
	    
	    private void removeSettingsFile(File settingsFile) {
	    	if (settingsFile != null) {
	    		settingsFile.getParentFile().delete();
	    	}
	    }
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.activity_main);
		this.openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.MainCameraView);
		this.openCvCameraView.setVisibility(SurfaceView.VISIBLE);
		this.openCvCameraView.setCvCameraViewListener(this);
		
		this.accTracker = new AccelerometerTracker();

		this.sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		this.accelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		this.geomagnetic = this.sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}

	@Override
	public void onResume()
	{
	    super.onResume();

        this.sensorManager.registerListener(this, this.accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        this.sensorManager.registerListener(this, this.geomagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        
	    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this, mLoaderCallback);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		this.sensorManager.unregisterListener(this);
		
		if (this.openCvCameraView != null)
			this.openCvCameraView.disableView();
	}

	public void onDestroy() {
		super.onDestroy();
		
		if (this.openCvCameraView != null)
			this.openCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		this.rgbaFrame = new Mat(height, width, CvType.CV_8UC4);
		this.grayscaleFrame = new Mat(height, width, CvType.CV_8UC1);
	}

	public void onCameraViewStopped() {
		this.rgbaFrame.release();
		this.grayscaleFrame.release();
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		this.rgbaFrame = inputFrame.rgba();
		this.grayscaleFrame = inputFrame.gray();
		
		if (this.drawRectangle) {
			Core.rectangle(this.rgbaFrame, this.rectangle.getPoint1(), this.rectangle.getPoint3(), this.RECTANGLE_COLOR, 3);
		} else if (this.trackFeatures) {
			/*if (this.framesSinceLastTracking == this.TRACKING_FREQUENCY) {
				new FeatureTrackerTask(this, this.featureTracker).execute(this.grayscaleFrame);
				this.framesSinceLastTracking = 0;
			}
			
			float[] angleChange = new float[3];
			SensorManager.getAngleChange(angleChange, this.rotationMatrix, this.referenceRotationMatrix);
			this.rectangle = this.accTracker.track(new float[] {this.azimuthAngle, this.pitchAngle, this.rollAngle});
			
			
			this.framesSinceLastTracking++;
			this.drawLastDetectedQuad();*/
		}
		/*
		DecimalFormat df = new DecimalFormat("0.00##");
	    Core.putText(this.rgbaFrame, "az " + df.format(this.azimuthAngle), new Point(10, 30), 3, 1, new Scalar(255, 0, 0, 255), 2);
	    Core.putText(this.rgbaFrame, "pi " + df.format(this.pitchAngle), new Point(10, 60), 3, 1, new Scalar(255, 0, 0, 255), 2);
	    Core.putText(this.rgbaFrame, "ro " + df.format(this.rollAngle), new Point(10, 90), 3, 1, new Scalar(255, 0, 0, 255), 2);
		*/
		
		
		return this.rgbaFrame;
	}
	
	@Override
	public void onTrackingFinished(Quadrangle detectedQuad) {
		if (this.trackFeatures) {
			if (detectedQuad != null && !detectedQuad.isMalformed()) {
				this.rectangle = detectedQuad;
			}
			this.drawLastDetectedQuad();
		}
	}
	
	private void drawLastDetectedQuad() {
		Core.polylines(this.rgbaFrame, this.rectangle.asPolyLineList(), true, this.RECTANGLE_COLOR, 3);
	}
	
	private void onRectangleSelectionStarted(MotionEvent event) {
		int cols = this.rgbaFrame.cols();
        int rows = this.rgbaFrame.rows();
               
        int xOffset = (this.openCvCameraView.getWidth() - cols) / 2;
        int yOffset = (this.openCvCameraView.getHeight() - rows) / 2;
        
        int xMin = (int) Math.min(event.getX(0), event.getX(1));
        int xMax = (int) Math.max(event.getX(0), event.getX(1));        
        int yMin = (int) Math.min(event.getY(0), event.getY(1));
        int yMax = (int) Math.max(event.getY(0), event.getY(1));

		this.rectangle.setPoint1(new Point(xMin - xOffset, yMin - yOffset));
		this.rectangle.setPoint2(new Point(xMax - xOffset, yMin - yOffset));
		this.rectangle.setPoint3(new Point(xMax - xOffset, yMax - yOffset));
		this.rectangle.setPoint4(new Point(xMin - xOffset, yMax - yOffset));
	}
	
	private void onRectangleSelectionCompleted() {
		//this.featureTracker.setTarget(this.grayscaleFrame, this.rectangle);
		//this.referenceRotationMatrix = this.rotationMatrix.clone();
		//this.accTracker.setTarget(this.rectangle);
		
		BitmapStorage bitmapStorage = new BitmapStorage(this);
		BitmapConverter bitmapConverter = new BitmapConverter();
		String bitmapFileName = bitmapStorage.saveBitmapToStorage(bitmapConverter.fromMat(this.rgbaFrame));
		
		Intent paintIntent = new Intent(this, PaintActivity.class);
		paintIntent.putExtra(BOUNDING_RECTANGLE, this.rectangle);  
		paintIntent.putExtra(DRAWING_SURFACE_FILE_NAME, bitmapFileName);
		startActivity(paintIntent);
	}
	
	private void toggleRectDrawingAndTracking(boolean isDrawingRect){
		this.drawRectangle = isDrawingRect;
		this.trackFeatures = !isDrawingRect;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
	    switch (event.getAction()) {
	    	case MotionEvent.ACTION_DOWN:
	    	case MotionEvent.ACTION_MOVE:
	    		if (event.getPointerCount() == 2) {
	    			this.toggleRectDrawingAndTracking(true);
		    		this.onRectangleSelectionStarted(event);		    			
	    		}
				break;
	    	case MotionEvent.ACTION_CANCEL:
	    	case MotionEvent.ACTION_UP:
	    		if (this.drawRectangle) {
	    			this.toggleRectDrawingAndTracking(false);
	    			this.onRectangleSelectionCompleted();
	    		}
	    		break;
	    	default:
	    		return false;
	    }
	    
	    return true;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		/*switch (event.sensor.getType()) {
			case Sensor.TYPE_MAGNETIC_FIELD:
				this.magneticValues = event.values.clone();
				break;
			case Sensor.TYPE_ACCELEROMETER:
				this.accelerometerValues = event.values.clone();
				break;
		}

		if (this.magneticValues != null && this.accelerometerValues != null) {
			SensorManager.getRotationMatrix(this.rotationMatrix, null, this.accelerometerValues, this.magneticValues);
			SensorManager.remapCoordinateSystem(this.rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, this.rotationMatrix);
			
			float[] orientation = new float[3];
			SensorManager.getOrientation(this.rotationMatrix, orientation);
			
			this.azimuthAngle = orientation[0];
			this.pitchAngle = orientation[1];
			this.rollAngle = orientation[2];
		}*/
	}
}
