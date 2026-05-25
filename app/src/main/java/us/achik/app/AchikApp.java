package us.achik.app;

import android.app.Application;
import com.google.android.gms.ads.MobileAds;

public class AchikApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(() -> MobileAds.initialize(this, status -> {})).start();
    }
}
