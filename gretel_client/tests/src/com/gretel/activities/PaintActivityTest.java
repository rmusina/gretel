package com.gretel.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;

import com.google.gson.Gson;
import com.gretel.GretelSettings;
import com.gretel.serializers.MatOfKeyPointSerializer;
import com.gretel.serializers.MatSerializer;
import com.gretel.services.controllers.AbstractRequestIntentService;
import com.gretel.services.controllers.JsonPostRequestIntentService;
import com.gretel.services.controllers.ServiceResultReceiver;
import com.gretel.services.pojo.KeyPointPojo;
import com.gretel.services.pojo.TrackableImagePojo;
import com.gretel.tests.R;
import com.gretel.trackers.FeatureTracker;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public class PaintActivityTest extends ActivityInstrumentationTestCase2<PaintActivity> {

    private MatOfKeyPoint features;

    private Mat featuresDescription;

    public PaintActivityTest() {
        super(PaintActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        FeatureTracker tracker = new FeatureTracker();
        Mat image = Utils.loadResource(GretelTestRunner.context, R.raw.image);

        this.features = tracker.detectFeatures(image);
        this.featuresDescription = tracker.describeFeatures(image, features);
    }

    public void testRESTApi() {
        Gson gson = new Gson();
        TrackableImagePojo imagePojo = new TrackableImagePojo();

        imagePojo.setLat(42.1);
        imagePojo.setLon(23.1);

        MatOfKeyPointSerializer keyPointSerializer = new MatOfKeyPointSerializer();
        KeyPointPojo[] keyPoints = keyPointSerializer.serialize(this.features);
        imagePojo.setImageFeatures(keyPoints);

        MatSerializer matSerializer = new MatSerializer();
        String[][] serFeatures = matSerializer.serialize(this.featuresDescription);
        imagePojo.setImageDesc(serFeatures);

        String json = gson.toJson(imagePojo);

        Context context = GretelTestRunner.context;
        final Intent intent = new Intent(context, JsonPostRequestIntentService.class);

        intent.putExtra(JsonPostRequestIntentService.RESULT_RECEIVER, new ServiceResultReceiver(new Handler()));
        intent.putExtra(JsonPostRequestIntentService.REQUEST_URL, GretelSettings.SERVICE_URL);
        intent.putExtra(JsonPostRequestIntentService.SERIALIZED_DATA, json);

        AbstractRequestIntentService intentService = new JsonPostRequestIntentService();
        intentService.onHandleIntent(intent);
    }
}
