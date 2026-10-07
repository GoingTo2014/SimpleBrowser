package com.example.simplebrowser;

import java.util.List;

/**
 * Built-in profile selector and manager.
 *
 * Profile management keeps normal internal navigation as a fallback, while
 * switching uses a narrow bridge so the action is immediate and reliable
 * across WebView callback variations.
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

    public void show(BrowserTab tab) {

        if (tab == null) return;

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
        tab.loading = false;
        tab.title = Localization.translate(
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
        activity.updateNavigationButtonsForTabs();
    }

    public void remove(BrowserTab tab) {
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
                        0.95f);

        int card =
                ColorUtils.mix(
                        accent,
                        android.graphics.Color.WHITE,
                        0.91f);

        int text =
                ColorUtils.getReadableTextColor(card);

        int secondary =
                ColorUtils.ensureContrast(
                        android.graphics.Color.rgb(
                                100, 100, 100),
                        card,
                        4.5d);

        int button =
                ColorUtils.ensureContrast(
                        accent,
                        card,
                        3.0d);

        boolean guestMode =
                activity.getTabManager()
                        .isGuestMode();

        String currentId =
                guestMode
                        ? "__guest__"
                        : ProfileManager.getActiveProfileId(
                                activity);

        StringBuilder html =
                new StringBuilder();

        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta name='viewport' content='width=device-width,initial-scale=1'>");
        html.append("<style>");
        html.append("*{box-sizing:border-box}");
        html.append("html,body{margin:0;padding:0;background:");
        html.append(ColorUtils.toHex(content));
        html.append(";color:");
        html.append(ColorUtils.toHex(text));
        html.append(";font-family:sans-serif}");
        html.append("body{padding:16px;max-width:900px;margin:auto}");
        html.append("h1{margin:0 0 4px;font-size:26px}");
        html.append(".desc{color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";font-size:13px;margin-bottom:14px;line-height:1.45}");
        html.append(".toolbar{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px}");
        html.append(".grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}");
        html.append(".card{background:");
        html.append(ColorUtils.toHex(card));
        html.append(";border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";border-radius:9px;padding:14px}");
        html.append(".avatar{width:56px;height:56px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";font-weight:bold;font-size:22px;margin-bottom:9px}");
        html.append(".name{font-weight:bold;font-size:16px}");
        html.append(".current{font-size:12px;color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";margin-top:3px}");
        html.append(".actions{display:flex;flex-wrap:wrap;gap:7px;margin-top:10px}");
        html.append("a.button,button.button{display:inline-block;border:1px solid ");
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
        html.append("</h1>");

        html.append("<div class='desc'>");
        html.append(escape(String.format(
                java.util.Locale.US,
                t("profiles.desc"),
                ProfileManager.PROFILE_LIMIT)));
        html.append("</div>");

        html.append("<div class='toolbar'>");
        html.append(actionLink(
                "simplebrowser://profile/create",
                t("profiles.create"),
                button));
        html.append(actionLink(
                "simplebrowser://profile/guest",
                t("profiles.guest"),
                button));
        html.append("</div>");

        List<ProfileManager.Profile> list =
                profiles.getProfiles(activity);

        html.append("<div class='grid'>");

        for (ProfileManager.Profile profile :
                list) {

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
                html.append(switchLink(
                        profile.id,
                        t("profiles.switch"),
                        button));
            }

            html.append(actionLink(
                    "simplebrowser://profile/edit/" +
                            attribute(profile.id),
                    t("profiles.edit"),
                    button));

            if (!profile.isMain()) {

                if (!current) {
                    html.append(actionLink(
                            "simplebrowser://profile/delete/" +
                                    attribute(profile.id),
                            t("common.delete"),
                            button));
                }
            }

            html.append("</div></div>");
        }

        if (guestMode) {

            html.append("<div class='card'>");
            html.append("<div class='avatar'>G</div>");
            html.append("<div class='name'>");
            html.append(escape(
                    t("profiles.guest")));
            html.append("</div>");
            html.append("<div class='current'>");
            html.append(escape(
                    t("profiles.current")));
            html.append("</div>");
            html.append("<div class='actions'>");
            html.append(actionLink(
                    "simplebrowser://profile/exit-guest",
                    t("guest.exit"),
                    button));
            html.append("</div>");
            html.append("</div>");
        }

        html.append("</div></body></html>");

        return Localization.translateHtml(
                activity,
                html.toString());
    }

    private String switchLink(
            String profileId,
            String label,
            int ignoredButtonColor) {

        String safeId =
                escapeJavaScript(
                        profileId);

        return "<a class='button' href='https://browser.local/profiles/action/switch/" +
                attribute(profileId) +
                "' onclick=\"if(window.ProfileActions){ProfileActions.switchProfile('" +
                safeId +
                "');return false;}\">" +
                escape(label) +
                "</a>";
    }

    private String escapeJavaScript(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String actionLink(
            String url,
            String label,
            int ignoredButtonColor) {

        /*
         * Use same-origin HTTPS for profile actions so Android WebView
         * reports the navigation consistently through its normal callbacks.
         */
        String actionUrl = url;

        if (url != null &&
                url.startsWith(
                        "simplebrowser://profile/")) {

            actionUrl =
                    "https://browser.local/profiles/action/" +
                    url.substring(
                            "simplebrowser://profile/"
                                    .length());
        }

        return "<a class='button' href='" +
                attribute(actionUrl) +
                "'>" +
                escape(label) +
                "</a>";
    }

    private String initial(String name) {

        if (name == null ||
                name.trim().isEmpty()) {
            return "?";
        }

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
                .replace("\"", "&quot;");
    }

    private String attribute(String value) {

        return escape(value)
                .replace("'", "&#39;");
    }

    private String t(String key) {

        return Localization.translate(
                activity,
                key);
    }
}
