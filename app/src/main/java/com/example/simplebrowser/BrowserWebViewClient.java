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

        String browserRoot =
                "browser://settings";

        if (lower.equals(browserRoot) ||
                lower.equals(
                        browserRoot + "/general")) {
            return "general";
        }

        if (lower.equals(
                browserRoot + "/websites")) {
            return "websites";
        }

        if (lower.equals(
                browserRoot + "/appearance")) {
            return "appearance";
        }

        if (lower.equals(
                browserRoot + "/privacy") ||
                lower.equals(
                        browserRoot + "/privacy-security")) {
            return "privacy-security";
        }

        /*
         * Settings HTML uses this HTTPS base URL so the
         * WebView can keep the page in its back/forward
         * history. The address bar still displays the
         * browser://settings URL.
         */
        String localRoot =
                "https://browser.local";

        if (lower.equals(localRoot) ||
                lower.equals(localRoot + "/") ||
                lower.equals(
                        localRoot + "/settings") ||
                lower.equals(
                        localRoot + "/settings/") ||
                lower.equals(
                        localRoot + "/settings/general")) {
            return "general";
        }

        if (lower.equals(
                localRoot +
                        "/settings/websites")) {
            return "websites";
        }

        if (lower.equals(
                localRoot +
                        "/settings/appearance")) {
            return "appearance";
        }

        if (lower.equals(
                localRoot +
                        "/settings/privacy") ||
                lower.equals(
                        localRoot +
                                "/settings/privacy-security")) {
            return "privacy-security";
        }

        return null;
    }

    @Override
    public void onPageStarted(
            WebView view,
            String url,
            Bitmap favicon) {

        String settingsSection =
                getSettingsSection(url);

        if (settingsSection != null) {

            if (!tab.settingsPage) {

                activity.restoreSettingsPage(
                        tab,
                        settingsSection);

            } else {

                tab.settingsSection =
                        settingsSection;
                tab.url =
                        activity.getSettingsUrl(
                                settingsSection);
            }

        } else if (tab.settingsPage) {

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
