package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;

public class BrowserSettings {

    private static final String PREFS =
            "browser_settings";

    private final SharedPreferences preferences;

    public BrowserSettings(Context context) {
        this(
                context,
                ProfileManager.getActiveProfileId(context));
    }

    public BrowserSettings(
            Context context,
            String profileId) {

        preferences =
                context.getSharedPreferences(
                        ProfileManager.scopedPrefsName(
                                PREFS,
                                profileId),
                        Context.MODE_PRIVATE);

        /*
         * Versions before the built-in New Tab page used Google as
         * the implicit default. Migrate that old implicit value once,
         * while allowing the user to explicitly select Google again.
         */
        if (!preferences.contains("home_default_migrated") &&
                "https://www.google.com/".equals(
                        preferences.getString(
                                "home",
                                null))) {

            preferences.edit()
                    .putString(
                            "home",
                            "browser://default")
                    .putBoolean(
                            "home_default_migrated",
                            true)
                    .apply();
        }
    }

    public String getLanguage() {
        return preferences.getString(
                "language",
                Localization.SYSTEM);
    }

    public void setLanguage(
            String language) {

        String value =
                Localization.SYSTEM.equals(language) ||
                Localization.ENGLISH.equals(language) ||
                Localization.SPANISH.equals(language) ||
                Localization.PORTUGUESE.equals(language) ||
                Localization.FRENCH.equals(language) ||
                Localization.JAPANESE.equals(language) ||
                Localization.CHINESE.equals(language) ||
                Localization.HINDI.equals(language) ||
                Localization.RUSSIAN.equals(language) ||
                Localization.GERMAN.equals(language) ||
                Localization.ARABIC.equals(language)
                        ? language
                        : Localization.SYSTEM;

        preferences.edit()
                .putString("language", value)
                .apply();
    }

    public String getHomePage() {

        String value =
                preferences.getString(
                        "home",
                        "browser://default");

        if ("custom".equals(value)) {

            String custom =
                    preferences.getString(
                            "custom_home",
                            "");

            if (!custom.trim().isEmpty()) {
                return custom.trim();
            }

            return "about:blank";
        }

        return value;
    }

    public String getHomeSelection() {
        return preferences.getString(
                "home",
                "browser://default");
    }

    public void setHomePage(String value) {
        preferences.edit()
                .putString("home", value)
                .putBoolean(
                        "home_default_migrated",
                        true)
                .apply();
    }

    public String getCustomHomePage() {
        return preferences.getString(
                "custom_home",
                "");
    }

    public void setCustomHomePage(
            String value) {

        preferences.edit()
                .putString(
                        "custom_home",
                        value == null
                                ? ""
                                : value.trim())
                .apply();
    }

    public String getSearchEngine() {
        return preferences.getString(
                "search",
                "google");
    }

    public void setSearchEngine(String value) {
        preferences.edit()
                .putString("search", value)
                .apply();
    }

    public String getCustomSearchUrl() {
        return preferences.getString(
                "custom_search",
                "");
    }

    public void setCustomSearchUrl(
            String value) {

        preferences.edit()
                .putString(
                        "custom_search",
                        value == null
                                ? ""
                                : value.trim())
                .apply();
    }

    public boolean isJavaScriptEnabled() {
        return preferences.getBoolean(
                "javascript",
                true);
    }

    public boolean arePopupsEnabled() {
        return preferences.getBoolean(
                "popups",
                true);
    }

    public boolean areCookiesEnabled() {
        return preferences.getBoolean(
                "cookies",
                true);
    }

    public boolean isStorageEnabled() {
        return preferences.getBoolean(
                "storage",
                true);
    }

    public boolean areImagesEnabled() {
        return preferences.getBoolean(
                "images",
                true);
    }

    public boolean isZoomEnabled() {
        return preferences.getBoolean(
                "zoom",
                true);
    }

    public boolean isGeolocationEnabled() {
        return preferences.getBoolean(
                "geolocation",
                false);
    }

    public boolean isMediaAutoplayEnabled() {
        return preferences.getBoolean(
                "media_autoplay",
                false);
    }

    public boolean isDesktopMode() {
        return preferences.getBoolean(
                "desktop_mode",
                false);
    }

    public String getUserAgentProfile() {
        return preferences.getString(
                "user_agent_profile",
                UserAgentProfiles.DEFAULT);
    }

    public void setUserAgentProfile(
            String profile) {
        preferences.edit()
                .putString(
                        "user_agent_profile",
                        profile == null
                                ? UserAgentProfiles.DEFAULT
                                : profile)
                .apply();
    }

    public String getCustomUserAgent() {
        return preferences.getString(
                "custom_user_agent",
                "");
    }

