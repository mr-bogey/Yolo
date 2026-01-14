package top.bogey.yolo.bean;

import com.google.ai.edge.litert.Accelerator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModelInfo {
    private final String id = UUID.randomUUID().toString();
    private final long time = System.currentTimeMillis();
    private String name;
    private YoloVersion version;
    private String description;
    private String author;
    private List<String> labels;
    private int imageSize;

    private Accelerator accelerator = Accelerator.NONE;

    public static ModelInfo parseModeInfo(String json) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        if (jsonObject == null) return null;
        ModelInfo modelInfo = new ModelInfo();

        modelInfo.name = jsonObject.has("name") ? jsonObject.get("name").getAsString() : "";
        modelInfo.description = jsonObject.has("description") ? jsonObject.get("description").getAsString() : "";
        modelInfo.author = jsonObject.has("author") ? jsonObject.get("author").getAsString() : "";

        String version = jsonObject.has("version") ? jsonObject.get("version").getAsString() : "";
        modelInfo.version = parseVersion(version);

        Type labelsType = new TypeToken<List<String>>() {
        }.getType();
        modelInfo.labels = jsonObject.has("labels") ? gson.fromJson(jsonObject.get("labels"), labelsType) : new ArrayList<>();
        modelInfo.imageSize = jsonObject.has("imageSize") ? jsonObject.get("imageSize").getAsInt() : 640;

        if (modelInfo.isValid()) return modelInfo;
        return null;
    }

    private static YoloVersion parseVersion(String versionName) {
        if (versionName.contains("8")) return YoloVersion.V8;
        if (versionName.contains("10")) return YoloVersion.V10;
        if (versionName.contains("11")) return YoloVersion.V11;
        return YoloVersion.NULL;
    }

    public boolean isValid() {
        if (name == null || name.isEmpty()) return false;
        if (version == YoloVersion.NULL) return false;
        if (labels.isEmpty()) return false;
        if (imageSize % 32 != 0) return false;
        return true;
    }

    public String getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    public String getName() {
        return name;
    }

    public YoloVersion getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public List<String> getLabels() {
        return labels;
    }

    public int getImageSize() {
        return imageSize;
    }

    public Accelerator getAccelerator() {
        return accelerator;
    }

    public void setAccelerator(Accelerator accelerator) {
        this.accelerator = accelerator;
    }

    public int getBoxCount() {
        int p3 = (imageSize / 8) * (imageSize / 8);
        int p4 = (imageSize / 16) * (imageSize / 16);
        int p5 = (imageSize / 32) * (imageSize / 32);
        return p3 + p4 + p5;
    }
}
