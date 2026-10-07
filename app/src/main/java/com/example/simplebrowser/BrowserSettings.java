package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.Map;

public class BrowserSettings {

    private static final String PREFS =
            "browser_settings";

    private static final String[] SYNC_KEYS = {
            "language",
            "home",
            "custom_home",
            "search",
            "custom_search",
            "javascript",
            "popups",
            "cookies",
            "storage",
            "images",
            "zoom",
            "geolocation",
            "media_autoplay",
            "desktop_mode",
            "user_agent_profile",
            "custom_user_agent",
            "camera",
            "js_open_windows",
            "overview_mode",
            "text_zoom",
            "minimum_font_size",
            "web_sql",
            "safe_browsing",
            "hardware_acceleration",
            "wide_viewport",
            "offline_mode",
            "cache_mode",
            "automatic_updates",
            "restore_tabs",
            "color",
            "microphone",
            "save_form_data",
            "third_party_cookies",
            "mixed_content",
            "file_access_from_file_urls",
            "universal_access_from_file_urls"
    };

    private final SharedPreferences preferences;
    private final Context context;
    private final String profileId;


    public BrowserSettings(Context context) {
        this(
                context,
                ProfileManager.getActiveProfileId(context));
    }

    public BrowserSettings(
            Context context,
            String profileId) {

        this.context =
                context.getApplicationContext();

        this.profileId =
                profileId == null ||
                profileId.trim().isEmpty()
                        ? ProfileManager.MAIN_ID
                        : profileId;

        preferences =
                context.getSharedPreferences(
                        ProfileManager.scopedPrefsName(
                                PREFS,
                                this.profileId),
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

        markSyncChanged();
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

        markSyncChanged();
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

        markSyncChanged();
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

        markSyncChanged();
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

        markSyncChanged();
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

        markSyncChanged();
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

    public boolean isWideViewportEnabled() {
        return preferences.getBoolean(
                "wide_viewport",
                true);
    }

    public boolean isOfflineModeEnabled() {
        return preferences.getBoolean(
                "offline_mode",
                false);
    }

    public void setInt(
            String name,
            int value) {

        preferences.edit()
                .putInt(name, value)
                .apply();

        markSyncChanged();
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

        markSyncChanged();
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

        markSyncChanged();
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

        markSyncChanged();
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
                .commit();
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

        markSyncChanged();
    }

    public void setAccentColor(
            String color) {

        preferences.edit()
                .putString("color", color)
                .apply();

        markSyncChanged();
    }

    /**
     * Exports user-facing browser settings for Simple Account sync.
     * Device-specific state such as update timestamps and saved tab state is
     * intentionally excluded.
     */
    public String exportSyncJson() {
        JSONObject result =
                new JSONObject();

        Map<String, ?> all =
                preferences.getAll();

        for (String key : SYNC_KEYS) {
            if (!all.containsKey(key)) {
                continue;
            }

            Object value =
                    all.get(key);

            try {
                if (value instanceof Boolean ||
                        value instanceof String ||
                        value instanceof Integer ||
                        value instanceof Long ||
                        value instanceof Float ||
                        value instanceof Double) {
                    result.put(
                            key,
                            value);
                }
            } catch (Exception ignored) {
            }
        }

        return result.toString();
    }

    /**
     * Imports only known syncable browser settings and preserves unrelated
     * local preferences.
     */
    public void importSyncJson(
            JSONObject source) {

        if (source == null) {
            return;
        }

        SharedPreferences.Editor editor =
                preferences.edit();

        for (String key : SYNC_KEYS) {
            if (!source.has(key)) {
                continue;
            }

            try {
                Object value =
                        source.get(key);

                if (value == null ||
                        JSONObject.NULL.equals(value)) {
                    continue;
                }

                if (value instanceof Boolean) {
                    editor.putBoolean(
                            key,
                            ((Boolean) value)
                                    .booleanValue());

                } else if (value instanceof Number) {
                    editor.putInt(
                            key,
                            ((Number) value)
                                    .intValue());

                } else {
                    editor.putString(
                            key,
                            String.valueOf(value));
                }

            } catch (Exception ignored) {
            }
        }

        editor.apply();
    }

    private void markSyncChanged() {
        SimpleSyncManager.markProfileChanged(
                context,
                profileId);
    }

    public void reset() {
        preferences.edit()
                .clear()
                .apply();

        markSyncChanged();
    }
}
