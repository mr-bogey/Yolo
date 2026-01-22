package top.bogey.yolo.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.ai.edge.litert.Accelerator;
import com.google.ai.edge.litert.CompiledModel;
import com.google.ai.edge.litert.Environment;
import com.google.ai.edge.litert.LiteRtException;
import com.google.ai.edge.litert.TensorBuffer;

import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import top.bogey.yolo.IYolo;
import top.bogey.yolo.IYoloCallback;
import top.bogey.yolo.bean.ModelInfo;
import top.bogey.yolo.bean.YoloManager;

public class YoloService extends Service {
    private Environment gpuEnvironment;
    private final Map<String, CompiledModel> modelCache = new HashMap<>();

    private final IYolo.Stub stub = new IYolo.Stub() {
        private static final float IOU_THRESHOLD = 0.5f;
        private final YoloManager yoloManager = YoloManager.getInstance();

        @Override
        public void runYolo(Bitmap bitmap, String modelName, float similarity, IYoloCallback callback) throws RemoteException {
            ModelInfo modelInfo = yoloManager.findModel(modelName);
            if (modelInfo == null) {
                callback.onResult(new ArrayList<>());
                return;
            }

            CompiledModel model = getModel(modelInfo);
            if (model == null) {
                callback.onResult(new ArrayList<>());
                return;
            }
            Log.d("TAG", "runYolo: " + modelName);

            // 处理图片
            int imageSize = modelInfo.getImageSize();
            LetterBox letterBox = LetterBox.create(bitmap, imageSize, imageSize);
            TensorImage tensorImage = TensorImage.fromBitmap(letterBox.bitmap());
            ImageProcessor processor = new ImageProcessor.Builder().add(new NormalizeOp(0.0f, 255.0f)).build();
            tensorImage = processor.process(tensorImage);
            float[] floatArray = tensorImage.getTensorBuffer().getFloatArray();

            // 运行模型
            float[] resultFloatArray = null;
            List<TensorBuffer> inputs = null;
            List<TensorBuffer> outputs = null;
            try {
                inputs = model.createInputBuffers();
                outputs = model.createOutputBuffers();
                inputs.get(0).writeFloat(floatArray);
                model.run(inputs, outputs);
                resultFloatArray = outputs.get(0).readFloat();
            } catch (LiteRtException e) {
                e.printStackTrace();
            } finally {
                if (inputs != null) inputs.forEach(TensorBuffer::close);
                if (outputs != null) outputs.forEach(TensorBuffer::close);
            }

            // 解析结果
            List<YoloResult> results = switch (modelInfo.getVersion()) {
                case V8, V11 -> parseOutput(resultFloatArray, modelInfo, similarity);
                case V10 -> parseOutputV10(resultFloatArray, modelInfo, similarity);
                default -> null;
            };

            if (results == null) {
                callback.onResult(new ArrayList<>());
                return;
            }

            Log.d("TAG", "runYolo: " + results);

            // 还原坐标
            for (YoloResult result : results) {
                RectF rect = result.getArea();
                float offsetX = letterBox.offsetX();
                float offsetY = letterBox.offsetY();
                float scale = letterBox.scale();

                float left = (rect.left * imageSize - offsetX) / scale;
                float top = (rect.top * imageSize - offsetY) / scale;
                float right = (rect.right * imageSize - offsetX) / scale;
                float bottom = (rect.bottom * imageSize - offsetY) / scale;
                rect.set(left, top, right, bottom);
            }

            Log.d("TAG", "runYolo: " + results);
            Log.d("TAG", "runYolo: " + results.size());
            callback.onResult(results);
        }

        private CompiledModel getModel(ModelInfo modelInfo) {
            String modelPath = yoloManager.getModelPath(YoloService.this, modelInfo.getId());
            CompiledModel model = modelCache.get(modelInfo.getId());
            if (model != null) return model;
            try {
                switch (modelInfo.getAccelerator()) {
                    case GPU -> {
                        if (gpuEnvironment == null) gpuEnvironment = Environment.create();
                        model = CompiledModel.create(modelPath, new CompiledModel.Options(Accelerator.GPU), gpuEnvironment);
                    }
                    case CPU -> model = CompiledModel.create(modelPath, new CompiledModel.Options(Accelerator.CPU));
                }
                modelCache.put(modelInfo.getId(), model);
            } catch (LiteRtException ignored) {
                modelCache.forEach((key, value) -> value.close());
            }
            return model;
        }

        private List<YoloResult> parseOutput(float[] output, ModelInfo modelInfo, float confThreshold) {
            List<String> labels = modelInfo.getLabels();
            int classesNum = labels.size();
            int boxCount = modelInfo.getBoxCount();

            List<YoloResult> results = new ArrayList<>();
            for (int i = 0; i < boxCount; i++) {
                float maxScore = -1f;
                int classId = -1;

                for (int j = 0; j < classesNum; j++) {
                    float score = output[(4 + j) * boxCount + i];
                    if (score > maxScore) {
                        maxScore = score;
                        classId = j;
                    }
                }

                if (maxScore < confThreshold) continue;

                String label = labels.get(classId);

                float cx = output[i];
                float cy = output[boxCount + i];
                float w = output[2 * boxCount + i];
                float h = output[3 * boxCount + i];

                float left = cx - w / 2;
                float top = cy - h / 2;
                float right = cx + w / 2;
                float bottom = cy + h / 2;

                YoloResult result = new YoloResult(new RectF(left, top, right, bottom), label, maxScore);
                results.add(result);
            }
            return nonMaxSuppression(results);
        }

        private List<YoloResult> parseOutputV10(float[] output, ModelInfo modelInfo, float confThreshold) {
            List<String> labels = modelInfo.getLabels();
            int channelNum = 6;
            int boxCount = 300;

            List<YoloResult> results = new ArrayList<>();
            for (int i = 0; i < boxCount; i++) {
                float score = output[i * channelNum + 4];
                if (score < confThreshold) continue;

                float left = output[i * channelNum];
                float top = output[i * channelNum + 1];
                float right = output[i * channelNum + 2];
                float bottom = output[i * channelNum + 3];
                int classId = (int) output[i * channelNum + 5];
                String label = labels.get(classId);

                YoloResult result = new YoloResult(new RectF(left, top, right, bottom), label, score);
                results.add(result);
            }
            return results;
        }

        private List<YoloResult> nonMaxSuppression(List<YoloResult> list) {
            List<YoloResult> results = new ArrayList<>();
            list.sort((a, b) -> Float.compare(b.getSimilar(), a.getSimilar()));

            while (!list.isEmpty()) {
                YoloResult current = list.remove(0);
                results.add(current);

                Iterator<YoloResult> iterator = list.iterator();
                while (iterator.hasNext()) {
                    YoloResult other = iterator.next();
                    if (other.getName().equals(current.getName())) {
                        float iou = intersectionOverUnion(current, other);
                        if (iou > IOU_THRESHOLD) {
                            iterator.remove();
                        }
                    }
                }
            }

            return results;
        }

        private float intersectionOverUnion(YoloResult a, YoloResult b) {
            RectF aRect = a.getArea();
            RectF bRect = b.getArea();
            float left = Math.max(aRect.left, bRect.left);
            float top = Math.max(aRect.top, bRect.top);
            float right = Math.min(aRect.right, bRect.right);
            float bottom = Math.min(aRect.bottom, bRect.bottom);

            float width = Math.max(right - left, 0);
            float height = Math.max(bottom - top, 0);
            float intersectionArea = width * height;

            float aArea = aRect.width() * aRect.height();
            float bArea = bRect.width() * bRect.height();
            float unionArea = aArea + bArea - intersectionArea;
            if (unionArea > 0) return intersectionArea / unionArea;
            return 0;
        }

        @Override
        public List<String> getModelList() {
            List<String> list = new ArrayList<>();
            for (ModelInfo model : yoloManager.getModels()) {
                list.add(model.getName());
            }
            return list;
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        modelCache.forEach((id, model) -> model.close());
        modelCache.clear();
    }
}
