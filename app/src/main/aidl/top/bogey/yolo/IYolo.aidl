// IYolo.aidl
package top.bogey.yolo;

import android.graphics.Bitmap;

import top.bogey.yolo.IYoloCallback;

interface IYolo {
    void runYolo(in Bitmap bitmap, in String modelName, in float similarity, in IYoloCallback callback);

    List<String> getModelList();
}