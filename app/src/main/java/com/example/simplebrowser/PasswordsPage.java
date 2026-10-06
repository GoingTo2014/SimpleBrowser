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

        if (tab == null) {
            return;
        }

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
        tab.title =
                Localization.translate(
                        activity,
                        "Password manager");

        BrowserPage.load(
                tab.webView,
                BrowserPage.PASSWORDS,
                createHtml(),
                BrowserPage.PASSWORDS);

        activity.setBrowserPageIcon(
                tab,
                BrowserIconDrawable.SECURE);

        activity.updateTabTitle(tab);
        activity.updateSecurity(tab);
        activity.settingsLoaded(tab);
    }

    public void remove(
            BrowserTab tab) {

        if (tab != null) {
            tab.passwordsPage = false;
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

        StringBuilder html =
                new StringBuilder();

        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta name='viewport' content='width=device-width,initial-scale=1'>");
        html.append("<style>*{box-sizing:border-box}html,body{margin:0;padding:0;background:");
        html.append(ColorUtils.toHex(content));
        html.append(";color:");
        html.append(ColorUtils.toHex(text));
        html.append(";font-family:sans-serif}body{padding:16px;max-width:900px;margin:auto}");
        html.append("h1{margin:0;font-size:26px}.warning{color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";font-size:13px;margin:5px 0 14px}.card{background:");
        html.append(ColorUtils.toHex(card));
        html.append(";border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";border-radius:8px;padding:12px;margin-top:10px}");
        html.append("label{display:block;font-weight:bold;font-size:13px;margin-top:9px}");
        html.append("input,textarea{width:100%;padding:9px;margin-top:4px;border:1px solid ");
        html.append(ColorUtils.toHex(secondary));
        html.append(";background:");
        html.append(ColorUtils.toHex(card));
        html.append(";color:");
        html.append(ColorUtils.toHex(text));
        html.append("}textarea{min-height:70px;resize:vertical}");
        html.append(".actions{display:flex;flex-wrap:wrap;gap:7px;margin-top:10px}");
        html.append("button{border:1px solid ");
        html.append(ColorUtils.toHex(button));
        html.append(";background:");
        html.append(ColorUtils.toHex(button));
        html.append(";color:");
        html.append(ColorUtils.toHex(ColorUtils.getReadableTextColor(button)));
        html.append(";border-radius:5px;padding:9px 12px;font-weight:bold}");
        html.append(".site{font-weight:bold}.user{font-size:13px;color:");
        html.append(ColorUtils.toHex(secondary));
        html.append(";margin-top:3px}");
        html.append("@media(max-width:420px){body{padding:10px}.actions button{flex:1 1 120px}}");
        html.append("</style></head><body>");

        html.append("<h1>");
        html.append(escape(t("passwords.title")));
        html.append("</h1>");
        html.append("<div class='warning'>");
        html.append(escape(t("passwords.security_desc")));
        html.append("</div>");

        html.append("<div class='card'>");
        html.append("<strong>");
        html.append(escape(t("passwords.add")));
        html.append("</strong>");

        html.append("<input id='entry-id' type='hidden' value='0'>");
        html.append("<label>");
        html.append(escape(t("passwords.site")));
        html.append("</label>");
        html.append("<input id='site' type='text' autocomplete='off'>");

        html.append("<label>");
        html.append(escape(t("passwords.username")));
        html.append("</label>");
        html.append("<input id='username' type='text' autocomplete='off'>");

        html.append("<label>");
        html.append(escape(t("passwords.password")));
        html.append("</label>");
        html.append("<input id='password' type='password' autocomplete='new-password'>");

        html.append("<label>");
        html.append(escape(t("passwords.note")));
        html.append("</label>");
        html.append("<textarea id='note'></textarea>");

        html.append("<div class='actions'>");
        html.append("<button onclick='saveEntry()'>");
        html.append(escape(t("passwords.save")));
        html.append("</button>");
        html.append("<button onclick='clearEditor()'>");
        html.append(escape(t("passwords.clear")));
        html.append("</button>");
        html.append("</div></div>");

        List<PasswordStore.Entry> entries;

        try {
            entries = store.getEntries();
        } catch (Exception exception) {
            entries = new java.util.ArrayList<>();
            html.append("<div class='card'>");
            html.append(escape(t("passwords.error")));
            html.append("</div>");
        }

        if (entries != null &&
                entries.isEmpty()) {

            html.append("<div class='card'>");
            html.append(escape(t("passwords.empty")));
            html.append("</div>");
        }

        if (entries != null) {
            for (PasswordStore.Entry entry :
                    entries) {

                html.append("<div class='card'>");
                html.append("<div class='site'>");
                html.append(escape(entry.site));
                html.append("</div>");
                html.append("<div class='user'>");
                html.append(escape(entry.username));
                html.append("</div>");
                html.append("<div class='actions'>");

                html.append("<button onclick='editEntry(");
                html.append(entry.id);
                html.append(")'>");
                html.append(escape(t("passwords.edit")));
                html.append("</button>");

                html.append("<button onclick='revealPassword(");
                html.append(entry.id);
                html.append(")'>");
                html.append(escape(t("passwords.reveal")));
                html.append("</button>");

                html.append("<button onclick='deleteEntry(");
                html.append(entry.id);
                html.append(")'>");
                html.append(escape(t("common.delete")));
                html.append("</button>");

                html.append("</div></div>");
            }
        }

        html.append("<script>");
        html.append("function saveEntry(){Android.saveEntry(");
        html.append("parseInt(document.getElementById('entry-id').value||'0'),");
        html.append("document.getElementById('site').value,");
        html.append("document.getElementById('username').value,");
        html.append("document.getElementById('password').value,");
        html.append("document.getElementById('note').value)}");

        html.append("function clearEditor(){");
        html.append("document.getElementById('entry-id').value='0';");
        html.append("document.getElementById('site').value='';");
        html.append("document.getElementById('username').value='';");
        html.append("document.getElementById('password').value='';");
        html.append("document.getElementById('note').value='';}");

        html.append("function editEntry(id){");
        html.append("var value=JSON.parse(Android.getEntry(id));");
        html.append("document.getElementById('entry-id').value=value.id;");
        html.append("document.getElementById('site').value=value.site;");
        html.append("document.getElementById('username').value=value.username;");
        html.append("document.getElementById('password').value=value.password;");
        html.append("document.getElementById('note').value=value.note;");
        html.append("window.scrollTo(0,0);}");

        html.append("function revealPassword(id){");
        html.append("var p=Android.getPassword(id);");
        html.append("alert(");
        html.append(js(t("passwords.revealed")));
        html.append("+p);}");

        html.append("function deleteEntry(id){");
        html.append("if(confirm(");
        html.append(js(t("passwords.confirm_delete")));
        html.append(")){Android.deleteEntry(id)}}");

        html.append("</script></body></html>");

        return Localization.translateHtml(
                activity,
                html.toString());
    }

    private String t(String key) {
        return Localization.translate(
                activity,
                key);
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

    private String js(String value) {
        if (value == null) {
            return "''";
        }

        return "'" +
                value.replace("\", "\\")
                        .replace("'", "\'")
                        .replace("", "\r")
                        .replace("
", "\n") +
                "'";
    }

    public final class Bridge {

        private final BrowserTab tab;

        public Bridge(BrowserTab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public String getEntry(long id) {

            try {
                PasswordStore.Entry entry =
                        store.get(id);

                if (entry == null) {
                    return "{}";
                }

                return new JSONObject()
                        .put("id", entry.id)
                        .put("site", entry.site)
                        .put("username", entry.username)
                        .put("password", entry.password)
                        .put("note", entry.note)
                        .toString();

            } catch (Exception exception) {
                return "{}";
            }
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
        public void saveEntry(
                long id,
                String site,
                String username,
                String password,
                String note) {

            try {
                store.save(
                        id,
                        site,
                        username,
                        password,
                        note);

                activity.runOnUiThread(
                        () -> show(tab));

            } catch (Exception exception) {
                activity.runOnUiThread(
                        () -> activity.showPasswordManagerError(
                                t("passwords.save_failed")));
            }
        }

        @JavascriptInterface
        public void deleteEntry(long id) {

            store.delete(id);

            activity.runOnUiThread(
                    () -> show(tab));
        }
    }
}
