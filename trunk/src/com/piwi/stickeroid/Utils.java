package com.piwi.stickeroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class Utils
{
    static boolean copyFile(File in, File out)
    {
        try
        {
            InputStream is = new FileInputStream(in);
            OutputStream os = new FileOutputStream(out);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();
            return true;
        }
        catch(IOException e)
        {
            Log.e("ExternalStorage", "Error writing " + out, e);
        }
        return false;
    }

    static void showToaster(Context ctxt, int id)
    {
        Toast toast = Toast.makeText(ctxt, id, Toast.LENGTH_SHORT);
        toast.show();
    }

    static void showToaster(Context ctxt, String msg)
    {
        Toast toast = Toast.makeText(ctxt, msg, Toast.LENGTH_SHORT);
        toast.show();
    }
}
