package com.example.simplebrowser;

import android.app.Activity;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.GeolocationPermissions;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;

import java.io.File;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private static final String SETTINGS_ROOT =
            "browser://settings";

    private EditText urlBox;
    private ProgressBar progressBar;
    private LinearLayout toolbar;
    private PopupWindow browserMenu;

    private BrowserSettings browserSettings;
    private TabManager tabManager;
    private SettingsPage settingsPage;
    private SecurityManager securityManager;

    private BrowserHistory browserHistory;
    private DefaultPage defaultPage;
    private HistoryPage historyPage;
    private ErrorPage errorPage;

    private static final int INCOGNITO_CHROME =
            Color.rgb(32, 33, 36);

    private static final int INCOGNITO_URL =
            Color.rgb(48, 49, 52);

    private GeolocationPermissions.Callback
            pendingGeolocationCallback;

    private String pendingGeolocationOrigin;

    private static final int LOCATION_PERMISSION_REQUEST =
            1501;

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

        browserHistory =
                new BrowserHistory(this);

        defaultPage =
                new DefaultPage(this);

        historyPage =
                new HistoryPage(
                        this,
                        browserHistory);

        errorPage =
                new ErrorPage(this);

        setupToolbarIcons();
        setupButtons();
        applyBrowserAppearance();

        String launchUrl =
                getLaunchUrl();

        if (launchUrl != null) {

            BrowserTab initialTab =
                    tabManager.addTab(
                            "about:blank");

            loadTabUrl(
                    initialTab,
                    launchUrl);

        } else if (
                browserSettings.isRestoreTabsEnabled() &&
                tabManager.restoreTabs()) {

            // Restored tabs are already loaded.

        } else {

            BrowserTab initialTab =
                    tabManager.addTab(
                            "about:blank");

            loadTabUrl(
                    initialTab,
                    browserSettings.getHomePage());
        }
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
                getAccentColor();

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
                browserSettings == null
                        ? BrowserIconDrawable.RELOAD
                        : BrowserIconDrawable.RELOAD,
                iconColor);

        setToolbarIcon(
                R.id.settings,
                BrowserIconDrawable.MORE,
                iconColor);
    }

    private void setToolbarIcon(
            int id,
            int type,
            int color) {

        ImageButton button =
                findViewById(id);

        button.setImageDrawable(
                new BrowserIconDrawable(
                        type,
                        color));

        button.setBackgroundColor(
                Color.TRANSPARENT);

        button.setScaleType(
                ImageButton.ScaleType.CENTER_INSIDE);
    }

    private void setupButtonPressAnimation(
            final ImageButton button) {

        int iconColor =
                ColorUtils.getReadableTextColor(
                        getAccentColor());

        button.setOnTouchListener(null);
        button.setOnHoverListener(null);

        button.setBackground(
                ButtonFeedback.create(
                        iconColor));

        button.setScaleX(1f);
        button.setScaleY(1f);
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

            } else {

                tab.webView.reload();
            }
        });

        settings.setOnClickListener(v -> {

            hideKeyboard();

            showBrowserMenu(settings);
        });

        ImageButton newTab =
                findViewById(R.id.new_tab);

        newTab.setOnClickListener(v -> {

            hideKeyboard();

            tabManager.addTab(
                    browserSettings.getHomePage());
        });

        setupButtonPressAnimation(back);
        setupButtonPressAnimation(forward);
        setupButtonPressAnimation(home);
        setupButtonPressAnimation(reload);
        setupButtonPressAnimation(settings);

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

    private void showBrowserMenu(
            ImageButton anchor) {

        if (browserMenu != null &&
                browserMenu.isShowing()) {

            browserMenu.dismiss();
            return;
        }

        int accent =
                getAccentColor();

        int readable =
                ColorUtils.getReadableTextColor(
                        accent);

        int menuBackground =
                ColorUtils.mix(
                        accent,
                        Color.WHITE,
                        0.90f);

        LinearLayout menu =
                new LinearLayout(this);

        menu.setOrientation(
                LinearLayout.VERTICAL);

        menu.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8));

        menu.setBackgroundColor(
                menuBackground);

        addMenuActionButton(
                menu,
                "History",
                () -> {

                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    BrowserTab tab =
                            getActiveTab();

                    if (tab != null) {
                        showHistory(tab, "");
                    }
                },
                accent,
                readable);

        addMenuActionButton(
                menu,
                "New Incognito Tab",
                () -> {

                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    tabManager.addIncognitoTab(
                            browserSettings.getHomePage());
                },
                accent,
                readable);

        addMenuSectionButton(
                menu,
                "General",
                "general",
                accent,
                readable);

        addMenuSectionButton(
                menu,
                "Websites",
                "websites",
                accent,
                readable);

        addMenuSectionButton(
                menu,
                "Appearance",
                "appearance",
                accent,
                readable);

        addMenuSectionButton(
                menu,
                "Privacy & Security",
                "privacy-security",
                accent,
                readable);

        CheckBox desktop =
                new CheckBox(this);

        desktop.setText(
                "Desktop mode");

        desktop.setTextColor(
                ColorUtils
                        .getReadableTextColor(
                                menuBackground));

        desktop.setChecked(
                browserSettings.isDesktopMode());

        desktop.setPadding(
                dp(4),
                dp(2),
                dp(4),
                dp(2));

        desktop.setOnCheckedChangeListener(
                (button, checked) -> {

                    browserSettings.setBoolean(
                            "desktop_mode",
                            checked);

                    applyDesktopMode();

                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }
                });

        menu.addView(
                desktop,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(48)));

        browserMenu =
                new PopupWindow(
                        menu,
                        dp(220),
                        android.view.ViewGroup
                                .LayoutParams
                                .WRAP_CONTENT,
                        true);

        browserMenu.setBackgroundDrawable(
                new android.graphics.drawable
                        .ColorDrawable(
                                menuBackground));

        browserMenu.setOutsideTouchable(
                true);

        browserMenu.showAsDropDown(
                anchor,
                anchor.getWidth() -
                        dp(220),
                0);
    }

    private void addMenuActionButton(
            LinearLayout menu,
            String text,
            final Runnable action,
            int accent,
            int readable) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(readable);
        button.setBackgroundColor(accent);

        button.setOnClickListener(
                v -> action.run());

        menu.addView(
                button,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(40)));
    }

    private void addMenuSectionButton(
            LinearLayout menu,
            String text,
            String section,
            int accent,
            int readable) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(readable);
        button.setBackgroundColor(accent);

        button.setOnClickListener(
                v -> {

                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    BrowserTab tab =
                            getActiveTab();

                    if (tab != null) {
                        showSettings(
                                tab,
                                section);
                    }
                });

        menu.addView(
                button,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(40)));
    }

    public void openUrlOrSearch(
            String input) {

        openUrlOrSearchForTab(
                getActiveTab(),
                input);
    }

    public void openUrlOrSearchForTab(
            BrowserTab tab,
            String input) {

        hideKeyboard();

        if (tab == null ||
                input == null) {
            return;
        }

        String value =
                input.trim();

        if (value.isEmpty()) {
            return;
        }

        String settingsSection =
                getSettingsSection(value);

        if (settingsSection != null) {

            showSettings(
                    tab,
                    settingsSection);

            return;
        }

        if ("browser://default".equalsIgnoreCase(value)) {

            showDefaultPage(tab);
            return;
        }

        if ("browser://history".equalsIgnoreCase(value)) {

            showHistory(tab, "");
            return;
        }

        if (isLocalPath(value)) {

            loadTabUrl(
                    tab,
                    toLocalUri(value));

            return;
        }

        if (isUrl(value)) {

            if (!value.startsWith("http://") &&
                    !value.startsWith("https://") &&
                    !value.startsWith("file://") &&
                    !value.startsWith("content://")) {

                value =
                        "https://" + value;
            }

            loadTabUrl(
                    tab,
                    value);

            return;
        }

        loadTabUrl(
                tab,
                buildSearchUrl(value));
    }

        private String getSettingsSection(
            String url) {

        if (url == null) {
            return null;
        }

        String lower =
                url.trim()
                        .toLowerCase();

        if (!lower.startsWith(
                SETTINGS_ROOT)) {

            return null;
        }

        if (lower.equals(
                SETTINGS_ROOT)) {

            return "general";
        }

        String suffix =
                lower.substring(
                        SETTINGS_ROOT.length());

        if (suffix.startsWith("/")) {
            suffix = suffix.substring(1);
        }

        if ("general".equals(suffix)) {
            return "general";
        }

        if ("websites".equals(suffix)) {
            return "websites";
        }

        if ("appearance".equals(suffix)) {
            return "appearance";
        }

        if ("privacy".equals(suffix) ||
                "privacy-security"
                        .equals(suffix)) {
            return "privacy-security";
        }

        return null;
    }

    public String getSettingsUrl(
            String section) {

        if (!"websites".equals(section) &&
                !"appearance".equals(section) &&
                !"privacy-security".equals(section)) {
            section = "general";
        }

        return SETTINGS_ROOT +
                "/" +
                section;
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

        if ("custom".equals(engine)) {
            String custom =
                    browserSettings
                            .getCustomSearchUrl();

            if (!custom.trim().isEmpty()) {
                return custom.trim();
            }
        }

        return
                "https://www.google.com/search?q=";
    }

    private String buildSearchUrl(
            String input) {

        String encoded;

        try {

            encoded =
                    URLEncoder.encode(
                            input,
                            "UTF-8");

        } catch (Exception e) {

            encoded = input;
        }

        String template =
                getSearchUrl();

        if (template.contains("%s")) {

            return template.replace(
                    "%s",
                    encoded);
        }

        if (template.endsWith("=") ||
                template.endsWith("?") ||
                template.endsWith("&")) {

            return template + encoded;
        }

        if (template.contains("?")) {
            return template + "&q=" + encoded;
        }

        return template + "?q=" + encoded;
    }

    public void loadTabUrl(
            BrowserTab tab,
            String url) {

        if (tab == null ||
                tab.webView == null) {
            return;
        }

        String settingsSection =
                getSettingsSection(url);

        if (settingsSection != null) {

            showSettings(
                    tab,
                    settingsSection);

            return;
        }

        if ("browser://default".equalsIgnoreCase(url)) {

            showDefaultPage(tab);
            return;
        }

        if ("browser://history".equalsIgnoreCase(url)) {

            showHistory(tab, "");
            return;
        }

        removeInternalPageState(tab);

        tab.webView.loadUrl(url);
    }

    private void removeInternalPageState(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        settingsPage.remove(tab);

        defaultPage.remove(tab);
        historyPage.remove(tab);

        tab.settingsPage = false;
        tab.defaultPage = false;
        tab.historyPage = false;
        tab.errorPage = false;
        tab.settingsSection = "general";
    }

    public void showDefaultPage(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        removeInternalPageState(tab);
        defaultPage.show(tab);
        tabManager.selectTab(tab);
    }

    public void showErrorPage(
            BrowserTab tab,
            String url,
            String description) {

        if (tab == null) {
            return;
        }

        removeInternalPageState(tab);

        tab.errorPage = true;
        errorPage.show(
                tab,
                url,
                description);

        if (tab == getActiveTab()) {
            applyActiveTabAppearance();
        }
    }

    public void showHistory(
            BrowserTab tab,
            String query) {

        if (tab == null) {
            return;
        }

        removeInternalPageState(tab);
        historyPage.show(
                tab,
                query);
        tabManager.selectTab(tab);
    }

    public void recordVisit(
            BrowserTab tab,
            String url) {

        if (tab == null ||
                tab.isIncognito ||
                tab.settingsPage ||
                tab.defaultPage ||
                tab.historyPage ||
                tab.errorPage ||
                url == null) {
            return;
        }

        browserHistory.addVisit(
                url,
                tab.title);
    }

    public void saveTabs() {

        if (browserSettings
                .isRestoreTabsEnabled()) {

            tabManager.saveTabs();

        } else {

            browserSettings.setSavedTabsJson("");
        }
    }

    public void applyActiveTabAppearance() {

        BrowserTab tab =
                getActiveTab();

        int chromeColor;
        int urlBackground;

        if (tab != null &&
                tab.isIncognito) {

            chromeColor =
                    INCOGNITO_CHROME;

            urlBackground =
                    INCOGNITO_URL;

        } else {

            int accent =
                    getAccentColor();

            chromeColor =
                    accent;

            urlBackground =
                    ColorUtils.darken(
                            accent,
                            0.12f);
        }

        int readable =
                ColorUtils.getReadableTextColor(
                        chromeColor);

        toolbar.setBackgroundColor(
                chromeColor);

        setToolbarIcon(
                R.id.back,
                BrowserIconDrawable.BACK,
                readable);

        setToolbarIcon(
                R.id.forward,
                BrowserIconDrawable.FORWARD,
                readable);

        setToolbarIcon(
                R.id.home,
                BrowserIconDrawable.HOME,
                readable);

        setToolbarIcon(
                R.id.settings,
                BrowserIconDrawable.MORE,
                readable);

        ImageButton newTab =
                findViewById(R.id.new_tab);

        newTab.setColorFilter(
                readable,
                PorterDuff.Mode.SRC_IN);

        urlBox.setBackgroundColor(
                Color.TRANSPARENT);

        GradientDrawable urlDrawable =
                new GradientDrawable();

        urlDrawable.setColor(
                urlBackground);

        urlDrawable.setCornerRadius(
                dp(5));

        urlDrawable.setStroke(
                dp(1),
                ColorUtils.darken(
                        urlBackground,
                        0.25f));

        urlBox.setBackground(
                urlDrawable);

        int urlText =
                ColorUtils.getReadableTextColor(
                        urlBackground);

        urlBox.setTextColor(urlText);

        urlBox.setHintTextColor(
                ColorUtils.mix(
                        urlBackground,
                        urlText,
                        0.50f));

        securityManager.applyAppearance();

        tabManager.updateTabAppearanceColors();
        tabManager.updateTitles();
        updateReloadButton(tab);
        updateNavigationButtons();
    }

        public void showSettings(
            BrowserTab tab) {

        showSettings(
                tab,
                "general");
    }

    public void showSettings(
            BrowserTab tab,
            String section) {

        if (tab == null) {
            return;
        }

        settingsPage.show(
                tab,
                section);

        tabManager.selectTab(tab);

        updateSecurity(tab);
        updateNavigationButtons();
    }

    public void showSettingsSection(
            BrowserTab tab,
            String section) {

        showSettings(
                tab,
                section);
    }

    public void handleGeolocationRequest(
            String origin,
            GeolocationPermissions.Callback callback) {

        if (!browserSettings.isGeolocationEnabled() ||
                callback == null) {

            if (callback != null) {
                callback.invoke(
                        origin,
                        false,
                        false);
            }

            return;
        }

        if (Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(
                        Manifest.permission
                                .ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

            callback.invoke(
                    origin,
                    true,
                    false);

            return;
        }

        if (pendingGeolocationCallback != null) {

            pendingGeolocationCallback.invoke(
                    pendingGeolocationOrigin,
                    false,
                    false);
        }

        pendingGeolocationOrigin = origin;
        pendingGeolocationCallback = callback;

        requestPermissions(
                new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode !=
                LOCATION_PERMISSION_REQUEST) {
            return;
        }

        if (pendingGeolocationCallback == null) {
            return;
        }

        boolean granted =
                grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED;

        pendingGeolocationCallback.invoke(
                pendingGeolocationOrigin,
                granted,
                false);

        pendingGeolocationCallback = null;
        pendingGeolocationOrigin = null;
    }

    public void removeSettingsBridge(
            BrowserTab tab) {

        settingsPage.remove(tab);
    }

    public void restoreSettingsPage(
            BrowserTab tab,
            String section) {

        settingsPage.restore(
                tab,
                section);
    }

    public void pageStarted(
            BrowserTab tab,
            String url) {

        if (tab == getActiveTab()) {

            if (tab.settingsPage) {

                setUrlText(
                        getSettingsUrl(
                                tab.settingsSection));

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
                        getSettingsUrl(
                                tab.settingsSection));

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

                        if (tab ==
                                getActiveTab() &&
                                !tab.loading) {

                            progressBar
                                    .setVisibility(
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

        progressBar.setProgress(
                progress);

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
                    getSettingsUrl(
                            tab.settingsSection));

            progressBar.setVisibility(
                    View.GONE);
        }

        updateReloadButton(tab);
        updateNavigationButtons();
    }

    public void updateTabTitle(
            BrowserTab tab) {

        if (tab == null ||
                tabManager == null) {
            return;
        }

        tabManager.updateTabTitle(tab);
    }

    public void updateSecurity(
            BrowserTab tab) {

        if (securityManager == null) {
            return;
        }

        securityManager.updateIcon(tab);
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
                enabled
                        ? 1f
                        : 0.30f);

        button.setScaleX(1f);
        button.setScaleY(1f);
    }

    private void updateReloadButton(
            BrowserTab tab) {

        if (tab == null ||
                tab != getActiveTab()) {
            return;
        }

        ImageButton reload =
                findViewById(R.id.reload);

        int iconColor =
                ColorUtils
                        .getReadableTextColor(
                                getAccentColor());

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

    private int getAccentColor() {

        return ColorUtils.parseColor(
                browserSettings.getAccentColor(),
                Color.rgb(
                        63,
                        81,
                        181));
    }

    public void applyWebsiteSettings() {

        tabManager.applyWebSettings();
        updateNavigationButtons();
        updateReloadButton(
                getActiveTab());
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

    public void applyBrowserAppearance() {

        applyActiveTabAppearance();
    }

        public void updateNavigationButtonsForTabs() {
        updateNavigationButtons();
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

    public void hideKeyboard() {

        InputMethodManager manager =
                (InputMethodManager)
                        getSystemService(
                                Context
                                        .INPUT_METHOD_SERVICE);

        if (manager != null) {

            manager.hideSoftInputFromWindow(
                    urlBox.getWindowToken(),
                    0);
        }

        urlBox.clearFocus();
    }

    @Override
    protected void onPause() {

        saveTabs();
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        saveTabs();

        if (browserHistory != null) {
            browserHistory.close();
        }

        super.onDestroy();
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
                    1401);
        } catch (Exception e) {
            startActivityForResult(
                    new Intent(
                            Intent.ACTION_OPEN_DOCUMENT)
                            .addCategory(
                                    Intent.CATEGORY_OPENABLE)
                            .setType("text/html"),
                    1401);
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

        if (requestCode != 1401 ||
                resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {
            return;
        }

        BrowserTab tab =
                getActiveTab();

        if (tab != null) {
            loadTabUrl(
                    tab,
                    data.getData().toString());
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
        } catch (Exception ignored) {
        }
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

    public int dp(int value) {

        return (int) (
                value *
                getResources()
                        .getDisplayMetrics()
                        .density +
                0.5f);
    }
}
