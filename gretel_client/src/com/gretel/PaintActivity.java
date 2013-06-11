package com.gretel;

import com.gretel.trackers.Quadrangle;
import com.gretel.utils.BitmapStorage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

public class PaintActivity extends Activity
	implements ColorPickerDialog.OnColorChangedListener {
	
    private Paint paint;
    
	private Quadrangle boundingRect;
	
	private Bitmap drawingSurface;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(new PaintView(this));

        this.paint = new Paint();
        this.paint.setAntiAlias(true);
        this.paint.setDither(true);
        this.paint.setColor(0xFFFF0000);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeJoin(Paint.Join.ROUND);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setStrokeWidth(12);
        
        Intent intent = getIntent();
		this.boundingRect = (Quadrangle)intent.getParcelableExtra(MainActivity.BOUNDING_RECTANGLE);
		
		String drawingSurfaceFileName = intent.getStringExtra(MainActivity.DRAWING_SURFACE_FILE_NAME);
		BitmapStorage bitmapStorage = new BitmapStorage(this);
		this.drawingSurface = bitmapStorage.getBitmapFromStorage(drawingSurfaceFileName);
		bitmapStorage.deleteBitmapFromStorage(drawingSurfaceFileName);
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
	    		new ColorPickerDialog(this, this, this.paint.getColor()).show();
                return true;
	    	case R.id.menu_done:
	    		break;
    	}
    	
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void colorChanged(int color) {
		this.paint.setColor(color);
	}
	
	public class PaintView extends View {        
	    private static final float TOUCH_TOLERANCE = 4;
	    
		private Bitmap bitmap;
		
	    private Canvas canvas;
	    
	    private Path path;
	    
	    private Paint bitmapPaint;
	    
	    private Paint backgroundPaint;
		
	    private float prevX;
	    
	    private float prevY;
	
	    public PaintView(Context c) {
	        super(c);
	
	        this.path = new Path();
	        this.backgroundPaint = new Paint(Paint.DITHER_FLAG);
	        this.bitmapPaint = new Paint(Paint.DITHER_FLAG);
	    }
	
	    @Override
	    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	        super.onSizeChanged(w, h, oldw, oldh);
	        this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
	        this.canvas = new Canvas(this.bitmap);
	    }
	
	    @Override
	    protected void onDraw(Canvas canvas) {
	    	canvas.drawColor(0xFFAAAAAA);
	    	canvas.drawBitmap(drawingSurface, 0, 0, this.backgroundPaint);
	    	canvas.drawBitmap(this.bitmap, 0, 0, this.bitmapPaint);	
	    	canvas.drawPath(this.path, paint);
	    }
	
	    private boolean isInBoundingRectangle(float x, float y) {
	    	if (x >= boundingRect.getPoint1().x && x <= boundingRect.getPoint3().x &&
	        	y >= boundingRect.getPoint1().y && y <= boundingRect.getPoint3().y) {
	    		return true;
	    	}
	    	
	    	return false;
	    }
	    
	    private void onTouchStart(float x, float y) {
	        this.path.reset();
	        
	        if (isInBoundingRectangle(x, y)) {
		        this.path.moveTo(x, y);
		        this.prevX = x;
		        this.prevY = y;
	        }
	    }
	    
	    private void onTouchMove(float x, float y) {
	        float dx = Math.abs(x - this.prevX);
	        float dy = Math.abs(y - this.prevY);
	        
	        if ((dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) && isInBoundingRectangle(x, y)) {
	        	if (!this.path.isEmpty()) {
	        		this.path.quadTo(this.prevX, this.prevY, (x + this.prevX)/2, (y + this.prevY)/2);
	        	} else {
	        		this.path.moveTo(x, y);
	        	}
	        	
	        	this.prevX = x;
	        	this.prevY = y;
	        }
	    }
	    
	    private void onTouchUp() {
	    	if (this.path.isEmpty()) {
	    		return;
	    	}
	    	
	    	this.path.lineTo(this.prevX, this.prevY);
	    	this.canvas.drawPath(this.path, paint);
	    	this.path.reset();
	    }
	
	    @Override
	    public boolean onTouchEvent(MotionEvent event) {
	        float x = event.getX();
	        float y = event.getY();
	
	        switch (event.getAction()) {
	            case MotionEvent.ACTION_DOWN:
	                onTouchStart(x, y);
	                invalidate();
	                break;
	            case MotionEvent.ACTION_MOVE:
	                onTouchMove(x, y);
	                invalidate();
	                break;
	            case MotionEvent.ACTION_UP:
	                onTouchUp();
	                invalidate();
	                break;
	        }
	        
	        return true;
	    }
	}
}
