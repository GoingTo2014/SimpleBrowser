package com.example.simplebrowser;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

public class SettingsPage {

    private final MainActivity activity;
    private final BrowserSettings settings;

    public SettingsPage(
            MainActivity activity,
            BrowserSettings settings) {

        this.activity = activity;
        this.settings = settings;
    }

    public void show(
            BrowserTab tab) {

        tab.settingsPage = true;
        tab.loading = false;
        tab.url = "browser://settings";
        tab.title = "Settings";

        WebView webView =
                tab.webView;

        /*
         * The bridge is installed ONLY here.
         * Normal websites never receive it.
         */
        webView.addJavascriptInterface(
                new SettingsBridge(),
                "Android"
        );

        webView.getSettings()
                .setJavaScriptEnabled(true);

        webView.loadDataWithBaseURL(
                "https://browser.local/",
                createHtml(),
                "text/html",
                "UTF-8",
                null
        );

        activity.updateTabTitle(tab);
        activity.settingsLoaded(tab);
    }

    public void remove(
            BrowserTab tab) {

        tab.webView
                .removeJavascriptInterface(
                        "Android"
                );

        tab.settingsPage = false;
    }

    private String createHtml() {

        String dark =
                settings.isDarkMode()
                        ? "checked"
                        : "";

        String javascript =
                settings.isJavaScriptEnabled()
                        ? "checked"
                        : "";

        String popups =
                settings.arePopupsEnabled()
                        ? "checked"
                        : "";

        String cookies =
                settings.areCookiesEnabled()
                        ? "checked"
                        : "";

        String storage =
                settings.isStorageEnabled()
                        ? "checked"
                        : "";

        String search =
                settings.getSearchEngine();

        String home =
                settings.getHomePage();

        String background =
                settings.isDarkMode()
                        ? "#000000"
                        : "#ffffff";

        String text =
                settings.isDarkMode()
                        ? "#ffffff"
                        : "#000000";

        String border =
                settings.isDarkMode()
                        ? "#333333"
                        : "#dddddd";

        return "<!DOCTYPE html>" +

                "<html>" +

                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1'>" +

                "<style>" +

                "body{" +
                "font-family:sans-serif;" +
                "margin:0;" +
                "padding:20px;" +
                "background:" +
                background +
                ";" +
                "color:" +
                text +
                ";" +
                "}" +

                "h1{" +
                "font-size:28px;" +
                "}" +

                "h2{" +
                "font-size:20px;" +
                "margin-top:28px;" +
                "}" +

                ".setting{" +
                "padding:16px 0;" +
                "border-bottom:1px solid " +
                border +
                ";" +
                "}" +

                "select{" +
                "font-size:16px;" +
                "padding:8px;" +
                "margin-top:8px;" +
                "max-width:100%;" +
                "}" +

                "label{" +
                "font-size:17px;" +
                "}" +

                "button{" +
                "font-size:16px;" +
                "padding:8px 14px;" +
                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<h1>Browser Settings</h1>" +

                "<h2>General</h2>" +

                "<div class='setting'>" +

                "<label>Home page</label><br>" +

                "<select onchange=" +
                "\"Android.setHome(this.value)\">" +

                homeOption(
                        "https://www.google.com/",
                        "Google",
                        home) +

                homeOption(
                        "https://www.bing.com/",
                        "Bing",
                        home) +

                homeOption(
                        "https://duckduckgo.com/",
                        "DuckDuckGo",
                        home) +

                homeOption(
                        "about:blank",
                        "Blank page",
                        home) +

                "</select>" +

                "</div>" +

                "<div class='setting'>" +

                "<label>Search engine</label><br>" +

                "<select onchange=" +
                "\"Android.setSearch(this.value)\">" +

                searchOption(
                        "google",
                        "Google",
                        search) +

                searchOption(
                        "bing",
                        "Bing",
                        search) +

                searchOption(
                        "duckduckgo",
                        "DuckDuckGo",
                        search) +

                searchOption(
                        "yahoo",
                        "Yahoo",
                        search) +

                "</select>" +

                "</div>" +

                "<h2>Website settings</h2>" +

                checkbox(
                        "JavaScript",
                        "javascript",
                        javascript) +

                checkbox(
                        "Pop-up windows",
                        "popups",
                        popups) +

                checkbox(
                        "Cookies",
                        "cookies",
                        cookies) +

                checkbox(
                        "Website storage",
                        "storage",
                        storage) +

                "<h2>Appearance</h2>" +

                checkbox(
                        "Black mode",
                        "dark",
                        dark) +

                "<div class='setting'>" +

                "<label>Browser color</label><br>" +

                "<input type='color' " +
                "value='" +
                settings.getAccentColor() +
                "' " +
                "onchange=" +
                "\"Android.setColor(this.value)\">" +

                "</div>" +

                "<h2>Privacy</h2>" +

                "<div class='setting'>" +

                "<button onclick=" +
                "\"Android.clearData()\">" +

                "Clear browser data" +

                "</button>" +

                "</div>" +

                "</body>" +

                "</html>";
    }

    private String homeOption(
            String value,
            String text,
            String current) {

        String selected =
                value.equals(current)
                        ? " selected"
                        : "";

        return "<option value='" +
                value +
                "'" +
                selected +
                ">" +
                text +
                "</option>";
    }

    private String searchOption(
            String value,
            String text,
            String current) {

        String selected =
                value.equals(current)
                        ? " selected"
                        : "";

        return "<option value='" +
                value +
                "'" +
                selected +
                ">" +
                text +
                "</option>";
    }

    private String checkbox(
            String text,
            String name,
            String checked) {

        return "<div class='setting'>" +

                "<label>" +

                "<input type='checkbox' " +
                checked +
                " onchange=\"" +
                "Android.setSetting('" +
                name +
                "',this.checked)\">" +

                " " +
                text +

                "</label>" +

                "</div>";
    }

    private class SettingsBridge {

        @JavascriptInterface
        public void setHome(
                String home) {

            settings.setHomePage(home);
        }

        @JavascriptInterface
        public void setSearch(
                String search) {

            settings.setSearchEngine(search);
        }

        @JavascriptInterface
        public void setSetting(
                String name,
                boolean value) {

            settings.setBoolean(
                    name,
                    value);

            activity.runOnUiThread(
                    () -> {

                activity.applyWebsiteSettings();

                if ("dark".equals(name)) {

                    BrowserTab tab =
                            activity.getActiveTab();

                    if (tab != null &&
                            tab.settingsPage) {

                        show(tab);
                    }
                }
            });
        }

        @JavascriptInterface
        public void setColor(
                String color) {

            settings.setAccentColor(color);

            activity.runOnUiThread(
                    () -> activity
                            .applyBrowserAppearance());
        }

        @JavascriptInterface
        public void clearData() {

            activity.runOnUiThread(
                    () -> {

                android.webkit.CookieManager
                        .getInstance()
                        .removeAllCookie();

                BrowserTab tab =
                        activity.getActiveTab();

                if (tab != null) {

                    tab.webView
                            .clearCache(true);

                    tab.webView
                            .clearHistory();
                }

                Toast.makeText(
                        activity,
                        "Browser data cleared",
                        Toast.LENGTH_SHORT)
                        .show();
            });
        }
    }
              }
