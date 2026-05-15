package io.github.jason07289.cicd.mcp.svn.svnkit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jason07289.cicd.mcp.svn.api.DiffRevisionTruncation;
import org.junit.jupiter.api.Test;

class UnifiedDiffTruncationTest {

    @Test
    void truncate_respectsPerFileAndTotalLines() {
        String diff =
                "Index: a.txt\n"
                        + "===================================================================\n"
                        + "line-a1\n"
                        + "line-a2\n"
                        + "Index: b.txt\n"
                        + "===================================================================\n"
                        + "line-b1\n"
                        + "line-b2\n"
                        + "line-b3\n";

        UnifiedDiffTruncation.Result r =
                UnifiedDiffTruncation.truncate(
                        diff,
                        /* maxTotal */ 100,
                        /* maxPerFile */ 2,
                        /* maxFiles */ 1,
                        /* lineOffset */ 0);

        assertThat(r.text()).contains("Index: a.txt").doesNotContain("Index: b.txt");
        DiffRevisionTruncation m = r.meta();
        assertThat(m.lineTruncated()).isTrue();
        assertThat(m.fileSectionsIncluded()).isEqualTo(1);
        assertThat(m.fileSectionsOmitted()).isEqualTo(1);
    }

    @Test
    void lineOffset_skipsLinesInCappedAssembly() {
        String diff = "Index: x\n===\nL1\nL2\nL3\n";
        UnifiedDiffTruncation.Result r =
                UnifiedDiffTruncation.truncate(diff, 100, 100, 10, 4);
        assertThat(r.text()).contains("L3");
        assertThat(r.text()).doesNotContain("L1");
        assertThat(r.meta().lineOffsetApplied()).isEqualTo(4);
    }
}
