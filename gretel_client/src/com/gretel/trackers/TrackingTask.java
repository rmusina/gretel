package com.gretel.trackers;

import android.os.AsyncTask;

import com.gretel.trakers.objects.TrackableObject;

import org.opencv.core.Mat;

import java.util.List;


public class TrackingTask extends AsyncTask<Mat, Void, List<TrackableObject>>{

	private FeatureTracker tracker;
	
	private TrackingListener listener;
	
	public TrackingTask(TrackingListener listener, FeatureTracker tracker) {
		super();
		this.listener = listener;
		this.tracker = tracker;
	}
	
	@Override
	protected List<TrackableObject> doInBackground(Mat... image) {
		//return this.tracker.trackAll(image[0]);
        return null;
	}
	
	@Override
    protected void onPostExecute(List<TrackableObject> result) {
		//this.listener.onTrackingFinished(result);
    }
}
