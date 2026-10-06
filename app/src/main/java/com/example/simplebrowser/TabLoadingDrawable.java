package com.example.simplebrowser;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

/**
 * Lightweight API-19-compatible animated loading spinner for tab favicons.
 */
public class TabLoadingDrawable extends Drawable
        implements Runnable {

    private static final long FRAME_DELAY_MS = 50L;
    private static final float STEP_DEGREES = 18f;

    private final Paint paint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private float rotation;
    private boolean running;

    public TabLoadingDrawable(int color) {

        paint.setStyle(
                Paint.Style.STROKE);

        paint.setStrokeCap(
                Paint.Cap.ROUND);

        paint.setStrokeWidth(
                2.2f);

        setColor(color);
    }

    public void setColor(int color) {
        paint.setColor(color);
        invalidateSelf();
    }

    public void start() {

        if (running) {
            return;
        }

        running = true;

        scheduleSelf(
                this,
                SystemClock.uptimeMillis() +
                        FRAME_DELAY_MS);

        invalidateSelf();
    }

    public void stop() {

        running = false;
        unscheduleSelf(this);
        invalidateSelf();
    }

    @Override
    public void run() {

        if (!running) {
            return;
        }

        rotation =
                (rotation + STEP_DEGREES) %
                        360f;

        invalidateSelf();

        scheduleSelf(
                this,
                SystemClock.uptimeMillis() +
                        FRAME_DELAY_MS);
    }

    @Override
    public void draw(Canvas canvas) {

        Rect bounds =
                getBounds();

        float size =
                Math.min(
                        bounds.width(),
                        bounds.height());

        if (size <= 0f) {
            return;
        }

        float stroke =
                Math.max(
                        1.5f,
                        size * 0.11f);

        paint.setStrokeWidth(stroke);

        float half =
                size / 2f;

        float radius =
                half - stroke;

        RectF arc =
                new RectF(
                        bounds.centerX() - radius,
                        bounds.centerY() - radius,
                        bounds.centerX() + radius,
                        bounds.centerY() + radius);

        canvas.save();

        canvas.rotate(
                rotation,
                bounds.centerX(),
                bounds.centerY());

        canvas.drawArc(
                arc,
                -55f,
                275f,
                false,
                paint);

        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(
            ColorFilter colorFilter) {

        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
