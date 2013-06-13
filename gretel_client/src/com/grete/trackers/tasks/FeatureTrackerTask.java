package com.grete.trackers.tasks;

import java.util.List;

import org.opencv.core.Mat;

import com.gretel.trackers.FeatureTracker;
import com.gretel.trakers.objects.TrackableObject;

import android.os.AsyncTask;


public class FeatureTrackerTask extends AsyncTask<Mat, Void, List<TrackableObject>>{

	private FeatureTracker tracker;
	
	private TrackingListener listener;
	
	public FeatureTrackerTask(TrackingListener listener, FeatureTracker tracker) {
		super();
		this.listener = listener;
		this.tracker = tracker;
	}
	
	@Override
	protected List<TrackableObject> doInBackground(Mat... image) {
		return this.tracker.trackAll(image[0]);
	}
	
	@Override
    protected void onPostExecute(List<TrackableObject> result) {
		this.listener.onTrackingFinished(result);
    }
}
