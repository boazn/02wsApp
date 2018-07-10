/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package il.co.jws.app;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;

import com.google.firebase.crash.FirebaseCrash;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static il.co.jws.app.UpdateService.TAG;

/**
 *
 * @author boaz
 */
public class DownloadBitmap extends AsyncTask<String, Void, Bitmap> {

    private final RemoteViews views;
    private final int WidgetID;
    private final Context context; 
    private final AppWidgetManager WidgetManager;
    private int lang;
    private String tempunit;

    public DownloadBitmap(RemoteViews views, int appWidgetID, AppWidgetManager appWidgetManager, Context p_context) {
        this.views = views;
        this.WidgetID = appWidgetID;
        this.WidgetManager = appWidgetManager;
        this.context = p_context;
        SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, context.MODE_PRIVATE);
        this.lang = prefs.getInt(Config.PREFS_LANG, 1);
        this.tempunit = prefs.getString(Config.PREFS_TEMPUNIT, Config.C_TEMPUNIT);
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        try {
            InputStream in;URL url;
            if (lang == 1)
              url = new URL(Config.URL_SHORT);
            else
              url = new URL(Config.URL_SHORT_ENG + "&temp_unit=" + tempunit);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            try {
                 con.connect();
            }
            catch (Exception e) {//No address associated with hostname
                Log.e(TAG, "ImageDownload: Download failed: " + e.getMessage());
                con.connect();
            }
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                in = con.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                bitmap = scaleDownBitmap(bitmap, 100, context);
                Log.v(TAG, "ImageDownload: download succeeded");
                Log.v(TAG, "ImageDownload: Param 0 is: " + params[0]);
                return bitmap;
            }


            //NOTE:  it is not thread-safe to set the ImageView from inside this method.  It must be done in onPostExecute()
        }

        catch (Exception e) {
            Log.e(TAG, "ImageDownload: Download failed: " + e.getMessage());
            FirebaseCrash.report(e);
        }
        return null;
    }
    
    public static Bitmap scaleDownBitmap(Bitmap photo, int newHeight, Context context) {
        if (photo == null)
            return null;
        final float densityMultiplier = context.getResources().getDisplayMetrics().density;        
        int h= (int) (newHeight*densityMultiplier);
        int w= (int) (h * photo.getWidth()/((double) photo.getHeight()));
        Bitmap newphoto = Bitmap.createScaledBitmap(photo, w, h, true);
        Log.v(TAG, "scaleDownBitmap: old bitmap: height:" + photo.getHeight()+ " width:" + photo.getWidth() + " bytes:" + photo.getByteCount());
        Log.v(TAG, "scaleDownBitmap: new bitmap: height:" + newphoto.getHeight()+ " width:" + newphoto.getWidth() + " bytes:" + newphoto.getByteCount());
        return newphoto;
    }

    @Override
    public void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.btnOpenApp, bitmap);

            WidgetManager.updateAppWidget(WidgetID, views);
            Log.v(TAG, "setImageViewBitmap succeeded for ID " + WidgetID + ". height:" + bitmap.getHeight() + " width:" + bitmap.getWidth() + " bytes:" + bitmap.getByteCount());
        } else {
            Log.v(TAG, "bitmap is null");
        }
    }

}
