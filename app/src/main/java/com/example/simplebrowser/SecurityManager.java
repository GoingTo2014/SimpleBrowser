package com.example.simplebrowser;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.http.SslCertificate;
import android.widget.ImageButton;

import java.text.DateFormat;
import java.util.Date;

public class SecurityManager {

    private static final int SECURE_GREEN =
            Color.rgb(0, 170, 70);

    private static final int LOCAL_BLUE =
            Color.rgb(35, 105, 190);

    private static final int WARNING_RED =
            Color.rgb(210, 45, 45);

    private final MainActivity activity;
    private final ImageButton securityButton;

    public SecurityManager(
            MainActivity activity,
            ImageButton securityButton) {

        this.activity = activity;
        this.securityButton =
                securityButton;

        BrowserTab tab =
                activity.getActiveTab();

        int chromeColor =
                tab != null &&
                        tab.isIncognito
                        ? Color.rgb(
                                32, 33, 36)
                        : ColorUtils.parseColor(
                                activity
                                        .getBrowserSettings()
                                        .getAccentColor(),
                                Color.WHITE);

        securityButton.setBackground(
                ButtonFeedback.create(
                        ColorUtils
                                .getReadableTextColor(
                                        chromeColor)));

        securityButton.setOnClickListener(
                v -> {

                    BrowserTab currentTab =
                            activity.getActiveTab();

                    if (currentTab != null) {
                        showInfo(currentTab);
                    }
                });
    }

    public void applyAppearance() {

        int accent =
                ColorUtils.parseColor(
                        activity
                                .getBrowserSettings()
                                .getAccentColor(),
                        Color.rgb(
                                63, 81, 181));

        securityButton.setBackground(
                ButtonFeedback.create(
                        ColorUtils
                                .getReadableTextColor(
                                        accent)));

        updateIcon(
                activity.getActiveTab());
    }

    public void updateIcon(
            BrowserTab tab) {

        if (tab == null ||
                tab != activity.getActiveTab()) {
            return;
        }

        String url =
                tab.webView.getUrl();

        if (url == null) {
            url = tab.url;
        }

        String tabUrl =
                tab.url == null
                        ? ""
                        : tab.url.trim()
                                .toLowerCase();

        if (tab.settingsPage ||
                tab.defaultPage ||
                tab.historyPage ||
                tab.downloadsPage ||
                tabUrl.startsWith("browser://")) {

            setFileIcon(
                    Localization.translate(activity, "security.browser_page"));

            return;
        }

        String lower =
                url == null
                        ? ""
                        : url.toLowerCase();

        if (lower.startsWith("browser://")) {

            setFileIcon(
                    Localization.translate(activity, "security.browser_page"));

            return;
        }

        if (lower.startsWith(
                "file://") ||
                lower.startsWith(
                        "content://")) {

            setFileIcon(
                    Localization.translate(activity, "security.local_file"));

            return;
        }

        boolean https =
                lower.startsWith(
                        "https://");

        if (tab.sslError) {

            setWarning(
                    Localization.translate(activity, "security.warning"));

        } else if (https) {

            setIcon(
                    android.R.drawable.ic_lock_lock,
                    SECURE_GREEN,
                    Localization.translate(activity, "security.secure"));

        } else {

            setUnlockIcon(
                    Localization.translate(activity, "security.unconfirmed"));
        }
    }

    private void setIcon(
            int drawableRes,
            int color,
            String description) {

        securityButton.setImageResource(
                drawableRes);

        securityButton.setColorFilter(
                color,
                PorterDuff.Mode.SRC_IN);

        securityButton.setContentDescription(
                description);
    }

    private int getThemeIconColor() {

        BrowserTab active =
                activity.getActiveTab();

        int chromeColor;

        if (active != null &&
                active.isIncognito) {

            chromeColor =
                    Color.rgb(
                            32,
                            33,
                            36);

        } else {

            chromeColor =
                    ColorUtils.parseColor(
                            activity
                                    .getBrowserSettings()
                                    .getAccentColor(),
                            Color.WHITE);
        }

        return ColorUtils.getReadableTextColor(
                chromeColor);
    }

    private void setFileIcon(
            String description) {

        securityButton.setImageDrawable(
                new BrowserIconDrawable(
                        BrowserIconDrawable.FILE,
                        getThemeIconColor()));

        securityButton.setColorFilter(null);
        securityButton.setContentDescription(
                description);
    }

    private void setUnlockIcon(
            String description) {

        securityButton.setImageDrawable(
                new BrowserIconDrawable(
                        BrowserIconDrawable.UNLOCK,
                        getThemeIconColor()));

        securityButton.setColorFilter(null);
        securityButton.setContentDescription(
                description);
    }

    private void setWarning(
            String description) {

        setIcon(
                android.R.drawable.ic_dialog_alert,
                WARNING_RED,
                description);
    }

