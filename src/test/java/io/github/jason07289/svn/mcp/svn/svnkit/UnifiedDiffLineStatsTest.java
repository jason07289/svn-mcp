package io.github.jason07289.svn.mcp.svn.svnkit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UnifiedDiffLineStatsTest {

    @Test
    void countsPlusMinusLines() {
        String diff =
                "--- a/f\n+++ b/f\n@@ -1 +1 @@\n-old\n+new\n+another\n context\n";
        long[] r = UnifiedDiffLineStats.countAddedRemoved(diff);
        assertThat(r[0]).isEqualTo(2L);
        assertThat(r[1]).isEqualTo(1L);
    }
}
