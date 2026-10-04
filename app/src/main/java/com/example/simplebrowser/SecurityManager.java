package com.example.simplebrowser;

import android.app.AlertDialog;
import android.net.http.SslCertificate;
import android.view.View;
import android.widget.ImageButton;

import java.text.DateFormat;
import java.util.Date;

public class SecurityManager {

    private final MainActivity activity;
    private final ImageButton securityButton;

    public SecurityManager(
            MainActivity activity,
            ImageButton securityButton) {

        this.activity = activity;
        this.securityButton = securityButton;

        securityButton.setOnClickListener(
                v -> {

            BrowserTab tab =
                    activity.getActiveTab();

            if (tab != null) {
                showInfo(tab);
            }
        });
    }

    public void updateIcon(
            BrowserTab tab) {

        if (tab == null ||
                tab != activity.getActiveTab()) {

            return;
        }

        if (tab.settingsPage) {

            securityButton.setImageResource(
                    android.R.drawable.ic_menu_manage);

            return;
        }

        String url =
                tab.webView.getUrl();

        if (url == null) {
            url = tab.url;
        }

        boolean https =
                url != null &&
                url.startsWith("https://");

        if (tab.sslError) {

            securityButton.setImageResource(
                    android.R.drawable.ic_dialog_alert);

        } else if (https) {

            securityButton.setImageResource(
                    android.R.drawable.ic_lock_lock);

        } else {

            securityButton.setImageResource(
                    android.R.drawable.ic_dialog_alert);
        }
    }

    private void showInfo(
            BrowserTab tab) {

        if (tab.settingsPage) {

            new AlertDialog.Builder(activity)
                    .setTitle("Browser Settings")
                    .setMessage(
                            "This is a built-in browser page.")
                    .setPositiveButton(
                            "OK",
                            null)
                    .show();

            return;
        }

        String url =
                tab.webView.getUrl();

        if (url == null) {
            url = tab.url;
        }

        boolean https =
                url != null &&
                url.startsWith("https://");

        if (!https) {

            new AlertDialog.Builder(activity)
                    .setTitle("Not secure")
                    .setMessage(
                            "This page is not using HTTPS.\n\n" +
                            "URL:\n" +
                            safe(url))
                    .setPositiveButton(
                            "OK",
                            null)
                    .show();

            return;
        }

        if (tab.sslError) {

            new AlertDialog.Builder(activity)
                    .setTitle("Connection is not secure")
                    .setMessage(
                            "The SSL certificate could not " +
                            "be trusted.\n\n" +
                            "The page was blocked.")
                    .setPositiveButton(
                            "OK",
                            null)
                    .show();

            return;
        }

        SslCertificate certificate =
                tab.webView.getCertificate();

        if (certificate == null) {

            new AlertDialog.Builder(activity)
                    .setTitle("Certificate")
                    .setMessage(
                            "No certificate information " +
                            "is available.")
                    .setPositiveButton(
                            "OK",
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
                "Connection is secure\n\n");

        message.append("URL:\n");
        message.append(safe(url));

        message.append("\n\nIssued to:\n");

        if (issuedTo != null) {
            message.append(
                    safe(issuedTo.getCName()));
        } else {
            message.append("Unknown");
        }

        message.append("\n\nIssued by:\n");

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

            message.append("Unknown");
        }

        message.append("\n\nValid from:\n");

        message.append(
                validFrom == null
                        ? "Unknown"
                        : format.format(validFrom));

        message.append("\n\nValid until:\n");

        message.append(
                validTo == null
                        ? "Unknown"
                        : format.format(validTo));

        new AlertDialog.Builder(activity)
                .setTitle("Certificate")
                .setMessage(message.toString())
                .setPositiveButton(
                        "OK",
                        null)
                .show();
    }

    private String safe(String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "Unknown";
        }

        return value;
    }
}
