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

    public String settingsSection = "general";

    /** Original WebView UA used when Desktop mode is off. */
    public String defaultUserAgent = "";

    /*
     * Long-press tab dragging state. The Runnable is owned by
     * this tab so one tab cannot cancel another tab's timer.
     */
    public float dragStartX = 0f;
    public float dragStartY = 0f;
    public boolean dragging = false;
    public Runnable dragRunnable;
}
