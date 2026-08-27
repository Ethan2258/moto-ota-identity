package io.github.ethan2258.motootaidentity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class OtaChannelAlias {
    public static final String OFF = "";
    public static final String DEFAULT = "retgb";

    private static final Set<String> ALLOWED = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(OFF, "retgb", "teleu", "retapac", "reteu")));

    public static boolean isValid(String value) {
        return value != null && ALLOWED.contains(value.trim().toLowerCase(Locale.ROOT));
    }

    public static String normalize(String value) {
        if (value == null) {
            return OFF;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return ALLOWED.contains(normalized) ? normalized : OFF;
    }

    private OtaChannelAlias() {
    }
}
