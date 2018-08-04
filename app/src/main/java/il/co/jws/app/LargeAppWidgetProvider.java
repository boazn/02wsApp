package il.co.jws.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

/**
 *
 * @author boaz
 */
public class LargeAppWidgetProvider extends AppWidgetProvider {

    public static final String TAG = "APP_WIDGET";
    public static int appid[];
    public static RemoteViews rview;
    public static Long refTimeMS;
    public static String LARGE_WIDGET_UPDATE = "LARGE_WIDGET_FREQ_UPDATE";

    @Override
    public void onReceive(Context context, Intent intent) {
        // call to super.onReceive to delegate other widget intents
        super.onReceive(context, intent);

        if(LARGE_WIDGET_UPDATE.equals(intent.getAction())){

            Bundle extras = intent.getExtras();
            if(extras!=null) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisAppWidget = new ComponentName(context.getPackageName(), LargeAppWidgetProvider.class.getName());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

                onUpdate(context, appWidgetManager, appWidgetIds);
            }

            //Toast.makeText(context, "onReceiver()", Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.i(TAG, "onUpdate of LargeAppWidgetProvider called");
        // Store following arg for later use	
        appid = appWidgetIds;

                  // Following shows how to make different parts of the widget clickable, leading
        // to different actions for clicking on different parts. This must be done with a
        // PendingIntent, because the widget is a RemoteViews, hosted by the homescreen.
        // Thus we can't just add a click listener as we would for a normal activity.
        rview = new RemoteViews(context.getPackageName(), R.layout.large_appwidget);

        // Add click handling for the widget text that will open the main activity when pressed
        Intent launchAppIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingActivity = PendingIntent.getActivity(context, 0,
                launchAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rview.setOnClickPendingIntent(R.id.btnOpenApp, pendingActivity);

                  // Add click handling for the widget update icon that will update widgets by starting a
        // Service.  Note that a click on the update icon for one instance of the widget will update 
        // all instances if there is more than one instance of the same AppWidget on the 
        // homescreen.
        Intent intent = new Intent(context, UpdateService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        intent.putExtra(Config.WIDGET_TYPE, Config.WIDGET_TYPE_LARGE);
        PendingIntent pendingService = PendingIntent.getService(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        rview.setOnClickPendingIntent(R.id.btnRefreshLargeAppWidget, pendingService);
        rview.setInt(R.id.btnRefreshLargeAppWidget, "setBackgroundColor", android.graphics.Color.TRANSPARENT);

        // Tell widget manager to update with information about the two click listeners
        appWidgetManager.updateAppWidget(appWidgetIds, rview);

        Log.i(TAG, "Click listeners added in onUpdate");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Util.scheduleJob(context, rectappWidgetIds, Config.WIDGET_TYPE_RECT);
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

    }

         // Called when first instance of AppWidget is added to AppWidget host (normally the
    // home screen).
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.i(TAG, "onEnabled of LargeAppWidgetProvider called. refTimeMS = "
                + UpdateService.refTimeMS);
    }

         // Called each time an instance of the AppWidget is removed from the host
    @Override
    public void onDeleted(Context context, int[] appWidgetId) {
        super.onDeleted(context, appWidgetId);
        Log.i(TAG, "Removing instance of AppWidget");
    }

         // Called when last instance of AppWidget is deleted from the AppWidget host.
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.i(TAG, "Removing last AppWidget instance.");
    }
}
