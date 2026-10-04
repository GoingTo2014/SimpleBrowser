package com.example.simplebrowser;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

    private static final String HOME_URL = "https://www.google.com/";
    private static final String SEARCH_URL =
            "https://www.google.com/search?q=";

    private WebView webView;
    private EditText urlBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        webView = findViewById(R.id.webview);
        urlBox = findViewById(R.id.url);

        Button backButton = findViewById(R.id.back);
        Button forwardButton = findViewById(R.id.forward);
        Button homeButton = findViewById(R.id.home);
        Button reloadButton = findViewById(R.id.reload);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient());

        webView.loadUrl(HOME_URL);

        backButton.setOnClickListener(v -> {
            hideKeyboard();
            urlBox.clearFocus();

            if (webView.canGoBack()) {
                webView.goBack();
            }
        });

        forwardButton.setOnClickListener(v -> {
            hideKeyboard();
            urlBox.clearFocus();

            if (webView.canGoForward()) {
                webView.goForward();
            }
        });

        homeButton.setOnClickListener(v -> {
            hideKeyboard();
            urlBox.clearFocus();
            webView.loadUrl(HOME_URL);
        });

        reloadButton.setOnClickListener(v -> {
            hideKeyboard();
            urlBox.clearFocus();
            webView.reload();
        });

        webView.setOnTouchListener((v, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                hideKeyboard();
                urlBox.clearFocus();
            }

            return false;
        });

        urlBox.setOnEditorActionListener((v, actionId, event) -> {
            boolean enterPressed =
                    event != null &&
                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                    event.getAction() == KeyEvent.ACTION_DOWN;

            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                enterPressed) {

                String input = urlBox.getText().toString().trim();

                if (!input.isEmpty()) {
                    openUrlOrSearch(input);
                }

                return true;
            }

            return false;
        });
    }

    private void openUrlOrSearch(String input) {
        hideKeyboard();
        urlBox.clearFocus();

        if (isUrl(input)) {
            String url = input;

            if (!url.startsWith("http://") &&
                !url.startsWith("https://")) {
                url = "https://" + url;
            }

            webView.loadUrl(url);

        } else {
            String searchUrl = SEARCH_URL + urlEncode(input);
            webView.loadUrl(searchUrl);
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

    private String urlEncode(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            return text;
        }
    }

    private void hideKeyboard() {
        InputMethodManager manager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (manager != null) {
            manager.hideSoftInputFromWindow(urlBox.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            hideKeyboard();
            urlBox.clearFocus();
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
