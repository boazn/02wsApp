/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package il.co.jws.app;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.firebase.crash.FirebaseCrash;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

import static il.co.jws.app.UpdateService.TAG;


/**
 *
 * @author boaz
 */
public class DownloadJson extends AsyncTask<String, Void, String> {

    private final RemoteViews views;
    private final int WidgetID;
    private final AppWidgetManager WidgetManager;
    private final String AppWidgetType;
    private Context Context;
    private final String urljson = "http://www.02ws.co.il/02wsjson.txt";
    private int lang;
    private String temp_unit;

    public DownloadJson(RemoteViews views, int appWidgetID, AppWidgetManager appWidgetManager, String AppWidgetType, Context context) {
        this.views = views;
        this.WidgetID = appWidgetID;
        this.WidgetManager = appWidgetManager;
        this.AppWidgetType = AppWidgetType;
        this.Context = context;
        SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, context.MODE_PRIVATE);
        this.lang = prefs.getInt(Config.PREFS_LANG, 1);
        this.temp_unit = prefs.getString(Config.PREFS_TEMPUNIT, Config.C_TEMPUNIT);
    }

    private float getCorF(float temp){
        if (temp_unit == Config.F_TEMPUNIT)
            temp = Math.round(((9 * temp) / 5) + 32);
        return temp;
    }
    public Current readJsonStream(String in) throws IOException, JSONException {

        if (in == null || in.isEmpty())
            return null;

        JSONObject jsonRootObject = new JSONObject(in);
        JSONObject current = jsonRootObject.getJSONObject("jws").getJSONObject("current");
        JSONObject feelsLike = jsonRootObject.getJSONObject("jws").getJSONObject("feelslike");
        JSONObject states = jsonRootObject.getJSONObject("jws").getJSONObject("states");
        JSONArray sigweather = jsonRootObject.getJSONObject("jws").getJSONObject("states").getJSONArray("sigweather");
        Current c = new Current();
        c.temp =  Float.valueOf(current.getString("temp"));
        c.tempUnits = String.valueOf(Character.toChars(176)) + 'c';
        c.feelsLike = feelsLike.getString("value");
        c.sigtitle = "";c.sigext = "";
        if (sigweather.length() > 1){
            c.sigtitle = sigweather.getJSONObject(0).getString("sigtitle" + lang);
            c.sigext = sigweather.getJSONObject(0).getString("sigext" + lang);
        }
        c.date = current.getString("daytime" + lang);
        c.isRaining = states.getString("israining").isEmpty() ? false : true;
        c.isLight = current.getString("islight").isEmpty() ? false : true;
        c.isWindy = current.getString("iswindy").isEmpty() ? false : true;
        c.isDusty = current.getString("isdusty").isEmpty() ? false : true;
        String lastForecastUpdateTS = states.getString("lastForecastUpdateTS");
        if (!lastForecastUpdateTS.isEmpty())
            c.lastForecastUpdate = Long.valueOf(states.getString("lastForecastUpdateTS"));
        try {
            c.nowind = Float.valueOf(current.getString("windspd")) == 0 && Float.valueOf(current.getString("windspd10min")) == 0;
        }
        catch (Exception e){
            c.nowind = false;
        }
       return c;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            InputStream in;
            URL url = new URL(urljson);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            try {
                con.connect();
            }
            catch (Exception e){//Connection timed out
                Log.e(TAG, "JsonDownload: Download failed: " + e.getMessage());
                con.connect();
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                in = con.getInputStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
                Log.v(TAG, "JsonDownload: download InputStream succeeded: ");
                Log.v(TAG, "JsonDownload: Param 0 is: " + params[0]);
                return total.toString();

            }
       //NOTE:  it is not thread-safe to set the ImageView from inside this method.  It must be done in onPostExecute()
        }


        catch (Exception e) {
            Log.e(TAG, "JsonDownload: Download failed: " + e.getMessage());
            FirebaseCrash.report(e);
        }
        return null;
    }

    @Override
    public void onPostExecute(String in) {
        Current c = new Current();

        try {
            c = readJsonStream(in);
        } catch (IOException ex) {
            Log.e(TAG, TAG, ex);
            Logger.getLogger(DownloadJson.class.getName()).log(Level.SEVERE, null, ex);
            FirebaseCrash.report(ex);
        } catch (JSONException ex) {
            Log.e(TAG, TAG, ex);
            Logger.getLogger(DownloadJson.class.getName()).log(Level.SEVERE, null, ex);
            FirebaseCrash.report(ex);
        }
        try {
            if (AppWidgetType.equals(Config.WIDGET_TYPE_SMALL))
                renderSmallWidget(c);
            else
                renderRectWidget(c);
            WidgetManager.updateAppWidget(WidgetID, views);
            if (c != null)
                Log.v(TAG, "json onPostExecute succeed. temp=" + c.temp + " WidgetID=" + WidgetID);
            else
                Log.v(TAG, "json onPostExecute failed: Json null ");
        } catch(Exception e){
            FirebaseCrash.report(e);
        }
    }

    public void renderRectWidget(Current c){
        if (c == null) return;
        java.text.DecimalFormat format = new java.text.DecimalFormat();
        String temp = String.valueOf(format.format(getCorF(c.temp)));
        views.setTextViewText(R.id.textViewUp, c.date);
        views.setTextViewText(R.id.textViewTemp, temp + temp_unit);
        views.setTextViewText(R.id.textViewAboveTemp, Context.getResources().getString(R.string.feelslike) + ' ' + c.feelsLike + c.tempUnits);
        views.setTextViewText(R.id.textViewBelowTemp, c.sigtitle +  '\n' + c.sigext);
        statesLogic(c);
    }
    public void playSound(int rawSound){
        SharedPreferences prefs = Context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        boolean boolSound = prefs.getBoolean(Config.PREFS_SOUND, false);
        AudioManager am = (AudioManager)Context.getSystemService(Context.AUDIO_SERVICE);
        if ((boolSound)&&(am.getRingerMode()==AudioManager.RINGER_MODE_NORMAL)){
            MediaPlayer mediaPlayer = MediaPlayer.create(Context, rawSound);
            mediaPlayer.start();
        }
    }
    public void renderSmallWidget(Current c){
        if (c == null) return;
        java.text.DecimalFormat format = new java.text.DecimalFormat();
        String temp = String.valueOf(format.format(getCorF(c.temp)));
        int cold = R.drawable.cold;
        int hot = R.drawable.hot;
        int vcold = R.drawable.vcold;
        int snowy = R.drawable.snowy;
        int vhot = R.drawable.vhot;
        int imgToSet, color;
        views.setTextViewText(R.id.btnOpenAppSmall, temp);
        if (temp != null && !temp.isEmpty())
        {
            if ((c.temp) > 30){
                imgToSet = vhot;
                color = Color.BLACK;
            }
            else if (c.temp > 20){
                imgToSet = hot;
                color = Color.rgb(139,69,19);//brown
            }
            else if (c.temp > 10){
                imgToSet = cold;
                color = Color.DKGRAY;
            }
            else if (c.temp > 2){
                imgToSet = vcold;
                color = Color.WHITE;
            }
            else{
                imgToSet = snowy;
                color = Color.DKGRAY;
            }
            views.setInt(R.id.imgBackgroundAppSmall, "setImageResource", imgToSet);
            views.setTextColor(R.id.btnOpenAppSmall, color);
        }
        views.setInt(R.id.btnOpenAppSmall, "setBackgroundColor", android.graphics.Color.TRANSPARENT);
        statesLogic(c);
    }

    private void statesLogic (Current c){
        SharedPreferences prefs = Context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        long currentTS = new java.sql.Timestamp(System.currentTimeMillis()).getTime();
        long lastSoundAlert = prefs.getLong(Config.LAST_TIME_SOUND_ALERT, 0);
        long lastSoundForecast = prefs.getLong(Config.LAST_TIME_SOUND_FORECAST, 0);
        if ((currentTS - c.lastForecastUpdate*1000 < Config.TIME_SOUND_ALERT_INTERVAL)&&((currentTS - lastSoundForecast) > Config.TIME_SOUND_ALERT_INTERVAL)){
            playSound(R.raw.lighttrainshort);
            editor.putLong(Config.LAST_TIME_SOUND_FORECAST, currentTS);
            editor.commit();
        }
        else
        {
            Log.v(TAG, "currentTS: " + currentTS + " (currentTS - lastSoundForecast)=" + (currentTS - lastSoundForecast) + " lastForecastUpdate:" + c.lastForecastUpdate*1000 + " currentTS - lastForecastUpdate = " + (currentTS -  c.lastForecastUpdate*1000) + " < " + Config.TIME_SOUND_ALERT_INTERVAL);
        }

        if (currentTS - lastSoundAlert < Config.TIME_SOUND_ALERT_INTERVAL) {
            Log.v(TAG, "currentTS: " + currentTS + " lastSoundAlert:" + lastSoundAlert + " currentTS - lastSoundAlert = " + (currentTS - lastSoundAlert) + " < " + Config.TIME_SOUND_ALERT_INTERVAL);
            return;

        }
        if ((c.isRaining)&&(c.isLight)){
            playSound(R.raw.rainfibl);
            editor.putLong(Config.LAST_TIME_SOUND_ALERT, currentTS);
            editor.commit();
        }

        if ((c.nowind)&&(c.isLight)){
            playSound(R.raw.owl);
            editor.putLong(Config.LAST_TIME_SOUND_ALERT, currentTS);
            editor.commit();
        }

        if ((c.isDusty)&&(c.isLight)){
            playSound(R.raw.crow);
            editor.putLong(Config.LAST_TIME_SOUND_ALERT, currentTS);
            editor.commit();
        }

        if ((c.isWindy)&&(c.isLight)){
            playSound(R.raw.wind);
            editor.putLong(Config.LAST_TIME_SOUND_ALERT, currentTS);
            editor.commit();
        }



    }

    private class Current {
        public float temp;
        public String tempUnits;
        public String feelsLike;
        public String sigtitle;
        public String sigext;
        public String date;
        public Boolean isRaining;
        public Boolean nowind;
        public Boolean isDusty;
        public Boolean isLight;
        public Boolean isWindy;
        public long lastForecastUpdate ;
    }
 }



