package com.fxzs.lingxiagent.util;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import timber.log.Timber;

public class GlobalProvider extends ContentProvider {

    // 固定
    public static final String AUTHORITY = "com.fxzs.lingxiagent.globalprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/config");

    // 👇 增加 UriMatcher
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int CONFIG = 1;

    static {
        sUriMatcher.addURI(AUTHORITY, "config", CONFIG); // 这里是关键 PATH
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // 👇 先匹配 PATH，不匹配直接返回空
        if (sUriMatcher.match(uri) != CONFIG) {
            return new MatrixCursor(new String[]{"value"});
        }

        // 下面你原来的代码完全不用动！
        Context context = getContext();
        if (context == null) {
            return new MatrixCursor(new String[]{"value"});
        }

        SharedPreferences sp = context.getSharedPreferences(WakeUpPermissionHelper.PREF_NAME,Context.MODE_MULTI_PROCESS);
        String key = uri.getQueryParameter("key");

        if (key == null || (!key.equals(WakeUpPermissionHelper.KEY_WAKEUP_POWER_ENABLED)
                && !key.equals(WakeUpPermissionHelper.KEY_WAKEUP_KEYBORD_ENABLED)
                && !key.equals(WakeUpPermissionHelper.KEY_WAKEUP_ENABLED))) {
            return new MatrixCursor(new String[]{"value"});
        }

        boolean value = sp.getBoolean(key, false);
        MatrixCursor cursor = new MatrixCursor(new String[]{"value"});
        cursor.addRow(new Object[]{value ? 1 : 0});
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
}
