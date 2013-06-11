package com.gretel.trackers;

import org.opencv.core.Mat;

import android.os.AsyncTask;


public class FeatureTrackerTask extends AsyncTask<Mat, Void, Quadrangle>{

	private FeatureTracker tracker;
	
	private TrackingListener listener;
	
	public FeatureTrackerTask(TrackingListener listener, FeatureTracker tracker) {
		super();
		this.listener = listener;
		this.tracker = tracker;
	}
	
	@Override
	protected Quadrangle doInBackground(Mat... image) {
		return this.tracker.track(image[0]);
	}
	
	@Override
    protected void onPostExecute(Quadrangle result) {
		this.listener.onTrackingFinished(result);
    }
}
