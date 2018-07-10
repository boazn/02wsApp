package il.co.jws.app;

public interface Config {
  
    // CONSTANTS
    static final String SERVER_REGISTER_URL =  "http://www.02ws.co.il/gcm_register.php";
    static final String SERVER_SUB_URL =  "http://www.02ws.co.il/subscription_reciever.php";
    static final String SERVER_SUB_URL_ACTION =  "updateregid";
     
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
    public static final String PREFS_SUB_ID = "PREFS_SUB_ID";
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