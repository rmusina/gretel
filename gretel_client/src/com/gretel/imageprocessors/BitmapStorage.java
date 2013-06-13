package com.gretel.imageprocessors;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapStorage {
	private File cacheDir; 
	
	public BitmapStorage() {
	}
	
	public BitmapStorage(Context context) {
		this.cacheDir = context.getCacheDir();
	}
	
	public String getRandomFileName(String extension) {
		String fileName = UUID.randomUUID().toString();
		return String.format("%s.%s", fileName, extension);
	}
	
	public String saveBitmapToStorage(Bitmap image) {
		if (this.cacheDir == null) {
			throw new NullPointerException("No context was specified for BitmapStorage.");
		}
		
		File storeFile = new File(this.cacheDir, getRandomFileName("png"));
		
		try {
			FileOutputStream fos = new FileOutputStream(storeFile); 
			image.compress(Bitmap.CompressFormat.PNG, 100, fos);
			
			fos.flush();
			fos.close();
		} catch (Exception e) {
		    e.printStackTrace();
		    return null;
		}
		
		return storeFile.getPath();
	}
	
	public Bitmap getBitmapFromStorage(String filePath) {
		try {
			return BitmapFactory.decodeFile(filePath);
		} catch(Exception e) {
		    e.printStackTrace();
			return null;
		}		
	}
	
	public boolean deleteBitmapFromStorage(String filePath) {
		try {
			File file = new File(filePath);
			return file.delete();
		} catch(Exception e) {
		    e.printStackTrace();
			return false;
		}		
	}
}
