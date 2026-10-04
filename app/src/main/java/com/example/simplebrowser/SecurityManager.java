package com.example.simplebrowser;

import android.app.AlertDialog;
import android.net.http.SslCertificate;
import android.view.View;
import android.webkit.WebView;
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
                    android.R.drawable.ic_menu_manage
            );

            securityButton.setContentDescription(
                    "Browser settings"
            );

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
                    android.R.drawable.ic_dialog_alert
            );

            securityButton.setContentDescription(
                    "Connection is not secure"
            );

        } else if (https) {

            securityButton.setImageResource(
                    android.R.drawable.ic_lock_lock
            );

            securityButton.setContentDescription(
                    "Secure connection"
            );

        } else {

            securityButton.setImageResource(
                    android.R.drawable.ic_dialog_alert
            );

            securityButton.setContentDescription(
                    "Connection is not secure"
            );
        }
    }

    private void showInfo(
            BrowserTab tab) {

        String url =
                tab.webView.getUrl();

        if (url == null) {
            url = tab.url;
        }

        if (tab.settingsPage) {

            new AlertDialog.Builder(activity)
                    .setTitle("Browser Settings")
                    .setMessage(
                            "This is a built-in browser page."
                    )
                    .setPositiveButton(
                            "OK",
                            null)
                    .show();

            return;
        }

        boolean https =
                url != null &&
                url.startsWith("https://");

        if (!https) {

            new AlertDialog.Builder(activity)
                    .setTitle("Connection")
                    .setMessage(
                            "This page is not using HTTPS.\n\n" +
                            "The connection is not protected " +
                            "by HTTPS.\n\n" +
                            "URL:\n" +
                            safe(url)
                    )
                    .setPositiveButton(
                            "OK",
                            null)
                    .show();

            return;
        }

        if (tab.sslError) {

            new AlertDialog.Builder(activity)
                    .setTitle(
                            "Connection is not secure")
                    .setMessage(
                            "The site's SSL certificate " +
                            "could not be trusted.\n\n" +
                            "The page was blocked."
                    )
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
                            "is available for this page."
                    )
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

        StringBuilder text =
                new StringBuilder();

        text.append(
                "Connection is secure\n\n");

        text.append("URL:\n");
        text.append(safe(url));

        text.append("\n\nIssued to:\n");

        if (issuedTo != null) {
            text.append(
                    safe(issuedTo.getCName()));
        } else {
            text.append("Unknown");
        }

        text.append("\n\nIssued by:\n");

        if (issuedBy != null) {
            text.append(
                    safe(issuedBy.getCName()));

            if (issuedBy.getOName() != null &&
                    !issuedBy.getOName().isEmpty()) {

                text.append("\n");
                text.append(
                        issuedBy.getOName());
            }

        } else {
            text.append("Unknown");
        }

        text.append("\n\nValid from:\n");

        text.append(
                validFrom == null
                        ? "Unknown"
                        : format.format(validFrom));

        text.append("\n\nValid until:\n");

        text.append(
                validTo == null
                        ? "Unknown"
                        : format.format(validTo));

        new AlertDialog.Builder(activity)
                .setTitle("Certificate")
                .setMessage(text.toString())
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
