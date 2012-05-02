package com.piwi.stickeroid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.widget.Toast;

public class Utils
{
    public static String getVersionName(Context context)
    {
        try
        {
            ComponentName comp = new ComponentName(context, Utils.class);
            PackageInfo pinfo = context.getPackageManager()
                    .getPackageInfo(comp.getPackageName(), 0);
            return pinfo.versionName;
        }
        catch(NameNotFoundException e)
        {
            return "";
        }
    }

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
        }
        return false;
    }

    static boolean equalsFilesBinary(File first, File second)
    {
        final int BUFFER_SIZE = 32 * 1024;

        boolean result = false;

        try
        {
            if(first.exists() && second.exists() && first.isFile() && second.isFile())
            {
                if(first.getCanonicalPath().equals(second.getCanonicalPath()))
                {
                    result = true;
                }
                else
                {
                    FileInputStream firstInput = null;
                    FileInputStream secondInput = null;
                    BufferedInputStream bufFirstInput = null;
                    BufferedInputStream bufSecondInput = null;

                    try
                    {
                        firstInput = new FileInputStream(first);
                        secondInput = new FileInputStream(second);
                        bufFirstInput = new BufferedInputStream(firstInput, BUFFER_SIZE);
                        bufSecondInput = new BufferedInputStream(secondInput, BUFFER_SIZE);

                        int firstByte;
                        int secondByte;

                        while(true)
                        {
                            firstByte = bufFirstInput.read();
                            secondByte = bufSecondInput.read();
                            if(firstByte != secondByte)
                            {
                                break;
                            }
                            if(firstByte < 0 && secondByte < 0)
                            {
                                result = true;
                                break;
                            }
                        }
                    }
                    catch(IOException ioe)
                    {
                        result = false;
                    }
                    finally
                    {
                        try
                        {
                            if(bufFirstInput != null)
                            {
                                bufFirstInput.close();
                            }
                        }
                        finally
                        {
                            if(bufSecondInput != null)
                            {
                                bufSecondInput.close();
                            }
                        }
                    }
                }
            }
        }
        catch(IOException ioe)
        {
            result = false;
        }

        return result;
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
