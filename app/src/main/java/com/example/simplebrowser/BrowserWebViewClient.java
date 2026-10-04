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

    private boolean isBrowserLocalPage(
            String url,
            String path) {

        if (url == null) {
            return false;
        }

        String lower =
                url.trim()
                        .toLowerCase();

        return lower.equals(
                "https://browser.local" +
                        path);
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

        boolean defaultPage =
                isBrowserLocalPage(
                        url,
                        "/default");

        boolean historyPage =
                isBrowserLocalPage(
                        url,
                        "/history");

        boolean downloadsPage =
                isBrowserLocalPage(
                        url,
                        "/downloads");

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

        } else if (defaultPage ||
                historyPage ||
                downloadsPage ||
                (tab.errorPage &&
                 (url == null ||
                  url.equals(tab.url)))) {

            /*
             * These are browser-owned HTML documents.
             * Keep their bridges/state and do not treat the
             * browser.local base URL as a real website.
             */
            if (defaultPage) {
                tab.defaultPage = true;
            }

            if (historyPage) {
                tab.historyPage = true;
            }

            if (downloadsPage) {
                tab.downloadsPage = true;
            }

            tab.loading = false;

        } else if (tab.errorPage) {

            /*
             * A different URL means the user left the custom
             * error page, usually by pressing Back.
             */
            tab.errorPage = false;

            if (tab.settingsPage ||
                    tab.defaultPage ||
                    tab.historyPage ||
                    tab.downloadsPage) {
                activity.removeSettingsBridge(tab);
            }

            tab.settingsPage = false;
            tab.defaultPage = false;
            tab.historyPage = false;
            tab.downloadsPage = false;

        } else if (tab.settingsPage ||
                tab.defaultPage ||
                tab.historyPage ||
                tab.downloadsPage) {

            activity.removeSettingsBridge(tab);
            tab.defaultPage = false;
            tab.historyPage = false;
            tab.settingsPage = false;
            tab.webView.removeJavascriptInterface(
                    "DefaultPage");
            tab.webView.removeJavascriptInterface(
                    "HistoryPage");
            tab.webView.removeJavascriptInterface(
                    "DownloadsPage");
        }

        if (!tab.settingsPage &&
                !tab.defaultPage &&
                !tab.historyPage &&
                !tab.downloadsPage &&
                !tab.errorPage) {

            tab.url = url;
            tab.loading = true;
            tab.sslError = false;
        }

        activity.pageStarted(
                tab,
                tab.errorPage
                        ? tab.url
                        : url);
    }

    @Override
    public void onPageFinished(
            WebView view,
            String url) {

        if (tab.errorPage) {

            activity.pageFinished(
                    tab,
                    tab.url);

            return;
        }

        if (!tab.settingsPage &&
                !tab.defaultPage &&
                !tab.historyPage &&
                !tab.downloadsPage) {

            tab.url = url;
            tab.loading = false;

            activity.recordVisit(
                    tab,
                    url);
        }

        activity.pageFinished(
                tab,
                tab.settingsPage
                        ? activity.getSettingsUrl(
                                tab.settingsSection)
                        : url);
    }

    @Override
    public void onReceivedError(
            WebView view,
            int errorCode,
            String description,
            String failingUrl) {

        if (tab.isIncognito) {
            // Incognito still gets the improved error page,
            // but it is never written to browsing history.
        }

        String current =
                view.getUrl();

        if (failingUrl != null &&
                (current == null ||
                 failingUrl.equals(current))) {

            tab.sslError = false;

            activity.showErrorPage(
                    tab,
                    failingUrl,
                    description);
        }
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
