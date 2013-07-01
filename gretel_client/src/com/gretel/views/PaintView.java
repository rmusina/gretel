package com.gretel.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.MotionEvent;
import android.view.View;

import com.gretel.trakers.objects.Quadrangle;

@SuppressLint("ViewConstructor")
public class PaintView extends View {        
    private static final float TOUCH_TOLERANCE = 4;
    
	private Bitmap bitmap;
	
    private Canvas canvas;
    
    private Path path;
    
    private Paint bitmapPaint;
    
    private Paint backgroundPaint;
    
    private Quadrangle boundingRect;
    
    private Bitmap drawingSurface;
	
    private Paint paint;
    
    private float prevX;
    
    private float prevY;

    public PaintView(Context c, Bitmap drawingSurface, Quadrangle boundingRect) {
        super(c);

        this.paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(0xFFFF0000);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(12);
        
        this.drawingSurface = drawingSurface;
        this.boundingRect = boundingRect;
        this.path = new Path();
        this.backgroundPaint = new Paint(Paint.DITHER_FLAG);
        this.bitmapPaint = new Paint(Paint.DITHER_FLAG);
    }

    public Bitmap getBitmap() {

        Bitmap croppedBitmap = Bitmap.createBitmap(this.bitmap,
                (int)boundingRect.getPoint1().x,
                (int)boundingRect.getPoint1().y,
                (int)(boundingRect.getPoint3().x - boundingRect.getPoint1().x),
                (int)(boundingRect.getPoint3().y - boundingRect.getPoint1().y));

    	return croppedBitmap;
    }
    
    public Bitmap getDrawingSurface() {
		return drawingSurface;
	}

    public Quadrangle getBoundingRect() {
        return boundingRect;
    }

	public void setDrawingSurface(Bitmap drawingSurface) {
		this.drawingSurface = drawingSurface;
	}

	public Paint getPaint() {
		return paint;
	}

	public void setPaint(Paint paint) {
		this.paint = paint;
	}

	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        this.canvas = new Canvas(this.bitmap);

        Paint transPainter = new Paint();
        transPainter.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), transPainter);
    }

    @Override
    protected void onDraw(Canvas canvas) {
    	canvas.drawColor(0xFFAAAAAA);
    	canvas.drawBitmap(this.drawingSurface, 0, 0, this.backgroundPaint);
    	canvas.drawBitmap(this.bitmap, 0, 0, this.bitmapPaint);	
    	canvas.drawPath(this.path, this.paint);
    }

    private boolean isInBoundingRectangle(float x, float y) {
    	if (x >= this.boundingRect.getPoint1().x && x <= this.boundingRect.getPoint3().x &&
        	y >= this.boundingRect.getPoint1().y && y <= this.boundingRect.getPoint3().y) {
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
    	this.canvas.drawPath(this.path, this.paint);
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
