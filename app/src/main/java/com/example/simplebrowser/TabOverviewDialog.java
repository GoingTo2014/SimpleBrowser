package com.example.simplebrowser;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class TabOverviewDialog {

    private final MainActivity activity;

    public TabOverviewDialog(
            MainActivity activity) {

        this.activity = activity;
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
                activity
                        .getTabManager()
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

        Button close =
                new Button(activity);

        close.setText("Done");
        close.setAllCaps(false);
        close.setTextColor(
                getTextColor());

        close.setOnClickListener(
                v -> dialog.dismiss());

        header.addView(
                close,
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
                activity
                        .getTabManager()
                        .getTabs()) {

            list.addView(
                    createTabCard(
                            tab,
                            dialog));
        }

        scroll.addView(
                list,
                new ScrollView.LayoutParams(
                        -1,
                        -2));

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1f));

        dialog.setContentView(root);

        if (dialog.getWindow() != null) {

            dialog.getWindow()
                    .setLayout(
                            dp(560),
                            dp(650));

            dialog.getWindow()
                    .setBackgroundDrawable(
                            new android.graphics.drawable
                                    .ColorDrawable(
                                            background));
        }

        dialog.show();

        if (dialog.getWindow() != null) {

            dialog.getWindow()
                    .setLayout(
                            Math.min(
                                    dp(560),
                                    (int) (
                                            activity
                                                    .getResources()
                                                    .getDisplayMetrics()
                                                    .widthPixels *
                                            0.94f)),
                            Math.min(
                                    dp(650),
                                    (int) (
                                            activity
                                                    .getResources()
                                                    .getDisplayMetrics()
                                                    .heightPixels *
                                            0.88f)));
        }
    }

    private View createTabCard(
            final BrowserTab tab,
            final Dialog dialog) {

        LinearLayout card =
                new LinearLayout(activity);

        card.setOrientation(
                LinearLayout.HORIZONTAL);

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

        ImageView preview =
                new ImageView(activity);

        preview.setScaleType(
                ImageView.ScaleType.CENTER_CROP);

        Bitmap bitmap =
                createPreview(tab);

        if (bitmap != null) {

            preview.setImageBitmap(
                    bitmap);

        } else {

            preview.setBackgroundColor(
                    ColorUtils.darken(
                            getSurfaceColor(),
                            0.05f));

            preview.setImageDrawable(
                    null);
        }

        card.addView(
                preview,
                new LinearLayout.LayoutParams(
                        dp(150),
                        dp(88)));

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
                getDisplayUrl(tab);

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

        Button select =
                new Button(activity);

        select.setText(
                tab == activity.getActiveTab()
                        ? "Current"
                        : "Open");

        select.setAllCaps(false);
        select.setTextColor(
                getTextColor());

        select.setOnClickListener(
                v -> {

                    activity
                            .getTabManager()
                            .selectTab(tab);

                    dialog.dismiss();
                });

        card.addView(
                select,
                new LinearLayout.LayoutParams(
                        dp(78),
                        dp(44)));

        card.setOnClickListener(
                v -> {

                    activity
                            .getTabManager()
                            .selectTab(tab);

                    dialog.dismiss();
                });

        card.setOnLongClickListener(
                v -> true);

        View divider =
                new View(activity);

        divider.setBackgroundColor(
                getBorderColor());

        LinearLayout wrapper =
                new LinearLayout(activity);

        wrapper.setOrientation(
                LinearLayout.VERTICAL);

        wrapper.addView(
                card,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(106)));

        wrapper.addView(
                divider,
                new LinearLayout.LayoutParams(
                        -1,
                        1));

        return wrapper;
    }

    private Bitmap createPreview(
            BrowserTab tab) {

        if (!tab.hasLoaded ||
                tab.webView == null) {
            return null;
        }

        try {

            Picture picture =
                    tab.webView.capturePicture();

            if (picture == null ||
                    picture.getWidth() <= 0 ||
                    picture.getHeight() <= 0) {
                return null;
            }

            int width = 320;
            int height = 180;

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888);

            Canvas canvas =
                    new Canvas(bitmap);

            canvas.drawColor(
                    Color.WHITE);

            float scale =
                    Math.min(
                            width /
                                    (float) picture
                                            .getWidth(),
                            height /
                                    (float) picture
                                            .getHeight());

            if (scale > 1f) {
                scale = 1f;
            }

            float drawnWidth =
                    picture.getWidth() * scale;

            float drawnHeight =
                    picture.getHeight() * scale;

            canvas.translate(
                    (width - drawnWidth) / 2f,
                    (height - drawnHeight) / 2f);

            canvas.scale(
                    scale,
                    scale);

            picture.draw(canvas);

            return bitmap;

        } catch (Throwable ignored) {

            return null;
        }
    }

    private String getDisplayUrl(
            BrowserTab tab) {

        if (tab.defaultPage) {
            return "";
        }

        if (tab.settingsPage) {
            return activity.getSettingsUrl(
                    tab.settingsSection);
        }

        if (tab.historyPage) {
            return "browser://history";
        }

        if (tab.downloadsPage) {
            return "browser://downloads";
        }

        if (tab.errorPage) {
            return tab.url == null
                    ? ""
                    : tab.url;
        }

        return tab.url == null
                ? ""
                : tab.url;
    }

    private int getAccentColor() {

        return ColorUtils.parseColor(
                activity
                        .getBrowserSettings()
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
                activity
                        .getResources()
                        .getDisplayMetrics()
                        .density +
                0.5f);
    }
}
