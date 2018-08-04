package il.co.jws.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static il.co.jws.app.MainViewController.printStacktrace;

/**
 *
 * @author boaz
 */
public class SmallAppWidgetProvider extends AppWidgetProvider {

    public static final String TAG = "APP_WIDGET";
    public static int appid[];
    public static RemoteViews rview;
    public static Long refTimeMS;
    public static String SMALL_WIDGET_FREQ_UPDATE = "SMALL_WIDGET_FREQ_UPDATE";
    static AlarmManager myAlarmManager;
    static PendingIntent myPendingIntent;
    private BroadcastReceiver mReceiver;
    private static List<BroadcastReceiver> receivers = new ArrayList<BroadcastReceiver>();

    @Override
    public void onReceive(Context context, Intent intent) {
        // call to super.onReceive to delegate other widget intents
        super.onReceive(context, intent);

        if(SMALL_WIDGET_FREQ_UPDATE.equals(intent.getAction())){

            Bundle extras = intent.getExtras();
            if(extras!=null) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisAppWidget = new ComponentName(context.getPackageName(), SmallAppWidgetProvider.class.getName());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

                onUpdate(context, appWidgetManager, appWidgetIds);
            }

            //Toast.makeText(context, "onReceiver()", Toast.LENGTH_LONG).show();
        }
    }

    // convenience method to count the number of installed widgets
    private int widgetsInstalled(Context context) {
        ComponentName thisWidget = new ComponentName(context, SmallAppWidgetProvider.class);
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        return mgr.getAppWidgetIds(thisWidget).length;
    }
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.i(TAG, "onUpdate of SmallAppWidgetProvider called");
        // Store following arg for later use	
        appid = appWidgetIds;

                  // Following shows how to make different parts of the widget clickable, leading
        // to different actions for clicking on different parts. This must be done with a
        // PendingIntent, because the widget is a RemoteViews, hosted by the homescreen.
        // Thus we can't just add a click listener as we would for a normal activity.
        rview = new RemoteViews(context.getPackageName(), R.layout.small_appwidget);

        // Add click handling for the widget text that will open the main activity when pressed
        Intent launchAppIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingActivity = PendingIntent.getActivity(context, 0,
                launchAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rview.setOnClickPendingIntent(R.id.btnOpenAppSmall, pendingActivity);
        try {
            Intent intent = new Intent(context, UpdateService.class);
            intent.putExtra(Config.WIDGET_TYPE, Config.WIDGET_TYPE_SMALL);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            appWidgetManager.updateAppWidget(appWidgetIds, rview);

            Log.i(TAG, "SmallAppWidgetProvider: Click listeners added in onUpdate");

            // Update the widget displayed content using the Service MyUpdateService
            context.startService(intent);
        }
        catch (Exception e){
            FirebaseCrash.report(e);
        }
        registerScreenReceiver(context.getApplicationContext());
    }

         // Called when first instance of AppWidget is added to AppWidget host (normally the
    // home screen).
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        //prepare Alarm Service to trigger Widget

        Log.i(TAG, "onEnabled of SmallAppWidgetProvider called. refTimeMS = "
                + UpdateService.refTimeMS);
    }

    protected void registerScreenReceiver(Context context){
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        try {
            if (isReceiverRegistered(mReceiver))
                return;
            mReceiver = new ScreenReceiver();
            context.registerReceiver(mReceiver, filter);
            receivers.add(mReceiver);

        }
        catch (IllegalArgumentException e){
            printStacktrace(e);
        }
    }

    public boolean isReceiverRegistered(BroadcastReceiver receiver){
        boolean registered = receivers.contains(receiver);
        Log.i(getClass().getSimpleName(), "is receiver "+receiver+" registered? "+registered);
        return registered;
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
    static void SaveAlarmManager(AlarmManager tAlarmManager, PendingIntent tPendingIntent)
    {
        myAlarmManager = tAlarmManager;
        myPendingIntent = tPendingIntent;
    }
}
