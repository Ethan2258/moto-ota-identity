package io.github.ethan2258.motootaidentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONObject;
import org.junit.Test;

public final class ProfileValidatorTest {
    @Test
    public void completeProfileIsAccepted() throws Exception {
        assertNull(ProfileValidator.validate(completeProfile()));
    }

    @Test
    public void missingRequiredFieldIsRejected() throws Exception {
        JSONObject profile = completeProfile();
        profile.put("otaSourceSha1", "");

        assertEquals("缺少必要字段: otaSourceSha1", ProfileValidator.validate(profile));
    }

    @Test
    public void nonNumericMotorolaVersionIsRejected() throws Exception {
        JSONObject profile = completeProfile();
        profile.put("ro.mot.version", "not-an-integer");

        assertEquals("ro.mot.version 必须是整数", ProfileValidator.validate(profile));
    }

    private static JSONObject completeProfile() throws Exception {
        JSONObject profile = new JSONObject();
        for (String key : ProfileContract.FIELDS) {
            profile.put(key, "");
        }
        profile.put("fingerprint", "motorola/device/product:16/BUILD/123:user/release-keys");
        profile.put("buildDevice", "device");
        profile.put("buildId", "BUILD");
        profile.put("buildDisplayId", "BUILD-1");
        profile.put("buildIncrementalVersion", "123");
        profile.put("otaSourceSha1", "1234567890abcdef");
        profile.put("userLocation", "Non-CN");
        profile.put("canonicalName", "product");
        profile.put("ro.mot.build.device", "device");
        profile.put("ro.mot.build.oem.product", "product");
        profile.put("ro.mot.build.system.product", "product");
        profile.put("ro.mot.build.product.increment", "123");
        profile.put("ro.mot.version", "1");
        profile.put("securityVersion", "2026-07-01");
        return profile;
    }
}
