package il.co.jws.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by boaz on 07/08/2016.
 */
public class FontSizeActivity extends Activity {

    SeekBar seekBarFontSize;
    TextView textViewSampleText;
    Button btnOk;
    int textSize = 2;
    int saveProgress;
    private Context context;
     int startpoint = 15;
    int maxpoint = 30;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fontsizeseekbar);
        btnOk = (Button)findViewById(R.id.btnOkChangeFontSize);
        context = this;
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
            }
        });
        textViewSampleText = (TextView)findViewById(R.id.textViewSampleText);
        textViewSampleText.setTextScaleX(textSize);
        seekBarFontSize = (SeekBar)findViewById(R.id.seekBarFontSize);
        SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
        seekBarFontSize.setMax(maxpoint);
        int progress = prefs.getInt(Config.PREFS_ZOOM_TEXT, 100) - 100 + startpoint;
        saveProgress = progress;
        seekBarFontSize.setProgress(progress);
        textSize = textSize + progress;
        textViewSampleText.setTextSize(textSize);
        seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                int delta = progress-saveProgress;
                textSize = textSize + delta;
                saveProgress = progress;
                textViewSampleText.setTextSize(textSize);
                SharedPreferences prefs = context.getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(Config.PREFS_ZOOM_TEXT, prefs.getInt(Config.PREFS_ZOOM_TEXT, 100) + delta);
                editor.commit();
            }
        });

    }
}
