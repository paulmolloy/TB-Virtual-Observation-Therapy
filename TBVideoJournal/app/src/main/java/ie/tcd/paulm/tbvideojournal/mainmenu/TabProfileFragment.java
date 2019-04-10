package ie.tcd.paulm.tbvideojournal.mainmenu;

import android.app.Dialog;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.auth.Auth;
import ie.tcd.paulm.tbvideojournal.firestore.FSNurse;
import ie.tcd.paulm.tbvideojournal.firestore.FSPatient;
import ie.tcd.paulm.tbvideojournal.misc.Misc;

public class TabProfileFragment extends Fragment {

    TextView patientName, nurseName;
    TextView prescriptions1, prescriptions2,prescriptions3,prescriptions4,prescriptions5;
    private static final String  TAG              = "Profile page";

    private FirebaseStorage storage;
    private Dialog videoDialog;
    private VideoView videoView;
    private Button closeVideoButton;
    private static final String VOT_DIR = "/tb-vot/";
    private static final String FIREBASE_VOT_DIR = "vot-videos/";
    private static final String TUTORIAL_FILE_NAME = "tutorial.mp4";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_profile, container, false);

        patientName = view.findViewById(R.id.MainMenu_name);
        nurseName = view.findViewById(R.id.MainMenu_nurse);
        prescriptions1 = view.findViewById(R.id.prescriptions);
        prescriptions2 = view.findViewById(R.id.prescriptions2);
        prescriptions3 = view.findViewById(R.id.prescriptions3);
        prescriptions4 = view.findViewById(R.id.prescriptions4);
        prescriptions5 = view.findViewById(R.id.prescriptions5);
        prescriptions1.setVisibility(SurfaceView.GONE);
        prescriptions2.setVisibility(SurfaceView.GONE);
        prescriptions3.setVisibility(SurfaceView.GONE);
        prescriptions4.setVisibility(SurfaceView.GONE);
        prescriptions5.setVisibility(SurfaceView.GONE);


        loadFirestoreStuff();

        ImageView imageView = (ImageView) view.findViewById(R.id.profile_pic);

        Glide.with(this).load("https://firebasestorage.googleapis.com/v0/b/tb-vot.appspot.com/o/patient-photos%2F" +
                Auth.getCurrentUserID() +
                ".jpg?alt=media&token=9b52b435-92ba-445d-aad6-43e4b84f1da1")
                .apply(RequestOptions.circleCropTransform())
                .override(200,200)
                .into(imageView);


        videoDialog = new Dialog(getActivity());
        videoDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        videoDialog.setContentView(R.layout.view_video_dialog);
        videoView = (VideoView) videoDialog.findViewById(R.id.last_video_test);
        closeVideoButton = videoDialog.findViewById(R.id.dialog_close_b);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.copyFrom(videoDialog.getWindow().getAttributes());
        videoDialog.getWindow().setAttributes(lp);

        videoDialog.getWindow().setFormat(PixelFormat.TRANSLUCENT);
        File root = Environment.getExternalStorageDirectory();

        File dir = new File (root.getAbsolutePath() + VOT_DIR);
        dir.mkdirs();

        closeVideoButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.dialog_close_b:
                        videoDialog.dismiss();
                }
            }

        });

        return view;
    }

    private void loadFirestoreStuff(){

        FSPatient.download(
                Auth.getCurrentUserID(),
                patientData -> {

                    patientName.setText("Welcome back " + patientData.name);

                    FSNurse.download(
                            patientData.nurseID,
                            nurseData -> nurseName.setText("Your nurse is " + nurseData.name),
                            error -> nurseName.setText(error)
                    );

                    prescriptions1.setVisibility(SurfaceView.VISIBLE);
                    prescriptions2.setVisibility(SurfaceView.VISIBLE);
                    prescriptions3.setVisibility(SurfaceView.VISIBLE);
                    prescriptions4.setVisibility(SurfaceView.VISIBLE);
                    prescriptions5.setVisibility(SurfaceView.VISIBLE);
                    showTutorial();

                },
                error -> patientName.setText(error)
        );

    }

    private MainActivity getRoot(){
        return (MainActivity) getActivity();
    }

    private void showTutorial(){


        // Play a vot video by clicking on it in the list.
        storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference vidRef = storageRef.child(FIREBASE_VOT_DIR + TUTORIAL_FILE_NAME);
        File root = Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath() + VOT_DIR );
        dir.mkdirs();
        File localFile = new File(dir.getAbsolutePath() + "/" + TUTORIAL_FILE_NAME);
        Log.d(TAG, "Listview downloading file: " + localFile.getAbsolutePath());

        // Download video if it isn't local.
        // TODO(paulmolloy): Download video if local video is older than the firebase video.
        if(!localFile.exists()) {
            // TODO(paulmolloy): do progress indicator.
            Misc.toast("Vot " + TUTORIAL_FILE_NAME + " is not stored locally downloading...", getContext(), true);
            Log.d(TAG, "Downloading video: " + FIREBASE_VOT_DIR + TUTORIAL_FILE_NAME);

            vidRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    // Local vot file has been created.
                    Uri uri = Uri.fromFile(localFile);
                    videoDialog.show();
                    videoView.setVideoURI(uri);
                    videoView.start();

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    Misc.toast("Failed to download video:" + TUTORIAL_FILE_NAME, getContext());
                    Log.e(TAG, "Failed to download video: " + exception);

                }
            });

        }else{
            // Play the video in the VideoView.
            Uri uri = Uri.parse(localFile.getAbsolutePath()); //Declare your url here.
            videoDialog.show();
            videoView.setVideoURI(uri);
            videoView.start();
        }
    }
}
