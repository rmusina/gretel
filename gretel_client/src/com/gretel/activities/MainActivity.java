package com.gretel.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.gretel.R;
import com.gretel.imageprocessors.BitmapConverter;
import com.gretel.imageprocessors.BitmapStorage;
import com.gretel.trackers.FeatureTracker;
import com.gretel.trackers.TrackingListener;
import com.gretel.trackers.WallTracker;
import com.gretel.trakers.objects.Quadrangle;
import com.gretel.trakers.objects.TrackableImage;
import com.gretel.trakers.objects.TrackableObject;
import com.gretel.trakers.objects.TrackingTarget;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements 
				CvCameraViewListener2, 
				View.OnTouchListener, 
				TrackingListener {
	
	private final Scalar RECTANGLE_COLOR = new Scalar(255);
	
	private final int TRACKING_FREQUENCY = 3;
		
	private final String DETECTOR_SETTINGS_FILE_NAME = "detector_settings.yml";
	
	private final String DESCRIPTOR_SETTINGS_FILE_NAME = "descriptor_settings.yml";

    public final static String BOUNDING_RECTANGLE = "com.gretel.BOUNDING_RECTANGLE";

	public static final String DRAWING_SURFACE_FILE_NAME = "com.gretel.DRAWING_SURFACE";
	
	private CameraBridgeViewBase openCvCameraView;
	
	private Mat rgbaFrame;
	
	private Mat grayscaleFrame;
	
	private int framesSinceLastTracking = 0;
	
	private Quadrangle rectangle = new Quadrangle();
	
	private boolean drawRectangle = false;

	private boolean trackFeatures = false;
	
	private FeatureTracker featureTracker;

    private WallTracker wallTracker;

	private List<TrackingTarget> trackingTargets = new ArrayList<TrackingTarget>();

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

                        wallTracker = new WallTracker();
                        Intent intent = getIntent();

                        if (intent.hasExtra(PaintActivity.BOUNDING_RECTANGLE)) {
                            BitmapStorage bitmapStorage = new BitmapStorage(getApplicationContext());
                            BitmapConverter converter = new BitmapConverter();

                            Quadrangle boundingRect = intent.getParcelableExtra(PaintActivity.BOUNDING_RECTANGLE);
                            String drawingSurfaceFileName = intent.getStringExtra(PaintActivity.DRAWING_SURFACE_FILE_NAME);
                            String graffitiFileName = intent.getStringExtra(PaintActivity.GRAFITTI_FILE_NAME);

                            Bitmap drawingSurface = bitmapStorage.getBitmapFromStorage(drawingSurfaceFileName);
                            bitmapStorage.deleteBitmapFromStorage(drawingSurfaceFileName);
                            Mat drawingSurfaceMat = converter.toMat(drawingSurface);
                            Imgproc.cvtColor(drawingSurfaceMat, drawingSurfaceMat, Imgproc.COLOR_RGB2GRAY);

                            Bitmap graffiti = bitmapStorage.getBitmapFromStorage(graffitiFileName);
                            bitmapStorage.deleteBitmapFromStorage(graffitiFileName);
                            Mat graffitiMat = converter.toMat(graffiti);

                            addTrackingTarget(drawingSurfaceMat, graffitiMat, boundingRect);
                            trackFeatures = true;
                        }

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
	}

	@Override
	public void onResume()
	{
	    super.onResume();

	    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this, mLoaderCallback);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		if (this.openCvCameraView != null)
			this.openCvCameraView.disableView();
	}

	@Override
    public void onDestroy() {
		super.onDestroy();
		
		if (this.openCvCameraView != null)
			this.openCvCameraView.disableView();
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
            List<TrackableObject> trackedObjects = new ArrayList<TrackableObject>();

            /*if (this.framesSinceLastTracking % 5 == 0) {
                trackedObjects = this.featureTracker.trackAll(this.grayscaleFrame, this.trackingTargets);
            } else {
                trackedObjects = this.wallTracker.trackAll(this.grayscaleFrame, this.trackingTargets);
            }*/

            trackedObjects = this.wallTracker.trackAll(this.grayscaleFrame, this.trackingTargets);
            //this.framesSinceLastTracking++;
			this.drawTrackableObjects(trackedObjects);
		}

        return this.rgbaFrame;
	}

	@Override
	public void onTrackingFinished(List<TrackableObject> result) {
		if (this.trackFeatures && !result.isEmpty()) {
            drawTrackableObjects(result);
		}
	}
	
	private void drawTrackableObjects(List<TrackableObject> trackedObjects) {
		for (TrackableObject trackedObject : trackedObjects) {
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

    private void addTrackingTarget(Mat background, Mat graffiti, Quadrangle selectedQuad) {
        MatOfKeyPoint features = this.featureTracker.detectFeatures(
                background,
                selectedQuad.getTrackingMask(background));
        Mat featuresDescription = this.featureTracker.describeFeatures(
                background,
                features);

        TrackableImage trackableImage = new TrackableImage(graffiti, selectedQuad);
        TrackingTarget target = new TrackingTarget(features, featuresDescription, trackableImage);
        this.trackingTargets.add(target);
    }

	private void onRectangleSelectionCompleted() {
        BitmapStorage bitmapStorage = new BitmapStorage(this);
		BitmapConverter bitmapConverter = new BitmapConverter();
		String bitmapFileName = bitmapStorage.saveBitmapToStorage(bitmapConverter.fromRgbaMat(this.rgbaFrame));
		
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
}
