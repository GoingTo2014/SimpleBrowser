package com.example.simplebrowser;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Shows every tab in the current session with a readable live-page preview.
 */
public class TabOverviewDialog {

    private final MainActivity activity;
    private final PreviewStore previewStore;

    public TabOverviewDialog(
            MainActivity activity) {

        this.activity = activity;
        this.previewStore =
                new PreviewStore(activity);
    }

    public void show() {

        final Dialog dialog =
                new Dialog(activity);

        LinearLayout root =
                new LinearLayout(activity);

        root.setOrientation(
                LinearLayout.VERTICAL);
        root.setPadding(
                dp(10),
                dp(10),
                dp(10),
                dp(10));

        int background =
                getBackgroundColor();

        root.setBackgroundColor(
                background);

        LinearLayout header =
                new LinearLayout(activity);

        header.setGravity(
                Gravity.CENTER_VERTICAL);

        TextView title =
                new TextView(activity);

        title.setText(
                activity.getTabManager()
                        .isIncognitoMode()
                        ? "Incognito tabs"
                        : "Tabs");
        title.setTextSize(20);
        title.setTypeface(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
        title.setTextColor(
                getTextColor());

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1f));

        Button done =
                new Button(activity);

        done.setText("Done");
        done.setAllCaps(false);
        done.setTextColor(
                getTextColor());
        done.setOnClickListener(
                v -> dialog.dismiss());

        header.addView(
                done,
                new LinearLayout.LayoutParams(
                        dp(75),
                        dp(44)));

        root.addView(header);

        ScrollView scroll =
                new ScrollView(activity);

        LinearLayout list =
                new LinearLayout(activity);

        list.setOrientation(
                LinearLayout.VERTICAL);

        for (BrowserTab tab :
                activity.getTabManager()
                        .getTabs()) {

            list.addView(
                    createTabCard(
                            tab,
                            dialog));
        }

