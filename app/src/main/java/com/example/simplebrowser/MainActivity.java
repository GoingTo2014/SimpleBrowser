package com.example.simplebrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.net.URLEncoder;
import java.io.File;

public class MainActivity extends Activity {

    private static final String SETTINGS_URL =
            "browser://settings";

    private static final int LOCAL_FILE_REQUEST =
            1401;

    private EditText urlBox;
    private ProgressBar progressBar;
    private LinearLayout toolbar;

    private BrowserSettings browserSettings;
    private TabManager tabManager;
    private SettingsPage settingsPage;
    private SecurityManager securityManager;

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        browserSettings =
                new BrowserSettings(this);

        setContentView(
                R.layout.main);

        urlBox =
                findViewById(R.id.url);

        progressBar =
                findViewById(R.id.progress);

        toolbar =
                findViewById(R.id.toolbar);

        FrameLayout container =
                findViewById(
                        R.id.webview_container);

        LinearLayout tabs =
                findViewById(R.id.tabs);

        ImageButton security =
                findViewById(R.id.security);

        tabManager =
                new TabManager(
                        this,
                        container,
                        tabs);

        settingsPage =
                new SettingsPage(
                        this,
                        browserSettings);

        securityManager =
                new SecurityManager(
                        this,
                        security);

        setupToolbarIcons();
        setupButtons();
        applyBrowserAppearance();

        BrowserTab initialTab =
                tabManager.addTab("about:blank");

        String launchUrl =
                getLaunchUrl();

