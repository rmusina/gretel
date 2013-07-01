package com.gretel.serializers;

import com.gretel.services.pojo.KeyPointPojo;

import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.KeyPoint;

public class MatOfKeyPointSerializer {

    public KeyPointPojo[] serialize(MatOfKeyPoint matOfKeyPoint) {
        KeyPoint[] keyPoints = matOfKeyPoint.toArray();
        KeyPointPojo[] serializedKeyPoints = new KeyPointPojo[keyPoints.length];
        int i = 0;

        for (KeyPoint keyPoint : keyPoints) {
            serializedKeyPoints[i++] = new KeyPointPojo(keyPoint.pt.x, keyPoint.pt.y, keyPoint.size);
        }

        return serializedKeyPoints;
    }

    public MatOfKeyPoint deserialize(KeyPointPojo[] mat) {
        return null;
    }
}
