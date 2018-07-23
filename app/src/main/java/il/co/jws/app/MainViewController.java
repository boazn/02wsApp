package il.co.jws.app;

import static android.content.Context.MODE_PRIVATE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.DrawableRes;
import android.util.Log;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.Purchase;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles control logic of the BaseGamePlayActivity
 */
public class MainViewController {
    private static final String TAG = "MainViewController";


    // How many units (1/4 tank is our unit) fill in the tank.
    private static final int TANK_MAX = 4;

    private final UpdateListener mUpdateListener;
    private MainActivity mActivity;

    // Tracks if we currently own subscriptions SKUs
    private boolean mGoldMonthly;
    private boolean mGoldYearly;

    // Tracks if we currently own a premium car
    private boolean mIsPremium;
    private boolean mIsAlertsOnly;
    private Context mContext;
    private FirebaseAnalytics mFirebaseAnalytics;
    // Current amount of gas in tank, in units
    private int mTank;

    public MainViewController(MainActivity activity, Context context, FirebaseAnalytics firebaseAnalytics) {
        mUpdateListener = new UpdateListener();
        mActivity = activity;
        mContext = context;
        mFirebaseAnalytics = firebaseAnalytics;
    }

    public void useGas() {
        mTank--;

        Log.d(TAG, "Tank is now: " + mTank);
    }

    public UpdateListener getUpdateListener() {
        return mUpdateListener;
    }
    public boolean isPremiumPurchased() {
        return mIsPremium;
    }

    public boolean isAlertOnlyPurchased() {
        return mIsAlertsOnly;
    }
    public boolean isGoldMonthlySubscribed() {
        return mGoldMonthly;
    }

    public boolean isGoldYearlySubscribed() {
        return mGoldYearly;
    }

