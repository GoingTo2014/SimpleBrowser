package com.example.simplebrowser;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

public class CookiesPage {

    private final MainActivity activity;
    private final CookieStore store;

    public CookiesPage(
            MainActivity activity,
            CookieStore store) {
        this.activity = activity;
        this.store = store;
    }

    public void show(
            BrowserTab tab) {

        tab.settingsPage = false;
        tab.errorPage = false;
        tab.defaultPage = false;
        tab.historyPage = false;
        tab.downloadsPage = false;
        tab.cookiesPage = true;
        tab.loading = false;
        tab.url = BrowserPage.COOKIES;
        tab.title = "Cookies";

        WebView webView =
                tab.webView;

        webView.removeJavascriptInterface(
                "CookiesPage");

        webView.getSettings()
                .setJavaScriptEnabled(true);

        webView.addJavascriptInterface(
                new Bridge(tab),
                "CookiesPage");

        BrowserPage.load(
                webView,
                BrowserPage.COOKIES,
                createHtml(tab.isIncognito),
                null);

        activity.updateTabTitle(tab);
        activity.setUrlText(
                BrowserPage.COOKIES);
        activity.updateSecurity(tab);
    }

    public void remove(
            BrowserTab tab) {

        if (tab == null) {
            return;
        }

        tab.webView.removeJavascriptInterface(
                "CookiesPage");

        tab.cookiesPage = false;
    }

    private String createHtml(
            boolean incognito) {

        String background =
                incognito ? "#202124" : "#F6F7F9";
        String surface =
                incognito ? "#303134" : "#FFFFFF";
        String text =
                incognito ? "#FFFFFF" : "#202124";
        String secondary =
                incognito ? "#B9B9B9" : "#5F6368";
        String border =
                incognito ? "#4A4B4F" : "#E1E4E8";

        StringBuilder html =
                new StringBuilder();

        html.append(
                "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' " +
                "content='width=device-width,initial-scale=1'>");
        html.append("<style>");
        html.append(
                "html,body{margin:0;padding:0;overflow-x:hidden;" +
                "background:" + background + ";" +
                "color:" + text + ";font-family:sans-serif;}");
        html.append(
                ".page{max-width:920px;margin:0 auto;" +
                "padding:22px 18px 30px;}");
        html.append("h1{margin:0;font-size:27px;}");
        html.append(
                ".sub{margin:5px 0 18px;color:" +
                secondary + ";font-size:13px;}");
        html.append(
                ".entry{background:" + surface + ";" +
                "border:1px solid " + border + ";" +
                "border-radius:12px;padding:14px;margin-bottom:10px;}");
        html.append(
                ".domain{font-size:17px;font-weight:600;margin-bottom:10px;}");
        html.append(
                ".cookie{display:flex;gap:8px;align-items:center;" +
                "padding:8px 0;border-top:1px solid " +
                border + ";}");
        html.append(
                ".name{font-weight:600;word-break:break-word;}");
        html.append(
                ".value{color:" + secondary + ";" +
                "word-break:break-all;min-width:0;}");
        html.append(
                ".grow{flex:1;min-width:0;}");
        html.append(
                "button{border:0;border-radius:8px;height:38px;" +
                "padding:0 12px;background:#202124;color:#FFF;}");
        html.append(
                ".danger{background:#B3261E;}");
        html.append(
                ".empty{text-align:center;padding:50px 10px;" +
                "color:" + secondary + ";}");
        html.append("</style></head><body>");
        html.append("<div class='page'>");
        html.append("<h1>Cookies</h1>");

        if (incognito) {

            html.append(
                    "<div class='sub'>" +
                    "Cookie data is temporary in Incognito Mode." +
                    "</div>");

        } else {

            html.append(
                    "<div class='sub'>" +
                    "Websites that currently have cookies stored by Simple Browser." +
                    "</div>");
        }

        if (incognito) {
            html.append(
                    "<div class='empty'>Cookies are not managed from Incognito Mode.</div>");
        } else {

            int shown = 0;

            for (String domain :
                    store.getDomains()) {

                List<CookieStore.CookieValue> cookies =
                        store.parseCookies(
                                store.getCookies(domain));

                if (cookies.isEmpty()) {
                    continue;
                }

                shown++;

                html.append(
                        "<div class='entry'>");
                html.append(
                        "<div class='domain'>")
                        .append(
                                escape(domain))
                        .append("</div>");

                for (CookieStore.CookieValue cookie :
                        cookies) {

                    html.append(
                            "<div class='cookie'>");
                    html.append(
                            "<div class='grow'>");
                    html.append(
                            "<div class='name'>")
                            .append(
                                    escape(cookie.name))
                            .append("</div>");
                    html.append(
                            "<div class='value'>")
                            .append(
                                    escape(cookie.value))
                            .append("</div>");
                    html.append("</div>");
                    html.append(
                            "<button onclick=\"CookiesPage.edit('" +
                            js(cookie.name) +
                            "','" +
                            js(domain) +
                            "')\">Edit</button>");
                    html.append(
                            "<button class='danger' " +
                            "onclick=\"CookiesPage.deleteCookie('" +
                            js(cookie.name) +
                            "','" +
                            js(domain) +
                            "')\">Delete</button>");
                    html.append("</div>");
                }

                html.append(
                        "<button class='danger' " +
                        "onclick=\"CookiesPage.deleteDomain('" +
                        js(domain) +
                        "')\">Delete site cookies</button>");
                html.append("</div>");
            }

            if (shown == 0) {
                html.append(
                        "<div class='empty'>" +
                        "No cookies are currently known." +
                        "</div>");
            }
        }

        html.append("</div></body></html>");

        return html.toString();
    }

    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String js(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }

    private class Bridge {

        private final BrowserTab tab;

        Bridge(BrowserTab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public void edit(
                String name,
                String domain) {

            activity.runOnUiThread(
                    () -> {

                        String current =
                                findValue(
                                        domain,
                                        name);

                        EditText input =
                                new EditText(activity);

                        input.setSingleLine(true);
                        input.setInputType(
                                InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_NORMAL);
                        input.setText(current);
                        input.setSelection(
                                input.length());

                        new AlertDialog.Builder(activity)
                                .setTitle(
                                        "Edit cookie: " + name)
                                .setView(input)
                                .setNegativeButton(
                                        "Cancel",
                                        null)
                                .setPositiveButton(
                                        "Save",
                                        (dialog, which) -> {

                                            store.setCookie(
                                                    domain,
                                                    name,
                                                    input.getText()
                                                            .toString());

                                            show(tab);
                                        })
                                .show();
                    });
        }

        @JavascriptInterface
        public void deleteCookie(
                String name,
                String domain) {

            activity.runOnUiThread(
                    () -> {

                        store.deleteCookie(
                                domain,
                                name);

                        show(tab);
                    });
        }

        @JavascriptInterface
        public void deleteDomain(
                String domain) {

            activity.runOnUiThread(
                    () -> {

                        store.removeDomain(
                                domain);

                        Toast.makeText(
                                activity,
                                "Site cookies deleted",
                                Toast.LENGTH_SHORT)
                                .show();

                        show(tab);
                    });
        }
    }

    private String findValue(
            String domain,
            String name) {

        for (CookieStore.CookieValue cookie :
                store.parseCookies(
                        store.getCookies(domain))) {

            if (cookie.name.equals(name)) {
                return cookie.value;
            }
        }

        return "";
    }
}
