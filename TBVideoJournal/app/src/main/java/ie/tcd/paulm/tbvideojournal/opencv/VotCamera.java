package ie.tcd.paulm.tbvideojournal.opencv;



import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.opencv.android.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.File;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.auth.Auth;
import ie.tcd.paulm.tbvideojournal.firestore.FSVotVideoRef;
import ie.tcd.paulm.tbvideojournal.misc.Misc;
import ie.tcd.paulm.tbvideojournal.steps.PillIntakeGuide;
import kotlin.Unit;

import static android.app.Activity.RESULT_OK;

public class VotCamera extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String  TAG              = "FaceCamera";

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat grayscaleImage;
    private Mat colorImage;
    private Mat grayscaleImageRot;
    private Mat mRgbaF;
    private Mat mRgbaT;
    private FaceDetector faceDetector;
    private PillDetector pillDetector;
    // FPS Debugging.
    private int mFPS;
    private long startTime;
    private long currentTime;
    private TextView fpsTextView;
    private static final String VOT_DIR = "/tb-vot/";
    private static final String FIREBASE_VOT_DIR = "vot-videos/";

    private static final String FIREBASE_FILENAME = "-vot-";

    // Screen capture stuff.
    private static final String VOT_SCREEN_RECORD_VIDEO_FILENAME = "screen_record_latest";
    private static final int CAST_PERMISSION_CODE = 22;
    private DisplayMetrics mDisplayMetrics;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private MediaProjectionManager mProjectionManager;
    private boolean recording = false;
    private int VIDEO_BITRATE = 512 * 2000;// 40000;
    private PillIntakeGuide guide;
    private Confidence confidence;
    private enum Step {EMPTY, FACE, PILL, SWALLOW}
    private Step currentStep;
    // Magic number scales to get rid of black bars without losing part of camera.
    private static final float scaleFactor = 1.30f;
    private boolean dontRecordDemo = false;





    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    // Initialize OpenCV
                    mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        Log.i(TAG, "called onCreateView");
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = view.findViewById(R.id.camera_surface_view);

        mOpenCvCameraView.setScaleX(scaleFactor);
        mOpenCvCameraView.setScaleY(scaleFactor);
