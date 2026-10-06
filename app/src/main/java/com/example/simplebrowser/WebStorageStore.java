package com.example.simplebrowser;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Profile-scoped shadow storage for Web Storage's localStorage API.
 *
 * Android 4.4 WebView keeps its DOM storage in the process-global WebView
 * data area. This store preserves each profile's localStorage separately so
 * the browser can clear the shared WebView storage on profile switches and
 * restore the selected profile afterwards.
 *
 * A small mirrored copy is kept in the profile-scoped browser settings
 * preferences as well as SQLite. This is intentional: a profile switch can
 * close/reopen WebView and SQLite objects while Android's WebStorage is being
 * cleared, so the snapshot must have a second persistent path that cannot be
 * affected by WebView's global storage cleanup.
 */
public final class WebStorageStore
        extends SQLiteOpenHelper {

    private static final String DATABASE_NAME =
            "web_storage.db";

    private static final int DATABASE_VERSION = 1;

    private static final String TABLE =
            "local_storage";

    private static final String BACKUP_PREFIX =
            "__simplebrowser_webstorage__";

    private final SharedPreferences backupPreferences;

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

        backupPreferences =
                context.getSharedPreferences(
                        ProfileManager.scopedPrefsName(
                                "browser_settings",
                                profileId),
                        Context.MODE_PRIVATE);
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

    private String backupKey(String origin) {
        return BACKUP_PREFIX + origin;
    }

    private String readBackup(String origin) {
        try {
            return backupPreferences.getString(
                    backupKey(origin),
                    null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void writeBackup(
            String origin,
            String data) {
        try {
            backupPreferences.edit()
                    .putString(
                            backupKey(origin),
                            data)
                    .apply();
        } catch (Throwable ignored) {
        }
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

        try {
            getWritableDatabase()
                    .insertWithOnConflict(
                            TABLE,
                            null,
                            values,
                            SQLiteDatabase
                                    .CONFLICT_REPLACE);
        } catch (Throwable ignored) {
            // The preferences mirror below is the recovery path.
        }

        writeBackup(
                cleanOrigin,
                value);
    }

    public synchronized String get(
            String origin) {

        String cleanOrigin =
                normalizeOrigin(origin);

        if (cleanOrigin == null) {
            return "{}";
        }

        try {
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
                if (cursor.moveToFirst()) {
                    String data =
                            cursor.getString(0);

                    if (data != null &&
                            !data.trim().isEmpty()) {
                        return data;
                    }
                }
            } finally {
                cursor.close();
            }
        } catch (Throwable ignored) {
        }

        String backup =
                readBackup(cleanOrigin);

        return backup == null ||
                backup.trim().isEmpty()
                ? "{}"
                : backup;
    }

    public synchronized boolean has(
            String origin) {

        String cleanOrigin =
                normalizeOrigin(origin);

        if (cleanOrigin == null) {
            return false;
        }

        try {
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
                if (cursor.moveToFirst()) {
                    return true;
                }
            } finally {
                cursor.close();
            }
        } catch (Throwable ignored) {
        }

        return readBackup(cleanOrigin) != null;
    }

    public synchronized List<String>
            getOrigins() {

        Set<String> origins =
                new HashSet<>();

        try {
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
                    origins.add(cursor.getString(0));
                }
            } finally {
                cursor.close();
            }
        } catch (Throwable ignored) {
        }

        try {
            Map<String, ?> values =
                    backupPreferences.getAll();

            for (String key : values.keySet()) {
                if (key.startsWith(BACKUP_PREFIX)) {
                    String origin =
                            key.substring(
                                    BACKUP_PREFIX.length());

                    if (normalizeOrigin(origin) != null) {
                        origins.add(origin);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        ArrayList<String> result =
                new ArrayList<>(origins);

        java.util.Collections.sort(
                result,
                String.CASE_INSENSITIVE_ORDER);

        return result;
    }

    public synchronized void deleteOrigin(
            String origin) {

        String cleanOrigin =
                normalizeOrigin(origin);

        if (cleanOrigin == null) {
            return;
        }

        try {
            getWritableDatabase()
                    .delete(
                            TABLE,
                            "origin = ?",
                            new String[] {
                                    cleanOrigin
                            });
        } catch (Throwable ignored) {
        }

        try {
            backupPreferences.edit()
                    .remove(
                            backupKey(cleanOrigin))
                    .apply();
        } catch (Throwable ignored) {
        }
    }

    public synchronized void clear() {

        try {
            getWritableDatabase()
                    .delete(
                            TABLE,
                            null,
                            null);
        } catch (Throwable ignored) {
        }

        try {
            SharedPreferences.Editor editor =
                    backupPreferences.edit();

            for (String key :
                    backupPreferences.getAll().keySet()) {
                if (key.startsWith(BACKUP_PREFIX)) {
                    editor.remove(key);
                }
            }

            editor.apply();
        } catch (Throwable ignored) {
        }
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
