package io.github.jason07289.cicd.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Controls {@link SvnRepositoryOperations#diffRevision} output size and shape for MCP clients.
 *
 * @param ignoreWhitespace svn diff option
 * @param outputMode {@code unified} (default) or {@code paths_only} (changed paths only, no diff body)
 * @param limitPolicy {@link LimitPolicy#MCP_DEFAULT} applies configured line/file caps; {@link LimitPolicy#NONE}
 *     keeps legacy byte cap only (used for internal stats aggregation)
 * @param maxTotalLines optional override of server default (clamped server-side)
 * @param maxLinesPerFile optional override per file section
 * @param maxFiles optional override of max file sections
 * @param lineOffset skip this many lines from the capped unified output (pagination)
 * @param maxCharsPerLine optional max UTF-16 code units per line before ellipsizing (server clamps)
 * @param maxResponseBytes optional max UTF-8 bytes for unified_diff body after caps (server clamps)
 * @param writeSpillFile when true and {@code diff_spill_directory} is configured, writes full diff before
 *     line truncation to a file and returns its path in {@link DiffRevisionResult#spillFilePath()}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiffRevisionRequest(
        boolean ignoreWhitespace,
        String outputMode,
        LimitPolicy limitPolicy,
        Integer maxTotalLines,
        Integer maxLinesPerFile,
        Integer maxFiles,
        Integer lineOffset,
        Integer maxCharsPerLine,
        Long maxResponseBytes,
        boolean writeSpillFile) {

    public enum LimitPolicy {
        MCP_DEFAULT,
        NONE
    }

    public DiffRevisionRequest {
        if (outputMode == null || outputMode.isBlank()) {
            outputMode = "unified";
        }
        if (limitPolicy == null) {
            limitPolicy = LimitPolicy.MCP_DEFAULT;
        }
    }

    /** Back-compat: unified diff, MCP line limits, no spill. */
    public static DiffRevisionRequest legacy(boolean ignoreWhitespace) {
        return new DiffRevisionRequest(
                ignoreWhitespace,
                "unified",
                LimitPolicy.MCP_DEFAULT,
                null,
                null,
                null,
                null,
                null,
                null,
                false);
    }

    /** Full unified diff for internal line stats; only {@code file_content_max_bytes} applies. */
    public static DiffRevisionRequest internalStats(boolean ignoreWhitespace) {
        return new DiffRevisionRequest(
                ignoreWhitespace,
                "unified",
                LimitPolicy.NONE,
                null,
                null,
                null,
                null,
                null,
                null,
                false);
    }
}
