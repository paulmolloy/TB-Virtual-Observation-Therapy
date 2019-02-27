package ie.tcd.paulm.tbvideojournal.mainmenu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.auth.Auth;
import ie.tcd.paulm.tbvideojournal.firestore.FSNurse;
import ie.tcd.paulm.tbvideojournal.firestore.FSPatient;

public class TabProfileFragment extends Fragment {

    TextView patientName, nurseName;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_profile, container, false);

        Button signOutButton = view.findViewById(R.id.MainMenu_signOut);
        signOutButton.setOnClickListener(b -> {
            Auth.signOut();
            getRoot().showSignInScreen();
        });

        patientName = view.findViewById(R.id.MainMenu_name);
        nurseName = view.findViewById(R.id.MainMenu_nurse);

        loadFirestoreStuff();

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

                },
                error -> patientName.setText(error)
        );

    }

    private MainActivity getRoot(){
        return (MainActivity) getActivity();
    }
}
