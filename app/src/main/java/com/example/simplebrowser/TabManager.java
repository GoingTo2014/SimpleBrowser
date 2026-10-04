package com.example.simplebrowser;

import android.animation.LayoutTransition;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TabManager {

    private static final long DRAG_HOLD_MS =
            450L;

    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36";

    private final MainActivity activity;
    private final FrameLayout webViewContainer;
    private final LinearLayout tabsLayout;
    private final HorizontalScrollView tabScroll;
    private final LayoutTransition tabTransition;

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

        if (tabsLayout.getParent()
                instanceof HorizontalScrollView) {

            tabScroll =
                    (HorizontalScrollView)
                            tabsLayout.getParent();

        } else {

            tabScroll = null;
        }

        tabTransition =
                new LayoutTransition();

        tabTransition.setDuration(160);

        tabsLayout.setLayoutTransition(
                tabTransition);
    }

    private void rebuildTabOrderWithAnimation(
            BrowserTab draggedTab,
            int oldDraggedLeft,
            int oldDraggedTranslation) {

        final java.util.HashMap<BrowserTab, Integer> oldLefts =
                new java.util.HashMap<>();

        for (BrowserTab current : tabs) {

            oldLefts.put(
                    current,
                    current.tabView.getLeft());
        }

        final int draggedVisualLeft =
                oldDraggedLeft +
                        oldDraggedTranslation;

        tabsLayout.setLayoutTransition(
                null);

        tabsLayout.removeAllViews();

        for (BrowserTab current : tabs) {

            tabsLayout.addView(
                    current.tabView);
        }

        tabsLayout.post(
                () -> {

                    for (BrowserTab current : tabs) {

                        Integer previous =
                                oldLefts.get(current);

                        if (previous == null) {
                            continue;
                        }

                        int targetLeft =
                                current.tabView.getLeft();

                        int startLeft;

                        if (current == draggedTab) {

                            startLeft =
                                    draggedVisualLeft;

                        } else {

                            startLeft =
                                    previous;
                        }

                        int delta =
                                startLeft -
                                        targetLeft;

                        current.tabView.setTranslationX(
                                delta);

                        current.tabView.animate()
                                .translationX(0f)
                                .setDuration(180)
                                .start();
                    }

                    draggedTab.tabView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(180)
                            .start();

                    tabsLayout.setLayoutTransition(
                            tabTransition);

                    updateTabAppearanceColors();
                });
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

        WebSettings webSettings =
                tab.webView.getSettings();

        tab.defaultUserAgent =
                webSettings.getUserAgentString();

        applyWebSettings(
                tab,
                webSettings,
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
                dp(2),
                0,
                0,
                0);

        tab.tabView.setClickable(true);

        tab.titleView =
                new TextView(activity);

        tab.titleView.setText(
                tab.title);

        tab.titleView.setSingleLine(true);

        tab.titleView.setGravity(
                Gravity.CENTER_VERTICAL);

        tab.titleView.setPadding(
                dp(7),
                0,
                dp(7),
                0);

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
                dp(6),
                dp(6),
                dp(6),
                dp(6));

        tab.closeButton.setOnClickListener(
                v -> closeTab(tab));

        /*
         * The title owns the touch stream. The parent
         * HorizontalScrollView is allowed to behave normally
         * until the long-press timer fires. Once dragging
         * starts, parent interception is explicitly blocked.
         */
        tab.titleView.setOnTouchListener(
                (view, event) -> {

                    switch (event.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            tab.dragStartX =
                                    event.getRawX();

                            tab.dragStartY =
                                    event.getRawY();

                            tab.dragStartScrollX =
                                    tabScroll == null
                                            ? 0f
                                            : tabScroll.getScrollX();

                            tab.dragOriginalIndex =
                                    tabs.indexOf(tab);

                            tab.dragging = false;

                            startDragTimer(tab);

                            return true;

                        case MotionEvent.ACTION_MOVE:

                            if (!tab.dragging) {

                                float dx =
                                        event.getRawX() -
                                        tab.dragStartX;

                                float dy =
                                        event.getRawY() -
                                        tab.dragStartY;

                                if ((dx * dx + dy * dy) >
                                        dp(12) * dp(12)) {

                                    cancelDragTimer(tab);
                                }

                                return true;
                            }

                            /*
                             * A long press has claimed the
                             * gesture. Keep the parent from
                             * stealing it and reorder continuously.
                             */
                            view.getParent()
                                    .requestDisallowInterceptTouchEvent(
                                            true);

                            updateDragTranslation(
                                    tab,
                                    event.getRawX());

                            autoScrollTabs(
                                    event.getRawX());

                            return true;

                        case MotionEvent.ACTION_UP:

                            cancelDragTimer(tab);

                            if (tab.dragging) {

                                finishDrag(tab);

                            } else {

                                selectTab(tab);
                            }

                            view.getParent()
                                    .requestDisallowInterceptTouchEvent(
                                            false);

                            return true;

                        case MotionEvent.ACTION_CANCEL:

                            cancelDragTimer(tab);

                            if (tab.dragging) {
                                finishDrag(tab);
                            }

                            view.getParent()
                                    .requestDisallowInterceptTouchEvent(
                                            false);

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

    private void startDragTimer(
            final BrowserTab tab) {

        cancelDragTimer(tab);

        tab.dragRunnable =
                () -> {

                    /*
                     * The pointer stayed down long enough.
                     * From this point onward the tab owns the
                     * gesture, including the HorizontalScrollView.
                     */
                    if (tab.dragging) {
                        return;
                    }

                    tab.dragging = true;

                    tab.tabView
                            .getParent()
                            .requestDisallowInterceptTouchEvent(
                                    true);

                    tabsLayout.setLayoutTransition(
                            null);

                    tab.tabView.setAlpha(
                            0.82f);

                    tab.tabView.setScaleX(
                            1.03f);

                    tab.tabView.setScaleY(
                            1.03f);

                    tab.tabView.setTranslationX(
                            0f);

                };

        tab.titleView.postDelayed(
                tab.dragRunnable,
                DRAG_HOLD_MS);
    }

    private void cancelDragTimer(
            BrowserTab tab) {

        if (tab.dragRunnable != null) {

            tab.titleView.removeCallbacks(
                    tab.dragRunnable);

            tab.dragRunnable = null;
        }
    }

    private void updateDragTranslation(
            BrowserTab tab,
            float rawX) {

        if (!tab.dragging) {
            return;
        }

        float scrollDelta =
                tabScroll == null
                        ? 0f
                        : tabScroll.getScrollX() -
                                tab.dragStartScrollX;

        float translation =
                (rawX - tab.dragStartX) +
                        scrollDelta;

        int tabWidth =
                tab.tabView.getWidth();

        if (tabWidth <= 0) {
            tabWidth = dp(181);
        }

        int originalIndex =
                tab.dragOriginalIndex;

        if (originalIndex < 0) {
            originalIndex =
                    tabs.indexOf(tab);
        }

        int tabCount =
                tabs.size();

        float originalLeft =
                originalIndex * tabWidth;

        float minimum =
                -originalLeft;

        float maximum =
                (tabCount - 1) * tabWidth -
                        originalLeft;

        if (translation < minimum) {
            translation = minimum;
        }

        if (translation > maximum) {
            translation = maximum;
        }

        tab.tabView.setTranslationX(
                translation);
    }

    private int getDragTargetIndex(
            BrowserTab tab) {

        int tabWidth =
                tab.tabView.getWidth();

        if (tabWidth <= 0) {
            tabWidth = dp(181);
        }

        int originalIndex =
                tab.dragOriginalIndex;

        if (originalIndex < 0) {
            originalIndex =
                    tabs.indexOf(tab);
        }

        float visualLeft =
                (originalIndex * tabWidth) +
                        tab.tabView.getTranslationX();

        int target =
                Math.round(
                        visualLeft / tabWidth);

        if (target < 0) {
            target = 0;
        }

        if (target >= tabs.size()) {
            target = tabs.size() - 1;
        }

        return target;
    }

    private void commitDrag(
            BrowserTab tab) {

        if (!tab.dragging ||
                !tabs.contains(tab)) {
            return;
        }

        int currentIndex =
                tabs.indexOf(tab);

        int targetIndex =
                getDragTargetIndex(tab);

        int oldLeft =
                tab.tabView.getLeft();

        int oldTranslation =
                Math.round(
                        tab.tabView
                                .getTranslationX());

        if (currentIndex != targetIndex) {

            tabs.remove(currentIndex);

            /*
             * Removing first means targetIndex is the exact
             * final slot in the remaining list. Reinsert
             * without any off-by-one correction.
             */
            tabs.add(
                    targetIndex,
                    tab);
        }

        tab.dragging = false;
        cancelDragTimer(tab);

        if (currentIndex != targetIndex) {

            rebuildTabOrderWithAnimation(
                    tab,
                    oldLeft,
                    oldTranslation);

        } else {

            tab.tabView.animate()
                    .translationX(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .start();

            tabsLayout.setLayoutTransition(
                    tabTransition);
        }

        tab.tabView
                .setAlpha(1f);

        selectTab(tab);
    }

    private void autoScrollTabs(
            float rawX) {

        if (tabScroll == null) {
            return;
        }

        int[] location =
                new int[2];

        tabScroll.getLocationOnScreen(
                location);

        int left =
                location[0];

        int right =
                left +
                tabScroll.getWidth();

        int edge =
                dp(45);

        if (rawX <
                left + edge) {

            tabScroll.smoothScrollBy(
                    -dp(18),
                    0);

        } else if (
                rawX >
                        right - edge) {

            tabScroll.smoothScrollBy(
                    dp(18),
                    0);
        }
    }

    private void finishDrag(
            BrowserTab tab) {

        commitDrag(tab);

        tab.tabView
                .setAlpha(1f);

        tab.tabView
                .setScaleX(1f);

        tab.tabView
                .setScaleY(1f);
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

            url =
                    activity.getSettingsUrl(
                            tab.settingsSection);

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

        int accent =
                ColorUtils.parseColor(
                        activity
                                .getBrowserSettings()
                                .getAccentColor(),
                        Color.rgb(
                                63, 81, 181));

        int inactiveBackground =
                ColorUtils.mix(
                        accent,
                        Color.WHITE,
                        0.55f);

        int background =
                tab == activeTab
                        ? accent
                        : inactiveBackground;

        int textColor =
                ColorUtils
                        .getReadableTextColor(
                                background);

        tab.tabView.setBackgroundColor(
                background);

        tab.titleView.setTextColor(
                textColor);

        tab.closeButton.setColorFilter(
                textColor);
    }

    public void updateTabAppearanceColors() {

        int accent =
                ColorUtils.parseColor(
                        activity
                                .getBrowserSettings()
                                .getAccentColor(),
                        Color.rgb(
                                63, 81, 181));

        tabsLayout.setBackgroundColor(
                ColorUtils.darken(
                        accent,
                        0.14f));

        for (BrowserTab tab :
                tabs) {

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

        cancelDragTimer(tab);

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

        for (BrowserTab tab :
                tabs) {

            WebSettings webSettings =
                    tab.webView.getSettings();

            applyWebSettings(
                    tab,
                    webSettings,
                    browserSettings);
        }

        CookieManager.getInstance()
                .setAcceptCookie(
                        browserSettings
                                .areCookiesEnabled());

        updateTabAppearanceColors();
    }

    private void applyWebSettings(
            BrowserTab tab,
            WebSettings webSettings,
            BrowserSettings browserSettings) {

        /*
         * The internal settings page always needs JS for
         * its own controls, regardless of website JS preference.
         */
        if (!tab.settingsPage) {

            webSettings.setJavaScriptEnabled(
                    browserSettings
                            .isJavaScriptEnabled());
        }

        webSettings.setDomStorageEnabled(
                browserSettings
                        .isStorageEnabled());

        boolean imagesEnabled =
                browserSettings.areImagesEnabled();

        webSettings.setLoadsImagesAutomatically(
                imagesEnabled);

        webSettings.setBlockNetworkImage(
                !imagesEnabled);

        boolean zoomEnabled =
                browserSettings.isZoomEnabled();

        webSettings.setSupportZoom(
                zoomEnabled);

        webSettings.setBuiltInZoomControls(
                zoomEnabled);

        webSettings.setDisplayZoomControls(
                false);

        webSettings.setGeolocationEnabled(
                browserSettings
                        .isGeolocationEnabled());

        webSettings.setMediaPlaybackRequiresUserGesture(
                !browserSettings
                        .isMediaAutoplayEnabled());

        webSettings.setCacheMode(
                WebSettings.LOAD_DEFAULT);

        webSettings.setSupportMultipleWindows(
                browserSettings
                        .arePopupsEnabled());

        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        if (browserSettings.isDesktopMode()) {

            webSettings.setUserAgentString(
                    DESKTOP_USER_AGENT);

            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);

        } else {

            webSettings.setUserAgentString(
                    tab.defaultUserAgent);

            webSettings.setUseWideViewPort(false);
            webSettings.setLoadWithOverviewMode(false);
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

        for (BrowserTab tab :
                tabs) {

            updateTabTitle(tab);
        }
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
