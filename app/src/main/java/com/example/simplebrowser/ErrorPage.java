package com.example.simplebrowser;

import android.webkit.WebView;

/**
 * Self-contained network error page.
 */
public class ErrorPage {

    private final MainActivity activity;

    public ErrorPage(MainActivity activity) {
        this.activity = activity;
    }

    public void show(
            BrowserTab tab,
            String url,
            String description) {

        tab.errorPage = true;
        tab.loading = false;
        tab.url = url == null ? "" : url;
        tab.title = "Page unavailable";

        String safeUrl =
                escape(url);

        String safeDescription =
                escape(
                        description == null ||
                        description.trim().isEmpty()
                                ? "The page could not be loaded."
                                : description);

        String html =
                "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' " +
                "content='width=device-width,initial-scale=1'>" +
                "<style>" +
                "html,body{margin:0;min-height:100%;" +
                "font-family:sans-serif;background:#FFFFFF;" +
                "color:#202124;}" +
                ".page{min-height:100vh;display:flex;" +
                "align-items:center;justify-content:center;" +
                "padding:24px;}" +
                ".box{max-width:620px;width:100%;}" +
                "h1{font-size:27px;margin:0 0 10px;}" +
                "p{line-height:1.5;color:#5F6368;}" +
                ".url{margin-top:18px;padding:12px;" +
                "border:1px solid #DADCE0;border-radius:6px;" +
                "word-break:break-all;background:#F8F9FA;}" +
                "button{margin-top:18px;padding:10px 15px;" +
                "font-size:15px;border:0;border-radius:5px;" +
                "background:#202124;color:#FFFFFF;}" +
                "</style></head><body>" +
                "<div class='page'><div class='box'>" +
                "<h1>This page isn't working</h1>" +
                "<p>" + safeDescription + "</p>" +
                "<div class='url'>" + safeUrl + "</div>" +
                "<button onclick='goBack()'>Go back</button>" +
                "</div></div>" +
                "<script>" +
                "function goBack(){history.back();}" +
                "</script></body></html>";

        tab.webView.loadDataWithBaseURL(
                "https://browser.local/error",
                html,
                "text/html",
                "UTF-8",
                url);

        activity.updateTabTitle(tab);
        activity.setUrlText(url);
        activity.setLoading(false);
        activity.updateSecurity(tab);
    }

    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace(""", "&quot;")
                .replace("'", "&#39;");
    }
}
