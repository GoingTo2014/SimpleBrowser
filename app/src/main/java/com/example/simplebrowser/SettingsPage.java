package com.example.simplebrowser;

import android.graphics.Color;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

/**
 * Built-in settings UI.
 *
 * The page is self-contained HTML/CSS/JS so it works on the
 * Android 4.4 WebView without external web dependencies.
 */
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

        int accentColor =
                ColorUtils.parseColor(
                        settings.getAccentColor(),
                        Color.rgb(
                                63, 81, 181));

        int accentText =
                ColorUtils
                        .getReadableTextColor(
                                accentColor);

        int accentSoft =
                ColorUtils.mix(
                        accentColor,
                        accentText,
                        0.16f);

        int accentBorder =
                ColorUtils.mix(
                        accentColor,
                        Color.WHITE,
                        0.28f);

        String accent =
                ColorUtils.toHex(accentColor);

        String accentTextHex =
                ColorUtils.toHex(accentText);

        String accentSoftHex =
                ColorUtils.toHex(accentSoft);

        String accentBorderHex =
                ColorUtils.toHex(accentBorder);

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +

                "<meta name='viewport' " +
                "content='width=device-width," +
                "initial-scale=1'>" +

                "<style>" +

                "*{box-sizing:border-box;}" +

                "html,body{" +
                "margin:0;" +
                "padding:0;" +
                "min-height:100%;" +
                "font-family:sans-serif;" +
                "background:#F5F6F8;" +
                "color:#202124;" +
                "}" +

                ".layout{" +
                "display:flex;" +
                "min-height:100vh;" +
                "}" +

                ".sidebar{" +
                "width:205px;" +
                "background:" +
                accent +
                ";" +
                "padding:18px 0;" +
                "flex-shrink:0;" +
                "color:" +
                accentTextHex +
                ";" +
                "}" +

                ".brand{" +
                "font-size:20px;" +
                "font-weight:bold;" +
                "padding:0 18px 20px;" +
                "}" +

                ".nav{" +
                "padding:11px 18px;" +
                "font-size:14px;" +
                "color:" +
                accentTextHex +
                ";" +
                "opacity:.86;" +
                "cursor:pointer;" +
                "}" +

                ".nav:hover{" +
                "background:" +
                accentSoftHex +
                ";" +
                "opacity:1;" +
                "}" +

                ".nav.active{" +
                "background:" +
                accentSoftHex +
                ";" +
                "opacity:1;" +
                "font-weight:bold;" +
                "border-left:3px solid " +
                accentTextHex +
                ";" +
                "padding-left:15px;" +
                "}" +

                ".content{" +
                "width:100%;" +
                "max-width:820px;" +
                "padding:28px;" +
                "}" +

                "h1{" +
                "font-size:28px;" +
                "margin:0;" +
                "color:" +
                accent +
                ";" +
                "}" +

                ".subtitle{" +
                "color:#666;" +
                "margin:5px 0 25px;" +
                "}" +

                "h2{" +
                "font-size:18px;" +
                "margin:0 0 9px;" +
                "}" +

                ".card{" +
                "background:#FFFFFF;" +
                "border:1px solid #DDDDDD;" +
                "border-radius:8px;" +
                "overflow:hidden;" +
                "margin-bottom:25px;" +
                "}" +

                ".row{" +
                "padding:15px 16px;" +
                "border-bottom:1px solid #E5E5E5;" +
                "}" +

                ".row:last-child{border-bottom:0;}" +

                ".switchrow{" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:space-between;" +
                "gap:12px;" +
                "}" +

                ".title{font-size:15px;}" +

                ".description{" +
                "font-size:12px;" +
                "color:#666;" +
                "margin-top:3px;" +
                "}" +

                "select{" +
                "width:100%;" +
                "margin-top:8px;" +
                "padding:8px;" +
                "font-size:15px;" +
                "}" +

                "input[type=color]{" +
                "width:60px;" +
                "height:34px;" +
                "padding:0;" +
                "border:1px solid " +
                accentBorderHex +
                ";" +
                "background:#FFFFFF;" +
                "}" +

                "input[type=checkbox]{width:20px;height:20px;}" +

                "button{" +
                "font-size:14px;" +
                "padding:9px 13px;" +
                "background:" +
                accent +
                ";" +
                "color:" +
                accentTextHex +
                ";" +
                "border:0;" +
                "border-radius:5px;" +
                "}" +

                ".section{display:none;}" +

                ".section.active{display:block;}" +

                "@media(max-width:600px){" +

                ".sidebar{width:145px;}" +

                ".content{padding:20px 15px;}" +

                ".brand{padding-left:13px;}" +

                ".nav{padding-left:13px;}" +

                ".nav.active{padding-left:10px;}" +

                "}" +

                "</style>" +

                "</head>" +

                "<body>" +

                "<div class='layout'>" +

                "<div class='sidebar'>" +

                "<div class='brand'>Simple Browser</div>" +

                "<div id='nav-general' class='nav active' " +
                "onclick=\"showSection('general')\">" +
                "General</div>" +

                "<div id='nav-privacy' class='nav' " +
                "onclick=\"showSection('privacy')\">" +
                "Privacy &amp; Security</div>" +

                "<div id='nav-websites' class='nav' " +
                "onclick=\"showSection('websites')\">" +
                "Websites</div>" +

                "<div id='nav-appearance' class='nav' " +
                "onclick=\"showSection('appearance')\">" +
                "Appearance</div>" +

                "</div>" +

                "<div class='content'>" +

                "<h1>Settings</h1>" +

                "<div class='subtitle'>Configure Simple Browser</div>" +

                "<div id='section-general' class='section active'>" +

                "<h2>General</h2>" +

                "<div class='card'>" +

                "<div class='row'>" +
                "<div class='title'>Home page</div>" +
                "<select onchange=\"Android.setHome(this.value)\">" +

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
                "<div class='title'>Search engine</div>" +
                "<select onchange=\"Android.setSearch(this.value)\">" +

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

                "<div class='row switchrow'>" +

                "<div>" +
                "<div class='title'>Desktop mode</div>" +
                "<div class='description'>" +
                "Ask websites to use their desktop layout and desktop user-agent." +
                "</div>" +
                "</div>" +

                "<input type='checkbox'" +
                (settings.isDesktopMode()
                        ? " checked"
                        : "") +
                " onchange=\"Android.setSetting('desktop_mode',this.checked)\">" +

                "</div>" +

                "<div class='row'>" +

                "<div class='title'>Default browser</div>" +

                "<div class='description'>" +
                "Ask Android to choose which installed browser should open web links." +
                "</div>" +

                "<br>" +

                "<button onclick=\"Android.chooseDefaultBrowser()\">" +
                "Choose browser" +
                "</button>" +

                "</div>" +

                "<div class='row'>" +

                "<div class='title'>Open local HTML file</div>" +

                "<div class='description'>" +
                "Choose an HTML file stored on the device." +
                "</div>" +

                "<br>" +

                "<button onclick=\"Android.openLocalFile()\">" +
                "Open HTML file" +
                "</button>" +

                "</div>" +

                "</div>" +
                "</div>" +

                "<div id='section-privacy' class='section'>" +

                "<h2>Privacy &amp; Security</h2>" +

                "<div class='card'>" +

                "<div class='row'>" +

                "<button onclick=\"Android.clearData()\">" +
                "Clear browsing data" +
                "</button>" +

                "</div>" +

                "<div class='row'>" +

                "<button onclick=\"Android.resetSettings()\">" +
                "Restore default settings" +
                "</button>" +

                "</div>" +

                "</div>" +
                "</div>" +

                "<div id='section-websites' class='section'>" +

                "<h2>Websites</h2>" +

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

                "</div>" +

                "<div id='section-appearance' class='section'>" +

                "<h2>Appearance</h2>" +

                "<div class='card'>" +

                "<div class='row switchrow'>" +

                "<div>" +
                "<div class='title'>Browser color</div>" +
                "<div class='description'>" +
                "Changes the browser toolbar and this settings interface." +
                "</div>" +
                "</div>" +

                "<input type='color' " +
                "value='" + accent + "' " +
                "onchange=\"Android.setColor(this.value)\">" +

                "</div>" +

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
                "if(section){" +
                "section.className='section'+(n===name?' active':'');" +
                "}" +
                "if(nav){" +
                "nav.className='nav'+(n===name?' active':'');" +
                "}" +
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
                (enabled ? " checked" : "") +
                " onchange=\"Android.setSetting('" +
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

                        if ("desktop_mode"
                                .equals(name)) {

                            activity.applyDesktopMode();

                        } else {

                            activity.applyWebsiteSettings();
                        }
                    });
        }

        @JavascriptInterface
        public void setColor(
                String color) {

            int parsed =
                    ColorUtils.parseColor(
                            color,
                            Color.rgb(
                                    63, 81, 181));

            settings.setAccentColor(
                    ColorUtils.toHex(parsed));

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

                        activity.applyWebsiteSettings();
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
        public void chooseDefaultBrowser() {
            activity.runOnUiThread(
                    () -> activity.chooseDefaultBrowser());
        }

        @JavascriptInterface
        public void openLocalFile() {
            activity.runOnUiThread(
                    () -> activity.openLocalFilePicker());
        }
    }
}
