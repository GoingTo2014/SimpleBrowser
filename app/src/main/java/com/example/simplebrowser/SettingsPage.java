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
        tab.sslError = false;
        tab.url = "browser://settings";
        tab.title = "Settings";

        WebView webView =
                tab.webView;

        webView.removeJavascriptInterface(
                "Android");

        webView.getSettings()
                .setJavaScriptEnabled(true);

        webView.addJavascriptInterface(
                new SettingsBridge(),
                "Android");

        webView.loadDataWithBaseURL(
                "https://browser.local/",
                createHtml(),
                "text/html",
                "UTF-8",
                null);

        activity.updateTabTitle(tab);
        activity.settingsLoaded(tab);
    }

    public void remove(
            BrowserTab tab) {

        /*
         * Absolutely remove the bridge before
         * the WebView becomes a normal webpage.
         */
        tab.webView
                .removeJavascriptInterface(
                        "Android");

        tab.settingsPage = false;

        tab.webView.getSettings()
                .setJavaScriptEnabled(
                        settings
                                .isJavaScriptEnabled());
    }

    private String createHtml() {

        boolean dark =
                settings.isDarkMode();

        String background =
                dark
                        ? "#121212"
                        : "#FFFFFF";

        String sidebar =
                dark
                        ? "#1E1E1E"
                        : "#F3F3F3";

        String card =
                dark
                        ? "#1E1E1E"
                        : "#FFFFFF";

        String text =
                dark
                        ? "#FFFFFF"
                        : "#202124";

        String secondary =
                dark
                        ? "#BDBDBD"
                        : "#666666";

        String border =
                dark
                        ? "#333333"
                        : "#DDDDDD";

        String accent =
                settings.getAccentColor();

        return "<!DOCTYPE html>" +

                "<html>" +

                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1'>" +

                "<style>" +

                "*{" +
                "box-sizing:border-box;" +
                "}" +

                "html,body{" +
                "margin:0;" +
                "padding:0;" +
                "min-height:100%;" +
                "font-family:sans-serif;" +
                "background:" +
                background +
                ";" +
                "color:" +
                text +
                ";" +
                "}" +

                ".layout{" +
                "display:flex;" +
                "min-height:100vh;" +
                "}" +

                ".sidebar{" +
                "width:200px;" +
                "background:" +
                sidebar +
                ";" +
                "border-right:1px solid " +
                border +
                ";" +
                "padding:18px 0;" +
                "flex-shrink:0;" +
                "}" +

                ".brand{" +
                "font-size:20px;" +
                "font-weight:bold;" +
                "padding:0 18px 20px;" +
                "}" +

                ".nav{" +
                "padding:10px 18px;" +
                "font-size:14px;" +
                "color:" +
                secondary +
                ";" +
                "}" +

                ".nav:hover{" +
                "background:" +
                (dark ? "#2A2A2A" : "#E8E8E8") +
                ";cursor:pointer;" +
                "}" +

                ".nav.active{" +
                "color:" +
                accent +
                ";" +
                "font-weight:bold;" +
                "background:" +
                background +
                ";" +
                "}" +

                ".content{" +
                "width:100%;" +
                "max-width:780px;" +
                "padding:28px;" +
                "}" +

                "h1{" +
                "font-size:27px;" +
                "margin:0;" +
                "}" +

                ".subtitle{" +
                "color:" +
                secondary +
                ";" +
                "margin-top:5px;" +
                "margin-bottom:25px;" +
                "}" +

                "h2{" +
                "font-size:18px;" +
                "margin:25px 0 9px;" +
                "}" +

                ".card{" +
                "background:" +
                card +
                ";" +
                "border:1px solid " +
                border +
                ";" +
                "border-radius:7px;" +
                "overflow:hidden;" +
                "}" +

                ".row{" +
                "padding:14px 16px;" +
                "border-bottom:1px solid " +
                border +
                ";" +
                "}" +

                ".row:last-child{" +
                "border-bottom:0;" +
                "}" +

                ".switchrow{" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:space-between;" +
                "}" +

                ".title{" +
                "font-size:15px;" +
                "}" +

                ".description{" +
                "font-size:12px;" +
                "color:" +
                secondary +
                ";" +
                "margin-top:3px;" +
                "}" +

                "select{" +
                "width:100%;" +
                "margin-top:8px;" +
                "padding:7px;" +
                "font-size:15px;" +
                "}" +

                "input[type=color]{" +
                "width:55px;" +
                "height:32px;" +
                "padding:0;" +
                "border:0;" +
                "background:transparent;" +
                "}" +

                "button{" +
                "font-size:14px;" +
                "padding:8px 12px;" +
                "}" +

                "@media(max-width:600px){" +

                ".sidebar{" +
                "width:135px;" +
                "}" +

                ".content{" +
                "padding:20px 15px;" +
                "}" +

                ".brand{" +
                "padding-left:13px;" +
                "}" +

                ".nav{" +
                "padding-left:13px;" +
                "}" +

                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='layout'>" +

                "<div class='sidebar'>" +

                "<div class='brand'>" +
                "Simple Browser" +
                "</div>" +

                "<div id='nav-general' class='nav active' onclick=\"showSection('general')\">" +
                "General" +
                "</div>" +

                "<div id='nav-privacy' class='nav' onclick=\"showSection('privacy')\">" +
                "Privacy & Security" +
                "</div>" +

                "<div id='nav-websites' class='nav' onclick=\"showSection('websites')\">" +
                "Websites" +
                "</div>" +

                "<div id='nav-appearance' class='nav' onclick=\"showSection('appearance')\">" +
                "Appearance" +
                "</div>" +

                "</div>" +

                "<div class='content'>" +

                "<h1>Settings</h1>" +

                "<div class='subtitle'>" +
                "Configure Simple Browser" +
                "</div>" +

                "<div id='section-general' class='section'><h2>General</h2>" +

                "<div class='card'>" +

                "<div class='row'>" +

                "<div class='title'>" +
                "Home page" +
                "</div>" +

                "<select onchange=\"" +
                "Android.setHome(this.value)\">" +

                homeOption(
                        "https://www.google.com/",
                        "Google") +

                homeOption(
                        "https://www.bing.com/",
                        "Bing") +

                homeOption(
                        "https://duckduckgo.com/",
                        "DuckDuckGo") +

                homeOption(
                        "about:blank",
                        "Blank page") +

                "</select>" +

                "</div>" +

                "<div class='row'>" +

                "<div class='title'>" +
                "Search engine" +
                "</div>" +

                "<select onchange=\"" +
                "Android.setSearch(this.value)\">" +

                searchOption(
                        "google",
                        "Google") +

                searchOption(
                        "bing",
                        "Bing") +

                searchOption(
                        "duckduckgo",
                        "DuckDuckGo") +

                searchOption(
                        "yahoo",
                        "Yahoo") +

                "</select>" +

                "</div>" +

                "</div>" +

                "</div><div id='section-websites' class='section'><h2>Websites</h2>" +

                "<div class='card'>" +

                settingRow(
                        "JavaScript",
                        "Allow websites to run JavaScript",
                        "javascript",
                        settings.isJavaScriptEnabled()) +

                settingRow(
                        "Pop-ups",
                        "Allow websites to open new windows",
                        "popups",
                        settings.arePopupsEnabled()) +

                settingRow(
                        "Cookies",
                        "Allow websites to store cookies",
                        "cookies",
                        settings.areCookiesEnabled()) +

                settingRow(
                        "Website storage",
                        "Allow websites to use local storage",
                        "storage",
                        settings.isStorageEnabled()) +

                "</div>" +

                "</div><div id='section-appearance' class='section'><h2>Appearance</h2>" +

                "<div class='card'>" +

                settingRow(
                        "Dark mode",
                        "Use a dark browser theme",
                        "dark",
                        settings.isDarkMode()) +

                "<div class='row switchrow'>" +

                "<div>" +

                "<div class='title'>" +
                "Browser color" +
                "</div>" +

                "<div class='description'>" +
                "Accent color for the browser" +
                "</div>" +

                "</div>" +

                "<input type='color' " +
                "value='" +
                accent +
                "' " +
                "onchange=\"" +
                "Android.setColor(this.value)\">" +

                "</div>" +

                "</div>" +

                "</div><div id='section-privacy' class='section'><h2>Privacy</h2>" +

                "<div class='card'>" +

                "<div class='row'>" +

                "<button onclick=\"" +
                "Android.clearData()\">" +

                "Clear browsing data" +

                "</button>" +

                "</div>" +

                "<div class='row'>" +

                "<button onclick=\"" +
                "Android.resetSettings()\">" +

                "Restore default settings" +

                "</button>" +

                "</div>" +

                "</div>" +

                "</div>" +

                "</div>" +

                "<script>" +
                "function showSection(name){" +
                "var names=['general','privacy','websites','appearance'];" +
                "for(var i=0;i<names.length;i++){" +
                "var n=names[i];" +
                "var section=document.getElementById('section-'+n);" +
                "var nav=document.getElementById('nav-'+n);" +
                "if(section) section.style.display=(n===name?'block':'none');" +
                "if(nav) nav.className='nav'+(n===name?' active':'');" +
                "}" +
                "}" +
                "showSection('general');" +
                "</script>" +

                "</body>" +

                "</html>";
    }

    private String homeOption(
            String value,
            String label) {

        String selected =
                value.equals(
                        settings.getHomePage())
                        ? " selected"
                        : "";

        return "<option value='" +
                value +
                "'" +
                selected +
                ">" +
                label +
                "</option>";
    }

    private String searchOption(
            String value,
            String label) {

        String selected =
                value.equals(
                        settings.getSearchEngine())
                        ? " selected"
                        : "";

        return "<option value='" +
                value +
                "'" +
                selected +
                ">" +
                label +
                "</option>";
    }

    private String settingRow(
            String title,
            String description,
            String name,
            boolean enabled) {

        String checked =
                enabled
                        ? " checked"
                        : "";

        return "<div class='row switchrow'>" +

                "<div>" +

                "<div class='title'>" +
                title +
                "</div>" +

                "<div class='description'>" +
                description +
                "</div>" +

                "</div>" +

                "<input type='checkbox'" +
                checked +
                " onchange=\"" +
                "Android.setSetting('" +
                name +
                "',this.checked)\">" +

                "</div>";
    }

    private class SettingsBridge {

        @JavascriptInterface
        public void setHome(
                String value) {

            settings.setHomePage(value);
        }

        @JavascriptInterface
        public void setSearch(
                String value) {

            settings.setSearchEngine(value);
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

                if ("dark".equals(name)) {

                    /*
                     * Recreate the Activity using
                     * the correct Android theme.
                     *
                     * No CSS inversion is used.
                     */
                    activity.changeDarkMode();

                } else {

                    activity.applyWebsiteSettings();
                }
            });
        }

        @JavascriptInterface
        public void setColor(
                String color) {

            /*
             * Only the actual accent color is stored.
             * No website colors are inverted.
             */
            settings.setAccentColor(color);

            activity.runOnUiThread(
                    () -> {

                activity.applyBrowserAppearance();

                BrowserTab tab =
                        activity.getActiveTab();

                if (tab != null &&
                        tab.settingsPage) {

                    show(tab);
                }
            });
        }

        @JavascriptInterface
        public void clearData() {

            activity.runOnUiThread(
                    () -> {

                for (BrowserTab tab :
                        activity
                                .getTabManager()
                                .getTabs()) {

                    tab.webView.clearCache(true);
                    tab.webView.clearHistory();
                }

                android.webkit.CookieManager
                        .getInstance()
                        .removeAllCookie();

                Toast.makeText(
                        activity,
                        "Browsing data cleared",
                        Toast.LENGTH_SHORT)
                        .show();
            });
        }

        @JavascriptInterface
        public void resetSettings() {

            activity.runOnUiThread(
                    () -> {

                settings.reset();

                activity.changeDarkMode();
            });
        }
    }
}
