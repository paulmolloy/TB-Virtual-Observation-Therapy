package ie.tcd.paulm.tbvideojournal.auth;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Auth {

    // Lazy init Firebase Auth
    private static FirebaseAuth _auth;
    private static FirebaseAuth auth(){
        if(_auth == null) _auth = FirebaseAuth.getInstance();
        return _auth;
    }

    public static FirebaseUser getCurrentUser(){
        return auth().getCurrentUser();
    }

    public static boolean isSignedIn(){
        return getCurrentUser() != null;
    }

    public static String getCurrentUserID(){
        FirebaseUser u = getCurrentUser();
        if(u == null) {
            Log.e("Auth", "The user seems to be null in tb.auth.Auth.getCurrentUserID()");
            return "";
        }
        return u.getUid();
    }

    /** Attempts to sign in using the provided email and password */
    public static Task<AuthResult> signIn(String email, String password) {
        return auth().signInWithEmailAndPassword(email, password);
    }

    public static void signOut(){
        auth().signOut();
    }


}
