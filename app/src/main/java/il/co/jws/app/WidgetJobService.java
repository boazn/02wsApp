package il.co.jws.app;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.PersistableBundle;

/**
 * JobService to be scheduled by the JobScheduler.
 * start another service
 */
public class WidgetJobService extends JobService {
    private static final String TAG = "WidgetService";

    @Override
    public boolean onStartJob(JobParameters params) {
        PersistableBundle pb = params.getExtras();
        Intent service = new Intent(getApplicationContext(), UpdateService.class);
        service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, pb.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS));
        service.putExtra(Config.WIDGET_TYPE, pb.getString(Config.WIDGET_TYPE));
        getApplicationContext().startService(service);
        //Util.scheduleJob(getApplicationContext()); // reschedule the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

}
