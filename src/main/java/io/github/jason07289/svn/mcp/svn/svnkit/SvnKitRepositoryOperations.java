package io.github.jason07289.svn.mcp.svn.svnkit;

import io.github.jason07289.svn.mcp.config.SvnMcpProperties;
import io.github.jason07289.svn.mcp.config.SvnMcpProperties.RepositoryEntry;
import io.github.jason07289.svn.mcp.svn.api.*;
import io.github.jason07289.svn.mcp.svn.api.SearchInPathResult;
import io.github.jason07289.svn.mcp.svn.api.SearchMatch;
import io.github.jason07289.svn.mcp.svn.service.RepositoryEntryResolver;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnAnnotate;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

@Service
public class SvnKitRepositoryOperations implements SvnRepositoryOperations {

    private static final Logger log = LoggerFactory.getLogger(SvnKitRepositoryOperations.class);

    private static final int DEFAULT_FLAT_MAX_DEPTH = 6;
    private static final int DEFAULT_FLAT_MAX_ENTRIES = 2000;

    private final RepositoryEntryResolver resolver;
    private final SvnMcpProperties properties;

    public SvnKitRepositoryOperations(
            RepositoryEntryResolver resolver, SvnMcpProperties properties) {
        this.resolver = resolver;
        this.properties = properties;
    }

    @Override
    public ListPathResult listPath(
            String repositoryId,
            String path,
            Long revision,
            Long pegRevision,
            String viewMode,
            Integer flatMaxDepth,
            Integer flatMaxEntries)
            throws SvnAccessException {
        RepositoryEntry entry = resolver.require(repositoryId);
        String normalized = normalizePath(path);
        String mode = viewMode == null || viewMode.isBlank() ? "tree" : viewMode.toLowerCase();
        try (RepositorySession session = open(entry)) {
            SVNRepository repo = session.repository();
            long rev = resolveRevision(repo, revision);
            if ("flat".equals(mode)) {
                int depth =
                        flatMaxDepth != null && flatMaxDepth > 0
                                ? flatMaxDepth
                                : DEFAULT_FLAT_MAX_DEPTH;
                int maxEntries =
                        flatMaxEntries != null && flatMaxEntries > 0
                                ? flatMaxEntries
                                : DEFAULT_FLAT_MAX_ENTRIES;
                List<PathEntry> entries =
                        listFlat(repo, normalized, rev, depth, maxEntries);
                return new ListPathResult(rev, normalized, "flat", entries);
            }
            SVNNodeKind kind = repo.checkPath(normalized, rev);
            if (kind != SVNNodeKind.DIR) {
                throw new SvnAccessException(
                        "Path is not a directory at r" + rev + ": " + normalized);
            }
            List<PathEntry> entries = new ArrayList<>();
            repo.getDir(
                    normalized,
                    rev,
                    null,
                    (SVNDirEntry dirEntry) ->
                            entries.add(
                                    new PathEntry(
                                            dirEntry.getName(),
                                            joinRelative(normalized, dirEntry.getName()),
                                            kindName(dirEntry.getKind()),
                                            dirEntry.getSize())));
            return new ListPathResult(rev, normalized, "tree", entries);
        } catch (SVNException e) {
            throw svnAccessFailed("list_path", repositoryId, e);
        }
    }

    @Override
    public GetFileResult getFile(
            String repositoryId, String path, Long revision, Long pegRevision)
            throws SvnAccessException {
        RepositoryEntry entry = resolver.require(repositoryId);
        String normalized = normalizePath(path);
        try (RepositorySession session = open(entry)) {
            SVNRepository repo = session.repository();
            long rev = resolveRevision(repo, revision);
            SVNNodeKind kind = repo.checkPath(normalized, rev);
            if (kind != SVNNodeKind.FILE) {
                throw new SvnAccessException(
                        "Path is not a file at r" + rev + ": " + normalized);
            }
            SVNProperties props = new SVNProperties();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            repo.getFile(normalized, rev, props, out);
            byte[] bytes = out.toByteArray();
            long maxBytes = properties.getDefaults().getFileContentMaxBytes();
            boolean truncated = bytes.length > maxBytes;
            byte[] payload = truncated ? java.util.Arrays.copyOf(bytes, (int) maxBytes) : bytes;

            String mime = props.getStringValue(SVNProperty.MIME_TYPE);
            boolean text = isProbablyText(mime, payload);
            String encodingHint = text ? "UTF-8" : null;
            String textContent = null;
            String base64 = null;
            if (text) {
                textContent = decodeUtf8Lenient(payload);
            } else {
                base64 = Base64.getEncoder().encodeToString(payload);
            }
            return new GetFileResult(
                    rev, normalized, mime, text, encodingHint, truncated, textContent, base64);
        } catch (SVNException e) {
            throw svnAccessFailed("get_file", repositoryId, e);
        }
    }

