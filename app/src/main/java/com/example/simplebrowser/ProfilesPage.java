package com.example.simplebrowser;

import java.util.List;

/**
 * Built-in profile selector and manager.
 */
public final class ProfilesPage {

    private final MainActivity activity;
    private final ProfileManager profiles;

    public ProfilesPage(
            MainActivity activity,
            ProfileManager profiles) {

        this.activity = activity;
        this.profiles = profiles;
    }

    public void show(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        tab.settingsPage = false;
        tab.defaultPage = false;
        tab.historyPage = false;
        tab.downloadsPage = false;
        tab.cookiesPage = false;
        tab.bookmarksPage = false;
        tab.passwordsPage = false;
        tab.errorPage = false;
        tab.profilesPage = true;
        tab.url = BrowserPage.PROFILES;
        tab.title =
                Localization.translate(
                        activity,
                        "Profiles");

        BrowserPage.load(
                tab.webView,
                BrowserPage.PROFILES,
                createHtml(),
                BrowserPage.PROFILES);

        activity.setBrowserPageIcon(
                tab,
                BrowserIconDrawable.HOME);

        activity.updateTabTitle(tab);
        activity.updateSecurity(tab);
        activity.settingsLoaded(tab);
    }

    public void remove(
            BrowserTab tab) {

        if (tab != null) {
            tab.profilesPage = false;
        }
    }

