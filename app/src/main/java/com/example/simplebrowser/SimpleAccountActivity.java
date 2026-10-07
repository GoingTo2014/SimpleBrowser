package com.example.simplebrowser;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.graphics.Rect;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;

import java.util.Locale;

/**
 * Hosts the Simple Account GitHub Pages authentication UI in a contained
 * WebView popup.
 */
public final class SimpleAccountActivity extends Activity {

    private WebView webView;
    private SimpleAccountManager accountManager;
    private boolean completed;

    private static final String ACCOUNT_ORIGIN =
            "https://goingto2014.github.io";

    private static final String ACCOUNT_PATH =
            "/SimpleAccount/";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        accountManager =
                new SimpleAccountManager(this);

        showPopup();
    }

    private void showPopup() {
        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL);

        root.setBackgroundColor(
                Color.WHITE);

        LinearLayout header =
                new LinearLayout(this);

        header.setGravity(
                Gravity.CENTER_VERTICAL);

        header.setPadding(
                dp(16),
                dp(6),
                dp(8),
                dp(6));

        TextView title =
                new TextView(this);

        title.setText(
                "Simple Account");

        title.setTextColor(
                Color.rgb(32, 33, 36));

        title.setTextSize(18f);

        title.setGravity(
                Gravity.CENTER_VERTICAL);

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1f));

        TextView close =
                new TextView(this);

        close.setText("×");
        close.setTextSize(28f);
        close.setTextColor(
                Color.rgb(80, 80, 80));
        close.setGravity(
                Gravity.CENTER);

        close.setClickable(true);

        close.setOnClickListener(
                v -> finish());

        header.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)));

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(60)));

        FrameLayout content =
                new FrameLayout(this);

        ProgressBar progress =
                new ProgressBar(this);

        progress.setIndeterminate(true);

        content.addView(
                progress,
                new FrameLayout.LayoutParams(
                        dp(42),
                        dp(42),
                        Gravity.CENTER));

        webView =
                new AccountWebView(this);

        configureWebView();

        content.addView(
                webView,
                new FrameLayout.LayoutParams(
                        -1,
                        -1));

        root.addView(
                content,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1f));

        setContentView(root);

        android.view.Window window =
                getWindow();

        window.setSoftInputMode(
                android.view.WindowManager.LayoutParams
                        .SOFT_INPUT_ADJUST_RESIZE |
                android.view.WindowManager.LayoutParams
                        .SOFT_INPUT_STATE_UNSPECIFIED);

        /*
         * The old implementation nested an AlertDialog inside this
         * dialog-themed Activity. That created a separate dialog window for
         * the WebView and could prevent Android 4.4 from routing IME input
         * to it. This Activity is now the single Account window.
         */
        window.clearFlags(
                android.view.WindowManager.LayoutParams
                        .FLAG_ALT_FOCUSABLE_IM |
                android.view.WindowManager.LayoutParams
                        .FLAG_NOT_FOCUSABLE);

        window.getDecorView().setPadding(0, 0, 0, 0);
        window.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(
                        Color.WHITE));
    }

    private void configureWebView() {
        webView.setBackgroundColor(
                Color.WHITE);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        /*
         * Let Chromium handle HTML input taps itself. On KitKat, manually
         * calling showSoftInput() after the tap can race Chromium's focus
         * update and make the IME appear briefly and then disappear.
         */
        webView.setOnTouchListener(
                (view, event) -> {
                    if (event.getAction() ==
                            MotionEvent.ACTION_DOWN &&
                            !view.hasFocus()) {
                        view.requestFocusFromTouch();
                    }
                    return false;
                });

        android.webkit.WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadsImagesAutomatically(true);

        webView.setWebChromeClient(
                new WebChromeClient());

        webView.addJavascriptInterface(
                new AccountBridge(),
                "Android");

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            String url) {

                        return handleUrl(url);
                    }

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url) {
                    }

                    @Override
                    public void onReceivedError(
                            WebView view,
                            int errorCode,
                            String description,
                            String failingUrl) {
                    }
                });

        String accountUrl =
                SimpleAccountManager.ACCOUNT_SITE;

        try {
            BrowserSettings browserSettings =
                    new BrowserSettings(this);

            String language =
                    browserSettings.getLanguage();

            if (language != null &&
                    !language.trim().isEmpty() &&
                    !Localization.SYSTEM.equals(language)) {
                accountUrl +=
                        "?lang=" +
                        android.net.Uri.encode(language);
            }
        } catch (Throwable ignored) {
        }

        webView.loadUrl(accountUrl);
    }

    private boolean handleUrl(
            String url) {

        if (url == null ||
                !isTrustedAccountUrl(url)) {
            /*
             * Do not let the account popup become a general-purpose browser.
             * External links are intentionally blocked.
             */
            return true;
        }

        return false;
    }

    private boolean isTrustedAccountUrl(
            String url) {

        try {
            Uri uri =
                    Uri.parse(url);

            String host =
                    uri.getHost();

            String path =
                    uri.getPath();

            return "https".equalsIgnoreCase(
                        uri.getScheme()) &&
                    ACCOUNT_ORIGIN
                        .substring(
                                "https://".length())
                        .equalsIgnoreCase(host) &&
                    path != null &&
                    path.startsWith(
                            ACCOUNT_PATH);

        } catch (Throwable ignored) {
            return false;
        }
    }

    private void finishWithSession(
            String idToken,
            String refreshToken,
            String uid,
            String email) {

        if (completed) {
            return;
        }

        if (!isTrustedAccountUrl(
                webView.getUrl())) {
            return;
        }

        if (idToken == null ||
                idToken.length() == 0 ||
                refreshToken == null ||
                refreshToken.length() == 0 ||
                uid == null ||
                uid.length() == 0) {
            return;
        }

        completed = true;

        accountManager.saveSession(
                uid,
                email,
                idToken,
                refreshToken,
                3600L);

        runOnUiThread(
                () -> {
                    setResult(
                            RESULT_OK);

                    finish();
                });
    }

    private int dp(int value) {
        return (int)
                (value *
                 getResources()
                         .getDisplayMetrics()
                         .density +
                 0.5f);
    }

    private static final class AccountWebView
            extends WebView {

        private boolean laidOutOnce;

        AccountWebView(
                android.content.Context context) {
            super(context);
        }

        @Override
        public boolean onCheckIsTextEditor() {
            return true;
        }

        @Override
        protected void onFocusChanged(
                boolean focused,
                int direction,
                Rect previouslyFocusedRect) {

            /*
             * Android 4.4 WebView can drop native focus while an HTML input
             * is editing, which immediately kills the IME. Keep the native
             * WebView editor focused for the lifetime of the page.
             */
            super.onFocusChanged(
                    true,
                    direction,
                    previouslyFocusedRect);
        }

        @Override
        protected void onLayout(
                boolean changed,
                int left,
                int top,
                int right,
                int bottom) {

            /*
             * The first layout can otherwise trigger the KitKat WebView
             * focus/IME race. Keep the initial layout pass stable.
             */
            if (!laidOutOnce) {
                super.onLayout(
                        changed,
                        left,
                        top,
                        right,
                        bottom);
                laidOutOnce = true;
                return;
            }

            super.onLayout(
                    changed,
                    left,
                    top,
                    right,
                    bottom);
        }

        @Override
        public InputConnection onCreateInputConnection(
                EditorInfo outAttrs) {

            InputConnection connection =
                    super.onCreateInputConnection(
                            outAttrs);

            if (outAttrs != null) {
                outAttrs.imeOptions &=
                        ~EditorInfo.IME_ACTION_GO;
                outAttrs.imeOptions &=
                        ~EditorInfo.IME_ACTION_SEARCH;
                outAttrs.imeOptions &=
                        ~EditorInfo.IME_ACTION_SEND;
                outAttrs.imeOptions &=
                        ~EditorInfo.IME_ACTION_DONE;
                outAttrs.imeOptions |=
                        EditorInfo.IME_ACTION_NEXT;
            }

            return connection;
        }
    }

    private final class AccountBridge {

        @JavascriptInterface
        public void onSimpleAccountAuthenticated(
                final String idToken,
                final String refreshToken,
                final String uid,
                final String email) {

            finishWithSession(
                    idToken,
                    refreshToken,
                    uid,
                    email);
        }

        @JavascriptInterface
        public void onSimpleAccountCancelled() {
            runOnUiThread(
                    () -> {
                        if (!completed) {
                            setResult(
                                    RESULT_CANCELED);
                            finish();
                        }
                    });
        }
    }
}
