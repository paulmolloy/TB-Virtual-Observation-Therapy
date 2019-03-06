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

public class TabVideoFragment extends Fragment {

    TextView takenToday,timeToTake,streak;
    private boolean tmpTaken;
    private String tmpTime;
    private int tmpStreak;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_video, container, false);

        //Dummy values, will be actual values from firebase when data is in the DB
        tmpTaken = false;
        tmpTime = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
        tmpStreak = 5;


        takenToday = view.findViewById(R.id.MainMenu_taken_today);
        takenToday.setText("Taken today: "+ String.valueOf(tmpTaken));
        timeToTake = view.findViewById(R.id.MainMenu_time_to_take);
        timeToTake.setText("Time to take prescription: "+ tmpTime);
        streak = view.findViewById(R.id.MainMenu_streak);
        streak.setText("Current streak: "+ tmpStreak);

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

    private MainActivity getRoot(){
        return (MainActivity) getActivity();
    }
}
