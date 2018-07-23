package il.co.jws.app;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.app.NotificationChannel;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
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
import android.widget.ListView;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import android.support.v7.app.ActionBarDrawerToggle;
import android.widget.Toast;

import static il.co.jws.app.MainViewController.printStacktrace;

public class MainActivity extends Activity implements BillingProvider {

    private WebView webview;
    public @interface BillingActivity {
        /** A type of SKU for in-app products. */
        String ALERTS_CLICK = "alerts click";
        String ALERTS_YEARLY_CLICK = "alerts yearly click";
        String ADFREE_CLICK = "adfree click";
        String ADFREE_YEARLY_CLICK = "adfree yearly click";
        /** A type of SKU for subscriptions. */
        String SUBS_QUERY = "subs query";
    }
    String mBillingActivity;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private String[] mNavTitles;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private int lang;
    private BillingManager mBillingManager;
    private MainViewController mViewController;
    private FirebaseAnalytics mFirebaseAnalytics;
    private String tempunit;
    Context context;
    Menu mmenu;
    Toolbar toolbar;
    boolean isFromAlerts = false;
    boolean replyFromAlerts = false;
    boolean isFromUpload = false;
    boolean isFromAdFreeCode = false;
    private BroadcastReceiver mReceiver;

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
        // Start the controller and load game data
        mViewController = new MainViewController(this, getApplicationContext(), mFirebaseAnalytics);
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
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerList = findViewById(R.id.left_drawer);
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
        mSwipeRefreshLayout = findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doRefresh(false);
            }
        });
        webview = findViewById(R.id.webview);
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

    /**
     * Show an alert dialog to the user
     * @param messageId String id to display inside the alert dialog
     */
    @UiThread
    void alert(@StringRes int messageId) {
        alert(messageId, null);
    }

    /**
     * Show an alert dialog to the user
     * @param messageId String id to display inside the alert dialog
     * @param optionalParam Optional attribute for the string
     */
    @UiThread
    void alert(@StringRes int messageId, @Nullable Object optionalParam) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new RuntimeException("Dialog could be shown only from the main thread");
        }

        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setNeutralButton("OK", null);

        if (optionalParam == null) {
            bld.setMessage(messageId);
        } else {
            bld.setMessage(getResources().getString(messageId, optionalParam));
        }

        bld.create().show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mmenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.main, menu);
        // Restore preferences
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        lang = prefs.getInt(Config.PREFS_LANG, 1);
        tempunit = prefs.getString(Config.PREFS_TEMPUNIT, Config.C_TEMPUNIT);
        boolean boolgetNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS, true);
        boolean boolgetRainNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS_RAIN, false);
        boolean boolgetTipsNotifications = prefs.getBoolean(Config.PREFS_NOTIFICATIONS_TIPS, true);
        boolean boolgetBillingQuery = prefs.getBoolean(Config.BILLING_QUERY, false);
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
        int iEntries = prefs.getInt(Config.PREFS_ENTRIES, 0);
        //final Toolbar toolbar = findViewById(R.id.toolbar);
        if (!(iEntries % 100 == 0))
            return true;

        // We load a drawable and create a location to show a tap target here
        // We need the display to get the width and height at this point in time
        final Display display = getWindowManager().getDefaultDisplay();
        // Load our little droid guy
        final Drawable nav = getResources().getDrawable( R.drawable.ic_menu);
        //nav = new ScaleDrawable(nav, 0, 50, 50).getDrawable();
        //nav.setBounds(0, 0, 50, 50);
        // Tell our droid buddy where we want him to appear
        final Rect navTarget = new Rect(280, 350, 0 , 0);

        final Rect shorttermforecastTarget = new Rect(0, 0, 100 , 100);
        // Using deprecated methods makes you look way cool
        shorttermforecastTarget.offset(display.getWidth()-150, display.getHeight() / 4);
        final Rect soundTarget = new Rect(0, 0, 100 , 100);
        // Using deprecated methods makes you look way cool
        soundTarget.offset(display.getWidth()-150, display.getHeight() / 2);
        final Rect adfreeTarget = new Rect(0, 0, 100 , 100);
        // Using deprecated methods makes you look way cool
        adfreeTarget.offset(display.getWidth()-150, (int) (0.8* display.getHeight()));
        final Rect settingsTarget = new Rect(0, 0, 100 , 100);
        settingsTarget.offset(display.getWidth()-130, 120);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                TapTargetSequence ts =  new TapTargetSequence(MainActivity.this)
                        .targets(

                           /*TapTarget.forView(findViewById(R.id.action_rain_notifications), "This is a short term forecast", "click to register")
                                .outerCircleAlpha(0.76f)
                                .targetRadius(20)
                                .textColor(android.R.color.white).id(2),*/
                                // Likewise, this tap target will target the search button
                                //TapTarget.forToolbarMenuItem(toolbar, R.id.action_rain_notifications, "This is a short term forecast", "As you can see, it has gotten pretty dark around here...").id(2),
                                // You can also target the overflow button in your toolbar
                                TapTarget.forBounds(settingsTarget , getResources().getString(R.string.guide_settings_title), getResources().getString(R.string.guide_settings_desc))
                                        .outerCircleAlpha(0.96f)
                                        .outerCircleColor(R.color.gray)
                                        .targetCircleColor(R.color.white)
                                        .targetRadius(60)
                                        .cancelable(false)
                                        .textColor(R.color.black).icon(getResources().getDrawable(R.drawable.icons8_menu_vertical_50)).id(1),
                                // This tap target will target the back button, we just need to pass its containing toolbar
                                //TapTarget.forToolbarNavigationIcon(toolbar, "navigation button", "desc for back button").textColor(android.R.color.white).id(4),
                                TapTarget.forBounds(shorttermforecastTarget, getResources().getString(R.string.guide_shorttermalerts_title), getResources().getString(R.string.guide_shorttermalerts_desc))
                                        .outerCircleAlpha(0.96f)
                                        .outerCircleColor(R.color.gray)
                                        .targetCircleColor(R.color.white)
                                        .targetRadius(60)
                                        .textColor(R.color.black)
                                        .cancelable(false)
                                        .icon(getResources().getDrawable(R.drawable.checkbox)).id(2),
                                TapTarget.forBounds(soundTarget, getResources().getString(R.string.guide_soundTarget_title), getResources().getString(R.string.guide_soundTarget_desc))
                                        .outerCircleAlpha(0.96f)
                                        .outerCircleColor(R.color.gray)
                                        .targetCircleColor(R.color.white)
                                        .targetRadius(60)
                                        .textColor(R.color.black)
                                        .cancelable(false)
                                        .icon(getResources().getDrawable(R.drawable.checkbox)).id(3),
                                TapTarget.forBounds(adfreeTarget, getResources().getString(R.string.guide_adfreeTarget_title), getResources().getString(R.string.guide_adfreeTarget_desc))
                                        .outerCircleAlpha(0.96f)
                                        .outerCircleColor(R.color.gray)
                                        .targetCircleColor(R.color.white)
                                        .targetRadius(60)
                                        .textColor(R.color.black)
                                        .cancelable(false)
                                        .icon(getResources().getDrawable(R.drawable.checkbox)).id(4),
                                TapTarget.forBounds(navTarget, getResources().getString(R.string.guide_navigationdrawer_title), getResources().getString(R.string.guide_navigationdrawer_desc))
                                        .outerCircleAlpha(0.96f)
                                        .outerCircleColor(R.color.gray)
                                        .targetCircleColor(R.color.white)
                                        .targetRadius(60)
                                        .textColor(R.color.black)
                                        .cancelable(false)
                                        .icon(nav).id(5))
                        .listener(new TapTargetSequence.Listener() {
                            // This listener will tell us when interesting(tm) events happen in regards
                            // to the sequence
                            @Override
                            public void onSequenceFinish() {
                                // Yay
                                Log.d("TapTargetView", "finished");


                            }

                            @Override
                            public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                                Log.d("TapTargetView", "Clicked on " + lastTarget.id());

                            }



                            @Override
                            public void onSequenceCanceled(TapTarget lastTarget) {
                                // Boo
                        /*final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Uh oh")
                                .setMessage("You canceled the sequence")
                                .setPositiveButton("Oops", null).show();
                        TapTargetView.showFor(dialog,                 // `this` is an Activity
                                TapTarget.forView(dialog.getButton(DialogInterface.BUTTON_POSITIVE), "Uh oh!", "You canceled the sequence at step " + lastTarget.id())
                                        // All options below are optional
                                        .outerCircleColor(R.color.red)      // Specify a color for the outer circle
                                        .outerCircleAlpha(0.96f)            // Specify the alpha amount for the outer circle
                                        .targetCircleColor(R.color.white)   // Specify a color for the target circle
                                        .titleTextSize(20)                  // Specify the size (in sp) of the title text
                                        .titleTextColor(R.color.white)      // Specify the color of the title text
                                        .descriptionTextSize(10)            // Specify the size (in sp) of the description text
                                        .descriptionTextColor(R.color.white)  // Specify the color of the description text
                                        .textColor(R.color.blue)            // Specify a color for both the title and description text
                                        .textTypeface(Typeface.SANS_SERIF)  // Specify a typeface for the text
                                        .dimColor(R.color.white)            // If set, will dim behind the view with 30% opacity of the given color
                                        .drawShadow(true)                   // Whether to draw a drop shadow or not
                                        .cancelable(false)                  // Whether tapping outside the outer circle dismisses the view
                                        .tintTarget(true)                   // Whether to tint the target view's color
                                        .transparentTarget(false)           // Specify whether the target is transparent (displays the content underneath)
                                        //.icon(R.drawable.ic_camera)                     // Specify a custom drawable to draw as the target
                                        .targetRadius(30),                  // Specify the target radius (in dp)
                                new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                                    @Override
                                    public void onTargetClick(TapTargetView view) {
                                        super.onTargetClick(view);      // This call is optional

                                    }
                                });*/
                            }
                        });
                ts.start();

            }
        });
        editor.putInt(Config.PREFS_ENTRIES,++iEntries);
        editor.commit();
        return true;
    }

    @Override
   protected void onDestroy() {
        if (mBillingManager != null) {
            mBillingManager.destroy();
        }
      super.onDestroy();
      try {
          mViewController.trimCache(); //if trimCache is static
      } catch (Exception e) {
        Log.i(Config.TAG, "onDestroy" + e.getMessage());
        FirebaseCrash.report(e);
      }
    }

    @Override
    public boolean onMenuOpened (int featureId,
                                 Menu menu){
        mBillingActivity = BillingActivity.SUBS_QUERY;
        // Create and initialize BillingManager which talks to BillingLibrary
        mBillingManager = new BillingManager(this, mViewController.getUpdateListener());
        return true;
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
        else if (item.getItemId() == R.id.adfree_monthly) {

            bundle.putString("full_text", "clicked on enter_code");
            mFirebaseAnalytics.logEvent("enter_code", bundle);
            MenuItem yearlyItem = mmenu.findItem(R.id.adfree_yearly);
            MenuItem enter_codeItem = mmenu.findItem(R.id.enter_code);
            MenuItem adfree_monthly = mmenu.findItem(R.id.adfree_monthly);
            if (item.isChecked()){
                mBillingActivity = BillingActivity.ADFREE_CLICK;
                mBillingManager = new BillingManager(this, mViewController.getUpdateListener());
                yearlyItem.setChecked(false);
                enter_codeItem.setChecked(true);
            }
            else
            {
                mViewController.cancelSubscription(BillingConstants.SKU_AD_FREE, prefs.getString(Config.PREFS_SUB_ID, ""));
                mViewController.notifyServerForSubChange("0");
                enter_codeItem.setChecked((yearlyItem.isChecked())||(adfree_monthly.isChecked()));

            }


        }
        else if (item.getItemId() == R.id.adfree_yearly) {

            bundle.putString("full_text", "clicked on enter_code");
            mFirebaseAnalytics.logEvent("enter_code", bundle);
            MenuItem yearlyItem = mmenu.findItem(R.id.adfree_yearly);
            MenuItem enter_codeItem = mmenu.findItem(R.id.enter_code);
            MenuItem adfree_monthly = mmenu.findItem(R.id.adfree_monthly);
            if (item.isChecked()){
                mBillingActivity = BillingActivity.ADFREE_YEARLY_CLICK;
                mBillingManager = new BillingManager(this, mViewController.getUpdateListener());
                adfree_monthly.setChecked(false);
                enter_codeItem.setChecked(true);

            }
            else
            {
                mViewController.cancelSubscription(BillingConstants.SKU_AD_FREE_YEARLY, prefs.getString(Config.PREFS_SUB_ID, ""));
                mViewController.notifyServerForSubChange("0");
                enter_codeItem.setChecked((yearlyItem.isChecked())||(adfree_monthly.isChecked()));
            }


        }
        else if (item.getItemId() == R.id.action_lighttrain) {
            bundle.putString("full_text", "clicked on action_lighttrain");
            mFirebaseAnalytics.logEvent("action_lighttrain", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.lightrain);
            editor.commit();
            mViewController.playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_lighttrainbell) {
            bundle.putString("full_text", "clicked on action_lighttrainbell");
            mFirebaseAnalytics.logEvent("action_lighttrainbell", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.lightrainbell);
            editor.commit();
            mViewController.playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_lighttrainpassing) {
            bundle.putString("full_text", "clicked on action_lighttrainpassing");
            mFirebaseAnalytics.logEvent("action_lighttrainpassing", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.lightrainpassing);
            editor.commit();
            mViewController.playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_lighttrainshort) {
            bundle.putString("full_text", "clicked on action_lighttrainshort");
            mFirebaseAnalytics.logEvent("action_lighttrainshort", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.lighttrainshort);
            editor.commit();
            mViewController.playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_JerOfGold) {
            bundle.putString("full_text", "clicked on action_JerOfGold");
            mFirebaseAnalytics.logEvent("action_JerOfGold", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.jerofgold);
            editor.commit();
            mViewController.playSound();
            return true;
        }
        else if (item.getItemId() == R.id.action_JerOfGoldDrugs) {
            bundle.putString("full_text", "clicked on action_JerOfGoldDrugs");
            mFirebaseAnalytics.logEvent("action_JerOfGoldDrugs", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.jerofgolddrugs);
            editor.commit();
            mViewController.playSound();
            return true;
        }

        else if (item.getItemId() == R.id.action_vop) {
            bundle.putString("full_text", "clicked on action_vop");
            mFirebaseAnalytics.logEvent("action_vop", bundle);
            editor.putInt(Config.PREFS_ALERT_SOUND, R.raw.vop);
            editor.commit();
            mViewController.playSound();
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
            mViewController.toggleNotifications();
            return true;
        }
        else if (item.getItemId() == R.id.action_tips_notifications){
             editor.putBoolean(Config.PREFS_NOTIFICATIONS_TIPS, item.isChecked());
             editor.commit();
            mViewController.toggleNotifications();
             return true;
        
        }
        else if (item.getItemId() == R.id.action_rain_notifications){
            // item check status is based on sons
            if (item.isChecked()) {
                item.setChecked(false);
            } else {
                item.setChecked(true);
            }

            return true;

        }
        else if (item.getItemId() == R.id.shorttermalerts_monthly) {
              editor.putBoolean(Config.PREFS_NOTIFICATIONS_RAIN, item.isChecked());
              editor.commit();
              if (item.isChecked()) {
                  mBillingActivity = BillingActivity.ALERTS_CLICK;
                  mBillingManager = new BillingManager(this, mViewController.getUpdateListener());
                  MenuItem yearlyItem = mmenu.findItem(R.id.shorttermalerts_yearly);
                  yearlyItem.setChecked(false);
                  MenuItem shorttermItem = mmenu.findItem(R.id.action_rain_notifications);
                  shorttermItem.setChecked(true);
              }
               else
              {
                  mViewController.cancelSubscription(BillingConstants.SKU_ALERTS, prefs.getString(Config.PREFS_SUB_ID, ""));
                  mViewController.toggleNotifications();
              }

              return true;

        }
        else if (item.getItemId() == R.id.shorttermalerts_yearly) {
            editor.putBoolean(Config.PREFS_NOTIFICATIONS_RAIN, item.isChecked());
            editor.commit();
            if (item.isChecked()) {
                mBillingActivity = BillingActivity.ALERTS_YEARLY_CLICK;
                mBillingManager = new BillingManager(this, mViewController.getUpdateListener());
                MenuItem monthlyItem = mmenu.findItem(R.id.shorttermalerts_monthly);
                monthlyItem.setChecked(false);
                MenuItem shorttermItem = mmenu.findItem(R.id.action_rain_notifications);
                shorttermItem.setChecked(true);
            }
            else
            {
                mViewController.cancelSubscription(BillingConstants.SKU_ALERTS_YEARLY, prefs.getString(Config.PREFS_SUB_ID, ""));
                mViewController.toggleNotifications();
            }

            return true;

        }
        else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void onBillingManagerSetupFinished() {
        Log.v(Config.TAG, "onBillingManagerSetupFinished");
        if (BillingActivity.ALERTS_CLICK.equals(mBillingActivity)){
            mBillingManager.initiatePurchaseFlow(BillingConstants.SKU_ALERTS, BillingClient.SkuType.SUBS);
        }
        else if (BillingActivity.ADFREE_CLICK.equals(mBillingActivity)){
            mBillingManager.initiatePurchaseFlow(BillingConstants.SKU_AD_FREE, BillingClient.SkuType.SUBS);
            /*List<String> skuList = new ArrayList<>();
            skuList.add(BillingConstants.SKU_AD_FREE_YEARLY);
            skuList.add(BillingConstants.SKU_AD_FREE);
            mBillingManager.querySkuDetailsAsync(BillingClient.SkuType.SUBS, skuList,  new SkuDetailsResponse());*/
        }
        else if (BillingActivity.ADFREE_YEARLY_CLICK.equals(mBillingActivity)) {
            mBillingManager.initiatePurchaseFlow(BillingConstants.SKU_AD_FREE_YEARLY, BillingClient.SkuType.SUBS);
        }
        else if (BillingActivity.ALERTS_YEARLY_CLICK.equals(mBillingActivity)) {
            mBillingManager.initiatePurchaseFlow(BillingConstants.SKU_ALERTS_YEARLY, BillingClient.SkuType.SUBS);
        }
        else if (BillingActivity.SUBS_QUERY.equals(mBillingActivity)){
            mBillingManager.queryPurchases();

        }
    }

     private class SkuDetailsResponse implements SkuDetailsResponseListener {

        @Override
        public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
            if (responseCode == BillingClient.BillingResponse.OK
                    && skuDetailsList != null) {
                for (SkuDetails skuDetails : skuDetailsList) {
                    String sku = skuDetails.getSku();
                    String price = skuDetails.getPrice();
                    if (BillingConstants.SKU_ALERTS.equals(sku)) {
                        Toast.makeText(MainActivity.this, BillingConstants.SKU_ALERTS, Toast.LENGTH_LONG).show();
                    }  else if (BillingConstants.SKU_AD_FREE.equals(sku)) {
                        Toast.makeText(MainActivity.this, BillingConstants.SKU_AD_FREE, Toast.LENGTH_LONG).show();
                    }
                    else if (BillingConstants.SKU_AD_FREE_YEARLY.equals(sku)) {
                        Toast.makeText(MainActivity.this, BillingConstants.SKU_AD_FREE_YEARLY, Toast.LENGTH_LONG).show();
                    }
                    else if (BillingConstants.SKU_ALERTS_YEARLY.equals(sku)) {
                        Toast.makeText(MainActivity.this, BillingConstants.SKU_ALERTS_YEARLY, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
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
        guid = (adFreeGUID.length() > 0) ? "&reg_id=" + mViewController.getRegistrationId(this) : "";
        fulltext += boolFulltext ? "1" : "0";
        sound += boolSound ? "1" : "0";
        cloth += boolShowCloth ? "1" : "0";
        return lang + fulltext + sound + cloth + temp + guid;
    }

    protected void doExtUrl (String url){
        webview = (WebView) findViewById(R.id.webview);
        webview.loadUrl(url);
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
        mViewController.toggleNotifications();
    }

    public String getEmailAddress() {
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        String email = prefs.getString(Config.PREFS_EMAIL, null);
        if (email != null)
            return email;
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                false, null, null, null, null);

        try {
            startActivityForResult(intent, Config.REQUEST_CODE_EMAIL);
            email = prefs.getString(Config.PREFS_EMAIL, null);
            return email;
        } catch (ActivityNotFoundException e) {
            // This device may not have Google Play Services installed.
            // TODO: do something else
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Config.REQUEST_CODE_EMAIL && resultCode == RESULT_OK) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Config.PREFS_EMAIL, accountName);
            editor.commit();
        }
    }

    @Override
    public BillingManager getBillingManager() {
        return mBillingManager;
    }

    @Override
    public boolean isPremiumPurchased() {
        return mViewController.isPremiumPurchased();
    }

    @Override
    public boolean isAlertsOnly() {
        return mViewController.isAlertOnlyPurchased();
    }

    @Override
    public boolean isMonthlySubscribed() {
        return mViewController.isGoldMonthlySubscribed();
    }

   @Override
    public boolean isYearlySubscribed() {
       return mViewController.isGoldYearlySubscribed();
    }



    public String getSubsID (List<Purchase> purchaseList){
        if (isAlertsOnly() || isPremiumPurchased()){
            for (Purchase purchase : purchaseList) {
                return purchase.getPurchaseToken();
            }
        }

        return "";
    }
    public void showRefreshedUi(List<Purchase> purchaseList) {
        String Str = "Premium:" + isPremiumPurchased() + " alerts:" + isAlertsOnly() + " isMonthly:" + isMonthlySubscribed() + " isYearly:" + isYearlySubscribed();
        //Toast.makeText(MainActivity.this, Str, Toast.LENGTH_LONG).show();
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Config.PREFS_NOTIFICATIONS_RAIN, isAlertsOnly());
        editor.putInt(Config.PREFS_APPROVED, isPremiumPurchased()? 1 : isAlertsOnly()? 1: 0);
        editor.putBoolean(Config.PREFS_ADFREE, isPremiumPurchased());
        editor.putString(Config.PREFS_SUB_ID, getSubsID(purchaseList));
        editor.commit();

        MenuItem NotifRainItem = mmenu.findItem(R.id.action_rain_notifications);
        NotifRainItem.setChecked(isAlertsOnly());
        MenuItem shorttermalertsMonthlyItem = mmenu.findItem(R.id.shorttermalerts_monthly);
        MenuItem shorttermalertsYearlyItem = mmenu.findItem(R.id.shorttermalerts_yearly);
        shorttermalertsMonthlyItem.setChecked(isAlertsOnly()&&mViewController.isGoldMonthlySubscribed());
        shorttermalertsYearlyItem.setChecked(isAlertsOnly()&&mViewController.isGoldYearlySubscribed());
        MenuItem AdfreeItem = mmenu.findItem(R.id.enter_code);
        AdfreeItem.setChecked(isPremiumPurchased());
        MenuItem adfreeMonthlyItem = mmenu.findItem(R.id.adfree_monthly);
        MenuItem adfreeYearlyItem = mmenu.findItem(R.id.adfree_yearly);
        adfreeMonthlyItem.setChecked(isPremiumPurchased()&&mViewController.isGoldMonthlySubscribed());
        adfreeYearlyItem.setChecked(isPremiumPurchased()&&mViewController.isGoldYearlySubscribed());

        if (BillingActivity.ALERTS_CLICK.equals(mBillingActivity)){
            mViewController.toggleNotifications();
        }
        else if (BillingActivity.ADFREE_CLICK.equals(mBillingActivity)){
            mViewController.notifyServerForSubChange("1");
        }
        else if (BillingActivity.ALERTS_YEARLY_CLICK.equals(mBillingActivity)){
            mViewController.toggleNotifications();
        }
        else if (BillingActivity.ADFREE_YEARLY_CLICK.equals(mBillingActivity)){
            mViewController.notifyServerForSubChange("1");
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
        protected String doInBackground(String... strings) {
            return null;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
         super.onPreExecute();
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
        // Note: We query purchases in onResume() to handle purchases completed while the activity
        // is inactive. For example, this can happen if the activity is destroyed during the
        // purchase flow. This ensures that when the activity is resumed it reflects the user's
        // current purchases.
        if (mBillingManager != null
                && mBillingManager.getBillingClientResponseCode() == BillingClient.BillingResponse.OK) {
            mBillingManager.queryPurchases();
        }
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
                case 7:
                    doUrl("forecast/getForecast.php&region=isr");
                    break;
                case 8:
                    doUrl("forecast/getForecast.php");
                    break;
                case 9:
                    doUrl("snow.php");
                    break;
                case 10:
                    doExtUrl("https://m.youtube.com/channel/UCcFdTuHfckfOsCy7MwbY9vQ");
                    break;
                default:
                    doUrl("");
                    break;
         }

            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }


}



