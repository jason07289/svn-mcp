package io.github.jason07289.svn.mcp.svn.svnkit;

/** Counts +/- lines in a unified diff (excludes file headers). */
final class UnifiedDiffLineStats {

    private UnifiedDiffLineStats() {}

    static long[] countAddedRemoved(String unifiedDiff) {
        if (unifiedDiff == null || unifiedDiff.isEmpty()) {
            return new long[] {0L, 0L};
        }
        long added = 0;
        long removed = 0;
        for (String line : unifiedDiff.split("\n", -1)) {
            if (line.startsWith("+++") || line.startsWith("---")) {
                continue;
            }
            if (line.startsWith("+")) {
                added++;
            } else if (line.startsWith("-")) {
                removed++;
            }
        }
        return new long[] {added, removed};
    }
}
