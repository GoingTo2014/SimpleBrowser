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

    private static final String PROFILE_PROCESS_PROFILE_ID_EXTRA =
            "simplebrowser.profile_id";

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
    private WebStorageStore webStorageStore;
    private CookiesPage cookiesPage;
    private DemoPage demoPage;
    private UpdateManager updateManager;
    private SimpleAccountManager simpleAccountManager;

    private static final int PASSWORD_AUTH_REQUEST = 2001;

    private boolean passwordManagerAuthenticated;
    private boolean awaitingPasswordAuthentication;
    private boolean profileSwitching;
    private BrowserTab pendingPasswordTab;
    private String pendingPasswordSite;
    private long pendingPasswordRevealId = -1L;
    private BrowserTab pendingPasswordFileTab;
    private String pendingPasswordExport;
    private static final int PASSWORD_EXPORT_REQUEST = 3101;
    private static final int PASSWORD_IMPORT_REQUEST = 3102;
    private static final int WEB_FILE_CHOOSER_REQUEST = 4201;

    private android.webkit.ValueCallback<Uri[]> pendingFileChooser;
    private android.webkit.ValueCallback<Uri> pendingLegacyFileChooser;

    private static final int INCOGNITO_CHROME =
            Color.rgb(32, 33, 36);

    private static final int INCOGNITO_URL =
            Color.rgb(48, 49, 52);

    private GeolocationPermissions.Callback
            pendingGeolocationCallback;

    private String pendingGeolocationOrigin;

    private static final int LOCATION_PERMISSION_REQUEST =
            1501;

    private static final int MEDIA_PERMISSION_REQUEST =
            1502;

    private android.webkit.PermissionRequest pendingMediaPermissionRequest;
    private String[] pendingMediaPermissionResources;

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        profileManager =
                new ProfileManager(this);

        simpleAccountManager =
                new SimpleAccountManager(this);

        /*
         * Persistent non-main profiles have dedicated Android processes on
         * Android 9+. Redirect before touching any WebView API so the default
         * process is permanently reserved for Main's WebView data directory.
         */
        if (Build.VERSION.SDK_INT >= 28 &&
                getClass().equals(MainActivity.class) &&
                redirectToActiveProfileProcess()) {
            return;
        }

        /*
         * Select the correct Chromium data directory before any WebView API
         * is initialized. On API 19-27 this also recovers an interrupted
         * legacy profile-directory swap.
         */
        ProfileSwitchService.recoverIfNeeded(this);
        ProfileWebViewStorage.configureForProcess(this);

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

        webStorageStore =
                new WebStorageStore(this);

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

        cookieStore.restoreCookies(
                new Runnable() {
                    @Override
                    public void run() {
                        finishStartup();
                    }
                });
    }

    private boolean redirectToActiveProfileProcess() {

        String activeProfileId =
                ProfileManager.getActiveProfileId(this);

        if (ProfileManager.MAIN_ID.equals(
                activeProfileId) ||
                ProfileManager.isGuest(
                        activeProfileId)) {
            return false;
        }

        try {
            profileSwitching = true;

            Intent forward =
                    getIntent() == null
                            ? new Intent()
                            : new Intent(getIntent());

            forward.setClass(
                    this,
                    ProfileProcessActivity.class);

            forward.putExtra(
                    PROFILE_PROCESS_PROFILE_ID_EXTRA,
                    activeProfileId);

            forward.addFlags(
                    Intent.FLAG_ACTIVITY_NO_ANIMATION);

            startActivity(forward);
            overridePendingTransition(0, 0);
            finish();
            return true;

        } catch (Throwable ignored) {
            profileSwitching = false;
            return false;
        }
    }

    private void finishStartup() {

        if (isFinishing()) {
            return;
        }

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
                    tab.isGuest ||
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

        boolean guest =
                active != null &&
                        active.isGuest;

        int accent =
                guest
                        ? Color.WHITE
                        : incognito
                        ? Color.rgb(
                                48, 49, 52)
                        : getAccentColor();

        int readable =
                ColorUtils.getReadableTextColor(
                        accent);

        int menuBackground =
                guest
                        ? Color.rgb(
                                250, 250, 250)
                        : ColorUtils.darken(
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

        addMenuActionButton(
                menu,
                "Settings",
                () -> {
                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    BrowserTab tab =
                            getActiveTab();

                    if (tab != null) {
                        showSettings(
                                tab,
                                "general");
                    }
                },
                accent,
                readable);

        addMenuActionButton(
                menu,
                "account.title",
                () -> {
                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    showSimpleAccount();
                },
                accent,
                readable);

        addMenuActionButton(
                menu,
                "Bookmarks",
                () -> {
                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    BrowserTab tab =
                            getActiveTab();

                    if (tab != null) {
                        showBookmarks(tab, "");
                    }
                },
                accent,
                readable);

        addMenuActionButton(
                menu,
                "Password manager",
                () -> {
                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    BrowserTab tab =
                            getActiveTab();

                    if (tab != null) {
                        showPasswordManager(tab);
                    }
                },
                accent,
                readable);

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
                guest
                        ? "guest.exit"
                        : incognito
                        ? "menu.exit_incognito"
                        : "menu.enter_incognito",
                () -> {
                    if (browserMenu != null) {
                        browserMenu.dismiss();
                    }

                    if (guest) {
                        exitGuestProfile();
                    } else if (incognito) {
                        tabManager.exitIncognitoMode();
                    } else {
                        tabManager.enterIncognitoMode(
                                browserSettings.getHomePage());
                    }
                },
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

    private void syncSimpleAccount(
            boolean showResult) {

        SimpleSyncManager.syncAsync(
                this,
                showResult
                        ? new SimpleSyncManager.Callback() {
                            @Override
                            public void onComplete(
                                    boolean success,
                                    String message) {

                                android.widget.Toast
                                        .makeText(
                                                MainActivity.this,
                                                success
                                                        ? Localization.translate(
                                                                MainActivity.this,
                                                                "account.sync_complete") +
                                                                "\n" +
                                                                message
                                                        : Localization.translate(
                                                                MainActivity.this,
                                                                "account.sync_failed"),
                                                android.widget.Toast
                                                        .LENGTH_LONG)
                                        .show();
                            }
                        }
                        : null);
    }

    private void showSimpleAccount() {

        if (simpleAccountManager == null) {
            simpleAccountManager =
                    new SimpleAccountManager(this);
        }

        if (simpleAccountManager.isSignedIn()) {

            String email =
                    simpleAccountManager.getEmail();

            AlertDialog dialog =
                    new AlertDialog.Builder(this)
                            .setTitle(
                                    Localization.translate(
                                            this,
                                            "account.title"))
                            .setMessage(
                                    Localization.translate(
                                            this,
                                            "account.signed_in_as") +
                                    "\n\n" +
                                    (email == null ||
                                    email.trim().isEmpty()
                                            ? Localization.translate(
                                                    this,
                                                    "account.signed_in")
                                            : email))
                            .setPositiveButton(
                                    Localization.translate(
                                            this,
                                            "account.sign_out"),
                                    (d, which) -> {
                                        simpleAccountManager.signOut();

                                        android.widget.Toast
                                                .makeText(
                                                        this,
                                                        Localization.translate(
                                                                this,
                                                                "account.signed_out"),
                                                        android.widget.Toast
                                                                .LENGTH_SHORT)
                                                .show();
                                    })
                            .setNeutralButton(
                                    Localization.translate(
                                            this,
                                            "account.sync_now"),
                                    (d, which) -> {
                                        syncSimpleAccount(true);
                                    })
                            .setNegativeButton(
                                    Localization.translate(
                                            this,
                                            "common.close"),
                                    null)
                            .create();

            dialog.show();
            return;
        }

        try {
            Intent intent =
                    new Intent(
                            this,
                            SimpleAccountActivity.class);

            startActivityForResult(
                    intent,
                    5101);

        } catch (Throwable error) {

            android.widget.Toast
                    .makeText(
                            this,
                            Localization.translate(
                                    this,
                                    "account.open_failed"),
                            android.widget.Toast
                                    .LENGTH_LONG)
                    .show();
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

        if (requestCode == 5101) {
            if (resultCode == RESULT_OK) {
                if (simpleAccountManager == null) {
                    simpleAccountManager =
                            new SimpleAccountManager(this);
                }

                if (simpleAccountManager.isSignedIn()) {
                    syncSimpleAccount(true);
                }
            }

            return;
        }

        if (requestCode ==
                WEB_FILE_CHOOSER_REQUEST) {

            handleWebFileChooserResult(
                    resultCode,
                    data);
            return;
        }

        if (requestCode ==
                PASSWORD_AUTH_REQUEST) {

            awaitingPasswordAuthentication = false;

            if (resultCode ==
                    RESULT_OK) {

                passwordManagerAuthenticated = true;

                if (pendingPasswordTab != null) {
                    BrowserTab passwordTab =
                            pendingPasswordTab;

                    if (pendingPasswordRevealId > 0L) {
                        long revealId =
                                pendingPasswordRevealId;
                        pendingPasswordRevealId = -1L;

                        showPasswordEntry(
                                passwordTab,
                                revealId);

                    } else if (pendingPasswordSite != null &&
                            !pendingPasswordSite.trim().isEmpty()) {

                        String site =
                                pendingPasswordSite;
                        pendingPasswordSite = null;

                        showPasswordSite(
                                passwordTab,
                                site);

                    } else {
                        showPasswordManagerPage(
                                passwordTab);
                    }
                }
            }

            pendingPasswordTab = null;
            pendingPasswordSite = null;
            pendingPasswordRevealId = -1L;
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

        if (cookieStore != null &&
                (tabManager == null ||
                 !tabManager.isGuestMode())) {
            cookieStore.startSync();
        }

        if (updateManager != null) {
            updateManager.resumePendingInstall();
        }

        if (simpleAccountManager == null) {
            simpleAccountManager =
                    new SimpleAccountManager(this);
        }

        if (simpleAccountManager.isSignedIn()) {
            SimpleSyncManager.syncAsync(
                    this,
                    null);
        }
    }

    @Override
    protected void onPause() {

        /*
         * MainActivity can be paused while it is still in the lightweight
         * profile-routing phase of onCreate(). Do not persist browser state
         * until the actual browser objects exist.
         */
        if (!profileSwitching &&
                browserSettings != null &&
                tabManager != null) {
            saveTabs();
        }

        if (!awaitingPasswordAuthentication) {
            passwordManagerAuthenticated = false;
        }

        boolean guestMode =
                tabManager != null &&
                tabManager.isGuestMode();

        if (cookieStore != null) {
            if (!profileSwitching &&
                    !guestMode) {
                cookieStore.snapshotCookies();
                snapshotAllWebStorage();
            }
            cookieStore.stopSync();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (!profileSwitching &&
                browserSettings != null &&
                tabManager != null) {
            saveTabs();
        }

        if (webStorageStore != null) {
            webStorageStore.close();
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
                tabManager != null &&
                tabManager.isGuestMode()) {
            clearRuntimeWebStorage();
            CookieStore.clearRuntimeCookies();
        }

        super.onDestroy();
    }

    public boolean openWebFileChooser(
            android.webkit.ValueCallback<Uri[]> callback,
            String[] acceptTypes,
            boolean allowMultiple) {

        if (callback == null) {
            return false;
        }

        clearPendingFileChooser();

        pendingFileChooser = callback;

        Intent intent =
                new Intent(
                        Intent.ACTION_GET_CONTENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE);

        intent.setType(
                getFileChooserMimeType(
                        acceptTypes));

        intent.putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                allowMultiple);

        try {
            startActivityForResult(
                    intent,
                    WEB_FILE_CHOOSER_REQUEST);
            return true;
        } catch (Throwable ignored) {
            clearPendingFileChooser();
            return false;
        }
    }

    public boolean openLegacyFileChooser(
            android.webkit.ValueCallback<Uri> callback,
            String acceptType) {

        if (callback == null) {
            return false;
        }

        clearPendingFileChooser();

        pendingLegacyFileChooser = callback;

        Intent intent =
                new Intent(
                        Intent.ACTION_GET_CONTENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE);

        intent.setType(
                acceptType == null ||
                acceptType.trim().isEmpty()
                        ? "*/*"
                        : acceptType.trim());

        try {
            startActivityForResult(
                    intent,
                    WEB_FILE_CHOOSER_REQUEST);
            return true;
        } catch (Throwable ignored) {
            clearPendingFileChooser();
            return false;
        }
    }

    private String getFileChooserMimeType(
            String[] acceptTypes) {

        if (acceptTypes != null) {
            for (String accept : acceptTypes) {
                if (accept != null &&
                        !accept.trim().isEmpty() &&
                        accept.contains("/")) {
                    return accept.trim();
                }
            }
        }

        return "*/*";
    }

    private void clearPendingFileChooser() {

        if (pendingFileChooser != null) {
            try {
                pendingFileChooser.onReceiveValue(null);
            } catch (Throwable ignored) {
            }
        }

        if (pendingLegacyFileChooser != null) {
            try {
                pendingLegacyFileChooser.onReceiveValue(null);
            } catch (Throwable ignored) {
            }
        }

        pendingFileChooser = null;
        pendingLegacyFileChooser = null;
    }

    private void handleWebFileChooserResult(
            int resultCode,
            Intent data) {

        android.webkit.ValueCallback<Uri[]> modern =
                pendingFileChooser;

        android.webkit.ValueCallback<Uri> legacy =
                pendingLegacyFileChooser;

        pendingFileChooser = null;
        pendingLegacyFileChooser = null;

        if (resultCode != RESULT_OK ||
                data == null) {

            if (modern != null) {
                modern.onReceiveValue(null);
            }

            if (legacy != null) {
                legacy.onReceiveValue(null);
            }

            return;
        }

        Uri[] results = null;

        ClipData clipData =
                data.getClipData();

        if (clipData != null &&
                clipData.getItemCount() > 0) {

            results =
                    new Uri[
                            clipData.getItemCount()];

            for (int i = 0;
                    i < clipData.getItemCount();
                    i++) {

                results[i] =
                        clipData.getItemAt(i)
                                .getUri();
            }

        } else if (data.getData() != null) {

            results =
                    new Uri[] {
                            data.getData()
                    };
        }

        if (modern != null) {
            modern.onReceiveValue(results);
        }

        if (legacy != null) {
            legacy.onReceiveValue(
                    results == null ||
                    results.length == 0
                            ? null
                            : results[0]);
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
