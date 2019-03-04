package ie.tcd.paulm.tbvideojournal.misc;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Misc {

    /** Convenience method to show a toast */
    public static void toast(String text, Context context){
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    /** Convenience method to show a toast */
    public static void toast(String text, Context context, boolean shortToast){
        Toast.makeText(context, text, shortToast ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
    }


    public static void copyFile(File src, File dst) throws IOException
    {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

}
