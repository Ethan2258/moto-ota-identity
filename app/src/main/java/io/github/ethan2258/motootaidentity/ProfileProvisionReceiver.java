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
    private static final String EXTRA_CHANNEL_ALIAS = "channel_alias";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_PROVISION.equals(intent.getAction())) {
            return;
        }
        try {
            String encoded = intent.getStringExtra(EXTRA_PROFILE_BASE64);
            String channelAlias = intent.getStringExtra(EXTRA_CHANNEL_ALIAS);
            boolean hasProfile = encoded != null;
            boolean hasEnabled = intent.hasExtra(EXTRA_ENABLED);
            boolean hasChannelAlias = intent.hasExtra(EXTRA_CHANNEL_ALIAS);
            if (!hasProfile && !hasEnabled && !hasChannelAlias) {
                throw new IllegalArgumentException("No configuration supplied");
            }
            if (hasProfile && encoded.length() > 32_768) {
                throw new IllegalArgumentException("Oversized profile_base64");
            }
            if (hasChannelAlias && !OtaChannelAlias.isValid(channelAlias)) {
                throw new IllegalArgumentException("Unsupported channel_alias");
            }

            android.content.SharedPreferences.Editor editor = context
                    .getSharedPreferences(ProfileContract.PREFS, Context.MODE_PRIVATE)
                    .edit();
            if (hasProfile) {
                String jsonText = new String(
                        Base64.decode(encoded, Base64.NO_WRAP),
                        StandardCharsets.UTF_8);
                JSONObject profile = new JSONObject(jsonText);
                String error = ProfileValidator.validate(profile);
                if (error != null) {
                    throw new IllegalArgumentException(error);
                }
                editor.putString(ProfileContract.PREF_JSON, profile.toString());
            }
            if (hasEnabled) {
                editor.putBoolean(
                        ProfileContract.PREF_ENABLED,
                        intent.getBooleanExtra(EXTRA_ENABLED, false));
            }
            if (hasChannelAlias) {
                editor.putString(
                        ProfileContract.PREF_CHANNEL_ALIAS,
                        OtaChannelAlias.normalize(channelAlias));
            }
            boolean saved = editor.commit();
            if (!saved) {
                throw new IllegalStateException("SharedPreferences commit failed");
            }
            ProfileAccess.grantToTarget(context);
            setResultCode(Activity.RESULT_OK);
            setResultData("Configuration saved");
        } catch (Exception error) {
            setResultCode(Activity.RESULT_CANCELED);
            setResultData("Configuration rejected: " + error.getMessage());
        }
    }
}
