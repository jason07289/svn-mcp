package io.github.jason07289.svn.mcp.svn.svnkit;

import io.github.jason07289.svn.mcp.config.SvnMcpProperties;
import io.github.jason07289.svn.mcp.config.SvnMcpProperties.RepositoryEntry;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Creates a temporary FSFS repository ({@code file://}) and seeds it with two revisions on
 * {@code readme.txt} for integration-style tests.
 */
public final class LocalSvnRepositorySupport {

    private LocalSvnRepositorySupport() {}

    public static void setupSvnKitFactories() {
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
    }

    /**
     * After return: revision 1 = "line1\n", revision 2 = "line1\nline2\n" for {@code readme.txt} at
     * repository root.
     */
    public static Fixture createTwoRevisionReadmeRepo(Path tempDir) throws Exception {
        setupSvnKitFactories();
        File repoDir = tempDir.resolve("svn-repo").toFile();
        SVNURL repoUrl = SVNRepositoryFactory.createLocalRepository(repoDir, true, true);

        File importRoot = tempDir.resolve("import-stage").toFile();
        importRoot.mkdirs();
        Files.writeString(importRoot.toPath().resolve("readme.txt"), "line1\n", StandardCharsets.UTF_8);

        SVNClientManager cm = SVNClientManager.newInstance();
        cm.getCommitClient().doImport(importRoot, repoUrl, "initial import", true);

        File wc = tempDir.resolve("wc").toFile();
        cm.getUpdateClient()
                .doCheckout(
                        repoUrl,
                        wc,
                        SVNRevision.HEAD,
                        SVNRevision.HEAD,
                        SVNDepth.INFINITY,
                        false);

        File readme = new File(wc, "readme.txt");
        Files.writeString(readme.toPath(), "line1\nline2\n", StandardCharsets.UTF_8);
        cm.getCommitClient().doCommit(new File[] {wc}, false, "second revision", false, true);

        cm.dispose();

        SvnMcpProperties properties = new SvnMcpProperties();
        RepositoryEntry entry = new RepositoryEntry();
        entry.setId("local");
        entry.setName("local test");
        entry.setRootUrl(repoUrl.toString());
        entry.setGroup("test");
        properties.setRepositories(List.of(entry));
        properties.getDefaults().setLogLimitMax(500);
        properties.getDefaults().setFileContentMaxBytes(2_000_000L);

        return new Fixture(properties, repoUrl);
    }

    /**
     * Repository root contains two top-level directories {@code a} and {@code b} (each with a file),
     * for tests that need multiple directory entries at the repository root.
     */
    public static Fixture createRepoWithTwoRootDirectories(Path tempDir) throws Exception {
        setupSvnKitFactories();
        File repoDir = tempDir.resolve("svn-repo").toFile();
        SVNURL repoUrl = SVNRepositoryFactory.createLocalRepository(repoDir, true, true);

        File importRoot = tempDir.resolve("import-stage").toFile();
        importRoot.mkdirs();
        new File(importRoot, "a").mkdirs();
        new File(importRoot, "b").mkdirs();
        Files.writeString(importRoot.toPath().resolve("a").resolve("x.txt"), "a\n", StandardCharsets.UTF_8);
        Files.writeString(importRoot.toPath().resolve("b").resolve("y.txt"), "b\n", StandardCharsets.UTF_8);

        SVNClientManager cm = SVNClientManager.newInstance();
        cm.getCommitClient().doImport(importRoot, repoUrl, "import two dirs", true);
        cm.dispose();

        SvnMcpProperties properties = new SvnMcpProperties();
        RepositoryEntry entry = new RepositoryEntry();
        entry.setId("local");
        entry.setName("local test");
        entry.setRootUrl(repoUrl.toString());
        entry.setGroup("test");
        properties.setRepositories(List.of(entry));
        properties.getDefaults().setLogLimitMax(500);
        properties.getDefaults().setFileContentMaxBytes(2_000_000L);

        return new Fixture(properties, repoUrl);
    }

    public record Fixture(SvnMcpProperties properties, SVNURL repoUrl) {}
}
