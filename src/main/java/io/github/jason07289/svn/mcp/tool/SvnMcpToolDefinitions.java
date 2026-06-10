package io.github.jason07289.svn.mcp.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SvnMcpToolDefinitions {

    private SvnMcpToolDefinitions() {}

    public static List<McpToolDefinition> all() {
        return List.of(
                listRepositories(),
                listPath(),
                getFile(),
                getLog(),
                getRevision(),
                diffFile(),
                blameFile(),
                resolveRevisionRange(),
                diffRevision(),
                repositoryAuthorStats(),
                searchInPath());
    }

    private static McpToolDefinition listRepositories() {
        return new McpToolDefinition(
                "list_repositories",
                "Lists repositories defined in application.yml (id, name, root URL, group; source=config). "
                        + "Credentials are not returned.",
                McpJsonSchemas.emptyObject());
    }

    private static McpToolDefinition listPath() {
        return new McpToolDefinition(
                "list_path",
                "Lists a directory at a revision (tree) or lists files recursively under a path (flat) using SVNKit.",
                McpJsonSchemas.object(
                        Map.of(
                                "repository_id",
                                McpJsonSchemas.stringProp("Repository id from configuration."),
                                "path",
                                McpJsonSchemas.stringProp(
                                        "Directory path relative to repository root; use empty string for root."),
                                "revision",
                                McpJsonSchemas.integerProp("Optional revision number; omit for HEAD."),
                                "peg_revision",
                                McpJsonSchemas.integerProp(
                                        "Optional peg revision (reserved; pass revision for now)."),
                                "view_mode",
                                McpJsonSchemas.stringEnumProp(
                                        "tree: one directory level; flat: files under path with limits.",
                                        List.of("tree", "flat")),
                                "flat_max_depth",
                                McpJsonSchemas.integerProp("For view_mode=flat: max directory depth."),
                                "flat_max_entries",
                                McpJsonSchemas.integerProp(
                                        "For view_mode=flat: max number of file entries.")),
                        List.of("repository_id")));
    }

    private static McpToolDefinition getFile() {
        return new McpToolDefinition(
                "get_file",
                "Reads a file at a revision. Returns text or Base64 with mime metadata; truncates to configured max bytes.",
                McpJsonSchemas.object(
                        Map.of(
                                "repository_id",
                                McpJsonSchemas.stringProp("Repository id from configuration."),
                                "path",
                                McpJsonSchemas.stringProp("File path relative to repository root."),
                                "revision",
                                McpJsonSchemas.integerProp("Optional revision number; omit for HEAD."),
                                "peg_revision",
                                McpJsonSchemas.integerProp("Optional peg revision (reserved).")),
                        List.of("repository_id", "path")));
    }

    private static McpToolDefinition getLog() {
        return new McpToolDefinition(
                "get_log",
                "Commits affecting a path (newest-first within revision range), with changed paths when available. "
                        + "When start_date/end_date are set (ISO-8601), revision bounds are derived via getDatedRevision and entries are filtered by commit date. "
                        + "Optional author + author_match (exact|contains) filter server-side.",
                McpJsonSchemas.object(getLogProperties(), List.of("repository_id")));
    }

    private static McpToolDefinition getRevision() {
        return new McpToolDefinition(
                "get_revision",
                "Returns metadata and changed paths for a single revision. "
                        + "Prefer this (or get_log with a narrow path) before diff_revision on large trees "
                        + "so you can scope diffs or use diff_revision output_mode=paths_only.",
                McpJsonSchemas.object(
                        Map.of(
                                "repository_id",
                                McpJsonSchemas.stringProp("Repository id from configuration."),
                                "revision",
                                McpJsonSchemas.integerProp("Revision number to inspect.")),
                        List.of("repository_id", "revision")));
    }

    private static McpToolDefinition diffFile() {
        return new McpToolDefinition(
                "diff_file",
                "Unified diff of a file between two revisions (SVNKit diff). "
                        + "Responses are capped by default (total lines / bytes) to avoid MCP token overflow; "
                        + "use max_total_lines and line_offset to page. "
                        + "When write_spill_file is true and diff_spill_directory is configured, writes the full diff to disk and returns spill_file_path.",
                McpJsonSchemas.object(
                        Map.of(
                                "repository_id",
                                McpJsonSchemas.stringProp("Repository id from configuration."),
                                "path",
                                McpJsonSchemas.stringProp("File path relative to repository root."),
                                "from_revision",
                                McpJsonSchemas.integerProp("Older revision (smaller number)."),
                                "to_revision",
                                McpJsonSchemas.integerProp("Newer revision (larger number)."),
                                "ignore_whitespace",
                                McpJsonSchemas.booleanProp("If true, ignore whitespace in diff."),
                                "max_total_lines",
                                McpJsonSchemas.integerProp(
                                        "Optional cap on emitted diff lines (server clamps to a safe maximum)."),
                                "line_offset",
                                McpJsonSchemas.integerProp(
                                        "Skip this many lines from the capped diff (pagination; combine with truncation.next_line_offset from a prior call)."),
                                "write_spill_file",
                                McpJsonSchemas.booleanProp(
                                        "If true, write full diff to diff_spill_directory before truncation and return spill_file_path.")),
                        List.of("repository_id", "path", "from_revision", "to_revision")));
    }

    private static McpToolDefinition blameFile() {
        return new McpToolDefinition(
                "blame_file",
                "Line-by-line blame (revision and author per line) for a file.",
                McpJsonSchemas.object(
                        Map.of(
                                "repository_id",
                                McpJsonSchemas.stringProp("Repository id from configuration."),
                                "path",
                                McpJsonSchemas.stringProp("File path relative to repository root."),
                                "revision",
                                McpJsonSchemas.integerProp(
                                        "Optional end revision for annotate; omit for HEAD.")),
                        List.of("repository_id", "path")));
    }

    private static McpToolDefinition resolveRevisionRange() {
        return new McpToolDefinition(
                "resolve_revision_range",
                "Maps an inclusive time range to SVN revisions using getDatedRevision (approximate). "
                        + "Use LogEntry dates for strict boundaries.",
                McpJsonSchemas.object(
                        Map.of(
                                "repository_id",
                                McpJsonSchemas.stringProp("Repository id from configuration."),
                                "path",
                                McpJsonSchemas.stringProp(
                                        "Optional path under the repo root; empty for root."),
                                "start_inclusive",
                                McpJsonSchemas.stringProp("Inclusive start instant (ISO-8601)."),
                                "end_inclusive",
                                McpJsonSchemas.stringProp("Inclusive end instant (ISO-8601).")),
                        List.of("repository_id", "start_inclusive", "end_inclusive")));
    }

    private static McpToolDefinition diffRevision() {
        return new McpToolDefinition(
                "diff_revision",
                "Unified diff for a single revision (svn diff -c REV) under an optional path prefix. "
                        + "For large trees, prefer a narrow path, or use output_mode=paths_only (changed paths only, no diff body) after scoping with get_revision/get_log. "
                        + "Default unified output applies per-line character caps, line and file-section caps, and a UTF-8 byte cap on the diff body (see server defaults) to reduce MCP token overflow. "
                        + "Pagination: pass line_offset; read truncation.next_line_offset and truncation.has_more. "
                        + "Optional write_spill_file writes the full diff to diff_spill_directory before truncation.",
                McpJsonSchemas.object(
                        Map.ofEntries(
                                Map.entry(
                                        "repository_id",
                                        McpJsonSchemas.stringProp("Repository id from configuration.")),
                                Map.entry(
                                        "path",
                                        McpJsonSchemas.stringProp(
                                                "Optional path prefix under repo root; empty for entire tree — avoid on large repos unless necessary.")),
                                Map.entry(
                                        "revision",
                                        McpJsonSchemas.integerProp(
                                                "Revision to diff (compared to revision-1).")),
                                Map.entry(
                                        "ignore_whitespace",
                                        McpJsonSchemas.booleanProp("If true, ignore whitespace in diff.")),
                                Map.entry(
                                        "output_mode",
                                        McpJsonSchemas.stringEnumProp(
                                                "unified (default) or paths_only (changed paths from revision metadata, no diff body).",
                                                List.of("unified", "paths_only"))),
                                Map.entry(
                                        "max_total_lines",
                                        McpJsonSchemas.integerProp(
                                                "Optional max lines in unified diff after caps (server clamps).")),
                                Map.entry(
                                        "max_lines_per_file",
                                        McpJsonSchemas.integerProp(
                                                "Optional max lines per Index (file) section (server clamps).")),
                                Map.entry(
                                        "max_files",
                                        McpJsonSchemas.integerProp(
                                                "Optional max file sections (Index headers) to include (server clamps).")),
                                Map.entry(
                                        "line_offset",
                                        McpJsonSchemas.integerProp(
                                                "Skip this many lines from the capped unified diff (pagination).")),
                                Map.entry(
                                        "max_chars_per_line",
                                        McpJsonSchemas.integerProp(
                                                "Optional max UTF-16 code units per diff line before ellipsizing (server clamps).")),
                                Map.entry(
                                        "max_response_bytes",
                                        McpJsonSchemas.integerProp(
                                                "Optional max UTF-8 bytes for unified_diff after caps (server clamps).")),
                                Map.entry(
                                        "write_spill_file",
                                        McpJsonSchemas.booleanProp(
                                                "If true, write full diff to diff_spill_directory before truncation; response may include spill_file_path."))),
                        List.of("repository_id", "revision")));
    }

    private static McpToolDefinition repositoryAuthorStats() {
        return new McpToolDefinition(
                "repository_author_stats",
                "Per-author commit counts and diff line totals for a time window. "
                        + "by_author is sorted by diff magnitude (lines added + removed) descending. "
                        + "Use calendar_date + timezone for a local calendar day, or start_inclusive + end_inclusive (ISO-8601).",
                McpJsonSchemas.object(
                        Map.of(
                                "repository_id",
                                McpJsonSchemas.stringProp("Repository id from configuration."),
                                "path_prefix",
                                McpJsonSchemas.stringProp(
                                        "Optional path prefix; empty for whole repository."),
                                "calendar_date",
                                McpJsonSchemas.stringProp(
                                        "Optional local calendar day (YYYY-MM-DD). Use with timezone (default UTC)."),
                                "timezone",
                                McpJsonSchemas.stringProp(
                                        "IANA zone id when using calendar_date (e.g. Asia/Seoul)."),
                                "start_inclusive",
                                McpJsonSchemas.stringProp(
                                        "Inclusive range start (ISO-8601). Required if calendar_date is omitted."),
                                "end_inclusive",
                                McpJsonSchemas.stringProp(
                                        "Inclusive range end (ISO-8601). Required if calendar_date is omitted."),
                                "max_revisions_to_analyze",
                                McpJsonSchemas.integerProp(
                                        "Cap revisions for diff stats (bounded by server default).")),
                        List.of("repository_id")));
    }

    private static McpToolDefinition searchInPath() {
        return new McpToolDefinition(
                "search_in_path",
                "Searches file contents under a path for a keyword (e.g. a table name) "
                        + "and returns matching files with their last author. "
                        + "All file reads share a single SVN session to avoid N+1 connection overhead. "
                        + "Result format per match: modulePath | filePath | lastAuthor. "
                        + "Use file_extensions to narrow the scan (default: java).",
                McpJsonSchemas.object(
                        Map.ofEntries(
                                Map.entry(
                                        "repository_id",
                                        McpJsonSchemas.stringProp("Repository id from configuration.")),
                                Map.entry(
                                        "path",
                                        McpJsonSchemas.stringProp(
                                                "Base path to search under (e.g. trunk/com/example/service).")),
                                Map.entry(
                                        "keyword",
                                        McpJsonSchemas.stringProp(
                                                "Keyword to search for in file contents (e.g. table name, SQL fragment).")),
                                Map.entry(
                                        "revision",
                                        McpJsonSchemas.integerProp(
                                                "Optional revision number; omit for HEAD.")),
                                Map.entry(
                                        "file_extensions",
                                        McpJsonSchemas.stringProp(
                                                "Comma-separated file extensions to include (default: java). Examples: java,xml or java,xml,properties.")),
                                Map.entry(
                                        "case_sensitive",
                                        McpJsonSchemas.booleanProp(
                                                "If true, keyword matching is case-sensitive (default: false).")),
                                Map.entry(
                                        "max_files_to_scan",
                                        McpJsonSchemas.integerProp(
                                                "Max number of matching-extension files to read (capped at 500, default 200).")),
                                Map.entry(
                                        "max_matches",
                                        McpJsonSchemas.integerProp(
                                                "Stop collecting results after this many matches (capped at 200, default 50)."))),
                        List.of("repository_id", "path", "keyword")));
    }

    private static Map<String, Map<String, Object>> getLogProperties() {
        Map<String, Map<String, Object>> m = new LinkedHashMap<>();
        m.put("repository_id", McpJsonSchemas.stringProp("Repository id from configuration."));
        m.put("path", McpJsonSchemas.stringProp("Path relative to repository root; empty for root."));
        m.put(
                "start_revision",
                McpJsonSchemas.integerProp(
                        "Optional high revision for log traversal (defaults to HEAD). Ignored when start_date/end_date are set."));
        m.put(
                "end_revision",
                McpJsonSchemas.integerProp(
                        "Optional low revision (defaults to 0). Ignored when start_date/end_date are set."));
        m.put(
                "limit",
                McpJsonSchemas.integerProp("Max number of log entries (capped by server defaults)."));
        m.put("stop_on_copy", McpJsonSchemas.booleanProp("If true, stop when a copy is encountered."));
        m.put(
                "start_date",
                McpJsonSchemas.stringProp(
                        "Optional inclusive start (ISO-8601). When set with end_date, bounds revisions and filters by commit time."));
        m.put(
                "end_date",
                McpJsonSchemas.stringProp(
                        "Optional inclusive end (ISO-8601). When set with start_date, bounds revisions and filters by commit time."));
        m.put("author", McpJsonSchemas.stringProp("Optional filter by svn:author."));
        m.put(
                "author_match",
                McpJsonSchemas.stringEnumProp(
                        "How author matches: exact (default) or contains.",
                        List.of("exact", "contains")));
        return m;
    }
}
