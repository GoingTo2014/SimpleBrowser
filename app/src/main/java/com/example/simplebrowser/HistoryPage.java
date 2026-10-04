package com.example.simplebrowser;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class HistoryPage {

    private final MainActivity activity;
    private final BrowserHistory history;

    public HistoryPage(
            MainActivity activity,
            BrowserHistory history) {

        this.activity = activity;
        this.history = history;
    }

    public void show(
            BrowserTab tab,
            String query) {

        tab.settingsPage = false;
        tab.errorPage = false;
        tab.defaultPage = false;
        tab.historyPage = true;
        tab.loading = false;
        tab.url = "browser://history";
        tab.title = "History";

        WebView webView = tab.webView;

        webView.removeJavascriptInterface(
                "HistoryPage");

        webView.getSettings()
                .setJavaScriptEnabled(true);

        webView.addJavascriptInterface(
                new Bridge(tab),
                "HistoryPage");

        webView.loadDataWithBaseURL(
                "https://browser.local/history",
                createHtml(
                        query,
                        tab.isIncognito),
                "text/html",
                "UTF-8",
                null);

        activity.updateTabTitle(tab);
        activity.setUrlText("browser://history");
        activity.updateSecurity(tab);
    }

    public void remove(BrowserTab tab) {

        if (tab == null) {
            return;
        }

        tab.webView.removeJavascriptInterface(
                "HistoryPage");
        tab.historyPage = false;
    }

    private String createHtml(
            String query,
            boolean incognito) {

        List<BrowserHistory.Entry> entries =
                incognito
                        ? java.util.Collections
                                .<BrowserHistory.Entry>
                                emptyList()
                        : history.getEntries(query);

        String background =
                incognito
                        ? "#202124"
                        : "#F6F7F9";

        String surface =
                incognito
                        ? "#303134"
                        : "#FFFFFF";

        String text =
                incognito
                        ? "#FFFFFF"
                        : "#202124";

        String secondary =
                incognito
                        ? "#B9B9B9"
                        : "#5F6368";

        String border =
                incognito
                        ? "#4A4B4F"
                        : "#E1E4E8";

        StringBuilder html =
                new StringBuilder();

        html.append(
                "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' " +
                "content='width=device-width,initial-scale=1'>");

        html.append("<style>");

        html.append(
                "html,body{margin:0;padding:0;" +
                "background:" + background + ";" +
                "color:" + text + ";" +
                "font-family:sans-serif;}");

        html.append(
                ".page{max-width:920px;margin:0 auto;" +
                "padding:22px 18px 30px;}");

        html.append(
                "h1{margin:0;font-size:27px;}");

        html.append(
                ".sub{margin:5px 0 18px;color:" +
                secondary + ";font-size:13px;}");

        html.append(
                ".top{display:flex;gap:8px;" +
                "margin-bottom:18px;align-items:center;}");

        html.append(
                "input{flex:1;min-width:0;height:42px;" +
                "box-sizing:border-box;padding:0 12px;" +
                "font-size:15px;border:1px solid " +
                border + ";border-radius:9px;" +
                "background:" + surface + ";color:" + text + ";}");

        html.append(
                "button{height:42px;flex:0 0 auto;" +
                "border:0;border-radius:9px;" +
                "padding:0 13px;background:#202124;color:#FFF;}");

        html.append(
                ".entry{background:" + surface + ";" +
                "border:1px solid " + border + ";" +
                "border-radius:12px;padding:14px;" +
                "margin-bottom:10px;}");

        html.append(
                ".row{display:flex;gap:12px;" +
                "align-items:flex-start;}");

        /*
         * min-width:0 is important here. Without it, a very long
         * URL can force the flex item wider than the page and push
         * the Delete button off-screen on older WebViews.
         */
        html.append(
                ".grow{flex:1;min-width:0;width:0;}");

        html.append(
                ".title{font-size:15px;font-weight:600;" +
                "line-height:20px;word-break:break-word;}");

        html.append(
                ".title a{color:" + text + ";" +
                "text-decoration:none;}");

        html.append(
                ".url{margin-top:5px;font-size:12px;" +
                "line-height:17px;color:" + secondary + ";" +
                "white-space:normal;word-break:break-all;" +
                "overflow-wrap:break-word;max-width:100%;}");

        html.append(
                ".time{margin-top:7px;font-size:12px;" +
                "color:" + secondary + ";}");

        html.append(
                ".delete{background:#B3261E;" +
                "padding:0 11px;}");

        html.append(
                ".empty{margin-top:22px;color:" +
                secondary + ";font-size:14px;}");

        html.append("</style></head><body>");
        html.append("<div class='page'>");
        html.append("<h1>History</h1>");

        if (incognito) {

            html.append(
                    "<div class='sub'>" +
                    "Browsing history is not saved in Incognito Mode." +
                    "</div>");

        } else {

            html.append(
                    "<div class='sub'>" +
                    "Pages you've visited in Simple Browser." +
                    "</div>");

            html.append("<div class='top'>");

            html.append(
                    "<input id='search' " +
                    "placeholder='Search history' " +
                    "value='" +
                    escape(query) +
                    "' onkeydown='searchKey(event)'>");

            html.append(
                    "<button onclick='searchNow()'>Search</button>");

            html.append(
                    "<button onclick='clearAll()'>Clear</button>");

            html.append("</div>");
        }

        if (entries.isEmpty()) {

            html.append(
                    "<div class='empty'>" +
                    (incognito
                            ? "Nothing is shown here while browsing privately."
                            : "No history entries found.") +
                    "</div>");

        } else {

            DateFormat format =
                    DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM,
                            DateFormat.SHORT);

            for (BrowserHistory.Entry entry :
                    entries) {

                String title =
                        entry.title == null ||
                        entry.title.trim().isEmpty()
                                ? entry.url
                                : entry.title;

                html.append(
                        "<div class='entry'><div class='row'>" +
                        "<div class='grow'>");

                html.append(
                        "<div class='title'>" +
                        "<a href='javascript:openEntry(" +
                        entry.id +
                        ")'>" +
                        escape(title) +
                        "</a></div>");

                html.append(
                        "<div class='url'>" +
                        escape(entry.url) +
                        "</div>");

                html.append(
                        "<div class='time'>" +
                        escape(
                                format.format(
                                        new Date(entry.time))) +
                        "</div>");

                html.append("</div>");

                html.append(
                        "<button class='delete' " +
                        "onclick='removeEntry(" +
                        entry.id +
                        ")'>Delete</button>");

                html.append(
                        "</div></div>");
            }
        }

        html.append("</div>");

        html.append("<script>");

        if (!incognito) {

            html.append(
                    "function searchNow(){" +
                    "HistoryPage.search(" +
                    "document.getElementById('search').value);}");

            html.append(
                    "function searchKey(e){" +
                    "if(e.keyCode===13)searchNow();}");

            html.append(
                    "function clearAll(){HistoryPage.clear();}");

            html.append(
                    "function removeEntry(id){" +
                    "HistoryPage.remove(id);}");

            html.append(
                    "function openEntry(id){" +
                    "HistoryPage.open(id);}");
        }

        html.append("</script>");
        html.append("</body></html>");

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

    private class Bridge {

        private final BrowserTab tab;

        Bridge(BrowserTab tab) {
            this.tab = tab;
        }

        @JavascriptInterface
        public void search(String value) {

            activity.runOnUiThread(
                    () -> show(
                            tab,
                            value));
        }

        @JavascriptInterface
        public void open(long id) {

            activity.runOnUiThread(
                    () -> {

                        List<BrowserHistory.Entry> entries =
                                history.getEntries("");

                        for (BrowserHistory.Entry entry :
                                entries) {

                            if (entry.id == id) {

                                activity.openUrlOrSearchForTab(
                                        tab,
                                        entry.url);

                                return;
                            }
                        }
                    });
        }

        @JavascriptInterface
        public void remove(long id) {

            activity.runOnUiThread(
                    () -> {

                        history.delete(id);
                        show(tab, "");
                    });
        }

        @JavascriptInterface
        public void clear() {

            activity.runOnUiThread(
                    () -> {

                        history.clear();

                        Toast.makeText(
                                activity,
                                "History cleared",
                                Toast.LENGTH_SHORT)
                                .show();

                        show(tab, "");
                    });
        }
    }
}
