package com.gretel.serializers;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class MatSerializer {
    public String[][] serialize(Mat mat) {
        String[][] serializedMat = new String[mat.rows()][mat.cols()];

        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                double[] row = mat.get(i, j);

                if (row.length == 1) {
                    serializedMat[i][j] = Long.toBinaryString(Double.doubleToRawLongBits(row[0]));
                }
            }
        }

        return serializedMat;
    }

    public Mat deserialize(String[][] mat) {
        if (mat == null || mat.length == 0) {
            return new Mat(0, 0, CvType.CV_8UC1);
        }

        int n = mat.length;
        int m = mat[0].length;

        Mat deserializedMat = new Mat(n, m, CvType.CV_8UC1);

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                /*for (int c = 0; c < 8; c++) {
                    byte[] entry = new byte[1];
                    entry[0] = new Byte(mat[i][j]);
                    deserializedMat.put(i, j, entry);
                }*/
            }
        }

        return deserializedMat;
    }
}
