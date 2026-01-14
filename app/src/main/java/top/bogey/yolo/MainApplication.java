package top.bogey.yolo;

import android.app.Application;

import com.tencent.mmkv.MMKV;

public class MainApplication extends Application {
    private static MainApplication instance;

    public static MainApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        MMKV.initialize(this);
    }
}
