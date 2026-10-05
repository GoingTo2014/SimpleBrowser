package com.example.simplebrowser;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.ConsoleMessage;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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

        BrowserLogger.log(
                "NAV",
                "shouldOverrideUrlLoading: " +
                url);

        return false;
    }

    @Override
    public void doUpdateVisitedHistory(
            WebView view,
            String url,
            boolean isReload) {

        if (url == null ||
                isBrowserInternal(url) ||
                tab.settingsPage ||
                tab.defaultPage ||
                tab.historyPage ||
                tab.downloadsPage ||
                tab.errorPage) {
            return;
        }

        tab.url = url;

        if (tab == activity.getActiveTab()) {
            activity.setUrlText(url);
            activity.updateSecurity(tab);
            activity.updateNavigationButtonsForTabs();
        }

        BrowserLogger.log(
                "HISTORY",
                (isReload ? "reload: " : "url: ") +
                url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(
            WebView view,
            String url) {

        if (url != null) {
            BrowserLogger.log(
                    "NETWORK",
                    "GET " + url);
        }

        return null;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(
            WebView view,
            WebResourceRequest request) {

        if (Build.VERSION.SDK_INT >= 21 &&
                request != null &&
                request.getUrl() != null) {

            BrowserLogger.log(
                    "NETWORK",
                    request.getMethod() +
                    " " +
                    request.getUrl().toString());
        }

        return null;
    }

    private boolean isBrowserInternal(
            String url) {

        if (url == null) {
            return false;
        }

        String lower =
                url.trim()
                        .toLowerCase();

        return lower.startsWith(
                        "browser://") ||
                lower.startsWith(
                        "https://browser.local/");
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

        if (lower.equals(
                browserRoot + "/advanced")) {
            return "advanced";
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

        boolean errorPage =
                isBrowserLocalPage(
                        url,
                        "/error");

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
                errorPage ||
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

            if (errorPage) {
                tab.errorPage = true;
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

        if (failingUrl == null ||
                failingUrl.trim().isEmpty()) {
            return;
        }

        /*
         * Android 4.4 can report errors without a main-frame flag.
         * Matching the failing URL against the tab's current navigation
         * URL filters out most subresource errors while avoiding the
         * browser.local URL used by the custom error document itself.
         */
        String currentUrl =
                tab.url;

        if (tab.errorPage ||
                currentUrl == null ||
                currentUrl.trim().isEmpty() ||
                failingUrl.equals(currentUrl)) {

            if (!isBrowserInternal(failingUrl)) {

                BrowserLogger.log(
                        "ERROR",
                        "Load error " +
                        errorCode +
                        " for " +
                        failingUrl +
                        ": " +
                        description);

                tab.sslError = false;

                activity.showErrorPage(
                        tab,
                        failingUrl,
                        description);
            }
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
