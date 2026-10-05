package com.example.simplebrowser;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HistoryPage {

    private static final int PAGE_SIZE = 25;

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
        tab.downloadsPage = false;
        tab.loading = false;
        tab.url = "browser://history";
        tab.title = "History";

        WebView webView =
                tab.webView;

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
        activity.setUrlText(
                "browser://history");
        activity.updateSecurity(tab);
    }

    public void remove(
            BrowserTab tab) {

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
                        ? Collections
                                .<BrowserHistory.Entry>
                                emptyList()
                        : history.getEntries(
                                query,
                                PAGE_SIZE,
                                0);

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
                "html,body{margin:0;padding:0;overflow-x:hidden;" +
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
                ".list{margin-top:2px;}");

        html.append(
                ".loading{text-align:center;padding:12px;" +
                "color:" + secondary + ";display:none;}");

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

        html.append("<div id='list' class='list'>");

        for (BrowserHistory.Entry entry :
                entries) {

            html.append(
                    entryHtml(
                            entry,
                            text,
                            secondary));
        }

        if (entries.isEmpty()) {

            html.append(
                    "<div id='empty' class='loading' " +
                    "style='display:block;'>");

            html.append(
                    incognito
                            ? "Nothing is shown here while browsing privately."
                            : "No history entries found.");

            html.append("</div>");
        }

        html.append("</div>");

        if (!incognito &&
                !entries.isEmpty()) {

            html.append(
                    "<div id='loading' class='loading'>" +
                    "Loading more..." +
                    "</div>");
        }

        html.append("</div>");

        if (!incognito) {

            html.append("<script>");

            html.append(
                    "var offset=" +
                    entries.size() +
                    ",loading=false,done=" +
                    (entries.size() < PAGE_SIZE
                            ? "false"
                            : "false") +
                    ";");

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

            html.append(
                    "function esc(v){" +
                    "return String(v==null?'':v)" +
                    ".replace(/&/g,'&amp;')" +
                    ".replace(/</g,'&lt;')" +
                    ".replace(/>/g,'&gt;')" +
                    ".replace(/\"/g,'&quot;')" +
                    ".replace(/'/g,'&#39;');}");

            html.append(
                    "function appendItems(json){" +
                    "var data=JSON.parse(json);" +
                    "var list=document.getElementById('list');" +
                    "var empty=document.getElementById('empty');" +
                    "if(empty)empty.style.display='none';" +
                    "for(var i=0;i<data.length;i++){" +
                    "var e=data[i];" +
                    "var title=e.title&&e.title.length?e.title:e.url;" +
                    "var s='<div class=\\'entry\\'><div class=\\'row\\'>" +
                    "<div class=\\'grow\\'>" +
                    "<div class=\\'title\\'><a href=\\'javascript:openEntry(" +
                    "'+e.id+')\\'>'+esc(title)+'</a></div>'+" +
                    "'<div class=\\'url\\'>'+esc(e.url)+'</div>'+" +
                    "'<div class=\\'time\\'>'+esc(e.timeText)+'</div>'+" +
                    "'</div><button class=\\'delete\\' onclick=\\'removeEntry(" +
                    "'+e.id+')\\'>Delete</button></div></div>';" +
                    "list.insertAdjacentHTML('beforeend',s);" +
                    "}" +
                    "offset+=data.length;" +
                    "if(data.length===0)done=true;}");

            html.append(
                    "function loadMore(){" +
                    "if(loading||done)return;" +
                    "loading=true;" +
                    "document.getElementById('loading').style.display='block';" +
                    "var json=HistoryPage.loadMore(" +
                    "document.getElementById('search').value,offset);" +
                    "appendItems(json);" +
                    "loading=false;" +
                    "document.getElementById('loading').style.display='none';}");

            html.append(
                    "window.onscroll=function(){" +
                    "if(window.innerHeight+window.pageYOffset >= " +
                    "document.body.offsetHeight-500)loadMore();" +
                    "};");

            html.append("</script>");
        }

        html.append("</body></html>");

        return html.toString();
    }

    private String entryHtml(
            BrowserHistory.Entry entry,
            String text,
            String secondary) {

        DateFormat format =
                DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM,
                        DateFormat.SHORT);

        String title =
                entry.title == null ||
                entry.title.trim().isEmpty()
                        ? entry.url
                        : entry.title;

        return
                "<div class='entry'><div class='row'>" +
                "<div class='grow'>" +
                "<div class='title'>" +
                "<a href='javascript:openEntry(" +
                entry.id +
                ")'>" +
                escape(title) +
                "</a></div>" +
                "<div class='url'>" +
                escape(entry.url) +
                "</div>" +
                "<div class='time'>" +
                escape(
                        format.format(
                                new Date(entry.time))) +
                "</div>" +
                "</div>" +
                "<button class='delete' " +
                "onclick='removeEntry(" +
                entry.id +
                ")'>Delete</button>" +
                "</div></div>";
    }

    private String escape(
            String value) {

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
        public void search(
                String value) {

            activity.runOnUiThread(
                    () -> show(
                            tab,
                            value));
        }

        @JavascriptInterface
        public String loadMore(
                String query,
                int offset) {

            try {

                List<BrowserHistory.Entry> entries =
                        history.getEntries(
                                query,
                                PAGE_SIZE,
                                Math.max(
                                        0,
                                        offset));

                JSONArray array =
                        new JSONArray();

                DateFormat format =
                        DateFormat.getDateTimeInstance(
                                DateFormat.MEDIUM,
                                DateFormat.SHORT);

                for (BrowserHistory.Entry entry :
                        entries) {

                    JSONObject object =
                            new JSONObject();

                    String title =
                            entry.title == null ||
                            entry.title.trim().isEmpty()
                                    ? entry.url
                                    : entry.title;

                    object.put(
                            "id",
                            entry.id);

                    object.put(
                            "url",
                            entry.url);

                    object.put(
                            "title",
                            title);

                    object.put(
                            "timeText",
                            format.format(
                                    new Date(
                                            entry.time)));

                    array.put(object);
                }

                return array.toString();

            } catch (Exception e) {

                return "[]";
            }
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
