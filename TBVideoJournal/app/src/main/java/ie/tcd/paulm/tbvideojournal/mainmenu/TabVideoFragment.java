package ie.tcd.paulm.tbvideojournal.mainmenu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;

public class TabVideoFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_video, container, false);

        Button faceButton = view.findViewById(R.id.MainMenu_vot);
        faceButton.setOnClickListener(b -> getRoot().showFaceScreen());

        return view;
    }

    private MainActivity getRoot(){
        return (MainActivity) getActivity();
    }
}
