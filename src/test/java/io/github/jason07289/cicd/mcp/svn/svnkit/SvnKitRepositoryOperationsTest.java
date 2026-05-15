package io.github.jason07289.cicd.mcp.svn.svnkit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jason07289.cicd.mcp.svn.api.*;
import io.github.jason07289.cicd.mcp.svn.service.RepositoryEntryResolver;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("svnkit")
class SvnKitRepositoryOperationsTest {

    @TempDir Path tempDir;

    @Test
    void listPath_tree_listsRootEntries() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        ListPathResult r = ops.listPath("local", "", null, null, "tree", null, null);

        assertThat(r.viewMode()).isEqualTo("tree");
        assertThat(r.path()).isEmpty();
        assertThat(r.entries()).extracting(PathEntry::name).contains("readme.txt");
    }

    @Test
    void listPath_flat_containsReadmeFile() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        ListPathResult r =
                ops.listPath("local", "", null, null, "flat", 3, 50);

        assertThat(r.viewMode()).isEqualTo("flat");
        assertThat(r.entries()).anyMatch(e -> "readme.txt".equals(e.name()));
    }

    @Test
    void getFile_readsHeadContent() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        GetFileResult file = ops.getFile("local", "readme.txt", null, null);

        assertThat(file.text()).isTrue();
        assertThat(file.contentText()).contains("line2");
    }

    @Test
    void getLog_returnsAtLeastTwoRevisions() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        GetLogResult log =
                ops.getLog("local", "", null, null, 20, false, null, null, null, null);

        assertThat(log.entries()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void getRevision_returnsMetadataForRev2() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        GetRevisionResult rev = ops.getRevision("local", 2L);

        assertThat(rev.revision()).isEqualTo(2L);
        assertThat(rev.message()).isNotBlank();
    }

    @Test
    void diffFile_unifiedDiffBetweenRevisions() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        DiffFileResult diff = ops.diffFile("local", "readme.txt", 1L, 2L, false, DiffFileRequest.defaults());

        assertThat(diff.unifiedDiff()).isNotBlank();
    }

    @Test
    void diffFile_ignoreWhitespace_changesOutput() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        DiffFileResult strict =
                ops.diffFile("local", "readme.txt", 1L, 2L, false, DiffFileRequest.defaults());
        DiffFileResult ignore =
                ops.diffFile("local", "readme.txt", 1L, 2L, true, DiffFileRequest.defaults());

        assertThat(strict.unifiedDiff()).isNotBlank();
        assertThat(ignore.unifiedDiff()).isNotBlank();
    }

    @Test
    void diffRevision_returnsNonEmptyForSecondRevision() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        DiffRevisionResult dr =
                ops.diffRevision("local", "", 2L, DiffRevisionRequest.legacy(false));
        assertThat(dr.unifiedDiff()).isNotBlank();
        assertThat(dr.fromRevision()).isEqualTo(1L);
        assertThat(dr.revision()).isEqualTo(2L);
    }

    @Test
    void diffRevision_pathsOnly_returnsChangedPathsWithoutUnifiedBody() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        DiffRevisionResult dr =
                ops.diffRevision(
                        "local",
                        "",
                        2L,
                        new DiffRevisionRequest(
                                false,
                                "paths_only",
                                DiffRevisionRequest.LimitPolicy.MCP_DEFAULT,
                                null,
                                null,
                                null,
                                null,
                                false));

        assertThat(dr.unifiedDiff()).isNull();
        assertThat(dr.changedPaths()).isNotEmpty();
    }

    @Test
    void resolveRevisionRange_returnsRevisions() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        Date start = new Date(0L);
        Date end = new Date(System.currentTimeMillis() + 60_000L);
        ResolveRevisionRangeResult rr = ops.resolveRevisionRange("local", "", start, end);
        assertThat(rr.startRevision()).isGreaterThanOrEqualTo(rr.endRevision());
    }

    @Test
    void repositoryAuthorStats_returnsRankings() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        Date start = new Date(0L);
        Date end = new Date(System.currentTimeMillis() + 60_000L);
        RepositoryAuthorStatsResult stats =
                ops.repositoryAuthorStats("local", "", start, end, 50);
        assertThat(stats.byAuthor()).isNotEmpty();
        assertThat(stats.rankings().get("largest_diff")).isNotBlank();
    }

    @Test
    void blameFile_returnsLines() throws Exception {
        LocalSvnRepositorySupport.Fixture fx =
                LocalSvnRepositorySupport.createTwoRevisionReadmeRepo(tempDir);
        SvnKitRepositoryOperations ops = operations(fx);

        List<BlameLine> lines = ops.blameFile("local", "readme.txt", null);

        assertThat(lines).isNotEmpty();
        assertThat(lines.get(0).line()).isNotNull();
    }

    private static SvnKitRepositoryOperations operations(LocalSvnRepositorySupport.Fixture fx) {
        RepositoryEntryResolver resolver = new RepositoryEntryResolver(fx.properties());
        return new SvnKitRepositoryOperations(resolver, fx.properties());
    }
}
