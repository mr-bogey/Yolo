package top.bogey.yolo.ui;

import android.content.Intent;

public interface ActivityResultCallback {
    void onResult(int code, Intent intent);
}
