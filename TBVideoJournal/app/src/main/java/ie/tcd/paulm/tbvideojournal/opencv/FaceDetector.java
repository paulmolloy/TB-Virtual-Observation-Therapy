package ie.tcd.paulm.tbvideojournal.opencv;


import android.content.Context;

import android.util.Log;
import org.opencv.core.Mat;
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

import ie.tcd.paulm.tbvideojournal.R;

public class FaceDetector {

    private static final String  TAG              = "FaceCamera";

    private MatOfRect faces;
    private static CascadeClassifier cascadeClassifier;
    private static int absoluteFaceSize;



    public FaceDetector(Context context, int height){
        // The faces will be ~ 20% of the height of the screen.
        absoluteFaceSize = (int) (height * 0.3);
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            //File mCascadeFile = new File(cascadeDir, "haarcascade_frontalcatface_extended.xml");

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

    }
    public Mat process(Mat colorImage, Mat grayscaleImageRot) {
        // Use the classifier to detect faces
        faces = new MatOfRect();
        if (cascadeClassifier != null) {
            // Got to feed in a square image
            Log.d(TAG, "proccolorface: " + colorImage.width() + "h: " + colorImage.height());
            Log.d(TAG, "procgreyface: " + grayscaleImageRot.width() + "h: " + grayscaleImageRot.height());
            cascadeClassifier.detectMultiScale(grayscaleImageRot.submat(0,1080,0,1080), faces, 1.8, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size(absoluteFaceSize*2, absoluteFaceSize*2));// a different type of scale factor.
        }

        Log.d(TAG, "Face image size width:" + colorImage.width() + "Height: " + colorImage.height());

        Imgproc.rectangle(colorImage, new Point(0,0), new Point(1080,1080), new Scalar(200, 200, 200, 255), 3);
        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i <facesArray.length; i++) {
//            Imgproc.circle(colorImage, new Point(facesArray[i].x, facesArray[i].y), 1,  new Scalar(0, 255, 0, 255), 3);
//            Rect recRotScaled = new Rect(scalePosition(facesArray[i].width)- scalePosition(facesArray[i].y),facesArray[i].x,
//                    scalePosition(facesArray[i].height), scalePosition(facesArray[i].width));

            //Imgproc.rectangle(colorImage, recRotScaled.br(), recRotScaled.tl(), new Scalar(0, 255, 255, 255), 3);

            Rect recRot = new Rect(colorImage.height()-facesArray[i].y-facesArray[i].height,colorImage.width()-facesArray[i].x-facesArray[i].width,
                    facesArray[i].height, facesArray[i].width);
            Imgproc.rectangle(colorImage, recRot.br(), recRot.tl(), new Scalar(0, 255, 0, 255), 3);
            Log.d(TAG, "Face image x:" + facesArray[i].x + " y: " + facesArray[i].y);


        }


        return colorImage;
    }

    private int scalePosition(int position) {
        return (int) Math.round(position * 1.5);
    }

    public int getConfidence() {
        if (faces.elemSize() > 0 ) return 1;
        return 0;
    }


}

