package com.example.simplebrowser;

import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BrowserTab {

    public WebView webView;

    public LinearLayout tabView;
    public TextView titleView;
    public ImageButton closeButton;

    public String title = "New Tab";
    public String url = "";

    public boolean loading = false;
    public boolean sslError = false;
    public boolean settingsPage = false;

    /** Original WebView UA used when Desktop mode is off. */
    public String defaultUserAgent = "";

    public float dragStartX = 0;
    public boolean dragging = false;
}
