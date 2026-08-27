package io.github.ethan2258.motootaidentity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProfileValidator {
    private ProfileValidator() {
    }

    public static String validate(JSONObject profile) {
        List<String> missing = new ArrayList<>();
        for (String key : ProfileContract.REQUIRED) {
            if (profile.optString(key, "").trim().isEmpty()) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            return "缺少必要字段: " + String.join(", ", missing);
        }

        String fingerprint = profile.optString("fingerprint", "");
        if (!fingerprint.contains("/") || !fingerprint.contains(":")) {
            return "fingerprint 格式不完整";
        }

        String sourceSha1 = profile.optString("otaSourceSha1", "").trim();
        if (sourceSha1.length() < 8 || sourceSha1.length() > 128) {
            return "otaSourceSha1 长度异常";
        }

        String securityVersion = profile.optString("securityVersion", "");
        if (!securityVersion.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "securityVersion 必须是 YYYY-MM-DD";
        }

        String location = profile.optString("userLocation", "Non-CN");
        if (!("CN".equals(location) || "Non-CN".equals(location))) {
            return "userLocation 只能是 CN 或 Non-CN";
        }

        String motVersion = profile.optString("ro.mot.version", "").trim();
        if (!motVersion.isEmpty()) {
            try {
                Integer.parseInt(motVersion);
            } catch (NumberFormatException exception) {
                return "ro.mot.version 必须是整数";
            }
        }

        for (String key : ProfileContract.FIELDS) {
            String value = profile.optString(key, "");
            if (value.length() > 512) {
                return String.format(Locale.US, "%s 超过 512 字符", key);
            }
        }
        return null;
    }
}
