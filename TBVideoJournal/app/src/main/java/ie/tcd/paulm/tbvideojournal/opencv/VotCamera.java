package ie.tcd.paulm.tbvideojournal.opencv;



import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jcodec.common.io.FileChannelWrapper;
import org.opencv.android.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.auth.Auth;
import ie.tcd.paulm.tbvideojournal.firestore.FSVotVideoRef;
import ie.tcd.paulm.tbvideojournal.misc.Misc;
import ie.tcd.paulm.tbvideojournal.steps.PillIntakeSteps;

import static android.app.Activity.RESULT_OK;

public class VotCamera extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String  TAG              = "FaceCamera";

    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier cascadeClassifier;
    private Mat grayscaleImage;
    private Mat colorImage;
    private Mat rotatedColorImage;
    private Mat grayscaleImageRot;
    private Mat mRgba;
    private Mat mRgbaF;
    private Mat mRgbaT;
    private FaceDetector faceDetector;
    private FileChannelWrapper out;
    private Boolean finished;
    private FFmpeg ffmpeg;
    private File dir;
    private int frameCount;
    // FPS Debugging.
    private int mFPS;
    private long startTime;
    private long currentTime;
    private TextView fpsTextView;
    private static final String VOT_DIR = "/tb-vot/";
    private static final String VOT_FRAME_PREFIX = "tb-bitmap-frame-";
    private static final  String VOT_VIDEO_FILENAME = "latest-vot";
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
    private int VIDEO_BITRATE = 512 * 1000;// 40000;




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
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        startTime = 0;
        currentTime = 1000;
        fpsTextView = (TextView) view.findViewById(R.id.fps_tv);

        PillIntakeSteps steps = new PillIntakeSteps((MainActivity)getActivity(), (RelativeLayout)view);
        File root = Environment.getExternalStorageDirectory();

        // TODO(paulmolloy) write to apps internal dir for more privacy.
        // Setup external /downloads dir for writing to.
        if (!isExternalStorageWritable()) {
            Log.e(TAG, "Failed to get External Storage");
        }
        File dir = new File (root.getAbsolutePath() + VOT_DIR);
        Log.d(TAG, "Full file path: " + dir.getAbsolutePath());
        dir.mkdirs();
        frameCount = 0;
        finished = false;


        steps.onAllPillsTaken(() -> {
            Misc.toast("All pills taken! (Will add UI for this in a bit)", getContext());
            finished = true;
            ffmpeg =  FFmpeg.getInstance(getContext());
            loadFFMpegBinary();
            // Magic command for turning the VOT images into an mp4.
            String[] videoCommand = new String[]{"-i", root.getAbsolutePath() + VOT_DIR
                    + VOT_FRAME_PREFIX + "%01d.jpg" + "", "-c:v", "libx264", "-c:a", "aac", "-vf",
                    "setpts=2*PTS, transpose=2", "-pix_fmt", "yuv420p", "-crf", "10", "-r", "15",
                    "-shortest", "-y", root.getAbsolutePath() + VOT_DIR + VOT_VIDEO_FILENAME + ".mp4"};
             execFFmpegBinary(videoCommand);
             stopRecording();
             recording = false;
            saveVotFirebase(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + VOT_DIR + VOT_SCREEN_RECORD_VIDEO_FILENAME + ".mp4");

        });
        steps.onStepChanged((step, pill) -> {
            // Misc.toast("Now on pill " + pill + ", step " + step, getContext(), true);
            // TODO(paulmolloy): Record Timestamps here.
            if( !recording) {
                prepareRecording();
                startRecording();
                recording = true;
            }

        });

        mDisplayMetrics= new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager) getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);


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
        faceDetector = new FaceDetector(getContext(), height);
    }

    @Override
    public void onCameraViewStopped() {
            // colorImage.release();
            // rotatedColorImage.release();
            releaseMediaRecorder();
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
        colorImage = faceDetector.process(colorImage, grayscaleImageRot);

            // Un-comment this to enable the Ffmpeg video saving again.
//        // Save the frame to be used for the video
//        if( !finished) {
//            saveTempBitmapFrame(matToBitmap(colorImage), frameCount);
//            Log.d(TAG, "Bitmap added to list");
//            frameCount ++;
//        }


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


    // FFMpeg Related stuff below


    // matToBitmap converts an image Matrix to a Bitmap image.
    private Bitmap matToBitmap(Mat mat){
        Bitmap bitmap = Bitmap.createBitmap(mOpenCvCameraView.getWidth()/4,mOpenCvCameraView.getHeight()/4, Bitmap.Config.ARGB_8888);
        try {
            bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bitmap);

        }catch(Exception e){
            Log.e(TAG, "Exception converting mat to bitmap: " + e);
        }
        return bitmap;
    }

    // loadFFMpegBinary sets up Ffmpeg for doing commands.
    private void loadFFMpegBinary() {
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {

                }
            });
        } catch (FFmpegNotSupportedException e) {
            Log.e(TAG, "An exception happended setting up ffmpeg: " + e);
        }
    }

    // execFFmpegBinary runs Ffppeg commands tokenized in a string array.
    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "FAILED with output : "+s);
                }

                @Override
                public void onSuccess(String s) {
                    Log.d(TAG, "SUCCESS with output : "+s);
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Progress command : ffmpeg " + Arrays.toString(command));
                    Log.d(TAG, "progress : "+s);
                }

                @Override
                public void onStart() {

                    Log.d(TAG, "Started command : ffmpeg " + Arrays.toString(command));

                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg ");
                    // TODO(paulmolloy): Figure out why it doesn't seem to be finished yet here.
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG, "Running ffmpeg: " + e);
        }
    }

    // Saves a bitmap image to the applications dir with a given filename.
    private void saveImage(Bitmap finalBitmap, String fileName) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + VOT_DIR);
        myDir.mkdirs();
        String fname = fileName +".jpg";

        File file = new File(myDir, fname);
        if (file.exists()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            Log.d(TAG, "Saved frame to: " + file.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Failed to save frame: " + e);
            e.printStackTrace();

        }
    }

    // Saves a bitmap frame of the VOT using the frame number for file name.
    public void saveTempBitmapFrame(Bitmap bitmap, int frameNum ) {
        if (isExternalStorageWritable()) {
            saveImage(bitmap, VOT_FRAME_PREFIX + frameNum);
            Log.d(TAG, "Saved bitmap " + frameNum);
        }else{
            Log.w(TAG, "The external storage isn't writable, aborting saving frame.");
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
            Log.d(TAG, "Screen Cast Permission Denied");

            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
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

    private void saveVotFirebase(String videoFilePath){
        // Save video to firebase
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        // TODO(paulmolloy): Save to per user dirs.
        StorageReference videoRef = storageRef.child(FIREBASE_VOT_DIR + Auth.getCurrentUserID() + FIREBASE_FILENAME + genCurDateString() + ".mp4");

        Uri file = Uri.fromFile(new File(videoFilePath)); //Declare your url here.
        UploadTask uploadTask = videoRef.putFile(file);
        Log.d(TAG, "Uploading to : " + videoRef.getPath());

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
                Log.d(TAG, "Uploaded your vot successfully" );
                Misc.toast("Uploaded your vot successfully", getContext());
                Log.d(TAG, "Upload finished location: " + videoRef.getPath());
                // TODO(paulmolloy): Save reference to it in Firestore
                FSVotVideoRef.addVideoReference(videoRef.getPath(), "TB Vot on " + genCurDateString());





            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG, "Upload is " + progress + "% done");
            }
        });


    }

}