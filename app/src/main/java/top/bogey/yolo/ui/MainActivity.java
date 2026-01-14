package top.bogey.yolo.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import top.bogey.yolo.R;
import top.bogey.yolo.bean.YoloManager;
import top.bogey.yolo.databinding.ActivityMainBinding;
import top.bogey.yolo.service.YoloService;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String[]> openDocumentLauncher;

    private ActivityResultCallback resultCallback;

    private final YoloManager yoloManager = YoloManager.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolBar.setNavigationOnClickListener(v -> finish());

        ModelManagerAdapter adapter = new ModelManagerAdapter();
        binding.models.setAdapter(adapter);
        adapter.refresh();

        binding.addButton.setOnClickListener(v -> launcherOpenDocument((code, intent) -> {
            if (code == RESULT_OK) {
                Toast.makeText(this, R.string.importing, Toast.LENGTH_SHORT).show();

                new Thread(() -> {
                    boolean success = yoloManager.importModel(this, intent.getData());
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (success) {
                            Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show();
                            adapter.refresh();
                        } else {
                            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            }
        }, "application/zip"));

        openDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
            if (result != null && resultCallback != null) {
                Intent intent = new Intent();
                intent.setData(result);
                resultCallback.onResult(RESULT_OK, intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 导入模型前需要停止服务，防止模型测试冲突
        stopService(new Intent(this, YoloService.class));
    }

    public void launcherOpenDocument(ActivityResultCallback callback, String mimeType) {
        if (openDocumentLauncher == null) {
            if (callback != null) callback.onResult(Activity.RESULT_CANCELED, null);
            return;
        }
        resultCallback = callback;
        try {
            openDocumentLauncher.launch(new String[]{mimeType});
        } catch (ActivityNotFoundException ignored) {
        }
    }
}
