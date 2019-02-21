package ie.tcd.paulm.tbvideojournal.auth;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.mainmenu.MainMenuFragment;
import ie.tcd.paulm.tbvideojournal.misc.Misc;

public class SignInFragment extends Fragment {

    public SignInFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        Button peter = view.findViewById(R.id.SignIn_peterButton);
        peter.setOnClickListener(b -> signIn("peter.mc.patient@email.com", "Password123"));

        return view;

    }

    private void signIn(String email, String password){

        Auth.signIn("peter.mc.patient@email.com", "Password123")
            .addOnSuccessListener(r -> getRoot().showMainMenuScreen(true))
            .addOnFailureListener(e -> Misc.toast("Something went wrong", getContext()));

    }

    private MainActivity getRoot(){
        return (MainActivity) getActivity();
    }

}