        loadTabUrl(
                initialTab,
                launchUrl == null
                        ? browserSettings.getHomePage()
                        : launchUrl);
    }

    private String getLaunchUrl() {

        Intent intent =
                getIntent();

        if (intent == null ||
                !Intent.ACTION_VIEW.equals(
                        intent.getAction())) {
            return null;
        }

        Uri data =
                intent.getData();

        if (data == null) {
            return null;
        }

        String scheme =
                data.getScheme();

        if ("http".equalsIgnoreCase(scheme) ||
                "https".equalsIgnoreCase(scheme) ||
                "file".equalsIgnoreCase(scheme) ||
                "content".equalsIgnoreCase(scheme)) {

            return data.toString();
        }

        return null;
    }

    private void setupToolbarIcons() {

        int accent =
                ColorUtils.parseColor(
                        browserSettings.getAccentColor(),
                        Color.rgb(63, 81, 181));

        int iconColor =
                ColorUtils.getReadableTextColor(
                        accent);

        setToolbarIcon(
                R.id.back,
                BrowserIconDrawable.BACK,
                iconColor);

        setToolbarIcon(
                R.id.forward,
                BrowserIconDrawable.FORWARD,
                iconColor);

        setToolbarIcon(
                R.id.home,
                BrowserIconDrawable.HOME,
                iconColor);

        setToolbarIcon(
                R.id.reload,
                BrowserIconDrawable.RELOAD,
                iconColor);

        setToolbarIcon(
                R.id.settings,
                BrowserIconDrawable.SETTINGS,
                iconColor);
    }

    private void setToolbarIcon(
            int id,
            int type,
            int iconColor) {

        ImageButton button =
                findViewById(id);

        button.setImageDrawable(
                new BrowserIconDrawable(
                        type,
                        iconColor));

        button.setBackgroundColor(
                Color.TRANSPARENT);

        button.setScaleType(
                ImageButton.ScaleType.CENTER_INSIDE);
    }

    private void setupButtons() {

        ImageButton back =
                findViewById(R.id.back);

        ImageButton forward =
                findViewById(R.id.forward);

        ImageButton home =
                findViewById(R.id.home);

        ImageButton reload =
                findViewById(R.id.reload);

        ImageButton newTab =
                findViewById(R.id.new_tab);

        ImageButton settings =
                findViewById(R.id.settings);

        back.setOnClickListener(v -> {

            hideKeyboard();

            BrowserTab tab =
                    getActiveTab();

            if (tab != null &&
                    tab.webView.canGoBack()) {

                tab.webView.goBack();
            }

            updateNavigationButtons();
        });

        forward.setOnClickListener(v -> {

            hideKeyboard();

            BrowserTab tab =
                    getActiveTab();

            if (tab != null &&
                    tab.webView.canGoForward()) {

                tab.webView.goForward();
            }

            updateNavigationButtons();
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

            if (tab == null) {
                return;
            }

            if (tab.loading) {

                tab.webView.stopLoading();
                tab.loading = false;

                progressBar.setVisibility(
                        View.GONE);

                updateReloadButton(tab);
                updateNavigationButtons();

                return;
            }

            tab.webView.reload();
        });

        newTab.setOnClickListener(v -> {

            hideKeyboard();

            tabManager.addTab(
                    browserSettings
                            .getHomePage());
        });

        settings.setOnClickListener(v -> {

            hideKeyboard();

            BrowserTab tab =
                    getActiveTab();

            if (tab != null) {
                showSettings(tab);
            }
        });

        urlBox.setOnEditorActionListener(
                (v, actionId, event) -> {

            boolean enter =
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
                enter) {

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

        updateNavigationButtons();
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

        if (isLocalPath(input)) {

            loadTabUrl(
                    tab,
                    toLocalUri(input));

            return;
        }

        if (isUrl(input)) {

            if (!input.startsWith("http://") &&
                    !input.startsWith("https://") &&
                    !input.startsWith("file://") &&
                    !input.startsWith("content://")) {

                input =
                        "https://" + input;
            }

            loadTabUrl(
                    tab,
                    input);

            return;
        }

        try {

            String encoded =
                    URLEncoder.encode(
                            input,
                            "UTF-8");

            loadTabUrl(
                    tab,
                    getSearchUrl() +
                            encoded);

        } catch (Exception e) {

            loadTabUrl(
                    tab,
                    getSearchUrl() +
                            input);
        }
    }

    private boolean isLocalPath(
            String input) {

        if (input == null ||
                input.isEmpty()) {
            return false;
        }

        if (input.startsWith("/") &&
                !input.startsWith("//")) {
            return true;
        }

        if (input.startsWith("file://") ||
                input.startsWith("content://")) {
            return true;
        }

        try {
            return input.toLowerCase()
                    .endsWith(".html") &&
                    input.contains("/") &&
                    new File(input).exists();
        } catch (Exception e) {
            return false;
        }
    }

    private String toLocalUri(
            String input) {

        if (input.startsWith("file://") ||
                input.startsWith("content://")) {

            return input;
        }

        return Uri.fromFile(
                new File(input))
                .toString();
    }

    private boolean isUrl(
            String input) {

        if (input.startsWith("http://") ||
                input.startsWith("https://") ||
                input.startsWith("file://") ||
                input.startsWith("content://")) {

            return true;
        }

        if (input.contains(" ")) {
            return false;
        }

        return input.contains(".") ||
                input.startsWith("localhost:") ||
                input.startsWith("127.0.0.1:");
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
        updateNavigationButtons();
    }

    public void removeSettingsBridge(
            BrowserTab tab) {

        settingsPage.remove(tab);
    }

    public void pageStarted(
            BrowserTab tab,
            String url) {

        if (tab == getActiveTab()) {

            if (tab.settingsPage) {
                setUrlText(
                        SETTINGS_URL);
            } else {
                setUrlText(url);
            }

            progressBar.setProgress(0);
            progressBar.setVisibility(
                    View.VISIBLE);
        }

        updateSecurity(tab);
        updateReloadButton(tab);
        updateNavigationButtons();
    }

    public void pageFinished(
            BrowserTab tab,
            String url) {

        if (tab == getActiveTab()) {

            if (tab.settingsPage) {
                setUrlText(
                        SETTINGS_URL);
            } else {
                setUrlText(url);
            }

            progressBar.setProgress(100);
        }

        updateSecurity(tab);
        updateReloadButton(tab);
        updateNavigationButtons();

        if (tab == getActiveTab() &&
                !tab.loading) {

            progressBar.postDelayed(
                    () -> {

                        if (tab == getActiveTab() &&
                                !tab.loading) {

                            progressBar.setVisibility(
                                    View.GONE);
                        }

                    },
                    100);
        }
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

        updateReloadButton(tab);
        updateNavigationButtons();
    }

    public void settingsLoaded(
            BrowserTab tab) {

        if (tab == getActiveTab()) {

            setUrlText(
                    SETTINGS_URL);

            progressBar.setVisibility(
                    View.GONE);
        }

        updateReloadButton(tab);
        updateNavigationButtons();
    }

    private void updateNavigationButtons() {

        BrowserTab tab =
                getActiveTab();

        boolean backEnabled =
                tab != null &&
                tab.webView.canGoBack();

        boolean forwardEnabled =
                tab != null &&
                tab.webView.canGoForward();

        ImageButton back =
                findViewById(R.id.back);

        ImageButton forward =
                findViewById(R.id.forward);

        setToolbarButtonState(
                back,
                backEnabled);

        setToolbarButtonState(
                forward,
                forwardEnabled);
    }

    private void setToolbarButtonState(
            ImageButton button,
            boolean enabled) {

        button.setEnabled(enabled);
        button.setAlpha(
                enabled ? 1f : 0.35f);
    }

    private void updateReloadButton(
            BrowserTab tab) {

        if (tab == null ||
                tab != getActiveTab()) {
            return;
        }

        ImageButton reload =
                findViewById(R.id.reload);

        int accent =
                ColorUtils.parseColor(
                        browserSettings
                                .getAccentColor(),
                        Color.rgb(63, 81, 181));

        int iconColor =
                ColorUtils.getReadableTextColor(
                        accent);

        reload.setImageDrawable(
                new BrowserIconDrawable(
                        tab.loading
                                ? BrowserIconDrawable.STOP
                                : BrowserIconDrawable.RELOAD,
                        iconColor));

        reload.setContentDescription(
                tab.loading
                        ? "Stop loading"
                        : "Reload");
    }

    public void applyWebsiteSettings() {
        tabManager.applyWebSettings();
        updateNavigationButtons();
        updateReloadButton(
                getActiveTab());
    }

    public void applyBrowserAppearance() {

        int accent =
                ColorUtils.parseColor(
                        browserSettings
                                .getAccentColor(),
                        Color.rgb(63, 81, 181));

        int toolbarText =
                ColorUtils.getReadableTextColor(
                        accent);

        toolbar.setBackgroundColor(
                accent);

        setToolbarIcon(
                R.id.back,
                BrowserIconDrawable.BACK,
                toolbarText);

        setToolbarIcon(
                R.id.forward,
                BrowserIconDrawable.FORWARD,
                toolbarText);

        setToolbarIcon(
                R.id.home,
                BrowserIconDrawable.HOME,
                toolbarText);

        setToolbarIcon(
                R.id.settings,
                BrowserIconDrawable.SETTINGS,
                toolbarText);

        for (int id :
                new int[] {
                        R.id.back,
                        R.id.forward,
                        R.id.home,
                        R.id.reload,
                        R.id.settings
                }) {

            ImageButton button =
                    findViewById(id);

            button.setBackgroundColor(
                    Color.TRANSPARENT);
        }

        int urlBackground =
                ColorUtils.mix(
                        accent,
                        Color.WHITE,
                        0.82f);

        int urlText =
                ColorUtils.getReadableTextColor(
                        urlBackground);

        urlBox.setBackgroundColor(
                urlBackground);

        urlBox.setTextColor(urlText);

        urlBox.setHintTextColor(
                ColorUtils.mix(
                        urlBackground,
                        urlText,
                        0.45f));

        updateReloadButton(
                getActiveTab());

        securityManager.applyAppearance();

        tabManager.updateTabAppearanceColors();
        tabManager.updateTitles();
    }

    public void applyDesktopMode() {

        tabManager.applyWebSettings();

        for (BrowserTab tab :
                tabManager.getTabs()) {

            if (!tab.settingsPage) {
                tab.webView.reload();
            }
        }
    }

    public void openLocalFilePicker() {

        Intent intent =
                new Intent(
                        Intent.ACTION_GET_CONTENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE);

        intent.setType(
                "text/html");

        try {

            startActivityForResult(
                    intent,
                    LOCAL_FILE_REQUEST);

        } catch (Exception e) {

            Intent fallback =
                    new Intent(
                            Intent.ACTION_OPEN_DOCUMENT);

            fallback.addCategory(
                    Intent.CATEGORY_OPENABLE);

            fallback.setType(
                    "text/html");

            try {
                startActivityForResult(
                        fallback,
                        LOCAL_FILE_REQUEST);
            } catch (Exception ignored) {
                // No compatible file picker is installed.
            }
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data);

        if (requestCode !=
                LOCAL_FILE_REQUEST ||
                resultCode != RESULT_OK ||
                data == null) {
            return;
        }

        Uri uri =
                data.getData();

        if (uri == null) {
            return;
        }

        BrowserTab tab =
                getActiveTab();

        if (tab != null) {
            loadTabUrl(
                    tab,
                    uri.toString());
        }
    }

    public void chooseDefaultBrowser() {

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                                "http://example.com/"));

        intent.addCategory(
                Intent.CATEGORY_BROWSABLE);

        try {
            startActivity(intent);
        } catch (Exception e) {
            // No browser activity is registered.
        }
    }

    public void updateTabTitle(
            BrowserTab tab) {
        tabManager.updateTabTitle(tab);
    }

    public void updateNavigationButtonsForTabs() {
        updateNavigationButtons();
    }

    public void updateSecurity(
            BrowserTab tab) {
        securityManager.updateIcon(tab);
    }

    public BrowserTab getActiveTab() {
        return tabManager.getActiveTab();
    }

    public TabManager getTabManager() {
        return tabManager;
    }

    public BrowserSettings
            getBrowserSettings() {
        return browserSettings;
    }

    public void setUrlText(
            String text) {
        urlBox.setText(
                text == null ? "" : text);
    }

    public void setLoading(
            boolean loading) {

        BrowserTab tab =
                getActiveTab();

        if (tab != null) {
            tab.loading = loading;
        }

        progressBar.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE);

        updateReloadButton(tab);
        updateNavigationButtons();
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

    @Override
    public void onBackPressed() {

        BrowserTab tab =
                getActiveTab();

        if (tab != null &&
                tab.webView.canGoBack()) {

            hideKeyboard();
            tab.webView.goBack();
            return;
        }

        super.onBackPressed();
    }
}
