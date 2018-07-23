/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package il.co.jws.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

/**
 *
 * @author boaz
 */
public class UpdateService extends Service {

    public static final String TAG = "APP_WIDGET";
    private BackgroundThread background;
    public Intent savedIntent;
    public static long refTimeMS;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            this.startForeground(1, notification);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        background.interrupt();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        savedIntent = intent;
        int[] ids = savedIntent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        String AppWidgetType = savedIntent.getStringExtra(Config.WIDGET_TYPE);
        for (int i = 0; i < ids.length; i++) {
            Log.i(TAG, "#" + i + " WIDGET_TYPE: " + AppWidgetType + " " + ids[i]);
        }
        Log.i(TAG, "Starting the AppWidget update service with intent " + startId + " " + flags);
        doThreadStart(ids, AppWidgetType);
        // Want this service to continue running until explicitly stopped, so return sticky.
        return START_STICKY;
    }

    // Start the background thread to do the widget update as an instance of 
    // the class BackgroundThread
    private void doThreadStart(int[] p_ids, String p_AppWidgetType) {
        background = new BackgroundThread(p_ids, p_AppWidgetType);
        background.start();
    }

     // Class to run background thread for updates.  Not essential for this example since
    // the update is very fast, but good general practice if the update involves 
    // blocking operations like web access because otherwise this would run on
    // the main UI thread.  We override the run() method inherited from Thread to
    // perform the update.
    private class BackgroundThread extends Thread {


         int[] ids;
         String AppWidgetType;

         BackgroundThread(int[] p_ids, String p_AppWidgetType){
             ids = p_ids;
             AppWidgetType = p_AppWidgetType;
         }

         @Override
         public void run() {
             if (savedIntent == null)
                 return;
             // Retrieve a widget manager and get the IDs of any widget instances from the
             // intent that started this service
             AppWidgetManager apw = AppWidgetManager.getInstance(getApplicationContext());
             Log.i(TAG, "in BackgroundThread EXTRA_APPWIDGET_IDS length:" + ids.length);
             if ((ids == null) || (AppWidgetType == null)) {
                 Log.d(TAG, "EXTRA_APPWIDGET_IDS is null or WIDGET_TYPE is null");
                 return;
             }
             // Loop through all widget instances and update
             for (int i = 0; i < ids.length; i++) {
                 Log.i(TAG, "in BackgroundThread #" + i + " WIDGET_TYPE: " + AppWidgetType + " " + ids[i]);
                 if (AppWidgetType.equals(Config.WIDGET_TYPE_LARGE))
                     new DownloadBitmap(new RemoteViews(getApplicationContext().getPackageName(), R.layout.large_appwidget),
                             ids[i],
                             apw,
                             getApplicationContext()).execute("Bitmap");
                 else if (AppWidgetType.equals(Config.WIDGET_TYPE_SMALL))
                     new DownloadJson(new RemoteViews(getApplicationContext().getPackageName(), R.layout.small_appwidget),
                             ids[i],
                             apw,
                             AppWidgetType,
                             getApplicationContext()).execute(AppWidgetType);
                 else
                     new DownloadJson(new RemoteViews(getApplicationContext().getPackageName(), R.layout.rectangle_appwidget),
                             ids[i],
                             apw,
                             AppWidgetType,
                             getApplicationContext()).execute(AppWidgetType);


                 // This update is now finished, so stop the service
                 Log.i(TAG, "Stopping the AppWidget update service");
                 stopSelf();


             }
         }

     }
}
