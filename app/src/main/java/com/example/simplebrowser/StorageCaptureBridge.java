package com.example.simplebrowser;

import android.webkit.JavascriptInterface;

/**
 * Receives localStorage snapshots from a page and writes them into the
 * currently selected profile's storage database.
 */
public final class StorageCaptureBridge {

    private final MainActivity activity;

    public StorageCaptureBridge(
            MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void save(
            String origin,
            String data) {

        activity.saveWebStorageSnapshot(
                origin,
                data);
    }
}