        scroll.addView(list);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1f));

        dialog.setContentView(root);
        dialog.show();

        if (dialog.getWindow() != null) {

            int width =
                    Math.min(
                            dp(560),
                            (int) (
                                    activity
                                            .getResources()
                                            .getDisplayMetrics()
                                            .widthPixels *
                                    0.94f));

            int height =
                    Math.min(
                            dp(650),
                            (int) (
                                    activity
                                            .getResources()
                                            .getDisplayMetrics()
                                            .heightPixels *
                                    0.88f));

            dialog.getWindow().setLayout(
                    width,
                    height);

            dialog.getWindow()
                    .setBackgroundDrawable(
                            new ColorDrawable(
                                    background));
        }
    }

    private View createTabCard(
            final BrowserTab tab,
            final Dialog dialog) {

        LinearLayout wrapper =
                new LinearLayout(activity);

        wrapper.setOrientation(
                LinearLayout.VERTICAL);

        LinearLayout card =
                new LinearLayout(activity);

        card.setGravity(
                Gravity.CENTER_VERTICAL);
        card.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8));

        card.setBackgroundColor(
                tab == activity.getActiveTab()
                        ? ColorUtils.darken(
                                getAccentColor(),
                                0.08f)
                        : getSurfaceColor());

        int previewWidth =
                Math.min(
                        dp(220),
                        Math.max(
                                dp(140),
                                (int) (
                                        activity
                                                .getResources()
                                                .getDisplayMetrics()
                                                .widthPixels *
                                        0.38f)));

        int previewHeight =
                Math.max(
                        dp(79),
                        Math.round(
                                previewWidth *
                                9f / 16f));

        ImageView preview =
                new ImageView(activity);

        preview.setScaleType(
                ImageView.ScaleType.CENTER_CROP);

        Bitmap bitmap =
                createPreview(
                        tab,
                        previewWidth,
                        previewHeight);

        if (bitmap != null) {
            preview.setImageBitmap(bitmap);
            previewStore.save(
                    tab.previewKey,
                    bitmap);
        } else {
            Bitmap saved =
                    previewStore.load(
                            tab.previewKey);

            if (saved != null) {
                preview.setImageBitmap(saved);
            } else {
                preview.setBackgroundColor(
                        ColorUtils.darken(
                                getSurfaceColor(),
                                0.05f));
            }
        }

        card.addView(
                preview,
                new LinearLayout.LayoutParams(
                        previewWidth,
                        previewHeight));

        LinearLayout info =
                new LinearLayout(activity);

        info.setOrientation(
                LinearLayout.VERTICAL);
        info.setPadding(
                dp(10),
                0,
                dp(6),
                0);

        TextView tabTitle =
                new TextView(activity);

        tabTitle.setText(
                tab.title == null ||
                tab.title.trim().isEmpty()
                        ? "New Tab"
                        : tab.title);
        tabTitle.setTextSize(15);
        tabTitle.setMaxLines(2);
        tabTitle.setTextColor(
                getTextColor());

        info.addView(
                tabTitle,
                new LinearLayout.LayoutParams(
                        -1,
                        -2));

        TextView url =
                new TextView(activity);

        String displayUrl =
                BrowserPage.displayUrl(tab);

        url.setText(
                displayUrl.isEmpty()
                        ? (tab.hasLoaded
                                ? "New Tab"
                                : "Not loaded")
                        : displayUrl);
        url.setTextSize(12);
        url.setMaxLines(2);
        url.setTextColor(
                getSecondaryColor());

        info.addView(
                url,
                new LinearLayout.LayoutParams(
                        -1,
                        -2));

        card.addView(
                info,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1f));

        Button open =
                new Button(activity);

        open.setText(
                tab == activity.getActiveTab()
                        ? "Current"
                        : "Open");
        open.setAllCaps(false);
        open.setTextColor(
                getTextColor());
        open.setOnClickListener(
                v -> {
                    activity.getTabManager()
                            .selectTab(tab);
                    dialog.dismiss();
                });

        card.addView(
                open,
                new LinearLayout.LayoutParams(
                        dp(78),
                        dp(44)));

        card.setOnClickListener(
                v -> {
                    activity.getTabManager()
                            .selectTab(tab);
                    dialog.dismiss();
                });

        wrapper.addView(
                card,
                new LinearLayout.LayoutParams(
                        -1,
                        previewHeight + dp(16)));

        View divider =
                new View(activity);

        divider.setBackgroundColor(
                getBorderColor());

        wrapper.addView(
                divider,
                new LinearLayout.LayoutParams(
                        -1,
                        1));

        return wrapper;
    }

    private Bitmap createPreview(
            BrowserTab tab,
            int targetWidth,
            int targetHeight) {

        if (!tab.hasLoaded ||
                tab.webView == null) {
            return null;
        }

        try {

            int width =
                    Math.max(
                            1,
                            tab.webView.getWidth());

            int height =
                    Math.max(
                            1,
                            tab.webView.getHeight());

            int bitmapWidth =
                    targetWidth * 2;

            int bitmapHeight =
                    targetHeight * 2;

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            bitmapWidth,
                            bitmapHeight,
                            Bitmap.Config.ARGB_8888);

            Canvas canvas =
                    new Canvas(bitmap);

            canvas.drawColor(
                    Color.WHITE);

            if (width > 1 &&
                    height > 1) {

                float scale =
                        Math.max(
                                bitmapWidth /
                                        (float) width,
                                bitmapHeight /
                                        (float) height);

                float scaledWidth =
                        width * scale;

                float scaledHeight =
                        height * scale;

                canvas.save();
                canvas.translate(
                        (bitmapWidth -
                                scaledWidth) / 2f,
                        (bitmapHeight -
                                scaledHeight) / 2f);
                canvas.scale(
                        scale,
                        scale);
                tab.webView.draw(canvas);
                canvas.restore();

                return bitmap;
            }

            Picture picture =
                    tab.webView.capturePicture();

            if (picture == null ||
                    picture.getWidth() <= 0 ||
                    picture.getHeight() <= 0) {
                bitmap.recycle();
                return null;
            }

            float scale =
                    Math.min(
                            bitmapWidth /
                                    (float) picture.getWidth(),
                            bitmapHeight /
                                    (float) picture.getHeight());

            canvas.save();
            canvas.scale(
                    scale,
                    scale);
            picture.draw(canvas);
            canvas.restore();

            return bitmap;

        } catch (Throwable ignored) {

            return null;
        }
    }

    private int getAccentColor() {

        return ColorUtils.parseColor(
                activity.getBrowserSettings()
                        .getAccentColor(),
                Color.WHITE);
    }

    private int getBackgroundColor() {

        BrowserTab active =
                activity.getActiveTab();

        if (active != null &&
                active.isIncognito) {
            return Color.rgb(
                    32,
                    33,
                    36);
        }

        return ColorUtils.mix(
                getAccentColor(),
                Color.WHITE,
                0.94f);
    }

    private int getSurfaceColor() {

        BrowserTab active =
                activity.getActiveTab();

        if (active != null &&
                active.isIncognito) {
            return Color.rgb(
                    48,
                    49,
                    52);
        }

        return ColorUtils.mix(
                getAccentColor(),
                Color.WHITE,
                0.90f);
    }

    private int getTextColor() {

        return ColorUtils.getReadableTextColor(
                getBackgroundColor());
    }

    private int getSecondaryColor() {

        return ColorUtils.ensureContrast(
                Color.rgb(
                        95,
                        95,
                        95),
                getBackgroundColor(),
                4.5d);
    }

    private int getBorderColor() {

        return ColorUtils.ensureContrast(
                getAccentColor(),
                getSurfaceColor(),
                2.0d);
    }

    private int dp(int value) {

        return (int) (
                value *
                activity.getResources()
                        .getDisplayMetrics()
                        .density +
                0.5f);
    }
}
