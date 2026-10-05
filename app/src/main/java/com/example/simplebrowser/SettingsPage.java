package com.example.simplebrowser;

import android.graphics.Color;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import android.widget.Toast;

/**
 * Self-contained browser settings page.
 *
 * Sections have their own browser://settings/<section>
 * addresses while sharing one lightweight HTML renderer.
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

        show(tab, "general");
    }

    public void show(
            BrowserTab tab,
            String section) {

        section =
                normalizeSection(section);

        tab.settingsPage = true;
        tab.loading = false;
        tab.sslError = false;
        tab.settingsSection = section;
        tab.url =
                activity.getSettingsUrl(
                        section);
        tab.title = "Settings";

        WebView webView =
                tab.webView;

        webView.removeJavascriptInterface(
                "Android");

        webView.getSettings()
                .setJavaScriptEnabled(true);

        webView.addJavascriptInterface(
                new SettingsBridge(tab),
                "Android");

        webView.loadDataWithBaseURL(
                "https://browser.local/settings/" +
                        section,
                createHtml(
                        section,
                        tab.isIncognito),
                "text/html",
                "UTF-8",
                null);

        activity.updateTabTitle(tab);
        activity.settingsLoaded(tab);
    }

    public void restore(
            BrowserTab tab,
            String section) {

        section =
                normalizeSection(section);

        tab.settingsPage = true;
        tab.loading = false;
        tab.sslError = false;
        tab.settingsSection = section;
        tab.url =
                activity.getSettingsUrl(
                        section);
        tab.title = "Settings";

        WebView webView =
                tab.webView;

        webView.removeJavascriptInterface(
                "Android");

        webView.getSettings()
                .setJavaScriptEnabled(true);

        webView.addJavascriptInterface(
                new SettingsBridge(tab),
                "Android");

        activity.updateTabTitle(tab);
    }

    private String normalizeSection(
            String section) {

        if ("websites".equals(section) ||
                "appearance".equals(section) ||
                "privacy-security"
                        .equals(section) ||
                "advanced".equals(section)) {

            return section;
        }

        return "general";
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

    private String createHtml(
            String currentSection,
            boolean incognito) {

        int accentColor =
                ColorUtils.parseColor(
                        settings.getAccentColor(),
                        Color.WHITE);

        int sidebarColor =
                incognito
                        ? Color.rgb(
                                30, 30, 32)
                        : accentColor;

        int accentText =
                ColorUtils.getReadableTextColor(
                        sidebarColor);

        int accentSoft =
                incognito
                        ? Color.rgb(
                                55, 56, 60)
                        : ColorUtils.mix(
                                accentColor,
                                Color.WHITE,
                                0.16f);

        int accentSoftText =
                ColorUtils.getReadableTextColor(
                        accentSoft);

        int accentContent =
                incognito
                        ? Color.rgb(
                                32, 33, 36)
                        : ColorUtils.mix(
                                accentColor,
                                Color.WHITE,
                                0.94f);

        int cardBackground =
                incognito
                        ? Color.rgb(
                                48, 49, 52)
                        : ColorUtils.mix(
                                accentColor,
                                Color.WHITE,
                                0.90f);

        int cardText =
                incognito
                        ? Color.WHITE
                        : ColorUtils.getReadableTextColor(
                                cardBackground);

        int headingColor =
                incognito
                        ? Color.WHITE
                        : ColorUtils.ensureContrast(
                                accentColor,
                                accentContent,
                                4.5d);

        int accentBorder =
                incognito
                        ? Color.rgb(
                                95, 99, 104)
                        : ColorUtils.ensureContrast(
                                accentColor,
                                cardBackground,
                                2.5d);

        int secondaryText =
                incognito
                        ? Color.rgb(
                                190, 190, 195)
                        : ColorUtils.ensureContrast(
                                Color.rgb(95, 95, 95),
                                cardBackground,
                                4.5d);

        int contentSecondaryText =
                incognito
                        ? Color.rgb(
                                190, 190, 195)
                        : ColorUtils.ensureContrast(
                                Color.rgb(95, 95, 95),
                                accentContent,
                                4.5d);

        String accent =
                ColorUtils.toHex(accentColor);

        String accentTextHex =
                ColorUtils.toHex(accentText);

        String accentSoftHex =
                ColorUtils.toHex(accentSoft);

        String accentSoftTextHex =
                ColorUtils.toHex(accentSoftText);

        String accentContentHex =
                ColorUtils.toHex(accentContent);

        String cardBackgroundHex =
                ColorUtils.toHex(cardBackground);

        String cardTextHex =
                ColorUtils.toHex(cardText);

        String headingColorHex =
                ColorUtils.toHex(headingColor);

        String accentBorderHex =
                ColorUtils.toHex(accentBorder);

        String secondaryTextHex =
                ColorUtils.toHex(secondaryText);

        String contentSecondaryTextHex =
                ColorUtils.toHex(contentSecondaryText);

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
                "background:" +
                accentContentHex +
                ";" +
                "color:" + cardTextHex + ";" +
                "}" +

                ".layout{" +
                "display:flex;" +
                "min-height:100vh;" +
                "}" +

                ".sidebar{" +
                "width:205px;" +
                "background:" +
                ColorUtils.toHex(sidebarColor) +
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
                "color:" +
                accentSoftTextHex +
                ";" +
                "opacity:1;" +
                "}" +

                ".nav.active{" +
                "background:" +
                accentSoftHex +
                ";" +
                "color:" +
                accentSoftTextHex +
                ";" +
                "opacity:1;" +
                "font-weight:bold;" +
                "border-left:3px solid " +
                accentSoftTextHex +
                ";" +
                "padding-left:15px;" +
                "}" +

                ".content{" +
                "width:100%;" +
                "max-width:820px;" +
                "padding:28px;" +
                "background:" +
                accentContentHex +
                ";" +
                "}" +

                "h1{" +
                "font-size:28px;" +
                "margin:0;" +
                "color:" +
                headingColorHex +
                ";" +
                "}" +

                ".subtitle{" +
                "color:" +
                contentSecondaryTextHex +
                ";" +
                "margin:5px 0 25px;" +
                "}" +

                "h2{" +
                "font-size:18px;" +
                "margin:0 0 9px;" +
                "color:" +
                headingColorHex +
                ";" +
                "}" +

                ".card{" +
                "background:" +
                cardBackgroundHex +
                ";" +
                "color:" +
                cardTextHex +
                ";" +
                "border:1px solid " +
                accentBorderHex +
                ";" +
                "border-radius:8px;" +
                "overflow:hidden;" +
                "margin-bottom:25px;" +
                "}" +

                ".row{" +
                "padding:15px 16px;" +
                "border-bottom:1px solid " +
                accentBorderHex +
                ";" +
                "}" +

                ".row:last-child{border-bottom:0;}" +

                ".switchrow{" +
                "display:flex;" +
                "align-items:center;" +
                "justify-content:space-between;" +
                "gap:12px;" +
                "}" +

                ".title{" +
                "font-size:15px;" +
                "color:" +
                cardTextHex +
                ";" +
                "}" +

                ".description{" +
                "font-size:12px;" +
                "color:" +
                secondaryTextHex +
                ";" +
                "margin-top:3px;" +
                "}" +

                "select{" +
                "width:100%;" +
                "margin-top:8px;" +
                "padding:8px;" +
                "font-size:15px;" +
                "border:1px solid " +
                accentBorderHex +
                ";" +
                "background:" +
                cardBackgroundHex +
                ";" +
                "color:" +
                cardTextHex +
                ";" +
                "}" +

                "input[type=color]{" +
                "width:60px;" +
                "height:34px;" +
                "padding:0;" +
                "border:1px solid " +
                accentBorderHex +
                ";" +
                "background:" +
                cardBackgroundHex +
                ";" +
                "}" +

                "input[type=checkbox]{" +
                "width:20px;" +
                "height:20px;" +
                "}" +

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

                "input[type=text]{" +
                "width:100%;" +
                "margin-top:8px;" +
                "padding:9px;" +
                "font-size:14px;" +
                "border:1px solid " +
                accentBorderHex +
                ";" +
                "background:" +
                cardBackgroundHex +
                ";" +
                "color:" +
                cardTextHex +
                ";" +
                "}" +

                ".custom-hidden{" +
                "display:none;" +
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
                "<script>" +
                "function updateHomeVisibility(value){" +
                "var element=document.getElementById('custom-home');" +
                "if(element){element.className=value==='custom'?'row':'row custom-hidden';}" +
                "}" +
                "function updateSearchVisibility(value){" +
                "var element=document.getElementById('custom-search');" +
                "if(element){element.className=value==='custom'?'row':'row custom-hidden';}" +
                "}" +
                "function refreshLogs(){" +
                "var e=document.getElementById('logs');" +
                "if(e)e.value=Android.getLogs();" +
                "}" +
                "function showWebViewInfo(){Android.showWebViewInfo();}" +
                "</script>" +
                "</head>" +
                "<body>" +

                "<div class='layout'>" +

                "<div class='sidebar'>" +

                "<div class='brand'>Simple Browser " +
                BuildConfig.VERSION_NAME +
                "</div>" +

                "<div class='nav " +
                active(currentSection, "general") +
                "' onclick=\"Android.navigate('general')\">" +
                "General</div>" +

                "<div class='nav " +
                active(currentSection, "privacy-security") +
                "' onclick=\"Android.navigate('privacy-security')\">" +
                "Privacy &amp; Security</div>" +

                "<div class='nav " +
                active(currentSection, "websites") +
                "' onclick=\"Android.navigate('websites')\">" +
                "Websites</div>" +

                "<div class='nav " +
                active(currentSection, "appearance") +
                "' onclick=\"Android.navigate('appearance')\">" +
                "Appearance</div>" +

                "<div class='nav " +
                active(currentSection, "advanced") +
                "' onclick=\"Android.navigate('advanced')\">" +
                "Advanced</div>" +

                "</div>" +

                "<div class='content'>" +

                "<h1>Settings</h1>" +
                "<div class='subtitle'>Simple Browser " +
                BuildConfig.VERSION_NAME +
                " &bull; Configure Simple Browser</div>" +

                "<div id='section-general' class='section " +
                sectionActive(
                        currentSection,
                        "general") +
                "'>" +

                "<h2>General</h2>" +
                "<div class='card'>" +

                "<div class='row'>" +
                "<div class='title'>Home page</div>" +

                "<select onchange=\"updateHomeVisibility(this.value);Android.setHome(this.value)\">" +

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

                homeOption(
                        "browser://default",
                        "Default page") +

                "<option value='custom'" +
                ("custom".equals(
                        settings.getHomeSelection())
                        ? " selected"
                        : "") +
                ">Custom</option>" +

                "</select>" +
                "</div>" +

                "<div id='custom-home' class='row " +
                ("custom".equals(
                        settings.getHomeSelection())
                        ? ""
                        : "custom-hidden") +
                "'>" +
                "<div class='title'>Custom home page</div>" +
                "<div class='description'>Use a complete URL such as https://example.com/</div>" +
                "<input type='text' value='" +
                htmlAttribute(
                        settings.getCustomHomePage()) +
                "' onchange=\"Android.setCustomHome(this.value)\">" +
                "</div>" +

                "<div class='row'>" +
                "<div class='title'>Search engine</div>" +

                "<select onchange=\"updateSearchVisibility(this.value);Android.setSearch(this.value)\">" +

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

                "<option value='custom'" +
                ("custom".equals(
                        settings.getSearchEngine())
                        ? " selected"
                        : "") +
                ">Custom</option>" +

                "</select>" +
                "</div>" +

                "<div id='custom-search' class='row " +
                ("custom".equals(
                        settings.getSearchEngine())
                        ? ""
                        : "custom-hidden") +
                "'>" +
                "<div class='title'>Custom search URL</div>" +
                "<div class='description'>Use %s where the search text should be inserted.</div>" +
                "<input type='text' value='" +
                htmlAttribute(
                        settings.getCustomSearchUrl()) +
                "' onchange=\"Android.setCustomSearch(this.value)\">" +
                "</div>" +

                "<div class='row switchrow'>" +
                "<div>" +
                "<div class='title'>Restore tabs on startup</div>" +
                "<div class='description'>Reopen your normal tabs when Simple Browser starts again.</div>" +
                "</div>" +
                "<input type='checkbox'" +
                (settings.isRestoreTabsEnabled()
                        ? " checked"
                        : "") +
                " onchange=\"Android.setSetting('restore_tabs',this.checked)\">" +
                "</div>" +

                "</div>" +
                "</div>" +

                "<div id='section-privacy-security' class='section " +
                sectionActive(
                        currentSection,
                        "privacy-security") +
                "'>" +

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

                "<div id='section-websites' class='section " +
                sectionActive(
                        currentSection,
                        "websites") +
                "'>" +

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

                settingRow(
                        "Images",
                        "Allow websites to load network images",
                        "images",
                        settings.areImagesEnabled()) +

                settingRow(
                        "Zoom",
                        "Allow page zoom and pinch-to-zoom",
                        "zoom",
                        settings.isZoomEnabled()) +

                settingRow(
                        "Location access",
                        "Allow websites to request device location",
                        "geolocation",
                        settings.isGeolocationEnabled()) +

                settingRow(
                        "Media autoplay",
                        "Allow audio and video to start without a user gesture",
                        "media_autoplay",
                        settings.isMediaAutoplayEnabled()) +

                "</div>" +
                "</div>" +

                "<div id='section-advanced' class='section " +
                sectionActive(
                        currentSection,
                        "advanced") +
                "'>" +

                "<h2>Advanced</h2>" +
                "<div class='card'>" +

                "<div class='row'>" +
                "<div class='title'>Browser information</div>" +
                "<div class='description'>Simple Browser " +
                BuildConfig.VERSION_NAME +
                " &bull; Android API " +
                android.os.Build.VERSION.SDK_INT +
                "</div>" +
                "</div>" +

                "<div class='row'>" +
                "<div class='title'>Developer logs</div>" +
                "<div class='description'>Network requests, navigation changes, load errors, and JavaScript console messages. Logs are in memory only and capped automatically.</div>" +
                "<textarea id='logs' readonly " +
                "style='width:100%;height:300px;box-sizing:border-box;margin-top:10px;padding:10px;font-family:monospace;font-size:12px;background:" +
                cardBackgroundHex +
                ";color:" +
                cardTextHex +
                ";border:1px solid " +
                accentBorderHex +
                ";'></textarea>" +
                "<div style='margin-top:10px;display:flex;gap:8px;'>" +
                "<button onclick='refreshLogs()'>Refresh logs</button>" +
                "<button onclick=\"Android.clearLogs();refreshLogs()\">Clear logs</button>" +
                "</div>" +
                "</div>" +

                "<div class='row'>" +
                "<div class='title'>Current WebView</div>" +
                "<div class='description'>Inspect the active tab URL and user agent.</div>" +
                "<button style='margin-top:8px' onclick='showWebViewInfo()'>Show information</button>" +
                "</div>" +

                "</div>" +
                "</div>" +

                "<div id='section-appearance' class='section " +
                sectionActive(
                        currentSection,
                        "appearance") +
                "'>" +

                "<h2>Appearance</h2>" +

                "<div class='card'>" +

                "<div class='row switchrow'>" +

                "<div>" +
                "<div class='title'>Browser color</div>" +
                "<div class='description'>" +
                "Changes the toolbar, tabs, menu, and every part of the built-in settings UI." +
                "</div>" +
                "</div>" +

                "<input type='color' " +
                "value='" +
                accent +
                "' " +
                "onchange=\"Android.setColor(this.value)\">" +

                "</div>" +

                "</div>" +
                "</div>" +

                "</div>" +
                "</div>" +

                "</body>" +
                "</html>";
    }

    private String active(
            String current,
            String section) {

        return section.equals(current)
                ? "active"
                : "";
    }

    private String sectionActive(
            String current,
            String section) {

        return section.equals(current)
                ? "active"
                : "";
    }

    private String homeOption(
            String value,
            String label) {

        String selected =
                value.equals(
                        settings.getHomeSelection())
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

    private String htmlAttribute(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&#39;");
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

        private final BrowserTab tab;

        SettingsBridge(
                BrowserTab tab) {

            this.tab = tab;
        }

        @JavascriptInterface
        public void navigate(
                String section) {

            activity.runOnUiThread(
                    () -> activity
                            .showSettingsSection(
                                    tab,
                                    normalizeSection(
                                            section)));
        }

        @JavascriptInterface
        public String getLogs() {
            return BrowserLogger.getText();
        }

        @JavascriptInterface
        public void clearLogs() {
            BrowserLogger.clear();
        }

        @JavascriptInterface
        public void showWebViewInfo() {

            activity.runOnUiThread(
                    () -> {

                        BrowserTab current =
                                activity.getActiveTab();

                        if (current == null) {
                            return;
                        }

                        String url =
                                current.url == null
                                        ? ""
                                        : current.url;

                        String userAgent =
                                current.webView
                                        .getSettings()
                                        .getUserAgentString();

                        new android.app.AlertDialog.Builder(
                                activity)
                                .setTitle(
                                        "WebView information")
                                .setMessage(
                                        "URL:\n" +
                                        url +
                                        "\n\nUser agent:\n" +
                                        userAgent +
                                        "\n\nAndroid API: " +
                                        android.os.Build.VERSION.SDK_INT)
                                .setPositiveButton(
                                        "OK",
                                        null)
                                .show();
                    });
        }

        @JavascriptInterface
        public void setHome(
                String value) {
            settings.setHomePage(value);
        }

        @JavascriptInterface
        public void setCustomHome(
                String value) {

            settings.setCustomHomePage(value);
            settings.setHomePage("custom");
        }

        @JavascriptInterface
        public void setSearch(
                String value) {
            settings.setSearchEngine(value);
        }

        @JavascriptInterface
        public void setCustomSearch(
                String value) {

            settings.setCustomSearchUrl(value);
            settings.setSearchEngine("custom");
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

                        } else if ("restore_tabs"
                                .equals(name)) {

                            activity.saveTabs();

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
                            Color.WHITE);

            settings.setAccentColor(
                    ColorUtils.toHex(parsed));

            activity.runOnUiThread(
                    () -> {

                        activity.applyBrowserAppearance();

                        if (tab.settingsPage) {
                            show(
                                    tab,
                                    tab.settingsSection);
                        }
                    });
        }

        @JavascriptInterface
        public void clearData() {

            activity.runOnUiThread(
                    () -> {

                        for (BrowserTab current :
                                activity
                                        .getTabManager()
                                        .getTabs()) {

                            current.webView
                                    .clearCache(true);

                            current.webView
                                    .clearHistory();
                        }

                        android.webkit.CookieManager
                                .getInstance()
                                .removeAllCookie();

                        activity.clearBrowserHistory();
                        activity.clearDownloadHistory();

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

                        if (tab.settingsPage) {
                            show(
                                    tab,
                                    tab.settingsSection);
                        }
                    });
        }
    }
}