    public void setCustomUserAgent(
            String value) {
        preferences.edit()
                .putString(
                        "custom_user_agent",
                        value == null
                                ? ""
                                : value.trim())
                .apply();
    }

    public boolean isWebViewDebuggingEnabled() {
        return preferences.getBoolean(
                "webview_debugging",
                false);
    }

    public boolean isCameraEnabled() {
        return preferences.getBoolean(
                "camera",
                true);
    }

    public boolean isJavaScriptCanOpenWindowsAutomaticallyEnabled() {
        return preferences.getBoolean(
                "js_open_windows",
                false);
    }

    public boolean isJavaScriptCanOpenWindowsAutomatically() {
        return isJavaScriptCanOpenWindowsAutomaticallyEnabled();
    }

    public boolean isLoadWithOverviewMode() {
        return preferences.getBoolean(
                "overview_mode",
                false);
    }

    public int getTextZoom() {
        return clampInt(
                preferences.getInt(
                        "text_zoom",
                        100),
                50,
                200);
    }

    public int getMinimumFontSize() {
        return clampInt(
                preferences.getInt(
                        "minimum_font_size",
                        8),
                1,
                24);
    }

    public boolean isWebSqlEnabled() {
        return preferences.getBoolean(
                "web_sql",
                true);
    }

    public boolean isSafeBrowsingEnabled() {
        return preferences.getBoolean(
                "safe_browsing",
                true);
    }

    public boolean isHardwareAccelerationEnabled() {
        return preferences.getBoolean(
                "hardware_acceleration",
                true);
    }

    public void setInt(
            String name,
            int value) {

        preferences.edit()
                .putInt(name, value)
                .apply();
    }

    public void setCacheMode(String mode) {
        String value =
                "no_cache".equals(mode) ||
                "cache_only".equals(mode)
                        ? mode
                        : "default";

        preferences.edit()
                .putString(
                        "cache_mode",
                        value)
                .apply();
    }

    private int clampInt(
            int value,
            int minimum,
            int maximum) {

        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value));
    }

    public boolean isMicrophoneEnabled() {
        return preferences.getBoolean(
                "microphone",
                true);
    }

    public boolean isSaveFormDataEnabled() {
        return preferences.getBoolean(
                "save_form_data",
                false);
    }

    public boolean isThirdPartyCookiesEnabled() {
        return preferences.getBoolean(
                "third_party_cookies",
                true);
    }

    public boolean isMixedContentEnabled() {
        return preferences.getBoolean(
                "mixed_content",
                false);
    }

    public boolean isFileAccessFromFileUrlsEnabled() {
        return preferences.getBoolean(
                "file_access_from_file_urls",
                false);
    }

    public boolean isUniversalAccessFromFileUrlsEnabled() {
        return preferences.getBoolean(
                "universal_access_from_file_urls",
                false);
    }

    public String getCacheMode() {
        return preferences.getString(
                "cache_mode",
                "default");
    }

    public void setCacheMode(String mode) {
        String value =
                "no_cache".equals(mode) ||
                "cache_only".equals(mode)
                        ? mode
                        : "default";

        preferences.edit()
                .putString(
                        "cache_mode",
                        value)
                .apply();
    }

    public boolean isAutomaticUpdatesEnabled() {
        return preferences.getBoolean(
                "automatic_updates",
                true);
    }

    public void setAutomaticUpdatesEnabled(
            boolean enabled) {

        preferences.edit()
                .putBoolean(
                        "automatic_updates",
                        enabled)
                .apply();
    }

    public long getLastUpdateCheck() {
        return preferences.getLong(
                "update_last_check",
                0L);
    }

    public void setLastUpdateCheck(
            long time) {

        preferences.edit()
                .putLong(
                        "update_last_check",
                        time)
                .apply();
    }

    public boolean isRestoreTabsEnabled() {
        return preferences.getBoolean(
                "restore_tabs",
                true);
    }

    public void setRestoreTabsEnabled(
            boolean enabled) {

        preferences.edit()
                .putBoolean(
                        "restore_tabs",
                        enabled)
                .apply();
    }

    public String getSavedTabsJson() {
        return preferences.getString(
                "saved_tabs",
                "");
    }

    public void setSavedTabsJson(
            String value) {

        preferences.edit()
                .putString(
                        "saved_tabs",
                        value == null
                                ? ""
                                : value)
                .apply();
    }

    public String getAccentColor() {
        return preferences.getString(
                "color",
                "#FFFFFF");
    }

    public void setBoolean(
            String name,
            boolean value) {

        preferences.edit()
                .putBoolean(name, value)
                .apply();
    }

    public void setAccentColor(
            String color) {

        preferences.edit()
                .putString("color", color)
                .apply();
    }

    public void reset() {
        preferences.edit()
                .clear()
                .apply();
    }
}
