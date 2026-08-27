package io.github.ethan2258.motootaidentity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ProfileAccessReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ProfileAccess.grantToTarget(context);
    }
}
