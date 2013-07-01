package com.gretel.trackers;

import com.gretel.trakers.objects.TrackableObject;
import com.gretel.trakers.objects.TrackingTarget;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public abstract class Tracker {
    public abstract TrackableObject track(Mat image, TrackingTarget trackingTarget);

    public List<TrackableObject> trackAll(Mat image, List<TrackingTarget> targets) {
        List<TrackableObject> trackedObjects = new ArrayList<TrackableObject>();

        for (TrackingTarget target : targets) {
            TrackableObject trackedObject = this.track(image, target);

            if (trackedObject != null) {
                trackedObjects.add(trackedObject);
            }
        }

        return trackedObjects;
    }
}
