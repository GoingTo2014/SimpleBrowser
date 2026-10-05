package com.example.simplebrowser;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.webkit.WebView;

/**
 * Shared tab-preview renderer.
 */
public final class TabPreview {

    private TabPreview() {
    }

    public static Bitmap capture(
            WebView webView,
            int targetWidth,
            int targetHeight) {

        if (webView == null ||
                targetWidth <= 0 ||
                targetHeight <= 0) {
            return null;
        }

        try {
            int viewWidth =
                    Math.max(1, webView.getWidth());

            int viewHeight =
                    Math.max(1, webView.getHeight());

            int width =
                    Math.max(1, targetWidth);

            int height =
                    Math.max(1, targetHeight);

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888);

            Canvas canvas =
                    new Canvas(bitmap);

            canvas.drawColor(Color.WHITE);

            if (viewWidth > 1 &&
                    viewHeight > 1) {

                float scale =
                        Math.max(
                                width /
                                        (float) viewWidth,
                                height /
                                        (float) viewHeight);

                float scaledWidth =
                        viewWidth * scale;

                float scaledHeight =
                        viewHeight * scale;

                canvas.save();

                canvas.translate(
                        (width - scaledWidth) / 2f,
                        (height - scaledHeight) / 2f);

                canvas.scale(
                        scale,
                        scale);

                webView.draw(canvas);

                canvas.restore();

                return bitmap;
            }

            Picture picture =
                    webView.capturePicture();

            if (picture == null ||
                    picture.getWidth() <= 0 ||
                    picture.getHeight() <= 0) {

                bitmap.recycle();
                return null;
            }

            float scale =
                    Math.min(
                            width /
                                    (float) picture.getWidth(),
                            height /
                                    (float) picture.getHeight());

            canvas.save();
            canvas.scale(scale, scale);
            picture.draw(canvas);
            canvas.restore();

            return bitmap;

        } catch (Throwable ignored) {
            return null;
        }
    }
}
