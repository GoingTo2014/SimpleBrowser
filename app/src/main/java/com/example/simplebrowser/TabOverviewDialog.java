package com.example.simplebrowser;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
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

        final LinearLayout list =
                new LinearLayout(activity);

        list.setOrientation(
                LinearLayout.VERTICAL);

        LinearLayout header =
                new LinearLayout(activity);

        header.setGravity(
                Gravity.CENTER_VERTICAL);

        TextView title =
                new TextView(activity);

        title.setText(
                activity.getTabManager()
                        .isIncognitoMode()
                        ? Localization.translate(
                                activity,
                                "tab.incognito_tabs")
                        : Localization.translate(
                                activity,
                                "Tabs"));
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

        Button newTab =
                new Button(activity);

        newTab.setText(
                Localization.translate(
                        activity,
                        "New Tab"));
        newTab.setAllCaps(false);
        newTab.setTextColor(
                getTextColor());
        newTab.setOnClickListener(
                v -> {
                    activity.getTabManager()
                            .addCurrentModeTab(
                                    activity
                                            .getBrowserSettings()
                                            .getHomePage());

                    refreshTabList(
                            list,
                            dialog);
                });

        header.addView(
                newTab,
                new LinearLayout.LayoutParams(
                        dp(82),
                        dp(44)));

        Button done =
                new Button(activity);

        done.setText(Localization.translate(activity, "tab.done"));
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

        refreshTabList(
                list,
                dialog);

        ScrollView scroll =
                new ScrollView(activity);

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

    private void refreshTabList(
            LinearLayout list,
            Dialog dialog) {

        list.removeAllViews();

        for (BrowserTab tab :
                activity.getTabManager()
                        .getTabs()) {

            list.addView(
                    createTabCard(
                            tab,
                            dialog,
                            list));
        }
    }

    private View createTabCard(
            final BrowserTab tab,
            final Dialog dialog,
            final LinearLayout list) {

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
                                ? Localization.translate(activity, "New Tab")
                                : Localization.translate(activity, "tab.not_loaded"))
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
                        ? Localization.translate(
                                activity,
                                "tab.current")
                        : Localization.translate(
                                activity,
                                "Open"));
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

        ImageButton close =
                new ImageButton(activity);

        close.setImageResource(
                android.R.drawable
                        .ic_menu_close_clear_cancel);

        close.setContentDescription(
                Localization.translate(
                        activity,
                        "Close tab"));

        close.setBackgroundColor(
                Color.TRANSPARENT);

        close.setPadding(
                dp(7),
                dp(7),
                dp(7),
                dp(7));

        close.setOnClickListener(
                v -> {

                    activity.getTabManager()
                            .closeTab(tab);

                    if (activity.getTabManager()
                            .getTabs()
                            .isEmpty()) {

                        dialog.dismiss();
                        return;
                    }

                    refreshTabList(
                            list,
                            dialog);
                });

        card.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(44),
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

        if (tab == null ||
                !tab.hasLoaded) {
            return null;
        }

        return TabPreview.capture(
                tab.webView,
                targetWidth * 2,
                targetHeight * 2);
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
