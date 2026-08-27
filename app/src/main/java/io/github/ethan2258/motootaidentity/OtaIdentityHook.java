package io.github.ethan2258.motootaidentity;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class OtaIdentityHook implements IXposedHookLoadPackage {
    private static final String TAG = "MotoOtaIdentity";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!ProfileContract.TARGET_PACKAGE.equals(loadPackageParam.packageName)) {
            return;
        }

        try {
            Class<?> botaSettings = XposedHelpers.findClass(
                    "com.motorola.ccc.ota.sources.bota.settings.BotaSettings",
                    loadPackageParam.classLoader);

            XposedHelpers.findAndHookMethod(
                    "com.motorola.ccc.ota.utils.BuildPropReader",
                    loadPackageParam.classLoader,
                    "getExtraInfoAsJsonObject",
                    Context.class,
                    String.class,
                    int.class,
                    String.class,
                    String.class,
                    String.class,
                    botaSettings,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            applyProfile(param);
                        }
                    });
            log("Hook installed for " + ProfileContract.TARGET_PACKAGE);
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": hook installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void applyProfile(XC_MethodHook.MethodHookParam param) {
        try {
            if (!(param.getResult() instanceof JSONObject)
                    || param.args.length == 0
                    || !(param.args[0] instanceof Context)) {
                return;
            }

            ActiveProfile activeProfile = readProfile((Context) param.args[0]);
            if (!activeProfile.enabled) {
                log("Profile disabled; original request preserved");
                return;
            }

            String validationError = ProfileValidator.validate(activeProfile.profile);
            if (validationError != null) {
                log("Profile rejected: " + validationError);
                return;
            }

            JSONObject originalRequest = (JSONObject) param.getResult();
            JSONObject request = new JSONObject(originalRequest.toString());
            for (String key : ProfileContract.FIELDS) {
                if ("profileName".equals(key)) {
                    continue;
                }
                String value = activeProfile.profile.optString(key, "").trim();
                if (!value.isEmpty()) {
                    if ("ro.mot.version".equals(key)) {
                        request.put(key, Integer.parseInt(value));
                    } else {
                        request.put(key, value);
                    }
                }
            }

            String profileName = activeProfile.profile.optString("profileName", "unnamed");
            log("Applied profile=" + profileName + " digest=" + digest(activeProfile.profile));
            // Publish only a fully constructed copy so any failure leaves the original untouched.
            param.setResult(request);
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": profile application failed; original request preserved");
            XposedBridge.log(throwable);
        }
    }

    private static ActiveProfile readProfile(Context context) throws Exception {
        Uri uri = Uri.parse(ProfileContract.PROFILE_URI);
        try (Cursor cursor = context.getContentResolver().query(
                uri, new String[]{"enabled", "profile_json"}, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return new ActiveProfile(false, new JSONObject());
            }
            boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow("enabled")) == 1;
            String json = cursor.getString(cursor.getColumnIndexOrThrow("profile_json"));
            return new ActiveProfile(enabled, new JSONObject(json == null ? "{}" : json));
        }
    }

    private static String digest(JSONObject profile) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(profile.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(12);
        for (int i = 0; i < 6; i++) {
            result.append(String.format("%02x", hash[i]));
        }
        return result.toString();
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static final class ActiveProfile {
        final boolean enabled;
        final JSONObject profile;

        ActiveProfile(boolean enabled, JSONObject profile) {
            this.enabled = enabled;
            this.profile = profile;
        }
    }
}
