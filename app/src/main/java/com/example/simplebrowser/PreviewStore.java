package com.example.simplebrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

public final class PreviewStore {

    private static final String PREFS =
            "tab_previews";

    private final SharedPreferences preferences;

    public PreviewStore(Context context) {
        preferences =
                context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE);
    }

    public void save(
            String key,
            Bitmap bitmap) {

        if (key == null ||
                key.trim().isEmpty() ||
                bitmap == null) {
            return;
        }

        try {
            Bitmap scaled =
                    Bitmap.createScaledBitmap(
                            bitmap,
                            480,
                            270,
                            true);

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            scaled.compress(
                    Bitmap.CompressFormat.JPEG,
                    85,
                    output);

            scaled.recycle();

            String encoded =
                    android.util.Base64.encodeToString(
                            output.toByteArray(),
                            android.util.Base64.NO_WRAP);

            preferences.edit()
                    .putString(
                            "preview_" + key,
                            encoded)
                    .putString(
                            "known_" + key,
                            "1")
                    .apply();
        } catch (Throwable ignored) {
        }
    }

    public Bitmap load(
            String key) {

        if (key == null ||
                key.trim().isEmpty()) {
            return null;
        }

        String encoded =
                preferences.getString(
                        "preview_" + key,
                        "");

        if (encoded == null ||
                encoded.isEmpty()) {
            return null;
        }

        try {
            byte[] bytes =
                    android.util.Base64.decode(
                            encoded,
                            android.util.Base64.DEFAULT);

            return BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.length);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public void remove(String key) {

        if (key == null) {
            return;
        }

        preferences.edit()
                .remove("preview_" + key)
                .remove("known_" + key)
                .apply();
    }
}
