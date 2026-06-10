package io.github.jason07289.svn.mcp.tool;

import io.github.jason07289.svn.mcp.svn.api.DiffFileRequest;
import io.github.jason07289.svn.mcp.svn.api.DiffRevisionRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

final class SvnToolRequests {

    private SvnToolRequests() {}

    record ListPath(
            String repositoryId,
            String path,
            Long revision,
            Long pegRevision,
            String viewMode,
            Integer flatMaxDepth,
            Integer flatMaxEntries) {

        static ListPath from(Map<String, Object> args) {
            return new ListPath(
                    ToolArguments.requireString(args, "repository_id"),
                    ToolArguments.optionalString(args, "path", ""),
                    ToolArguments.optionalLong(args, "revision"),
                    ToolArguments.optionalLong(args, "peg_revision"),
                    ToolArguments.optionalString(args, "view_mode", "tree"),
                    ToolArguments.optionalInt(args, "flat_max_depth"),
                    ToolArguments.optionalInt(args, "flat_max_entries"));
        }
    }

    record GetFile(String repositoryId, String path, Long revision, Long pegRevision) {

        static GetFile from(Map<String, Object> args) {
            return new GetFile(
                    ToolArguments.requireString(args, "repository_id"),
                    ToolArguments.requireString(args, "path"),
                    ToolArguments.optionalLong(args, "revision"),
                    ToolArguments.optionalLong(args, "peg_revision"));
        }
    }

    record GetLog(
            String repositoryId,
            String path,
            Long startRevision,
            Long endRevision,
            Integer limit,
            Boolean stopOnCopy,
            Date startDate,
            Date endDate,
            String author,
            String authorMatch) {

        static GetLog from(Map<String, Object> args) {
            return new GetLog(
                    ToolArguments.requireString(args, "repository_id"),
                    ToolArguments.optionalString(args, "path", ""),
                    ToolArguments.optionalLong(args, "start_revision"),
                    ToolArguments.optionalLong(args, "end_revision"),
                    ToolArguments.optionalInt(args, "limit"),
                    ToolArguments.optionalBoolean(args, "stop_on_copy"),
                    ToolArguments.optionalIsoDate(args, "start_date"),
                    ToolArguments.optionalIsoDate(args, "end_date"),
                    ToolArguments.optionalStringNullable(args, "author"),
                    ToolArguments.optionalStringNullable(args, "author_match"));
        }
    }

    record GetRevision(String repositoryId, long revision) {

        static GetRevision from(Map<String, Object> args) {
            return new GetRevision(
                    ToolArguments.requireString(args, "repository_id"),
                    ToolArguments.requireLong(args, "revision"));
        }
    }

    record DiffFile(
            String repositoryId,
            String path,
            Long fromRevision,
            Long toRevision,
            boolean ignoreWhitespace,
            DiffFileRequest limits) {

        static DiffFile from(Map<String, Object> args) {
            return new DiffFile(
                    ToolArguments.requireString(args, "repository_id"),
                    ToolArguments.requireString(args, "path"),
                    ToolArguments.optionalLong(args, "from_revision"),
                    ToolArguments.optionalLong(args, "to_revision"),
                    Boolean.TRUE.equals(ToolArguments.optionalBoolean(args, "ignore_whitespace")),
                    new DiffFileRequest(
                            ToolArguments.optionalInt(args, "max_total_lines"),
                            ToolArguments.optionalInt(args, "line_offset"),
                            Boolean.TRUE.equals(
                                    ToolArguments.optionalBoolean(args, "write_spill_file"))));
        }
    }

    record ResolveRevisionRange(String repositoryId, String path, Date start, Date end) {

        static ResolveRevisionRange from(Map<String, Object> args) {
            return new ResolveRevisionRange(
                    ToolArguments.requireString(args, "repository_id"),
                    ToolArguments.optionalString(args, "path", ""),
                    ToolArguments.parseIsoToDate(
                            ToolArguments.requireString(args, "start_inclusive")),
                    ToolArguments.parseIsoToDate(
                            ToolArguments.requireString(args, "end_inclusive")));
        }
    }

