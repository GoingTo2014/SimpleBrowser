package com.example.simplebrowser;

import android.webkit.JavascriptInterface;

/**
 * Receives localStorage snapshots from a specific WebView/tab.
 *
 * Captures are disabled during profile/session transitions so late callbacks
 * from an old WebView cannot overwrite another profile.
 */
public final class StorageCaptureBridge {

    private final MainActivity activity;
    private final BrowserTab tab;

    public StorageCaptureBridge(
            MainActivity activity,
            BrowserTab tab) {

        this.activity = activity;
        this.tab = tab;
    }

    @JavascriptInterface
    public void save(
            String origin,
            String data) {

        if (activity == null ||
                tab == null ||
                tab.isIncognito ||
                tab.isGuest ||
                activity.isProfileSwitching()) {
            return;
        }

        activity.saveWebStorageSnapshot(
                origin,
                data);
    }
}