    @Override
    public GetLogResult getLog(
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
            throws SvnAccessException {
        RepositoryEntry entry = resolver.require(repositoryId);
        String normalized = normalizePath(path);
        int maxLimit = Math.min(properties.getDefaults().getLogLimitMax(), Integer.MAX_VALUE);
        int lim =
                limit == null || limit <= 0
                        ? maxLimit
                        : Math.min(limit, properties.getDefaults().getLogLimitMax());
        boolean stop = stopOnCopy != null && stopOnCopy;
        try (RepositorySession session = open(entry)) {
            SVNRepository repo = session.repository();
            long latest = repo.getLatestRevision();
            long start;
            long end;
            if (startDateInclusive != null || endDateInclusive != null) {
                Date dEnd = endDateInclusive != null ? endDateInclusive : new Date();
                Date dStart =
                        startDateInclusive != null ? startDateInclusive : new Date(0L);
                if (dEnd.before(dStart)) {
                    throw new SvnAccessException("end_date must be on or after start_date");
                }
                long revAtEnd = repo.getDatedRevision(dEnd);
                long revAtStart = repo.getDatedRevision(dStart);
                start = Math.max(revAtEnd, revAtStart);
                end = Math.min(revAtEnd, revAtStart);
            } else {
                start = startRevision != null ? startRevision : latest;
                end = endRevision != null ? endRevision : 0L;
            }
            List<LogEntry> entries = new ArrayList<>();
            String matchMode =
                    authorMatch == null || authorMatch.isBlank() ? "exact" : authorMatch.trim();
            repo.log(
                    new String[] {normalized},
                    start,
                    end,
                    true,
                    false,
                    lim,
                    stop,
                    null,
                    (SVNLogEntry logEntry) -> {
                        if (logEntry.getRevision() < 0) {
                            return;
                        }
                        if (!matchesAuthorFilter(author, matchMode, logEntry.getAuthor())) {
                            return;
                        }
                        if (!matchesDateFilter(
                                logEntry.getDate(), startDateInclusive, endDateInclusive)) {
                            return;
                        }
                        Map<String, String> changed = mapChangedPaths(logEntry.getChangedPaths());
                        entries.add(
                                new LogEntry(
                                        logEntry.getRevision(),
                                        logEntry.getAuthor(),
                                        logEntry.getDate(),
                                        logEntry.getMessage(),
                                        changed.isEmpty() ? null : changed));
                    });
            return new GetLogResult(normalized, start, end, entries);
        } catch (SVNException e) {
            throw svnAccessFailed("get_log", repositoryId, e);
        }
    }

    @Override
    public ResolveRevisionRangeResult resolveRevisionRange(
            String repositoryId, String path, Date startInclusive, Date endInclusive)
            throws SvnAccessException {
        if (startInclusive == null || endInclusive == null) {
            throw new SvnAccessException("start and end dates are required");
        }
        if (endInclusive.before(startInclusive)) {
            throw new SvnAccessException("end must be on or after start");
        }
        RepositoryEntry entry = resolver.require(repositoryId);
        String normalized = normalizePath(path);
        try (RepositorySession session = open(entry)) {
            SVNRepository repo = session.repository();
            long rEnd = repo.getDatedRevision(endInclusive);
            long rStart = repo.getDatedRevision(startInclusive);
            long high = Math.max(rEnd, rStart);
            long low = Math.min(rEnd, rStart);
            String note =
                    "Revisions are from getDatedRevision; filter LogEntry.date for strict bounds.";
            return new ResolveRevisionRangeResult(
                    normalized, startInclusive, endInclusive, high, low, note);
        } catch (SVNException e) {
            throw svnAccessFailed("resolve_revision_range", repositoryId, e);
        }
    }

    @Override
    public DiffRevisionResult diffRevision(
            String repositoryId, String path, long revision, DiffRevisionRequest request)
            throws SvnAccessException {
        if (request == null) {
            throw new SvnAccessException("request is required");
        }
        if (revision <= 0) {
            throw new SvnAccessException("revision must be positive");
        }
        RepositoryEntry entry = resolver.require(repositoryId);
        String normalized = normalizePath(path);
        long fromRev = revision - 1;
        if ("paths_only".equalsIgnoreCase(request.outputMode().trim())) {
            GetRevisionResult meta = getRevision(repositoryId, revision);
            return new DiffRevisionResult(
                    normalized,
                    revision,
                    fromRev,
                    null,
                    false,
                    meta.changedPaths(),
                    null,
                    null);
        }
        if (!"unified".equalsIgnoreCase(request.outputMode().trim())) {
            throw new SvnAccessException(
                    "output_mode must be unified or paths_only, got: " + request.outputMode());
        }
        try (RepositorySession session = open(entry)) {
            SVNURL targetUrl =
                    normalized.isEmpty()
                            ? session.rootUrl()
                            : session.rootUrl().appendPath(normalized, false);
            ISVNAuthenticationManager auth = session.auth();
            SvnOperationFactory factory = new SvnOperationFactory();
            factory.setAuthenticationManager(auth);
            SvnDiff diff = factory.createDiff();
            diff.setSource(
                    SvnTarget.fromURL(targetUrl),
                    SVNRevision.create(fromRev),
                    SVNRevision.create(revision));
            SVNDiffOptions opts = new SVNDiffOptions();
            if (request.ignoreWhitespace()) {
                opts.setIgnoreAllWhitespace(true);
            }
            diff.setDiffOptions(opts);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            diff.setOutput(out);
            diff.run();
            factory.dispose();
            String fullText = out.toString(StandardCharsets.UTF_8);

            String spillPath = null;
            if (request.writeSpillFile()) {
                spillPath = writeSpillFileIfConfigured(repositoryId, revision, fullText);
            }

            if (request.limitPolicy() == DiffRevisionRequest.LimitPolicy.NONE) {
                return byteCapDiffRevision(normalized, revision, fromRev, fullText, spillPath);
            }

            SvnMcpProperties.Defaults d = properties.getDefaults();
            int maxTotal =
                    clampOptional(
                            request.maxTotalLines(),
                            d.getDiffMaxTotalLines(),
                            1,
                            Math.max(100, d.getDiffMaxTotalLines() * 4));
            int maxPerFile =
                    clampOptional(
                            request.maxLinesPerFile(),
                            d.getDiffMaxLinesPerFile(),
                            1,
                            Math.max(50, d.getDiffMaxLinesPerFile() * 4));
            int maxFiles =
                    clampOptional(
                            request.maxFiles(),
                            d.getDiffMaxFilesPerResponse(),
                            1,
                            Math.max(5, d.getDiffMaxFilesPerResponse() * 4));
            int lineOff = Math.max(0, request.lineOffset() != null ? request.lineOffset() : 0);
            int maxChars =
                    clampOptional(
                            request.maxCharsPerLine(),
                            d.getDiffMaxCharsPerLine(),
                            1,
                            Math.max(100, d.getDiffMaxCharsPerLine() * 4));
            long maxRespBytes =
                    clampLongOptional(
                            request.maxResponseBytes(),
                            d.getDiffMaxResponseBytes(),
                            1L,
                            Math.max(1024L, d.getDiffMaxResponseBytes() * 4L));

            UnifiedDiffTruncation.Result tr =
                    UnifiedDiffTruncation.truncate(
                            fullText, maxTotal, maxPerFile, maxFiles, lineOff, maxChars);
            return applyByteCapToDiffRevision(
                    normalized, revision, fromRev, tr.text(), tr.meta(), spillPath, maxRespBytes);
        } catch (SVNException e) {
            throw svnAccessFailed("diff_revision", repositoryId, e);
        }
    }

    @Override
    public RepositoryAuthorStatsResult repositoryAuthorStats(
            String repositoryId,
            String pathPrefix,
            Date startInclusive,
            Date endInclusive,
            Integer maxRevisionsToAnalyze)
            throws SvnAccessException {
        if (startInclusive == null || endInclusive == null) {
            throw new SvnAccessException("start and end dates are required");
        }
        if (endInclusive.before(startInclusive)) {
            throw new SvnAccessException("end must be on or after start");
        }
        int defaultCap = properties.getDefaults().getMaxRevisionsForStats();
        int cap =
                maxRevisionsToAnalyze != null && maxRevisionsToAnalyze > 0
                        ? Math.min(maxRevisionsToAnalyze, defaultCap)
                        : defaultCap;

        GetLogResult logResult =
                getLog(
                        repositoryId,
                        pathPrefix,
                        null,
                        null,
                        properties.getDefaults().getLogLimitMax(),
                        false,
                        startInclusive,
                        endInclusive,
                        null,
                        null);

        List<LogEntry> inWindow = new ArrayList<>(logResult.entries());
        inWindow.sort(Comparator.comparingLong(LogEntry::revision).reversed());

        List<LogEntry> toAnalyze = new ArrayList<>();
        for (LogEntry e : inWindow) {
            if (e.revision() <= 0) {
                continue;
            }
            toAnalyze.add(e);
            if (toAnalyze.size() >= cap) {
                break;
            }
        }
        long positiveRevisions =
                inWindow.stream().filter(e -> e.revision() > 0).count();
        boolean truncated = positiveRevisions > cap;

        Map<String, int[]> counts = new HashMap<>();
        Map<String, long[]> lines = new HashMap<>();
        Map<String, List<Long>> revs = new HashMap<>();

        for (LogEntry e : toAnalyze) {
            String authKey = authorKey(e.author());
            counts.putIfAbsent(authKey, new int[] {0});
            counts.get(authKey)[0]++;
            revs.computeIfAbsent(authKey, k -> new ArrayList<>()).add(e.revision());

            long[] addRem;
            try {
                DiffRevisionResult dr =
                        diffRevision(
                                repositoryId,
                                pathPrefix,
                                e.revision(),
                                DiffRevisionRequest.internalStats(false));
                addRem = UnifiedDiffLineStats.countAddedRemoved(dr.unifiedDiff());
            } catch (SvnAccessException ex) {
                log.warn(
                        "diff_revision failed for r{} in stats: {}",
                        e.revision(),
                        ex.getMessage());
                addRem = new long[] {0L, 0L};
            }
            long[] acc = lines.computeIfAbsent(authKey, k -> new long[] {0L, 0L});
            acc[0] += addRem[0];
            acc[1] += addRem[1];
        }

        List<AuthorActivityRow> rows = new ArrayList<>();
        for (Map.Entry<String, int[]> c : counts.entrySet()) {
            String a = c.getKey();
            long[] lr = lines.getOrDefault(a, new long[] {0L, 0L});
            long mag = lr[0] + lr[1];
            List<Long> rlist = revs.getOrDefault(a, List.of());
            rlist = new ArrayList<>(rlist);
            rlist.sort(Comparator.reverseOrder());
            rows.add(
                    new AuthorActivityRow(
                            a, c.getValue()[0], lr[0], lr[1], mag, rlist));
        }

        rows.sort(
                Comparator.comparingLong(AuthorActivityRow::diffMagnitude)
                        .reversed()
                        .thenComparingInt(AuthorActivityRow::commitCount)
                        .reversed()
                        .thenComparing(AuthorActivityRow::author));

        AuthorActivityRow mostCommitsRow = null;
        for (AuthorActivityRow r : rows) {
            if (mostCommitsRow == null
                    || r.commitCount() > mostCommitsRow.commitCount()
                    || (r.commitCount() == mostCommitsRow.commitCount()
                            && r.author().compareTo(mostCommitsRow.author()) < 0)) {
                mostCommitsRow = r;
            }
        }

        Map<String, String> rankings = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            rankings.put("largest_diff", rows.get(0).author());
        }
        if (mostCommitsRow != null) {
            rankings.put("most_commits", mostCommitsRow.author());
        }

        return new RepositoryAuthorStatsResult(
                repositoryId,
                normalizePath(pathPrefix),
                startInclusive,
                endInclusive,
                logResult.startRevision(),
                logResult.endRevision(),
                inWindow.size(),
                toAnalyze.size(),
                truncated,
                rows,
                rankings);
    }

