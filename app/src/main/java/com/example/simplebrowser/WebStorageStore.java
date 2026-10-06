package com.example.simplebrowser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Profile-scoped shadow storage for Web Storage's localStorage API.
 *
 * Android 4.4 WebView keeps its DOM storage in the process-global WebView
 * data area. This store preserves each profile's localStorage separately so
 * the browser can clear the shared WebView storage on profile switches and
 * restore the selected profile afterwards.
 */
public final class WebStorageStore
        extends SQLiteOpenHelper {

    private static final String DATABASE_NAME =
            "web_storage.db";

    private static final int DATABASE_VERSION = 1;

    private static final String TABLE =
            "local_storage";

    public WebStorageStore(Context context) {
        this(
                context,
                ProfileManager.getActiveProfileId(
                        context));
    }

    public WebStorageStore(
            Context context,
            String profileId) {

        super(
                context,
                ProfileManager.scopedDatabaseName(
                        DATABASE_NAME,
                        profileId),
                null,
                DATABASE_VERSION);
    }

    @Override
    public void onCreate(
            SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE " +
                TABLE +
                " (" +
                "origin TEXT PRIMARY KEY," +
                "data TEXT NOT NULL," +
                "updated INTEGER NOT NULL" +
                ")");

        db.execSQL(
                "CREATE INDEX idx_local_storage_updated " +
                "ON " +
                TABLE +
                "(updated)");
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {
    }

    public synchronized void put(
            String origin,
            String data) {

        String cleanOrigin =
                normalizeOrigin(origin);

        if (cleanOrigin == null) {
            return;
        }

        String value =
                data == null ||
                data.trim().isEmpty()
                        ? "{}"
                        : data;

        ContentValues values =
                new ContentValues();

        values.put(
                "origin",
                cleanOrigin);

        values.put(
                "data",
                value);

        values.put(
                "updated",
                System.currentTimeMillis());

        getWritableDatabase()
                .insertWithOnConflict(
                        TABLE,
                        null,
                        values,
                        SQLiteDatabase
                                .CONFLICT_REPLACE);
    }

    public synchronized String get(
            String origin) {

        String cleanOrigin =
                normalizeOrigin(origin);

        if (cleanOrigin == null) {
            return "{}";
        }

        Cursor cursor =
                getReadableDatabase()
                        .query(
                                TABLE,
                                new String[] {
                                        "data"
                                },
                                "origin = ?",
                                new String[] {
                                        cleanOrigin
                                },
                                null,
                                null,
                                null);

        try {
            if (!cursor.moveToFirst()) {
                return "{}";
            }

            String data =
                    cursor.getString(0);

            return data == null ||
                    data.trim().isEmpty()
                    ? "{}"
                    : data;

        } finally {
            cursor.close();
        }
    }

    public synchronized boolean has(
            String origin) {

        String cleanOrigin =
                normalizeOrigin(origin);

        if (cleanOrigin == null) {
            return false;
        }

        Cursor cursor =
                getReadableDatabase()
                        .query(
                                TABLE,
                                new String[] {
                                        "origin"
                                },
                                "origin = ?",
                                new String[] {
                                        cleanOrigin
                                },
                                null,
                                null,
                                null);

        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    public synchronized List<String>
            getOrigins() {

        ArrayList<String> result =
                new ArrayList<>();

        Cursor cursor =
                getReadableDatabase()
                        .query(
                                TABLE,
                                new String[] {
                                        "origin"
                                },
                                null,
                                null,
                                null,
                                null,
                                "origin COLLATE NOCASE ASC");

        try {
            while (cursor.moveToNext()) {
                result.add(
                        cursor.getString(0));
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public synchronized void deleteOrigin(
            String origin) {

        String cleanOrigin =
                normalizeOrigin(origin);

        if (cleanOrigin == null) {
            return;
        }

        getWritableDatabase()
                .delete(
                        TABLE,
                        "origin = ?",
                        new String[] {
                                cleanOrigin
                        });
    }

    public synchronized void clear() {

        getWritableDatabase()
                .delete(
                        TABLE,
                        null,
                        null);
    }

    public static String normalizeOrigin(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {
            return null;
        }

        try {

            URI uri =
                    new URI(value.trim());

            String scheme =
                    uri.getScheme();

            String host =
                    uri.getHost();

            if (host == null ||
                    !("http".equalsIgnoreCase(
                            scheme) ||
                     "https".equalsIgnoreCase(
                            scheme))) {
                return null;
            }

            StringBuilder result =
                    new StringBuilder();

            result.append(
                    scheme.toLowerCase(
                            Locale.US));

            result.append("://");
            result.append(
                    host.toLowerCase(
                            Locale.US));

            int port =
                    uri.getPort();

            if (port >= 0) {
                result.append(":");
                result.append(port);
            }

            return result.toString();

        } catch (Exception ignored) {
            return null;
        }
    }
}
