package com.example.simplebrowser;

import android.app.Activity;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import java.net.URLEncoder;

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

        urlBox.setOnEditorActionListener((v, actionId, event) -> {

            boolean enterPressed =
                    event != null &&
                    event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER &&
                    event.getAction() == android.view.KeyEvent.ACTION_DOWN;

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
            if (!input.startsWith("http://") &&
                !input.startsWith("https://")) {

                input = "https://" + input;
            }

            webView.loadUrl(input);

        } else {
            try {
                String encoded =
                        URLEncoder.encode(input, "UTF-8");

                webView.loadUrl(SEARCH_URL + encoded);

            } catch (Exception e) {
                webView.loadUrl(SEARCH_URL + input);
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
                (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);

        if (manager != null) {
            manager.hideSoftInputFromWindow(
                    urlBox.getWindowToken(), 0);
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
