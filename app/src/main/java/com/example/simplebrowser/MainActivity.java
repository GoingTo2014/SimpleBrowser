package com.example.simplebrowser;

import android.app.Activity;
import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebView;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private EditText urlBox;
    private ProgressBar progressBar;
    private LinearLayout toolbar;
    private PopupWindow browserMenu;

    private BrowserSettings browserSettings;
    private ProfileManager profileManager;
    private BookmarkStore bookmarkStore;
    private PasswordStore passwordStore;
    private BookmarksPage bookmarksPage;
    private PasswordsPage passwordsPage;
    private ProfilesPage profilesPage;
    private TabManager tabManager;
    private SettingsPage settingsPage;
    private SecurityManager securityManager;

    private BrowserHistory browserHistory;
    private DownloadHistory downloadHistory;
    private DefaultPage defaultPage;
    private HistoryPage historyPage;
    private DownloadsPage downloadsPage;
    private ErrorPage errorPage;
    private CookieStore cookieStore;
    private CookiesPage cookiesPage;
    private DemoPage demoPage;
    private UpdateManager updateManager;

    private static final int PASSWORD_AUTH_REQUEST = 2001;

    private boolean passwordManagerAuthenticated;
    private boolean awaitingPasswordAuthentication;
    private boolean profileSwitching;
    private BrowserTab pendingPasswordTab;
    private BrowserTab pendingPasswordFileTab;
    private String pendingPasswordExport;
    private static final int PASSWORD_EXPORT_REQUEST = 3101;
    private static final int PASSWORD_IMPORT_REQUEST = 3102;

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

        profileManager =
                new ProfileManager(this);

        browserSettings =
                new BrowserSettings(this);

        updateManager =
                new UpdateManager(
                        this,
                        browserSettings);

        WebView.setWebContentsDebuggingEnabled(
                browserSettings.isWebViewDebuggingEnabled());

        setContentView(
                R.layout.main);

        applySystemBarInsets();

        urlBox =
                findViewById(R.id.url);

        urlBox.setSelectAllOnFocus(true);

        urlBox.setHint(
                Localization.translate(
                        this,
                        "Search or enter an address"));

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

        downloadHistory =
                new DownloadHistory(this);

        defaultPage =
                new DefaultPage(this);

        historyPage =
                new HistoryPage(
                        this,
                        browserHistory);

        downloadsPage =
                new DownloadsPage(
                        this,
                        downloadHistory);

        errorPage =
                new ErrorPage(this);

        bookmarkStore = new BookmarkStore(this);
        passwordStore = new PasswordStore(this);
        bookmarksPage = new BookmarksPage(this, bookmarkStore);
        passwordsPage = new PasswordsPage(this, passwordStore);
        profilesPage = new ProfilesPage(this, profileManager);

        cookieStore =
                new CookieStore(this);
        cookieStore.restoreCookies();

        cookiesPage =
                new CookiesPage(
                        this,
                        cookieStore);

        demoPage =
                new DemoPage(this);

        setupToolbarIcons();
        setupButtons();
        applyResponsiveToolbar();
        applyBrowserAppearance();

        String launchUrl =
                getLaunchUrl();

        if (launchUrl != null) {

            tabManager.addTab(
                    launchUrl);

        } else if (
                browserSettings.isRestoreTabsEnabled() &&
                tabManager.restoreTabs()) {

            // Restored tabs are already loaded.

        } else {

            tabManager.addTab(
                    browserSettings.getHomePage());
        }

        if (updateManager != null) {
            updateManager.checkForUpdates(
                    false,
                    null);
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

        setToolbarIcon(
                R.id.bookmark,
                BrowserIconDrawable.BOOKMARK,
                iconColor);

        setContentDescription(
                R.id.back,
                "Back");
        setContentDescription(
                R.id.forward,
                "Forward");
        setContentDescription(
                R.id.home,
                "Home");
        setContentDescription(
                R.id.reload,
                "Reload");
        setContentDescription(
                R.id.settings,
                "Browser settings");
        setContentDescription(
                R.id.bookmark,
                "Bookmark");
        setContentDescription(
                R.id.tab_overview,
                "Tab overview");
        setContentDescription(
                R.id.new_tab,
                "New Tab");
    }

    private void setContentDescription(
            int id,
            String key) {

        View view =
                findViewById(id);

        if (view != null) {
            view.setContentDescription(
                    Localization.translate(
                            this,
                            key));
        }
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

        ImageButton tabOverview =
                findViewById(R.id.tab_overview);

        back.setOnClickListener(v -> {

            hideKeyboard();

            BrowserTab tab =
                    getActiveTab();

            if (tab != null &&
                    tab.errorPage) {

                goBackFromInternalPage(tab);

            } else if (tab != null &&
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
                    tab.errorPage) {

                goForwardFromInternalPage(tab);

            } else if (tab != null &&
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
                tabManager.updateTabLoadingState(tab);

                progressBar.setVisibility(
                        View.GONE);

                updateReloadButton(tab);
                updateNavigationButtons();

            } else {

                tab.loading = true;
                tabManager.updateTabLoadingState(tab);
                refreshTab(tab);
            }
        });

        settings.setOnClickListener(v -> {

            hideKeyboard();

            showBrowserMenu(settings);
        });

        ImageButton bookmark =
                findViewById(R.id.bookmark);

        bookmark.setOnClickListener(v -> {
            BrowserTab tab = getActiveTab();

            if (tab == null ||
                    tab.isIncognito ||
                    bookmarkStore == null ||
                    tab.webView == null) {
                return;
            }

            String url = tab.webView.getUrl();

            if (url == null ||
                    (!url.startsWith("http://") &&
                     !url.startsWith("https://"))) {
                return;
            }

            if (bookmarkStore.isBookmarked(url)) {
                bookmarkStore.deleteByUrl(url);
            } else {
                bookmarkStore.addOrUpdate(
                        url,
                        tab.title,
                        tab.favicon);
            }

            updateBookmarkButton(tab);
        });

        tabOverview.setOnClickListener(v -> {

            hideKeyboard();

            new TabOverviewDialog(this)
                    .show();
        });

        ImageButton newTab =
                findViewById(R.id.new_tab);

        newTab.setOnClickListener(v -> {

            hideKeyboard();

            tabManager.addCurrentModeTab(
                    browserSettings.getHomePage());
        });

        setupButtonPressAnimation(back);
        setupButtonPressAnimation(forward);
        setupButtonPressAnimation(home);
        setupButtonPressAnimation(reload);
        setupButtonPressAnimation(settings);
        setupButtonPressAnimation(bookmark);
        setupButtonPressAnimation(tabOverview);

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

        BrowserTab active =
                getActiveTab();

        boolean incognito =
                active != null &&
                        active.isIncognito;

        int accent =
                incognito
                        ? Color.rgb(
                                48, 49, 52)
                        : getAccentColor();

        int readable =
                ColorUtils.getReadableTextColor(
                        accent);

        int menuBackground =
                ColorUtils.darken(
                        accent,
                        incognito
                                ? 0.03f
                                : 0.06f);

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

        addCurrentProfileMenuHeader(
                menu,
                accent,
                readable);

        addMenuActionButton(menu, "Bookmarks", () -> {
            if (browserMenu != null) browserMenu.dismiss();
            BrowserTab tab = getActiveTab();
            if (tab != null) showBookmarks(tab, "");
        }, accent, readable);

        addMenuActionButton(menu, "Password manager", () -> {
            if (browserMenu != null) browserMenu.dismiss();
            BrowserTab tab = getActiveTab();
            if (tab != null) showPasswordManager(tab);
        }, accent, readable);

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
                "Downloads",
                () -> {

                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    BrowserTab tab =
                            getActiveTab();

                    if (tab != null) {
                        showDownloads(tab, "");
                    }
                },
                accent,
                readable);

        addMenuActionButton(
                menu,
                incognito
                        ? "Exit Incognito Mode"
                        : "Enter Incognito Mode",
                () -> {

                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    if (incognito) {
                        tabManager.exitIncognitoMode();
                    } else {
                        tabManager.enterIncognitoMode(
                                browserSettings.getHomePage());
                    }
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
                Localization.translate(
                        this,
                        "Desktop mode"));

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

    private void addCurrentProfileMenuHeader(
            LinearLayout menu,
            int accent,
            int readable) {

        ProfileManager.Profile profile =
                profileManager.getActiveProfile(
                        this);

        if (profile == null) {
            return;
        }

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL);
        row.setGravity(
                android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8));
        row.setBackgroundColor(
                accent);
        row.setClickable(true);
        row.setOnClickListener(
                v -> {
                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    BrowserTab tab =
                            getActiveTab();

                    if (tab != null) {
                        showProfiles(tab);
                    }
                });

        TextView avatar =
                new TextView(this);

        avatar.setGravity(
                android.view.Gravity.CENTER);
        avatar.setText(
                profile.name == null ||
                        profile.name.trim().isEmpty()
                        ? "?"
                        : String.valueOf(
                                Character.toUpperCase(
                                        profile.name
                                                .trim()
                                                .charAt(0))));
        avatar.setTextColor(readable);
        avatar.setTextSize(18f);

        GradientDrawable circle =
                new GradientDrawable();
        circle.setShape(
                GradientDrawable.OVAL);
        circle.setColor(
                Color.TRANSPARENT);
        circle.setStroke(
                dp(2),
                readable);

        avatar.setBackground(circle);

        row.addView(
                avatar,
                new LinearLayout.LayoutParams(
                        dp(40),
                        dp(40)));

        TextView name =
                new TextView(this);

        name.setText(profile.name);
        name.setTextColor(readable);
        name.setTextSize(16f);
        name.setGravity(
                android.view.Gravity.CENTER_VERTICAL);
        name.setPadding(
                dp(10),
                0,
                dp(4),
                0);

        row.addView(
                name,
                new LinearLayout.LayoutParams(
                        0,
                        dp(40),
                        1f));

        menu.addView(
                row,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(56)));
    }

    private void addMenuActionButton(
            LinearLayout menu,
            String text,
            final Runnable action,
            int accent,
            int readable) {

        Button button =
                new Button(this);

        button.setText(
                Localization.translate(
                        this,
                        text));
        button.setAllCaps(false);
        button.setTextColor(
                readable);
        button.setBackgroundColor(
                accent);
        button.setGravity(
                android.view.Gravity.RIGHT |
                android.view.Gravity.CENTER_VERTICAL);
        button.setPadding(
                dp(12),
                0,
                dp(14),
                0);

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

        button.setText(
                Localization.translate(
                        this,
                        text));
        button.setAllCaps(false);
        button.setTextColor(readable);
        button.setBackgroundColor(accent);
        button.setGravity(
                android.view.Gravity.RIGHT |
                android.view.Gravity.CENTER_VERTICAL);
        button.setPadding(
                dp(12),
                0,
                dp(14),
                0);

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

    public boolean handleWebViewLongPress(
            BrowserTab tab) {

        if (tab == null ||
                tab.webView == null) {
            return false;
        }

        WebView.HitTestResult result =
                tab.webView.getHitTestResult();

        if (result == null) {
            return false;
        }

        int type =
                result.getType();

        String extra =
                result.getExtra();

        if (extra == null ||
                extra.trim().isEmpty()) {
            return false;
        }

        boolean image =
                type ==
                        WebView.HitTestResult
                                .IMAGE_TYPE ||
                type ==
                        WebView.HitTestResult
                                .SRC_IMAGE_ANCHOR_TYPE;

        // Android's WebView HitTestResult uses type 9 for video.
        // Use the numeric value here because VIDEO_TYPE is not exposed
        // by every Android SDK stub used to compile the app.
        boolean video =
                type == 9;

        // Audio uses type 10 in Android WebView. Treat it like other media.
        boolean audio =
                type == 10;

        boolean media =
                video ||
                audio;

        boolean link =
                type ==
                        WebView.HitTestResult
                                .SRC_ANCHOR_TYPE;

        if (!image &&
                !media &&
                !link) {
            return false;
        }

        final String target =
                extra.trim();

        String title;

        if (image) {
            title = "Image";
        } else if (media) {
            title = audio ? "Audio" : "Video";
        } else {
            title = "Link";
        }

        final String[] actions =
                image
                        ? new String[] {
                                "Open",
                                "Open in new tab",
                                "Download",
                                "Share",
                                "Copy URL"
                        }
                        : media
                        ? new String[] {
                                "Open",
                                "Open in new tab",
                                "Download",
                                "Share",
                                "Copy URL"
                        }
                        : new String[] {
                                "Open",
                                "Open in new tab",
                                "Download",
                                "Share",
                                "Copy link"
                        };

        final String[] translatedActions =
                new String[actions.length];

        for (int i = 0;
                i < actions.length;
                i++) {
            translatedActions[i] =
                    Localization.translate(
                            this,
                            actions[i]);
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle(
                        Localization.translate(
                                this,
                                title))
                .setItems(
                        translatedActions,
                        (dialog, which) -> {

                            String action =
                                    actions[which];

                            if ("Open".equals(action)) {

                                openUrlOrSearchForTab(
                                        tab,
                                        target);

                            } else if (
                                    "Open in new tab"
                                            .equals(action)) {

                                tabManager.addCurrentModeTab(
                                        target);

                            } else if (
                                    "Download".equals(action)) {

                                startDownload(
                                        tab,
                                        target,
                                        tab.webView
                                                .getSettings()
                                                .getUserAgentString(),
                                        null,
                                        image
                                                ? "image/*"
                                                : media
                                                ? (audio
                                                ? "audio/*"
                                                : "video/*")
                                                : null,
                                        -1);

                            } else if (
                                    "Share".equals(action)) {

                                shareUrl(
                                        target);

                            } else {

                                copyToClipboard(
                                        image
                                                ? "Image URL"
                                                : media
                                                ? (audio
                                                ? "Audio URL"
                                                : "Video URL")
                                                : "Link",
                                        target);
                            }
                        })
                .show();

        return true;
    }

    private void copyToClipboard(
            String label,
            String value) {

        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                CLIPBOARD_SERVICE);

        if (clipboard != null) {

            clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                            label,
                            value));

            android.widget.Toast.makeText(
                    this,
                    Localization.translate(
                            this,
                            "Copied"),
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void shareUrl(
            String url) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_SEND);

            intent.setType(
                    "text/plain");

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    url);

            startActivity(
                    Intent.createChooser(
                            intent,
                            Localization.translate(
                                    this,
                                    "Share")));

        } catch (Exception e) {

            android.widget.Toast.makeText(
                    this,
                    Localization.translate(
                            this,
                            "No app can share this"),
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void showBookmarks(BrowserTab tab, String query) {
        if (tab == null) return;
        removeInternalPageState(tab);
        bookmarksPage.show(tab, query);
        tabManager.selectTab(tab);
        recordInternalVisit(tab, BrowserPage.BOOKMARKS);
    }

    public void addCurrentPageBookmark() {
        BrowserTab tab = getActiveTab();
        if (tab == null || tab.webView == null) return;
        String url = tab.webView.getUrl();
        if (url == null ||
                (!url.startsWith("http://") && !url.startsWith("https://"))) {
            android.widget.Toast.makeText(this,
                    Localization.translate(this, "bookmarks.invalid"),
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        bookmarkStore.addOrUpdate(url, tab.title, tab.favicon);
        android.widget.Toast.makeText(this,
                Localization.translate(this, "bookmarks.added"),
                android.widget.Toast.LENGTH_SHORT).show();
    }

    public void showProfiles(BrowserTab tab) {
        if (tab == null) return;
        removeInternalPageState(tab);
        profilesPage.show(tab);
        tabManager.selectTab(tab);
        recordInternalVisit(tab, BrowserPage.PROFILES);
    }

    public void showPasswordManager(BrowserTab tab) {
        if (tab == null) return;
        if (passwordManagerAuthenticated) {
            showPasswordManagerPage(tab);
            return;
        }
        KeyguardManager keyguard =
                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        boolean secure = keyguard != null && keyguard.isKeyguardSecure();
        if (secure && Build.VERSION.SDK_INT >= 21) {
            try {
                Intent intent = keyguard.createConfirmDeviceCredentialIntent(
                        Localization.translate(this, "passwords.unlock_title"),
                        Localization.translate(this, "passwords.unlock_desc"));
                if (intent != null) {
                    pendingPasswordTab = tab;
                    awaitingPasswordAuthentication = true;
                    startActivityForResult(intent, PASSWORD_AUTH_REQUEST);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        if (secure && Build.VERSION.SDK_INT < 21) {
            android.widget.Toast.makeText(this,
                    Localization.translate(this, "passwords.api_limit"),
                    android.widget.Toast.LENGTH_LONG).show();
        }
        passwordManagerAuthenticated = true;
        showPasswordManagerPage(tab);
    }

    private void showPasswordManagerPage(BrowserTab tab) {
        passwordsPage.show(tab);
        tabManager.selectTab(tab);
        recordInternalVisit(tab, BrowserPage.PASSWORDS);
    }

    public void startPasswordExport(
            BrowserTab tab) {

        final EditText password =
                new EditText(this);

        password.setSingleLine(true);
        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setHint(
                Localization.translate(
                        this,
                        "passwords.export_password"));

        new AlertDialog.Builder(this)
                .setTitle(
                        Localization.translate(
                                this,
                                "passwords.export_title"))
                .setView(password)
                .setNegativeButton(
                        Localization.translate(
                                this,
                                "common.cancel"),
                        null)
                .setPositiveButton(
                        Localization.translate(
                                this,
                                "passwords.export"),
                        (dialog, which) -> {
                            try {
                                pendingPasswordExport =
                                        passwordStore
                                                .exportEncrypted(
                                                        password.getText()
                                                                .toString());

                                Intent intent =
                                        new Intent(
                                                Intent.ACTION_CREATE_DOCUMENT);

                                intent.addCategory(
                                        Intent.CATEGORY_OPENABLE);
                                intent.setType(
                                        "application/octet-stream");
                                intent.putExtra(
                                        Intent.EXTRA_TITLE,
                                        "simplebrowser-passwords.sbpass");

                                pendingPasswordFileTab = tab;

                                startActivityForResult(
                                        intent,
                                        PASSWORD_EXPORT_REQUEST);

                            } catch (Exception exception) {
                                showPasswordManagerError(
                                        Localization.translate(
                                                this,
                                                "passwords.export_failed"));
                            }
                        })
                .show();
    }

    public void startPasswordImport(
            BrowserTab tab) {

        pendingPasswordFileTab = tab;

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE);

        intent.setType(
                "application/octet-stream");

        try {
            startActivityForResult(
                    intent,
                    PASSWORD_IMPORT_REQUEST);
        } catch (Throwable exception) {
            showPasswordManagerError(
                    Localization.translate(
                            this,
                            "passwords.import_failed"));
        }
    }

    private void promptForPasswordImport(
            Uri uri) {

        final EditText password =
                new EditText(this);

        password.setSingleLine(true);
        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setHint(
                Localization.translate(
                        this,
                        "passwords.export_password"));

        new AlertDialog.Builder(this)
                .setTitle(
                        Localization.translate(
                                this,
                                "passwords.import_title"))
                .setView(password)
                .setNegativeButton(
                        Localization.translate(
                                this,
                                "common.cancel"),
                        null)
                .setPositiveButton(
                        Localization.translate(
                                this,
                                "passwords.import"),
                        (dialog, which) -> {

                            InputStream input = null;

                            try {
                                input =
                                        getContentResolver()
                                                .openInputStream(
                                                        uri);

                                StringBuilder data =
                                        new StringBuilder();

                                byte[] buffer =
                                        new byte[8192];

                                int count;

                                while ((count =
                                        input.read(buffer)) != -1) {

                                    data.append(
                                            new String(
                                                    buffer,
                                                    0,
                                                    count,
                                                    "UTF-8"));
                                }

                                int imported =
                                        passwordStore
                                                .importEncrypted(
                                                        data.toString(),
                                                        password.getText()
                                                                .toString());

                                BrowserTab tab =
                                        pendingPasswordFileTab;

                                if (tab != null) {
                                    passwordsPage.show(tab, "");
                                    tabManager.selectTab(tab);
                                }

                                android.widget.Toast.makeText(
                                        this,
                                        Localization.translate(
                                                this,
                                                "passwords.imported") +
                                                " " +
                                                imported,
                                        android.widget.Toast.LENGTH_SHORT)
                                        .show();

                            } catch (Exception exception) {

                                showPasswordManagerError(
                                        Localization.translate(
                                                this,
                                                "passwords.import_failed"));

                            } finally {

                                if (input != null) {
                                    try {
                                        input.close();
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        })
                .show();
    }

    public void promptToSavePassword(
            BrowserTab tab,
            String site,
            String username,
            String password) {

        if (tab == null ||
                tab.isIncognito ||
                password == null ||
                password.isEmpty() ||
                passwordStore == null) {
            return;
        }

        final String cleanSite =
                site == null || site.trim().isEmpty()
                        ? tab.webView.getUrl()
                        : site.trim();

        final String cleanUsername =
                username == null
                        ? ""
                        : username;

        try {
            if (passwordStore.containsCredentials(
                    cleanSite,
                    cleanUsername,
                    password)) {
                return;
            }
        } catch (Exception ignored) {
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        Localization.translate(
                                this,
                                "passwords.save_prompt_title"))
                .setMessage(
                        Localization.translate(
                                this,
                                "passwords.save_prompt"))
                .setNegativeButton(
                        Localization.translate(
                                this,
                                "passwords.not_now"),
                        null)
                .setPositiveButton(
                        Localization.translate(
                                this,
                                "passwords.save_password"),
                        (dialog, which) -> {

                            try {
                                String favicon =
                                        tab.favicon == null
                                                ? ""
                                                : android.util.Base64
                                                        .encodeToString(
                                                                bitmapToPng(
                                                                        tab.favicon),
                                                                android.util.Base64.NO_WRAP);

                                passwordStore.save(
                                        -1,
                                        cleanSite,
                                        cleanUsername,
                                        password,
                                        "",
                                        favicon);

                            } catch (Exception exception) {
                                showPasswordManagerError(
                                        Localization.translate(
                                                this,
                                                "passwords.save_failed"));
                            }
                        })
                .show();
    }

    private byte[] bitmapToPng(
            Bitmap bitmap)
            throws Exception {

        java.io.ByteArrayOutputStream output =
                new java.io.ByteArrayOutputStream();

        bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                output);

        return output.toByteArray();
    }

    public void showPasswordManagerError(String message) {
        android.widget.Toast.makeText(this, message,
                android.widget.Toast.LENGTH_LONG).show();
    }

    public void switchProfile(String profileId) {
        if (profileId == null) return;
        performProfileSwitch(profileId);
    }

    public void enterGuestProfile(BrowserTab tab) {
        performProfileSwitch(ProfileManager.createGuestSession());
    }

    private void performProfileSwitch(String targetProfileId) {
        String current = ProfileManager.getActiveProfileId(this);
        if (targetProfileId == null || current.equals(targetProfileId)) return;
        if (!ProfileManager.isGuest(targetProfileId) &&
                profileManager.getProfile(this, targetProfileId) == null) return;

        profileSwitching = true;
        saveTabs();
        if (cookieStore != null) cookieStore.snapshotCookies();
        CookieStore.clearRuntimeCookies();

        if (browserHistory != null) browserHistory.close();
        if (downloadHistory != null) downloadHistory.close();
        if (bookmarkStore != null) bookmarkStore.close();
        if (passwordStore != null) passwordStore.close();

        if (ProfileManager.isGuest(current)) {
            ProfileManager.deleteProfileData(this, current);
        }

        ProfileManager.setActiveProfileId(this, targetProfileId);
        recreate();
    }

    public void showProfileEditor(
            BrowserTab tab,
            String profileId) {

        ProfileManager.Profile profile =
                profileId == null
                        ? null
                        : profileManager.getProfile(
                                this,
                                profileId);

        if (profileId != null &&
                profile == null) {
            return;
        }

        final EditText name =
                new EditText(this);

        name.setSingleLine(true);
        name.setHint(
                Localization.translate(
                        this,
                        "profiles.name"));

        name.setText(
                profile == null
                        ? "Profile " +
                                Math.max(
                                        1,
                                        profileManager
                                                .getPersistentProfileCount(
                                                        this))
                        : profile.name);

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                profileId == null
                                        ? Localization.translate(
                                                this,
                                                "profiles.create")
                                        : Localization.translate(
                                                this,
                                                "profiles.edit"))
                        .setView(name)
                        .setNegativeButton(
                                Localization.translate(
                                        this,
                                        "common.cancel"),
                                null)
                        .setPositiveButton(
                                Localization.translate(
                                        this,
                                        "common.save"),
                                null)
                        .create();

        dialog.setOnShowListener(
                ignored -> {
                    Button save =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE);

                    save.setOnClickListener(
                            v -> {

                                String cleanName =
                                        name.getText()
                                                .toString()
                                                .trim();

                                if (cleanName.isEmpty()) {
                                    cleanName = "Profile";
                                }

                                boolean ok;

                                if (profileId == null) {
                                    ok =
                                            profileManager
                                                    .createProfile(
                                                            this,
                                                            cleanName)
                                                    != null;
                                } else {
                                    ok =
                                            profileManager
                                                    .updateProfile(
                                                            this,
                                                            profileId,
                                                            cleanName);
                                }

                                if (!ok) {
                                    showPasswordManagerError(
                                            Localization.translate(
                                                    this,
                                                    "profiles.cannot_save"));
                                    return;
                                }

                                dialog.dismiss();

                                if (tab != null &&
                                        tab.profilesPage) {
                                    profilesPage.show(tab);
                                    tabManager.selectTab(tab);
                                }
                            });
                });

        dialog.show();
    }

    public boolean handleProfileActionUrl(
            BrowserTab tab,
            String url) {

        if (url == null ||
                !url.startsWith(
                        "simplebrowser://profile/")) {
            return false;
        }

        String remainder =
                url.substring(
                        "simplebrowser://profile/"
                                .length());

        int slash =
                remainder.indexOf('/');

        String action =
                slash < 0
                        ? remainder
                        : remainder.substring(
                                0,
                                slash);

        String profileId =
                slash < 0
                        ? ""
                        : remainder.substring(
                                slash + 1);

        if ("create".equals(action)) {
            showProfileEditor(tab, null);
            return true;
        }

        if ("guest".equals(action)) {
            enterGuestProfile(tab);
            return true;
        }

        if ("switch".equals(action)) {
            switchProfile(profileId);
            return true;
        }

        if ("edit".equals(action)) {
            showProfileEditor(
                    tab,
                    profileId);
            return true;
        }

        if ("delete".equals(action)) {

            ProfileManager.Profile profile =
                    profileManager.getProfile(
                            this,
                            profileId);

            if (profile == null ||
                    profile.isMain()) {
                return true;
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            Localization.translate(
                                    this,
                                    "profiles.delete_confirm"))
                    .setMessage(profile.name)
                    .setNegativeButton(
                            Localization.translate(
                                    this,
                                    "common.cancel"),
                            null)
                    .setPositiveButton(
                            Localization.translate(
                                    this,
                                    "common.delete"),
                            (dialog, which) -> {

                                if (profileManager.deleteProfile(
                                        this,
                                        profileId) &&
                                        tab != null) {

                                    profilesPage.show(
                                            tab);
                                    tabManager.selectTab(
                                            tab);
                                }
                            })
                    .show();

            return true;
        }

        return true;
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

        if (BrowserPage.DEFAULT.equalsIgnoreCase(value)) {

            showDefaultPage(tab);
            return;
        }

        if (BrowserPage.HISTORY.equalsIgnoreCase(value)) {

            showHistory(tab, "");
            return;
        }

        if (BrowserPage.DOWNLOADS.equalsIgnoreCase(value)) {

            showDownloads(tab, "");
            return;
        }

        if (BrowserPage.COOKIES.equalsIgnoreCase(value)) {

            showCookies(tab);
            return;
        }

        if (BrowserPage.DEMO.equalsIgnoreCase(value)) {

            showDemoPage(tab);
            return;
        }

        if (BrowserPage.BOOKMARKS.equalsIgnoreCase(value)) {
            showBookmarks(tab, "");
            return;
        }

        if (BrowserPage.PASSWORDS.equalsIgnoreCase(value)) {
            showPasswordManager(tab);
            return;
        }

        if (BrowserPage.PROFILES.equalsIgnoreCase(value)) {
            showProfiles(tab);
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

        return BrowserPage.getSettingsSection(
                url);
    }

    public String getSettingsUrl(
            String section) {

        return BrowserPage.settingsUrl(
                section);
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

        tab.hasLoaded = true;
        tab.pendingUrl =
                url == null ||
                url.trim().isEmpty()
                        ? "about:blank"
                        : url;

        String settingsSection =
                getSettingsSection(url);

        if (settingsSection != null) {

            showSettings(
                    tab,
                    settingsSection);

            return;
        }

        if (BrowserPage.DEFAULT.equalsIgnoreCase(url)) {

            showDefaultPage(tab);
            return;
        }

        if (BrowserPage.HISTORY.equalsIgnoreCase(url)) {

            showHistory(tab, "");
            return;
        }

        if (BrowserPage.DOWNLOADS.equalsIgnoreCase(url)) {

            showDownloads(tab, "");
            return;
        }

        if (BrowserPage.COOKIES.equalsIgnoreCase(url)) {

            showCookies(tab);
            return;
        }

        if (BrowserPage.DEMO.equalsIgnoreCase(url)) {

            showDemoPage(tab);
            return;
        }

        if (BrowserPage.BOOKMARKS.equalsIgnoreCase(url)) {
            showBookmarks(tab, "");
            return;
        }

        if (BrowserPage.PASSWORDS.equalsIgnoreCase(url)) {
            showPasswordManager(tab);
            return;
        }

        if (BrowserPage.PROFILES.equalsIgnoreCase(url)) {
            showProfiles(tab);
            return;
        }

        removeInternalPageState(tab);

        tab.loading = true;
        tab.sslError = false;
        tab.favicon = null;
        tabManager.updateTabLoadingState(tab);

        tab.webView.loadUrl(url);
        updateBookmarkButton(tab);
    }

    public void removeInternalPageState(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        settingsPage.remove(tab);

        defaultPage.remove(tab);
        historyPage.remove(tab);
        downloadsPage.remove(tab);
        cookiesPage.remove(tab);
        bookmarksPage.remove(tab);
        passwordsPage.remove(tab);
        profilesPage.remove(tab);

        tab.settingsPage = false;
        tab.defaultPage = false;
        tab.historyPage = false;
        tab.downloadsPage = false;
        tab.cookiesPage = false;
        tab.bookmarksPage = false;
        tab.passwordsPage = false;
        tab.profilesPage = false;
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

        recordInternalVisit(
                tab,
                BrowserPage.DEFAULT);
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

        recordInternalVisit(
                tab,
                url);

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

        recordInternalVisit(
                tab,
                BrowserPage.HISTORY);
    }

    public void showDownloads(
            BrowserTab tab,
            String query) {

        if (tab == null) {
            return;
        }

        removeInternalPageState(tab);
        downloadsPage.show(
                tab,
                query);
        tabManager.selectTab(tab);

        recordInternalVisit(
                tab,
                BrowserPage.DOWNLOADS);
    }

    public void showCookies(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        removeInternalPageState(tab);
        cookiesPage.show(tab);
        tabManager.selectTab(tab);

        recordInternalVisit(
                tab,
                BrowserPage.COOKIES);
    }

    public void showDemoPage(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        removeInternalPageState(tab);
        demoPage.show(tab);
        tabManager.selectTab(tab);

        recordInternalVisit(
                tab,
                BrowserPage.DEMO);
    }

    public void refreshLocalizedChrome() {

        urlBox.setHint(
                Localization.translate(
                        this,
                        "Search or enter an address"));

        setupToolbarIcons();
        applyResponsiveToolbar();

        BrowserTab active =
                getActiveTab();

        if (active != null) {
            updateTabTitle(active);
            updateSecurity(active);
            updateReloadButton(active);
        }

        updateNavigationButtons();
    }

    public void refreshTab(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        if (tab.settingsPage) {
            settingsPage.show(
                    tab,
                    tab.settingsSection);
            return;
        }

        if (tab.historyPage) {
            historyPage.show(
                    tab,
                    "");
            return;
        }

        if (tab.downloadsPage) {
            downloadsPage.show(
                    tab,
                    "");
            return;
        }

        if (tab.cookiesPage) {
            cookiesPage.show(tab);
            return;
        }

        if (tab.bookmarksPage) {
            bookmarksPage.show(tab, "");
            return;
        }

        if (tab.passwordsPage) {
            if (passwordManagerAuthenticated) {
                passwordsPage.show(tab);
            } else {
                showPasswordManager(tab);
            }
            return;
        }

        if (tab.profilesPage) {
            profilesPage.show(tab);
            return;
        }

        if (BrowserPage.DEMO.equals(
                tab.url)) {
            demoPage.show(tab);
            return;
        }

        if (tab.errorPage) {
            String retry =
                    tab.url;

            if (retry != null &&
                    !retry.trim().isEmpty()) {
                loadTabUrl(tab, retry);
            }
            return;
        }

        if (tab.defaultPage) {
            defaultPage.show(tab);
            return;
        }

        tab.webView.reload();
    }

    public void clearBrowserHistory() {

        if (browserHistory != null) {
            browserHistory.clear();
        }
    }

    public void clearDownloadHistory() {

        if (downloadHistory != null) {
            downloadHistory.clear();
        }
    }

    public void clearCookieIndex() {

        if (cookieStore != null) {
            cookieStore.clearIndex();
        }
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public BrowserHistory getBrowserHistory() {
        return browserHistory;
    }

    public void startDownload(
            BrowserTab tab,
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType,
            long contentLength) {

        if (url == null ||
                url.trim().isEmpty()) {
            return;
        }

        try {

            String filename =
                    URLUtil.guessFileName(
                            url,
                            contentDisposition,
                            mimeType);

            if (filename == null ||
                    filename.trim().isEmpty()) {
                filename = "download";
            }

            android.app.DownloadManager.Request request =
                    new android.app.DownloadManager.Request(
                            Uri.parse(url));

            request.setTitle(filename);
            request.setDescription(
                    Localization.translate(
                            this,
                            "Simple Browser"));

            if (mimeType != null &&
                    !mimeType.trim().isEmpty()) {
                request.setMimeType(mimeType);
            }

            request.setNotificationVisibility(
                    android.app.DownloadManager
                            .Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            if (userAgent != null &&
                    !userAgent.trim().isEmpty()) {
                request.addRequestHeader(
                        "User-Agent",
                        userAgent);
            }

            String cookie =
                    CookieManager
                            .getInstance()
                            .getCookie(url);

            if (cookie != null &&
                    !cookie.trim().isEmpty()) {
                request.addRequestHeader(
                        "Cookie",
                        cookie);
            }

            android.app.DownloadManager manager =
                    (android.app.DownloadManager)
                            getSystemService(
                                    DOWNLOAD_SERVICE);

            if (manager == null) {
                throw new IllegalStateException(
                        "Download manager unavailable");
            }

            long downloadId =
                    manager.enqueue(request);

            if (tab == null ||
                    !tab.isIncognito) {

                downloadHistory.add(
                        downloadId,
                        url,
                        filename,
                        mimeType,
                        System.currentTimeMillis());
            }

            android.widget.Toast.makeText(
                    this,
                    Localization.translate(
                            this,
                            "Download started: ") + filename,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();

        } catch (Exception e) {

            android.widget.Toast.makeText(
                    this,
                    Localization.translate(
                            this,
                            "Unable to start download"),
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void recordVisit(
            BrowserTab tab,
            String url) {

        if (tab == null ||
                tab.isIncognito ||
                url == null ||
                url.trim().isEmpty()) {
            return;
        }

        android.graphics.Bitmap icon =
                tab.favicon != null
                        ? tab.favicon
                        : tab.webView == null
                        ? null
                        : tab.webView.getFavicon();

        browserHistory.addVisit(
                url,
                tab.title,
                icon);
    }

    private void recordInternalVisit(
            BrowserTab tab,
            String url) {

        if (tab == null ||
                tab.isIncognito ||
                url == null ||
                url.trim().isEmpty()) {
            return;
        }

        String lower =
                url.trim().toLowerCase();

        // browser:// pages are browser UI, not browsing history.
        if (lower.startsWith("browser://")) {
            return;
        }

        browserHistory.addVisit(
                url,
                tab.title,
                tab.favicon);
    }

    public void updateTabIcon(
            BrowserTab tab,
            android.graphics.Bitmap favicon) {

        if (tabManager != null) {
            tabManager.updateTabIcon(
                    tab,
                    favicon);
        }

        if (tab != null &&
                favicon != null &&
                !favicon.isRecycled() &&
                bookmarkStore != null &&
                tab.webView != null) {

            String url =
                    tab.webView.getUrl();

            if (url != null &&
                    (url.startsWith("http://") ||
                     url.startsWith("https://"))) {

                bookmarkStore.updateFavicon(
                        url,
                        favicon);
            }
        }

        updateBookmarkButton(tab);
    }

    public void setBrowserPageIcon(
            BrowserTab tab,
            int iconType) {

        if (tab == null ||
                tabManager == null) {
            return;
        }

        int chromeColor =
                tab.isIncognito
                        ? INCOGNITO_CHROME
                        : getAccentColor();

        int color =
                ColorUtils.getReadableTextColor(
                        chromeColor);

        android.graphics.Bitmap bitmap =
                android.graphics.Bitmap.createBitmap(
                        32,
                        32,
                        android.graphics.Bitmap.Config.ARGB_8888);

        android.graphics.Canvas canvas =
                new android.graphics.Canvas(bitmap);

        BrowserIconDrawable drawable =
                new BrowserIconDrawable(
                        iconType,
                        color);

        drawable.setBounds(
                0, 0, 32, 32);

        drawable.draw(canvas);

        tab.favicon = bitmap;

        tabManager.updateTabIcon(
                tab,
                bitmap);
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

        int tabBarColor =
                tab != null &&
                        tab.isIncognito
                        ? INCOGNITO_CHROME
                        : ColorUtils.darken(
                                chromeColor,
                                0.14f);

        toolbar.setBackgroundColor(
                chromeColor);

        View tabBar =
                findViewById(R.id.tab_bar);

        if (tabBar != null) {
            tabBar.setBackgroundColor(
                    tabBarColor);
        }

        View tabScroll =
                findViewById(R.id.tab_scroll);

        if (tabScroll != null) {
            tabScroll.setBackgroundColor(
                    tabBarColor);
        }

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

        updateBookmarkButton(
                tab);

        ImageButton tabOverview =
                findViewById(R.id.tab_overview);

        tabOverview.setImageDrawable(
                new TabCountDrawable(
                        readable,
                        tabManager == null
                                ? 0
                                : tabManager
                                        .getTabs()
                                        .size()));

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

        recordInternalVisit(
                tab,
                BrowserPage.settingsUrl(
                        section));

        if ("general".equals(tab.settingsSection) &&
                updateManager != null) {
            updateManager.checkForUpdates(
                    true,
                    tab);
        }
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

    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    public void promptForAutomaticUpdate(
            UpdateManager.UpdateInfo update) {

        if (update == null ||
                isFinishing()) {
            return;
        }

        final String version =
                update.version == null
                        ? ""
                        : update.version;

        String notes =
                update.notes == null
                        ? ""
                        : update.notes.trim();

        if (notes.length() > 1400) {
            notes =
                    notes.substring(0, 1400) +
                    "...";
        }

        StringBuilder message =
                new StringBuilder();

        message.append(
                Localization.translate(
                        this,
                        "settings.update_available"));
        message.append(": ");
        message.append(version);

        if (!notes.isEmpty()) {
            message.append("\n\n");
            message.append(notes);
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        Localization.translate(
                                this,
                                "settings.update_title"))
                .setMessage(
                        message.toString())
                .setNegativeButton(
                        Localization.translate(
                                this,
                                "settings.update_later"),
                        null)
                .setPositiveButton(
                        Localization.translate(
                                this,
                                "settings.update_now"),
                        (dialog, which) -> {

                            BrowserTab tab =
                                    getActiveTab();

                            updateManager
                                    .downloadAndInstall(
                                            update,
                                            tab,
                                            false);
                        })
                .show();
    }

    public void checkForUpdatesFromSettings(
            BrowserTab tab) {

        if (updateManager != null) {
            updateManager.checkForUpdates(
                    true,
                    tab);
        }
    }

    public void installAvailableUpdate(
            BrowserTab tab) {

        if (updateManager == null) {
            return;
        }

        UpdateManager.UpdateInfo update =
                updateManager.getAvailableUpdate();

        if (update != null) {
            updateManager.downloadAndInstall(
                    update,
                    tab,
                    false);
        } else {
            checkForUpdatesFromSettings(tab);
        }
    }

    public void onUpdateCheckFinished(
            BrowserTab tab,
            UpdateManager.UpdateInfo update,
            String error,
            boolean manual) {

        if (tab != null &&
                tab.settingsPage) {

            settingsPage.updateUpdateStatus(
                    tab,
                    update,
                    error);
        }

        if (!manual &&
                update != null &&
                error == null) {
            // UpdateManager handles the automatic download/install.
        }
    }

    public void onUpdateStatus(
            BrowserTab tab,
            String message,
            boolean showInstall) {

        if (tab != null &&
                tab.settingsPage) {

            settingsPage.updateUpdateStatusText(
                    tab,
                    message,
                    showInstall);
        }
    }

    public void onAutomaticUpdatesChanged(
            BrowserTab tab,
            boolean enabled) {

        Runnable updateUi =
                () -> {
                    if (tab != null &&
                            tab.settingsPage) {
                        settingsPage.updateAutomaticUpdatesUi(
                                tab,
                                enabled);
                    }
                };

        if (Looper.myLooper() ==
                Looper.getMainLooper()) {
            updateUi.run();
        } else {
            runOnUiThread(updateUi);
        }
    }

    public void removeSettingsBridge(
            BrowserTab tab) {

        settingsPage.remove(tab);
    }

    public void restoreSettingsPage(
            BrowserTab tab,
            String section) {

        settingsPage.show(
                tab,
                section);

        if ("general".equals(
                tab.settingsSection) &&
                updateManager != null) {
            updateManager.checkForUpdates(
                    true,
                    tab);
        }
    }

    public void pageStarted(
            BrowserTab tab,
            String url) {

        if (tab == getActiveTab()) {

            if (tab.settingsPage) {

                setUrlText(
                        getSettingsUrl(
                                tab.settingsSection));

            } else if (tab.defaultPage) {

                setUrlText("");

            } else if (tab.historyPage) {

                setUrlText(
                        BrowserPage.HISTORY);

            } else if (tab.downloadsPage) {

                setUrlText(
                        BrowserPage.DOWNLOADS);

            } else if (BrowserPage.DEMO.equals(
                    tab.url)) {

                setUrlText(
                        BrowserPage.DEMO);

            } else if (tab.bookmarksPage) {

                setUrlText(BrowserPage.BOOKMARKS);

            } else if (tab.passwordsPage) {

                setUrlText(BrowserPage.PASSWORDS);

            } else if (tab.profilesPage) {

                setUrlText(BrowserPage.PROFILES);

            } else if (tab.errorPage) {

                setUrlText(tab.url);

            } else {

                setUrlText(url);
            }

            progressBar.setProgress(0);
            progressBar.setVisibility(
                    View.VISIBLE);
        }

        tabManager.updateTabLoadingState(tab);
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

            } else if (tab.defaultPage) {

                setUrlText("");

            } else if (tab.historyPage) {

                setUrlText(
                        BrowserPage.HISTORY);

            } else if (tab.downloadsPage) {

                setUrlText(
                        BrowserPage.DOWNLOADS);

            } else if (BrowserPage.DEMO.equals(
                    tab.url)) {

                setUrlText(
                        BrowserPage.DEMO);

            } else if (tab.bookmarksPage) {

                setUrlText(BrowserPage.BOOKMARKS);

            } else if (tab.passwordsPage) {

                setUrlText(BrowserPage.PASSWORDS);

            } else if (tab.profilesPage) {

                setUrlText(BrowserPage.PROFILES);

            } else if (tab.errorPage) {

                setUrlText(tab.url);

            } else {

                setUrlText(url);
            }

            progressBar.setProgress(100);
        }

        tabManager.updateTabLoadingState(tab);
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

    public void goBackFromInternalPage(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        hideKeyboard();

        if (tab.errorPage) {

            int delta =
                    findErrorNavigationDelta(
                            tab,
                            -1);

            if (delta != 0) {
                tab.errorPage = false;
                tab.webView.goBackOrForward(delta);
                updateNavigationButtons();
                return;
            }

            showDefaultPage(tab);
            return;
        }

        if (tab.webView.canGoBack()) {
            tab.webView.goBack();
            return;
        }

        showDefaultPage(tab);
    }

    public void goForwardFromInternalPage(
            BrowserTab tab) {

        if (tab == null ||
                !tab.errorPage) {
            return;
        }

        hideKeyboard();

        int delta =
                findErrorNavigationDelta(
                        tab,
                        1);

        if (delta != 0) {
            tab.errorPage = false;
            tab.webView.goBackOrForward(delta);
        }

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
                (tab.errorPage
                        ? canNavigateFromErrorPage(
                                tab,
                                -1)
                        : tab.webView.canGoBack());

        boolean forwardEnabled =
                tab != null &&
                (tab.errorPage
                        ? canNavigateFromErrorPage(
                                tab,
                                1)
                        : tab.webView.canGoForward());

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

    private boolean canNavigateFromErrorPage(
            BrowserTab tab,
            int direction) {

        return findErrorNavigationDelta(
                tab,
                direction) != 0;
    }

    private int findErrorNavigationDelta(
            BrowserTab tab,
            int direction) {

        if (tab == null ||
                tab.webView == null ||
                (direction != -1 &&
                 direction != 1)) {

            return 0;
        }

        android.webkit.WebBackForwardList history =
                tab.webView.copyBackForwardList();

        int current =
                history.getCurrentIndex();

        if (current < 0) {
            return 0;
        }

        String failingUrl =
                tab.url == null
                        ? ""
                        : tab.url;

        for (int index =
                current + direction;
                index >= 0 &&
                index < history.getSize();
                index += direction) {

            android.webkit.WebHistoryItem item =
                    history.getItemAtIndex(index);

            if (item == null ||
                    item.getUrl() == null) {
                continue;
            }

            String candidate =
                    item.getUrl();

            if (BrowserPage.isInternalUrl(candidate)) {
                continue;
            }

            if (!failingUrl.isEmpty() &&
                    failingUrl.equals(candidate)) {
                continue;
            }

            return index - current;
        }

        return 0;
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

    private void updateBookmarkButton(
            BrowserTab tab) {

        ImageButton button =
                findViewById(R.id.bookmark);

        if (button == null) {
            return;
        }

        int color =
                tab != null &&
                        tab.isIncognito
                        ? ColorUtils.getReadableTextColor(
                                INCOGNITO_CHROME)
                        : ColorUtils.getReadableTextColor(
                                getAccentColor());

        boolean bookmarked =
                tab != null &&
                        !tab.isIncognito &&
                        bookmarkStore != null &&
                        tab.webView != null &&
                        tab.webView.getUrl() != null &&
                        bookmarkStore.isBookmarked(
                                tab.webView.getUrl());

        button.setImageDrawable(
                new BrowserIconDrawable(
                        BrowserIconDrawable.BOOKMARK,
                        color,
                        bookmarked));

        button.setEnabled(
                tab != null &&
                        !tab.isIncognito &&
                        tab.webView != null &&
                        tab.webView.getUrl() != null);

        button.setAlpha(
                tab != null &&
                        !tab.isIncognito
                        ? 1f
                        : 0.35f);
    }

    private void updateReloadButton(
            BrowserTab tab) {

        if (tab == null ||
                tab != getActiveTab()) {
            return;
        }

        ImageButton reload =
                findViewById(R.id.reload);

        int chromeColor =
                tab.isIncognito
                        ? INCOGNITO_CHROME
                        : getAccentColor();

        int iconColor =
                ColorUtils
                        .getReadableTextColor(
                                chromeColor);

        reload.setImageDrawable(
                new BrowserIconDrawable(
                        tab.loading
                                ? BrowserIconDrawable.STOP
                                : BrowserIconDrawable.RELOAD,
                        iconColor));

        reload.setContentDescription(
                Localization.translate(
                        this,
                        tab.loading
                                ? "Stop loading"
                                : "Reload"));
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

    private void applyResponsiveToolbar() {

        int width =
                getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        float widthDp =
                width / density;

        boolean compactPhoneLayout =
                widthDp < 500f;

        int buttonSize;

        if (compactPhoneLayout) {
            buttonSize =
                    widthDp < 360
                            ? 34
                            : 40;
        } else {
            buttonSize =
                    widthDp < 320
                            ? 34
                            : widthDp < 360
                            ? 36
                            : widthDp < 420
                            ? 40
                            : 44;
        }

        int buttonHeight =
                compactPhoneLayout
                        ? 42
                        : widthDp < 360
                        ? 40
                        : 44;

        int padding =
                widthDp < 320
                        ? 6
                        : widthDp < 360
                        ? 7
                        : 8;

        int[] toolbarButtons = {
                R.id.back,
                R.id.forward,
                R.id.home,
                R.id.reload,
                R.id.security,
                R.id.bookmark,
                R.id.settings
        };

        for (int id : toolbarButtons) {

            ImageButton button =
                    findViewById(id);

            if (button == null) {
                continue;
            }

            button.setMinimumWidth(0);
            button.setMinimumHeight(0);
            button.setPadding(
                    dp(padding),
                    dp(padding),
                    dp(padding),
                    dp(padding));

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams)
                            button.getLayoutParams();

            params.width = dp(buttonSize);
            params.height = dp(buttonHeight);
            params.weight = 0;
            button.setLayoutParams(params);
        }

        ImageButton newTab =
                findViewById(R.id.new_tab);

        ImageButton tabOverview =
                findViewById(R.id.tab_overview);

        View toolbarSpacer =
                findViewById(R.id.toolbar_spacer);

        int tabButtonSize =
                widthDp < 360
                        ? 34
                        : 36;

        for (ImageButton button :
                new ImageButton[] {
                        newTab,
                        tabOverview
                }) {

            if (button == null) {
                continue;
            }

            button.setMinimumWidth(0);
            button.setMinimumHeight(0);

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams)
                            button.getLayoutParams();

            params.width = dp(
                    compactPhoneLayout
                            ? buttonSize
                            : tabButtonSize);
            params.height = dp(
                    compactPhoneLayout
                            ? buttonHeight
                            : tabButtonSize);
            params.weight = 0;
            button.setLayoutParams(params);
        }

        LinearLayout urlRow =
                findViewById(R.id.url_row);

        LinearLayout tabBar =
                findViewById(R.id.tab_bar);

        if (compactPhoneLayout) {

            if (toolbarSpacer != null) {
                toolbarSpacer.setVisibility(View.VISIBLE);

                LinearLayout.LayoutParams spacerParams =
                        (LinearLayout.LayoutParams)
                                toolbarSpacer.getLayoutParams();

                spacerParams.width = 0;
                spacerParams.height = -1;
                spacerParams.weight = 1f;

                toolbarSpacer.setLayoutParams(
                        spacerParams);
            }

            int spacerIndex =
                    toolbarSpacer == null
                            ? toolbar.indexOfChild(
                                    findViewById(R.id.settings))
                            : toolbar.indexOfChild(
                                    toolbarSpacer);

            if (newTab != null &&
                    newTab.getParent() != toolbar) {

                if (newTab.getParent()
                        instanceof android.view.ViewGroup) {

                    ((android.view.ViewGroup)
                            newTab.getParent())
                            .removeView(newTab);
                }

                toolbar.addView(
                        newTab,
                        Math.min(
                                toolbar.getChildCount(),
                                spacerIndex + 1));
            }

            if (tabOverview != null &&
                    tabOverview.getParent() != toolbar) {

                if (tabOverview.getParent()
                        instanceof android.view.ViewGroup) {

                    ((android.view.ViewGroup)
                            tabOverview.getParent())
                            .removeView(tabOverview);
                }

                toolbar.addView(
                        tabOverview,
                        Math.min(
                                toolbar.getChildCount(),
                                spacerIndex + 2));
            }

            if (urlBox.getParent() != urlRow) {

                if (urlBox.getParent()
                        instanceof android.view.ViewGroup) {

                    ((android.view.ViewGroup)
                            urlBox.getParent())
                            .removeView(urlBox);
                }

                urlBox.setMinimumWidth(0);
                urlBox.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                -1,
                                dp(42),
                                0f));

                urlRow.addView(urlBox);
            }

            urlRow.setVisibility(View.VISIBLE);
            tabBar.setVisibility(View.GONE);

        } else {

            if (newTab != null &&
                    newTab.getParent() != tabBar) {

                if (newTab.getParent()
                        instanceof android.view.ViewGroup) {

                    ((android.view.ViewGroup)
                            newTab.getParent())
                            .removeView(newTab);
                }

                tabBar.addView(newTab);
            } else if (newTab != null) {

                tabBar.removeView(newTab);
                tabBar.addView(newTab);
            }

            if (tabOverview != null &&
                    tabOverview.getParent() != tabBar) {

                if (tabOverview.getParent()
                        instanceof android.view.ViewGroup) {

                    ((android.view.ViewGroup)
                            tabOverview.getParent())
                            .removeView(tabOverview);
                }

                tabBar.addView(tabOverview);
            } else if (tabOverview != null) {

                tabBar.removeView(tabOverview);
                tabBar.addView(tabOverview);
            }

            if (urlBox.getParent() != toolbar) {

                if (urlBox.getParent()
                        instanceof android.view.ViewGroup) {

                    ((android.view.ViewGroup)
                            urlBox.getParent())
                            .removeView(urlBox);
                }

                urlBox.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                0,
                                dp(42),
                                1f));

                int settingsIndex =
                        toolbar.indexOfChild(
                                findViewById(R.id.settings));

                toolbar.addView(
                        urlBox,
                        Math.max(0, settingsIndex));
            }

            if (toolbarSpacer != null) {
                toolbarSpacer.setVisibility(View.GONE);

                LinearLayout.LayoutParams spacerParams =
                        (LinearLayout.LayoutParams)
                                toolbarSpacer.getLayoutParams();

                spacerParams.width = 0;
                spacerParams.height = -1;
                spacerParams.weight = 0f;

                toolbarSpacer.setLayoutParams(
                        spacerParams);
            }

            urlRow.setVisibility(View.GONE);
            tabBar.setVisibility(View.VISIBLE);
        }

        urlBox.setMinimumWidth(
                dp(compactPhoneLayout
                        ? 0
                        : widthDp < 320
                        ? 72
                        : 80));

        int urlPadding =
                widthDp < 360
                        ? 6
                        : 9;

        urlBox.setPadding(
                dp(urlPadding),
                0,
                dp(urlPadding),
                0);
    }

    private void applySystemBarInsets() {

        if (Build.VERSION.SDK_INT < 30) {
            return;
        }

        getWindow()
                .setDecorFitsSystemWindows(false);

        View root =
                findViewById(R.id.root);

        if (root == null) {
            return;
        }

        root.setOnApplyWindowInsetsListener(
                (view, insets) -> {

                    android.graphics.Insets bars =
                            insets.getInsets(
                                    android.view.WindowInsets
                                            .Type.systemBars());

                    view.setPadding(
                            view.getPaddingLeft(),
                            bars.top,
                            view.getPaddingRight(),
                            bars.bottom);

                    return insets;
                });

        root.requestApplyInsets();
    }

    public void applyBrowserAppearance() {

        applyActiveTabAppearance();
    }

    public void updateTabOverviewIcon() {

        ImageButton tabOverview =
                findViewById(R.id.tab_overview);

        if (tabOverview == null) {
            return;
        }

        BrowserTab active =
                getActiveTab();

        int color =
                ColorUtils.getReadableTextColor(
                        active != null &&
                        active.isIncognito
                                ? INCOGNITO_CHROME
                                : getAccentColor());

        int count =
                tabManager == null
                        ? 0
                        : tabManager.getTabs().size();

        tabOverview.setImageDrawable(
                new TabCountDrawable(
                        color,
                        count));
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
            tabManager.updateTabLoadingState(tab);
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
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data);

        if (requestCode ==
                PASSWORD_AUTH_REQUEST) {

            awaitingPasswordAuthentication = false;

            if (resultCode ==
                    RESULT_OK) {

                passwordManagerAuthenticated = true;

                if (pendingPasswordTab != null) {
                    showPasswordManagerPage(
                            pendingPasswordTab);
                }
            }

            pendingPasswordTab = null;
            return;
        }

        if (requestCode ==
                PASSWORD_EXPORT_REQUEST) {

            Uri uri =
                    data == null
                            ? null
                            : data.getData();

            if (resultCode == RESULT_OK &&
                    uri != null &&
                    pendingPasswordExport != null) {

                OutputStream output = null;

                try {
                    output =
                            getContentResolver()
                                    .openOutputStream(uri);

                    if (output == null) {
                        throw new Exception("output");
                    }

                    output.write(
                            pendingPasswordExport
                                    .getBytes("UTF-8"));

                    output.flush();

                    android.widget.Toast.makeText(
                            this,
                            Localization.translate(
                                    this,
                                    "passwords.exported"),
                            android.widget.Toast.LENGTH_SHORT)
                            .show();

                } catch (Exception exception) {
                    showPasswordManagerError(
                            Localization.translate(
                                    this,
                                    "passwords.export_failed"));
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            pendingPasswordExport = null;
            pendingPasswordFileTab = null;
            return;
        }

        if (requestCode ==
                PASSWORD_IMPORT_REQUEST) {

            Uri uri =
                    data == null
                            ? null
                            : data.getData();

            if (resultCode == RESULT_OK &&
                    uri != null) {

                promptForPasswordImport(uri);
            } else {
                pendingPasswordFileTab = null;
            }

            return;
        }

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

    @Override
    protected void onResume() {

        super.onResume();

        if (cookieStore != null) {
            cookieStore.startSync();
        }

        if (updateManager != null) {
            updateManager.resumePendingInstall();
        }
    }

    @Override
    protected void onPause() {

        if (!profileSwitching) {
            saveTabs();
        }

        if (!awaitingPasswordAuthentication) {
            passwordManagerAuthenticated = false;
        }

        if (cookieStore != null) {
            cookieStore.snapshotCookies();
            cookieStore.stopSync();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (!profileSwitching) {
            saveTabs();
        }

        if (browserHistory != null) {
            browserHistory.close();
        }

        if (downloadHistory != null) {
            downloadHistory.close();
        }

        if (bookmarkStore != null) {
            bookmarkStore.close();
        }

        if (passwordStore != null) {
            passwordStore.close();
        }

        if (!profileSwitching &&
                ProfileManager.isGuest(
                        ProfileManager.getActiveProfileId(this))) {
            ProfileManager.deleteProfileData(
                    this,
                    ProfileManager.getActiveProfileId(this));
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
                (tab.settingsPage ||
                 tab.defaultPage ||
                 tab.historyPage ||
                 tab.downloadsPage ||
                 tab.cookiesPage ||
                 tab.bookmarksPage ||
                 tab.passwordsPage ||
                 tab.profilesPage ||
                 BrowserPage.DEMO.equals(
                         tab.url) ||
                 tab.errorPage)) {

            goBackFromInternalPage(tab);
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
