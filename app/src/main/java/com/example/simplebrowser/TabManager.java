package com.example.simplebrowser;

import android.animation.LayoutTransition;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TabManager {

    private final MainActivity activity;
    private final FrameLayout webViewContainer;
    private final LinearLayout tabsLayout;

    private final List<BrowserTab> tabs =
            new ArrayList<>();

    private BrowserTab activeTab;

    public TabManager(
            MainActivity activity,
            FrameLayout webViewContainer,
            LinearLayout tabsLayout) {

        this.activity = activity;
        this.webViewContainer =
                webViewContainer;
        this.tabsLayout =
                tabsLayout;

        LayoutTransition transition =
                new LayoutTransition();

        transition.setDuration(180);

        tabsLayout.setLayoutTransition(
                transition);
    }

    public BrowserTab addTab(
            String url) {

        BrowserTab tab =
                new BrowserTab();

        tab.webView =
                new WebView(activity);

        configureWebView(tab);
        createTabView(tab);

        tabs.add(tab);

        tabsLayout.addView(
                tab.tabView);

        webViewContainer.addView(
                tab.webView);

        selectTab(tab);

        activity.loadTabUrl(
                tab,
                url);

        return tab;
    }

    private void configureWebView(
            BrowserTab tab) {

        WebSettings settings =
                tab.webView.getSettings();

        BrowserSettings browserSettings =
                activity.getBrowserSettings();

        settings.setJavaScriptEnabled(
                browserSettings
                        .isJavaScriptEnabled());

        settings.setDomStorageEnabled(
                browserSettings
                        .isStorageEnabled());

        settings.setCacheMode(
                WebSettings.LOAD_DEFAULT);

        settings.setSupportMultipleWindows(
                browserSettings
                        .arePopupsEnabled());

        CookieManager.getInstance()
                .setAcceptCookie(
                        browserSettings
                                .areCookiesEnabled());

        tab.webView.setLayerType(
                WebView.LAYER_TYPE_HARDWARE,
                null);

        tab.webView.setWebViewClient(
                new BrowserWebViewClient(
                        activity,
                        tab));

        tab.webView.setWebChromeClient(
                new BrowserChromeClient(
                        activity,
                        tab));

        tab.webView.setOnTouchListener(
                (v, event) -> {

            if (event.getAction() ==
                    MotionEvent.ACTION_DOWN) {

                activity.hideKeyboard();
            }

            return false;
        });
    }

    private void createTabView(
            BrowserTab tab) {

        tab.tabView =
                new LinearLayout(activity);

        tab.tabView.setOrientation(
                LinearLayout.HORIZONTAL);

        tab.tabView.setGravity(
                Gravity.CENTER_VERTICAL);

        tab.tabView.setPadding(
                2, 0, 0, 0);

        tab.titleView =
                new TextView(activity);

        tab.titleView.setText(
                tab.title);

        tab.titleView.setSingleLine(true);

        tab.titleView.setGravity(
                Gravity.CENTER_VERTICAL);

        tab.titleView.setPadding(
                7, 0, 7, 0);

        tab.titleView.setMaxWidth(
                dp(160));

        tab.closeButton =
                new ImageButton(activity);

        tab.closeButton.setImageResource(
                android.R.drawable
                        .ic_menu_close_clear_cancel);

        tab.closeButton.setContentDescription(
                "Close tab");

        tab.closeButton.setBackgroundColor(
                Color.TRANSPARENT);

        tab.closeButton.setPadding(
                6, 6, 6, 6);

        tab.closeButton.setOnClickListener(
                v -> closeTab(tab));

        /*
         * Drag from the title.
         */
        tab.titleView.setOnTouchListener(
                (v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    tab.dragStartX =
                            event.getRawX();

                    tab.dragging = false;

                    return false;

                case MotionEvent.ACTION_MOVE:

                    float distance =
                            event.getRawX()
                            - tab.dragStartX;

                    if (!tab.dragging &&
                            Math.abs(distance) >
                                    dp(10)) {

                        tab.dragging = true;
                    }

                    if (tab.dragging) {

                        tab.tabView
                                .setTranslationX(
                                        distance);

                        return true;
                    }

                    return false;

                case MotionEvent.ACTION_UP:

                    if (tab.dragging) {

                        finishDrag(tab);

                        tab.dragging = false;

                        return true;
                    }

                    selectTab(tab);

                    return true;

                case MotionEvent.ACTION_CANCEL:

                    tab.tabView
                            .animate()
                            .translationX(0)
                            .setDuration(120)
                            .start();

                    tab.dragging = false;

                    return true;
            }

            return false;
        });

        tab.tabView.addView(
                tab.titleView,
                new LinearLayout.LayoutParams(
                        dp(145),
                        dp(36)));

        tab.tabView.addView(
                tab.closeButton,
                new LinearLayout.LayoutParams(
                        dp(34),
                        dp(36)));

        tab.tabView.setOnClickListener(
                v -> selectTab(tab));
    }

    private void finishDrag(
            BrowserTab tab) {

        float visualLeft =
                tab.tabView.getLeft()
                + tab.tabView.getTranslationX();

        float center =
                visualLeft
                + tab.tabView.getWidth() / 2f;

        int oldIndex =
                tabs.indexOf(tab);

        int newIndex =
                oldIndex;

        for (int i = 0;
             i < tabs.size();
             i++) {

            if (i == oldIndex) {
                continue;
            }

            BrowserTab other =
                    tabs.get(i);

            float otherCenter =
                    other.tabView.getLeft()
                    + other.tabView.getWidth()
                    / 2f;

            if (center < otherCenter) {

                newIndex = i;
                break;
            }

            newIndex = i;
        }

        if (newIndex != oldIndex) {

            tabs.remove(oldIndex);

            if (newIndex > oldIndex) {
                newIndex--;
            }

            tabs.add(
                    newIndex,
                    tab);

            tabsLayout.removeView(
                    tab.tabView);

            tabsLayout.addView(
                    tab.tabView,
                    newIndex);
        }

        tab.tabView
                .animate()
                .translationX(0)
                .setDuration(180)
                .start();

        selectTab(tab);
    }

    public void selectTab(
            BrowserTab tab) {

        activeTab = tab;

        for (BrowserTab current : tabs) {

            current.webView.setVisibility(
                    current == tab
                            ? View.VISIBLE
                            : View.GONE);

            updateTabAppearance(
                    current);
        }

        String url;

        if (tab.settingsPage) {

            url = "browser://settings";

        } else {

            url = tab.webView.getUrl();

            if (url == null) {
                url = tab.url;
            }
        }

        if (url == null) {
            url = "";
        }

        activity.setUrlText(url);

        activity.setLoading(
                tab.loading);

        activity.updateSecurity(tab);
    }

    private void updateTabAppearance(
            BrowserTab tab) {

        if (tab == activeTab) {

            String accent =
                    activity
                            .getBrowserSettings()
                            .getAccentColor();

            try {

                tab.titleView.setTextColor(
                        Color.parseColor(accent));

            } catch (Exception e) {

                tab.titleView.setTextColor(
                        Color.BLUE);
            }

        } else {

            tab.titleView.setTextColor(
                    activity
                            .getBrowserSettings()
                            .isDarkMode()
                            ? Color.LTGRAY
                            : Color.DKGRAY);
        }
    }

    public void closeTab(
            BrowserTab tab) {

        if (tabs.size() <= 1) {

            Toast.makeText(
                    activity,
                    "At least one tab must stay open",
                    Toast.LENGTH_SHORT)
                    .show();

            return;
        }

        int index =
                tabs.indexOf(tab);

        boolean wasActive =
                tab == activeTab;

        tabs.remove(tab);

        tabsLayout.removeView(
                tab.tabView);

        webViewContainer.removeView(
                tab.webView);

        tab.webView.stopLoading();
        tab.webView.destroy();

        if (wasActive) {

            int next =
                    Math.min(
                            index,
                            tabs.size() - 1);

            selectTab(
                    tabs.get(next));
        }
    }

    public BrowserTab getActiveTab() {
        return activeTab;
    }

    public List<BrowserTab> getTabs() {
        return tabs;
    }

    public void applyWebSettings() {

        BrowserSettings browserSettings =
                activity.getBrowserSettings();

        for (BrowserTab tab : tabs) {

            WebSettings settings =
                    tab.webView.getSettings();

            if (!tab.settingsPage) {

                settings.setJavaScriptEnabled(
                        browserSettings
                                .isJavaScriptEnabled());
            }

            settings.setDomStorageEnabled(
                    browserSettings
                            .isStorageEnabled());

            settings.setSupportMultipleWindows(
                    browserSettings
                            .arePopupsEnabled());
        }

        CookieManager.getInstance()
                .setAcceptCookie(
                        browserSettings
                                .areCookiesEnabled());

        for (BrowserTab tab : tabs) {

            updateTabAppearance(tab);
        }
    }

    public void updateTabTitle(
            BrowserTab tab) {

        String title =
                tab.title;

        if (title == null ||
                title.trim().isEmpty()) {

            title = "New Tab";
        }

        if (title.length() > 22) {

            title =
                    title.substring(0, 22)
                    + "...";
        }

        tab.titleView.setText(title);
    }

    public void updateTitles() {

        for (BrowserTab tab : tabs) {
            updateTabTitle(tab);
        }
    }

    private int dp(int value) {

        return (int) (
                value *
                activity.getResources()
                        .getDisplayMetrics()
                        .density
                + 0.5f);
    }
}
