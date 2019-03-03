package ie.tcd.paulm.tbvideojournal.mainmenu;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.auth.Auth;
import ie.tcd.paulm.tbvideojournal.firestore.FSNurse;
import ie.tcd.paulm.tbvideojournal.firestore.FSPatient;
import ie.tcd.paulm.tbvideojournal.firestore.FSVotVideoRef;
import ie.tcd.paulm.tbvideojournal.misc.Misc;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;


public class TabDiaryFragment extends Fragment {

    private static final  String VOT_VIDEO_FILENAME = "latest-vot";
    private static final String VOT_SCREEN_RECORD_VIDEO_FILENAME = "screen_record_latest";

    private static final String VOT_DIR = "/tb-vot/";
    private static final String TAG = "TabDiaryFragment";
    ArrayAdapter<String> arrayAdapter;
    private ListView votsLV;
    private Map<String, String> nameToFSPath;
    private FirebaseStorage storage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tab_diary, container, false);
        File root = Environment.getExternalStorageDirectory();
        Uri uri = Uri.parse(root.getAbsolutePath() + VOT_DIR + VOT_SCREEN_RECORD_VIDEO_FILENAME + ".mp4"); //Declare your url here.
        votsLV = (ListView) v.findViewById(R.id.vots_lv);
        VideoView mVideoView  = (VideoView)v.findViewById(R.id.last_video);
        mVideoView.setMediaController(new MediaController(getContext()));
        mVideoView.setVideoURI(uri);

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

        votsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parentView, View childView,
                                       int position, long id)
            {
                // Select video
                String item = (String) votsLV.getItemAtPosition(position);
                Misc.toast("You selected: " + item, getContext());
                Log.d(TAG, "You selected: " + item);

                // Download video if it isn't local.
                storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                StorageReference vidRef = storageRef.child(nameToFSPath.get(item));

                File root = Environment.getExternalStorageDirectory();
                File dir = new File (root.getAbsolutePath() + VOT_DIR );
                dir.mkdirs();
                String fileName = new File(nameToFSPath.get(item)).getName();
                File localFile = new File(dir.getAbsolutePath() + fileName);
                Log.d(TAG, "Listview downloading file: " + localFile.getAbsolutePath());

                if(!localFile.exists()) {
                    // TODO(paulmolloy): do progress indicator.
                    Misc.toast("Vot " + item + " is not stored locally downloading...", getContext());

                    vidRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            // Local temp file has been created

                            Uri uri = Uri.parse(localFile.getAbsolutePath()); //Declare your url here.
                            mVideoView.setVideoURI(uri);
                            mVideoView.requestFocus();
                            mVideoView.start();


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
                    Uri uri = Uri.parse(localFile.getAbsolutePath()); //Declare your url here.
                    mVideoView.setVideoURI(uri);
                    mVideoView.requestFocus();
                    mVideoView.start();
                }

            }

        });



        return v;
    }

    public static void copyFile(File src, File dst) throws IOException
    {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }
}