    @Override
    public SearchInPathResult searchInPath(
            String repositoryId,
            String path,
            String keyword,
            Long revision,
            java.util.List<String> fileExtensions,
            boolean caseSensitive,
            int maxFilesToScan,
            int maxMatches)
            throws SvnAccessException {
        RepositoryEntry entry = resolver.require(repositoryId);
        String normalized = normalizePath(path);
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();
        java.util.Set<String> exts =
                (fileExtensions == null || fileExtensions.isEmpty())
                        ? java.util.Set.of("java")
                        : fileExtensions.stream()
                                .map(s -> s.toLowerCase().replaceFirst("^\\.", ""))
                                .collect(java.util.stream.Collectors.toSet());

        try (RepositorySession session = open(entry)) {
            SVNRepository repo = session.repository();
            long rev = resolveRevision(repo, revision);

            List<PathEntry> allFiles =
                    listFlat(repo, normalized, rev, DEFAULT_FLAT_MAX_DEPTH, maxFilesToScan * 2);

            List<PathEntry> candidates =
                    allFiles.stream()
                            .filter(
                                    f -> {
                                        String name = f.name().toLowerCase();
                                        int dot = name.lastIndexOf('.');
                                        if (dot < 0) return false;
                                        return exts.contains(name.substring(dot + 1));
                                    })
                            .collect(java.util.stream.Collectors.toList());

            int filesScanned = 0;
            boolean scanLimitReached = false;
            List<SearchMatch> matches = new ArrayList<>();

            for (PathEntry file : candidates) {
                if (filesScanned >= maxFilesToScan) {
                    scanLimitReached = true;
                    break;
                }
                if (matches.size() >= maxMatches) {
                    break;
                }
                filesScanned++;

                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    repo.getFile(file.relativePath(), rev, null, out);
                    byte[] bytes = out.toByteArray();

                    if (!isProbablyText(null, bytes)) {
                        continue;
                    }
                    String content = decodeUtf8Lenient(bytes);
                    String searchContent = caseSensitive ? content : content.toLowerCase();
                    if (!searchContent.contains(searchKeyword)) {
                        continue;
                    }

                    String[] lines = content.split("\n", -1);
                    List<String> matchedLines = new ArrayList<>();
                    int matchCount = 0;
                    for (String line : lines) {
                        String searchLine = caseSensitive ? line : line.toLowerCase();
                        if (searchLine.contains(searchKeyword)) {
                            matchCount++;
                            if (matchedLines.size() < 5) {
                                matchedLines.add(line.stripLeading());
                            }
                        }
                    }

                    String lastAuthor = null;
                    long lastRevision = rev;
                    List<SVNLogEntry> logBuf = new ArrayList<>();
                    repo.log(
                            new String[] {file.relativePath()},
                            rev,
                            1L,
                            false,
                            false,
                            1L,
                            false,
                            null,
                            (SVNLogEntry le) -> {
                                if (le.getRevision() >= 0 && logBuf.isEmpty()) {
                                    logBuf.add(le);
                                }
                            });
                    if (!logBuf.isEmpty()) {
                        lastAuthor = logBuf.get(0).getAuthor();
                        lastRevision = logBuf.get(0).getRevision();
                    }

                    String filePath = file.relativePath();
                    int lastSlash = filePath.lastIndexOf('/');
                    String modulePath = lastSlash > 0 ? filePath.substring(0, lastSlash) : normalized;
                    String fileName = lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;

                    matches.add(
                            new SearchMatch(
                                    modulePath,
                                    filePath,
                                    fileName,
                                    lastAuthor,
                                    lastRevision,
                                    matchCount,
                                    matchedLines.isEmpty() ? null : matchedLines));
                } catch (SVNException e) {
                    log.warn(
                            "Skipping file {} during search (r{}): {}",
                            file.relativePath(),
                            rev,
                            e.getMessage());
                }
            }

            return new SearchInPathResult(
                    rev, normalized, keyword, filesScanned, matches.size(), scanLimitReached, matches);
        } catch (SVNException e) {
            throw svnAccessFailed("search_in_path", repositoryId, e);
        }
    }

    private static String authorKey(String author) {
        if (author == null || author.isBlank()) {
            return "(no author)";
        }
        return author;
    }

    private static boolean matchesAuthorFilter(
            String authorFilter, String authorMatch, String logAuthor) {
        if (authorFilter == null || authorFilter.isBlank()) {
            return true;
        }
        String a = logAuthor != null ? logAuthor : "";
        if ("contains".equalsIgnoreCase(authorMatch)) {
            return a.contains(authorFilter);
        }
        return authorFilter.equals(a);
    }

    private static boolean matchesDateFilter(
            Date commitDate, Date startInclusive, Date endInclusive) {
        if (startInclusive == null && endInclusive == null) {
            return true;
        }
        if (commitDate == null) {
            return false;
        }
        if (startInclusive != null && commitDate.before(startInclusive)) {
            return false;
        }
        if (endInclusive != null && commitDate.after(endInclusive)) {
            return false;
        }
        return true;
    }

    @Override
    public GetRevisionResult getRevision(String repositoryId, long revision)
            throws SvnAccessException {
        RepositoryEntry entry = resolver.require(repositoryId);
        try (RepositorySession session = open(entry)) {
            SVNRepository repo = session.repository();
            List<GetRevisionResult> holder = new ArrayList<>(1);
            repo.log(
                    new String[] {""},
                    revision,
                    revision,
                    true,
                    false,
                    1,
                    false,
                    null,
                    (SVNLogEntry logEntry) -> {
                        Map<String, String> changed = mapChangedPaths(logEntry.getChangedPaths());
                        holder.add(
                                new GetRevisionResult(
                                        logEntry.getRevision(),
                                        logEntry.getAuthor(),
                                        logEntry.getDate(),
                                        logEntry.getMessage(),
                                        changed.isEmpty() ? null : changed));
                    });
            if (holder.isEmpty()) {
                throw new SvnAccessException("Revision not found: " + revision);
            }
            return holder.get(0);
        } catch (SVNException e) {
            throw svnAccessFailed("get_revision", repositoryId, e);
        }
    }

    @Override
    public DiffFileResult diffFile(
            String repositoryId,
            String path,
            Long fromRevision,
            Long toRevision,
            boolean ignoreWhitespace,
            DiffFileRequest limits)
            throws SvnAccessException {
        DiffFileRequest req = limits != null ? limits : DiffFileRequest.defaults();
        RepositoryEntry entry = resolver.require(repositoryId);
        String normalized = normalizePath(path);
        if (fromRevision == null || toRevision == null) {
            throw new SvnAccessException("from_revision and to_revision are required");
        }
        try (RepositorySession session = open(entry)) {
            SVNURL fileUrl = session.rootUrl().appendPath(normalized, false);
            ISVNAuthenticationManager auth = session.auth();
            SvnOperationFactory factory = new SvnOperationFactory();
            factory.setAuthenticationManager(auth);
            SvnDiff diff = factory.createDiff();
            diff.setSource(
                    SvnTarget.fromURL(fileUrl),
                    SVNRevision.create(fromRevision),
                    SVNRevision.create(toRevision));
            SVNDiffOptions opts = new SVNDiffOptions();
            if (ignoreWhitespace) {
                opts.setIgnoreAllWhitespace(true);
            }
            diff.setDiffOptions(opts);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            diff.setOutput(out);
            diff.run();
            factory.dispose();
            String fullText = out.toString(StandardCharsets.UTF_8);

            String spillPath = null;
            if (req.writeSpillFile()) {
                spillPath =
                        writeSpillFileIfConfigured(
                                repositoryId + "-file-" + fromRevision + "-" + toRevision,
                                fullText);
            }

            SvnMcpProperties.Defaults d = properties.getDefaults();
            int maxTotal =
                    clampOptional(
                            req.maxTotalLines(), d.getDiffMaxTotalLines(), 1, d.getDiffMaxTotalLines() * 4);
            int maxPerFile =
                    clampOptional(
                            null,
                            d.getDiffMaxLinesPerFile(),
                            1,
                            Math.max(50, d.getDiffMaxLinesPerFile() * 4));
            int maxFiles =
                    clampOptional(
                            null,
                            d.getDiffMaxFilesPerResponse(),
                            1,
                            Math.max(5, d.getDiffMaxFilesPerResponse() * 4));
            int lineOff = Math.max(0, req.lineOffset() != null ? req.lineOffset() : 0);
            int maxChars =
                    clampOptional(null, d.getDiffMaxCharsPerLine(), 1, Math.max(100, d.getDiffMaxCharsPerLine() * 4));
            long maxRespBytes =
                    clampLongOptional(null, d.getDiffMaxResponseBytes(), 1L, Math.max(1024L, d.getDiffMaxResponseBytes() * 4L));

            UnifiedDiffTruncation.Result tr =
                    UnifiedDiffTruncation.truncate(
                            fullText, maxTotal, maxPerFile, maxFiles, lineOff, maxChars);
            return applyByteCapToDiffFile(tr.text(), tr.meta(), spillPath, maxRespBytes);
        } catch (SVNException e) {
            throw svnAccessFailed("diff_file", repositoryId, e);
        }
    }

    private DiffRevisionResult byteCapDiffRevision(
            String path, long revision, long fromRev, String text, String spillPath) {
        long max = properties.getDefaults().getFileContentMaxBytes();
        boolean truncated = text.length() > max;
        String payload = truncated ? text.substring(0, (int) max) + "\n... [truncated]\n" : text;
        return new DiffRevisionResult(path, revision, fromRev, payload, truncated, null, spillPath, null);
    }

    private DiffRevisionResult applyByteCapToDiffRevision(
            String path,
            long revision,
            long fromRev,
            String text,
            DiffRevisionTruncation lineMeta,
            String spillPath,
            long maxResponseBytes) {
        long utf8Len = utf8ByteLength(text);
        boolean byteTrunc = utf8Len > maxResponseBytes;
        String payload =
                byteTrunc
                        ? utf8PrefixWithinByteLimit(text, maxResponseBytes)
                                + "\n... [truncated by bytes]\n"
                        : text;
        DiffRevisionTruncation meta = mergeBytesTruncated(lineMeta, byteTrunc);
        boolean truncated =
                byteTrunc
                        || lineMeta.lineTruncated()
                        || lineMeta.lineCharsTruncated();
        return new DiffRevisionResult(
                path, revision, fromRev, payload, truncated, null, spillPath, meta);
    }

    private DiffFileResult applyByteCapToDiffFile(
            String text, DiffRevisionTruncation lineMeta, String spillPath, long maxResponseBytes) {
        long utf8Len = utf8ByteLength(text);
        boolean byteTrunc = utf8Len > maxResponseBytes;
        String payload =
                byteTrunc
                        ? utf8PrefixWithinByteLimit(text, maxResponseBytes)
                                + "\n... [truncated by bytes]\n"
                        : text;
        DiffRevisionTruncation meta = mergeBytesTruncated(lineMeta, byteTrunc);
        boolean truncated =
                byteTrunc
                        || lineMeta.lineTruncated()
                        || lineMeta.lineCharsTruncated();
        return new DiffFileResult(payload, truncated, spillPath, meta);
    }

    private static DiffRevisionTruncation mergeBytesTruncated(
            DiffRevisionTruncation lineMeta, boolean bytesTruncated) {
        if (lineMeta == null) {
            return null;
        }
        if (!bytesTruncated) {
            return lineMeta;
        }
        return new DiffRevisionTruncation(
                lineMeta.lineTruncated(),
                lineMeta.linesEmitted(),
                lineMeta.linesInFullDiff(),
                lineMeta.fileSectionsIncluded(),
                lineMeta.fileSectionsOmitted(),
                lineMeta.lineOffsetApplied(),
                lineMeta.nextLineOffset(),
                lineMeta.hasMore(),
                lineMeta.lineCharsTruncated(),
                lineMeta.linesCharCapped(),
                true);
    }

    private static long utf8ByteLength(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        return s.getBytes(StandardCharsets.UTF_8).length;
    }

    /** Longest prefix of {@code text} whose UTF-8 encoding length is at most {@code maxBytes}. */
    private static String utf8PrefixWithinByteLimit(String text, long maxBytes) {
        if (text.isEmpty() || maxBytes <= 0) {
            return "";
        }
        if (utf8ByteLength(text) <= maxBytes) {
            return text;
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (utf8ByteLength(text.substring(0, mid)) <= maxBytes) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, low);
    }

    private String writeSpillFileIfConfigured(String repositoryId, long revision, String fullText) {
        return writeSpillFileIfConfigured(repositoryId + "-r" + revision, fullText);
    }

    private String writeSpillFileIfConfigured(String nameKey, String fullText) {
        String dir = properties.getDefaults().getDiffSpillDirectory();
        if (dir == null || dir.isBlank()) {
            if (fullText != null && !fullText.isEmpty()) {
                log.warn("write_spill_file requested but diff_spill_directory is not set; skipping spill");
            }
            return null;
        }
        try {
            Path d = Path.of(dir);
            Files.createDirectories(d);
            Path f = d.resolve("svn-mcp-diff-" + nameKey + "-" + System.nanoTime() + ".diff");
            Files.writeString(f, fullText, StandardCharsets.UTF_8);
            return f.toAbsolutePath().toString();
        } catch (Exception e) {
            log.warn("Failed to write diff spill file: {}", e.getMessage());
            return null;
        }
    }

    private static int clampOptional(Integer value, int def, int min, int max) {
        int v = value != null && value > 0 ? value : def;
        return Math.min(max, Math.max(min, v));
    }

    private static long clampLongOptional(Long value, long def, long min, long max) {
        long v = value != null && value > 0 ? value : def;
        return Math.min(max, Math.max(min, v));
    }

    @Override
    public List<BlameLine> blameFile(String repositoryId, String path, Long revision)
            throws SvnAccessException {
        RepositoryEntry entry = resolver.require(repositoryId);
        String normalized = normalizePath(path);
        try (RepositorySession session = open(entry)) {
            SVNRepository repo = session.repository();
            long endRev = revision != null ? revision : repo.getLatestRevision();
            SVNURL fileUrl = session.rootUrl().appendPath(normalized, false);
            ISVNAuthenticationManager auth = session.auth();
            SvnOperationFactory factory = new SvnOperationFactory();
            factory.setAuthenticationManager(auth);
            SvnAnnotate annotate = factory.createAnnotate();
            annotate.setSingleTarget(SvnTarget.fromURL(fileUrl));
            annotate.setStartRevision(SVNRevision.create(0));
            annotate.setEndRevision(SVNRevision.create(endRev));
            List<BlameLine> lines = new ArrayList<>();
            annotate.setReceiver(
                    (target, item) -> {
                        if (item.isLine()) {
                            int n = item.getLineNumber();
                            lines.add(
                                    new BlameLine(
                                            n > 0 ? n : lines.size() + 1,
                                            item.getRevision(),
                                            item.getAuthor(),
                                            item.getLine()));
                        }
                    });
            annotate.run();
            factory.dispose();
            return lines;
        } catch (SVNException e) {
            throw svnAccessFailed("blame_file", repositoryId, e);
        }
    }

    private List<PathEntry> listFlat(
            SVNRepository repo, String rootDir, long rev, int maxDepth, int maxEntries)
            throws SVNException {
        List<PathEntry> result = new ArrayList<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(rootDir);
        int depthBase = depthOf(rootDir);
        while (!queue.isEmpty() && result.size() < maxEntries) {
            String dir = queue.removeFirst();
            if (depthOf(dir) - depthBase > maxDepth) {
                continue;
            }
            SVNNodeKind kind = repo.checkPath(dir, rev);
            if (kind != SVNNodeKind.DIR) {
                continue;
            }
            repo.getDir(
                    dir,
                    rev,
                    null,
                    (SVNDirEntry entry) -> {
                        if (result.size() >= maxEntries) {
                            return;
                        }
                        String rel = joinRelative(dir, entry.getName());
                        if (entry.getKind() == SVNNodeKind.DIR) {
                            queue.add(rel);
                        }
                        if (entry.getKind() == SVNNodeKind.FILE) {
                            result.add(
                                    new PathEntry(
                                            entry.getName(),
                                            rel,
                                            kindName(entry.getKind()),
                                            entry.getSize()));
                        }
                    });
        }
        return result;
    }

    private static int depthOf(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                count++;
            }
        }
        return count;
    }

    private static String joinRelative(String dir, String name) {
        if (dir == null || dir.isEmpty()) {
            return name;
        }
        return dir + "/" + name;
    }

    private static Map<String, String> mapChangedPaths(
            Map<String, SVNLogEntryPath> changedPaths) {
        if (changedPaths == null || changedPaths.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, SVNLogEntryPath> e : changedPaths.entrySet()) {
            char t = e.getValue().getType();
            sorted.put(e.getKey(), String.valueOf(t));
        }
        return sorted;
    }

    private static String kindName(SVNNodeKind kind) {
        if (kind == null) {
            return "unknown";
        }
        if (kind == SVNNodeKind.FILE) {
            return "file";
        }
        if (kind == SVNNodeKind.DIR) {
            return "dir";
        }
        if (kind == SVNNodeKind.NONE) {
            return "none";
        }
        return kind.toString();
    }

    private RepositorySession open(RepositoryEntry entry) throws SVNException {
        SVNURL root = SVNURL.parseURIEncoded(entry.getRootUrl());
        SVNRepository repository = SVNRepositoryFactory.create(root);
        ISVNAuthenticationManager auth = buildAuth(entry);
        repository.setAuthenticationManager(auth);
        return new RepositorySession(repository, root, auth);
    }

    private ISVNAuthenticationManager buildAuth(RepositoryEntry entry) {
        String user = entry.getCredentials().getUsername();
        String pass = entry.getCredentials().getPassword();
        boolean hasUser = user != null && !user.isBlank();
        boolean hasPass = pass != null && !pass.isBlank();
        if (hasUser || hasPass) {
            return SVNWCUtil.createDefaultAuthenticationManager(
                    hasUser ? user : "", hasPass ? pass.toCharArray() : null);
        }
        return SVNWCUtil.createDefaultAuthenticationManager();
    }

    private long resolveRevision(SVNRepository repo, Long revision) throws SVNException {
        if (revision == null || revision < 0) {
            return repo.getLatestRevision();
        }
        return revision;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }
        String p = path.startsWith("/") ? path.substring(1) : path;
        while (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private static boolean isProbablyText(String mime, byte[] bytes) {
        if (mime != null) {
            String m = mime.toLowerCase();
            if (m.startsWith("text/")
                    || m.contains("xml")
                    || m.contains("json")
                    || m.contains("javascript")) {
                return true;
            }
            if (m.startsWith("image/")
                    || m.startsWith("video/")
                    || m.startsWith("audio/")
                    || m.startsWith("application/octet-stream")) {
                return false;
            }
        }
        return isValidUtf8(bytes);
    }

    private static boolean isValidUtf8(byte[] bytes) {
        CharsetDecoder decoder =
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(java.nio.ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private static String decodeUtf8Lenient(byte[] bytes) {
        CharsetDecoder decoder =
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static String svnMessage(SVNException e) {
        String m = e.getMessage();
        return m != null ? m : e.getClass().getSimpleName();
    }

    private SvnAccessException svnAccessFailed(String operation, String repositoryId, SVNException e) {
        log.error(
                "SVN operation failed [operation={}, repositoryId={}]: {}",
                operation,
                repositoryId,
                svnMessage(e),
                e);
        return new SvnAccessException(svnMessage(e), e);
    }

    private record RepositorySession(
            SVNRepository repository, SVNURL rootUrl, ISVNAuthenticationManager auth)
            implements AutoCloseable {
        @Override
        public void close() {
            repository.closeSession();
        }
    }
}
