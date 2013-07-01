package com.gretel.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.gretel.GretelSettings;
import com.gretel.R;
import com.gretel.dialogs.ColorPickerDialog;
import com.gretel.imageprocessors.BitmapConverter;
import com.gretel.imageprocessors.BitmapStorage;
import com.gretel.serializers.MatOfKeyPointSerializer;
import com.gretel.serializers.MatSerializer;
import com.gretel.services.controllers.JsonPostRequestIntentService;
import com.gretel.services.controllers.ServiceResultReceiver;
import com.gretel.services.controllers.ServiceStatus;
import com.gretel.services.pojo.KeyPointPojo;
import com.gretel.services.pojo.TrackableImagePojo;
import com.gretel.trackers.FeatureTracker;
import com.gretel.trakers.objects.Quadrangle;
import com.gretel.views.PaintView;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public class PaintActivity extends Activity 
	implements ColorPickerDialog.OnColorChangedListener,
			  ServiceResultReceiver.Receiver{

    public final static String BOUNDING_RECTANGLE = "com.gretel.BOUNDING_RECTANGLE";

    public static final String DRAWING_SURFACE_FILE_NAME = "com.gretel.DRAWING_SURFACE";

    public static final String GRAFITTI_FILE_NAME = "com.gretel.GRAFITTI_FILE_NAME";

	private ServiceResultReceiver serviceResultReceiver;
	
	private PaintView paintView;
	
	private MatOfKeyPoint features;
	
	private Mat featuresDescription;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
		Quadrangle boundingRect = intent.getParcelableExtra(MainActivity.BOUNDING_RECTANGLE);
		
		String drawingSurfaceFileName = intent.getStringExtra(MainActivity.DRAWING_SURFACE_FILE_NAME);
		BitmapStorage bitmapStorage = new BitmapStorage(this);
		Bitmap drawingSurface = bitmapStorage.getBitmapFromStorage(drawingSurfaceFileName);
		bitmapStorage.deleteBitmapFromStorage(drawingSurfaceFileName);
		
		//this.describeImageFeatures(drawingSurface, boundingRect);
		
        this.paintView = new PaintView(this, drawingSurface, boundingRect);
        this.setContentView(this.paintView);
        
        this.serviceResultReceiver = new ServiceResultReceiver(new Handler());
        this.serviceResultReceiver.setReceiver(this);
	}
	
	@Override
	public void onResume()
	{
	    super.onResume();
	    
	    this.serviceResultReceiver.setReceiver(this);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		this.serviceResultReceiver.setReceiver(null);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.paint_activity_menu, menu);
        
        return true;
    }
    
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
    	switch (item.getItemId()) {
	    	case R.id.menu_color:
	    		new ColorPickerDialog(this, this, this.paintView.getPaint().getColor()).show();
                return true;
	    	case R.id.menu_done:
	    		onDoneEditing();
	    		break;
    	}
    	
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void colorChanged(int color) {
		this.paintView.getPaint().setColor(color);
	}
	
	private void describeImageFeatures(Bitmap bitmap, Quadrangle boundingRectangle) {
		FeatureTracker tracker = new FeatureTracker();
		BitmapConverter converter = new BitmapConverter();
		Mat image = converter.toMat(bitmap);
		
		this.features = tracker.detectFeatures(image, boundingRectangle.getTrackingMask(image));
        this.featuresDescription = tracker.describeFeatures(image, features);
	}

    private void postImageData() {
        Gson gson = new Gson();
        TrackableImagePojo imagePojo = new TrackableImagePojo();

        imagePojo.setLat(42.1);
        imagePojo.setLon(23.1);

        MatOfKeyPointSerializer keyPointSerializer = new MatOfKeyPointSerializer();
        KeyPointPojo[] keyPoints = keyPointSerializer.serialize(this.features);
        imagePojo.setImageFeatures(keyPoints);

        MatSerializer matSerializer = new MatSerializer();
        String[][] serFeatures = matSerializer.serialize(this.featuresDescription);
        imagePojo.setImageDesc(serFeatures);

        String json = gson.toJson(imagePojo);

        final Intent intent = new Intent(this, JsonPostRequestIntentService.class);

        intent.putExtra(JsonPostRequestIntentService.RESULT_RECEIVER, new ServiceResultReceiver(new Handler()));
        intent.putExtra(JsonPostRequestIntentService.REQUEST_URL, GretelSettings.SERVICE_URL);
        intent.putExtra(JsonPostRequestIntentService.SERIALIZED_DATA, json);

        startService(intent);
    }

	private void onDoneEditing() {
        BitmapStorage bitmapStorage = new BitmapStorage(this);
        String bitmapFileName = bitmapStorage.saveBitmapToStorage(this.paintView.getDrawingSurface());
        String graffitiFileName = bitmapStorage.saveBitmapToStorage(this.paintView.getBitmap());

        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra(BOUNDING_RECTANGLE, this.paintView.getBoundingRect());
        mainIntent.putExtra(DRAWING_SURFACE_FILE_NAME, bitmapFileName);
        mainIntent.putExtra(GRAFITTI_FILE_NAME, graffitiFileName);
        startActivity(mainIntent);
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
				Intent mainIntent = new Intent(this, MainActivity.class);
				startActivity(mainIntent);
				break;
			case ServiceStatus.ERROR:
				showErrorMessage(resultData.getString(Intent.EXTRA_TEXT));
				break;
		}
	}
}
