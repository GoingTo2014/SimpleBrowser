package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;

public class BrowserSettings {

    private static final String PREFS =
            "browser_settings";

    private final SharedPreferences preferences;

    public BrowserSettings(Context context) {
        preferences =
                context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE);
    }

    public String getHomePage() {
        return preferences.getString(
                "home",
                "https://www.google.com/");
    }

    public void setHomePage(String value) {
        preferences.edit()
                .putString("home", value)
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

    public boolean isDesktopMode() {
        return preferences.getBoolean(
                "desktop_mode",
                false);
    }

    public String getAccentColor() {
        return preferences.getString(
                "color",
                "#3F51B5");
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
