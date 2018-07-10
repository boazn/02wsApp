/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package il.co.jws.app;


import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Spinner;


/**
 *
 * @author boaz
 */
public class LargeAppWidgetConfigActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private int appWidgetId;
    private Context context;
    public EditText titleText;
    public String unitsString;
    public String titleString;
    public static final String TAG = "APP_WIDGET";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "\nBegin configuration with ConfigWidget");
        setContentView(R.layout.appwidget_config);

            // This prevents widget from being installed if user backs out of configuration
        // activity before completion (returning RESULT_CANCELED causes the AppWidget
        // to not be installed)
        this.setResult(RESULT_CANCELED);

        context = this.getApplicationContext();

        // Get the ID for the AppWidget being configured from the Intent that launched the Activity
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

            // Set an intent for the Cancel button  (returning RESULT_CANCELED causes the AppWidget
        // to not be installed)
        Intent cancelIntent = new Intent();
        cancelIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, cancelIntent);

        // Identify and set listener for units spinner
        Spinner unitsSpinner = (Spinner) findViewById(R.id.unitsSpinner);
        unitsSpinner.setOnItemSelectedListener(this);

        // Populate the units spinner with options defined in the array res/values/arrays.xml
        ArrayAdapter<CharSequence> aa = ArrayAdapter.createFromResource(this,
                R.array.units, android.R.layout.simple_spinner_item);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitsSpinner.setAdapter(aa);

        // Click handling for the Cancel button
        Button cancel = (Button) findViewById(R.id.cancelbutton);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Click handling for the OK button        
        Button ok = (Button) findViewById(R.id.okbutton);
        ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // Do the intial update after configuration
                initialUpdate();

                      // Return OK result and app widget id; see
                // http://developer.android.com/guide/topics/appwidgets/
                // index.html#UpdatingFromTheConfiguration. (Returning RESULT_OK causes the
                // AppWidget to be installed.)
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);
                Log.i(TAG, "End of configuration");
                finish();
            }
        });
    }

    // Method to perform the initial update after the widget is configured. Necessary
    // because if there is a configuration activity the System skips the initial update and
    // it is the responsibility of the programmer to provide it (the System automatically
    // supplies updates after the first one though).
    public void initialUpdate() {
        Log.i(TAG, "  Starting initial update after configure, this id = " + appWidgetId);
        AppWidgetManager apw = AppWidgetManager.getInstance(context);

        // Construct remote view that will define look of widget
        RemoteViews rview = new RemoteViews(getPackageName(), R.layout.large_appwidget);

        // Add click handling for the widget text that will open the main activity when pressed
        Intent launchAppIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingActivity = PendingIntent.getActivity(context, 0,
                launchAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rview.setOnClickPendingIntent(R.id.btnOpenApp, pendingActivity);

             // Add click handling for the widget update icon that will update widgets by starting a
        // service. 
        int[] ids = LargeAppWidgetProvider.appid;
        Log.i(TAG, "  List of current AppWidgets:");
        for (int i = 0; i < ids.length; i++) {
            Log.i(TAG, "    ids = " + ids[i]);
            new DownloadBitmap(rview, ids[i], apw, context).execute("MyTestString");
        }
        Intent intentUpdate = new Intent(context, UpdateService.class);
        intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

             // Following hack suggested by comments of "Snailer" at
        // http://stackoverflow.com/questions/4011178/multiple-instances-of-widget
        // -only-updating-last-widget
        // See also http://www.bogdanirimia.ro/android-widget-click-event-multiple-instances/269
        // If the following two lines are not included and more than one instance of the AppWidget
        // is added to the desktop, clicking update on any of them will update only the last one
        // added, until the System does the first update on the AppWidgets.  After that, clicking
        // any instance will update all, as expected.  The issue appears to be Android reuse of
        // Intents.  The purpose of the hack is to allow the PendingIntents to be distinguishable
        // for different instances (since their Intent arguments carry different data). With this hack,
        // now before the first System update each instance of the AppWidget will update separately 
        // if its update button is clicked. After first System update, clicking any instance will update all.
        // It appears that if you add several AppWidget instances after a System update on
        // already existing instances, until the next System update each of the new instances will
        // update separately if you click on it, but clicking on one of the old instances will update 
        // only the last new instance added (until the next System update, when again clicking on any
        // widget will cause all to update). Note: the "xxx" in the following is an arbitrary string.
        Uri data = Uri.withAppendedPath(Uri.parse("largeAppWigdet" + "://widget/id/"), String.valueOf(appWidgetId));
        intentUpdate.setData(data);
        PendingIntent pendingService = PendingIntent.getService(context,
                0, intentUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
        rview.setOnClickPendingIntent(R.id.btnRefreshLargeAppWidget, pendingService);
        rview.setInt(R.id.btnRefreshLargeAppWidget, "setBackgroundColor", android.graphics.Color.TRANSPARENT);
        // Tell widget manager to update with the new information
        apw.updateAppWidget(appWidgetId, rview);
    }

    // Spinner events
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "  Spinner, position=" + position + " id=" + view.getId() + " id=" + R.id.unitsSpinner);
        switch (position) {
            case 0:
                unitsString = "Minutes";
                break;
            case 1:
                unitsString = "Hours";
                break;
            case 2:
                unitsString = "Days";
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {

    }

}
