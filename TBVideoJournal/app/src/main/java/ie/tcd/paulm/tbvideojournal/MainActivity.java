package ie.tcd.paulm.tbvideojournal;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import ie.tcd.paulm.tbvideojournal.auth.Auth;
import ie.tcd.paulm.tbvideojournal.auth.SignInFragment;
import ie.tcd.paulm.tbvideojournal.mainmenu.MainMenuFragment;
import ie.tcd.paulm.tbvideojournal.opencv.CameraFragment;
import ie.tcd.paulm.tbvideojournal.opencv.FaceDetector;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TB Video Journal";

    public MainActivity() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!Auth.isSignedIn()) showSignInScreen();
        else showMainMenuScreen(true);

    }

    @Override
    public void onBackPressed() {

        FragmentManager fm = getSupportFragmentManager();

        if (fm.getBackStackEntryCount() == 1) finish();
        else fm.popBackStack();

    }





    // ------------------------- Screens -------------------------

    public void showMainMenuScreen(boolean clearBackStack){
        showFragment(new MainMenuFragment(), "Main Menu", clearBackStack);
    }

    public void showSignInScreen(){
        showFragment(new SignInFragment(), "Sign In", true);
    }

    public void showCameraScreen(){
        showFragment(new CameraFragment(), "Camera");
    }
    public void showFaceScreen(){
        showFragment(new FaceDetector(), "FaceCamera");
    }

    /**
     * Displays a different fragment (switches screens)
     * @param fragment The new fragment to display
     * @param fragmentName A unique string that can be used to identify this fragment
     */
    public void showFragment(Fragment fragment, String fragmentName){
        showFragment(fragment, fragmentName, false);
    }

    /**
     * Displays a different fragment (switches screens)
     * @param fragment The new fragment to display
     * @param fragmentName A unique string that can be used to identify this fragment
     * @param clearBackStack If <b>true</b> then the back button history will be forgotten, i.e.
     *                       pressing the back button (the triangle on the bottom left) will close
     *                       the app. If <b>false</b>, then pressing the back button will open the
     *                       previous fragment.
     */
    public void showFragment(Fragment fragment, String fragmentName, boolean clearBackStack){

        FragmentManager fm = getSupportFragmentManager();

        if(clearBackStack && fm.getBackStackEntryCount() > 0){
            String name = fm.getBackStackEntryAt(0).getName();
            fm.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        fm.beginTransaction()
            .replace(R.id.MainActivity_fragmentContainer, fragment)
            .addToBackStack(fragmentName)
            .commit();

    }



}