package il.co.jws.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

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
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            // do whatever you need to do here
            wasScreenOn = false;
       } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            // and do whatever you need to do here
           wasScreenOn = true;

            ComponentName thisSMallAppWidget = new ComponentName(context.getPackageName(), SmallAppWidgetProvider.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisSMallAppWidget);
            Intent smallWidgetIntent = new Intent(context, UpdateService.class);
            smallWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            smallWidgetIntent.putExtra(Config.WIDGET_TYPE, Config.WIDGET_TYPE_SMALL);
            //PendingIntent pendingService = PendingIntent.getService(context, 0, smallWidgetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            context.startService(smallWidgetIntent);



        } else if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            ComponentName thisRectAppWidget = new ComponentName(context.getPackageName(), RectangleAppWidgetProvider.class.getName());
            int[] rectappWidgetIds = appWidgetManager.getAppWidgetIds(thisRectAppWidget);
            Intent rectwidgetIntent = new Intent(context, UpdateService.class);
            rectwidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, rectappWidgetIds);
            rectwidgetIntent.putExtra(Config.WIDGET_TYPE, Config.WIDGET_TYPE_RECT);
            //PendingIntent rectpendingService = PendingIntent.getService(context, 0, rectwidgetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            context.startService(rectwidgetIntent);
            Log.i(TAG,"userpresent (unlocked)");
        }
        Log.i(TAG,"wasScreenOn "+wasScreenOn);
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

}