    record DiffRevision(String repositoryId, String path, long revision, DiffRevisionRequest request) {

        static DiffRevision from(Map<String, Object> args) {
            Boolean ignoreWs = ToolArguments.optionalBoolean(args, "ignore_whitespace");
            return new DiffRevision(
                    ToolArguments.requireString(args, "repository_id"),
                    ToolArguments.optionalString(args, "path", ""),
                    ToolArguments.requireLong(args, "revision"),
                    new DiffRevisionRequest(
                            ignoreWs != null && ignoreWs,
                            ToolArguments.optionalString(args, "output_mode", "unified"),
                            DiffRevisionRequest.LimitPolicy.MCP_DEFAULT,
                            ToolArguments.optionalInt(args, "max_total_lines"),
                            ToolArguments.optionalInt(args, "max_lines_per_file"),
                            ToolArguments.optionalInt(args, "max_files"),
                            ToolArguments.optionalInt(args, "line_offset"),
                            ToolArguments.optionalInt(args, "max_chars_per_line"),
                            ToolArguments.optionalLong(args, "max_response_bytes"),
                            Boolean.TRUE.equals(
                                    ToolArguments.optionalBoolean(args, "write_spill_file"))));
        }
    }

    record RepositoryAuthorStats(
            String repositoryId, String pathPrefix, Date start, Date end, Integer maxRevisionsToAnalyze) {

        static RepositoryAuthorStats from(Map<String, Object> args) {
            String calendarDate = ToolArguments.optionalStringNullable(args, "calendar_date");
            Date start;
            Date end;
            if (calendarDate != null) {
                String tz = ToolArguments.optionalStringNullable(args, "timezone");
                Date[] bounds =
                        ToolArguments.calendarDayInclusiveBounds(
                                calendarDate, tz != null && !tz.isBlank() ? tz : "UTC");
                start = bounds[0];
                end = bounds[1];
            } else {
                start = ToolArguments.optionalIsoDate(args, "start_inclusive");
                end = ToolArguments.optionalIsoDate(args, "end_inclusive");
                if (start == null || end == null) {
                    throw new IllegalArgumentException(
                            "Provide either calendar_date (optional timezone, default UTC) "
                                    + "or both start_inclusive and end_inclusive (ISO-8601).");
                }
            }
            return new RepositoryAuthorStats(
                    ToolArguments.requireString(args, "repository_id"),
                    ToolArguments.optionalString(args, "path_prefix", ""),
                    start,
                    end,
                    ToolArguments.optionalInt(args, "max_revisions_to_analyze"));
        }
    }

    record BlameFile(String repositoryId, String path, Long revision) {

        static BlameFile from(Map<String, Object> args) {
            return new BlameFile(
                    ToolArguments.requireString(args, "repository_id"),
                    ToolArguments.requireString(args, "path"),
                    ToolArguments.optionalLong(args, "revision"));
        }
    }

    record SearchInPath(
            String repositoryId,
            String path,
            String keyword,
            Long revision,
            List<String> fileExtensions,
            boolean caseSensitive,
            int maxFilesToScan,
            int maxMatches) {

        static SearchInPath from(Map<String, Object> args) {
            String extStr = ToolArguments.optionalStringNullable(args, "file_extensions");
            Integer maxScan = ToolArguments.optionalInt(args, "max_files_to_scan");
            Integer maxMatches = ToolArguments.optionalInt(args, "max_matches");
            return new SearchInPath(
                    ToolArguments.requireString(args, "repository_id"),
                    ToolArguments.requireString(args, "path"),
                    ToolArguments.requireString(args, "keyword"),
                    ToolArguments.optionalLong(args, "revision"),
                    extStr != null && !extStr.isBlank()
                            ? List.of(extStr.split("[,\\s]+"))
                            : List.of(),
                    Boolean.TRUE.equals(ToolArguments.optionalBoolean(args, "case_sensitive")),
                    maxScan != null && maxScan > 0 ? Math.min(maxScan, 500) : 200,
                    maxMatches != null && maxMatches > 0 ? Math.min(maxMatches, 200) : 50);
        }
    }
}
