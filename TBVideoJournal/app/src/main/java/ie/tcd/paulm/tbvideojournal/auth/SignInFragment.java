package ie.tcd.paulm.tbvideojournal.auth;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;
import ie.tcd.paulm.tbvideojournal.mainmenu.MainMenuFragment;
import ie.tcd.paulm.tbvideojournal.misc.Misc;


public class SignInFragment extends Fragment {

    private EditText Email;
    private EditText Password;
    //private TextView Attempts;
    private Button Login;

    public SignInFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        Email = (EditText) view.findViewById(R.id.emailEdtTxt);
        Password = (EditText) view.findViewById(R.id.passwordEdtTxt);
        //Attempts = (TextView) view.findViewById(R.id.attemptsTxtView);
        Login = (Button) view.findViewById(R.id.loginBtn);
        Login.setOnClickListener(button -> {
            String email = Email.getText().toString();
            String password = Password.getText().toString();
            signIn(email, password);
        }
        );

        return view;
    }

    private void signIn(String email, String password){

        Auth.signIn(email, password)
            .addOnSuccessListener(result -> getRoot().showMainMenuScreen(true))
            .addOnFailureListener(error -> Misc.toast("Something went wrong", getContext()));

    }

    private MainActivity getRoot(){
        return (MainActivity) getActivity();
    }


}
