package ie.tcd.paulm.tbvideojournal.mainmenu;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.File;

import ie.tcd.paulm.tbvideojournal.R;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;


public class TabDiaryFragment extends Fragment {

    private static final  String VOT_VIDEO_FILENAME = "latest-vot";
    private static final String VOT_SCREEN_RECORD_VIDEO_FILENAME = "screen_record_latest";

    private static final String VOT_DIR = "/tb-vot/";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tab_diary, container, false);
        File root = Environment.getExternalStorageDirectory();
        Uri uri = Uri.parse(root.getAbsolutePath() + VOT_DIR + VOT_SCREEN_RECORD_VIDEO_FILENAME + ".mp4"); //Declare your url here.

        VideoView mVideoView  = (VideoView)v.findViewById(R.id.last_video);
        mVideoView.setMediaController(new MediaController(getContext()));
        mVideoView.setVideoURI(uri);
        mVideoView.requestFocus();
        mVideoView.start();

        return v;
    }
}
