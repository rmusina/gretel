package com.gretel.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.AndroidTestRunner;
import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;
import android.util.Log;

import com.gretel.imageprocessors.BitmapStorage;
import com.gretel.tests.R;

import junit.framework.TestSuite;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;

public class GretelTestRunner extends InstrumentationTestRunner {
    private static final long MANAGER_TIMEOUT = 3000;

    public static Context context;

    private AndroidTestRunner androidTestRunner;

    private static String TAG = "gretel_test";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log("OpenCV loaded successfully");
                    synchronized (this) {
                        notify();
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    static public void Log(String message) {
        Log.e(TAG, message);
    }

    @Override
    public TestSuite getTestSuite() {
        InstrumentationTestSuite suite = new InstrumentationTestSuite(this);

        suite.addTestSuite(PaintActivityTest.class);
        return suite;
    }

    @Override
    public void onStart() {
        if (!OpenCVLoader.initDebug()) {
            Log("Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, getContext(), mLoaderCallback);

            synchronized (this) {
                try {
                    wait(MANAGER_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log("OpenCV library found inside test package. Using it!");
        }

        context = getContext();

        super.onStart();
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        androidTestRunner = super.getAndroidTestRunner();
        return androidTestRunner;
    }
}
