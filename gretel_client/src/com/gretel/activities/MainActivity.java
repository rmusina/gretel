package com.gretel.activities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

import com.grete.trackers.tasks.FeatureTrackerTask;
import com.grete.trackers.tasks.TrackingListener;
import com.gretel.R;
import com.gretel.imageprocessors.BitmapConverter;
import com.gretel.imageprocessors.BitmapStorage;
import com.gretel.services.controllers.JsonGetRequestIntentService;
import com.gretel.services.controllers.MultipartPostRequestIntentService;
import com.gretel.services.controllers.ServiceResultReceiver;
import com.gretel.services.controllers.ServiceStatus;
import com.gretel.trackers.AccelerometerTracker;
import com.gretel.trackers.FeatureTracker;
import com.gretel.trakers.objects.Quadrangle;
import com.gretel.trakers.objects.TrackableObject;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

public class MainActivity extends Activity implements 
				CvCameraViewListener2, 
				View.OnTouchListener, 
				TrackingListener,
				ServiceResultReceiver.Receiver {
	
	private final Scalar RECTANGLE_COLOR = new Scalar(255);
	
	private final int TRACKING_FREQUENCY = 3;
		
	private final String DETECTOR_SETTINGS_FILE_NAME = "detector_settings.yml";
	
	private final String DESCRIPTOR_SETTINGS_FILE_NAME = "descriptor_settings.yml";

    public final static String BOUNDING_RECTANGLE = "com.gretel.BOUNDING_RECTANGLE";

	public static final String DRAWING_SURFACE_FILE_NAME = "com.gretel.DRAWING_SURFACE";
	
	private final String SERVICE_URL = "http://192.168.0.103:8000/artefacts/";
	
	private CameraBridgeViewBase openCvCameraView;
	
	private Mat rgbaFrame;
	
	private Mat grayscaleFrame;
	
	private int framesSinceLastTracking = 0;
	
	private Quadrangle rectangle = new Quadrangle();
	
	private boolean drawRectangle = false;

	private boolean trackFeatures = false;
	
	private FeatureTracker featureTracker;
	
	private List<TrackableObject> prevTrackedObjects = new ArrayList<TrackableObject>();
	
	private ServiceResultReceiver serviceResultReceiver;
	
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
		
		this.serviceResultReceiver = new ServiceResultReceiver(new Handler());
        this.serviceResultReceiver.setReceiver(this);
	}

	@Override
	public void onResume()
	{
	    super.onResume();

	    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this, mLoaderCallback);
	    if (this.prevTrackedObjects.isEmpty()) {
	    	this.getArtefactData();
	    }
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		if (this.openCvCameraView != null)
			this.openCvCameraView.disableView();
	}

	public void onDestroy() {
		super.onDestroy();
		
		if (this.openCvCameraView != null)
			this.openCvCameraView.disableView();
	}
	
	private void getArtefactData() {
		final Intent intent = new Intent(this, JsonGetRequestIntentService.class);
		intent.putExtra(MultipartPostRequestIntentService.RESULT_RECEIVER, this.serviceResultReceiver);
        intent.putExtra(MultipartPostRequestIntentService.REQUEST_URL, this.SERVICE_URL);
        
        startService(intent);
	}
	
	private void showErrorMessage(String errorMessage) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("Error");
		alertDialogBuilder.setMessage(errorMessage);
		alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				dialog.cancel();
			}
		});
		
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
			case ServiceStatus.RUNNING:
				break;
			case ServiceStatus.FINISHED:
				showErrorMessage(resultData.getString(Intent.EXTRA_TEXT));
				break;
			case ServiceStatus.ERROR:
				showErrorMessage(resultData.getString(Intent.EXTRA_TEXT));
				break;
		}
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
			this.rectangle.draw(this.rgbaFrame, this.RECTANGLE_COLOR);
		} else if (this.trackFeatures) {
			if (this.framesSinceLastTracking == this.TRACKING_FREQUENCY) {
				new FeatureTrackerTask(this, this.featureTracker).execute(this.grayscaleFrame);
				this.framesSinceLastTracking = 0;
			}
			
			this.framesSinceLastTracking++;
			this.drawTrackableObjects(this.prevTrackedObjects);
		}		
		
		return this.rgbaFrame;
	}

	@Override
	public void onTrackingFinished(List<TrackableObject> result) {
		if (this.trackFeatures && !result.isEmpty()) {
			drawTrackableObjects(result);
			this.prevTrackedObjects = result;
		}
	}
	
	private void drawTrackableObjects(List<TrackableObject> trackableObjects) {
		for (TrackableObject trackedObject : trackableObjects) {
			trackedObject.draw(this.rgbaFrame, this.RECTANGLE_COLOR);
		}
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
		this.featureTracker.addTarget(this.grayscaleFrame, this.rectangle);
		
		/*BitmapStorage bitmapStorage = new BitmapStorage(this);
		BitmapConverter bitmapConverter = new BitmapConverter();
		String bitmapFileName = bitmapStorage.saveBitmapToStorage(bitmapConverter.fromMat(this.rgbaFrame));
		
		Intent paintIntent = new Intent(this, PaintActivity.class);
		paintIntent.putExtra(BOUNDING_RECTANGLE, this.rectangle);  
		paintIntent.putExtra(DRAWING_SURFACE_FILE_NAME, bitmapFileName);
		startActivity(paintIntent);*/
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
}
