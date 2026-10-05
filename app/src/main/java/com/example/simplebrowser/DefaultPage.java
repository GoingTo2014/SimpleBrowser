package com.example.simplebrowser;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * Self-contained default/new-tab page.
 */
public class DefaultPage {

    private final MainActivity activity;

    public DefaultPage(MainActivity activity) {
        this.activity = activity;
    }

    public void show(BrowserTab tab) {

        tab.settingsPage = false;
        tab.errorPage = false;
        tab.historyPage = false;
        tab.defaultPage = true;
        tab.loading = false;
        tab.url = "";
        tab.pendingUrl = "browser://default";
        tab.hasLoaded = true;
        tab.title = "New Tab";

        WebView webView = tab.webView;

        webView.removeJavascriptInterface(
                "DefaultPage");

        webView.getSettings()
                .setJavaScriptEnabled(true);

        webView.addJavascriptInterface(
                new Bridge(tab),
                "DefaultPage");

        BrowserPage.load(
                webView,
                BrowserPage.DEFAULT,
                createHtml(tab.isIncognito),
                null);

        activity.updateTabTitle(tab);
        activity.setUrlText("");
        activity.updateSecurity(tab);
    }

    public void remove(BrowserTab tab) {

        if (tab == null) {
            return;
        }

        tab.webView.removeJavascriptInterface(
                "DefaultPage");
        tab.defaultPage = false;
    }

    private String createHtml(
            boolean incognito) {

        String background =
                incognito
                        ? "#202124"
                        : "#FFFFFF";

        String text =
                incognito
                        ? "#FFFFFF"
                        : "#202124";

        String secondary =
                incognito
                        ? "#B9B9B9"
                        : "#666666";

        return "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' " +
                "content='width=device-width,initial-scale=1'>" +
                "<style>" +
                "html,body{margin:0;min-height:100%;" +
                "font-family:sans-serif;" +
                "background:" + background + ";" +
                "color:" + text + ";}" +
                ".page{min-height:100vh;display:flex;" +
                "align-items:center;justify-content:center;" +
                "padding:24px;}" +
                ".box{width:100%;max-width:620px;text-align:center;}" +
                "h1{font-size:30px;margin:0 0 8px;}" +
                ".sub{color:" + secondary + ";" +
                "margin-bottom:24px;}" +
                "input{width:100%;height:48px;" +
                "padding:0 14px;font-size:17px;" +
                "box-sizing:border-box;border:1px solid #9AA0A6;" +
                "border-radius:6px;background:" +
                (incognito ? "#303134" : "#F7F7F7") +
                ";color:" + text + ";}" +
                "</style></head><body>" +
                "<div class='page'><div class='box'>" +
                "<h1>Simple Browser</h1>" +
                "<div class='sub'>Search or enter an address</div>" +
                "<input id='search' autofocus " +
                "placeholder='Search or enter an address' " +
                "onkeydown=\"go(event)\">" +
                "</div></div>" +
                "<script>" +
                "function go(e){" +
                "if(e.keyCode===13){" +
                "DefaultPage.search(document.getElementById('search').value);" +
                "}}" +
                "</script></body></html>";
    }

    private class Bridge {

        private final BrowserTab tab;

        Bridge(BrowserTab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public void search(String value) {

            activity.runOnUiThread(
                    () -> {

                        if (value != null &&
                                !value.trim().isEmpty()) {
                            activity.openUrlOrSearchForTab(
                                    tab,
                                    value.trim());
                        }
                    });
        }
    }
}
