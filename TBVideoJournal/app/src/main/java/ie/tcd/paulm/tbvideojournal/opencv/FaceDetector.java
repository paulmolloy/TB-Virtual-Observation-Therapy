package ie.tcd.paulm.tbvideojournal.opencv;



import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.misc.Misc;
import ie.tcd.paulm.tbvideojournal.steps.PillIntakeSteps;

import static android.support.constraint.Constraints.TAG;
import static org.jcodec.common.model.ColorSpace.RGB;

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
    private FileChannelWrapper out;
    private File videoFile;
    private AndroidSequenceEncoder encoder;
    private Boolean finished;
    private List<Bitmap> bitmaps;
    private int frameNum;
    private FFmpeg ffmpeg;
    private File dir;
    private int frameCount;


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
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = view.findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        PillIntakeSteps steps = new PillIntakeSteps((MainActivity)getActivity(), (RelativeLayout)view);
        bitmaps = new ArrayList<Bitmap>();

        File root = Environment.getExternalStorageDirectory();

        // TODO(paulmolloy) write to apps internal dir.
        // Setup external /downloads dir for writing to.
        File dir = new File (root.getAbsolutePath() + "/download");
        Log.d(TAG, "Full file path: " + dir.getAbsolutePath());
        dir.mkdirs();
        frameCount = 0;


        steps.onAllPillsTaken(() -> {
            Misc.toast("All pills taken! (Will add UI for this in a bit)", getContext());
            finished = true;
            ffmpeg =  FFmpeg.getInstance(getContext());
            loadFFMpegBinary();
            String[] cmd = new String[]{"-version"};
            execFFmpegBinary(cmd);
            // Do video encoding asyncronously.
            // TODO(paulmolloy): Problems takes ages, video is sped up so timestamps won't work.
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    // Save frame to video.
                    try {
                        for(int i = 0; i < bitmaps.size(); i++) {
                            encoder.encodeImage(bitmaps.get(i));
                            Log.d(TAG, "Encoding frame " + i + " of " + bitmaps.size());

                        }
                        encoder.finish();
                        Log.d(TAG, "Finished encoding video");

                    }catch(Exception e ) {
                        Log.e(TAG, "Exception occured finishing encoding video:" + e);
                    } finally {
                        NIOUtils.closeQuietly(out);
                    }
                }
            });


        });
        steps.onStepChanged((step, pill) -> {
            Misc.toast("Now on pill " + pill + ", step " + step, getContext(), true);
            // TODO(paulmolloy): Record Timestamps here.

        });


        File file = new File(dir, "myData.txt");
        String[] fileText = new String[]{"Hi , How are you", "Hellokl"};
        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            for(String s : fileText) {
                pw.println(s);
            }
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Video File
        videoFile = new File(root.getAbsolutePath() + "/download");

        if (!videoFile.exists()){
            videoFile.mkdirs();
        }

        // Setup video encoder.
        try {
            Log.d(TAG, "Destination file: " + videoFile.getAbsolutePath() + "/test.mp4");
            out = NIOUtils.writableFileChannel(videoFile.getAbsolutePath() + "/test.mp4");
            encoder = new AndroidSequenceEncoder(out, Rational.R(5, 1));

            finished = false;
        }catch(Exception e ) {
            Log.e(TAG, "Exception occured setting up encoding video");
        }
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
        // Close video encoding.
        if(!finished) {
            try {
                encoder.finish();
            }catch(Exception e ) {
                Log.e(TAG, "Exception occured finishing encoding video when stopped: " + e);
            } finally {
                NIOUtils.closeQuietly(out);
            }
        }

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





        if( !finished) {
            Bitmap bitmap = matToBitmap(colorImage);
            saveTempBitmapFrame(bitmap, frameCount);
            bitmaps.add(bitmap);
            Log.d(TAG, "Bitmap added to list");
            frameCount ++;
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
                    Log.d(TAG, "Started command : ffmpeg " + Arrays.toString(command));
                    Log.d(TAG, "progress : "+s);
                }

                @Override
                public void onStart() {

                    Log.d(TAG, "Started command : ffmpeg " + Arrays.toString(command));

                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg ");
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }

    private void saveImage(Bitmap finalBitmap, String fileName) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/download/");
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

    public void saveTempBitmapFrame(Bitmap bitmap, int frameNum ) {
        if (isExternalStorageWritable()) {
            saveImage(bitmap, "tb-bitmap-frame-"+ frameNum);
            Log.d(TAG, "Saved bitmap " + frameNum);
        }else{
            //prompt the user or do something
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


}