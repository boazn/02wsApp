package il.co.jws.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import android.support.v7.app.ActionBarDrawerToggle;
import android.widget.Toast;

public class MainActivity extends Activity {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private WebView webview;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private String[] mNavTitles;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private int lang;
    private FirebaseAnalytics mFirebaseAnalytics;
    private String tempunit;
    Context context;
    String regid;
    Menu mmenu;
    AtomicInteger msgId = new AtomicInteger();
    boolean isFromAlerts = false;
    boolean replyFromAlerts = false;
    boolean isFromUpload = false;
    boolean isFromAdFreeCode = false;
    private BroadcastReceiver mReceiver;
    private int MY_PERMISSIONS_REQUEST_GET_ACCOUNT;
    private int index;

    @Override
    protected  void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // getIntent() should always return the most recent
        setIntent(intent);
        //coming from notifications
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isFromAlerts = extras.getBoolean("IS_FROM_ALERT", false);
            replyFromAlerts = extras.getBoolean("REPLY_FROM_ALERT", false);
            isFromUpload = extras.getBoolean("IS_FROM_UPLOAD", false);
            isFromAdFreeCode = extras.getBoolean(Config.IS_FROM_ADFREE_ACTIVITY, false);
        }
        if (replyFromAlerts)
            doUrl("SendEmailForm.php");
        else if (isFromAlerts)
            doRefresh(isFromAlerts, false);
        else if (isFromAdFreeCode)
            doRefresh(true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseMessaging.getInstance().subscribeToTopic("02wsMessages");
        FirebaseInstanceId.getInstance().getToken();
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        registerScreenReceiver();
        // Get Info from outside
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isFromAlerts = extras.getBoolean("IS_FROM_ALERT", false);
            replyFromAlerts = extras.getBoolean("REPLY_FROM_ALERT", false);
        }
        Log.i(Config.TAG, "isFromAlerts=" + isFromAlerts + " replyFromAlerts=" + replyFromAlerts);
        // Restore preferences
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        lang = prefs.getInt(Config.PREFS_LANG, 1);
        //display
        setContentView(R.layout.activity_main);
        mNavTitles = getResources().getStringArray(R.array.navItems);
        TypedArray typedArray = getResources().obtainTypedArray(R.array.array_drawer_icons);
        NavDrawerItem[] NavDrawerList = new NavDrawerItem[mNavTitles.length];
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        for (int i = 0; i < mNavTitles.length; i++) {

            NavDrawerItem item = new NavDrawerItem(typedArray.getResourceId(i, -1), mNavTitles[i]);
            NavDrawerList[i] = item;
        }

        // Set the adapter for the list view
        mDrawerList.setAdapter(new NavDrawerAdapter(this, R.layout.drawer_list_item, NavDrawerList));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getActionBar().setTitle(mTitle);
                Log.v(Config.TAG, "onDrawerClosed");

            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getActionBar().setTitle(mDrawerTitle);
                Log.v(Config.TAG, "onDrawerOpened");
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doRefresh(false);
            }
        });
        float scale = getResources().getConfiguration().fontScale;
        webview = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = webview.getSettings();
        if  (android.os.Build.VERSION.SDK_INT >= 14) {
                webSettings.setTextZoom(prefs.getInt(Config.PREFS_ZOOM_TEXT, 100));
        }
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setAppCacheEnabled(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setGeolocationEnabled(false);
        webSettings.setNeedInitialFocus(false);
        webSettings.setSaveFormData(false);
        if  (android.os.Build.VERSION.SDK_INT >= 11){
            webSettings.setBuiltInZoomControls(true);
            webSettings.setSupportZoom(true);
            webSettings.setDisplayZoomControls(false);
        }
        CookieManager.getInstance().setAcceptCookie(true);
        if  (android.os.Build.VERSION.SDK_INT >= 21){
            CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true);
        }
        // Force links and redirects to open in the WebView instead of in a browser
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onReceivedError(final WebView view, int errorCode, String description,
                    final String failingUrl) {
                webview.loadUrl("file:///android_asset/networkerror.html");
            }
        });
         webview.setOnTouchListener(new WebView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        Log.v(Config.TAG, "ACTION_MOVE" + webview.getScrollY());
                        if (webview.getScrollY() > 0) {
                            mSwipeRefreshLayout.setEnabled(false);
                        } else {
                            mSwipeRefreshLayout.setEnabled(true);
                        }
                        break;

                    case MotionEvent.ACTION_DOWN:
                        Log.v(Config.TAG, "ACTION_DOWN" + webview.getScrollY());
                        if (webview.getScrollY() > 0) {
                            mSwipeRefreshLayout.setEnabled(false);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.v(Config.TAG, "ACTION_UP" + webview.getScrollY());
                    case MotionEvent.ACTION_CANCEL:
                        Log.v(Config.TAG, "ACTION_CANCEL" + webview.getScrollY());
                        mSwipeRefreshLayout.setEnabled(true);
                        break;
                }
                return false;
            }
        });
        if (replyFromAlerts)
            doUrl("SendEmailForm.php");
       else if (isFromAlerts)
            doRefresh(isFromAlerts, false);
        if (isFromUpload) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.uploaded)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do things
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mmenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        // Restore preferences
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        lang = prefs.getInt(Config.PREFS_LANG, 1);
        tempunit = prefs.getString(Config.PREFS_TEMPUNIT, Config.C_TEMPUNIT);
        boolean boolgetNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS, true);
        boolean boolgetRainNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS_RAIN, true);
        boolean boolgetTipsNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS_TIPS, true);
        boolean boolVibration = prefs.getBoolean(Config.PREFS_VIBRATION, true);
        boolean boolSound = prefs.getBoolean(Config.PREFS_SOUND, false);
        boolean boolFulltext = prefs.getBoolean(Config.PREFS_FULLTEXT, false);
        boolean boolShowCloth = prefs.getBoolean(Config.PREFS_CLOTH, true);
        Integer alertSound = prefs.getInt(Config.PREFS_ALERT_SOUND, R.raw.lightrain);
        MenuItem NotifItem = menu.findItem(R.id.action_notifications);
        MenuItem NotifRainItem = menu.findItem(R.id.action_rain_notifications);
        MenuItem NotifTips = menu.findItem(R.id.action_tips_notifications);
        MenuItem LangItem = menu.findItem(R.id.action_language);
        MenuItem TempItem = menu.findItem(R.id.action_temp);
        MenuItem AlertSoundItem = menu.findItem(R.id.action_alertsound);
        MenuItem SoundItem = menu.findItem(R.id.action_sound);
        MenuItem VibrationItem = menu.findItem(R.id.action_vibration);
        MenuItem FullTextItem = menu.findItem(R.id.action_force_fulltext);
        MenuItem ClothItem = menu.findItem(R.id.action_cloth);
        SubMenu LangSubMenu = LangItem.getSubMenu();
        SubMenu TempSubMenu = TempItem.getSubMenu();
        SubMenu AlertSoundSubMenu = AlertSoundItem.getSubMenu();
        MenuItem ItemToBeChecked = LangSubMenu.getItem(lang);
        MenuItem TempItemToBeChecked;
        if (tempunit.charAt(1) == Config.C_TEMPUNIT.charAt(1))
            TempItemToBeChecked = TempSubMenu.findItem(R.id.action_c);
        else
            TempItemToBeChecked = TempSubMenu.findItem(R.id.action_f);
        ItemToBeChecked.setChecked(true);
        TempItemToBeChecked.setChecked(true);
        MenuItem AlertSoundToBeChecked = AlertSoundSubMenu.findItem(R.id.action_lighttrain);
        if (alertSound == R.raw.lighttrainshort)
            AlertSoundToBeChecked = AlertSoundSubMenu.findItem(R.id.action_lighttrainshort);
        else if (alertSound == R.raw.vop)
            AlertSoundToBeChecked = AlertSoundSubMenu.findItem(R.id.action_vop);
        else if (alertSound == R.raw.lightrainbell)
            AlertSoundToBeChecked = AlertSoundSubMenu.findItem(R.id.action_lighttrainbell);
        else if (alertSound == R.raw.lightrainpassing)
            AlertSoundToBeChecked = AlertSoundSubMenu.findItem(R.id.action_lighttrainpassing);
        else if (alertSound == R.raw.jerofgold)
            AlertSoundToBeChecked = AlertSoundSubMenu.findItem(R.id.action_JerOfGold);
        else if (alertSound == R.raw.jerofgolddrugs)
            AlertSoundToBeChecked = AlertSoundSubMenu.findItem(R.id.action_JerOfGoldDrugs);
        else if (alertSound == 0)
            AlertSoundToBeChecked = AlertSoundSubMenu.findItem(R.id.action_nosound);
        AlertSoundToBeChecked.setChecked(true);
        NotifItem.setChecked(boolgetNotifications);
        NotifRainItem.setChecked(boolgetRainNotifications);
        NotifTips.setChecked(boolgetTipsNotifications);
        SoundItem.setChecked(boolSound);
        VibrationItem.setChecked(boolVibration);
        FullTextItem.setChecked(boolFulltext);
        ClothItem.setChecked(boolShowCloth);
        final MenuItem shareItem = menu.findItem(R.id.menu_item_share);
        shareItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String Imageurl = Config.URL_SHORT;
                if (lang == 0) {
                    Imageurl = Config.URL_SHORT_ENG;
                }
                Bundle bundle = new Bundle();
                bundle.putString("Email", getEmailAddress());
                bundle.putString("full_text", "clicked on share");
                //bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
                mFirebaseAnalytics.logEvent("share_action_bar", bundle);
                new ShareImageTask(getApplicationContext(), MainActivity.this, shareItem).execute(Imageurl);
                return true;
            }
        });
        doRefresh(isFromAlerts, false);
        return true;
    }

    @Override
   protected void onDestroy() {
      super.onDestroy();
      try {
         trimCache(); //if trimCache is static
      } catch (Exception e) {
        Log.i(Config.TAG, "onDestroy" + e.getMessage());
        FirebaseCrash.report(e);
      }
    }
   
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Bundle bundle = new Bundle();
        bundle.putString("Email", getEmailAddress());


        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
       if (item.isCheckable()){
            if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
            }
        }
        if (item.getItemId() == R.id.action_sound){
            editor.putBoolean(Config.PREFS_SOUND, item.isChecked());
            editor.commit();
            doRefresh(true);
            return true;
        }
        else if (item.getItemId() == R.id.action_cloth){
            editor.putBoolean(Config.PREFS_CLOTH, item.isChecked());
            editor.commit();
            doRefresh(true);
            return true;
        }
        else if (item.getItemId() == R.id.action_force_fulltext) {
            editor.putBoolean(Config.PREFS_FULLTEXT, item.isChecked());
            editor.commit();
            doRefresh(true);
            return true;
         }
         else if (item.getItemId() == R.id.action_vibration){
              editor.putBoolean(Config.PREFS_VIBRATION, item.isChecked());
              editor.commit();
         }
         else if (item.getItemId() == R.id.action_hebrew) {
            changeLang(item, 1);
            return true;
        }

         else if (item.getItemId() == R.id.action_english) {
            changeLang(item, 0);
            return true;
        
        }
        else if (item.getItemId() == R.id.action_c) {
            editor.putString(Config.PREFS_TEMPUNIT, Config.C_TEMPUNIT);
            editor.commit();
            doRefresh(true);
            return true;
        }
        else if (item.getItemId() == R.id.action_f) {
            editor.putString(Config.PREFS_TEMPUNIT, Config.F_TEMPUNIT);
            editor.commit();
            doRefresh(true);
            return true;
        }
         else if (item.getItemId() == R.id.action_opencamera) {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra("type", Config.CAMERA_CHOSEN);
            this.startActivity(intent);
         }
        else if (item.getItemId() == R.id.action_opengallery) {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra("type", Config.GALLERY_CHOSEN);
            this.startActivity(intent);
        }
        else if (item.getItemId() == R.id.change_font) {

            bundle.putString("full_text", "clicked on change_font");
            mFirebaseAnalytics.logEvent("change_font", bundle);
            Intent intent = new Intent(this, FontSizeActivity.class);
            this.startActivity(intent);
        }
        else if (item.getItemId() == R.id.enter_code) {

            bundle.putString("full_text", "clicked on enter_code");
            mFirebaseAnalytics.logEvent("enter_code", bundle);
            Intent intent = new Intent(this, AdFreeCodeActivity.class);
            this.startActivity(intent);
        }
        else if (item.getItemId() == R.id.action_lighttrain) {
            bundle.putString("full_text", "clicked on action_lighttrain");
            mFirebaseAnalytics.logEvent("action_lighttrain", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.lightrain);
            editor.commit();
            playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_lighttrainbell) {
            bundle.putString("full_text", "clicked on action_lighttrainbell");
            mFirebaseAnalytics.logEvent("action_lighttrainbell", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.lightrainbell);
            editor.commit();
            playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_lighttrainpassing) {
            bundle.putString("full_text", "clicked on action_lighttrainpassing");
            mFirebaseAnalytics.logEvent("action_lighttrainpassing", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.lightrainpassing);
            editor.commit();
            playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_lighttrainshort) {
            bundle.putString("full_text", "clicked on action_lighttrainshort");
            mFirebaseAnalytics.logEvent("action_lighttrainshort", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.lighttrainshort);
            editor.commit();
            playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_JerOfGold) {
            bundle.putString("full_text", "clicked on action_JerOfGold");
            mFirebaseAnalytics.logEvent("action_JerOfGold", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.jerofgold);
            editor.commit();
            playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_JerOfGoldDrugs) {
            bundle.putString("full_text", "clicked on action_JerOfGoldDrugs");
            mFirebaseAnalytics.logEvent("action_JerOfGoldDrugs", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.jerofgolddrugs);
            editor.commit();
            playSound();
            return true;
        }

        else if (item.getItemId() == R.id.action_vop) {
            bundle.putString("full_text", "clicked on action_vop");
            mFirebaseAnalytics.logEvent("action_vop", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.vop);
            editor.commit();
            playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_nosound) {
            bundle.putString("full_text", "clicked on action_nosound");
            mFirebaseAnalytics.logEvent("action_nosound", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, 0);
            editor.commit();
            return true;
        }
        else if (item.getItemId() == R.id.action_notifications) {
            editor.putBoolean(Config.PREFS_NOTIFICATIONS, item.isChecked());
            editor.commit();
            toggleNotifications();
            return true;
        }
        else if (item.getItemId() == R.id.action_tips_notifications){
             editor.putBoolean(Config.PREFS_NOTIFICATIONS_TIPS, item.isChecked());
             editor.commit();
             toggleNotifications();
             return true;
        
        }
        else if (item.getItemId() == R.id.action_rain_notifications) {
              editor.putBoolean(Config.PREFS_NOTIFICATIONS_RAIN, item.isChecked());
              editor.commit();
              toggleNotifications();
              return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
    
    protected void playSound(){
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        Integer alert_sound_pref = prefs.getInt(Config.PREFS_ALERT_SOUND, R.raw.lighttrainshort);
        MediaPlayer mediaPlayer = MediaPlayer.create(this, alert_sound_pref);
        mediaPlayer.start(); 
    }
    
    protected void doRefresh(Boolean newSettings) {
        doRefresh(false, newSettings);
    }
    protected void doRefresh(Boolean isFromAlerts, Boolean newSettings) {
        webview = (WebView) findViewById(R.id.webview);
        if (webview != null){
            if (isFromAlerts)
                webview.loadUrl(Config.ALERTS_URL_BASE + buildQueryString());
            else if ((webview.getUrl() == null)||newSettings ||(webview.getUrl() != null && !webview.getUrl().contains("02ws"))){
                Log.i(Config.TAG, "loadUrl= " + Config.SITE_URL_BASE + buildQueryString());
                webview.loadUrl(Config.SITE_URL_BASE + buildQueryString());
            }
            else{
                Log.i(Config.TAG, "loadUrl= " + webview.getUrl());
                webview.reload();
            }
        }

    }

    protected String buildQueryString(){
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        boolean boolSound = prefs.getBoolean(Config.PREFS_SOUND, false);
        boolean boolFulltext = prefs.getBoolean(Config.PREFS_FULLTEXT, false);
        boolean boolShowCloth = prefs.getBoolean(Config.PREFS_CLOTH, true);
        String adFreeGUID = prefs.getString(Config.PREFS_SUBGUID, "");
        tempunit = prefs.getString(Config.PREFS_TEMPUNIT, Config.C_TEMPUNIT);
        lang = prefs.getInt(Config.PREFS_LANG, 1);
        String fulltext = "&fullt=", sound = "&s=", cloth = "&c=", temp = "&tempunit=" + tempunit, guid;
        guid = (adFreeGUID.length() > 0) ? "&reg_id=" + getRegistrationId(this) : null;
        fulltext += boolFulltext ? "1" : "0";
        sound += boolSound ? "1" : "0";
        cloth += boolShowCloth ? "1" : "0";
        return lang + fulltext + sound + cloth + temp + guid;
    }

    protected void doUrl (String section){
        String urlext;
        webview = (WebView) findViewById(R.id.webview);
        if (section != "")
            urlext = buildQueryString() + "&section=" + section + "&email=" + getEmailAddress();
        else
            urlext = buildQueryString();
        webview.loadUrl(Config.SITE_URL_BASE + urlext);
    }

    @Override
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
            return;
        }
       // Otherwise defer to system default behavior.
        super.onBackPressed();
    }

    protected void changeLang(MenuItem item, final int lang) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Config.PREFS_LANG, lang);
        editor.commit();
        item.setChecked(true);
        doRefresh(true);
        final boolean boolgetNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS, true);
        final boolean boolgetRainNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS_RAIN, true);
        final boolean boolgetTipsNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS_TIPS, true);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                notifyServerForNotificationChange(boolgetNotifications, boolgetRainNotifications, boolgetTipsNotifications, lang);
                return null;
            }
        }.execute(null, null, null);
    }

    protected void toggleNotifications() {
        final MenuItem NotifActionItem = mmenu.findItem(R.id.action_notifications);
        final MenuItem NotifActionRainItem = mmenu.findItem(R.id.action_rain_notifications);
        final MenuItem NotifActionTipsItem = mmenu.findItem(R.id.action_tips_notifications);
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        lang = prefs.getInt(Config.PREFS_LANG, 1);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                notifyServerForNotificationChange(NotifActionItem.isChecked(), NotifActionRainItem.isChecked(), NotifActionTipsItem.isChecked(), lang);
                return null;
            }
        }.execute(null, null, null);

    }

    public String getEmailAddress() {
        if((ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions
                    (MainActivity.this, new String[]{
                            android.Manifest.permission.GET_ACCOUNTS
                    },MY_PERMISSIONS_REQUEST_GET_ACCOUNT);
        }
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                return account.name;

            }
        }
        return null;
    }

    protected void notifyServerForNotificationChange(Boolean ActionOn, Boolean ActionRainOn, Boolean ActionTipsOn, int lang) {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(Config.SERVER_REGISTER_URL);

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("name", getEmailAddress()));
            nameValuePairs.add(new BasicNameValuePair("email", getEmailAddress()));
            nameValuePairs.add(new BasicNameValuePair("regId", getRegistrationId(getApplicationContext())));
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
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(Config.PROPERTY_REG_ID, FirebaseInstanceId.getInstance().getToken());
        if (registrationId.isEmpty()) {
            Log.i(Config.TAG, "Registration not found.");
            Bundle bundle = new Bundle();

            bundle.putString("Email", getEmailAddress());
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

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

        public class ShareImageTask extends AsyncTask<String, String, String> {

        final private Context context;
        final private MenuItem shareditem;
        private ProgressDialog pDialog;
        String image_url;
        URL myFileUrl;
        String myFileUrl1;
        Bitmap bmImg = null;
        Intent share;
        File file;

        

        private ShareImageTask(Context applicationContext, MainActivity aThis, MenuItem shareItem) {
            this.context = aThis;
            this.shareditem = shareItem;
        }

      

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub

            super.onPreExecute();

             pDialog = new ProgressDialog(context);
             pDialog.setMessage(getString(R.string.loading) + "...");
             pDialog.setIndeterminate(false);
             pDialog.setCancelable(false);
             pDialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
            // TODO Auto-generated method stub

            /*try {

                myFileUrl = new URL(args[0]);
                HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                bmImg = BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                printStacktrace(e);
            }
            try {
                if (!isExternalStorageWritable()) {
                    return null;
                }
                String path = myFileUrl.getPath();
                String idStr = path.substring(path.lastIndexOf('/') + 1);
                File filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File dir = new File(filepath.getAbsolutePath());
                dir.mkdirs();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String fileName = idStr + '?' + timeStamp;
                file = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                bmImg.compress(CompressFormat.PNG, 85, fos);
                fos.flush();
                fos.close();

            } catch (Exception e) {
                printStacktrace(e);
            }*/
            return null;
        }

        /* Checks if external storage is available for read and write */
        public boolean isExternalStorageWritable() {
            String state = Environment.getExternalStorageState();
            return Environment.MEDIA_MOUNTED.equals(state);
        }

        @Override
        protected void onPostExecute(String args) {
            // TODO Auto-generated method stub
            pDialog.dismiss();
            SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
            int lang = prefs.getInt(Config.PREFS_LANG, 1);
            share = new Intent(Intent.ACTION_SEND);
            share.setType("text/html");//"image/png"
            //share.putExtra(Intent.EXTRA_STREAM, Uri.parse(file.getAbsolutePath()));
            share.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_extra_text) + lang);
            share.putExtra(Intent.EXTRA_TITLE, getString(R.string.share_extra_title));
            share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_extra_subject));
            startActivity(Intent.createChooser(share, getString(R.string.share_title)));
        }

    }


    @Override
    protected void onStop()
    {
        try {

            unregisterReceiver(mReceiver);
        }
        catch (IllegalArgumentException e){
            printStacktrace(e);
        }
        catch (Exception e) {
            printStacktrace(e);
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        // ONLY WHEN SCREEN TURNS ON
        if (!ScreenReceiver.wasScreenOn) {
            // THIS IS WHEN ONRESUME() IS CALLED DUE TO A SCREEN STATE CHANGE
            Log.i(Config.TAG,"SCREEN TURNED ON");
        } else {
            // THIS IS WHEN ONRESUME() IS CALLED WHEN THE SCREEN STATE HAS NOT CHANGED
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        // WHEN THE SCREEN IS ABOUT TO TURN OFF
        if (ScreenReceiver.wasScreenOn) {
            // THIS IS THE CASE WHEN ONPAUSE() IS CALLED BY THE SYSTEM DUE TO A SCREEN STATE CHANGE
            Log.i(Config.TAG,"SCREEN TURNED OFF");
            registerScreenReceiver();
        } else {
            // THIS IS WHEN ONPAUSE() IS CALLED WHEN THE SCREEN STATE HAS NOT CHANGED
        }
        super.onPause();
    }

    protected void registerScreenReceiver(){
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
    }

    public void trimCache() {
        try {
            File dir = getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
                Log.e(Config.TAG, "Directory " + dir.getName() + " deleted.");
            }
        } catch (Exception e) {
            // TODO: handle exception
            printStacktrace(e);
        }
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

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)  {
            selectItem(position);
        }


        /** show in the main content view */
        private void selectItem(int position) {

            Log.i(Config.TAG, "onDrawerSelectedItem " + position);
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.valueOf(position));
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "DrawerItem");
            //bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            // Highlight the selected item, update the title, and close the drawer
            mDrawerList.setItemChecked(position, true);
            switch (position){
                case 0:
                    doUrl("");
                    break;
                case 1:
                    doUrl("chatmobile.php");
                    break;
                case 2:
                    doUrl("SendEmailForm.php");
                    break;
                case 3:
                    doUrl("radar.php");
                    break;
                case 4:
                    doUrl("userPics.php");
                    break;
                case 5:
                    doUrl("picoftheday.php");
                    break;
                case 6:
                    doUrl("alerts.php");
                    break;
                default:
                    doUrl("");
                    break;
         }

            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }


}



