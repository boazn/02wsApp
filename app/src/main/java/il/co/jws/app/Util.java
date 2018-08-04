package il.co.jws.app;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.util.Log;

public class Util {

    public static void scheduleJob(Context context, int[] appWidgetIds, String str_WIDGET_TYPE) {
        ComponentName serviceComponent = new ComponentName(context, WidgetJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        bundle.putString(Config.WIDGET_TYPE, str_WIDGET_TYPE);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(500) // wait at least
                .setOverrideDeadline(3 * 1000) // maximum delay
                .setExtras(bundle);
                //.setPersisted(true);
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler =  (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        Log.v("JOB", "scheduleJob: " + bundle.getString(Config.WIDGET_TYPE) + " length:" + bundle.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS).length);
        jobScheduler.schedule(builder.build());
    }

}
