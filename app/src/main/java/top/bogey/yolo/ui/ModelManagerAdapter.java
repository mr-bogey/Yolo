package top.bogey.yolo.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import top.bogey.yolo.R;
import top.bogey.yolo.bean.ModelInfo;
import top.bogey.yolo.bean.YoloManager;
import top.bogey.yolo.databinding.ModelInfoItemBinding;

public class ModelManagerAdapter extends RecyclerView.Adapter<ModelManagerAdapter.ViewHolder> {
    private final List<ModelInfo> models = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ModelInfoItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.refresh(models.get(position));
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    public void refresh() {
        models.clear();
        models.addAll(YoloManager.getInstance().getModels());
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final YoloManager yoloManager = YoloManager.getInstance();
        private final Context context;
        private final ModelInfoItemBinding binding;
        private final Handler handler;
        private boolean delete;
        private ModelInfo modelInfo;

        public ViewHolder(ModelInfoItemBinding binding) {
            super(binding.getRoot());
            this.context = binding.getRoot().getContext();
            this.binding = binding;
            this.handler = new Handler();

            binding.delete.setOnClickListener(v -> {
                if (delete) {
                    yoloManager.removeModel(context, modelInfo.getId());
                    int index = getBindingAdapterPosition();
                    models.remove(index);
                    notifyItemRemoved(index);
                } else {
                    delete = true;
                    binding.delete.setChecked(true);
                    handler.postDelayed(() -> {
                        delete = false;
                        binding.delete.setChecked(false);
                    }, 2000);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                List<String> labels = modelInfo.getLabels();
                String[] labelsArray = new String[labels.size()];
                labels.toArray(labelsArray);

                new MaterialAlertDialogBuilder(context)
                        .setItems(labelsArray, (dialog, which) -> {
                            String label = labels.get(which);
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText(label, label);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(context, R.string.copy_tips, Toast.LENGTH_SHORT).show();
                        })
                        .setTitle(R.string.model_labels)
                        .show();
            });
        }

        public void refresh(ModelInfo modelInfo) {
            this.modelInfo = modelInfo;

            binding.name.setText(modelInfo.getName());
            binding.desc.setText(modelInfo.getDescription());
            binding.time.setText(formatDate(context, modelInfo.getTime(), true));
            StringBuilder builder = new StringBuilder();
            if (!modelInfo.getAuthor().isEmpty()) {
                builder.append(modelInfo.getAuthor()).append("-");
            }
            builder.append(modelInfo.getVersion().name()).append("-").append(modelInfo.getAccelerator().name());
            binding.version.setText(builder.toString());
        }

        public static String formatDate(Context context, long time, boolean ignoreYear) {
            Calendar current = Calendar.getInstance();
            current.setTimeInMillis(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            StringBuilder builder = new StringBuilder();
            if (current.get(Calendar.YEAR) != calendar.get(Calendar.YEAR) || !ignoreYear) builder.append(context.getString(R.string.year, current.get(Calendar.YEAR)));
            builder.append(context.getString(R.string.month, current.get(Calendar.MONTH) + 1));
            builder.append(context.getString(R.string.day, current.get(Calendar.DAY_OF_MONTH)));
            return builder.toString();
        }
    }
}
