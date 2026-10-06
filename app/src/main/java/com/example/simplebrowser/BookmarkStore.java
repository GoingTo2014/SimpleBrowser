package com.example.simplebrowser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Profile-scoped bookmark storage.
 */
public final class BookmarkStore
        extends SQLiteOpenHelper {

    private static final String DATABASE_NAME =
            "bookmarks.db";

    private static final int DATABASE_VERSION = 1;

    private static final String TABLE =
            "bookmarks";

    public static class Entry {
        public long id;
        public String url;
        public String title;
        public long time;
        public byte[] favicon;
    }

    public BookmarkStore(Context context) {
        this(
                context,
                ProfileManager.getActiveProfileId(
                        context));
    }

    public BookmarkStore(
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
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "url TEXT NOT NULL UNIQUE," +
                "title TEXT," +
                "time INTEGER NOT NULL," +
                "favicon BLOB" +
                ")");

        db.execSQL(
                "CREATE INDEX idx_bookmarks_title " +
                "ON " +
                TABLE +
                "(title COLLATE NOCASE)");
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {
    }

    public synchronized Entry addOrUpdate(
            String url,
            String title,
            android.graphics.Bitmap favicon) {

        if (url == null ||
                url.trim().isEmpty()) {
            return null;
        }

        String cleanUrl =
                url.trim();

        String cleanTitle =
                title == null ||
                        title.trim().isEmpty()
                        ? cleanUrl
                        : title.trim();

        ContentValues values =
                new ContentValues();

        values.put(
                "url",
                cleanUrl);

        values.put(
                "title",
                cleanTitle);

        values.put(
                "time",
                System.currentTimeMillis());

        byte[] encoded =
                encodeFavicon(favicon);

        if (encoded != null) {
            values.put(
                    "favicon",
                    encoded);
        }

        getWritableDatabase()
                .insertWithOnConflict(
                        TABLE,
                        null,
                        values,
                        SQLiteDatabase
                                .CONFLICT_REPLACE);

        return findByUrl(cleanUrl);
    }

    public synchronized Entry findByUrl(
            String url) {

        if (url == null) {
            return null;
        }

        Cursor cursor =
                getReadableDatabase()
                        .query(
                                TABLE,
                                new String[] {
                                        "id",
                                        "url",
                                        "title",
                                        "time",
                                        "favicon"
                                },
                                "url = ?",
                                new String[] {
                                        url.trim()
                                },
                                null,
                                null,
                                null);

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            return readEntry(cursor);
        } finally {
            cursor.close();
        }
    }

    public synchronized List<Entry> getEntries(
            String query) {

        ArrayList<Entry> result =
                new ArrayList<>();

        String selection = null;
        String[] args = null;

        if (query != null &&
                !query.trim().isEmpty()) {

            String like =
                    "%" +
                    query.trim() +
                    "%";

            selection =
                    "url LIKE ? OR title LIKE ?";

            args =
                    new String[] {
                            like,
                            like
                    };
        }

        Cursor cursor =
                getReadableDatabase()
                        .query(
                                TABLE,
                                new String[] {
                                        "id",
                                        "url",
                                        "title",
                                        "time",
                                        "favicon"
                                },
                                selection,
                                args,
                                null,
                                null,
                                "time DESC");

        try {
            while (cursor.moveToNext()) {
                result.add(
                        readEntry(cursor));
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public synchronized Entry get(
            long id) {

        Cursor cursor =
                getReadableDatabase()
                        .query(
                                TABLE,
                                new String[] {
                                        "id",
                                        "url",
                                        "title",
                                        "time",
                                        "favicon"
                                },
                                "id = ?",
                                new String[] {
                                        String.valueOf(id)
                                },
                                null,
                                null,
                                null);

        try {
            return cursor.moveToFirst()
                    ? readEntry(cursor)
                    : null;
        } finally {
            cursor.close();
        }
    }

    public synchronized void delete(
            long id) {

        getWritableDatabase()
                .delete(
                        TABLE,
                        "id = ?",
                        new String[] {
                                String.valueOf(id)
                        });
    }

    public synchronized void clear() {

        getWritableDatabase()
                .delete(
                        TABLE,
                        null,
                        null);
    }

    private Entry readEntry(
            Cursor cursor) {

        Entry entry =
                new Entry();

        entry.id =
                cursor.getLong(0);

        entry.url =
                cursor.getString(1);

        entry.title =
                cursor.getString(2);

        entry.time =
                cursor.getLong(3);

        entry.favicon =
                cursor.getBlob(4);

        return entry;
    }

    private byte[] encodeFavicon(
            android.graphics.Bitmap bitmap) {

        if (bitmap == null ||
                bitmap.isRecycled()) {
            return null;
        }

        try {
            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            bitmap.compress(
                    android.graphics.Bitmap.CompressFormat.PNG,
                    100,
                    output);

            return output.toByteArray();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String faviconDataUri(
            byte[] favicon) {

        return BrowserHistory.faviconDataUri(favicon);
    }
}
