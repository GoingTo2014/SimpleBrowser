package com.example.simplebrowser;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

    private static final String HOME_URL = "https://www.google.com/";

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

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                urlBox.setText(url);
                urlBox.setSelection(urlBox.length());
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.loadUrl(HOME_URL);

        backButton.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });

        forwardButton.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });

        homeButton.setOnClickListener(v -> webView.loadUrl(HOME_URL));

        reloadButton.setOnClickListener(v -> webView.reload());

        urlBox.setOnEditorActionListener((v, actionId, event) -> {
            boolean enterPressed =
                    event != null &&
                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                    event.getAction() == KeyEvent.ACTION_DOWN;

            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                enterPressed) {

                String url = urlBox.getText().toString().trim();

                if (!url.isEmpty()) {
                    if (!url.startsWith("http://") &&
                        !url.startsWith("https://")) {
                        url = "https://" + url;
                    }

                    webView.loadUrl(url);
                }

                return true;
            }

            return false;
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
