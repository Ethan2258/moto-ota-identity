package io.github.ethan2258.motootaidentity;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ProfileContract {
    public static final String MODULE_PACKAGE = "io.github.ethan2258.motootaidentity";
    public static final String TARGET_PACKAGE = "com.motorola.ccc.ota";
    public static final String AUTHORITY = MODULE_PACKAGE + ".profile";
    public static final String PROFILE_URI = "content://" + AUTHORITY + "/active";
    public static final String PREFS = "profile";
    public static final String PREF_ENABLED = "enabled";
    public static final String PREF_JSON = "profile_json";
    public static final String PREF_CHANNEL_ALIAS = "channel_alias";

    public static final String[] FIELDS = {
            "profileName",
            "carrier",
            "model",
            "fingerprint",
            "bootloaderVersion",
            "radioVersion",
            "buildDevice",
            "buildId",
            "buildDisplayId",
            "buildIncrementalVersion",
            "releaseVersion",
            "otaSourceSha1",
            "userLocation",
            "canonicalName",
            "ro.mot.build.device",
            "ro.mot.build.oem.product",
            "ro.mot.build.system.product",
            "ro.mot.build.product.increment",
            "ro.mot.version",
            "securityVersion"
    };

    public static final Set<String> REQUIRED = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "fingerprint",
                    "buildDevice",
                    "buildId",
                    "buildDisplayId",
                    "buildIncrementalVersion",
                    "otaSourceSha1",
                    "canonicalName",
                    "ro.mot.build.device",
                    "ro.mot.build.oem.product",
                    "ro.mot.build.system.product",
                    "ro.mot.build.product.increment",
                    "securityVersion"
            ))
    );

    private ProfileContract() {
    }
}
