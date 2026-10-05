package com.example.simplebrowser.languages;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class LanguagePack {

    private final String id;
    protected final Map<String, String> strings =
            new LinkedHashMap<>();

    protected LanguagePack(String id) {
        this.id = id;
    }

    protected void put(
            String key,
            String value) {
        strings.put(key, value);
    }

    public String getId() {
        return id;
    }

    public String get(String key) {
        return strings.get(key);
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(
                strings.keySet());
    }
}
