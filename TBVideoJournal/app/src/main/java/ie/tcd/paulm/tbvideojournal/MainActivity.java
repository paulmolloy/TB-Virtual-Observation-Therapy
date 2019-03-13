package ie.tcd.paulm.tbvideojournal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import ie.tcd.paulm.tbvideojournal.auth.Auth;
import ie.tcd.paulm.tbvideojournal.auth.SignInFragment;
import ie.tcd.paulm.tbvideojournal.mainmenu.MainMenuFragment;
import ie.tcd.paulm.tbvideojournal.opencv.VotCamera;

public class MainActivity extends AppCompatActivity {

    public static Context context;
    private static final String TAG = "TB Video Journal";


    public MainActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hack so we can Make a toast with this context in the firebase VotCamera.OnComplete()
        MainActivity.context = this.getApplicationContext();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA},
                    1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO},
                    1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }

        if(!Auth.isSignedIn()) showSignInScreen();
        else showMainMenuScreen(true);

    }

    @Override
    public void onBackPressed() {

        FragmentManager fm = getSupportFragmentManager();

        if (fm.getBackStackEntryCount() == 1) finish();
        else fm.popBackStack();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.logout) {
            Auth.signOut();
            showSignInScreen();
        }

        return super.onOptionsItemSelected(item);
    }





    // ------------------------- Screens -------------------------

    public void showMainMenuScreen(boolean clearBackStack){
        showFragment(new MainMenuFragment(), "Main Menu", clearBackStack);
    }

    public void showSignInScreen(){
        showFragment(new SignInFragment(), "Sign In", true);
    }

    public void showFaceScreen(){
        showFragment(new VotCamera(), "FaceCamera");
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