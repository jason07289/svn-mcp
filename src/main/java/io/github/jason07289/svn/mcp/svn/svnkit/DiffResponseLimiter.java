package io.github.jason07289.svn.mcp.svn.svnkit;

import io.github.jason07289.svn.mcp.config.SvnMcpProperties;
import io.github.jason07289.svn.mcp.svn.api.DiffFileRequest;
import io.github.jason07289.svn.mcp.svn.api.DiffFileResult;
import io.github.jason07289.svn.mcp.svn.api.DiffRevisionRequest;
import io.github.jason07289.svn.mcp.svn.api.DiffRevisionResult;
import io.github.jason07289.svn.mcp.svn.api.DiffRevisionTruncation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DiffResponseLimiter {

    private static final Logger log = LoggerFactory.getLogger(DiffResponseLimiter.class);

    private final SvnMcpProperties.Defaults defaults;

    DiffResponseLimiter(SvnMcpProperties.Defaults defaults) {
        this.defaults = defaults;
    }

    String writeRevisionSpillFileIfConfigured(String repositoryId, long revision, String fullText) {
        return writeSpillFileIfConfigured(repositoryId + "-r" + revision, fullText);
    }

    String writeFileSpillFileIfConfigured(
            String repositoryId, Long fromRevision, Long toRevision, String fullText) {
        return writeSpillFileIfConfigured(
                repositoryId + "-file-" + fromRevision + "-" + toRevision, fullText);
    }

    DiffRevisionResult legacyByteCapRevision(
            String path, long revision, long fromRev, String text, String spillPath) {
        long max = defaults.getFileContentMaxBytes();
        boolean truncated = text.length() > max;
        String payload = truncated ? text.substring(0, (int) max) + "\n... [truncated]\n" : text;
        return new DiffRevisionResult(path, revision, fromRev, payload, truncated, null, spillPath, null);
    }

    DiffRevisionResult limitRevision(
            String path,
            long revision,
            long fromRev,
            String fullText,
            String spillPath,
            DiffRevisionRequest request) {
        int maxTotal =
                clampOptional(
                        request.maxTotalLines(),
                        defaults.getDiffMaxTotalLines(),
                        1,
                        Math.max(100, defaults.getDiffMaxTotalLines() * 4));
        int maxPerFile =
                clampOptional(
                        request.maxLinesPerFile(),
                        defaults.getDiffMaxLinesPerFile(),
                        1,
                        Math.max(50, defaults.getDiffMaxLinesPerFile() * 4));
        int maxFiles =
                clampOptional(
                        request.maxFiles(),
                        defaults.getDiffMaxFilesPerResponse(),
                        1,
                        Math.max(5, defaults.getDiffMaxFilesPerResponse() * 4));
        int lineOffset = Math.max(0, request.lineOffset() != null ? request.lineOffset() : 0);
        int maxChars =
                clampOptional(
                        request.maxCharsPerLine(),
                        defaults.getDiffMaxCharsPerLine(),
                        1,
                        Math.max(100, defaults.getDiffMaxCharsPerLine() * 4));
        long maxResponseBytes =
                clampLongOptional(
                        request.maxResponseBytes(),
                        defaults.getDiffMaxResponseBytes(),
                        1L,
                        Math.max(1024L, defaults.getDiffMaxResponseBytes() * 4L));

        UnifiedDiffTruncation.Result truncated =
                UnifiedDiffTruncation.truncate(
                        fullText, maxTotal, maxPerFile, maxFiles, lineOffset, maxChars);
        return applyByteCapToDiffRevision(
                path, revision, fromRev, truncated.text(), truncated.meta(), spillPath, maxResponseBytes);
    }

    DiffFileResult limitFile(String fullText, String spillPath, DiffFileRequest request) {
        int maxTotal =
                clampOptional(
                        request.maxTotalLines(),
                        defaults.getDiffMaxTotalLines(),
                        1,
                        defaults.getDiffMaxTotalLines() * 4);
        int maxPerFile =
                clampOptional(
                        null,
                        defaults.getDiffMaxLinesPerFile(),
                        1,
                        Math.max(50, defaults.getDiffMaxLinesPerFile() * 4));
        int maxFiles =
                clampOptional(
                        null,
                        defaults.getDiffMaxFilesPerResponse(),
                        1,
                        Math.max(5, defaults.getDiffMaxFilesPerResponse() * 4));
        int lineOffset = Math.max(0, request.lineOffset() != null ? request.lineOffset() : 0);
        int maxChars =
                clampOptional(
                        null,
                        defaults.getDiffMaxCharsPerLine(),
                        1,
                        Math.max(100, defaults.getDiffMaxCharsPerLine() * 4));
        long maxResponseBytes =
                clampLongOptional(
                        null,
                        defaults.getDiffMaxResponseBytes(),
                        1L,
                        Math.max(1024L, defaults.getDiffMaxResponseBytes() * 4L));

        UnifiedDiffTruncation.Result truncated =
                UnifiedDiffTruncation.truncate(
                        fullText, maxTotal, maxPerFile, maxFiles, lineOffset, maxChars);
        return applyByteCapToDiffFile(
                truncated.text(), truncated.meta(), spillPath, maxResponseBytes);
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
                byteTrunc || lineMeta.lineTruncated() || lineMeta.lineCharsTruncated();
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
                byteTrunc || lineMeta.lineTruncated() || lineMeta.lineCharsTruncated();
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

    private String writeSpillFileIfConfigured(String nameKey, String fullText) {
        String dir = defaults.getDiffSpillDirectory();
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
}
