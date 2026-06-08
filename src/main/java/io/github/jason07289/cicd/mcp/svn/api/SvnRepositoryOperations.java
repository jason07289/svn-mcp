package io.github.jason07289.cicd.mcp.svn.api;

import java.util.Date;
import java.util.List;

/** Read-only SVN operations against configured repositories (SVNKit-backed). */
public interface SvnRepositoryOperations {

    ListPathResult listPath(
            String repositoryId,
            String path,
            Long revision,
            Long pegRevision,
            String viewMode,
            Integer flatMaxDepth,
            Integer flatMaxEntries)
            throws SvnAccessException;

    GetFileResult getFile(String repositoryId, String path, Long revision, Long pegRevision)
            throws SvnAccessException;

    GetLogResult getLog(
            String repositoryId,
            String path,
            Long startRevision,
            Long endRevision,
            Integer limit,
            Boolean stopOnCopy,
            Date startDateInclusive,
            Date endDateInclusive,
            String author,
            String authorMatch)
            throws SvnAccessException;

    GetRevisionResult getRevision(String repositoryId, long revision) throws SvnAccessException;

    DiffFileResult diffFile(
            String repositoryId,
            String path,
            Long fromRevision,
            Long toRevision,
            boolean ignoreWhitespace,
            DiffFileRequest limits)
            throws SvnAccessException;

    List<BlameLine> blameFile(String repositoryId, String path, Long revision)
            throws SvnAccessException;

    /**
     * Maps instants to revisions via {@link org.tmatesoft.svn.core.io.SVNRepository#getDatedRevision}.
     */
    ResolveRevisionRangeResult resolveRevisionRange(
            String repositoryId, String path, Date startInclusive, Date endInclusive)
            throws SvnAccessException;

    /** Unified diff for one revision (equivalent to {@code svn diff -c REV}) under path prefix. */
    DiffRevisionResult diffRevision(
            String repositoryId, String path, long revision, DiffRevisionRequest request)
            throws SvnAccessException;

    /** Aggregates per-author commit counts and diff line stats for a time window. */
    RepositoryAuthorStatsResult repositoryAuthorStats(
            String repositoryId,
            String pathPrefix,
            Date startInclusive,
            Date endInclusive,
            Integer maxRevisionsToAnalyze)
            throws SvnAccessException;

    /**
     * Searches file contents under a path for a keyword, returning matching files with their last
     * author. All file reads are performed within a single SVN session to avoid N+1 connection
     * overhead.
     */
    SearchInPathResult searchInPath(
            String repositoryId,
            String path,
            String keyword,
            Long revision,
            List<String> fileExtensions,
            boolean caseSensitive,
            int maxFilesToScan,
            int maxMatches)
            throws SvnAccessException;
}
