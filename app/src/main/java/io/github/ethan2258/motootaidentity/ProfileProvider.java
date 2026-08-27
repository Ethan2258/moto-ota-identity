package io.github.ethan2258.motootaidentity;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;

public final class ProfileProvider extends ContentProvider {
    private static final String[] COLUMNS = {"enabled", "profile_json", "channel_alias"};

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        enforceCaller();
        if (!"active".equals(uri.getLastPathSegment())) {
            throw new IllegalArgumentException("Unknown profile URI");
        }
        SharedPreferences prefs = getContext().getSharedPreferences(
                ProfileContract.PREFS, 0);
        MatrixCursor cursor = new MatrixCursor(COLUMNS, 1);
        cursor.addRow(new Object[]{
                prefs.getBoolean(ProfileContract.PREF_ENABLED, false) ? 1 : 0,
                prefs.getString(ProfileContract.PREF_JSON, "{}"),
                prefs.getString(ProfileContract.PREF_CHANNEL_ALIAS, OtaChannelAlias.DEFAULT)
        });
        return cursor;
    }

    private void enforceCaller() {
        int uid = Binder.getCallingUid();
        if (uid == Process.myUid()) {
            return;
        }
        String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
        if (packages != null) {
            for (String packageName : packages) {
                if (ProfileContract.TARGET_PACKAGE.equals(packageName)) {
                    return;
                }
            }
        }
        throw new SecurityException("Caller is not Motorola OTA");
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd." + ProfileContract.AUTHORITY + ".profile";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("Read only");
    }
}
