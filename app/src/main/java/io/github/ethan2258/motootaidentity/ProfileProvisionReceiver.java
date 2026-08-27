package io.github.ethan2258.motootaidentity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class ProfileProvisionReceiver extends BroadcastReceiver {
    public static final String ACTION_PROVISION =
            "io.github.ethan2258.motootaidentity.action.PROVISION_PROFILE";
    private static final String EXTRA_PROFILE_BASE64 = "profile_base64";
    private static final String EXTRA_ENABLED = "enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_PROVISION.equals(intent.getAction())) {
            return;
        }
        try {
            String encoded = intent.getStringExtra(EXTRA_PROFILE_BASE64);
            if (encoded == null || encoded.length() > 32_768) {
                throw new IllegalArgumentException("Missing or oversized profile_base64");
            }
            String jsonText = new String(
                    Base64.decode(encoded, Base64.NO_WRAP),
                    StandardCharsets.UTF_8);
            JSONObject profile = new JSONObject(jsonText);
            String error = ProfileValidator.validate(profile);
            if (error != null) {
                throw new IllegalArgumentException(error);
            }
            boolean enabled = intent.getBooleanExtra(EXTRA_ENABLED, false);
            boolean saved = context.getSharedPreferences(ProfileContract.PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(ProfileContract.PREF_JSON, profile.toString())
                    .putBoolean(ProfileContract.PREF_ENABLED, enabled)
                    .commit();
            if (!saved) {
                throw new IllegalStateException("SharedPreferences commit failed");
            }
            ProfileAccess.grantToTarget(context);
            setResultCode(Activity.RESULT_OK);
            setResultData(enabled ? "Profile saved and enabled" : "Profile saved and disabled");
        } catch (Exception error) {
            setResultCode(Activity.RESULT_CANCELED);
            setResultData("Profile rejected: " + error.getMessage());
        }
    }
}
