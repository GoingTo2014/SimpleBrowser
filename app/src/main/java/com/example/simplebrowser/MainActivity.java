package com.example.simplebrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final String HOME_URL =
            "https://www.google.com/";

    private static final String SEARCH_URL =
            "https://www.google.com/search?q=";

    private WebView webViewContainerDummy;
    private FrameLayout webViewContainer;
    private LinearLayout tabsLayout;
    private EditText urlBox;
    private ProgressBar progressBar;
    private ImageButton securityButton;

    private final List<TabData> tabs = new ArrayList<>();
    private TabData activeTab;

    private static class TabData {
        WebView webView;
        Button tabButton;

        String title = "New Tab";
        String url = "";

        boolean loading = false;
        boolean sslError = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        webViewContainer = findViewById(R.id.webview_container);
        tabsLayout = findViewById(R.id.tabs);

        urlBox = findViewById(R.id.url);
        progressBar = findViewById(R.id.progress);
        securityButton = findViewById(R.id.security);

        Button backButton = findViewById(R.id.back);
        Button forwardButton = findViewById(R.id.forward);
        Button homeButton = findViewById(R.id.home);
        Button reloadButton = findViewById(R.id.reload);
        Button newTabButton = findViewById(R.id.new_tab);

        backButton.setOnClickListener(v -> {
            hideKeyboard();
            urlBox.clearFocus();

            if (activeTab != null &&
                activeTab.webView.canGoBack()) {
                activeTab.webView.goBack();
            }
        });

        forwardButton.setOnClickListener(v -> {
            hideKeyboard();
            urlBox.clearFocus();

            if (activeTab != null &&
                activeTab.webView.canGoForward()) {
                activeTab.webView.goForward();
            }
        });

        homeButton.setOnClickListener(v -> {
            hideKeyboard();
            urlBox.clearFocus();

            if (activeTab != null) {
                activeTab.webView.loadUrl(HOME_URL);
            }
        });

        reloadButton.setOnClickListener(v -> {
            hideKeyboard();
            urlBox.clearFocus();

            if (activeTab != null) {
                activeTab.webView.reload();
            }
        });

        newTabButton.setOnClickListener(v -> {
            hideKeyboard();
            urlBox.clearFocus();

            addTab(HOME_URL);
        });

        securityButton.setOnClickListener(v -> {
            if (activeTab != null) {
                showSecurityInfo(activeTab);
            }
        });

        urlBox.setOnEditorActionListener((v, actionId, event) -> {

            boolean enterPressed =
                    event != null &&
                    event.getKeyCode() ==
                            android.view.KeyEvent.KEYCODE_ENTER &&
                    event.getAction() ==
                            android.view.KeyEvent.ACTION_DOWN;

            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                enterPressed) {

                String input =
                        urlBox.getText().toString().trim();

                if (!input.isEmpty()) {
                    openUrlOrSearch(input);
                }

                return true;
            }

            return false;
        });

        addTab(HOME_URL);
    }

    private void addTab(String initialUrl) {

        TabData tab = new TabData();

        tab.webView = new WebView(this);

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);

        tab.webView.setLayoutParams(params);

        WebSettings settings =
                tab.webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Keep the behavior from the current version.
        tab.webView.setLayerType(
                WebView.LAYER_TYPE_HARDWARE, null);

        tab.webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageStarted(
                            WebView view,
                            String url,
                            android.graphics.Bitmap favicon) {

                        tab.url = url;
                        tab.loading = true;
                        tab.sslError = false;

                        if (tab == activeTab) {
                            urlBox.setText(url);

                            progressBar.setProgress(0);
                            progressBar.setVisibility(
                                    View.VISIBLE);
                        }

                        updateSecurityIcon(tab);
                    }

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url) {

                        tab.url = url;
                        tab.loading = false;

                        if (tab == activeTab) {
                            urlBox.setText(url);

                            progressBar.setProgress(100);

                            progressBar.postDelayed(() -> {
                                if (tab == activeTab &&
                                    !tab.loading) {
                                    progressBar.setVisibility(
                                            View.GONE);
                                }
                            }, 150);
                        }

                        updateSecurityIcon(tab);
                    }

                    @Override
                    public void onReceivedSslError(
                            WebView view,
                            SslErrorHandler handler,
                            SslError error) {

                        tab.sslError = true;

                        if (tab == activeTab) {
                            updateSecurityIcon(tab);
                        }

                        // Do not continue through an invalid
                        // SSL certificate.
                        handler.cancel();
                    }
                });

        tab.webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onProgressChanged(
                            WebView view,
                            int newProgress) {

                        if (tab == activeTab) {

                            progressBar.setProgress(
                                    newProgress);

                            if (newProgress < 100) {
                                progressBar.setVisibility(
                                        View.VISIBLE);
                            } else if (!tab.loading) {
                                progressBar.setVisibility(
                                        View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onReceivedTitle(
                            WebView view,
                            String title) {

                        if (title == null ||
                            title.trim().isEmpty()) {
                            tab.title = "New Tab";
                        } else {
                            tab.title = title.trim();
                        }

                        updateTabTitle(tab);
                    }
                });

        // Close the keyboard when touching the webpage.
        tab.webView.setOnTouchListener(
                (v, event) -> {

                    if (event.getAction() ==
                            MotionEvent.ACTION_DOWN) {

                        hideKeyboard();
                        urlBox.clearFocus();
                    }

                    return false;
                });

        tab.tabButton = new Button(this);
        tab.tabButton.setText(tab.title);
        tab.tabButton.setSingleLine(true);

        tab.tabButton.setOnClickListener(v -> {
            selectTab(tab);
        });

        // Long-press a tab to close it.
        tab.tabButton.setOnLongClickListener(v -> {

            if (tabs.size() == 1) {
                Toast.makeText(
                        MainActivity.this,
                        "At least one tab must stay open",
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle("Close tab?")
                    .setMessage(tab.title)
                    .setPositiveButton(
                            "Close",
                            (dialog, which) ->
                                    closeTab(tab))
                    .setNegativeButton(
                            "Cancel",
                            null)
                    .show();

            return true;
        });

        tabs.add(tab);

        tabsLayout.addView(tab.tabButton);

        webViewContainer.addView(tab.webView);

        selectTab(tab);

        tab.webView.loadUrl(initialUrl);
    }

    private void selectTab(TabData tab) {

        activeTab = tab;

        hideKeyboard();
        urlBox.clearFocus();

        for (TabData current : tabs) {

            if (current == tab) {
                current.webView.setVisibility(
                        View.VISIBLE);

                current.tabButton.setSelected(true);

            } else {
                current.webView.setVisibility(
                        View.GONE);

                current.tabButton.setSelected(false);
            }
        }

        String url = tab.webView.getUrl();

        if (url == null) {
            url = tab.url;
        }

        if (url == null) {
            url = "";
        }

        urlBox.setText(url);

        if (tab.loading) {
            progressBar.setVisibility(
                    View.VISIBLE);
        } else {
            progressBar.setVisibility(
                    View.GONE);
        }

        updateSecurityIcon(tab);
    }

    private void closeTab(TabData tab) {

        int index = tabs.indexOf(tab);

        if (index == -1) {
            return;
        }

        boolean wasActive =
                tab == activeTab;

        tabs.remove(tab);

        tabsLayout.removeView(
                tab.tabButton);

        webViewContainer.removeView(
                tab.webView);

        tab.webView.stopLoading();
        tab.webView.destroy();

        if (wasActive) {

            int newIndex =
                    Math.min(index, tabs.size() - 1);

            selectTab(tabs.get(newIndex));
        }
    }

    private void updateTabTitle(TabData tab) {

        String title = tab.title;

        if (title.length() > 18) {
            title =
                    title.substring(0, 18) + "...";
        }

        tab.tabButton.setText(title);
    }

    private void updateSecurityIcon(TabData tab) {

        String url = tab.webView.getUrl();

        if (url == null) {
            url = tab.url;
        }

        boolean https =
                url != null &&
                url.startsWith("https://");

        if (tab.sslError) {

            securityButton.setImageResource(
                    android.R.drawable.ic_dialog_alert);

            securityButton.setContentDescription(
                    "Connection is not secure");

            return;
        }

        if (https) {

            securityButton.setImageResource(
                    android.R.drawable.ic_lock_lock);

            securityButton.setContentDescription(
                    "Secure connection");

        } else {

            securityButton.setImageResource(
                    android.R.drawable.ic_dialog_alert);

            securityButton.setContentDescription(
                    "Connection is not secure");
        }
    }

    private void showSecurityInfo(TabData tab) {

        String url = tab.webView.getUrl();

        if (url == null) {
            url = tab.url;
        }

        boolean https =
                url != null &&
                url.startsWith("https://");

        if (!https) {

            new AlertDialog.Builder(this)
                    .setTitle("Connection")
                    .setMessage(
                            "This page is not using HTTPS.\n\n" +
                            "The connection is not protected " +
                            "by HTTPS.\n\n" +
                            "URL:\n" + safeText(url))
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        if (tab.sslError) {

            new AlertDialog.Builder(this)
                    .setTitle("Connection is not secure")
                    .setMessage(
                            "The site's SSL certificate " +
                            "could not be trusted.\n\n" +
                            "The page was blocked.")
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        SslCertificate certificate =
                tab.webView.getCertificate();

        if (certificate == null) {

            new AlertDialog.Builder(this)
                    .setTitle("Certificate")
                    .setMessage(
                            "No certificate information " +
                            "is available for this page.")
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        SslCertificate.DName issuedTo =
                certificate.getIssuedTo();

        SslCertificate.DName issuedBy =
                certificate.getIssuedBy();

        Date validFrom =
                certificate.getValidNotBeforeDate();

        Date validTo =
                certificate.getValidNotAfterDate();

        DateFormat dateFormat =
                DateFormat.getDateTimeInstance();

        StringBuilder info =
                new StringBuilder();

        info.append("Connection is secure\n\n");

        info.append("URL:\n");
        info.append(safeText(url));
        info.append("\n\n");

        info.append("Issued to:\n");

        if (issuedTo != null) {
            info.append(safeText(
                    issuedTo.getCName()));
        } else {
            info.append("Unknown");
        }

        info.append("\n\n");

        info.append("Issued by:\n");

        if (issuedBy != null) {
            info.append(safeText(
                    issuedBy.getCName()));

            if (issuedBy.getOName() != null &&
                !issuedBy.getOName().isEmpty()) {

                info.append("\n");
                info.append(
                        issuedBy.getOName());
            }

        } else {
            info.append("Unknown");
        }

        info.append("\n\n");

        info.append("Valid from:\n");
        info.append(validFrom == null
                ? "Unknown"
                : dateFormat.format(validFrom));

        info.append("\n\n");

        info.append("Valid until:\n");
        info.append(validTo == null
                ? "Unknown"
                : dateFormat.format(validTo));

        new AlertDialog.Builder(this)
                .setTitle("Certificate")
                .setMessage(info.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private String safeText(String value) {

        if (value == null ||
            value.trim().isEmpty()) {

            return "Unknown";
        }

        return value;
    }

    private void openUrlOrSearch(String input) {

        hideKeyboard();
        urlBox.clearFocus();

        if (activeTab == null) {
            return;
        }

        if (isUrl(input)) {

            if (!input.startsWith("http://") &&
                !input.startsWith("https://")) {

                input = "https://" + input;
            }

            activeTab.webView.loadUrl(input);

        } else {

            try {

                String encoded =
                        URLEncoder.encode(
                                input,
                                "UTF-8");

                activeTab.webView.loadUrl(
                        SEARCH_URL + encoded);

            } catch (Exception e) {

                activeTab.webView.loadUrl(
                        SEARCH_URL + input);
            }
        }
    }

    private boolean isUrl(String input) {

        if (input.startsWith("http://") ||
            input.startsWith("https://")) {

            return true;
        }

        if (input.contains(" ")) {
            return false;
        }

        return input.contains(".") ||
                input.startsWith("localhost:") ||
                input.startsWith("127.0.0.1:");
    }

    private void hideKeyboard() {

        InputMethodManager manager =
                (InputMethodManager)
                        getSystemService(
                                Context.INPUT_METHOD_SERVICE);

        if (manager != null) {

            manager.hideSoftInputFromWindow(
                    urlBox.getWindowToken(),
                    0);
        }
    }

    @Override
    public void onBackPressed() {

        if (activeTab != null &&
            activeTab.webView.canGoBack()) {

            hideKeyboard();
            urlBox.clearFocus();

            activeTab.webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
                }
