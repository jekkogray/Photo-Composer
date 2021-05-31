package com.example.picturecomposer.util;

import android.graphics.Bitmap;


import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;

import java.util.ArrayList;
import java.util.List;

public class ExpHist {
    public static int run(Bitmap temp) {
        // convert bitmap to mat
        Mat src = new Mat();
        Bitmap bmp32 = temp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, src);

        List<Mat> bgrPlanes = new ArrayList<>();
        Core.split(src, bgrPlanes);

        int histSize = 256;

        float[] range = {0, 256}; //the upper boundary is exclusive
        MatOfFloat histRange = new MatOfFloat(range);

        boolean accumulate = false;

        Mat bHist = new Mat(), gHist = new Mat(), rHist = new Mat();
        Imgproc.calcHist(bgrPlanes, new MatOfInt(0), new Mat(), bHist, new MatOfInt(histSize), histRange, accumulate);
        Imgproc.calcHist(bgrPlanes, new MatOfInt(1), new Mat(), gHist, new MatOfInt(histSize), histRange, accumulate);
        Imgproc.calcHist(bgrPlanes, new MatOfInt(2), new Mat(), rHist, new MatOfInt(histSize), histRange, accumulate);

        float[] bHistData = new float[(int) (bHist.total() * bHist.channels())];
        bHist.get(0, 0, bHistData);
        float[] gHistData = new float[(int) (gHist.total() * gHist.channels())];
        gHist.get(0, 0, gHistData);
        float[] rHistData = new float[(int) (rHist.total() * rHist.channels())];
        rHist.get(0, 0, rHistData);

        // Get num vals at each intensity and sum total vals
        float totalVals = 0;
        float[] histData = new float[256];
        for(int i = 0; i < histSize; i++) {
            histData[i] = bHistData[i] + gHistData[i] + rHistData[i];
            totalVals = totalVals + histData[i];
        }

        // Get sum of all values * intensities
        float sum = 0;
        for(int i = 0; i < histSize; i++) {
            sum = sum + (histData[i] * i);
        }

        // Divide by total value to find average intensity
        float avg = sum / totalVals;

        // Check what range avg is in
        if(avg < 51) {
            return 1;
        } else if (avg < 102) {
            return 3;
        } else if (avg < 153) {
            return 5;
        } else if (avg < 205) {
            return 4;
        } else if (avg < 256) {
            return 2;
        } else {
            return 0;
        }
    }
}