package com.example.simplebrowser;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Tab overview icon with the current tab count overlaid in the center.
 */
public final class TabCountDrawable extends Drawable {

    private final BrowserIconDrawable tabsIcon;
    private final Paint textPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final String countText;

    public TabCountDrawable(
            int color,
            int count) {

        tabsIcon =
                new BrowserIconDrawable(
                        BrowserIconDrawable.TABS,
                        color);

        countText =
                count > 99
                        ? "99+"
                        : String.valueOf(
                                Math.max(0, count));

        textPaint.setColor(color);
        textPaint.setStyle(
                Paint.Style.FILL);
        textPaint.setTypeface(
                Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(
                Paint.Align.CENTER);
    }

    @Override
    public void draw(
            Canvas canvas) {

        Rect bounds =
                getBounds();

        canvas.save();
        canvas.scale(
                1.12f,
                1.12f,
                bounds.exactCenterX(),
                bounds.exactCenterY());

        tabsIcon.setBounds(
                bounds);

        tabsIcon.draw(canvas);
        canvas.restore();

        float size =
                Math.min(
                        bounds.width(),
                        bounds.height());

        textPaint.setTextSize(
                Math.max(
                        9f,
                        size * (
                                countText.length() > 2
                                        ? 0.46f
                                        : 0.54f)));

        Paint.FontMetrics metrics =
                textPaint.getFontMetrics();

        float centerX =
                bounds.exactCenterX();

        float centerY =
                bounds.exactCenterY() -
                (metrics.ascent +
                 metrics.descent) / 2f;

        canvas.drawText(
                countText,
                centerX,
                centerY,
                textPaint);
    }

    @Override
    public void setAlpha(
            int alpha) {

        tabsIcon.setAlpha(alpha);
        textPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(
            android.graphics.ColorFilter colorFilter) {

        tabsIcon.setColorFilter(
                colorFilter);

        textPaint.setColorFilter(
                colorFilter);

        invalidateSelf();
    }

    @Override
    public int getOpacity() {

        return android.graphics.PixelFormat.TRANSLUCENT;
    }

    @Override
    protected void onBoundsChange(
            Rect bounds) {

        super.onBoundsChange(bounds);
        tabsIcon.setBounds(bounds);
    }

    @Override
    public int getIntrinsicWidth() {
        return -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return -1;
    }
}
