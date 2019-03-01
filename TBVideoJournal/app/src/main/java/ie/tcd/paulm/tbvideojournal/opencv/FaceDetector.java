package ie.tcd.paulm.tbvideojournal.opencv;



import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.misc.Misc;
import ie.tcd.paulm.tbvideojournal.steps.PillIntakeSteps;

import static android.support.constraint.Constraints.TAG;

public class FaceDetector extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String  TAG              = "FaceCamera";

    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier cascadeClassifier;
    private Mat grayscaleImage;
    private Mat grayscaleImageRot;
    private Mat colorImage;
    private Mat rotatedColorImage;
    private int absoluteFaceSize;
    private Mat mRgba;
    private Mat mRgbaF;
    private Mat mRgbaT;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void initializeOpenCVDependencies() {

        try {
            // Copy the resource into a temp file so OpenCV can load it

            InputStream is = getResources().openRawResource(ie.tcd.paulm.tbvideojournal.R.raw.lbpcascade_frontalface);
            File cascadeDir = getContext().getDir("cascade", Context.MODE_PRIVATE);
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
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

        // And we are ready to go
        mOpenCvCameraView.setCameraIndex(1);
        mOpenCvCameraView.enableView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        Log.i(TAG, "called onCreateView");

//        getActivity().requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = view.findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        PillIntakeSteps steps = new PillIntakeSteps((MainActivity)getActivity(), (RelativeLayout)view);

        steps.onAllPillsTaken(() -> Misc.toast("All pills taken! (Will add UI for this in a bit)", getContext()));
        steps.onStepChanged((step, pill) -> Misc.toast("Now on pill " + pill + ", step " + step, getContext(), true));

        return view;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        rotatedColorImage = new Mat(width, width, CvType.CV_8UC4);
        grayscaleImageRot= new Mat(width, width, CvType.CV_8UC4);




        // The faces will be a 20% of the height of the screen
        absoluteFaceSize = (int) (height * 0.2);
    }

    @Override
    public void onCameraViewStopped() {
            // colorImage.release();
            // rotatedColorImage.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        colorImage = inputFrame.rgba();
        grayscaleImage = inputFrame.gray();



        MatOfRect faces = new MatOfRect();
        // Model is trained on 90 sideways photos so need to rotate image back and forth after
        // to classify.
        // Rotate mRgba 90 degrees
        Core.transpose(grayscaleImage, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, grayscaleImageRot,  0 ); // both vertical top bottom
        // Use the classifier to detect faces
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

            //Imgproc.rectangle(colorImage, facesArray[i].br(), facesArray[i].tl(), new Scalar(0, 255, 0, 255), 3);

        }
        return colorImage;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, getContext(), mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


}