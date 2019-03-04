package ie.tcd.paulm.tbvideojournal.opencv;


import android.content.Context;

import android.util.Log;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
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
        absoluteFaceSize = (int) (height * 0.2);
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
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
            cascadeClassifier.detectMultiScale(grayscaleImageRot, faces, 1.5, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }


        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i <facesArray.length; i++) {
            Rect recRot = new Rect(facesArray[i].width- facesArray[i].y,facesArray[i].x,
                    facesArray[i].height, facesArray[i].width);
            Imgproc.rectangle(colorImage, recRot.br(), recRot.tl(), new Scalar(0, 255, 0, 255), 3);

        }

        return colorImage;
    }


}

