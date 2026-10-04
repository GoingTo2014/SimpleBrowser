package com.example.simplebrowser;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Browser toolbar icons drawn directly with Canvas.
 *
 * This avoids media-player/system menu icons and keeps the browser
 * icon set consistent without external dependencies.
 */
public final class BrowserIconDrawable extends Drawable {

    public static final int BACK = 0;
    public static final int FORWARD = 1;
    public static final int HOME = 2;
    public static final int RELOAD = 3;
    public static final int SETTINGS = 4;
    public static final int STOP = 5;

    private final int type;
    private final Paint paint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private int color;
    private int alpha = 255;

    public BrowserIconDrawable(
            int type,
            int color) {

        this.type = type;
        this.color = color;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public void draw(Canvas canvas) {

        Rect bounds =
                getBounds();

        float width =
                bounds.width();

        float height =
                bounds.height();

        if (width <= 0f || height <= 0f) {
            return;
        }

        float scale =
                Math.min(width, height) / 32f;

        float left =
                bounds.left +
                (width - 32f * scale) / 2f;

        float top =
                bounds.top +
                (height - 32f * scale) / 2f;

        canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);

        paint.setColor(color);
        paint.setAlpha(alpha);
        paint.setStrokeWidth(2.5f);

        switch (type) {

            case BACK:
                drawBack(canvas);
                break;

            case FORWARD:
                drawForward(canvas);
                break;

            case HOME:
                drawHome(canvas);
                break;

            case RELOAD:
                drawReload(canvas);
                break;

            case SETTINGS:
                drawSettings(canvas);
                break;

            case STOP:
                drawStop(canvas);
                break;
        }

        canvas.restore();
    }

    private void drawBack(Canvas canvas) {

        Path arrow =
                new Path();

        arrow.moveTo(15f, 7f);
        arrow.lineTo(7f, 16f);
        arrow.lineTo(15f, 25f);

        canvas.drawPath(
                arrow,
                paint);

        canvas.drawLine(
                8f, 16f,
                27f, 16f,
                paint);
    }

    private void drawForward(Canvas canvas) {

        Path arrow =
                new Path();

        arrow.moveTo(17f, 7f);
        arrow.lineTo(25f, 16f);
        arrow.lineTo(17f, 25f);

        canvas.drawPath(
                arrow,
                paint);

        canvas.drawLine(
                5f, 16f,
                24f, 16f,
                paint);
    }

    private void drawHome(Canvas canvas) {

        Path roof =
                new Path();

        roof.moveTo(5f, 14f);
        roof.lineTo(16f, 5f);
        roof.lineTo(27f, 14f);

        canvas.drawPath(
                roof,
                paint);

        Path body =
                new Path();

        body.moveTo(8f, 13f);
        body.lineTo(8f, 26f);
        body.lineTo(24f, 26f);
        body.lineTo(24f, 13f);

        canvas.drawPath(
                body,
                paint);

        canvas.drawLine(
                14f, 26f,
                14f, 19f,
                paint);

        canvas.drawLine(
                18f, 26f,
                18f, 19f,
                paint);

        canvas.drawLine(
                14f, 19f,
                18f, 19f,
                paint);
    }

    private void drawReload(Canvas canvas) {

        RectF arc =
                new RectF(
                        6f, 6f,
                        26f, 26f);

        /*
         * A simple browser-style C arrow rather than the old
         * system/media sync icon.
         */
        canvas.drawArc(
                arc,
                42f,
                278f,
                false,
                paint);

        Path arrow =
                new Path();

        arrow.moveTo(24.5f, 7f);
        arrow.lineTo(24.5f, 14f);
        arrow.lineTo(17.5f, 11.5f);

        canvas.drawPath(
                arrow,
                paint);
    }

    private void drawSettings(Canvas canvas) {

        canvas.drawCircle(
                16f, 16f,
                7.5f,
                paint);

        canvas.drawCircle(
                16f, 16f,
                2.7f,
                paint);

        for (int i = 0; i < 8; i++) {

            double angle =
                    Math.PI * 2d * i / 8d;

            float innerX =
                    16f +
                    (float) Math.cos(angle) *
                            9.5f;

            float innerY =
                    16f +
                    (float) Math.sin(angle) *
                            9.5f;

            float outerX =
                    16f +
                    (float) Math.cos(angle) *
                            12f;

            float outerY =
                    16f +
                    (float) Math.sin(angle) *
                            12f;

            canvas.drawLine(
                    innerX,
                    innerY,
                    outerX,
                    outerY,
                    paint);
        }
    }

    private void drawStop(Canvas canvas) {

        paint.setStyle(Paint.Style.FILL);

        canvas.drawRect(
                8f, 8f,
                24f, 24f,
                paint);

        paint.setStyle(Paint.Style.STROKE);
    }

    public void setIconColor(
            int color) {

        this.color = color;
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(
            android.graphics.ColorFilter filter) {
        // Explicit icon color is used.
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
