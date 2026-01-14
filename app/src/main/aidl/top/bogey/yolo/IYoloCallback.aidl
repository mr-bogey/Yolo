// IYoloCallback.aidl
package top.bogey.yolo;

import top.bogey.yolo.service.YoloResult;

interface IYoloCallback {
    void onResult(in List<YoloResult> result);
}