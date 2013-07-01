package com.gretel.trackers;

import com.gretel.trakers.objects.TrackableObject;

import java.util.List;

public interface TrackingListener {
	public void onTrackingFinished(List<TrackableObject> result);
}
