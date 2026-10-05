package com.example.simplebrowser;

public final class UserAgentProfiles {

    public static final String DEFAULT = "default";
    public static final String CUSTOM = "custom";

    private static final String[] IDS = {
            DEFAULT,
            "chrome_windows",
            "firefox_windows",
            "edge_windows",
            "chrome_android",
            "safari_iphone",
            "safari_ipad",
            "samsung_browser",
            "android_tablet",
            "playstation_5",
            "xbox_series",
            "nintendo_switch",
            "apple_tv",
            "android_tv",
            "google_tv",
            "smart_tv"
    };

    private static final String[] LABELS = {
            "Default Android WebView",
            "Chrome - Windows",
            "Firefox - Windows",
            "Edge - Windows",
            "Chrome - Android",
            "Safari - iPhone",
            "Safari - iPad",
            "Samsung Internet",
            "Android Tablet",
            "PlayStation 5",
            "Xbox Series X",
            "Nintendo Switch",
            "Apple TV",
            "Android TV",
            "Google TV",
            "Smart TV"
    };

    private static final String[] VALUES = {
            "",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.6 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPad; CPU OS 18_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.6 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/28.0 Chrome/130.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 13; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (PlayStation 5 10.00) AppleWebKit/605.1.15 (KHTML, like Gecko)",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/120.0.0.0 Xbox",
            "Mozilla/5.0 (Nintendo Switch; WifiWebAuthApplet) AppleWebKit/606.4 (KHTML, like Gecko) NF/1.0.0.0.1 NintendoBrowser/5.1.0.22474",
            "AppleTV6,2/11.1 AppleWebKit/605.1.15",
            "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.101 Safari/537.36",
            "Mozilla/5.0 (Linux; Android 12; Google TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.193 Safari/537.36",
            "Mozilla/5.0 (SMART-TV; Linux; Tizen 8.0) AppleWebKit/537.36 (KHTML, like Gecko) Version/8.0 TV Safari/537.36"
    };

    private UserAgentProfiles() {
    }

    public static int size() {
        return IDS.length;
    }

    public static String id(int index) {
        return IDS[index];
    }

    public static String label(int index) {
        return LABELS[index];
    }

    public static String value(int index) {
        return VALUES[index];
    }

    public static String getLabel(String id) {
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) {
                return LABELS[i];
            }
        }
        return "Custom";
    }

    public static String getValue(
            String id,
            String custom,
            String fallback) {

        if (CUSTOM.equals(id)) {
            return custom == null ? fallback : custom.trim();
        }

        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) {
                return VALUES[i].trim().isEmpty()
                        ? fallback
                        : VALUES[i];
            }
        }

        return fallback;
    }
}
