package il.co.jws.app;

import android.app.Application;
import android.content.Context;
import il.co.jws.app.LocaleHelper;

public class MainApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }
}
