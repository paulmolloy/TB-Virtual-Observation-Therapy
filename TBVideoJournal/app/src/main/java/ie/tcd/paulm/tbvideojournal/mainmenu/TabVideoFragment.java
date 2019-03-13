package ie.tcd.paulm.tbvideojournal.mainmenu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.auth.Auth;
import ie.tcd.paulm.tbvideojournal.firestore.FSPatient;

public class TabVideoFragment extends Fragment {

    TextView takenToday,timeToTake,streak;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_video, container, false);

        takenToday = view.findViewById(R.id.MainMenu_taken_today);
        timeToTake = view.findViewById(R.id.MainMenu_time_to_take);
        streak = view.findViewById(R.id.MainMenu_streak);
        loadFirestoreStuff();

        Button faceButton = view.findViewById(R.id.MainMenu_vot);
        faceButton.setOnClickListener(b -> getRoot().showFaceScreen());

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }

    private void loadFirestoreStuff(){

        FSPatient.download(
                Auth.getCurrentUserID(),
                patientData -> {
                    takenToday.setText("Taken today: "+ String.valueOf(patientData.takenToday));
                    timeToTake.setText("Time to take prescription: "+ patientData.timeToTake);
                    streak.setText("Current streak: "+ patientData.streak);
                },
                error -> takenToday.setText(error)
        );

    }

    private MainActivity getRoot(){
        return (MainActivity) getActivity();
    }
}
