package com.example.simplebrowser;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.net.http.SslErrorHandler;
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

        if ("browser://settings".equals(
                url.toLowerCase())) {

            activity.showSettings(tab);

            return true;
        }

        // Leaving the internal settings page.
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

        // If we are navigating away from settings,
        // remove the bridge immediately.
        if (tab.settingsPage &&
                !"browser://settings".equals(
                        url.toLowerCase())) {

            activity.removeSettingsBridge(tab);
        }

        tab.url = url;
        tab.loading = true;
        tab.sslError = false;
        tab.settingsPage = false;

        activity.pageStarted(tab, url);
    }

    @Override
    public void onPageFinished(
            WebView view,
            String url) {

        tab.url = url;
        tab.loading = false;

        activity.pageFinished(tab, url);
    }

    @Override
    public void onReceivedSslError(
            WebView view,
            SslErrorHandler handler,
            SslError error) {

        tab.sslError = true;

        activity.updateSecurity(tab);

        // Never bypass an invalid certificate.
        handler.cancel();
    }
        }
