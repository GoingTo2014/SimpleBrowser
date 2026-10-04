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

        if (url != null) {

            String settingsSection =
                    getSettingsSection(url);

            if (settingsSection != null) {

                activity.showSettingsSection(
                        tab,
                        settingsSection);

                return true;
            }
        }

        /*
         * Never allow the Android bridge to survive
         * when the built-in settings page becomes
         * a normal page.
         */
        if (tab.settingsPage) {
            activity.removeSettingsBridge(tab);
        }

        return false;
    }

    private String getSettingsSection(
            String url) {

        if (url == null) {
            return null;
        }

        String lower =
                url.trim()
                        .toLowerCase();

        String root =
                "browser://settings";

        if (!lower.startsWith(root)) {
            return null;
        }

        if (lower.equals(root) ||
                lower.equals(
                        root + "/general")) {
            return "general";
        }

        if (lower.equals(
                root + "/websites")) {
            return "websites";
        }

        if (lower.equals(
                root + "/appearance")) {
            return "appearance";
        }

        if (lower.equals(
                root + "/privacy") ||
                lower.equals(
                        root + "/privacy-security")) {
            return "privacy-security";
        }

        return null;
    }

    @Override
    public void onPageStarted(
            WebView view,
            String url,
            Bitmap favicon) {

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

        activity.pageStarted(
                tab,
                url);
    }

    @Override
    public void onPageFinished(
            WebView view,
            String url) {

        if (!tab.settingsPage) {

            tab.url = url;
            tab.loading = false;
        }

        activity.pageFinished(
                tab,
                url);
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
