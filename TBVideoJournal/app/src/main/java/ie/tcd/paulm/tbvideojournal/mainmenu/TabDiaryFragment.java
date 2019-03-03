package ie.tcd.paulm.tbvideojournal.mainmenu;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tab_diary, container, false);
        File root = Environment.getExternalStorageDirectory();
        Uri uri = Uri.parse(root.getAbsolutePath() + VOT_DIR + VOT_SCREEN_RECORD_VIDEO_FILENAME + ".mp4"); //Declare your url here.
        votsLV = (ListView) v.findViewById(R.id.vots_lv);
        VideoView mVideoView  = (VideoView)v.findViewById(R.id.last_video);
        mVideoView.setMediaController(new MediaController(getContext()));
        mVideoView.setVideoURI(uri);
        mVideoView.requestFocus();
        mVideoView.start();

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
            }
            
        });



        return v;
    }
}
