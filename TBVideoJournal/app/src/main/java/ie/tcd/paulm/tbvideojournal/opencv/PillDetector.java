package ie.tcd.paulm.tbvideojournal.opencv;


import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ie.tcd.paulm.tbvideojournal.R;

public class PillDetector {

    private static final String  TAG              = "HandCamera";

    private static int absoluteFaceSize;
    private boolean pillDetected = false;
    private Scalar minHSV;
    private Scalar maxHSV;
    private Point lastPillCenter;
    private int height, width;
    private Date lastTouch, currentTime;
    private Point detectCenter;
    private int pillCount = 0;
    private int atMouthCount = 0;
    private boolean isSwallowed = false;



    public PillDetector(Context context, int height,  int width){
        // The faces will be ~ 20% of the height of the screen.
        this.height = height;
        this.width = width;
        absoluteFaceSize = (int) (height * 0.2);
        try {

        } catch (Exception e) {
            Log.e(TAG, "Error loading cascade", e);
        }
        minHSV = new Scalar(3);
        maxHSV = new Scalar(3);
        lastPillCenter = new Point(-1, -1);
        detectCenter = new Point(width/2, 2*height/3);


    }
    public void setPill() {

    }
    public Mat process(Rect[] facesArray, Mat colorImage) {
        Log.d(TAG, "processing pill detection");
        Log.d(TAG, "Step pill detected: " + pillDetected);
        if (pillDetected) {
            // clone frame because original frame needed for display
            Mat frame = colorImage.clone();

            // remove noise and convert to binary in HSV range determined by user input
            Imgproc.GaussianBlur(frame, frame, new Size(9, 9), 5);
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2HSV_FULL);
            Core.inRange(frame, minHSV, maxHSV, frame);

            List<MatOfPoint> contours =  getAllContours(frame);
            // Uncomment to display all contours.
//            for(MatOfPoint c : contours) {
//                Rect rect = Imgproc.boundingRect(c);
//                Imgproc.rectangle(colorImage, rect.br(), rect.tl(), new Scalar(0, 255, 255, 255), 3);
//            }
            int indexOfPillContour = getNearestPillContour(contours);

            //
            if(indexOfPillContour < 0) {
                Log.d(TAG, "No pill found in frame");
                pillCount = 0;
                if (atMouthCount > 2) {
                    // If there was no pill found and the pill was within the mouth the last two
                    // frames it was probably swallowed.
                    isSwallowed = true;
                    Log.d(TAG, "Step set swallowed: " + isSwallowed);

                    atMouthCount = 0;

                }
            }
            // Pill identified
            else{
                pillCount++;
                if(isSwallowed && pillCount > 2 ) {
                    isSwallowed = false;
                    Log.d(TAG, "Step set swallowed: " + isSwallowed);
                }
                Rect rect = Imgproc.boundingRect(contours.get(indexOfPillContour));
                lastPillCenter.x = rect.x + rect.width/2;
                lastPillCenter.y = rect.y + rect.height/2;
                Imgproc.rectangle(colorImage, rect.br(), rect.tl(), new Scalar(255, 0, 0, 255), 3);
                boolean atMouth = false;
                // if the pill is over a mouth add to count otherwise reset count;
                for(Rect face : facesArray) {
                    if(!atMouth && face.contains(lastPillCenter)){
                        atMouthCount++;
                        atMouth = true;
                    }
                }
                // if pill isn't at the mouth then reset the count of frames where it is at the
                // mouth.
                if(atMouth==false){
                    atMouthCount=0;
                }
            }
        }else {
            int detectRadius = this.width/8;
            int thickness = 5;
            // Draw pill start guide.
            Log.d(TAG, "Step circle:" + detectCenter.x + " " + detectCenter.y);
            Imgproc.circle(colorImage, detectCenter, detectRadius, new Scalar(255, 255, 255, 255), thickness);
        }
        return colorImage;
    }

    public boolean isSwallowed() {
        return isSwallowed;
    }

    public int getConfidence() {
        if (atMouthCount > 0 ) return 1;

        return 0;
    }

    // getNearestPillContour returns the index of the pill that was nearest to the last pill seen.
    protected int getNearestPillContour(List<MatOfPoint> contours){

        Rect rect;
        int indexOfMaxContour = -1;
        Log.d(TAG, "Pill distances:");
        double nearest = Double.MAX_VALUE;
        int nearestIndex = -1;
        for (int i = 0; i < contours.size(); i++) {

            rect = Imgproc.boundingRect(contours.get(i));
            int xMid = rect.x + rect.width/2;
            int yMid = rect.y + rect.height/2;
            double dist = euclidianDist(new Point(xMid, yMid), lastPillCenter);
            if(dist <= nearest) nearestIndex = i;
            Log.d(TAG, i + "Pill distances: " + dist );
            if(rect.contains(lastPillCenter))
                return i;
        }
        return indexOfMaxContour;
    }

    private double euclidianDist(Point a, Point b) {
        Double xDiff = a.x - b.x;
        Double yDiff = a.y - b.y;
        return Math.sqrt(Math.pow(xDiff,2) + Math.pow(yDiff, 2));


    }


    // getAllContours gets all contours in the image frame.
    protected List<MatOfPoint> getAllContours(Mat frame){
        Mat tmpFrame = frame.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Log.d(TAG, "all_contours channels" + tmpFrame.channels());
        Log.d(TAG, "all contours " + tmpFrame.toString());
        Imgproc.findContours(tmpFrame, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        return contours;
    }


    // getAvgHSV gets the a average HSV color of the pill.
    protected void getAvgHSV(Mat frame){

        // consider square patch around touched pixel
        int x = (int) lastPillCenter.x;
        int y = (int) lastPillCenter.y;
        int rows = frame.rows();
        int cols = frame.cols();

        Rect touchedSquare = new Rect();
        int squareSide = 5;

        touchedSquare.x = (x > squareSide) ? x - squareSide : 0;
        touchedSquare.y = (y > squareSide) ? y - squareSide : 0;

        touchedSquare.width = (x + squareSide < cols) ?
                x + squareSide - touchedSquare.x : cols - touchedSquare.x;
        touchedSquare.height = (y + squareSide < rows) ?
                y + squareSide - touchedSquare.y : rows - touchedSquare.y;

        Mat touchedMat = frame.submat(touchedSquare);

        // convert patch to HSV and get average values
        Log.d(TAG, "Cur channels" + touchedMat.channels());
        Imgproc.cvtColor(touchedMat, touchedMat, Imgproc.COLOR_RGB2HSV_FULL);

        Scalar sumHSV = Core.sumElems(touchedMat);
        int total = touchedSquare.width * touchedSquare.height;
        double avgHSV[] = {sumHSV.val[0] / total, sumHSV.val[1] / total, sumHSV.val[2] / total};
        assignHSV(avgHSV);
    }

    // assignHSV sets the range of HSV color values to look for to find pills.
    // used in hand detection code I was looking at.
    protected void assignHSV(double avgHSV[]){
        minHSV.val[0] = (avgHSV[0] > 10) ? avgHSV[0] - 10 : 0;
        maxHSV.val[0] = (avgHSV[0] < 245) ? avgHSV[0] + 10 : 255;

        minHSV.val[1] = (avgHSV[1] > 130) ? avgHSV[1] - 100 : 30;
        maxHSV.val[1] = (avgHSV[1] < 155) ? avgHSV[1] + 100 : 255;

        minHSV.val[2] = (avgHSV[2] > 130) ? avgHSV[2] - 100 : 30;
        maxHSV.val[2] = (avgHSV[2] < 155) ? avgHSV[2] + 100 : 255;

        Log.e("HSV", avgHSV[0]+", "+avgHSV[1]+", "+avgHSV[2]);
        Log.e("HSV", minHSV.val[0]+", "+minHSV.val[1]+", "+minHSV.val[2]);
        Log.e("HSV", maxHSV.val[0]+", "+maxHSV.val[1]+", "+maxHSV.val[2]);
    }





    public void resetPillDetector(){

        pillDetected = false;
        lastPillCenter = new Point(-1,-1);
        pillCount = 0;
        atMouthCount = 0;
        isSwallowed = false;
    }

    // updatePillColorSample recalibrates the system to use colors at event as the colors to
    // look for.
    public boolean updatePillColorSample(Mat colorImage) {

                lastTouch = currentTime;
                Log.d(TAG, "Sampled pill");

                // clone and blur touched frame
                Mat frame = colorImage.clone();
                Mat frameOut = new Mat();
                Imgproc.GaussianBlur(frame, frameOut, new Size(9, 9), 5);


                // set pill center point and average HSV value
                lastPillCenter.x = detectCenter.x;
                lastPillCenter.y = detectCenter.y;
                getAvgHSV(frameOut);

                pillDetected = true;

        return true;
    }

}

