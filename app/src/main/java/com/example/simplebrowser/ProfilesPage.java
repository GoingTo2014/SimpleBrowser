package com.example.simplebrowser;

import android.webkit.JavascriptInterface;

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
                        activity.getBrowserSettings()
                                .getAccentColor(),
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
                        android.graphics.Color.rgb(
                                90, 90, 90),
                        card,
                        4.5d);

        int button =
                ColorUtils.ensureContrast(
                        ColorUtils.darken(accent, 0.20f),
                        card,
                        3.0d);

        String currentId =
                ProfileManager.getActiveProfileId(
                        activity);

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
        html.append(";font-size:13px;margin-bottom:12px}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}");
        html.append(".card{background:");
        html.append(ColorUtils.toHex(card));
        html.append(";border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";border-radius:8px;padding:12px}.avatar{width:56px;height:56px;border-radius:50%;object-fit:cover;display:block;margin-bottom:9px;border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append("}.initials{width:56px;height:56px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";font-weight:bold;margin-bottom:9px}.name{font-weight:bold}.current{font-size:12px;color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";margin-top:2px}.actions{display:flex;flex-wrap:wrap;gap:7px;margin-top:9px}");
        html.append("button{border:1px solid ");
        html.append(ColorUtils.toHex(button));
        html.append(";background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";border-radius:5px;padding:9px 12px;font-weight:bold}");
        html.append("@media(max-width:600px){body{padding:10px}.grid{grid-template-columns:1fr}}");
        html.append("</style></head><body>");

        html.append("<h1>");
        html.append(escape(t("profiles.title")));
        html.append("</h1>");

        html.append("<div class='desc'>");
        html.append(escape(
                String.format(
                        java.util.Locale.US,
                        t("profiles.desc"),
                        ProfileManager.MAX_PROFILES)));
        html.append("</div>");

        html.append("<div class='actions'>");
        html.append("<button onclick='createProfile()'>");
        html.append(escape(t("profiles.create")));
        html.append("</button>");
        html.append("<button onclick='createGuest()'>");
        html.append(escape(t("profiles.guest")));
        html.append("</button>");
        html.append("</div>");

        List<ProfileManager.Profile> list =
                profiles.getProfiles(activity);

        html.append("<div class='grid'>");

        for (ProfileManager.Profile profile :
                list) {

            boolean current =
                    profile.id.equals(currentId);

            html.append("<div class='card'>");

            if (profile.pfpBase64 != null &&
                    !profile.pfpBase64.isEmpty()) {

                html.append("<img class='avatar' src='data:image/jpeg;base64,");
                html.append(
                        attribute(profile.pfpBase64));
                html.append("'>");

            } else {

                html.append("<div class='initials'>");
                html.append(
                        escape(
                                initials(profile.name)));
                html.append("</div>");
            }

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
                html.append("<button onclick='switchProfile(");
                html.append(jsAttribute(profile.id));
                html.append(")'>");
                html.append(escape(t("profiles.switch")));
                html.append("</button>");
            }

            if (!profile.isMain()) {
                html.append("<button onclick='editProfile(");
                html.append(jsAttribute(profile.id));
                html.append(")'>");
                html.append(escape(t("profiles.edit")));
                html.append("</button>");

                if (!current) {
                    html.append("<button onclick='deleteProfile(");
                    html.append(jsAttribute(profile.id));
                    html.append(")'>");
                    html.append(escape(t("common.delete")));
                    html.append("</button>");
                }
            }

            html.append("</div></div>");
        }

        html.append("</div>");
        html.append("<script>");
        html.append("function createProfile(){Android.createProfile()}");
        html.append("function createGuest(){if(confirm(");
        html.append(js(t("profiles.guest_confirm")));
        html.append(")){Android.createGuest()}}");
        html.append("function switchProfile(id){Android.switchProfile(id)}");
        html.append("function editProfile(id){Android.editProfile(id)}");
        html.append("function deleteProfile(id){if(confirm(");
        html.append(js(t("profiles.delete_confirm")));
        html.append(")){Android.deleteProfile(id)}}");
        html.append("</script></body></html>");

        return Localization.translateHtml(
                activity,
                html.toString());
    }

    private String initials(String name) {
        if (name == null ||
                name.trim().isEmpty()) {
            return "?";
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
                .replace(""", "&quot;");
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
                value.replace("\", "\\")
                        .replace("'", "\'") +
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
