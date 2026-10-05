package com.example.simplebrowser;

import android.webkit.WebView;

/**
 * Central registry and router for built-in browser pages.
 *
 * Add a new public browser:// route here first, then use
 * load() from the page renderer. The private WebView base URL
 * stays inside this class and is never used as a visible URL.
 */
public final class BrowserPage {

    public static final String DEFAULT =
            "browser://default";

    public static final String HISTORY =
            "browser://history";

    public static final String DOWNLOADS =
            "browser://downloads";

    public static final String ERROR =
            "browser://error";

    public static final String SETTINGS =
            "browser://settings";

    public static final String COOKIES =
            "browser://cookies";

    public static final String DEMO =
            "browser://demo";

    private static final String WEBVIEW_BASE =
            "https://browser.local";

    private BrowserPage() {
    }

    public static String settingsUrl(
            String section) {

        if (!isSettingsSection(section)) {
            section = "general";
        }

        return SETTINGS +
                "/" +
                section;
    }

    public static String getSettingsSection(
            String url) {

        String route =
                toPublicRoute(url);

        if (route == null) {
            return null;
        }

        if (route.equals(SETTINGS) ||
                route.equals(
                        SETTINGS + "/general")) {
            return "general";
        }

        String prefix =
                SETTINGS + "/";

        if (route.startsWith(prefix)) {
            String section =
                    route.substring(
                            prefix.length());

            if (isSettingsSection(section)) {
                return section;
            }
        }

        return null;
    }

    public static String toPublicRoute(
            String url) {

        if (url == null) {
            return null;
        }

        String value =
                url.trim();

        if (value.isEmpty()) {
            return null;
        }

        String lower =
                value.toLowerCase();

        if (lower.startsWith(
                "browser://")) {
            return normalizeBrowserRoute(value);
        }

        String localPrefix =
                WEBVIEW_BASE;

        if (lower.equals(localPrefix) ||
                lower.equals(localPrefix + "/")) {
            return SETTINGS + "/general";
        }

        String settingsPrefix =
                localPrefix + "/settings";

        if (lower.equals(settingsPrefix) ||
                lower.equals(
                        settingsPrefix + "/") ||
                lower.startsWith(
                        settingsPrefix + "/")) {

            String suffix =
                    value.substring(
                            settingsPrefix.length());

            if (suffix.isEmpty() ||
                    suffix.equals("/")) {
                return SETTINGS + "/general";
            }

            if (suffix.startsWith("/")) {
                suffix = suffix.substring(1);
            }

            if (isSettingsSection(suffix)) {
                return settingsUrl(suffix);
            }
        }

        if (matchesLocalPage(
                lower,
                "/default")) {
            return DEFAULT;
        }

        if (matchesLocalPage(
                lower,
                "/history")) {
            return HISTORY;
        }

        if (matchesLocalPage(
                lower,
                "/downloads")) {
            return DOWNLOADS;
        }

        if (matchesLocalPage(
                lower,
                "/error")) {
            return ERROR;
        }

        if (matchesLocalPage(
                lower,
                "/cookies")) {
            return COOKIES;
        }

        if (matchesLocalPage(
                lower,
                "/demo")) {
            return DEMO;
        }

        return null;
    }

    public static boolean isInternalUrl(
            String url) {

        return toPublicRoute(url) != null;
    }

    public static String displayUrl(
            BrowserTab tab) {

        if (tab == null) {
            return "";
        }

        if (tab.defaultPage) {
            return "";
        }

        if (tab.settingsPage) {
            return settingsUrl(
                    tab.settingsSection);
        }

        if (tab.historyPage) {
            return HISTORY;
        }

        if (tab.downloadsPage) {
            return DOWNLOADS;
        }

        if (tab.cookiesPage) {
            return COOKIES;
        }

        if (DEMO.equalsIgnoreCase(tab.url)) {
            return DEMO;
        }

        if (tab.errorPage) {
            return tab.url == null
                    ? ""
                    : tab.url;
        }

        String webUrl =
                tab.webView == null
                        ? null
                        : tab.webView.getUrl();

        String route =
                toPublicRoute(webUrl);

        if (route != null) {
            return route;
        }

        return tab.url == null
                ? ""
                : tab.url;
    }

    public static void load(
            WebView webView,
            String route,
            String html,
            String historyUrl) {

        if (webView == null) {
            return;
        }

        String publicRoute =
                toPublicRoute(route);

        if (publicRoute == null) {
            publicRoute = route;
        }

        String path =
                publicRoute;

        if (path.startsWith(
                "browser://")) {
            path =
                    path.substring(
                            "browser://".length());
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        webView.loadDataWithBaseURL(
                WEBVIEW_BASE + path,
                html,
                "text/html",
                "UTF-8",
                historyUrl);
    }

    private static boolean matchesLocalPage(
            String lowerUrl,
            String path) {

        return lowerUrl.equals(
                WEBVIEW_BASE + path);
    }

    private static String normalizeBrowserRoute(
            String value) {

        String lower =
                value.toLowerCase();

        if (lower.equals(
                DEFAULT)) {
            return DEFAULT;
        }

        if (lower.equals(
                HISTORY)) {
            return HISTORY;
        }

        if (lower.equals(
                DOWNLOADS)) {
            return DOWNLOADS;
        }

        if (lower.equals(
                ERROR)) {
            return ERROR;
        }

        if (lower.equals(
                COOKIES)) {
            return COOKIES;
        }

        if (lower.equals(
                DEMO)) {
            return DEMO;
        }

        if (lower.equals(
                SETTINGS)) {
            return SETTINGS + "/general";
        }

        if (lower.startsWith(
                SETTINGS + "/")) {

            String section =
                    value.substring(
                            SETTINGS.length() + 1)
                            .toLowerCase();

            if (isSettingsSection(section)) {
                return settingsUrl(section);
            }
        }

        return value;
    }

    private static boolean isSettingsSection(
            String section) {

        return "general".equals(section) ||
                "websites".equals(section) ||
                "appearance".equals(section) ||
                "privacy-security".equals(section) ||
                "advanced".equals(section);
    }
}
