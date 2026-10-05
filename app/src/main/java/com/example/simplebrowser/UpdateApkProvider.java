package com.example.simplebrowser;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

public class UpdateApkProvider extends ContentProvider {

    public static final String AUTHORITY =
            "com.example.simplebrowser.update";

    private static final String FILE_NAME =
            "simplebrowser-update.apk";

    public static Uri getUri() {
        return Uri.parse(
                "content://" +
                AUTHORITY +
                "/update.apk");
    }

    private File getUpdateFile() {
        Context context = getContext();

        if (context == null) {
            return null;
        }

        return new File(
                context.getCacheDir(),
                FILE_NAME);
    }

    private boolean isValidUri(Uri uri) {
        return uri != null &&
                "/update.apk".equals(
                        uri.getPath());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return isValidUri(uri)
                ? "application/vnd.android.package-archive"
                : null;
    }

    @Override
    public ParcelFileDescriptor openFile(
            Uri uri,
            String mode)
            throws FileNotFoundException {

        if (!isValidUri(uri)) {
            throw new FileNotFoundException(
                    "Unknown update URI");
        }

        if (!"r".equals(mode)) {
            throw new FileNotFoundException(
                    "Update APK is read-only");
        }

        File file = getUpdateFile();

        if (file == null ||
                !file.isFile()) {
            throw new FileNotFoundException(
                    "Update APK not found");
        }

        return ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        if (!isValidUri(uri)) {
            return null;
        }

        File file = getUpdateFile();

        if (file == null ||
                !file.isFile()) {
            return null;
        }

        MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                                OpenableColumns.DISPLAY_NAME,
                                OpenableColumns.SIZE
                        });

        cursor.addRow(
                new Object[] {
                        FILE_NAME,
                        file.length()
                });

        return cursor;
    }

    @Override
    public Uri insert(
            Uri uri,
            ContentValues values) {
        return null;
    }

    @Override
    public int delete(
            Uri uri,
            String selection,
            String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        return 0;
    }
}
