package com.example.simplebrowser;

import android.animation.LayoutTransition;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
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
    private final TabStripLayout tabStrip;
    private final HorizontalScrollView tabScroll;
    private final LayoutTransition tabTransition;
    private final PreviewStore previewStore;

    private final List<BrowserTab> normalTabs =
            new ArrayList<>();

    private final List<BrowserTab> incognitoTabs =
            new ArrayList<>();

    private final List<BrowserTab> guestTabs =
            new ArrayList<>();

    private List<BrowserTab> tabs;

    private BrowserTab activeTab;
    private BrowserTab normalActiveTab;
    private BrowserTab incognitoActiveTab;
    private BrowserTab guestActiveTab;
    private boolean incognitoMode = false;
    private boolean guestMode = false;

    public TabManager(
            MainActivity activity,
            FrameLayout webViewContainer,
            LinearLayout tabsLayout) {

        this.activity = activity;
        this.webViewContainer =
                webViewContainer;
        this.tabsLayout =
                tabsLayout;

        previewStore =
                new PreviewStore(activity);

        this.tabStrip =
                tabsLayout instanceof TabStripLayout
                        ? (TabStripLayout) tabsLayout
                        : null;

        this.tabs = normalTabs;

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

        return addTab(
                url,
                incognitoMode,
                true);
    }

    public BrowserTab addCurrentModeTab(
            String url) {

        return addTab(
                url,
                incognitoMode,
                true);
    }

    public BrowserTab addIncognitoTab(
            String url) {

        switchToSession(true);

        return addTab(
                url,
                true,
                true);
    }

    public BrowserTab addRestoredTab(
            String url,
            String title,
            String previewKey) {

        return addRestoredTab(
                url,
                title,
                previewKey,
                "");
    }

    public BrowserTab addRestoredTab(
            String url,
            String title,
            String previewKey,
            String faviconBase64) {

        BrowserTab tab =
                addTab(
                        url,
                        false,
                        false);

        if (previewKey != null &&
                !previewKey.trim().isEmpty()) {
            tab.previewKey =
                    previewKey.trim();
        }

        if (title != null &&
                !title.trim().isEmpty() &&
                !"about:blank".equalsIgnoreCase(
                        title.trim())) {

            tab.title =
                    title.trim();

        } else {

            tab.title = "New Tab";
        }

        if ("browser://default".equalsIgnoreCase(
                tab.pendingUrl)) {
            tab.title = "New Tab";
        }

        if (faviconBase64 != null &&
                !faviconBase64.trim().isEmpty()) {

            try {
                byte[] bytes =
                        Base64.decode(
                                faviconBase64,
                                Base64.DEFAULT);

                tab.favicon =
                        BitmapFactory.decodeByteArray(
                                bytes,
                                0,
                                bytes.length);

                if (tab.favicon != null) {
                    updateTabIcon(
                            tab,
                            tab.favicon);
                }
            } catch (Throwable ignored) {
            }
        }

        updateTabTitle(tab);

        return tab;
    }

    private BrowserTab addTab(
            String url,
            boolean incognito,
            boolean select) {

        BrowserTab tab =
                new BrowserTab();

        tab.isIncognito = incognito;
        tab.isGuest = guestMode;
        tab.pendingUrl =
                url == null ||
                url.trim().isEmpty()
                        ? "about:blank"
                        : url;

        tab.webView =
                new WebView(activity);

        configureWebView(tab);
        createTabView(tab);

        tabs.add(tab);

        tabsLayout.addView(
                tab.tabView);

        webViewContainer.addView(
                tab.webView);

        if (select) {
            selectTab(tab);
        }

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

        tab.webView.addJavascriptInterface(
                new PasswordCaptureBridge(
                        activity,
                        tab),
                "PasswordCapture");

        tab.webView.addJavascriptInterface(
                new StorageCaptureBridge(
                        activity),
                "StorageCapture");

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

        tab.webView.setOnLongClickListener(
                v -> activity.handleWebViewLongPress(
                        tab));

        tab.webView.setDownloadListener(
                (url,
                 userAgent,
                 contentDisposition,
                 mimeType,
                 contentLength) ->
                        activity.startDownload(
                                tab,
                                url,
                                userAgent,
                                contentDisposition,
                                mimeType,
                                contentLength));
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
                dp(116));

        tab.faviconView =
                new ImageView(activity);

        tab.faviconView.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE);

        tab.faviconView.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5));

        tab.faviconView.setVisibility(
                View.VISIBLE);

        tab.faviconView.setContentDescription(
                "Website icon");

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
                tab.faviconView,
                new LinearLayout.LayoutParams(
                        dp(28),
                        dp(36)));

        tab.tabView.addView(
                tab.titleView,
                new LinearLayout.LayoutParams(
                        dp(116),
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

                    if (tabStrip != null) {
                        tabStrip.setDraggedChild(
                                tab.tabView);
                    }

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
            tabWidth = dp(178);
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

        if (tabStrip != null) {
            tabStrip.clearDraggedChild();
        }

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

        if (incognitoMode) {
            incognitoActiveTab = tab;
        } else {
            normalActiveTab = tab;
        }

        if (!tab.hasLoaded) {

            tab.hasLoaded = true;

            String pending =
                    tab.pendingUrl;

            if (pending == null ||
                    pending.trim().isEmpty()) {
                pending = "about:blank";
            }

            activity.loadTabUrl(
                    tab,
                    pending);
        }

        for (BrowserTab current :
                tabs) {

            if (current == tab) {
                current.webView.setVisibility(
                        View.VISIBLE);
                try {
                    current.webView.onResume();
                } catch (Throwable ignored) {
                }
            } else {
                current.webView.setVisibility(
                        View.GONE);
                try {
                    current.webView.onPause();
                } catch (Throwable ignored) {
                }
            }

            updateTabAppearance(
                    current);
        }

        String url;

        if (tab.settingsPage) {

            url =
                    activity.getSettingsUrl(
                            tab.settingsSection);

        } else if (tab.defaultPage) {

            url = "";

        } else if (tab.historyPage) {

            url = "browser://history";

        } else if (tab.downloadsPage) {

            url = BrowserPage.DOWNLOADS;

        } else if (tab.cookiesPage) {

            url = BrowserPage.COOKIES;

        } else if (tab.bookmarksPage) {

            url = BrowserPage.BOOKMARKS;

        } else if (tab.passwordsPage) {

            url = BrowserPage.PASSWORDS;

        } else if (tab.profilesPage) {

            url = BrowserPage.PROFILES;

        } else if (tab.errorPage) {

            url = tab.url;

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
        activity.applyActiveTabAppearance();
        activity.updateTabOverviewIcon();
    }

    public void updateTabAppearance(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        int accent =
                tab.isIncognito
                        ? Color.rgb(
                                48,
                                49,
                                52)
                        : ColorUtils.parseColor(
                                activity
                                        .getBrowserSettings()
                                        .getAccentColor(),
                                Color.rgb(
                                        63, 81, 181));

        int inactiveBackground =
                ColorUtils.darken(
                        accent,
                        0.16f);

        int background =
                tab == activeTab
                        ? ColorUtils.darken(
                                accent,
                                0.08f)
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

        android.graphics.drawable.Drawable currentDrawable =
                tab.faviconView == null
                        ? null
                        : tab.faviconView.getDrawable();

        if (currentDrawable instanceof TabLoadingDrawable) {
            ((TabLoadingDrawable) currentDrawable).setColor(
                    textColor);
        }
    }

    public void updateTabAppearanceColors() {

        BrowserTab current =
                activeTab;

        int accent =
                current != null &&
                        current.isIncognito
                        ? Color.rgb(
                                48,
                                49,
                                52)
                        : ColorUtils.parseColor(
                                activity
                                        .getBrowserSettings()
                                        .getAccentColor(),
                                Color.WHITE);

        int tabBarBackground =
                current != null &&
                        current.isIncognito
                        ? Color.rgb(
                                30, 30, 32)
                        : ColorUtils.darken(
                                accent,
                                0.14f);

        tabsLayout.setBackgroundColor(
                tabBarBackground);

        if (tabScroll != null) {
            tabScroll.setBackgroundColor(
                    tabBarBackground);
        }

        for (BrowserTab tab :
                tabs) {

            updateTabAppearance(tab);
        }
    }

    public void closeTab(
            BrowserTab tab) {

        if (!tabs.contains(tab)) {
            return;
        }

        cancelDragTimer(tab);

        if (tabs.size() == 1) {

            tabs.remove(tab);

            tabsLayout.removeView(
                    tab.tabView);

            webViewContainer.removeView(
                    tab.webView);

            previewStore.remove(
                    tab.previewKey);

            stopTabLoadingIcon(tab);
            tab.webView.stopLoading();
            tab.webView.destroy();

            activeTab = null;

            if (incognitoMode || guestMode) {

                if (incognitoMode) {
                    incognitoActiveTab = null;
                } else {
                    guestActiveTab = null;
                }

                if (guestMode) {
                    exitGuestMode();
                } else {
                    switchToSession(false);
                }

                if (normalTabs.isEmpty()) {
                    activity.finish();
                }

            } else {

                normalActiveTab = null;
                activity.saveTabs();
                activity.finish();
            }

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

        previewStore.remove(
                tab.previewKey);

        stopTabLoadingIcon(tab);
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
        activity.updateTabOverviewIcon();
    }

    public void saveTabs() {

        JSONObject state =
                new JSONObject();

        JSONArray array =
                new JSONArray();

        int activePersistentIndex = -1;
        int persistentIndex = 0;

        for (BrowserTab tab : normalTabs) {

            String url;

            if (tab.settingsPage) {
                url = activity.getSettingsUrl(
                        tab.settingsSection);

            } else if (tab.defaultPage) {
                url = BrowserPage.DEFAULT;

            } else if (tab.historyPage) {
                url = BrowserPage.HISTORY;

            } else if (tab.downloadsPage) {
                url = BrowserPage.DOWNLOADS;

            } else if (tab.cookiesPage) {
                url = BrowserPage.COOKIES;

            } else if (tab.bookmarksPage) {
                url = BrowserPage.BOOKMARKS;

            } else if (tab.passwordsPage) {
                url = BrowserPage.PASSWORDS;

            } else if (tab.profilesPage) {
                url = BrowserPage.PROFILES;

            } else if (tab.errorPage) {
                url = tab.url;

            } else if (tab.hasLoaded) {

                url = tab.webView.getUrl();

                if (url == null ||
                        url.trim().isEmpty()) {
                    url = tab.url;
                }

            } else {

                url = tab.pendingUrl;
            }

            if (url == null ||
                    url.trim().isEmpty()) {
                url = "about:blank";
            }

            try {

                JSONObject object =
                        new JSONObject();

                object.put(
                        "url",
                        url);

                object.put(
                        "title",
                        tab.title);

                object.put(
                        "previewKey",
                        tab.previewKey);

                String faviconBase64 =
                        encodeFaviconForTabs(
                                tab.favicon);

                if (!faviconBase64.isEmpty()) {
                    object.put(
                            "favicon",
                            faviconBase64);
                }

                array.put(object);

                if (tab == normalActiveTab ||
                        (!incognitoMode &&
                         tab == activeTab)) {

                    activePersistentIndex =
                            persistentIndex;
                }

                persistentIndex++;

            } catch (Exception ignored) {
            }
        }

        try {

            state.put("tabs", array);
            state.put(
                    "active",
                    activePersistentIndex);

            activity.getBrowserSettings()
                    .setSavedTabsJson(
                            state.toString());

        } catch (Exception ignored) {
        }
    }

    public boolean restoreTabs() {

        if (incognitoMode) {
            switchToSession(false);
        }

        String json =
                activity.getBrowserSettings()
                        .getSavedTabsJson();

        if (json == null ||
                json.trim().isEmpty()) {
            return false;
        }

        try {

            JSONObject state =
                    new JSONObject(json);

            JSONArray array =
                    state.optJSONArray("tabs");

            if (array == null ||
                    array.length() == 0) {
                return false;
            }

            for (int i = 0;
                    i < array.length();
                    i++) {

                JSONObject object =
                        array.getJSONObject(i);

                addRestoredTab(
                        object.optString(
                                "url",
                                "about:blank"),
                        object.optString(
                                "title",
                                "New Tab"),
                        object.optString(
                                "previewKey",
                                ""),
                        object.optString(
                                "favicon",
                                ""));
            }

            int activeIndex =
                    state.optInt("active", 0);

            if (activeIndex < 0 ||
                    activeIndex >= tabs.size()) {
                activeIndex = 0;
            }

            if (!tabs.isEmpty()) {
                selectTab(
                        tabs.get(activeIndex));
            }

            return true;

        } catch (Exception e) {

            try {

                JSONArray array =
                        new JSONArray(json);

                if (array.length() == 0) {
                    return false;
                }

                for (int i = 0;
                        i < array.length();
                        i++) {

                    JSONObject object =
                            array.getJSONObject(i);

                    addRestoredTab(
                            object.optString(
                                    "url",
                                    "about:blank"),
                            object.optString(
                                    "title",
                                    "New Tab"),
                            object.optString(
                                    "previewKey",
                                    ""),
                            object.optString(
                                    "favicon",
                                    ""));
                }

                selectTab(
                        tabs.get(0));

                return true;

            } catch (Exception ignored) {

                activity.getBrowserSettings()
                        .setSavedTabsJson("");

                return false;
            }
        }
    }

    private String encodeFaviconForTabs(
            Bitmap favicon) {

        if (favicon == null ||
                favicon.isRecycled()) {
            return "";
        }

        try {
            int size =
                    48;

            int width =
                    favicon.getWidth();

            int height =
                    favicon.getHeight();

            float scale =
                    Math.min(
                            1f,
                            Math.min(
                                    (float) size /
                                            Math.max(
                                                    1,
                                                    width),
                                    (float) size /
                                            Math.max(
                                                    1,
                                                    height)));

            int targetWidth =
                    Math.max(
                            1,
                            Math.round(
                                    width * scale));

            int targetHeight =
                    Math.max(
                            1,
                            Math.round(
                                    height * scale));

            Bitmap scaled =
                    Bitmap.createScaledBitmap(
                            favicon,
                            targetWidth,
                            targetHeight,
                            true);

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            scaled.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    output);

            if (scaled != favicon) {
                scaled.recycle();
            }

            return Base64.encodeToString(
                    output.toByteArray(),
                    Base64.NO_WRAP);

        } catch (Throwable ignored) {
            return "";
        }
    }

    public void enterGuestMode(
            String homeUrl) {

        if (guestMode) {
            if (activeTab != null) {
                selectTab(activeTab);
            }
            return;
        }

        if (incognitoMode) {
            switchToSession(false);
        }

        if (activeTab != null) {
            normalActiveTab = activeTab;
        }

        incognitoMode = false;
        guestMode = true;
        tabs = guestTabs;

        if (tabStrip != null) {
            tabStrip.clearDraggedChild();
        }

        tabsLayout.setLayoutTransition(null);
        tabsLayout.removeAllViews();

        for (BrowserTab hidden : normalTabs) {
            hidden.webView.setVisibility(View.GONE);
            try { hidden.webView.onPause(); } catch (Throwable ignored) {}
        }

        for (BrowserTab hidden : incognitoTabs) {
            hidden.webView.setVisibility(View.GONE);
            try { hidden.webView.onPause(); } catch (Throwable ignored) {}
        }

        for (BrowserTab hidden : guestTabs) {
            hidden.webView.setVisibility(View.GONE);
            try { hidden.webView.onPause(); } catch (Throwable ignored) {}
        }

        for (BrowserTab current : guestTabs) {
            tabsLayout.addView(current.tabView);
        }

        tabsLayout.setLayoutTransition(tabTransition);

        activeTab = guestActiveTab;

        if (guestTabs.isEmpty()) {
            addTab(homeUrl, false, true);
        } else if (activeTab != null) {
            selectTab(activeTab);
        } else {
            selectTab(guestTabs.get(0));
        }
    }

    public void exitGuestMode() {

        if (!guestMode) {
            return;
        }

        if (activeTab != null) {
            guestActiveTab = activeTab;
        }

        guestMode = false;
        incognitoMode = false;
        tabs = normalTabs;

        if (tabStrip != null) {
            tabStrip.clearDraggedChild();
        }

        tabsLayout.setLayoutTransition(null);
        tabsLayout.removeAllViews();

        for (BrowserTab hidden : normalTabs) {
            hidden.webView.setVisibility(View.GONE);
            try { hidden.webView.onPause(); } catch (Throwable ignored) {}
        }

        for (BrowserTab hidden : incognitoTabs) {
            hidden.webView.setVisibility(View.GONE);
            try { hidden.webView.onPause(); } catch (Throwable ignored) {}
        }

        for (BrowserTab guest : guestTabs) {
            try { guest.webView.stopLoading(); } catch (Throwable ignored) {}
            try { guest.webView.onPause(); } catch (Throwable ignored) {}
            try { webViewContainer.removeView(guest.webView); } catch (Throwable ignored) {}
            try { guest.webView.destroy(); } catch (Throwable ignored) {}
        }

        guestTabs.clear();
        guestActiveTab = null;

        for (BrowserTab current : normalTabs) {
            tabsLayout.addView(current.tabView);
        }

        tabsLayout.setLayoutTransition(tabTransition);

        activeTab = normalActiveTab;

        if (activeTab != null && normalTabs.contains(activeTab)) {
            selectTab(activeTab);
        } else if (!normalTabs.isEmpty()) {
            selectTab(normalTabs.get(0));
        } else {
            activeTab = null;
            activity.setUrlText("");
            activity.updateTabOverviewIcon();
            activity.updateNavigationButtons();
            activity.applyActiveTabAppearance();
        }
    }

    public boolean isGuestMode() {
        return guestMode;
    }

    public void enterIncognitoMode(
            String homeUrl) {

        switchToSession(true);

        if (incognitoTabs.isEmpty()) {

            addTab(
                    homeUrl,
                    true,
                    true);

        } else if (activeTab != null) {

            selectTab(activeTab);
        }
    }

    public void exitIncognitoMode() {

        switchToSession(false);

        if (normalTabs.isEmpty()) {

            addTab(
                    activity.getBrowserSettings()
                            .getHomePage(),
                    false,
                    true);

        } else if (activeTab != null) {

            selectTab(activeTab);
        }
    }

    private void switchToSession(
            boolean incognito) {

        if (guestMode) {
            if (activeTab != null) {
                guestActiveTab = activeTab;
            }
            guestMode = false;
            incognitoMode = false;
            activeTab = null;
        }

        if (incognitoMode == incognito) {
            return;
        }

        if (activeTab != null) {

            if (incognitoMode) {
                incognitoActiveTab = activeTab;
            } else {
                normalActiveTab = activeTab;
            }
        }

        incognitoMode = incognito;

        tabs =
                incognito
                        ? incognitoTabs
                        : normalTabs;

        /*
         * Replace the visible tab strip with only this session's
         * tabs, and hide all WebViews belonging to the other session.
         */
        if (tabStrip != null) {
            tabStrip.clearDraggedChild();
        }

        tabsLayout.setLayoutTransition(null);
        tabsLayout.removeAllViews();

        for (BrowserTab hidden :
                normalTabs) {
            hidden.webView.setVisibility(View.GONE);
            try { hidden.webView.onPause(); } catch (Throwable ignored) {}
        }

        for (BrowserTab hidden :
                incognitoTabs) {
            hidden.webView.setVisibility(View.GONE);
            try { hidden.webView.onPause(); } catch (Throwable ignored) {}
        }

        for (BrowserTab hidden :
                guestTabs) {
            hidden.webView.setVisibility(View.GONE);
            try { hidden.webView.onPause(); } catch (Throwable ignored) {}
        }

        for (BrowserTab current :
                tabs) {
            tabsLayout.addView(
                    current.tabView);
        }

        tabsLayout.setLayoutTransition(
                tabTransition);

        activeTab =
                incognito
                        ? incognitoActiveTab
                        : normalActiveTab;

        if (activeTab != null &&
                tabs.contains(activeTab)) {

            selectTab(activeTab);

        } else if (!tabs.isEmpty()) {

            selectTab(tabs.get(0));

        } else {

            activeTab = null;
            activity.setUrlText("");
            activity.updateTabOverviewIcon();
            activity.updateNavigationButtonsForTabs();
            activity.applyActiveTabAppearance();
        }
    }

    public boolean isIncognitoMode() {
        return incognitoMode;
    }

    public BrowserTab getActiveTab() {
        return activeTab;
    }

    public List<BrowserTab> getTabs() {
        return tabs;
    }

    public List<BrowserTab> getNormalTabs() {
        return normalTabs;
    }

    /**
     * Moves a tab within the current session and keeps the visible
     * tab strip in the same order.
     */
    public void moveTab(
            BrowserTab tab,
            int targetIndex) {

        if (tab == null ||
                !tabs.contains(tab) ||
                tabs.size() < 2) {
            return;
        }

        int fromIndex =
                tabs.indexOf(tab);

        int safeTarget =
                Math.max(
                        0,
                        Math.min(
                                targetIndex,
                                tabs.size() - 1));

        if (fromIndex == safeTarget) {
            return;
        }

        tabs.remove(fromIndex);
        tabs.add(safeTarget, tab);

        tabsLayout.removeView(
                tab.tabView);

        tabsLayout.addView(
                tab.tabView,
                safeTarget);

        if (!incognitoMode && !guestMode) {
            saveTabs();
        }

        activity.updateTabOverviewIcon();
        updateTabAppearanceColors();
    }

    public void destroyAllTabsForProfileSwitch() {

        cancelDragStateForSwitch();

        java.util.ArrayList<BrowserTab> all =
                new java.util.ArrayList<>();

        all.addAll(normalTabs);
        all.addAll(incognitoTabs);
        all.addAll(guestTabs);

        for (BrowserTab tab : all) {
            if (tab == null || tab.webView == null) {
                continue;
            }

            try { tab.webView.stopLoading(); } catch (Throwable ignored) {}
            try { tab.webView.onPause(); } catch (Throwable ignored) {}
            try { webViewContainer.removeView(tab.webView); } catch (Throwable ignored) {}
            try { tab.webView.destroy(); } catch (Throwable ignored) {}
        }

        normalTabs.clear();
        incognitoTabs.clear();
        guestTabs.clear();

        normalActiveTab = null;
        incognitoActiveTab = null;
        guestActiveTab = null;
        activeTab = null;

        incognitoMode = false;
        guestMode = false;
        tabs = normalTabs;

        tabsLayout.setLayoutTransition(null);
        tabsLayout.removeAllViews();
        tabsLayout.setLayoutTransition(tabTransition);
    }

    private void cancelDragStateForSwitch() {

        if (tabStrip != null) {
            tabStrip.clearDraggedChild();
        }

        java.util.ArrayList<BrowserTab> all =
                new java.util.ArrayList<>();

        all.addAll(normalTabs);
        all.addAll(incognitoTabs);
        all.addAll(guestTabs);

        for (BrowserTab tab : all) {
            if (tab.dragRunnable != null &&
                    tab.titleView != null) {
                tab.titleView.removeCallbacks(
                        tab.dragRunnable);
                tab.dragRunnable = null;
            }
        }
    }

    public void applyWebSettings() {

        BrowserSettings browserSettings =
                activity.getBrowserSettings();

        for (BrowserTab tab :
                normalTabs) {

            applyWebSettings(
                    tab,
                    tab.webView.getSettings(),
                    browserSettings);
        }

        for (BrowserTab tab :
                incognitoTabs) {

            applyWebSettings(
                    tab,
                    tab.webView.getSettings(),
                    browserSettings);
        }

        for (BrowserTab tab :
                guestTabs) {

            applyWebSettings(
                    tab,
                    tab.webView.getSettings(),
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

            webSettings.setJavaScriptCanOpenWindowsAutomatically(
                    browserSettings
                            .isJavaScriptCanOpenWindowsAutomatically());

            webSettings.setLoadWithOverviewMode(
                    browserSettings
                            .isLoadWithOverviewMode());

            webSettings.setTextZoom(
                    browserSettings
                            .getTextZoom());

            webSettings.setMinimumFontSize(
                    browserSettings
                            .getMinimumFontSize());

            try {
                webSettings.setDatabaseEnabled(
                        (tab.isIncognito || tab.isGuest)
                                ? false
                                : browserSettings
                                        .isWebSqlEnabled());
            } catch (Throwable ignored) {
            }

            if (android.os.Build.VERSION.SDK_INT >= 26) {
                try {
                    webSettings.setSafeBrowsingEnabled(
                            browserSettings
                                    .isSafeBrowsingEnabled());
                } catch (Throwable ignored) {
                }
            }
        }

        webSettings.setDomStorageEnabled(
                (tab.isIncognito || tab.isGuest)
                        ? false
                        : browserSettings
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

        int cacheMode =
                WebSettings.LOAD_DEFAULT;

        if ("no_cache".equals(
                browserSettings.getCacheMode())) {
            cacheMode =
                    WebSettings.LOAD_NO_CACHE;
        } else if ("cache_only".equals(
                browserSettings.getCacheMode())) {
            cacheMode =
                    WebSettings.LOAD_CACHE_ONLY;
        }

        if (tab.isIncognito || tab.isGuest) {
            cacheMode =
                    WebSettings.LOAD_NO_CACHE;
        }

        webSettings.setCacheMode(cacheMode);

        webSettings.setSaveFormData(
                (tab.isIncognito || tab.isGuest)
                        ? false
                        : browserSettings
                                .isSaveFormDataEnabled());

        /*
         * Simple Browser has its own encrypted password manager. Keep
         * WebView's legacy credential database disabled so credentials do
         * not end up in a second, process-global password store.
         */
        webSettings.setSavePassword(false);

        webSettings.setSupportMultipleWindows(
                browserSettings
                        .arePopupsEnabled());

        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            webSettings.setAllowFileAccessFromFileURLs(
                    browserSettings
                            .isFileAccessFromFileUrlsEnabled());
            webSettings.setAllowUniversalAccessFromFileURLs(
                    browserSettings
                            .isUniversalAccessFromFileUrlsEnabled());
        }

        tab.webView.setLayerType(
                browserSettings
                        .isHardwareAccelerationEnabled()
                        ? WebView.LAYER_TYPE_HARDWARE
                        : WebView.LAYER_TYPE_SOFTWARE,
                null);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode(
                    browserSettings.isMixedContentEnabled()
                            ? WebSettings
                                    .MIXED_CONTENT_ALWAYS_ALLOW
                            : WebSettings
                                    .MIXED_CONTENT_NEVER_ALLOW);

            CookieManager.getInstance()
                    .setAcceptThirdPartyCookies(
                            tab.webView,
                            browserSettings
                                    .isThirdPartyCookiesEnabled());
        }

        if (browserSettings.isDesktopMode()) {

            webSettings.setUserAgentString(
                    DESKTOP_USER_AGENT);

            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);

        } else {

            webSettings.setUserAgentString(
                    browserSettings.getUserAgentProfile()
                            .equals(UserAgentProfiles.DEFAULT)
                            ? tab.defaultUserAgent
                            : UserAgentProfiles.getValue(
                                    browserSettings.getUserAgentProfile(),
                                    browserSettings.getCustomUserAgent(),
                                    tab.defaultUserAgent));

            webSettings.setUseWideViewPort(false);
            webSettings.setLoadWithOverviewMode(false);
        }
    }

    public void savePreview(
            BrowserTab tab) {

        if (tab == null ||
                tab.isIncognito ||
                tab.isGuest ||
                !tab.hasLoaded ||
                tab.webView == null) {
            return;
        }

        android.graphics.Bitmap bitmap =
                TabPreview.capture(
                        tab.webView,
                        480,
                        270);

        if (bitmap == null) {
            return;
        }

        previewStore.save(
                tab.previewKey,
                bitmap);

        bitmap.recycle();
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

    public void updateTabLoadingState(
            BrowserTab tab) {

        if (tab == null ||
                tab.faviconView == null) {
            return;
        }

        int color =
                tab.titleView == null
                        ? Color.WHITE
                        : tab.titleView
                                .getTextColors()
                                .getDefaultColor();

        android.graphics.drawable.Drawable currentDrawable =
                tab.faviconView.getDrawable();

        if (tab.loading) {

            TabLoadingDrawable spinner;

            if (currentDrawable instanceof TabLoadingDrawable) {
                spinner =
                        (TabLoadingDrawable) currentDrawable;
            } else {
                spinner =
                        new TabLoadingDrawable(color);

                tab.faviconView.setImageDrawable(
                        spinner);
            }

            spinner.setColor(color);
            tab.faviconView.setVisibility(
                    View.VISIBLE);
            spinner.start();

            return;
        }

        stopTabLoadingIcon(tab);

        Bitmap favicon =
                tab.favicon;

        if (favicon != null &&
                !favicon.isRecycled()) {

            tab.faviconView.setImageBitmap(
                    favicon);

        } else {

            tab.faviconView.setImageDrawable(
                    null);
        }

        // Keep this view visible so the tab title never moves.
        tab.faviconView.setVisibility(
                View.VISIBLE);
    }

    private void stopTabLoadingIcon(
            BrowserTab tab) {

        if (tab == null ||
                tab.faviconView == null) {
            return;
        }

        android.graphics.drawable.Drawable drawable =
                tab.faviconView.getDrawable();

        if (drawable instanceof TabLoadingDrawable) {
            ((TabLoadingDrawable) drawable).stop();
        }
    }

    public void updateTabIcon(
            BrowserTab tab,
            Bitmap favicon) {

        if (tab == null) {
            return;
        }

        tab.favicon = favicon;
        updateTabLoadingState(tab);
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
