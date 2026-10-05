package com.example.simplebrowser;

import android.os.Message;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class BrowserChromeClient
        extends WebChromeClient {

    private final MainActivity activity;
    private final BrowserTab tab;

    public BrowserChromeClient(
            MainActivity activity,
            BrowserTab tab) {

        this.activity = activity;
        this.tab = tab;
    }

    @Override
    public void onProgressChanged(
            WebView view,
            int progress) {

        activity.pageProgress(
                tab,
                progress);
    }

    @Override
    public void onReceivedTitle(
            WebView view,
            String title) {

        if (tab.settingsPage) {

            tab.title = "Settings";

        } else if (tab.defaultPage ||
                "about:blank".equalsIgnoreCase(title) ||
                title == null ||
                title.trim().isEmpty()) {

            /*
             * The built-in new-tab document must never expose
             * the WebView's temporary about:blank title.
             */
            tab.title = "New Tab";

        } else {

            tab.title = title.trim();
        }

        activity.updateTabTitle(tab);
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(
            String origin,
            GeolocationPermissions.Callback callback) {

        activity.handleGeolocationRequest(
                origin,
                callback);
    }

    @Override
    public boolean onCreateWindow(
            WebView view,
            boolean isDialog,
            boolean isUserGesture,
            Message resultMsg) {

        if (!activity
                .getBrowserSettings()
                .arePopupsEnabled()) {

            return false;
        }

        BrowserTab newTab =
                activity
                        .getTabManager()
                        .addTab("about:blank");

        WebView.WebViewTransport transport =
                (WebView.WebViewTransport)
                        resultMsg.obj;

        transport.setWebView(
                newTab.webView);

        resultMsg.sendToTarget();

        return true;
    }
}
