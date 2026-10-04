package com.example.simplebrowser;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.net.URLEncoder;

public class MainActivity extends Activity {

    private static final String SETTINGS_URL =
            "browser://settings";

    private EditText urlBox;
    private ProgressBar progressBar;

    private BrowserSettings browserSettings;
    private TabManager tabManager;
    private SettingsPage settingsPage;
    private SecurityManager securityManager;

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        urlBox =
                findViewById(R.id.url);

        progressBar =
                findViewById(R.id.progress);

        FrameLayout webViewContainer =
                findViewById(
                        R.id.webview_container);

        LinearLayout tabsLayout =
                findViewById(R.id.tabs);

        ImageButton security =
                findViewById(R.id.security);

        browserSettings =
                new BrowserSettings(this);

        tabManager =
                new TabManager(
                        this,
                        webViewContainer,
                        tabsLayout);

        settingsPage =
                new SettingsPage(
                        this,
                        browserSettings);

        securityManager =
                new SecurityManager(
                        this,
                        security);

        setupButtons();

        applyBrowserAppearance();

        tabManager.addTab(
                browserSettings.getHomePage());
    }

    private void setupButtons() {

        Button back =
                findViewById(R.id.back);

        Button forward =
                findViewById(R.id.forward);

        Button home =
                findViewById(R.id.home);

        Button reload =
                findViewById(R.id.reload);

        Button newTab =
                findViewById(R.id.new_tab);

        back.setOnClickListener(v -> {

            hideKeyboard();

            BrowserTab tab =
                    getActiveTab();

            if (tab != null &&
                    tab.webView.canGoBack()) {

                tab.webView.goBack();
            }
        });

        forward.setOnClickListener(v -> {

            hideKeyboard();

            BrowserTab tab =
                    getActiveTab();

            if (tab != null &&
                    tab.webView.canGoForward()) {

                tab.webView.goForward();
            }
        });

        home.setOnClickListener(v -> {

            hideKeyboard();

            BrowserTab tab =
                    getActiveTab();

            if (tab != null) {

                loadTabUrl(
                        tab,
                        browserSettings
                                .getHomePage());
            }
        });

        reload.setOnClickListener(v -> {

            hideKeyboard();

            BrowserTab tab =
                    getActiveTab();

            if (tab != null) {
                tab.webView.reload();
            }
        });

        newTab.setOnClickListener(v -> {

            hideKeyboard();

            tabManager.addTab(
                    browserSettings
                            .getHomePage());
        });

        urlBox.setOnEditorActionListener(
                (v, actionId, event) -> {

            boolean enterPressed =
                    event != null &&
                    event.getKeyCode() ==
                            android.view.KeyEvent
                                    .KEYCODE_ENTER &&
                    event.getAction() ==
                            android.view.KeyEvent
                                    .ACTION_DOWN;

            if (actionId ==
                    EditorInfo.IME_ACTION_GO ||
                actionId ==
                    EditorInfo.IME_ACTION_DONE ||
                enterPressed) {

                String input =
                        urlBox.getText()
                                .toString()
                                .trim();

                if (!input.isEmpty()) {
                    openUrlOrSearch(input);
                }

                return true;
            }

            return false;
        });
    }

    public void openUrlOrSearch(
            String input) {

        hideKeyboard();

        BrowserTab tab =
                getActiveTab();

        if (tab == null) {
            return;
        }

        if (SETTINGS_URL.equalsIgnoreCase(
                input)) {

            showSettings(tab);

            return;
        }

        if (isUrl(input)) {

            if (!input.startsWith("http://") &&
                    !input.startsWith("https://")) {

                input =
                        "https://" + input;
            }

            loadTabUrl(tab, input);

        } else {

            try {

                String encoded =
                        URLEncoder.encode(
                                input,
                                "UTF-8");

                String searchUrl =
                        getSearchUrl();

                loadTabUrl(
                        tab,
                        searchUrl + encoded);

            } catch (Exception e) {

                loadTabUrl(
                        tab,
                        getSearchUrl() + input);
            }
        }
    }

    private String getSearchUrl() {

        String engine =
                browserSettings
                        .getSearchEngine();

        if ("bing".equals(engine)) {

            return
                    "https://www.bing.com/search?q=";

        }

        if ("duckduckgo".equals(engine)) {

            return
                    "https://duckduckgo.com/?q=";
        }

        if ("yahoo".equals(engine)) {

            return
                    "https://search.yahoo.com/search?p=";
        }

        return
                "https://www.google.com/search?q=";
    }

    private boolean isUrl(
            String input) {

        if (input.startsWith("http://") ||
                input.startsWith("https://")) {

            return true;
        }

        if (input.contains(" ")) {
            return false;
        }

        return input.contains(".") ||
                input.startsWith("localhost:") ||
                input.startsWith("127.0.0.1:");
    }

    public void loadTabUrl(
            BrowserTab tab,
            String url) {

        if (SETTINGS_URL.equalsIgnoreCase(
                url)) {

            showSettings(tab);

            return;
        }

        if (tab.settingsPage) {

            removeSettingsBridge(tab);
        }

        tab.settingsPage = false;

        tab.webView.loadUrl(url);
    }

    public void showSettings(
            BrowserTab tab) {

        settingsPage.show(tab);

        tabManager.selectTab(tab);

        updateSecurity(tab);
    }

    public void removeSettingsBridge(
            BrowserTab tab) {

        settingsPage.remove(tab);
    }

    public void pageStarted(
            BrowserTab tab,
            String url) {

        if (tab == getActiveTab()) {

            setUrlText(url);

            progressBar.setProgress(0);

            progressBar.setVisibility(
                    View.VISIBLE);
        }

        updateSecurity(tab);
    }

    public void pageFinished(
            BrowserTab tab,
            String url) {

        if (tab == getActiveTab()) {

            setUrlText(url);

            progressBar.setProgress(100);

            progressBar.postDelayed(
                    () -> {

                if (tab == getActiveTab() &&
                        !tab.loading) {

                    progressBar.setVisibility(
                            View.GONE);
                }

            }, 150);
        }

        updateSecurity(tab);
    }

    public void pageProgress(
            BrowserTab tab,
            int progress) {

        if (tab != getActiveTab()) {
            return;
        }

        progressBar.setProgress(progress);

        if (progress < 100) {

            progressBar.setVisibility(
                    View.VISIBLE);

        } else if (!tab.loading) {

            progressBar.setVisibility(
                    View.GONE);
        }
    }

    public void settingsLoaded(
            BrowserTab tab) {

        if (tab == getActiveTab()) {

            setUrlText(
                    SETTINGS_URL);

            progressBar.setVisibility(
                    View.GONE);
        }
    }

    public void updateTabTitle(
            BrowserTab tab) {

        tabManager.updateTabTitle(tab);
    }

    public void updateSecurity(
            BrowserTab tab) {

        securityManager.updateIcon(tab);
    }

    public BrowserTab getActiveTab() {

        return tabManager.getActiveTab();
    }

    public BrowserSettings
            getBrowserSettings() {

        return browserSettings;
    }

    public void applyWebsiteSettings() {

        tabManager.applyWebSettings();
    }

    public void applyBrowserAppearance() {

        View root =
                findViewById(R.id.root);

        if (root == null) {
            return;
        }

        if (browserSettings.isDarkMode()) {

            root.setBackgroundColor(
                    Color.BLACK);

        } else {

            root.setBackgroundColor(
                    Color.WHITE);
        }
    }

    public void setUrlText(
            String text) {

        urlBox.setText(
                text == null
                        ? ""
                        : text);
    }

    public void setLoading(
            boolean loading) {

        progressBar.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE);
    }

    public void hideKeyboard() {

        InputMethodManager manager =
                (InputMethodManager)
                        getSystemService(
                                Context.INPUT_METHOD_SERVICE);

        if (manager != null) {

            manager.hideSoftInputFromWindow(
                    urlBox.getWindowToken(),
                    0);
        }

        urlBox.clearFocus();
    }

    private int findTabIndex(
            BrowserTab tab) {

        return tabManager
                .getTabs()
                .indexOf(tab);
    }

    @Override
    public void onBackPressed() {

        BrowserTab tab =
                getActiveTab();

        if (tab != null &&
                tab.webView.canGoBack()) {

            hideKeyboard();

            tab.webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
                    }
