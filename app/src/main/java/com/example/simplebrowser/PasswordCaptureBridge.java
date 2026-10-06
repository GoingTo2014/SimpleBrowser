package com.example.simplebrowser;

import android.webkit.JavascriptInterface;

/**
 * Narrow WebView bridge used only to notify Simple Browser about submitted
 * password forms. It never writes credentials itself; MainActivity asks the
 * user for confirmation first.
 */
public final class PasswordCaptureBridge {

    private final MainActivity activity;
    private final BrowserTab tab;

    private String lastSignature = "";
    private long lastSubmittedAt = 0L;

    public PasswordCaptureBridge(
            MainActivity activity,
            BrowserTab tab) {
        this.activity = activity;
        this.tab = tab;
    }

    @JavascriptInterface
    public void submitted(
            String site,
            String username,
            String password) {

        if (password == null ||
                password.isEmpty()) {
            return;
        }

        String signature =
                (site == null ? "" : site) +
                "\n" +
                (username == null ? "" : username);

        long now =
                System.currentTimeMillis();

        if (signature.equals(lastSignature) &&
                now - lastSubmittedAt < 2500L) {
            return;
        }

        lastSignature = signature;
        lastSubmittedAt = now;

        activity.runOnUiThread(
                () -> activity.promptToSavePassword(
                        tab,
                        site,
                        username,
                        password));
    }
    }
}
