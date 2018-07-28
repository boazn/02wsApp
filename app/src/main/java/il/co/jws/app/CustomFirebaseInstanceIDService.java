package il.co.jws.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Patterns;


import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CustomFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = "FirebaseService";
    private int MY_PERMISSIONS_REQUEST_GET_ACCOUNT;
    @Override
    public void onTokenRefresh() {
        try {
            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            Log.d(TAG, "Token Value: " + refreshedToken);
            FirebaseCrash.logcat(1 , FirebaseInstanceId.getInstance().getId() , "Token Value: " + refreshedToken);
            storeRegistrationId(refreshedToken);
            sendTheRegisteredTokenToWebServer(refreshedToken);

        }
        catch (Exception e ){
            FirebaseCrash.report(e);
        }

   }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *    *
     * @param token registration ID
     */
    private void storeRegistrationId(final String token) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        int appVersion = MainViewController.getAppVersion(getApplicationContext());
        Log.i(Config.TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Config.PROPERTY_REG_ID, token);
        editor.putInt(Config.PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }
    private void sendTheRegisteredTokenToWebServer(final String token){

        if (token == null)
            return;
        if (token.isEmpty())
            return;
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        final boolean ActionOn = prefs.getBoolean(Config.PREFS_NOTIFICATIONS, true);
        final boolean ActionRainOn = prefs.getBoolean(Config.PREFS_NOTIFICATIONS_RAIN, true);
        final boolean ActionTipsOn = prefs.getBoolean(Config.PREFS_NOTIFICATIONS_TIPS, true);
        final int lang = prefs.getInt(Config.PREFS_LANG, 1);
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(Config.SERVER_REGISTER_URL);

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            //nameValuePairs.add(new BasicNameValuePair("name", getEmailAddress()));
            //nameValuePairs.add(new BasicNameValuePair("email", getEmailAddress()));
            nameValuePairs.add(new BasicNameValuePair("regId", token));
            nameValuePairs.add(new BasicNameValuePair("lang", lang == 1 ? "1" : "0"));
            nameValuePairs.add(new BasicNameValuePair("active", ActionOn ? "1" : "0"));
            nameValuePairs.add(new BasicNameValuePair("active_rain_etc", ActionRainOn ? "1" : "0"));
            nameValuePairs.add(new BasicNameValuePair("active_tips", ActionTipsOn ? "1" : "0"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            Log.i(Config.TAG, "NotificationChange ActionOn=" + ActionOn + " ActionRainOn=" + ActionRainOn +  " lang=" + lang + " httppost response: " + response.getStatusLine().getReasonPhrase());

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block

        } catch (IOException e) {

        }


    }


}