package ie.tcd.paulm.tbvideojournal.misc;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class Misc {

    /** Convenience method to show a toast */
    public static void toast(String text, Context context){
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

}
