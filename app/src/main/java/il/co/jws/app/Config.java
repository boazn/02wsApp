package il.co.jws.app;

public interface Config {
  
    // CONSTANTS
    static final String SERVER_REGISTER_URL =  "http://www.02ws.co.il/gcm_register.php";
    static final String SERVER_GOOGLE_PLAY_API_PREF =  "https://www.googleapis.com/androidpublisher/v3/applications/il.co.jws.app/purchases/subscriptions/";
    static final String SERVER_GOOGLE_PLAY_API_REFRESH_TOKEN_URI = "https://accounts.google.com/o/oauth2/token";
    static final String SERVER_SUB_URL =  "http://www.02ws.co.il/subscription_reciever.php";
    static final String ACCESS_TOKEN_GOOGLE_API = "ACCESS_TOKEN_GOOGLE_API";
    static final String REFRESH_TOKEN_GOOGLE_API = "1/aAtONbtAwRFJ0Fm6G0k82dD6rYqPEZP59YmM4GE7tb0";
    static final String GRANT_TYPE_GOOGLE_API = "refresh_token";
    static final String CLIENT_ID_GOOGLE_API = "1056747824588-3vr2v9kqort65mb22k6cm0d7ip8n5ssl.apps.googleusercontent.com";
    static final String CLIENT_SECRET_GOOGLE_API = "A9vfZj2c8fTDBeaBGFM8Ndqn";
    static final String SERVER_SUB_URL_ACTION =  "storeSub";
     
    // Google project id
    static final String GOOGLE_SENDER_ID = "761995000479"; 
 
    /**
     * Tag used on log messages.
     */
    static final String TAG = "02WSAPP";
    static final String PROPERTY_APP_VERSION = "appVersion";
    static final String DISPLAY_MESSAGE_ACTION ="il.co.jws.app.gcm.DISPLAY_MESSAGE";
    static final String EXTRA_MESSAGE = "message";
    static final String WIDGET_UPDATE_ACTION = "UPDATE_ACTION";
    static final String WIDGET_TYPE = "WIDGET_TYPE";
    static final String FILE_TO_SHARE = "toshare.png";
    static final String URL_SHORT = "http://www.02ws.co.il/02ws_short.png?r=2000";
    static final String URL_SHORT_ENG = "http://www.02ws.co.il/02ws_short_eng.png?r=2000";
    public static final String PREFS_LANG = "lang";
    public static final String PREFS_TEMPUNIT = "tempunit";
    public static final String C_TEMPUNIT = "°C";
    public static final String F_TEMPUNIT = "°F";
    public static final int REQUEST_CODE_EMAIL = 1;
    public static final String PREFS_NOTIFICATIONS = "Notifications";
    public static final String PREFS_NOTIFICATIONS_RAIN = "Rain notifications";
    public static final String PREFS_NOTIFICATIONS_TIPS = "Tips notifications";
    public static final String PREFS_VIBRATION = "PREFS_VIBRATION";
    public static final String PREFS_SOUND = "PREFS_SOUND";
    public static final String PREFS_ALERT_SOUND = "PREFS_ALERT_SOUND";
    public static final String PREFS_CLOTH = "PREFS_CLOTH";
    public static final String PREFS_NAME = "PREFS_02WS";
    public static final String PREFS_FULLTEXT = "PREFS_FULLTEXT";
    public static final String PREFS_SUBGUID = "PREFS_SUBGUID";
    public static final String PREFS_EMAIL = "PREFS_EMAIL";
    public static final String PREFS_SUB_ID = "PREFS_SUB_ID";
    public static final String LATEST_SUB_ID = "LATEST_SUB_ID";
    public static final String PREFS_BILLING_ERROR = "PREFS_BILLING_ERROR";
    public static final String PREFS_APPROVED = "PREFS_APPROVED";
    public static final String PREFS_ENTRIES = "ENTRIES";
    public static final String PREFS_ADFREE = "ADFREE";
    public static final String PREFS_DAILYFORECAST = "DAILYFORECAST";
    public static final String BILLING_QUERY = "BILLING_QUERY";
    public static final String CHANNEL_ID = "02wsChannel";
    public static final String LAST_TIME_SOUND_ALERT = "LAST_TIME_SOUND_ALERT";
    public static final String LAST_CHECK_IN_APP_BILLING = "LAST_CHECK_IN_APP_BILLING";
    public static final int CHECK_IN_APP_BILLING_INTERVAL = 172800;//2 days
    public static final int TIME_SOUND_ALERT_INTERVAL = 10800;
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String SITE_URL_BASE = "http://www.02ws.co.il/small.php?lang=";
    public static final String ALERTS_URL_BASE = "http://www.02ws.co.il/small.php?section=alerts.php&lang=";
    public static final String UPLOAD_PIC_FOLDER = "02WSImages";
    public static final String UPLOAD_PIC_URL = "http://www.02ws.co.il/user_picture_reciever.php";
    public static final int SCALE_DOWN_FACTOR = 500;
    public static final String CAMERA_CHOSEN = "CAMERA_CHOSEN";
    public static final String GALLERY_CHOSEN = "GALLERY_CHOSEN";
    public static final String PREFS_ZOOM_TEXT = "PREFS_ZOOM_TEXT";
    public static final String WIDGET_TYPE_RECT = "RECT";
    public static final String WIDGET_TYPE_SMALL = "SMALL";
    public static final String IS_FROM_ADFREE_ACTIVITY = "IS_FROM_ADFREE_CODE";
    public static final String WIDGET_TYPE_LARGE = "LARGE";
     
}
