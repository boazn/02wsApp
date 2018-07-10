/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package il.co.jws.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.AlertDialog;
import com.google.firebase.crash.FirebaseCrash;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;






/**
 *
 * @author boaz
 */
public class CameraActivity extends Activity {

private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int PICK_IMAGE = 10;
private Uri fileUri;
private String fileName;
private String folderName = Config.UPLOAD_PIC_FOLDER;
private String serverUrl = Config.UPLOAD_PIC_URL;
private int MY_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE;
private Button btnSendPhoto;
private Button btnCancelPhoto;
private Bitmap mBitmapImage;
private String mComment;
private String mNameTitle;
private Context context;
private String mSelectedPath;
    private int MY_PERMISSIONS_REQUEST_GET_ACCOUNT;
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_camera);
    btnSendPhoto = (Button)findViewById(R.id.SendPhoto);
    btnCancelPhoto = (Button)findViewById(R.id.CancelPhoto);
    context = this;
    btnSendPhoto.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            EditText txtName = (EditText) findViewById(R.id.name);
            EditText txtComment = (EditText) findViewById(R.id.comment);
            mComment = txtComment.getText().toString();
            mNameTitle = txtName.getText().toString();
            if (mComment.isEmpty() || mNameTitle.isEmpty())
            {
                Toast.makeText(CameraActivity.this, R.string.missing, Toast.LENGTH_LONG).show();
            }
            else {
                uploadImage(mBitmapImage);
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("IS_FROM_UPLOAD", Boolean.valueOf(true));
                context.startActivity(intent);

            }
        }
    });
    btnCancelPhoto.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
        }
    });
    if((ContextCompat.checkSelfPermission(CameraActivity.this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            || (ContextCompat.checkSelfPermission(CameraActivity.this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))

    {
        ActivityCompat.requestPermissions
                (CameraActivity.this, new String[]{
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                },MY_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE);
    }
    if ( getIntent().getStringExtra("type").equalsIgnoreCase(Config.CAMERA_CHOSEN)) {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }
    else if (getIntent().getStringExtra("type").equalsIgnoreCase(Config.GALLERY_CHOSEN)) {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
        if (resultCode == RESULT_OK) {
            mSelectedPath = "/sdcard"+"/"+folderName+"/"+fileName;
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            mBitmapImage = BitmapFactory.decodeFile(mSelectedPath);
            if (mBitmapImage != null) {
                mBitmapImage = DownloadBitmap.scaleDownBitmap(mBitmapImage, Config.SCALE_DOWN_FACTOR, this);
                ImageView imageView = (ImageView) findViewById(R.id.imgCamera);
                imageView.setImageBitmap(mBitmapImage);
            }
            else{
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
            }

        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Image capture cancaled", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Image capture failed", Toast.LENGTH_LONG).show();
        }
    }

    if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
        if (resultCode == RESULT_OK) {
            try {
                Uri selectedImageUri = data.getData();
                mSelectedPath = getRealPathFromURI(selectedImageUri);
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                fileName = data.getData().getLastPathSegment() + ".jpg";
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                mBitmapImage = BitmapFactory.decodeStream(inputStream);
                if (mBitmapImage != null) {
                    mBitmapImage = DownloadBitmap.scaleDownBitmap(mBitmapImage, Config.SCALE_DOWN_FACTOR, this);
                    ImageView imageView = (ImageView) findViewById(R.id.imgCamera);
                    imageView.setImageBitmap(mBitmapImage);
                } else{
                    Intent intent = new Intent(context, MainActivity.class);
                    context.startActivity(intent);
                }

            } catch (FileNotFoundException ex) {
                Logger.getLogger(CameraActivity.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }


    
}
    public String getRealPathFromURI(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) // Source is Dropbox or other similar local file path
            return uri.getPath();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String strRealPath = cursor.getString(column_index);
        if (strRealPath == null)
            return uri.getPath();
        else
            return strRealPath;
   }
  private Uri getOutputMediaFileUri(int MEDIA_TYPE_IMAGE) {
      File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), folderName);
      // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(Config.TAG, "failed to create directory");
                return null;
            }
        }
       
       String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      fileName = "IMG_"+ timeStamp + ".jpg";
       File mediaFile = new File(mediaStorageDir.getPath() + File.separator +  fileName);
        Uri uriSavedImage = Uri.fromFile(mediaFile);
      return uriSavedImage;
        
    }
  
  
  protected void uploadImage(Bitmap bitmap)
  {
       new UploadTask().execute(bitmap);
  }




    private static String readStream(InputStream in) {
    BufferedReader reader = null;
    StringBuilder builder = new StringBuilder();
    try {
        reader = new BufferedReader(new InputStreamReader(in));
        String line = "";
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
    } catch (IOException e) {
        e.printStackTrace();
        FirebaseCrash.report(e);
    } finally {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
                FirebaseCrash.report(e);
            }
        }
    }
    return builder.toString();
}

    private class UploadTask extends AsyncTask<Bitmap, String, String> {

        public String getEmailAddress() {
            if((ContextCompat.checkSelfPermission(CameraActivity.this,
                    android.Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions
                        (CameraActivity.this, new String[]{
                                android.Manifest.permission.GET_ACCOUNTS
                        },MY_PERMISSIONS_REQUEST_GET_ACCOUNT);
            }
            Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
            Account[] accounts = AccountManager.get(CameraActivity.this).getAccounts();
            for (Account account : accounts) {
                if (emailPattern.matcher(account.name).matches()) {
                    return account.name;

                }
            }
            return "";
        }
        protected String doInBackground(Bitmap... bitmaps) {
            if (bitmaps[0] == null)
                return null;
            Location location;
            Bitmap bitmap = bitmaps[0];
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream); // convert Bitmap to ByteArrayOutputStream
            InputStream in = new ByteArrayInputStream(stream.toByteArray()); // convert ByteArrayOutputStream to ByteArrayInputStream
            GeoTagImage tag = new GeoTagImage();
            final SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
            String registrationId = prefs.getString(Config.PROPERTY_REG_ID, "");
            DefaultHttpClient httpclient = new DefaultHttpClient();

                HttpPost httppost = new HttpPost(
                        serverUrl); // server

                MultipartEntity reqEntity = new MultipartEntity();
                reqEntity.addPart("user",getEmailAddress());
                reqEntity.addPart("name",mNameTitle );
                try {
                    //Toast.makeText(CameraActivity.this, fileUri.getPath(), Toast.LENGTH_LONG).show();
                    tag.ReadExif(mSelectedPath);
                    location = tag.readGeoTagImage(mSelectedPath);
                    reqEntity.addPart("picdate", String.valueOf(location.getTime()));
                    reqEntity.addPart("x", String.valueOf(location.getLongitude()));
                    reqEntity.addPart("y", String.valueOf(location.getLatitude()));
                } catch (ParseException e) {
                    e.printStackTrace();
                    FirebaseCrash.report(e);
                }
                reqEntity.addPart("picname",fileName );
                reqEntity.addPart("comment",mComment);
                reqEntity.addPart("reg_id",registrationId);
                reqEntity.addPart("pic", fileName ,in);
                httppost.setEntity(reqEntity);
            Log.i(Config.TAG, "request " + httppost.getRequestLine());
                HttpResponse response = null;
                try {
                    response = httpclient.execute(httppost);
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    FirebaseCrash.report(e);
                    Toast.makeText(CameraActivity.this, R.string.notuploaded, Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    FirebaseCrash.report(e);
                }
                try {
                    if (response != null)
                        Log.i(Config.TAG, "response " + response.getStatusLine().toString());
                } finally {

                }



            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    FirebaseCrash.report(e);
                }
            }

            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    FirebaseCrash.report(e);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            Toast toast = Toast.makeText(CameraActivity.this, Html.fromHtml("<big><em><strong>" + getString(R.string.uploaded) + "</strong></em></big>"), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();


        }


    }

    public class GeoTagImage {

        /**
         *
         * Write Location information to image.
         * @param imagePath : image absolute path
         * @return : location information
         */
        public void MarkGeoTagImage(String imagePath,Location location)
        {
            try {
                ExifInterface exif = new ExifInterface(imagePath);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPS.convert(location.getLatitude()));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPS.latitudeRef(location.getLatitude()));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPS.convert(location.getLongitude()));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPS.longitudeRef(location.getLongitude()));
                SimpleDateFormat fmt_Exif = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                exif.setAttribute(ExifInterface.TAG_DATETIME,fmt_Exif.format(new Date(location.getTime())));
                exif.saveAttributes();
            } catch (IOException e) {
                e.printStackTrace();
                FirebaseCrash.report(e);
            }
        }

        /**
         * Read location information from image.
         * @param imagePath : image absolute path
         * @return : loation information
         */
        public Location readGeoTagImage(String imagePath) throws ParseException {
            Location loc = new Location("");
            try {
                ExifInterface exif = new ExifInterface(imagePath);
                float [] latlong = new float[2] ;
                if(exif.getLatLong(latlong)){
                    loc.setLatitude(latlong[0]);
                    loc.setLongitude(latlong[1]);
                }
                String date = exif.getAttribute(ExifInterface.TAG_DATETIME);
                if (date != null) {
                    SimpleDateFormat fmt_Exif = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                    loc.setTime(fmt_Exif.parse(date).getTime());
                }

            } catch (IOException e) {
                e.printStackTrace();
                FirebaseCrash.report(e);
            } catch (ParseException e) {
                e.printStackTrace();
                FirebaseCrash.report(e);
            }
            return loc;
        }

        String ReadExif(String file){
            if(file == null || file.isEmpty())
                return "";
            String exif="Exif: " + file;
            try {
                ExifInterface exifInterface = new ExifInterface(file);

                exif += "\nIMAGE_LENGTH: " + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
                exif += "\nIMAGE_WIDTH: " + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
                exif += "\n DATETIME: " + exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                exif += "\n TAG_MAKE: " + exifInterface.getAttribute(ExifInterface.TAG_MAKE);
                exif += "\n TAG_MODEL: " + exifInterface.getAttribute(ExifInterface.TAG_MODEL);
                exif += "\n TAG_ORIENTATION: " + exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
                exif += "\n TAG_WHITE_BALANCE: " + exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
                exif += "\n TAG_FOCAL_LENGTH: " + exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
                exif += "\n TAG_FLASH: " + exifInterface.getAttribute(ExifInterface.TAG_FLASH);
                exif += "\nGPS related:";

                float[] LatLong = new float[2];
                if(exifInterface.getLatLong(LatLong)){
                    exif += "\n latitude= " + LatLong[0];
                    exif += "\n longitude= " + LatLong[1];
                }else{
                    exif += "Exif tags are not available!";
                }
                Log.i(Config.TAG, "finished exif: " + exif);


            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

            }

            return exif;
        }
    }


}

//Code to convert  Degrees to DMS unit

class GPS {
    private static StringBuilder sb = new StringBuilder(20);
    /**
     * returns ref for latitude which is S or N.
     *
     * @param latitude
     * @return S or N
     */
    public static String latitudeRef(final double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }

    /**
     * returns ref for latitude which is S or N.
     *
     * @param longitude
     * @return S or N
     */
    public static String longitudeRef(final double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }
    /**
     * convert latitude into DMS (degree minute second) format. For instance<br/>
     * -79.948862 becomes<br/>
     * 79/1,56/1,55903/1000<br/>
     * It works for latitude and longitude<br/>
     *
     * @param latitude could be longitude.
     * @return
     */
    public static final String convert(double latitude) {
        latitude = Math.abs(latitude);
        final int degree = (int)latitude;
        latitude *= 60;
        latitude -= degree * 60.0d;
        final int minute = (int)latitude;
        latitude *= 60;
        latitude -= minute * 60.0d;
        final int second = (int)(latitude * 1000.0d);
        sb.setLength(0);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000,");
        return sb.toString();
    }
}


