package il.co.jws.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class AdFreeCodeActivity extends Activity {

    private Context context;
    private Button btnOkCode;
    private EditText txtCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_free_code);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        btnOkCode = (Button) findViewById(R.id.btnOkCode);
        txtCode = (EditText) findViewById(R.id.txtCode);
        context = this;
        LoadGuid();
        btnOkCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveGuid();

                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra(Config.IS_FROM_ADFREE_ACTIVITY, Boolean.valueOf(true));
                context.startActivity(intent);
            }
        });

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveGuid(){
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Config.PREFS_SUBGUID, txtCode.getText().toString());
        editor.commit();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                notifyServerForSubChange(txtCode.getText().toString());
                return null;
            }
        }.execute(null, null, null);
    }

    private void LoadGuid(){
        SharedPreferences prefs = this.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        txtCode.setText(prefs.getString(Config.PREFS_SUBGUID, ""));
    }

    protected void notifyServerForSubChange(String sGuid) {
        final SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        String registrationId = prefs.getString(Config.PROPERTY_REG_ID, FirebaseInstanceId.getInstance().getToken());
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(Config.SERVER_SUB_URL);

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("action", Config.SERVER_SUB_URL_ACTION));
            nameValuePairs.add(new BasicNameValuePair("guid", sGuid));
            nameValuePairs.add(new BasicNameValuePair("regId", registrationId));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            Log.i(Config.TAG, "notifyServerForSubChange Guid=" + sGuid + " httppost response: " + response.getStatusLine().getReasonPhrase());

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            MainViewController.printStacktrace(e);

        } catch (IOException e) {
            MainViewController.printStacktrace(e);

        }

    }


}
