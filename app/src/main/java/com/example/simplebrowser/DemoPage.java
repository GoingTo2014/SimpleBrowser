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
                t("demo.title");

        BrowserPage.load(
                tab.webView,
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

    private String js(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String createHtml() {

        String alert =
                js(t("demo.alert_message"));

        String confirm =
                js(t("demo.confirm_message"));

        String prompt =
                js(t("demo.prompt_message"));

        String submit =
                js(t("demo.submit_message"));

        return
                "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' " +
                "content='width=device-width,initial-scale=1'>" +
                "<title>" +
                t("demo.title") +
                "</title>" +
                "</head><body>" +

                "<h1>" + t("demo.title") + "</h1>" +
                "<p>" + t("demo.intro") + "</p>" +
                "<p>" + t("demo.description") + "</p>" +

                "<h2>" + t("demo.headings") + "</h2>" +
                "<h1>" + t("demo.heading1") + "</h1>" +
                "<h2>" + t("demo.heading2") + "</h2>" +
                "<h3>" + t("demo.heading3") + "</h3>" +
                "<h4>" + t("demo.heading4") + "</h4>" +
                "<h5>" + t("demo.heading5") + "</h5>" +
                "<h6>" + t("demo.heading6") + "</h6>" +
                "<p>" +
                t("demo.plain") + ", " +
                "<strong>" + t("demo.bold") + "</strong>, " +
                "<em>" + t("demo.italic") + "</em>, " +
                "<u>" + t("demo.underlined") + "</u>, " +
                "<mark>" + t("demo.marked") + "</mark>, " +
                "<small>" + t("demo.small") + "</small>, " +
                "<del>" + t("demo.deleted") + "</del>, " +
                "<ins>" + t("demo.inserted") + "</ins>, " +
                "H<sub>2</sub>O, x<sup>2</sup>.</p>" +
                "<p><a href='https://example.com/'>" +
                t("demo.link") +
                "</a></p>" +
                "<hr>" +

                "<h2>" + t("demo.lists") + "</h2>" +
                "<p>" + t("demo.unordered_list") + "</p>" +
                "<ul><li>" + t("demo.first_item") +
                "</li><li>" + t("demo.second_item") +
                "</li><li>" + t("demo.third_item") + "</li></ul>" +
                "<p>" + t("demo.ordered_list") + "</p>" +
                "<ol><li>" + t("demo.first_item") +
                "</li><li>" + t("demo.second_item") +
                "</li><li>" + t("demo.third_item") + "</li></ol>" +

                "<h2>" + t("demo.controls") + "</h2>" +
                "<form onsubmit='return submitForm();' " +
                "onreset='resetForm();'>" +

                "<fieldset>" +
                "<legend>" + t("demo.text_inputs") + "</legend>" +
                "<p><label>" + t("demo.text_box") +
                ": <input type='text' value='" +
                t("demo.example_text") + "'></label></p>" +
                "<p><label>" + t("demo.password") +
                ": <input type='password' value='" +
                t("demo.password_value") + "'></label></p>" +
                "<p><label>" + t("demo.email") +
                ": <input type='email' value='user@example.com'></label></p>" +
                "<p><label>" + t("demo.url") +
                ": <input type='url' value='https://example.com/'></label></p>" +
                "<p><label>" + t("demo.search") +
                ": <input type='search' value='" +
                t("demo.search_text") + "'></label></p>" +
                "<p><label>" + t("demo.number") +
                ": <input type='number' value='10' min='0' max='100'></label></p>" +
                "<p><label>Tel: <input type='tel' value='555-1234'></label></p>" +
                "</fieldset>" +

                "<fieldset>" +
                "<legend>" + t("demo.choices") + "</legend>" +
                "<p><label><input type='checkbox' checked> " +
                t("demo.checkbox") + "</label></p>" +
                "<p><label><input type='radio' name='demo-radio' checked> " +
                t("demo.radio1") + "</label></p>" +
                "<p><label><input type='radio' name='demo-radio'> " +
                t("demo.radio2") + "</label></p>" +
                "<p><label>" + t("demo.select") + ": <select>" +
                "<option>" + t("demo.first_item") + "</option>" +
                "<option>" + t("demo.second_item") + "</option>" +
                "<option>" + t("demo.third_item") + "</option>" +
                "</select></label></p>" +
                "<p><label>" + t("demo.multiple_select") +
                ": <select multiple size='3'>" +
                "<option selected>" + t("demo.first_item") + "</option>" +
                "<option>" + t("demo.second_item") + "</option>" +
                "<option>" + t("demo.third_item") + "</option>" +
                "</select></label></p>" +
                "</fieldset>" +

                "<fieldset>" +
                "<legend>" + t("demo.other_inputs") + "</legend>" +
                "<p><label>" + t("demo.text_area") +
                ":<br><textarea rows='4' cols='35'>" +
                t("demo.text_area_value") +
                "</textarea></label></p>" +
                "<p><label>" + t("demo.file") +
                ": <input type='file'></label></p>" +
                "<p><label>" + t("demo.date") +
                ": <input type='date'></label></p>" +
                "<p><label>" + t("demo.time") +
                ": <input type='time'></label></p>" +
                "<p><label>" + t("demo.month") +
                ": <input type='month'></label></p>" +
                "<p><label>" + t("demo.week") +
                ": <input type='week'></label></p>" +
                "<p><label>" + t("demo.datetime") +
                ": <input type='datetime-local'></label></p>" +
                "<p><label>" + t("demo.color") +
                ": <input type='color' value='#336699'></label></p>" +
                "<p><label>" + t("demo.range") +
                ": <input type='range' min='0' max='100' value='50'></label></p>" +
                "<p><input type='hidden' value='hidden'></p>" +
                "</fieldset>" +

                "<p>" +
                "<button type='button' onclick='alertBox()'>" +
                t("demo.button") + "</button> " +
                "<button type='submit'>" +
                t("demo.submit") + "</button> " +
                "<button type='reset'>" +
                t("demo.reset") + "</button>" +
                "</p>" +
                "</form>" +

                "<h2>" + t("demo.popups") + "</h2>" +
                "<p>" +
                "<button type='button' onclick='alertBox()'>" +
                t("demo.alert") + "</button> " +
                "<button type='button' onclick='confirmBox()'>" +
                t("demo.confirm") + "</button> " +
                "<button type='button' onclick='promptBox()'>" +
                t("demo.prompt") + "</button>" +
                "</p>" +

                "<h2>" + t("demo.tables") + "</h2>" +
                "<table>" +
                "<caption>" + t("demo.table_label") + "</caption>" +
                "<tr><th>" + t("demo.header1") +
                "</th><th>" + t("demo.header2") +
                "</th><th>" + t("demo.header3") + "</th></tr>" +
                "<tr><td>" + t("demo.cell1") +
                "</td><td>" + t("demo.cell2") +
                "</td><td>" + t("demo.cell3") + "</td></tr>" +
                "<tr><td>" + t("demo.cell4") +
                "</td><td>" + t("demo.cell5") +
                "</td><td>" + t("demo.cell6") + "</td></tr>" +
                "</table>" +

                "<h2>" + t("demo.other") + "</h2>" +
                "<p><pre>" +
                t("demo.preformatted") +
                "\n    " +
                t("demo.spacing") +
                "</pre></p>" +
                "<p><code>console.log('Code');</code></p>" +
                "<blockquote>" +
                t("demo.blockquote") +
                "</blockquote>" +
                "<p><img alt='" +
                t("demo.image") +
                "' src='data:image/gif;base64," +
                "R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=='></p>" +

                "<p><audio controls></audio></p>" +
                "<p><video controls width='320' height='180'></video></p>" +
                "<p><canvas id='demo-canvas' width='300' height='100'></canvas></p>" +
                "<p><iframe src='about:blank' " +
                "title='" + t("demo.iframe") + "'></iframe></p>" +
                "<p><output>" + t("demo.form_output") +
                "</output></p>" +
                "<progress value='65' max='100'>65%</progress>" +
                "<p><meter min='0' max='100' value='65'>65</meter></p>" +
                "<details><summary>" + t("demo.details") +
                "</summary><p>" + t("demo.hidden") +
                "</p></details>" +
                "<p><dialog open>" + t("demo.dialog") +
                ": " + t("demo.dialog_text") +
                "</dialog></p>" +

                "<script>" +
                "function alertBox(){alert('" + alert + "');}" +
                "function confirmBox(){confirm('" + confirm + "');}" +
                "function promptBox(){prompt('" + prompt +
                "','" + js(t("demo.example_text")) + "');}" +
                "function submitForm(){alert('" + submit +
                "');return false;}" +
                "function resetForm(){}" +
                "</script>" +

                "</body></html>";
    }
}