//        mOpenCvCameraView.setMaxFrameSize(1000,2000);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        startTime = 0;
        currentTime = 1000;
        fpsTextView = (TextView) view.findViewById(R.id.fps_tv);
        fpsTextView.setVisibility(View.GONE);
        guide = new PillIntakeGuide((MainActivity)getActivity(), (RelativeLayout)view);
        File root = Environment.getExternalStorageDirectory();

        // TODO(paulmolloy) write to apps internal dir for more privacy.
        // Setup external /downloads dir for writing to.Confidence
        if (!isExternalStorageWritable()) {
            Log.e(TAG, "Failed to get External Storage");
        }
        File dir = new File (root.getAbsolutePath() + VOT_DIR);
        Log.d(TAG, "Full file path: " + dir.getAbsolutePath());
        dir.mkdirs();

        guide.onStepStarted((pill, step) -> {
            if( !recording && !dontRecordDemo) {
                prepareRecording();
                startRecording();
                recording = true;
            }
            Log.d(TAG, "Step no:" + step);
            switch (step){
                case 0:
                    currentStep = Step.PILL;
                    pillDetector.resetPillDetector();
                    Log.d(TAG, "Step started pill");
                    break;
                case 1:
                    currentStep = Step.SWALLOW;
                    Log.d(TAG, "Step started swallow");

                    break;
                case 2:
                    currentStep = Step.EMPTY;
                    Log.d(TAG, "Step started Empty");

                    break;
                case 3:
                default:
                    currentStep = Step.FACE;
                    Log.d(TAG, "Step started face");

            }
            return Unit.INSTANCE;
        });
        guide.onStepCompleted((pill, step) -> {
            float stepConfidence = confidence.getConfidence();
            Log.d(TAG, "Step confidence " + step + ": " + stepConfidence );
            if (currentStep == Step.PILL) pillDetector.updatePillColorSample(colorImage);
            confidence.reset();

            return stepConfidence;
        });
        guide.onAllPillsTaken(timestampsAndConfidences -> {
            if(recording && !dontRecordDemo) {
                stopRecording();
                recording = false;
                String videoSavePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + VOT_DIR + VOT_SCREEN_RECORD_VIDEO_FILENAME + ".mp4";
                saveVotFirebase(videoSavePath, timestampsAndConfidences);
            }
            return Unit.INSTANCE;
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    Log.d(TAG, "update sample");
                    return pillDetector.updatePillColorSample(colorImage);
                }
                return true;
            }
        });



        mDisplayMetrics= new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager) getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        confidence = new Confidence();
        return view;
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        grayscaleImageRot= new Mat(width, width, CvType.CV_8UC4);
        faceDetector = new FaceDetector(getContext(), height, scaleFactor);
        pillDetector = new PillDetector(getContext(), height, width);
    }

    @Override
    public void onCameraViewStopped() {
        // colorImage.release();
        // rotatedColorImage.release();
        releaseMediaRecorder();
        File localFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + VOT_DIR + VOT_SCREEN_RECORD_VIDEO_FILENAME + ".mp4");
        // Delete the file if the user quits the app before it starts recording but after prepare.
        if(localFile.exists() && localFile.length()==0) {
            localFile.delete();
            Log.d(TAG, "Deleted empty file on early quit.");
        }

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        colorImage = inputFrame.rgba();
        Core.flip(colorImage, colorImage, 0); // Make mirror image
        grayscaleImage = inputFrame.gray();
        MatOfRect faces = new MatOfRect();
        Log.d(TAG, "Vid frame height: " + mOpenCvCameraView.getHeight() + " width: " + mOpenCvCameraView.getWidth());


        // Model is trained on 90 sideways photos so need to rotate image back and forth after
        // to classify.
        // Rotate mRgba 90 degrees
        Core.transpose(grayscaleImage, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, grayscaleImageRot,  0 ); // both vertical top bottom
        Rect[] facesArray = faceDetector.process(colorImage, grayscaleImageRot);


        // Use the classifier to detect faces
        if(currentStep == Step.PILL || currentStep == Step.SWALLOW)
            colorImage = pillDetector.process(facesArray, colorImage);
            confidence.addPillConfidence(pillDetector.getConfidence());
            confidence.setIsSwallowed(pillDetector.isSwallowed());
        if(currentStep != Step.PILL) {
            confidence.addFaceConfidence(faceDetector.getConfidence());
        }
        for(Rect face : facesArray)
            Imgproc.rectangle(colorImage, face.br(), face.tl(), new Scalar(0, 255, 0, 255), 3);


        // Keep track of what the FPS is.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentTime - startTime >= 1000) {
                    fpsTextView.setText("FPS: " + String.valueOf(mFPS));
                    mFPS = 0;
                    startTime = System.currentTimeMillis();
                }
                currentTime = System.currentTimeMillis();
                mFPS += 1;

            }
        });

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


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    // Screen Recording Stuff.

    public void releaseMediaRecorder() {
        Log.e("debug","releaseMediaRecorder");
        if (mMediaRecorder != null) {
            mMediaRecorder.reset(); // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
        }
    }


    private void startRecording() {
        // If mMediaProjection is null that means we didn't get a context, lets ask the user
        if (mMediaProjection == null) {
            // This asks for user permissions to capture the screen
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private void stopRecording() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
    }

    private void prepareRecording() {


        File root = Environment.getExternalStorageDirectory();

        // TODO(paulmolloy) write to apps internal dir for more privacy.
        // Setup external /downloads dir for writing to.
        File dir = new File (root.getAbsolutePath() + VOT_DIR);
        Log.d(TAG, "Full file path: " + dir.getAbsolutePath());
        dir.mkdirs();
        if (!isExternalStorageWritable()) {
            Log.e(TAG, "Failed to get External Storage");
            return;
        }
        String filePath;
        String videoName = (VOT_SCREEN_RECORD_VIDEO_FILENAME + ".mp4");
        filePath = dir+ File.separator + videoName;

        int width = mDisplayMetrics.widthPixels;
        int height = mDisplayMetrics.heightPixels;
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoSize(width, height);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setOutputFile(filePath);
        mMediaRecorder.setVideoEncodingBitRate(VIDEO_BITRATE);
        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed preparing: " + e);
            return;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CAST_PERMISSION_CODE) {
            // Where did we get this request from ? -_-
            Log.w(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Log.d(TAG, "Screen Cast Permission Denied ask again");
            Misc.toast("Screen Cast Denied, this is needed to record your VOT.", getContext());
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
        guide.recordingNow();
    }

    private VirtualDisplay createVirtualDisplay() {
        int screenDensity = mDisplayMetrics.densityDpi;
        int width = mDisplayMetrics.widthPixels;
        int height = mDisplayMetrics.heightPixels;
        return mMediaProjection.createVirtualDisplay(this.getClass().getSimpleName(),
                width, height, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null, null);
    }

    private String genCurDateString(){
        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);

        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        return df.format(c);

    }

    // Firebase video stuff

    private void saveVotFirebase(String videoFilePath, Object timestamps){
        // Save video to firebase
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference videoRef = storageRef.child(FIREBASE_VOT_DIR + Auth.getCurrentUserID() + FIREBASE_FILENAME + genCurDateString() + ".mp4");
        File lastVotFile = new File(videoFilePath);
        Uri lastVotUri = Uri.fromFile(lastVotFile); //Declare your url here.
        videoRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                boolean isOverwrite;
                if(task.isSuccessful()){
                    // Got the download URL for /me/profile.png'
                    // File Exists user has already uploaded a vot today, will overwrite.
                    // No need to add a reference as one already exists.
                    isOverwrite = true;
                }else{

                    // File doesn't exist: This is the users first vot attempt today
                    // Video reference will need to be created.
                    isOverwrite = false;
                }

                UploadTask uploadTask = videoRef.putFile(lastVotUri);
                Log.d(TAG, "Uploading to : " + videoRef.getPath());

                // Register observers to listen for when the download is done or if it fails
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.d(TAG, "Video upload failed: " + exception);
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                        guide.setUploadProgress(100);
                        Log.d(TAG, "Uploaded your vot successfully" );

                        Misc.toast("Uploaded your vot successfully", MainActivity.context);
                        Log.d(TAG, "Upload finished location: " + videoRef.getPath());
                        Log.d(TAG, "isOverwrite: " + isOverwrite);

                        // if(!isOverwrite) FSVotVideoRef.addVideoReference(videoRef.getPath(), "TB Vot on " + genCurDateString(), timestamps);
                        //    ↑   Slightly changing this so that instead today's document is overwritten every time
                        //        because the time stamps and confidences will change for every video
                        String date = genCurDateString();
                        FSVotVideoRef.addVideoReference(date, videoRef.getPath(), "TB Vot on " + date, timestamps);

                        String fileName = videoRef.getName();
                        File localFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                                + VOT_DIR + fileName);
                        // Copy file locally to final location from its temp location to avoid
                        // having to fetch it from Firebase using up bandwidth.
                        try {
                            Misc.copyFile(lastVotFile, localFile);
                        } catch(IOException e) {
                            Log.e(TAG, "Failed to copy latest vot locally" + e);
                        }
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.d(TAG, "Upload is " + progress + "% done");
                        guide.setUploadProgress((int)Math.round(progress));
                    }
                });

            }
        }
        );



    }

}