package com.example.simplebrowser;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

public class DownloadHistory
        extends SQLiteOpenHelper {

    private static final String DATABASE_NAME =
            "download_history.db";

    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_DOWNLOADS =
            "downloads";

    public static class Entry {
        public long id;
        public long downloadId;
        public String url;
        public String filename;
        public String mimeType;
        public long time;
    }

    public DownloadHistory(Context context) {
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
                TABLE_DOWNLOADS +
                " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "download_id INTEGER NOT NULL," +
                "url TEXT NOT NULL," +
                "filename TEXT," +
                "mime_type TEXT," +
                "time INTEGER NOT NULL" +
                ")");

        db.execSQL(
                "CREATE INDEX idx_downloads_time " +
                "ON " +
                TABLE_DOWNLOADS +
                "(time DESC)");
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {
    }

    public synchronized void add(
            long downloadId,
            String url,
            String filename,
            String mimeType,
            long time) {

        if (url == null ||
                url.trim().isEmpty()) {
            return;
        }

        ContentValues values =
                new ContentValues();

        values.put(
                "download_id",
                downloadId);

        values.put(
                "url",
                url.trim());

        values.put(
                "filename",
                filename == null
                        ? ""
                        : filename.trim());

        values.put(
                "mime_type",
                mimeType == null
                        ? ""
                        : mimeType.trim());

        values.put(
                "time",
                time);

        getWritableDatabase()
                .insert(
                        TABLE_DOWNLOADS,
                        null,
                        values);
    }

    public synchronized List<Entry> getEntries(
            String query) {

        ArrayList<Entry> entries =
                new ArrayList<>();

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
                    "url LIKE ? OR filename LIKE ?";

            args =
                    new String[] {
                            like,
                            like
                    };
        }

        Cursor cursor =
                db.query(
                        TABLE_DOWNLOADS,
                        new String[] {
                                "id",
                                "download_id",
                                "url",
                                "filename",
                                "mime_type",
                                "time"
                        },
                        selection,
                        args,
                        null,
                        null,
                        "time DESC");

        try {

            while (cursor.moveToNext()) {

                Entry entry =
                        new Entry();

                entry.id =
                        cursor.getLong(0);

                entry.downloadId =
                        cursor.getLong(1);

                entry.url =
                        cursor.getString(2);

                entry.filename =
                        cursor.getString(3);

                entry.mimeType =
                        cursor.getString(4);

                entry.time =
                        cursor.getLong(5);

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
                        TABLE_DOWNLOADS,
                        new String[] {
                                "id",
                                "download_id",
                                "url",
                                "filename",
                                "mime_type",
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

            entry.id = cursor.getLong(0);
            entry.downloadId = cursor.getLong(1);
            entry.url = cursor.getString(2);
            entry.filename = cursor.getString(3);
            entry.mimeType = cursor.getString(4);
            entry.time = cursor.getLong(5);

            return entry;

        } finally {

            cursor.close();
        }
    }

    public synchronized void delete(
            long id) {

        getWritableDatabase()
                .delete(
                        TABLE_DOWNLOADS,
                        "id = ?",
                        new String[] {
                                String.valueOf(id)
                        });
    }

    public synchronized void clear() {

        getWritableDatabase()
                .delete(
                        TABLE_DOWNLOADS,
                        null,
                        null);
    }
}
