package com.example.randomaptesting;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class GetBitmap extends AsyncTask<String, Void, Bitmap[]> {

    @Override
    protected Bitmap[] doInBackground(String[] urls) {
        Bitmap[] bm = null;
        try {
            URL aURL = new URL(urls[0]);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm[0] = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            System.out.println("Error getting bitmap");
        }
        return bm;
    }
}
