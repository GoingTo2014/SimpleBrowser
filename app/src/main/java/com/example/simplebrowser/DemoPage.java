package com.example.simplebrowser;

import android.webkit.WebView;

public final class DemoPage {

    private final MainActivity activity;

    public DemoPage(MainActivity activity) {
        this.activity = activity;
    }

    public void show(BrowserTab tab) {

        if (tab == null) {
            return;
        }

        tab.settingsPage = false;
        tab.errorPage = false;
        tab.defaultPage = false;
        tab.historyPage = false;
        tab.downloadsPage = false;
        tab.cookiesPage = false;
        tab.loading = false;
        tab.url = BrowserPage.DEMO;
        tab.title =
                Localization.translate(
                        activity,
                        "HTML Demo");

        WebView webView = tab.webView;

        BrowserPage.load(
                webView,
                BrowserPage.DEMO,
                createHtml(),
                null);

        activity.updateTabTitle(tab);
        activity.setUrlText(BrowserPage.DEMO);
        activity.updateSecurity(tab);
        activity.setLoading(false);
    }

    private String t(String key) {
        return Localization.translate(
                activity,
                key);
    }

    private String createHtml() {

        String html =
                "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<title>" + t("demo.title") + "</title>" +
                "</head><body>" +

                "<h1>" + t("demo.title") + "</h1>" +
                "<p>" + t("demo.intro") + "</p>" +
                "<p>" + t("demo.description") + "</p>" +

                "<h2>" + t("demo.headings") + "</h2>" +
                "<h1>Heading 1</h1>" +
                "<h2>Heading 2</h2>" +
                "<h3>Heading 3</h3>" +
                "<h4>Heading 4</h4>" +
                "<h5>Heading 5</h5>" +
                "<h6>Heading 6</h6>" +
                "<p>Plain text, <strong>bold</strong>, <em>italic</em>, " +
                "<u>underlined</u>, <mark>marked</mark>, " +
                "<small>small</small>, <del>deleted</del>, " +
                "<ins>inserted</ins>, H<sub>2</sub>O, x<sup>2</sup>.</p>" +
                "<p><a href='https://example.com/'>Link</a></p>" +
                "<hr>" +

                "<h2>" + t("demo.lists") + "</h2>" +
                "<p>Unordered list</p>" +
                "<ul><li>First item</li><li>Second item</li><li>Third item</li></ul>" +
                "<p>Ordered list</p>" +
                "<ol><li>First item</li><li>Second item</li><li>Third item</li></ol>" +

                "<h2>" + t("demo.controls") + "</h2>" +
                "<form onsubmit='return submitForm();' onreset='resetForm();'>" +

                "<fieldset>" +
                "<legend>" + t("demo.text_inputs") + "</legend>" +
                "<p><label>Text box: <input type='text' value='Example text'></label></p>" +
                "<p><label>Password: <input type='password' value='password'></label></p>" +
                "<p><label>Email: <input type='email' value='user@example.com'></label></p>" +
                "<p><label>URL: <input type='url' value='https://example.com/'></label></p>" +
                "<p><label>Search: <input type='search' value='Search text'></label></p>" +
                "<p><label>Number: <input type='number' value='10' min='0' max='100'></label></p>" +
                "</fieldset>" +

                "<fieldset>" +
                "<legend>" + t("demo.choices") + "</legend>" +
                "<p><label><input type='checkbox' checked> Checkbox</label></p>" +
                "<p><label><input type='radio' name='demo-radio' checked> Radio button 1</label></p>" +
                "<p><label><input type='radio' name='demo-radio'> Radio button 2</label></p>" +
                "<p><label>Select: <select>" +
                "<option>First option</option>" +
                "<option>Second option</option>" +
                "<option>Third option</option>" +
                "</select></label></p>" +
                "<p><label>Multiple select: <select multiple size='3'>" +
                "<option selected>First option</option>" +
                "<option>Second option</option>" +
                "<option>Third option</option>" +
                "</select></label></p>" +
                "</fieldset>" +

                "<fieldset>" +
                "<legend>" + t("demo.other_inputs") + "</legend>" +
                "<p><label>Text area:<br><textarea rows='4' cols='35'>Example text area</textarea></label></p>" +
                "<p><label>File: <input type='file'></label></p>" +
                "<p><label>Date: <input type='date'></label></p>" +
                "<p><label>Time: <input type='time'></label></p>" +
                "<p><label>Month: <input type='month'></label></p>" +
                "<p><label>Week: <input type='week'></label></p>" +
                "<p><label>Date/time: <input type='datetime-local'></label></p>" +
                "<p><label>Color: <input type='color' value='#336699'></label></p>" +
                "<p><label>Range: <input type='range' min='0' max='100' value='50'></label></p>" +
                "</fieldset>" +

                "<p>" +
                "<button type='button' onclick='alertBox()'>Button</button> " +
                "<button type='submit'>Submit</button> " +
                "<button type='reset'>Reset</button>" +
                "</p>" +
                "</form>" +

                "<h2>" + t("demo.popups") + "</h2>" +
                "<p>" +
                "<button type='button' onclick='alertBox()'>Alert</button> " +
                "<button type='button' onclick='confirmBox()'>Confirm</button> " +
                "<button type='button' onclick='promptBox()'>Prompt</button>" +
                "</p>" +

                "<h2>" + t("demo.tables") + "</h2>" +
                "<table border='1'>" +
                "<caption>" + t("demo.table_label") + "</caption>" +
                "<tr><th>Header 1</th><th>Header 2</th><th>Header 3</th></tr>" +
                "<tr><td>Cell 1</td><td>Cell 2</td><td>Cell 3</td></tr>" +
                "<tr><td>Cell 4</td><td>Cell 5</td><td>Cell 6</td></tr>" +
                "</table>" +

                "<h2>" + t("demo.other") + "</h2>" +
                "<p><pre>Preformatted text\n    With spacing preserved</pre></p>" +
                "<p><code>console.log('Code');</code></p>" +
                "<blockquote>Blockquote example.</blockquote>" +
                "<p><img alt='Image' src='data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=='></p>" +
                "<progress value='65' max='100'>65%</progress>" +
                "<p><meter min='0' max='100' value='65'>65</meter></p>" +
                "<details><summary>Details</summary><p>Hidden details content.</p></details>" +

                "<script>" +
                "function alertBox(){alert('Alert');}" +
                "function confirmBox(){confirm('Confirm');}" +
                "function promptBox(){prompt('Prompt','Example text');}" +
                "function submitForm(){alert('Submit');return false;}" +
                "function resetForm(){}" +
                "</script>" +

                "</body></html>";

        return Localization.translateHtml(
                activity,
                html);
    }

}