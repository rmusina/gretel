package com.gretel.activities;

import com.gretel.R;
import com.gretel.trakers.objects.Quadrangle;
import com.gretel.views.PaintView;
import com.gretel.dialogs.ColorPickerDialog;
import com.gretel.imageprocessors.BitmapStorage;
import com.gretel.services.controllers.MultipartPostRequestIntentService;
import com.gretel.services.controllers.ServiceResultReceiver;
import com.gretel.services.controllers.ServiceStatus;

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

public class PaintActivity extends Activity 
	implements ColorPickerDialog.OnColorChangedListener,
			  ServiceResultReceiver.Receiver{
	
	private final String SERVICE_URL = "http://192.168.0.103:8000/artefacts/";
	    
	private ServiceResultReceiver serviceResultReceiver;
	
	private PaintView paintView;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
		Quadrangle boundingRect = (Quadrangle)intent.getParcelableExtra(MainActivity.BOUNDING_RECTANGLE);
		
		String drawingSurfaceFileName = intent.getStringExtra(MainActivity.DRAWING_SURFACE_FILE_NAME);
		BitmapStorage bitmapStorage = new BitmapStorage(this);
		Bitmap drawingSurface = bitmapStorage.getBitmapFromStorage(drawingSurfaceFileName);
		bitmapStorage.deleteBitmapFromStorage(drawingSurfaceFileName);
		
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
	
	private void onDoneEditing() {
		Bundle stringBody = new Bundle();
		stringBody.putString("lat", "42.1");
		stringBody.putString("lon", "23.1");
		
		BitmapStorage storage = new BitmapStorage(this);
		Bundle fileBody = new Bundle();
		String imagePath = storage.saveBitmapToStorage(this.paintView.getBitmap());
		fileBody.putString("image", imagePath);
		
		final Intent intent = new Intent(this, MultipartPostRequestIntentService.class);
        
        intent.putExtra(MultipartPostRequestIntentService.RESULT_RECEIVER, this.serviceResultReceiver);
        intent.putExtra(MultipartPostRequestIntentService.REQUEST_URL, this.SERVICE_URL);
        intent.putExtra(MultipartPostRequestIntentService.STRING_BODY, stringBody);
        intent.putExtra(MultipartPostRequestIntentService.FILE_BODY, fileBody);
        
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
}
