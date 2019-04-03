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

    private MatOfRect faces;
    private static CascadeClassifier cascadeClassifier;
    private static int absoluteFaceSize;
    private float offsetFactX, offsetFactY;
    private float scaleFactX, scaleFactY;
    private boolean pillDetected = false;
    private Scalar minHSV;
    private Scalar maxHSV;
    private Point lastPillCenter;
    private Date currentTime;
    private Date lastTouch;



    public PillDetector(Context context, int height){
        // The faces will be ~ 20% of the height of the screen.
        absoluteFaceSize = (int) (height * 0.2);
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = context.getResources().openRawResource(R.raw.ok);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "ok.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error loading cascade", e);
        }
        minHSV = new Scalar(3);
        maxHSV = new Scalar(3);
        lastPillCenter = new Point(-1, -1);

    }
    public Mat process(Mat colorImage) {

        if (pillDetected) {
            // clone frame because original frame needed for display
            Mat frame = colorImage.clone();

            // remove noise and convert to binary in HSV range determined by user input
            Imgproc.GaussianBlur(frame, frame, new Size(9, 9), 5);
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2HSV_FULL);
            Core.inRange(frame, minHSV, maxHSV, frame);

            List<MatOfPoint> contours =  getAllContours(frame);
            for(MatOfPoint c : contours) {
                Rect rect = Imgproc.boundingRect(c);
                Imgproc.rectangle(colorImage, rect.br(), rect.tl(), new Scalar(0, 255, 255, 255), 3);
            }
            int indexOfPillContour = getNearestPillContour(contours);

            if(indexOfPillContour < 0)
                Log.d(TAG, "No pill found in frame");
            else{
                Rect rect = Imgproc.boundingRect(contours.get(indexOfPillContour));
                lastPillCenter.x = rect.x + rect.width/2;
                lastPillCenter.y = rect.y + rect.height/2;
                Imgproc.rectangle(colorImage, rect.br(), rect.tl(), new Scalar(0, 255, 0, 255), 3);
            }
        }
        return colorImage;
    }

    public int getConfidence() {
        if (faces.elemSize() > 0 ) return 1;
        return 0;
    }

    // getNearestPillContour returns the index of the pill that was nearest to the last pill seen.
    protected int getNearestPillContour(List<MatOfPoint> contours){

        Rect rect;
        int indexOfMaxContour = -1;
        for (int i = 0; i < contours.size(); i++) {
            rect = Imgproc.boundingRect(contours.get(i));
            if(rect.contains(lastPillCenter))
                return i;
        }
        return indexOfMaxContour;
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



    // TODO(paulmolloy): All the code below not needed when pill detected from static location.

    // setScaleFactors scales the touch coords to opencv frame coords.
    public void setScaleFactors(int vidWidth, int vidHeight, float deviceWidth, float deviceHeight){
        if(deviceHeight - vidHeight < deviceWidth - vidWidth){
            float temp = vidWidth * deviceHeight / vidHeight;
            offsetFactY = 0;
            offsetFactX = (deviceWidth - temp) / 2;
            scaleFactY = vidHeight / deviceHeight;
            scaleFactX = vidWidth / temp;
        }
        else{
            float temp = vidHeight * deviceWidth / vidWidth;
            offsetFactX= 0;
            offsetFactY = (deviceHeight - temp) / 2;
            scaleFactX = vidWidth / deviceWidth;
            scaleFactY = vidHeight / temp;
        }
    }

    // updatePillColorSample recalibrates the system to use colors at event as the colors to
    // look for.
    public boolean updatePillColorSample(MotionEvent event, Mat colorImage) {
        Log.d(TAG, "Got touch");
        currentTime = Calendar.getInstance().getTime();
        // Only update color on touches at least 2 seconds apart to avoid multiple touch events
        // at once.
        if (lastTouch != null) Log.d(TAG, "touch Cur time: " + currentTime.getTime() + "last time: " + lastTouch.getTime());
        if(lastTouch == null || currentTime.getTime() > lastTouch.getTime() + 200){ // .2 second cooldown before re-samples
            if(true) {
                lastTouch = currentTime;
                Log.d(TAG, "Set last touch");
                Log.d(TAG, "Channels before ycc: " + colorImage.channels());

                // clone and blur touched frame
                Mat frame = colorImage.clone();
                Log.d(TAG, "Channels before fClone: " + frame.channels());
                Mat frameOut = new Mat();
                Imgproc.GaussianBlur(frame, frameOut, new Size(9, 9), 5);
                Log.d(TAG, "Channels blur Clone: " + frameOut.channels());
                Log.d(TAG, "Channels blur: " + frame.channels());

                // calculate x, y coords because resolution is scaled on device display
                int x = Math.round((event.getX() - offsetFactX) * scaleFactX);
                int y = Math.round((event.getY() - offsetFactY) * scaleFactY);

                int rows = frameOut.rows();
                int cols = frameOut.cols();

                // return if touched point is outside camera resolution
                if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

                // set pill center point and average HSV value
                lastPillCenter.x = x;
                lastPillCenter.y = y;
                Log.d(TAG, "Channels before: " + frameOut.channels());
                getAvgHSV(frameOut);

                pillDetected = true;
            }
        }
        return true;
    }

}

