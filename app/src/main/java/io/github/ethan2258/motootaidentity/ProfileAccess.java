package io.github.ethan2258.motootaidentity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class ProfileAccess {
    private ProfileAccess() {
    }

    public static void grantToTarget(Context context) {
        context.grantUriPermission(
                ProfileContract.TARGET_PACKAGE,
                Uri.parse(ProfileContract.PROFILE_URI),
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
}
