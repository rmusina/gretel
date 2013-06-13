package com.grete.trackers.tasks;

import java.util.List;

import com.gretel.trakers.objects.TrackableObject;

public interface TrackingListener {
	public void onTrackingFinished(List<TrackableObject> result);
}
