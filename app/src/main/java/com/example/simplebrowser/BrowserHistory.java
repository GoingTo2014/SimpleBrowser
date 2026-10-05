package com.example.simplebrowser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent browser history for normal browsing.
 * Incognito tabs never write to this database.
 */
public class BrowserHistory
        extends SQLiteOpenHelper {

    private static final String DATABASE_NAME =
            "browser_history.db";

    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_VISITS =
            "visits";

    public static class Entry {
        public long id;
        public String url;
        public String title;
        public long time;
        public byte[] favicon;
    }

    public BrowserHistory(Context context) {
        super(
                context,
                DATABASE_NAME,
                null,
                DATABASE_VERSION);
    }

    @Override
    public void onCreate(
            SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE " +
                TABLE_VISITS +
                " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "url TEXT NOT NULL," +
                "title TEXT," +
                "time INTEGER NOT NULL," +
                "favicon BLOB" +
                ")");

        db.execSQL(
                "CREATE INDEX idx_visits_time " +
                "ON " +
                TABLE_VISITS +
                "(time DESC)");
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        if (oldVersion < 2) {
            db.execSQL(
                    "ALTER TABLE " +
                    TABLE_VISITS +
                    " ADD COLUMN favicon BLOB");
        }
    }

    public synchronized void addVisit(
            String url,
            String title) {

        addVisit(
                url,
                title,
                null);
    }

    public synchronized void addVisit(
            String url,
            String title,
            Bitmap faviconBitmap) {

        if (url == null ||
                url.trim().isEmpty()) {
            return;
        }

        String lower =
                url.trim()
                        .toLowerCase();

        boolean supported =
                lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.startsWith("file://") ||
                lower.startsWith("content://") ||
                lower.startsWith("browser://");

        if (!supported) {
            return;
        }

        ContentValues values =
                new ContentValues();

        values.put(
                "url",
                url.trim());

        values.put(
                "title",
                title == null
                        ? ""
                        : title.trim());

        values.put(
                "time",
                System.currentTimeMillis());

        byte[] favicon =
                encodeFavicon(faviconBitmap);

        if (favicon != null) {
            values.put(
                    "favicon",
                    favicon);
        }

        SQLiteDatabase db =
                getWritableDatabase();

        db.insert(
                TABLE_VISITS,
                null,
                values);
    }

    private byte[] encodeFavicon(
            Bitmap bitmap) {

        if (bitmap == null ||
                bitmap.isRecycled()) {
            return null;
        }

        try {

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    output);

            return output.toByteArray();

        } catch (Exception ignored) {

            return null;
        }
    }

    public static String faviconDataUri(
            byte[] favicon) {

        if (favicon == null ||
                favicon.length == 0) {
            return "";
        }

        try {
            return "data:image/png;base64," +
                    Base64.encodeToString(
                            favicon,
                            Base64.NO_WRAP);
        } catch (Exception ignored) {
            return "";
        }
    }

    public synchronized List<Entry> getEntries(
            String query) {

        return getEntries(
                query,
                Integer.MAX_VALUE,
                0);
    }

    public synchronized List<Entry> getEntries(
            String query,
            int limit,
            int offset) {

        ArrayList<Entry> entries =
                new ArrayList<>();

        if (limit <= 0) {
            return entries;
        }

        SQLiteDatabase db =
                getReadableDatabase();

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

        String limitClause =
                offset +
                "," +
                limit;

        Cursor cursor =
                db.query(
                        TABLE_VISITS,
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
                        "time DESC",
                        limitClause);

        try {

            while (cursor.moveToNext()) {

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

                entries.add(entry);
            }

        } finally {

            cursor.close();
        }

        return entries;
    }

    public synchronized Entry get(
            long id) {

        SQLiteDatabase db =
                getReadableDatabase();

        Cursor cursor =
                db.query(
                        TABLE_VISITS,
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

            if (!cursor.moveToFirst()) {
                return null;
            }

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

        } finally {

            cursor.close();
        }
    }

    public synchronized void delete(
            long id) {

        getWritableDatabase()
                .delete(
                        TABLE_VISITS,
                        "id = ?",
                        new String[] {
                                String.valueOf(id)
                        });
    }

    public synchronized void updateLatestFavicon(
            String url,
            String title,
            Bitmap faviconBitmap) {

        if (url == null ||
                url.trim().isEmpty() ||
                faviconBitmap == null ||
                faviconBitmap.isRecycled()) {
            return;
        }

        ContentValues values =
                new ContentValues();

        byte[] encoded =
                encodeFavicon(faviconBitmap);

        if (encoded != null) {
            values.put(
                    "favicon",
                    encoded);
        }

        if (title != null) {
            values.put(
                    "title",
                    title.trim());
        }

        getWritableDatabase().update(
                TABLE_VISITS,
                values,
                "id = (SELECT id FROM " +
                TABLE_VISITS +
                " WHERE url = ? " +
                "ORDER BY time DESC LIMIT 1)",
                new String[] { url.trim() });
    }

    public synchronized void clear() {

        getWritableDatabase()
                .delete(
                        TABLE_VISITS,
                        null,
                        null);
    }
}
