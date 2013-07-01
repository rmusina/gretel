package com.gretel.services.controllers;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.KeyPoint;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gretel.GretelSettings;
import com.gretel.imageprocessors.BitmapCache;
import com.gretel.imageprocessors.BitmapConverter;
import com.gretel.services.pojo.ArtefactServicePojo;
import com.gretel.services.pojo.KeyPointPojo;
import com.gretel.services.pojo.TrackableImagePojo;
import com.gretel.trakers.objects.Quadrangle;
import com.gretel.trakers.objects.TrackableObject;
import com.gretel.trakers.objects.TrackingTarget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

public class ArtefactDownloader
        implements ServiceResultReceiver.Receiver{
	
	private ServiceResultReceiver serviceResultReceiver;

	private Context context;

    private BitmapCache cache;

    public ArtefactDownloader(Context context, BitmapCache cache) {
		this.context = context;
        this.cache = cache;
		this.serviceResultReceiver = new ServiceResultReceiver(new Handler());
        this.serviceResultReceiver.setReceiver(this);
	}
	
	public void getArtefactData() {
		final Intent intent = new Intent(this.context, JsonGetRequestIntentService.class);
		intent.putExtra(MultipartPostRequestIntentService.RESULT_RECEIVER, this.serviceResultReceiver);
        intent.putExtra(MultipartPostRequestIntentService.REQUEST_URL, GretelSettings.SERVICE_URL);
        
        this.context.startService(intent);
	}

	private TrackingTarget parseTrackableImagePojo(TrackableImagePojo imagePojo) throws IOException {
		Gson parser = new Gson();
		/*List<KeyPointPojo> keyPointsPojo =
				parser.fromJson(imagePojo.getImageFeatures(), 
								new TypeToken<List<KeyPointPojo>>(){}.getType());
		
		KeyPoint[] keyPoints = new KeyPoint[keyPointsPojo.size()];
		int i = 0;
		
		for (KeyPointPojo keyPoint : keyPointsPojo) {
			if (keyPoint != null) {
				keyPoints[i++] = new KeyPoint(keyPoint.getX(), keyPoint.getY(), 1);
			}
		}

		TrackingTarget trackingTarget = new TrackingTarget(new MatOfKeyPoint(keyPoints), new Mat(), new Quadrangle());
		
		return trackingTarget;*/
        return null;
	}
	
	private List<TrackingTarget> parseResultData(Bundle resultData) {
		String json = resultData.getString(Intent.EXTRA_TEXT);	
		Gson parser = new Gson();
		ArtefactServicePojo imageInfo = parser.fromJson(json, ArtefactServicePojo.class);	
		
		List<TrackingTarget> trackingTargets = new ArrayList<TrackingTarget>();

		for (TrackableImagePojo imagePojo : imageInfo.getResults()) {
            try {
                trackingTargets.add(parseTrackableImagePojo(imagePojo));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

		return trackingTargets;
	}

	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
			case ServiceStatus.RUNNING:
				break;
			case ServiceStatus.FINISHED:
				parseResultData(resultData);
				break;
			case ServiceStatus.ERROR:
				break;
		}
	}
}
