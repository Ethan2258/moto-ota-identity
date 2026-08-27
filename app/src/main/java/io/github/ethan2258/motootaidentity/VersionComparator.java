package io.github.ethan2258.motootaidentity;

public final class VersionComparator {
    private VersionComparator() {
    }

    public static boolean isNewer(String candidate, String current) {
        int[] candidateParts = parse(candidate);
        int[] currentParts = parse(current);
        int length = Math.max(candidateParts.length, currentParts.length);
        for (int index = 0; index < length; index++) {
            int candidatePart = index < candidateParts.length ? candidateParts[index] : 0;
            int currentPart = index < currentParts.length ? currentParts[index] : 0;
            if (candidatePart != currentPart) {
                return candidatePart > currentPart;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        String normalized = version == null ? "" : version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        int suffix = normalized.indexOf('-');
        if (suffix >= 0) {
            normalized = normalized.substring(0, suffix);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Version is empty");
        }
        String[] parts = normalized.split("\\.");
        int[] result = new int[parts.length];
        for (int index = 0; index < parts.length; index++) {
            result[index] = Integer.parseInt(parts[index]);
            if (result[index] < 0) {
                throw new IllegalArgumentException("Version component is negative");
            }
        }
        return result;
    }
}