    protected void notifyServerForNotificationChange(Boolean ActionOn, Boolean ActionRainOn, Boolean ActionTipsOn, int lang, int Approved, String BillingToken, String BillingError) {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(Config.SERVER_REGISTER_URL);

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("name", mActivity.getEmailAddress()));
            nameValuePairs.add(new BasicNameValuePair("email", mActivity.getEmailAddress()));
            nameValuePairs.add(new BasicNameValuePair("regId", getRegistrationId(mContext)));
            nameValuePairs.add(new BasicNameValuePair("lang", lang == 1 ? "1" : "0"));
            nameValuePairs.add(new BasicNameValuePair("active", ActionOn ? "1" : "0"));
            nameValuePairs.add(new BasicNameValuePair("active_rain_etc", ActionRainOn ? "1" : "0"));
            nameValuePairs.add(new BasicNameValuePair("active_tips", ActionTipsOn ? "1" : "0"));
            nameValuePairs.add(new BasicNameValuePair("approved", String.valueOf(Approved)));
            nameValuePairs.add(new BasicNameValuePair("BillingToken", BillingToken));
            nameValuePairs.add(new BasicNameValuePair("BillingError", BillingError));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            Log.i(Config.TAG, "NotificationChange ActionOn=" + ActionOn + " ActionRainOn=" + ActionRainOn +  " lang=" + lang + " httppost response: " + response.getStatusLine().getReasonPhrase());

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            printStacktrace(e);

        } catch (IOException e) {
            printStacktrace(e);

        }

    }

    public static void  printStacktrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        Log.d(Config.TAG, exceptionAsString);
        FirebaseCrash.report(e);
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    public String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(Config.PROPERTY_REG_ID, FirebaseInstanceId.getInstance().getToken());
        if (registrationId.isEmpty()) {
            Log.i(Config.TAG, "Registration not found.");
            Bundle bundle = new Bundle();

            bundle.putString("Email", mActivity.getEmailAddress());
            bundle.putString("full_text", "Registration not found");
            mFirebaseAnalytics.logEvent("getRegistrationId", bundle);
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(Config.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(Config.TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    public void trimCache() {
        try {
            File dir = mContext.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
                Log.e(Config.TAG, "Directory " + dir.getName() + " deleted.");
            }
        } catch (Exception e) {
            // TODO: handle exception
            printStacktrace(e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
    }
    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }


    protected void toggleNotifications() {
        SharedPreferences prefs = mContext.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        final int lang = prefs.getInt(Config.PREFS_LANG, 1);
        final boolean boolgetNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS, true);
        final boolean boolgetRainNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS_RAIN, false);
        final boolean boolgetTipsNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS_TIPS, true);
        final String strSubsId = prefs.getString(Config.PREFS_SUB_ID, "");
        final int strApproved = prefs.getInt(Config.PREFS_APPROVED, 0);
        final String strBillingError = prefs.getString(Config.PREFS_BILLING_ERROR, "");
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                notifyServerForNotificationChange(boolgetNotifications, boolgetRainNotifications, boolgetTipsNotifications, lang, strApproved, strSubsId, strBillingError);
                return null;
            }
        }.execute(null, null, null);

    }
    protected void cancelSubscription(String subscriptionId, String token){

        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(Config.SERVER_GOOGLE_PLAY_API_PREF + subscriptionId + "/tokens/" + token + ":cancel");

        try {
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            Log.i(Config.TAG, "cancelSubscription subscriptionId=" + subscriptionId + " token=" + token + "response:" + response.getStatusLine().getReasonPhrase());

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            printStacktrace(e);

        } catch (IOException e) {
            printStacktrace(e);

        }

    }
    public void notifyServerForSubChange(String status) {
        final SharedPreferences prefs = mContext.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        String registrationId = prefs.getString(Config.PROPERTY_REG_ID, FirebaseInstanceId.getInstance().getToken());
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(Config.SERVER_SUB_URL);

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("action", Config.SERVER_SUB_URL_ACTION));
            nameValuePairs.add(new BasicNameValuePair("email", mActivity.getEmailAddress()));
            nameValuePairs.add(new BasicNameValuePair("status", status));
            nameValuePairs.add(new BasicNameValuePair("reg_id", registrationId));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            Log.i(Config.TAG, "notifyServerForSubChange status=" + status + " httppost response: " + response.getStatusLine().getReasonPhrase());

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            printStacktrace(e);

        } catch (IOException e) {
            printStacktrace(e);

        }

    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT > 25) {
            CharSequence name = mContext.getResources().getString(R.string.channel_name);
            String description = mContext.getResources().getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(Config.CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    public void playSound(){
        SharedPreferences prefs = mContext.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        Integer alert_sound_pref = prefs.getInt(Config.PREFS_ALERT_SOUND, R.raw.lighttrainshort);
        MediaPlayer mediaPlayer = MediaPlayer.create(mActivity, alert_sound_pref);
        mediaPlayer.start();
    }
    /**
     * Handler to billing updates
     */
    private class UpdateListener implements BillingManager.BillingUpdatesListener {
        @Override
        public void onBillingClientSetupFinished() {
            mActivity.onBillingManagerSetupFinished();
        }

        @Override
        public void onConsumeFinished(String token, @BillingResponse int result) {
            Log.d(TAG, "Consumption finished. Purchase token: " + token + ", result: " + result);

            // Note: We know this is the SKU_GAS, because it's the only one we consume, so we don't
            // check if token corresponding to the expected sku was consumed.
            // If you have more than one sku, you probably need to validate that the token matches
            // the SKU you expect.
            // It could be done by maintaining a map (updating it every time you call consumeAsync)
            // of all tokens into SKUs which were scheduled to be consumed and then looking through
            // it here to check which SKU corresponds to a consumed token.
            if (result == BillingResponse.OK) {
                // Successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(TAG, "Consumption successful. Provisioning.");

                mActivity.alert(R.string.alert_consume_success, mTank);
            } else {
                Log.d(TAG, token + " result=" + result);
                mActivity.alert(R.string.alert_error_consuming, result);
            }

            Log.d(TAG, "End consumption flow.");
        }

        @Override
        public void onPurchasesUpdated(List<Purchase> purchaseList) {
            mGoldMonthly = false;
            mGoldYearly = false;
            mIsAlertsOnly = false;
            for (Purchase purchase : purchaseList) {
                switch (purchase.getSku()) {
                    case BillingConstants.SKU_AD_FREE:
                        Log.d(TAG, "You are Premium! Congratulations!!!");
                        mIsPremium = true;
                        break;
                    case BillingConstants.SKU_ALERTS:
                        Log.d(TAG, "We have alerts!");
                        // We should consume the purchase and fill up the tank once it was consumed
                        //mActivity.getBillingManager().consumeAsync(purchase.getPurchaseToken());
                        mIsAlertsOnly = true;
                        break;
                    case BillingConstants.SKU_SUB_MONTHLY:
                        mGoldMonthly = true;
                        break;
                    case BillingConstants.SKU_SUB_YEARLY:
                        mGoldYearly = true;
                        break;
                }
            }

            mActivity.showRefreshedUi(purchaseList);
        }
    }
}
