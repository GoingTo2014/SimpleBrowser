package com.example.simplebrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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

        button.setOnTouchListener(
                (v, event) -> {

                    if (!button.isEnabled()) {
                        return false;
                    }

                    if (event.getAction() ==
                            MotionEvent.ACTION_DOWN) {

                        button.animate()
                                .scaleX(0.90f)
                                .scaleY(0.90f)
                                .setDuration(90)
                                .start();

                    } else if (
                            event.getAction() ==
                                    MotionEvent.ACTION_UP ||
                            event.getAction() ==
                                    MotionEvent.ACTION_CANCEL) {

                        button.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .start();
                    }

                    return false;
                });

        /*
         * Also support a real pointer hover when the
         * browser is used with a mouse/trackpad.
         */
        button.setOnHoverListener(
                (v, event) -> {

                    if (!button.isEnabled()) {
                        return false;
                    }

                    if (event.getAction() ==
                            MotionEvent.ACTION_HOVER_ENTER) {

                        button.animate()
                                .scaleX(1.06f)
                                .scaleY(1.06f)
                                .setDuration(100)
                                .start();

                    } else if (
                            event.getAction() ==
                                    MotionEvent.ACTION_HOVER_EXIT) {

                        button.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .start();
                    }

                    return false;
                });
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

        hideKeyboard();

        BrowserTab tab =
                getActiveTab();

        if (tab == null) {
            return;
        }

        String settingsSection =
                getSettingsSection(input);

        if (settingsSection != null) {

            showSettings(
                    tab,
                    settingsSection);

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

        return
                "https://www.google.com/search?q=";
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

        if (tab.settingsPage) {
            removeSettingsBridge(tab);
        }

        tab.settingsPage = false;
        tab.settingsSection = "general";

        tab.webView.loadUrl(url);
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

        int accent =
                getAccentColor();

        int readable =
                ColorUtils
                        .getReadableTextColor(
                                accent);

        toolbar.setBackgroundColor(
                accent);

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
                ColorUtils
                        .getReadableTextColor(
                                urlBackground);

        urlBox.setBackgroundColor(
                urlBackground);

        urlBox.setTextColor(
                urlText);

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
                tab.settingsPage &&
                !"general".equals(
                        tab.settingsSection)) {

            showSettings(
                    tab,
                    "general");

            return;
        }

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
