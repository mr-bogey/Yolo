package top.bogey.yolo.service;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

public record LetterBox(Bitmap bitmap, float scale, int offsetX, int offsetY) {

    public static LetterBox create(Bitmap bitmap, int targetWidth, int targetHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scale = Math.min(targetWidth * 1f / width, targetHeight * 1f / height);

        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        float offsetX = (targetWidth - newWidth) / 2f;
        float offsetY = (targetHeight - newHeight) / 2f;
        canvas.drawColor(Color.rgb(114, 114, 114));
        canvas.drawBitmap(newBitmap, offsetX, offsetY, null);

        return new LetterBox(result, scale, (int) offsetX, (int) offsetY);
    }
}
