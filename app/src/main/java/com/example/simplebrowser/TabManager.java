package com.example.simplebrowser;

import android.animation.LayoutTransition;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
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

    private float dragStartX;
    private boolean dragging;

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
                8, 0, 2, 0);

        tab.titleView =
                new TextView(activity);

        tab.titleView.setText(
                tab.title);

        tab.titleView.setSingleLine(true);

        tab.titleView.setGravity(
                Gravity.CENTER_VERTICAL);

        tab.titleView.setMaxWidth(
                dp(180));

        tab.closeButton =
                new ImageButton(activity);

        tab.closeButton.setImageResource(
                android.R.drawable
                        .ic_menu_close_clear_cancel);

        tab.closeButton.setContentDescription(
                "Close tab");

        tab.closeButton.setBackgroundColor(
                Color.TRANSPARENT);

        tab.closeButton.setOnClickListener(
                v -> closeTab(tab));

        /*
         * The title area is the draggable area.
         * The X button remains independently clickable.
         */
        tab.titleView.setOnTouchListener(
                (v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    dragStartX =
                            event.getRawX();

                    dragging = false;

                    return false;

                case MotionEvent.ACTION_MOVE:

                    float distance =
                            event.getRawX()
                            - dragStartX;

                    if (Math.abs(distance) >
                            dp(20)) {

                        dragging = true;

                        reorderTab(tab);
                    }

                    return true;

                case MotionEvent.ACTION_UP:

                    if (!dragging) {
                        selectTab(tab);
                    }

                    dragging = false;

                    return true;
            }

            return false;
        });

        tab.tabView.addView(
                tab.titleView,
                new LinearLayout.LayoutParams(
                        dp(150),
                        dp(48)));

        tab.tabView.addView(
                tab.closeButton,
                new LinearLayout.LayoutParams(
                        dp(42),
                        dp(48)));

        tab.tabView.setOnClickListener(
                v -> {

                    if (!dragging) {
                        selectTab(tab);
                    }
                });
    }

    private void reorderTab(
            BrowserTab tab) {

        int oldIndex =
                tabs.indexOf(tab);

        if (oldIndex < 0) {
            return;
        }

        int newIndex = oldIndex;

        int[] tabLocation =
                new int[2];

        tab.titleView.getLocationOnScreen(
                tabLocation);

        float center =
                tabLocation[0] +
                tab.titleView.getWidth() / 2f;

        for (int i = 0;
             i < tabs.size();
             i++) {

            if (i == oldIndex) {
                continue;
            }

            BrowserTab other =
                    tabs.get(i);

            int[] location =
                    new int[2];

            other.tabView
                    .getLocationOnScreen(
                            location);

            float otherCenter =
                    location[0] +
                    other.tabView.getWidth()
                    / 2f;

            if (center < otherCenter) {

                newIndex = i;
                break;
            }

            newIndex = i;
        }

        if (newIndex == oldIndex) {
            return;
        }

        tabs.remove(oldIndex);

        tabs.add(
                newIndex,
                tab);

        tabsLayout.removeView(
                tab.tabView);

        tabsLayout.addView(
                tab.tabView,
                newIndex);

        /*
         * LayoutTransition animates the other
         * tabs moving into their new positions.
         */
        tab.tabView.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(100)
                .withEndAction(
                        () -> tab.tabView
                                .animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start())
                .start();
    }

    public void selectTab(
            BrowserTab tab) {

        activeTab = tab;

        activity.hideKeyboard();

        for (BrowserTab current : tabs) {

            if (current == tab) {

                current.webView.setVisibility(
                        View.VISIBLE);

            } else {

                current.webView.setVisibility(
                        View.GONE);
            }
        }

        String url =
                tab.webView.getUrl();

        if (url == null) {
            url = tab.url;
        }

        if (tab.settingsPage) {
            url = "browser://settings";
        }

        if (url == null) {
            url = "";
        }

        activity.setUrlText(url);

        activity.setLoading(
                tab.loading);

        activity.updateSecurity(tab);
    }

    public void closeTab(
            BrowserTab tab) {

        if (tabs.size() == 1) {

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

            int newIndex =
                    Math.min(
                            index,
                            tabs.size() - 1);

            selectTab(
                    tabs.get(newIndex));
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

            settings.setJavaScriptEnabled(
                    browserSettings
                            .isJavaScriptEnabled());

            settings.setDomStorageEnabled(
                    browserSettings
                            .isStorageEnabled());

            settings.setSupportMultipleWindows(
                    browserSettings
                            .arePopupsEnabled());
        }

        android.webkit.CookieManager
                .getInstance()
                .setAcceptCookie(
                        browserSettings
                                .areCookiesEnabled());
    }

    public void updateTabTitle(
            BrowserTab tab) {

        String title =
                tab.title;

        if (title.length() > 20) {

            title =
                    title.substring(0, 20)
                    + "...";
        }

        tab.titleView.setText(title);
    }

    private int dp(int value) {

        return (int) (
                value *
                activity.getResources()
                        .getDisplayMetrics()
                        .density
                + 0.5f
        );
    }
          }
