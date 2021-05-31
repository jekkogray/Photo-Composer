package com.example.picturecomposer.util;

import android.graphics.Bitmap;


import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import static java.lang.Math.abs;
import static java.util.Arrays.sort;

public class CalcSkew {
    public static double run(Bitmap temp) {
        // Declare variables
        double angle;
        Mat dst = new Mat();

        // Convert bitmap to mat
        Mat src = new Mat();
        Bitmap bmp32 = temp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, src);

        // Edge detection
        Imgproc.Canny(src, dst, 50, 200, 3, false);

        // Probabilistic Hough Line Transform
        Mat linesP = new Mat(); // will hold the results of the detection
        Imgproc.HoughLinesP(dst, linesP, 1, Math.PI / 180, 100, 100, 50); // runs the actual detection


        // Count num lines
        int count = linesP.rows();

        // if no lines are detected, return -1
        if (count == 0) {
            return -1;
        }

        double[] angles = new double[count];

        // Find line angles
        for (int x = 0; x < count; x++) {
            double[] l = linesP.get(x, 0);
            Point pt1 = new Point(l[0], l[1]);
            Point pt2 = new Point(l[2], l[3]);

            double deltaX = pt2.x - pt1.x;
            double deltaY = pt2.y - pt1.y;

            // Calculate the angle
            double temp_angle = Math.atan2(deltaY, deltaX) * (180 / Math.PI);

            // if vertical line
            if (abs(temp_angle) > 45) {
                // find degree of difference with vertical
                double temp_diff;
                if (temp_angle < 0) {
                    temp_diff = 90 + temp_angle;
                } else {
                    temp_diff = 90 - temp_angle;
                }

                angles[x] = temp_diff;
                // if horizontal line
            } else {
                angles[x] = temp_angle;
            }
        }
        angle = median(angles);
        return abs(angle);
    }

    public static double median(double[] vals) {
        // sort array
        sort(vals);

        int length = vals.length;

        // if odd, return middle number (median)
        if (length % 2 != 0) {
            return vals[length/2];
        }

        // if even, return avg of two middle numbers
        return (vals[(length - 1)/2] + vals[length/2])/2.0;
    }
}
