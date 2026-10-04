package com.example.simplebrowser;

import android.animation.LayoutTransition;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.os.Handler;
import android.os.Looper;
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

    private static final long DRAG_HOLD_MS =
            350L;

    /*
     * A normal desktop Chrome-style UA is used when
     * Desktop mode is enabled. The WebView engine itself
     * remains the Android 4.4-compatible WebView.
     */
    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36";

    private final MainActivity activity;
    private final FrameLayout webViewContainer;
    private final LinearLayout tabsLayout;
    private final LayoutTransition tabTransition;

    private final List<BrowserTab> tabs =
            new ArrayList<>();

    private final Handler dragHandler =
            new Handler(Looper.getMainLooper());

    private BrowserTab activeTab;
    private BrowserTab pendingDragTab;

    public TabManager(
            MainActivity activity,
            FrameLayout webViewContainer,
            LinearLayout tabsLayout) {

        this.activity = activity;
        this.webViewContainer =
                webViewContainer;
        this.tabsLayout =
                tabsLayout;

        tabTransition =
                new LayoutTransition();

        tabTransition.setDuration(180);

        tabsLayout.setLayoutTransition(
                tabTransition);
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

        applyWebSettings(
                settings,
                activity.getBrowserSettings());

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
         * Hold the tab title for 350 ms to enter
         * a real drag mode. Moving then reorders both
         * the backing list and the visible tab bar.
         */
        tab.titleView.setOnTouchListener(
                (v, event) -> {

                    switch (event.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            tab.dragStartX =
                                    event.getRawX();

                            tab.dragging = false;
                            pendingDragTab = tab;

                            scheduleDragStart(tab);

                            return true;

                        case MotionEvent.ACTION_MOVE:

                            if (pendingDragTab != tab) {
                                return true;
                            }

                            float distance =
                                    event.getRawX()
                                    - tab.dragStartX;

                            if (tab.dragging) {

                                tab.tabView
                                        .setTranslationY(
                                                -dp(3));

                                reorderWhileDragging(
                                        tab,
                                        event.getRawX());

                                return true;
                            }

                            /*
                             * Cancel a pending long press if
                             * the pointer is moved too far.
                             */
                            if (Math.abs(distance) >
                                    dp(12)) {

                                cancelDragStart(tab);
                            }

                            return true;

                        case MotionEvent.ACTION_UP:

                            cancelDragStart(tab);

                            if (tab.dragging) {

                                finishDrag(tab);

                            } else {

                                selectTab(tab);
                            }

                            return true;

                        case MotionEvent.ACTION_CANCEL:

                            cancelDragStart(tab);

                            if (tab.dragging) {
                                cancelDragVisuals(tab);
                            }

                            return true;
                    }

                    return true;
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
                v -> {

                    if (!tab.dragging) {
                        selectTab(tab);
                    }
                });
    }

    private void scheduleDragStart(
            final BrowserTab tab) {

        cancelDragStart(tab);

        dragHandler.postDelayed(
                () -> {

                    if (pendingDragTab == tab &&
                            tab.tabView != null) {

                        startDragging(tab);
                    }

                },
                DRAG_HOLD_MS);
    }

    private void cancelDragStart(
            BrowserTab tab) {

        dragHandler.removeCallbacksAndMessages(
                null);

        if (pendingDragTab == tab &&
                !tab.dragging) {

            pendingDragTab = null;
        }
    }

    private void startDragging(
            BrowserTab tab) {

        tab.dragging = true;
        pendingDragTab = tab;

        tabsLayout.setLayoutTransition(null);

        tab.tabView.setAlpha(0.8f);
        tab.tabView.setScaleX(1.03f);
        tab.tabView.setScaleY(1.03f);

        tab.tabView.bringToFront();
    }

    private void reorderWhileDragging(
            BrowserTab tab,
            float rawX) {

        int currentIndex =
                tabs.indexOf(tab);

        if (currentIndex < 0) {
            return;
        }

        int targetIndex =
                currentIndex;

        for (int i = 0;
                i < tabs.size();
                i++) {

            BrowserTab other =
                    tabs.get(i);

            if (other == tab) {
                continue;
            }

            int[] location =
                    new int[2];

            other.tabView
                    .getLocationOnScreen(
                            location);

            float center =
                    location[0] +
                    other.tabView.getWidth() /
                            2f;

            if (rawX < center) {
                targetIndex = i;
                break;
            }

            targetIndex = i + 1;
        }

        if (targetIndex > tabs.size() - 1) {
            targetIndex =
                    tabs.size() - 1;
        }

        if (targetIndex == currentIndex) {
            return;
        }

        tabs.remove(currentIndex);
        tabs.add(targetIndex, tab);

        tabsLayout.removeView(tab.tabView);

        if (targetIndex >=
                tabsLayout.getChildCount()) {

            tabsLayout.addView(
                    tab.tabView);

        } else {

            tabsLayout.addView(
                    tab.tabView,
                    targetIndex);
        }

        tab.tabView.bringToFront();
    }

    private void finishDrag(
            BrowserTab tab) {

        cancelDragVisuals(tab);
        selectTab(tab);
    }

    private void cancelDragVisuals(
            BrowserTab tab) {

        tab.dragging = false;

        if (pendingDragTab == tab) {
            pendingDragTab = null;
        }

        tab.tabView.setTranslationY(0f);
        tab.tabView.setScaleX(1f);
        tab.tabView.setScaleY(1f);
        tab.tabView.setAlpha(1f);

        tabsLayout.setLayoutTransition(
                tabTransition);
    }

    public void selectTab(
            BrowserTab tab) {

        if (tab == null ||
                !tabs.contains(tab)) {
            return;
        }

        activeTab = tab;

        for (BrowserTab current :
                tabs) {

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

    public void updateTabAppearance(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        int textColor;

        if (tab == activeTab) {

            int accent =
                    ColorUtils.parseColor(
                            activity
                                    .getBrowserSettings()
                                    .getAccentColor(),
                            Color.rgb(
                                    63, 81, 181));

            textColor =
                    ColorUtils
                            .getReadableTextColor(
                                    ColorUtils
                                            .mix(
                                                    Color.WHITE,
                                                    accent,
                                                    0.10f));

        } else {

            textColor =
                    ColorUtils
                            .getReadableTextColor(
                                    Color.WHITE);
        }

        tab.titleView.setTextColor(
                textColor);
    }

    public void updateTabAppearanceColors() {

        for (BrowserTab tab : tabs) {
            updateTabAppearance(tab);
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

        cancelDragStart(tab);

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

        activity.updateNavigationButtonsForTabs();
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

            applyWebSettings(
                    settings,
                    browserSettings);
        }

        CookieManager.getInstance()
                .setAcceptCookie(
                        browserSettings
                                .areCookiesEnabled());

        updateTabAppearanceColors();
    }

    private void applyWebSettings(
            WebSettings settings,
            BrowserSettings browserSettings) {

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

        /*
         * Explicitly allow local HTML pages on API 19.
         */
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        if (browserSettings
                .isDesktopMode()) {

            settings.setUserAgentString(
                    DESKTOP_USER_AGENT);

            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);

        } else {

            /*
             * null tells WebView to return to its
             * built-in user agent.
             */
            settings.setUserAgentString(null);
            settings.setUseWideViewPort(false);
            settings.setLoadWithOverviewMode(false);
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
                    title.substring(0, 22) +
                    "...";
        }

        tab.titleView.setText(title);
    }

    public void updateTitles() {

        for (BrowserTab tab : tabs) {
            updateTabTitle(tab);
        }
    }

    public void updateNavigationStates() {
        activity.updateNavigationButtonsForTabs();
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
