package il.co.jws.app;


import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Date;
import java.util.Random;

public class FcmBroadcastReceiver extends com.google.firebase.messaging.FirebaseMessagingService{
	public static final int NOTIFICATION_ID  = new Random().nextInt(); ;
    private NotificationManager mNotificationManager;
    private FirebaseAnalytics mFirebaseAnalytics;
    NotificationCompat.Builder builder;
    public static final String MESSAGE_BODY = "message";
    public static final String MESSAGE_TITLE = "title";
    public static final String PICTURE_URL = "picture_url";
    public static final String EMBEDDED_URL = "embedded_url";
    RemoteViews contentViewBig,contentViewSmall;
    private static final String TAG = "02wsFirebaseMsgService";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(com.google.firebase.messaging.RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        Log.i(Config.TAG, "Received: " + remoteMessage.getData().toString());
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        SharedPreferences prefs =   this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        SendNotification Notify = new SendNotification(this, remoteMessage.getData().get(MESSAGE_BODY), remoteMessage.getData().get(MESSAGE_TITLE), remoteMessage.getData().get(PICTURE_URL), remoteMessage.getData().get(EMBEDDED_URL), prefs);
        Notify.Send();
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }


    // [END receive_message]


	// Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private class SendNotification  {
            private final Context context;
            private final String msg;
            private final String title;
            private final String picture_url;
            private final String embedded_url;
            private final SharedPreferences prefs;

            public SendNotification(Context p_context, String p_msg, String p_title, String p_picture_url, String p_embedded_url, SharedPreferences p_prefs){
                context = p_context;
                msg = p_msg;
                title = p_title;
                picture_url = p_picture_url;
                embedded_url = p_embedded_url;
                prefs = p_prefs;
            }

             Void Send() {

                 mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);


                 PendingIntent piDismiss = NotificationActivity.getDismissIntent(NOTIFICATION_ID, context);

                 Intent shareIntent = new Intent(Intent.ACTION_SEND);
                 shareIntent.setType("text/plain");
                 shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_name) + ": " + title + " - " + msg);
                 shareIntent.putExtra(Intent.EXTRA_TITLE, title);
                 shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);


                Intent notificationIntent = new Intent(context, MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                notificationIntent.putExtra("IS_FROM_ALERT", Boolean.valueOf(true));
                PendingIntent piMain = PendingIntent.getActivity(context, 0 /* Request code */, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                 PendingIntent piShare = PendingIntent.getActivity(context, 0, Intent.createChooser(shareIntent, getString(R.string.share_title)), PendingIntent.FLAG_UPDATE_CURRENT);

                 final Intent ReplyIntent = new Intent(context, MainActivity.class);
                 ReplyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                 ReplyIntent.putExtra("REPLY_FROM_ALERT", Boolean.valueOf(true));
                 PendingIntent piReply = PendingIntent.getActivity(context, 1 /* Request code */, ReplyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                 Integer alert_sound_pref = prefs.getInt(Config.PREFS_ALERT_SOUND, R.raw.lighttrainshort);
                 Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + alert_sound_pref);
                 Bitmap remote_picture = null;
                 contentViewBig = new RemoteViews(getPackageName(), R.layout.custom_notification);
                 contentViewSmall = new RemoteViews(getPackageName(),R.layout.custom_notification_small);
                 contentViewBig.setOnClickPendingIntent(R.id.reply, piReply);
                 contentViewBig.setOnClickPendingIntent(R.id.share, piShare);
                 contentViewBig.setOnClickPendingIntent(R.id.dismiss, piDismiss);


                 // Create the style object with BigPictureStyle subclass.
                NotificationCompat.BigPictureStyle picStyle = new
                        NotificationCompat.BigPictureStyle();

                NotificationCompat.BigTextStyle textStyle = new NotificationCompat.BigTextStyle();
                textStyle.setBigContentTitle(title);
                textStyle.bigText(msg);
                textStyle.setSummaryText(context.getString(R.string.app_name));

                 contentViewBig.setTextViewText(R.id.title, title);
                 contentViewSmall.setTextViewText(R.id.title, title);
                 contentViewBig.setTextViewText(R.id.text, msg);
                 contentViewSmall.setTextViewText(R.id.text, msg);
                 contentViewBig.setImageViewBitmap(R.id.image_app, ((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher, null)).getBitmap() );
                 contentViewSmall.setImageViewBitmap(R.id.image_app, ((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher, null)).getBitmap());
                try {
                    if ((picture_url != null)&&(!picture_url.isEmpty())) {
                        remote_picture = BitmapFactory.decodeStream(
                                (InputStream) new URL(picture_url).getContent());
                        contentViewBig.setImageViewBitmap(R.id.image_pic, BitmapFactory.decodeStream((InputStream) new URL(picture_url).getContent()));
                    }
                    if (remote_picture != null)
                        picStyle.bigPicture(remote_picture)
                                .bigLargeIcon(null);


                } catch (IOException e) {
                    printStacktrace(e);
                }
                long[] v = {500,1000};
                Boolean vibration = prefs.getBoolean(Config.PREFS_VIBRATION, true);
                if (!vibration)
                    v = null;
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(context, Config.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(remote_picture)
                .setContentTitle(title)
                .setContentText(msg)
                .setSubText(new Date().toString())
                .setAutoCancel(true)
                .setVibrate(v)
                .setContentIntent(piMain)
                .setCategory(Config.CHANNEL_ID)
                //.setCustomContentView(contentViewSmall)
                .setCustomBigContentView(contentViewBig)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(android.app.Notification.PRIORITY_MAX);

                if (remote_picture == null)
                    mBuilder.setStyle(textStyle);
                else
                    mBuilder.setStyle(picStyle);
                 mBuilder.addAction (R.drawable.ic_clear_black_24dp,
                         getString(R.string.dismiss), piDismiss)
                .addAction (R.drawable.ic_action,
                                 getString(R.string.share_title), piShare)
                 .addAction (R.drawable.ic_reply,
                         getString(R.string.reply_title), piReply);

                 mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
                 Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), soundUri);
                 if (r != null)
                    r.play();
                return null;
             }
        };

        private void printStacktrace(Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            Log.d(Config.TAG, exceptionAsString);
            FirebaseCrash.report(e);
        }

    }



    
    
    
	
