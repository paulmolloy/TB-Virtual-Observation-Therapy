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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.auth.Auth;

import ie.tcd.paulm.tbvideojournal.firestore.FSVotVideoRef;
import ie.tcd.paulm.tbvideojournal.misc.Misc;

public class TabDiaryFragment extends Fragment {

    private static final  String VOT_VIDEO_FILENAME = "latest-vot";
    private static final String VOT_SCREEN_RECORD_VIDEO_FILENAME = "screen_record_latest";

    private static final String VOT_DIR = "/tb-vot/";
    private static final String TAG = "TabDiaryFragment";
    ArrayAdapter<String> arrayAdapter;
    private ListView votsLV;
    private Map<String, String> nameToFSPath;
    private FirebaseStorage storage;
    private Uri uri;
    private View v;
    private Dialog videoDialog;
    private VideoView videoView;
    private Button closeVideoButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_tab_diary, container, false);
        if(getUserVisibleHint()){ // fragment is visible
            loadData();
        }
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


        return v;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) { // fragment is visible and have created
            loadData();
        }
    }

    public void loadData(){
        File root = Environment.getExternalStorageDirectory();
        votsLV = (ListView) v.findViewById(R.id.vots_lv);
        File localFile = new File(root.getAbsolutePath() + VOT_DIR + VOT_SCREEN_RECORD_VIDEO_FILENAME + ".mp4");
        if(localFile.exists() && localFile.length() != 0) {
            // The file shouldn't ever be empty if it exists here but check anyway.
            uri = Uri.fromFile(localFile);
            videoView.setVideoURI(uri);
        }

        // Fill ListView with the vot video labels.
        FSVotVideoRef.downloadVotReferences(Auth.getCurrentUserID(),
                vots -> {

                    List<String> votsElements = new ArrayList<String>();
                    nameToFSPath = new HashMap<String, String>();
                    for(FSVotVideoRef vot : vots) {
                        if(vot.label != null && vot.videoPath != null) {
                            votsElements.add(vot.label);
                            nameToFSPath.put(vot.label, vot.videoPath);
                            Log.d(TAG, "Adding to list: " + vot.label);
                        }

                    }

                    arrayAdapter = new ArrayAdapter<String>(
                            getContext(),
                            android.R.layout.simple_list_item_1,
                            votsElements);
                    votsLV.setAdapter(arrayAdapter);

                },

                error -> {
                    Log.e(TAG, "Error retrieving vot video refs: " + error );
                }

        );
        closeVideoButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.dialog_close_b:
                        videoDialog.dismiss();
                }
            }

        });

        votsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parentView, View childView,
                                    int position, long id)
            {
                // Select video
                String item = (String) votsLV.getItemAtPosition(position);
                Log.d(TAG, "You selected: " + item);

                // Play a vot video by clicking on it in the list.
                storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                StorageReference vidRef = storageRef.child(nameToFSPath.get(item));
                File root = Environment.getExternalStorageDirectory();
                File dir = new File (root.getAbsolutePath() + VOT_DIR );
                dir.mkdirs();
                String fileName = new File(nameToFSPath.get(item)).getName();
                File localFile = new File(dir.getAbsolutePath() + "/" + fileName);
                Log.d(TAG, "Listview downloading file: " + localFile.getAbsolutePath());

                // Download video if it isn't local.
                // TODO(paulmolloy): Download video if local video is older than the firebase video.
                if(!localFile.exists()) {
                    // TODO(paulmolloy): do progress indicator.
                    Misc.toast("Vot " + item + " is not stored locally downloading...", getContext(), true);
                    Log.d(TAG, "Downloading video: " + nameToFSPath.get(item));
                    vidRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            // Local vot file has been created.
                            uri = Uri.fromFile(localFile);
                            videoDialog.show();
                            videoView.setVideoURI(uri);
                            videoView.start();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle any errors
                            Misc.toast("Failed to download video:" + item, getContext());
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

        });

    }

}
