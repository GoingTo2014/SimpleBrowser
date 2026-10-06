package com.example.simplebrowser;

import android.webkit.JavascriptInterface;

import org.json.JSONObject;

import java.util.List;

/**
 * Built-in password manager page.
 */
public final class PasswordsPage {

    private final MainActivity activity;
    private final PasswordStore store;

    public PasswordsPage(
            MainActivity activity,
            PasswordStore store) {
        this.activity = activity;
        this.store = store;
    }

    public void show(
            BrowserTab tab) {
        show(tab, "");
    }

    public void show(
            BrowserTab tab,
            String selectedSite) {

        if (tab == null) return;

        tab.settingsPage = false;
        tab.defaultPage = false;
        tab.historyPage = false;
        tab.downloadsPage = false;
        tab.cookiesPage = false;
        tab.bookmarksPage = false;
        tab.profilesPage = false;
        tab.errorPage = false;
        tab.passwordsPage = true;
        tab.url = BrowserPage.PASSWORDS;
        tab.loading = false;
        tab.title =
                Localization.translate(
                        activity,
                        "Password manager");

        tab.webView.removeJavascriptInterface(
                "PasswordsPage");
        tab.webView.getSettings()
                .setJavaScriptEnabled(true);
        tab.webView.addJavascriptInterface(
                new Bridge(tab),
                "PasswordsPage");

        BrowserPage.load(
                tab.webView,
                BrowserPage.PASSWORDS,
                createHtml(selectedSite == null
                        ? ""
                        : selectedSite),
                BrowserPage.PASSWORDS);

        activity.setBrowserPageIcon(
                tab,
                BrowserIconDrawable.SECURE);
        activity.updateTabTitle(tab);
        activity.updateSecurity(tab);
        activity.updateNavigationButtonsForTabs();
    }

    public void remove(BrowserTab tab) {
        if (tab != null) {
            tab.webView.removeJavascriptInterface(
                    "PasswordsPage");
            tab.passwordsPage = false;
        }
    }

