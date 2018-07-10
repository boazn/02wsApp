/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package il.co.jws.app;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
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
        Log.i(TAG, "Starting the AppWidget update service with intent " + startId );
        doThreadStart();
        // Want this service to continue running until explicitly stopped, so return sticky.
        return START_STICKY;
    }

    // Start the background thread to do the widget update as an instance of 
    // the class BackgroundThread
    private void doThreadStart() {
        background = new BackgroundThread();
        background.start();
    }

     // Class to run background thread for updates.  Not essential for this example since
    // the update is very fast, but good general practice if the update involves 
    // blocking operations like web access because otherwise this would run on
    // the main UI thread.  We override the run() method inherited from Thread to
    // perform the update.
    private class BackgroundThread extends Thread {

              
        @Override
        public void run() {
            Log.i(TAG, "  Begin background thread");

            if (savedIntent == null)
                return;
            // Retrieve a widget manager and get the IDs of any widget instances from the
            // intent that started this service
            AppWidgetManager apw = AppWidgetManager.getInstance(getApplicationContext());
            RemoteViews viewsForLarge = new RemoteViews(getApplicationContext().getPackageName(), R.layout.large_appwidget);
            RemoteViews viewsForSmall = new RemoteViews(getApplicationContext().getPackageName(), R.layout.small_appwidget);
            RemoteViews viewsForRect = new RemoteViews(getApplicationContext().getPackageName(), R.layout.rectangle_appwidget);
            int[] ids = savedIntent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            String AppWidgetType = savedIntent.getStringExtra(Config.WIDGET_TYPE);
            if ((ids != null) && (AppWidgetType != null)) {
                if (ids.length > 0) {
                    // Loop through all widget instances and update
                    final int num = ids.length;
                    for (int i = 0; i < num; i++) {
                        if (AppWidgetType.equals(Config.WIDGET_TYPE_LARGE)) {
                            new DownloadBitmap(viewsForLarge, ids[i], apw, getApplicationContext()).execute("Bitmap");
                     } else if (AppWidgetType.equals(Config.WIDGET_TYPE_SMALL)) {
                            new DownloadJson(viewsForSmall, ids[i], apw, AppWidgetType, getApplicationContext()).execute(AppWidgetType);
                        }
                        else{
                            new DownloadJson(viewsForRect, ids[i], apw, AppWidgetType, getApplicationContext()).execute(AppWidgetType);
                        }
                    }
                    // This update is now finished, so stop the service
                    Log.i(TAG, "Stopping the AppWidget update service");
                    stopSelf();
                }

            } else {
                Log.i(TAG, "ids is null or AppWidgetType is null");
            }

        }
    }


}