    private void showInfo(
            BrowserTab tab) {

        if (tab.settingsPage) {

            new AlertDialog.Builder(activity)
                    .setTitle(Localization.translate(activity, "security.settings_title"))
                    .setMessage(Localization.translate(activity, "security.builtin"))
                    .setPositiveButton(
                            Localization.translate(activity, "common.ok"),
                            null)
                    .show();

            return;
        }

        String tabUrl =
                tab.url == null
                        ? ""
                        : tab.url.trim()
                                .toLowerCase();

        if (tab.settingsPage ||
                tab.defaultPage ||
                tab.historyPage ||
                tab.downloadsPage ||
                tabUrl.startsWith("browser://")) {

            new AlertDialog.Builder(activity)
                    .setTitle(Localization.translate(activity, "security.browser_page"))
                    .setMessage(Localization.translate(activity, "security.builtin"))
                    .setPositiveButton(
                            Localization.translate(activity, "common.ok"),
                            null)
                    .show();

            return;
        }

        String url =
                tab.webView.getUrl();

        if (url == null) {
            url = tab.url;
        }

        String lower =
                url == null
                        ? ""
                        : url.toLowerCase();

        if (lower.startsWith("file://") ||
                lower.startsWith("content://")) {

            new AlertDialog.Builder(activity)
                    .setTitle(Localization.translate(activity, "security.local_file"))
                    .setMessage(
                            Localization.translate(activity, "security.device_page") + "\n\n" +
                            "URL:\n" +
                            safe(url))
                    .setPositiveButton(
                            Localization.translate(activity, "common.ok"),
                            null)
                    .show();

            return;
        }

        boolean https =
                lower.startsWith(
                        "https://");

        if (!https) {

            new AlertDialog.Builder(activity)
                    .setTitle(Localization.translate(activity, "security.not_secure"))
                    .setMessage(
                            Localization.translate(activity, "security.not_secure_desc") + "\n\n" +
                            "URL:\n" +
                            safe(url))
                    .setPositiveButton(
                            Localization.translate(activity, "common.ok"),
                            null)
                    .show();

            return;
        }

        if (tab.sslError) {

            new AlertDialog.Builder(activity)
                    .setTitle(Localization.translate(activity, "security.connection_not_secure"))
                    .setMessage(
                            Localization.translate(activity, "security.cert_untrusted") + "\n\n" +
                            Localization.translate(activity, "security.page_blocked"))
                    .setPositiveButton(
                            Localization.translate(activity, "common.ok"),
                            null)
                    .show();

            return;
        }

        SslCertificate certificate =
                tab.webView.getCertificate();

        if (certificate == null) {

            new AlertDialog.Builder(activity)
                    .setTitle(Localization.translate(activity, "security.certificate"))
                    .setMessage(
                            Localization.translate(activity, "security.no_certificate"))
                    .setPositiveButton(
                            Localization.translate(activity, "common.ok"),
                            null)
                    .show();

            return;
        }

        SslCertificate.DName issuedTo =
                certificate.getIssuedTo();

        SslCertificate.DName issuedBy =
                certificate.getIssuedBy();

        Date validFrom =
                certificate
                        .getValidNotBeforeDate();

        Date validTo =
                certificate
                        .getValidNotAfterDate();

        DateFormat format =
                DateFormat.getDateTimeInstance();

        StringBuilder message =
                new StringBuilder();

        message.append(
                Localization.translate(activity, "security.connection_secure") + "\n\n");

        message.append(Localization.translate(activity, "security.url") + "\n");
        message.append(safe(url));

        message.append("\n\n" + Localization.translate(activity, "security.issued_to"));

        message.append(
                issuedTo == null
                        ? Localization.translate(activity, "common.unknown")
                        : safe(issuedTo.getCName()));

        message.append("\n\n" + Localization.translate(activity, "security.issued_by"));

        if (issuedBy != null) {

            message.append(
                    safe(issuedBy.getCName()));

            if (issuedBy.getOName() != null &&
                    !issuedBy.getOName().isEmpty()) {

                message.append("\n");
                message.append(
                        issuedBy.getOName());
            }

        } else {

            message.append(Localization.translate(activity, "common.unknown"));
        }

        message.append("\n\n" + Localization.translate(activity, "security.valid_from"));

        message.append(
                validFrom == null
                        ? Localization.translate(activity, "common.unknown")
                        : format.format(validFrom));

        message.append("\n\n" + Localization.translate(activity, "security.valid_until"));

        message.append(
                validTo == null
                        ? Localization.translate(activity, "common.unknown")
                        : format.format(validTo));

        new AlertDialog.Builder(activity)
                .setTitle(Localization.translate(activity, "security.certificate"))
                .setMessage(
                        message.toString())
                .setPositiveButton(
                        "OK",
                        null)
                .show();
    }

    private String safe(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {
            return "Unknown";
        }

        return value;
    }
}
