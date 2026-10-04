package com.example.simplebrowser;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public final class BrowserIconDrawable extends Drawable {

    public static final int BACK = 0;
    public static final int FORWARD = 1;
    public static final int HOME = 2;
    public static final int RELOAD = 3;
    public static final int MORE = 4;
    public static final int STOP = 5;
    public static final int SETTINGS_PAGE = 6;
    public static final int LOCAL_FILE = 7;
    public static final int SECURE = 8;

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

        paint.setStyle(
                Paint.Style.STROKE);

        paint.setStrokeCap(
                Paint.Cap.ROUND);

        paint.setStrokeJoin(
                Paint.Join.ROUND);
    }

    @Override
    public void draw(
            Canvas canvas) {

        Rect bounds =
                getBounds();

        float width =
                bounds.width();

        float height =
                bounds.height();

        if (width <= 0f ||
                height <= 0f) {
            return;
        }

        float scale =
                Math.min(
                        width,
                        height) / 32f;

        float left =
                bounds.left +
                (width - 32f * scale) / 2f;

        float top =
                bounds.top +
                (height - 32f * scale) / 2f;

        canvas.save();

        canvas.translate(
                left,
                top);

        canvas.scale(
                scale,
                scale);

        paint.setColor(color);
        paint.setAlpha(alpha);
        paint.setStrokeWidth(2.5f);
        paint.setStyle(Paint.Style.STROKE);

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

            case MORE:
                drawMore(canvas);
                break;

            case STOP:
                drawStop(canvas);
                break;

            case SETTINGS_PAGE:
                drawSettingsPage(canvas);
                break;

            case LOCAL_FILE:
                drawLocalFile(canvas);
                break;

            case SECURE:
                drawLock(canvas);
                break;
        }

        canvas.restore();
    }

    private void drawBack(
            Canvas canvas) {

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

    private void drawForward(
            Canvas canvas) {

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

    private void drawHome(
            Canvas canvas) {

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

    private void drawReload(
            Canvas canvas) {

        RectF arc =
                new RectF(
                        6f, 6f,
                        26f, 26f);

        canvas.drawArc(
                arc,
                45f,
                275f,
                false,
                paint);

        Path arrow =
                new Path();

        arrow.moveTo(
                24.5f, 7f);

        arrow.lineTo(
                24.5f, 14f);

        arrow.lineTo(
                17.5f, 11.5f);

        canvas.drawPath(
                arrow,
                paint);
    }

    private void drawMore(
            Canvas canvas) {

        paint.setStyle(
                Paint.Style.FILL);

        canvas.drawCircle(
                8f, 16f,
                2f,
                paint);

        canvas.drawCircle(
                16f, 16f,
                2f,
                paint);

        canvas.drawCircle(
                24f, 16f,
                2f,
                paint);

        paint.setStyle(
                Paint.Style.STROKE);
    }

    private void drawStop(
            Canvas canvas) {

        paint.setStyle(
                Paint.Style.FILL);

        canvas.drawRoundRect(
                new RectF(
                        8f, 8f,
                        24f, 24f),
                2f,
                2f,
                paint);

        paint.setStyle(
                Paint.Style.STROKE);
    }

    private void drawSettingsPage(
            Canvas canvas) {

        /*
         * A browser-settings icon: page outline with
         * simple sliders, intentionally distinct from
         * the normal security lock and local-file icon.
         */
        Path page =
                new Path();

        page.moveTo(8f, 5f);
        page.lineTo(21f, 5f);
        page.lineTo(25f, 9f);
        page.lineTo(25f, 27f);
        page.lineTo(8f, 27f);
        page.close();

        canvas.drawPath(
                page,
                paint);

        canvas.drawLine(
                11f, 13f,
                22f, 13f,
                paint);

        canvas.drawLine(
                11f, 18f,
                22f, 18f,
                paint);

        canvas.drawLine(
                11f, 23f,
                22f, 23f,
                paint);

        paint.setStyle(
                Paint.Style.FILL);

        canvas.drawCircle(
                15f, 13f,
                1.7f,
                paint);

        canvas.drawCircle(
                19f, 18f,
                1.7f,
                paint);

        canvas.drawCircle(
                14f, 23f,
                1.7f,
                paint);

        paint.setStyle(
                Paint.Style.STROKE);
    }

    private void drawLocalFile(
            Canvas canvas) {

        Path page =
                new Path();

        page.moveTo(8f, 4f);
        page.lineTo(20f, 4f);
        page.lineTo(25f, 9f);
        page.lineTo(25f, 28f);
        page.lineTo(8f, 28f);
        page.close();

        canvas.drawPath(
                page,
                paint);

        canvas.drawLine(
                20f, 4f,
                20f, 10f,
                paint);

        canvas.drawLine(
                20f, 10f,
                25f, 10f,
                paint);

        canvas.drawLine(
                11f, 15f,
                22f, 15f,
                paint);

        canvas.drawLine(
                11f, 20f,
                22f, 20f,
                paint);

        canvas.drawLine(
                11f, 25f,
                18f, 25f,
                paint);
    }

    private void drawLock(
            Canvas canvas) {

        RectF body =
                new RectF(
                        7f, 13f,
                        25f, 27f);

        canvas.drawRoundRect(
                body,
                2f,
                2f,
                paint);

        RectF shackle =
                new RectF(
                        11f, 6f,
                        21f, 18f);

        canvas.drawArc(
                shackle,
                180f,
                180f,
                false,
                paint);

        canvas.drawLine(
                11f, 12f,
                11f, 16f,
                paint);

        canvas.drawLine(
                21f, 12f,
                21f, 16f,
                paint);
    }

    public void setIconColor(
            int color) {

        this.color = color;
        invalidateSelf();
    }

    @Override
    public void setAlpha(
            int alpha) {

        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(
            android.graphics.ColorFilter filter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
