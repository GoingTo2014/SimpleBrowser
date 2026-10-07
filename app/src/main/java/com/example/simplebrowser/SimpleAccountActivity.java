package com.example.simplebrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
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
    private AlertDialog dialog;
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
                new WebView(this);

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

        dialog =
                new AlertDialog.Builder(this)
                        .setView(root)
                        .create();

        dialog.setOnDismissListener(
                d -> {
                    if (!completed &&
                            !isFinishing()) {
                        finish();
                    }
                });

        dialog.setOnShowListener(
                d -> {
                    if (dialog.getWindow() != null) {
                        android.view.Window window =
                                dialog.getWindow();

                        window.setLayout(
                                -1,
                                (int)
                                        (getResources()
                                                .getDisplayMetrics()
                                                .heightPixels *
                                                0.88f));

                        window.setGravity(
                                Gravity.CENTER);

                        window.setBackgroundDrawable(
                                new android.graphics.drawable
                                        .ColorDrawable(
                                                Color.WHITE));
                    }
                });

        dialog.show();
    }

    private void configureWebView() {
        webView.setBackgroundColor(
                Color.WHITE);

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
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request) {

                        if (request == null ||
                                request.getUrl() == null) {
                            return true;
                        }

                        return handleUrl(
                                request.getUrl()
                                        .toString());
                    }

                    @Override
                    public void onReceivedError(
                            WebView view,
                            int errorCode,
                            String description,
                            String failingUrl) {
                    }
                });

        webView.loadUrl(
                SimpleAccountManager.ACCOUNT_SITE);
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
                    if (dialog != null &&
                            dialog.isShowing()) {
                        dialog.dismiss();
                    }

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