    private String createHtml(
            String selectedSite) {

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
                                95, 95, 95),
                        card,
                        4.5d);

        int button = accent;

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
        html.append("body{padding:16px;max-width:920px;margin:auto}");
        html.append("h1{margin:0 0 4px;font-size:26px}");
        html.append(".desc{color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";font-size:13px;line-height:1.45;margin-bottom:12px}");
        html.append(".toolbar{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:10px}");
        html.append("button,.button{border:1px solid ");
        html.append(ColorUtils.toHex(button));
        html.append(";background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";border-radius:5px;padding:9px 12px;font-weight:bold;text-decoration:none;display:inline-block}");
        html.append("a.site{text-decoration:none;color:inherit}");
        html.append(".site{display:flex;align-items:center;gap:12px;background:");
        html.append(ColorUtils.toHex(card));
        html.append(";border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";border-radius:9px;padding:12px;margin-top:9px;cursor:pointer}");
        html.append(".favicon{width:44px;height:44px;border-radius:9px;object-fit:contain;flex:none}");
        html.append(".fallback{width:44px;height:44px;border-radius:9px;background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";display:flex;align-items:center;justify-content:center;font-size:18px;font-weight:bold;flex:none}");
        html.append(".site-name{font-weight:bold;font-size:16px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}");
        html.append(".count{color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";font-size:12px;margin-top:3px}");
        html.append(".entry{background:");
        html.append(ColorUtils.toHex(card));
        html.append(";border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";border-radius:9px;padding:12px;margin-top:9px}");
        html.append(".username{font-weight:bold;font-size:15px}");
        html.append(".note{color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";font-size:12px;margin-top:4px;white-space:pre-wrap}");
        html.append(".empty{background:");
        html.append(ColorUtils.toHex(card));
        html.append(";border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";border-radius:9px;padding:14px;margin-top:10px}");
        html.append("</style></head><body>");

        html.append("<h1>");
        html.append(escape(t("passwords.title")));
        html.append("</h1>");

        html.append("<div class='desc'>");
        html.append(escape(t("passwords.security_desc")));
        html.append("</div>");

        html.append("<div class='toolbar'>");
        html.append("<button onclick='importPasswords()'>");
        html.append(escape(t("passwords.import")));
        html.append("</button>");
        html.append("<button onclick='exportPasswords()'>");
        html.append(escape(t("passwords.export")));
        html.append("</button>");
        if (!selectedSite.trim().isEmpty()) {
            html.append("<button type='button' onclick='showSites()'>");
            html.append(escape(t("passwords.all_sites")));
            html.append("</button>");
        }
        html.append("</div>");

        if (selectedSite.trim().isEmpty()) {
            html.append(createSiteList(button, secondary));
        } else {
            html.append(createSiteDetails(
                    selectedSite,
                    button,
                    secondary));
        }

        html.append("<script>");
        html.append("function importPasswords(){PasswordsPage.importPasswords()}");
        html.append("function exportPasswords(){PasswordsPage.exportPasswords()}");
        html.append("function showSite(s){PasswordsPage.openSite(s)}");
        html.append("function showSites(){PasswordsPage.openSite('')}");
        html.append("function reveal(id){var p=PasswordsPage.getPassword(id);alert(");
        html.append(js(t("passwords.revealed")));
        html.append("+p)}");
        html.append("function del(id){if(confirm(");
        html.append(js(t("passwords.confirm_delete")));
        html.append(")){PasswordsPage.deleteEntry(id)}}");
        html.append("</script></body></html>");

        return Localization.translateHtml(
                activity,
                html.toString());
    }

    private String createSiteList(
            int button,
            int secondary) {

        StringBuilder html =
                new StringBuilder();

        try {
            List<PasswordStore.SiteGroup> groups =
                    store.getSiteGroups();

            if (groups.isEmpty()) {
                html.append("<div class='empty'>");
                html.append(escape(
                        t("passwords.empty")));
                html.append("</div>");
                return html.toString();
            }

            for (PasswordStore.SiteGroup group :
                    groups) {

                html.append("<div class='site' role='button' tabindex='0' onclick='showSite(");
                html.append(js(group.site));
                html.append(")'>");

                appendFavicon(
                        html,
                        group.faviconBase64,
                        group.label,
                        button);

                html.append("<div style='min-width:0'>");
                html.append("<div class='site-name'>");
                html.append(escape(group.label));
                html.append("</div>");
                html.append("<div class='count'>");
                html.append(group.count);
                html.append(" ");
                html.append(escape(
                        group.count == 1
                                ? t("passwords.saved_one")
                                : t("passwords.saved_many")));
                html.append("</div></div>");
                html.append("</div>");
            }

        } catch (Exception exception) {
            html.append("<div class='empty'>");
            html.append(escape(t("passwords.error")));
            html.append("</div>");
        }

        return html.toString();
    }

    private String createSiteDetails(
            String selectedSite,
            int button,
            int secondary) {

        StringBuilder html =
                new StringBuilder();

        try {
            List<PasswordStore.Entry> entries =
                    store.getEntriesForSite(
                            selectedSite);

            html.append("<div class='site'>");
            String label =
                    entries.isEmpty()
                            ? selectedSite
                            : siteLabel(entries.get(0).site);

            String favicon =
                    entries.isEmpty()
                            ? ""
                            : entries.get(0).faviconBase64;

            appendFavicon(
                    html,
                    favicon,
                    label,
                    button);

            html.append("<div class='site-name'>");
            html.append(escape(label));
            html.append("</div></div>");

            if (entries.isEmpty()) {
                html.append("<div class='empty'>");
                html.append(escape(
                        t("passwords.no_entries")));
                html.append("</div>");
                return html.toString();
            }

            for (PasswordStore.Entry entry :
                    entries) {

                html.append("<div class='entry'>");
                html.append("<div class='username'>");
                html.append(escape(entry.username));
                html.append("</div>");

                if (entry.note != null &&
                        !entry.note.trim().isEmpty()) {
                    html.append("<div class='note'>");
                    html.append(escape(entry.note));
                    html.append("</div>");
                }

                html.append("<div class='toolbar'>");
                html.append("<button onclick='reveal(");
                html.append(entry.id);
                html.append(")'>");
                html.append(escape(
                        t("passwords.reveal")));
                html.append("</button>");

                html.append("<button onclick='del(");
                html.append(entry.id);
                html.append(")'>");
                html.append(escape(
                        t("common.delete")));
                html.append("</button>");
                html.append("</div></div>");
            }

        } catch (Exception exception) {
            html.append("<div class='empty'>");
            html.append(escape(t("passwords.error")));
            html.append("</div>");
        }

        return html.toString();
    }

    private void appendFavicon(
            StringBuilder html,
            String encoded,
            String label,
            int button) {

        if (encoded != null &&
                !encoded.isEmpty()) {

            html.append("<img class='favicon' src='data:image/png;base64,");
            html.append(attribute(encoded));
            html.append("'>");

        } else {

            html.append("<div class='fallback'>");
            html.append(escape(initial(label)));
            html.append("</div>");
        }
    }

    private String siteLabel(String site) {
        try {
            android.net.Uri uri =
                    android.net.Uri.parse(site);

            if (uri.getHost() != null &&
                    !uri.getHost().isEmpty()) {
                return uri.getHost();
            }
        } catch (Throwable ignored) {
        }

        return site;
    }

    private String initial(String value) {
        if (value == null ||
                value.trim().isEmpty()) {
            return "?";
        }

        return String.valueOf(
                Character.toUpperCase(
                        value.trim().charAt(0)));
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

    private String js(String value) {
        if (value == null) return "''";
        return "'" +
                value.replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\r", "\\r")
                        .replace("\n", "\\n") +
                "'";
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
        public void openSite(String site) {
            activity.runOnUiThread(
                    () -> show(tab, site));
        }

        @JavascriptInterface
        public String getPassword(long id) {
            try {
                PasswordStore.Entry entry =
                        store.get(id);

                return entry == null
                        ? ""
                        : entry.password;
            } catch (Exception exception) {
                return "";
            }
        }

        @JavascriptInterface
        public void deleteEntry(long id) {
            store.delete(id);
            activity.runOnUiThread(
                    () -> show(tab));
        }

        @JavascriptInterface
        public void importPasswords() {
            activity.startPasswordImport(tab);
        }

        @JavascriptInterface
        public void exportPasswords() {
            activity.startPasswordExport(tab);
        }
    }
}
