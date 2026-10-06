package com.example.simplebrowser;

import android.os.Message;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.ValueCallback;
import android.net.Uri;

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
    public void onReceivedIcon(
            WebView view,
            android.graphics.Bitmap favicon) {

        if (favicon == null ||
                favicon.isRecycled()) {
            return;
        }

        tab.favicon = favicon;

        activity.updateTabIcon(
                tab,
                favicon);
    }

    @Override
    public void onReceivedTitle(
            WebView view,
            String title) {

        if (tab.settingsPage) {

            tab.title = Localization.translate(activity, "Settings");

        } else if (tab.errorPage) {

            tab.title = Localization.translate(activity, "Page unavailable");

        } else if (tab.defaultPage ||
                "about:blank".equalsIgnoreCase(title) ||
                title == null ||
                title.trim().isEmpty()) {

            /*
             * The built-in new-tab document must never expose
             * the WebView's temporary about:blank title.
             */
            tab.title = Localization.translate(activity, "New Tab");

        } else {

            tab.title = title.trim();
        }

        activity.updateTabTitle(tab);
    }

    @Override
    public boolean onConsoleMessage(
            ConsoleMessage message) {

        if (message != null) {
            BrowserLogger.log(
                    "CONSOLE",
                    message.message() +
                    " (" +
                    message.sourceId() +
                    ":" +
                    message.lineNumber() +
                    ", " +
                    message.messageLevel() +
                    ")");
        }

        return true;
    }

    @android.annotation.TargetApi(21)
    @Override
    public boolean onShowFileChooser(
            WebView webView,
            ValueCallback<Uri[]> filePathCallback,
            WebChromeClient.FileChooserParams fileChooserParams) {

        String[] acceptTypes =
                fileChooserParams == null
                        ? null
                        : fileChooserParams.getAcceptTypes();

        boolean allowMultiple =
                fileChooserParams != null &&
                fileChooserParams.getMode() ==
                        WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;

        return activity.openWebFileChooser(
                filePathCallback,
                acceptTypes,
                allowMultiple);
    }

    /*
     * Android 4.4-4.4W WebView uses these legacy callbacks instead of
     * onShowFileChooser().
     */
    public void openFileChooser(
            ValueCallback<Uri> uploadMsg,
            String acceptType,
            String capture) {

        activity.openLegacyFileChooser(
                uploadMsg,
                acceptType);
    }

    public void openFileChooser(
            ValueCallback<Uri> uploadMsg) {

        activity.openLegacyFileChooser(
                uploadMsg,
                "*/*");
    }

    public void openFileChooser(
            ValueCallback<Uri> uploadMsg,
            String acceptType) {

        activity.openLegacyFileChooser(
                uploadMsg,
                acceptType);
    }

    @android.annotation.TargetApi(21)
    @Override
    public void onPermissionRequest(
            android.webkit.PermissionRequest request) {

        activity.handleWebPermissionRequest(
                request);
    }

    @android.annotation.TargetApi(21)
    @Override
    public void onPermissionRequestCanceled(
            android.webkit.PermissionRequest request) {

        activity.handleWebPermissionRequestCanceled(
                request);
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
