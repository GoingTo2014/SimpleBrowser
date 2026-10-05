package com.example.simplebrowser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_VISITS =
            "visits";

    public static class Entry {
        public long id;
        public String url;
        public String title;
        public long time;
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
                "time INTEGER NOT NULL" +
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
    }

    public synchronized void addVisit(
            String url,
            String title) {

        if (url == null ||
                url.trim().isEmpty()) {
            return;
        }

        String lower =
                url.trim()
                        .toLowerCase();

        if (!lower.startsWith("http://") &&
                !lower.startsWith("https://") &&
                !lower.startsWith("file://") &&
                !lower.startsWith("content://")) {
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

        SQLiteDatabase db =
                getWritableDatabase();

        db.insert(
                TABLE_VISITS,
                null,
                values);
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
                                "time"
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
                                "time"
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

    public synchronized void clear() {

        getWritableDatabase()
                .delete(
                        TABLE_VISITS,
                        null,
                        null);
    }
}
