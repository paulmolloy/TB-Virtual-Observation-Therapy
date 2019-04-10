package ie.tcd.paulm.tbvideojournal.mainmenu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.auth.Auth;
import ie.tcd.paulm.tbvideojournal.firestore.FSNurse;
import ie.tcd.paulm.tbvideojournal.firestore.FSPatient;

public class TabProfileFragment extends Fragment {

    TextView patientName, nurseName;
    TextView prescriptions1, prescriptions2,prescriptions3,prescriptions4,prescriptions5;
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

                },
                error -> patientName.setText(error)
        );

    }

    private MainActivity getRoot(){
        return (MainActivity) getActivity();
    }
}
