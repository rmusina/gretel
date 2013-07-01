package com.gretel.services.controllers;

import java.util.List;

import com.gretel.trakers.objects.TrackableObject;

public interface ArtefactDownloadListener {
	void onReceiveResult(int resultCode, List<TrackableObject> trackableObjects);
}
