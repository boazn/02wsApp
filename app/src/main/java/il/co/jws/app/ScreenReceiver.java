package il.co.jws.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;

import java.util.Calendar;

/**
 * Created by boaz on 06/08/2016.
 */
public class ScreenReceiver extends BroadcastReceiver {
    public static final String TAG = "SCREEN";
    public static boolean wasScreenOn = true;
    public boolean isRegistered;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        //Log.i(TAG,"ScreenReceiver onReceive");
        Log.i(TAG," ****** ScreenReceiver onRecieve, action: "+intent.getAction() + " **********");

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            // do whatever you need to do here
            wasScreenOn = false;
       } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            // and do whatever you need to do here
            if (!isScreenOn(context))
                return;
           wasScreenOn = true;
            Log.i(TAG,"ACTION_SCREEN_ON");
            updateWidgets(context);

        } else if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            if (!isScreenOn(context))
                return;
            updateWidgets(context);
            Log.i(TAG,"userpresent (unlocked)");
        }
        else if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            if (!isScreenOn(context))
                return;
            updateWidgets(context);
            Log.i(TAG,"userpresent after boot");
        }

    }

    public void updateWidgets(Context context){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisRectAppWidget = new ComponentName(context.getPackageName(), RectangleAppWidgetProvider.class.getName());
        int[] rectappWidgetIds = appWidgetManager.getAppWidgetIds(thisRectAppWidget);
        Intent rectwidgetIntent = new Intent(context, UpdateService.class);
        rectwidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, rectappWidgetIds);
        rectwidgetIntent.putExtra(Config.WIDGET_TYPE, Config.WIDGET_TYPE_RECT);
        Log.i(TAG, "discovered " + rectappWidgetIds.length + " " + Config.WIDGET_TYPE_RECT + " " +  (rectappWidgetIds.length > 0 ? rectappWidgetIds[0] : "") );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (rectappWidgetIds.length > 0)
               // Util.scheduleJob(context, rectappWidgetIds, Config.WIDGET_TYPE_RECT);
               context.startForegroundService(rectwidgetIntent);
        } else {
            context.startService(rectwidgetIntent);
        }


        ComponentName thisSMallAppWidget = new ComponentName(context.getPackageName(), SmallAppWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisSMallAppWidget);
        Intent smallWidgetIntent = new Intent(context, UpdateService.class);
        smallWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        smallWidgetIntent.putExtra(Config.WIDGET_TYPE, Config.WIDGET_TYPE_SMALL);
        Log.i(TAG, "discovered " + appWidgetIds.length + " " + Config.WIDGET_TYPE_SMALL + " " +  (appWidgetIds.length > 0 ? appWidgetIds[0] : "") );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetIds.length > 0)
               // Util.scheduleJob(context, appWidgetIds, Config.WIDGET_TYPE_SMALL);
              context.startForegroundService(smallWidgetIntent);
        } else {
            context.startService(smallWidgetIntent);
        }
    }


    /**
     * register receiver
     * @param context - Context
     * @param filter - Intent Filter
     * @return see Context.registerReceiver(BroadcastReceiver,IntentFilter)
     */
    public Intent register(Context context, IntentFilter filter) {

            isRegistered = true;
            return context.registerReceiver(this, filter);

    }

    /**
     * unregister received
     * @param context - context
     * @return true if was registered else false
     */
    public boolean unregister(Context context) {
        if (isRegistered) {
            context.unregisterReceiver(this);  // edited
            isRegistered = false;
            return true;
        }
        return false;
    }

    private boolean isScreenOn(final Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 20) {

            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    return true;
                }
            }
        }
        return isInteractive(context);
    }

    private boolean isInteractive(final Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH
                ? powerManager.isInteractive()
                : powerManager.isScreenOn();
    }

}