    private String createHtml() {

        int accent =
                ColorUtils.parseColor(
                        activity.getBrowserSettings().getAccentColor(),
                        android.graphics.Color.WHITE);

        int content =
                ColorUtils.mix(
                        accent,
                        android.graphics.Color.WHITE,
                        0.94f);

        int card =
                ColorUtils.mix(
                        accent,
                        android.graphics.Color.WHITE,
                        0.90f);

        int text =
                ColorUtils.getReadableTextColor(card);

        int secondary =
                ColorUtils.ensureContrast(
                        android.graphics.Color.rgb(90, 90, 90),
                        card,
                        4.5d);

        int button =
                ColorUtils.ensureContrast(
                        ColorUtils.darken(accent, 0.12f),
                        card,
                        3.0d);

        String currentId =
                ProfileManager.getActiveProfileId(activity);

        StringBuilder html =
                new StringBuilder();

        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta name='viewport' content='width=device-width,initial-scale=1'>");
        html.append("<style>*{box-sizing:border-box}html,body{margin:0;padding:0;background:");
        html.append(ColorUtils.toHex(content));
        html.append(";color:");
        html.append(ColorUtils.toHex(text));
        html.append(";font-family:sans-serif}body{padding:16px;max-width:900px;margin:auto}");
        html.append("h1{margin:0 0 4px;font-size:26px}.desc{color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";font-size:13px;margin-bottom:12px}.actions{display:flex;flex-wrap:wrap;gap:7px;margin:10px 0}");
        html.append(".grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}");
        html.append(".card{background:");
        html.append(ColorUtils.toHex(card));
        html.append(";border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";border-radius:8px;padding:14px}.avatar{width:56px;height:56px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";font-weight:bold;font-size:21px;margin-bottom:9px}.name{font-weight:bold;font-size:16px}.current{font-size:12px;color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";margin-top:3px}.actions button{border:1px solid ");
        html.append(ColorUtils.toHex(button));
        html.append(";background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";border-radius:5px;padding:9px 12px;font-weight:bold;text-decoration:none}");
        html.append("@media(max-width:600px){body{padding:10px}.grid{grid-template-columns:1fr}}");
        html.append("</style></head><body>");

        html.append("<h1>");
        html.append(escape(t("profiles.title")));
        html.append("</h1><div class='desc'>");
        html.append(escape(String.format(
                java.util.Locale.US,
                t("profiles.desc"),
                ProfileManager.MAX_PROFILES)));
        html.append("</div>");

        html.append("<div class='actions'><a class='actions button' href='simplebrowser://profile/create'>");
        html.append(escape(t("profiles.create")));
        html.append("</a><a class='actions button' href='simplebrowser://profile/guest'>");
        html.append(escape(t("profiles.guest")));
        html.append("</a></div>");

        List<ProfileManager.Profile> list =
                profiles.getProfiles(activity);

        html.append("<div class='grid'>");

        for (ProfileManager.Profile profile : list) {

            boolean current =
                    profile.id.equals(currentId);

            html.append("<div class='card'>");
            html.append("<div class='avatar'>");
            html.append(escape(initial(profile.name)));
            html.append("</div>");
            html.append("<div class='name'>");
            html.append(escape(profile.name));
            html.append("</div>");

            if (current) {
                html.append("<div class='current'>");
                html.append(escape(t("profiles.current")));
                html.append("</div>");
            }

            html.append("<div class='actions'>");

            if (!current) {
                html.append("<a class='button' href='simplebrowser://profile/switch/");
                html.append(attribute(profile.id));
                html.append("'>");
                html.append(escape(t("profiles.switch")));
                html.append("</a>");
            }

            if (!profile.isMain()) {
                html.append("<a class='button' href='simplebrowser://profile/edit/");
                html.append(attribute(profile.id));
                html.append("'>");
                html.append(escape(t("profiles.edit")));
                html.append("</a>");
                if (!current) {
                    html.append("<a class='button' href='simplebrowser://profile/delete/");
                    html.append(attribute(profile.id));
                    html.append("'>");
                    html.append(escape(t("common.delete")));
                    html.append("</a>");
                }
            }

            html.append("</div></div>");
        }

        html.append("</div></body></html>");

        return Localization.translateHtml(
                activity,
                html.toString());
    }

    private String initial(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        return String.valueOf(
                Character.toUpperCase(
                        name.trim().charAt(0)));
    }

    private String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace(""", "&quot;");
    }

    private String attribute(String value) {
        return escape(value)
                .replace("'", "&#39;");
    }

    private String t(String key) {
        return Localization.translate(activity, key);
    }

}

        String[] parts =
                name.trim().split("\\s+");

        String result =
                String.valueOf(
                        Character.toUpperCase(
                                parts[0].charAt(0)));

        if (parts.length > 1) {
            result +=
                    Character.toUpperCase(
                            parts[parts.length - 1]
                                    .charAt(0));
        }

        return result;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String attribute(String value) {
        return escape(value)
                .replace("'", "&#39;");
    }

    private String jsAttribute(String value) {
        if (value == null) {
            return "''";
        }

        return "'" +
                value.replace("\\", "\\\\")
                        .replace("'", "\\'") +
                "'";
    }

    private String js(String value) {
        return jsAttribute(value);
    }

    private String t(String key) {
        return Localization.translate(
                activity,
                key);
    }

    public final class Bridge {

        private final BrowserTab tab;

        public Bridge(BrowserTab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public void createProfile() {
            activity.runOnUiThread(
                    () -> activity.showProfileEditor(
                            tab,
                            null));
        }

        @JavascriptInterface
        public void createGuest() {
            activity.runOnUiThread(
                    () -> activity.enterGuestProfile(tab));
        }

        @JavascriptInterface
        public void switchProfile(
                String profileId) {

            activity.runOnUiThread(
                    () -> activity.switchProfile(profileId));
        }

        @JavascriptInterface
        public void editProfile(
                String profileId) {

            activity.runOnUiThread(
                    () -> activity.showProfileEditor(
                            tab,
                            profileId));
        }

        @JavascriptInterface
        public void deleteProfile(
                String profileId) {

            boolean deleted =
                    profiles.deleteProfile(
                            activity,
                            profileId);

            activity.runOnUiThread(
                    () -> {
                        if (deleted) {
                            show(tab);
                        }
                    });
        }
    }
}
