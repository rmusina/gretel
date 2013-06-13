package com.gretel.services.controllers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;

import com.gretel.imageprocessors.BitmapStorage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;

public class MultipartPostRequestIntentService extends AbstractRequestIntentService {
	
	public final static String STRING_BODY = "STRING_BODY";
	
	public final static String FILE_BODY = "FILE_BODY";
	
	private final static String BOUNDARY = "-----------------------------109251257332103";
	
	public MultipartPostRequestIntentService() {
		super("MultipartRequestIntentService");	
	}

	private ByteArrayBody getFileAsBAB(Bitmap bitmap, String fileName) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, 90, outputStream);
        byte[] data = outputStream.toByteArray();
        
        return new ByteArrayBody(data, fileName);
	}
	
	protected HttpEntity parseIntentData(Intent intent)	throws Exception {
		MultipartEntity entity = new MultipartEntity(
				HttpMultipartMode.BROWSER_COMPATIBLE, 
				BOUNDARY, 
				Charset.defaultCharset());
	    
		Bundle stringBody = intent.getBundleExtra(STRING_BODY);
		
		for (String key : stringBody.keySet()) {
			entity.addPart(key, new StringBody(stringBody.getString(key)));
		}
		
		Bundle fileBody = intent.getBundleExtra(FILE_BODY);
		BitmapStorage bitmapStorage = new BitmapStorage();
		
		for (String key : fileBody.keySet()) {
			String filePath = fileBody.getString(key);
			Bitmap file = bitmapStorage.getBitmapFromStorage(filePath);
			entity.addPart(key, this.getFileAsBAB(file, new File(filePath).getName()));
		}
		
		return entity;
	}

	@Override
	protected HttpRequestBase buildRequestObject(String url, Intent intent)
			throws Exception {
		HttpPost httpPost = new HttpPost(url);
	    httpPost.setHeader("Accept", "application/json");
	    httpPost.setHeader("Content-type", String.format("multipart/form-data; boundary=%s", BOUNDARY));	    
	    httpPost.setEntity(this.parseIntentData(intent));
	    
		return httpPost;
	}
}
