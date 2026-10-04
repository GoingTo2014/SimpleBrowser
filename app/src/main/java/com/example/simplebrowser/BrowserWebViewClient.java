package com.example.simplebrowser;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserWebViewClient
        extends WebViewClient {

    private final MainActivity activity;
    private final BrowserTab tab;

    public BrowserWebViewClient(
            MainActivity activity,
            BrowserTab tab) {

        this.activity = activity;
        this.tab = tab;
    }

    @Override
    public boolean shouldOverrideUrlLoading(
            WebView view,
            String url) {

        if (url != null &&
                "browser://settings"
                        .equalsIgnoreCase(url)) {

            activity.showSettings(tab);

            return true;
        }

        /*
         * Never allow the Android bridge to
         * survive into a normal website.
         */
        if (tab.settingsPage) {

            activity.removeSettingsBridge(tab);
        }

        return false;
    }

    @Override
    public void onPageStarted(
            WebView view,
            String url,
            Bitmap favicon) {

        /*
         * loadDataWithBaseURL() uses the internal
         * browser.local base URL. Keep settings
         * marked as an internal page.
         */
        if (tab.settingsPage &&
                (url == null ||
                 !url.startsWith(
                         "https://browser.local"))) {

            activity.removeSettingsBridge(tab);
        }

        if (!tab.settingsPage) {

            tab.url = url;
            tab.loading = true;
            tab.sslError = false;
        }

        activity.pageStarted(tab, url);
    }

    @Override
    public void onPageFinished(
            WebView view,
            String url) {

        if (!tab.settingsPage) {

            tab.url = url;
            tab.loading = false;
        }

        activity.pageFinished(tab, url);
    }

    @Override
    public void onReceivedSslError(
            WebView view,
            SslErrorHandler handler,
            SslError error) {

        tab.sslError = true;

        activity.updateSecurity(tab);

        /*
         * Do not allow invalid certificates.
         */
        handler.cancel();
    }
}
