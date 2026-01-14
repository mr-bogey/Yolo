package top.bogey.yolo.bean;

import android.content.Context;
import android.net.Uri;

import com.google.ai.edge.litert.Accelerator;
import com.google.ai.edge.litert.CompiledModel;
import com.google.ai.edge.litert.LiteRtException;
import com.google.gson.Gson;
import com.tencent.mmkv.MMKV;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class YoloManager {
    private static final String MODEL = "model.tflite";
    private static final String MODEL_SUFFIX = ".tflite";
    private static final String METADATA = "metadata.json";
    private static final String MODEL_PATH = "models";

    private static YoloManager instance;

    public static YoloManager getInstance() {
        if (instance == null) instance = new YoloManager();
        return instance;
    }

    private final Map<String, ModelInfo> models = new HashMap<>();

    private final MMKV mmkv = MMKV.defaultMMKV();
    private final Gson gson = new Gson();

    private YoloManager() {
        String[] keys = mmkv.allKeys();
        if (keys != null) {
            for (String key : keys) {
                String json = mmkv.decodeString(key);
                ModelInfo modelInfo = gson.fromJson(json, ModelInfo.class);
                models.put(modelInfo.getId(), modelInfo);
            }
        }
    }

    public boolean importModel(Context context, Uri uri) {
        // 读取模型配置信息
        ModelInfo modelInfo = null;
        try (ZipInputStream zipInputStream = new ZipInputStream(context.getContentResolver().openInputStream(uri))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String name = zipEntry.getName();
                if (METADATA.equals(name)) {
                    StringBuilder builder = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zipInputStream.read(buffer)) != -1) {
                        builder.append(new String(buffer, 0, len));
                    }
                    modelInfo = ModelInfo.parseModeInfo(builder.toString());
                }
            }
        } catch (Exception ignored) {
        }
        if (modelInfo == null) return false;

        String modelDirPath = context.getFilesDir() + File.separator + MODEL_PATH + File.separator + modelInfo.getId();
        File modelDir = new File(modelDirPath);
        if (!modelDir.exists()) if (!modelDir.mkdirs()) return false;

        try (ZipInputStream zipInputStream = new ZipInputStream(context.getContentResolver().openInputStream(uri))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String name = zipEntry.getName();
                if (name.toLowerCase().contains(MODEL_SUFFIX)) {
                    File modelFile = new File(modelDirPath, MODEL);

                    try (FileOutputStream outputStream = new FileOutputStream(modelFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, len);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 测试模型可加速类型
        String modelPath = getModelPath(context, modelInfo.getId());
        try {
            CompiledModel model = CompiledModel.create(modelPath, new CompiledModel.Options(Accelerator.GPU));
            modelInfo.setAccelerator(Accelerator.GPU);
            model.close();
        } catch (LiteRtException ignored) {
            modelInfo.setAccelerator(Accelerator.CPU);
        }
        saveModelInfo(modelInfo);
        return true;
    }

    public void saveModelInfo(ModelInfo modelInfo) {
        String json = gson.toJson(modelInfo);
        mmkv.encode(modelInfo.getId(), json);
        models.put(modelInfo.getId(), modelInfo);
    }

    public void removeModel(Context context, String id) {
        ModelInfo modelInfo = models.get(id);
        if (modelInfo == null) return;
        models.remove(id);
        mmkv.removeValueForKey(id);
        String path = getModelPath(context, modelInfo.getId());
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    public ModelInfo findModel(String name) {
        for (ModelInfo value : models.values()) {
            if (value.getName().equals(name)) return value;
        }
        return null;
    }

    public List<ModelInfo> getModels() {
        return new ArrayList<>(models.values());
    }

    public String getModelPath(Context context, String path) {
        return context.getFilesDir() + File.separator + MODEL_PATH + File.separator + path + File.separator + MODEL;
    }
}
