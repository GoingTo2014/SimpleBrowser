package com.example.simplebrowser;

import android.content.Context;

import com.example.simplebrowser.languages.Chinese;
import com.example.simplebrowser.languages.English;
import com.example.simplebrowser.languages.Hindi;
import com.example.simplebrowser.languages.Japanese;
import com.example.simplebrowser.languages.LanguagePack;
import com.example.simplebrowser.languages.French;
import com.example.simplebrowser.languages.Portuguese;
import com.example.simplebrowser.languages.Spanish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Localization {

    public static final String SYSTEM = "system";
    public static final String ENGLISH = "en";
    public static final String SPANISH = "es";
    public static final String PORTUGUESE = "pt";
    public static final String FRENCH = "fr";
    public static final String JAPANESE = "ja";
    public static final String CHINESE = "zh";
    public static final String HINDI = "hi";

    private static final Map<String, LanguagePack> PACKS =
            new LinkedHashMap<>();

    private static final Map<String, String> ENGLISH_KEYS =
            new LinkedHashMap<>();

    static {
        register(new English());
        register(new Spanish());
        register(new Portuguese());
        register(new French());
        register(new Japanese());
        register(new Chinese());
        register(new Hindi());

        LanguagePack english =
                PACKS.get(ENGLISH);

        if (english != null) {

            List<String> keys =
                    new ArrayList<>(
                            english.keys());

            for (String key : keys) {

                String value =
                        english.get(key);

                if (value != null &&
                        !ENGLISH_KEYS.containsKey(value)) {
                    ENGLISH_KEYS.put(
                            value,
                            key);
                }
            }
        }
    }

    private Localization() {
    }

    private static void register(
            LanguagePack pack) {

        PACKS.put(
                pack.getId(),
                pack);
    }

    public static String resolve(
            Context context,
            String requested) {

        String choice =
                requested == null ||
                requested.trim().isEmpty()
                        ? SYSTEM
                        : requested;

        if (!SYSTEM.equals(choice)) {

            return PACKS.containsKey(choice)
                    ? choice
                    : ENGLISH;
        }

        String system =
                Locale.getDefault()
                        .getLanguage()
                        .toLowerCase(
                                Locale.US);

        if (PACKS.containsKey(system)) {
            return system;
        }

        return ENGLISH;
    }

    public static String translate(
            Context context,
            String keyOrEnglish) {

        String key =
                toKey(keyOrEnglish);

        String language =
                resolve(
                        context,
                        getRequestedLanguage(
                                context));

        LanguagePack pack =
                PACKS.get(language);

        if (pack == null) {
            pack =
                    PACKS.get(ENGLISH);
        }

        if (pack != null) {

            String value =
                    pack.get(key);

            if (value != null &&
                    !value.isEmpty()) {
                return value;
            }
        }

        LanguagePack english =
                PACKS.get(ENGLISH);

        if (english != null) {

            String value =
                    english.get(key);

            if (value != null &&
                    !value.isEmpty()) {
                return value;
            }
        }

        return key;
    }

    public static String translateHtml(
            Context context,
            String html) {

        if (html == null ||
                html.isEmpty()) {
            return html;
        }

        String language =
                resolve(
                        context,
                        getRequestedLanguage(
                                context));

        LanguagePack pack =
                PACKS.get(language);

        LanguagePack english =
                PACKS.get(ENGLISH);

        if (pack == null ||
                english == null) {
            return html;
        }

        List<String> keys =
                new ArrayList<>(
                        english.keys());

        Collections.sort(
                keys,
                new Comparator<String>() {
                    @Override
                    public int compare(
                            String first,
                            String second) {
                        return Integer.compare(
                                english.get(second).length(),
                                english.get(first).length());
                    }
                });

        String result = html;

        for (String key : keys) {

            String source =
                    english.get(key);

            if (source == null ||
                    source.isEmpty()) {
                continue;
            }

            String translated =
                    pack.get(key);

            if (translated == null ||
                    translated.isEmpty()) {
                translated =
                        english.get(key);
            }

            if (translated == null ||
                    translated.isEmpty()) {
                translated = key;
            }

            result =
                    result.replaceAll(
                            "(?<![A-Za-z0-9_])" +
                            Pattern.quote(source) +
                            "(?![A-Za-z0-9_])",
                            Matcher.quoteReplacement(
                                    translated));
        }

        return result;
    }

    public static boolean hasLanguage(
            String id) {

        return PACKS.containsKey(id);
    }

    public static String getLanguageName(
            Context context,
            String id) {

        String key;

        if (ENGLISH.equals(id)) {
            key = "settings.english";
        } else if (SPANISH.equals(id)) {
            key = "settings.spanish";
        } else if (PORTUGUESE.equals(id)) {
            key = "settings.portuguese";
        } else if (FRENCH.equals(id)) {
            key = "settings.french";
        } else if (JAPANESE.equals(id)) {
            key = "settings.japanese";
        } else if (CHINESE.equals(id)) {
            key = "settings.chinese";
        } else if (HINDI.equals(id)) {
            key = "settings.hindi";
        } else {
            key = "settings.system_default";
        }

        return translate(
                context,
                key);
    }

    private static String getRequestedLanguage(
            Context context) {

        if (context == null) {
            return SYSTEM;
        }

        return new BrowserSettings(context)
                .getLanguage();
    }

    private static String toKey(
            String keyOrEnglish) {

        if (keyOrEnglish == null ||
                keyOrEnglish.isEmpty()) {
            return "strings.untranslated";
        }

        LanguagePack english =
                PACKS.get(ENGLISH);

        if (english != null &&
                english.get(keyOrEnglish) != null) {
            return keyOrEnglish;
        }

        if (keyOrEnglish.startsWith(
                "strings.")) {
            return keyOrEnglish;
        }

        String key =
                ENGLISH_KEYS.get(
                        keyOrEnglish);

        if (key != null) {
            return key;
        }

        return "strings.untranslated." +
                slug(keyOrEnglish);
    }

    private static String slug(
            String value) {

        StringBuilder result =
                new StringBuilder();

        boolean separator = false;

        for (int i = 0;
                i < value.length();
                i++) {

            char character =
                    Character.toLowerCase(
                            value.charAt(i));

            if ((character >= 'a' &&
                    character <= 'z') ||
                    (character >= '0' &&
                    character <= '9')) {

                result.append(character);
                separator = false;

            } else if (!separator) {

                result.append('_');
                separator = true;
            }

            if (result.length() >= 48) {
                break;
            }
        }

        while (result.length() > 0 &&
                result.charAt(
                        result.length() - 1) == '_') {

            result.deleteCharAt(
                    result.length() - 1);
        }

        if (result.length() == 0) {
            return "value";
        }

        return result.toString();
    }
}
