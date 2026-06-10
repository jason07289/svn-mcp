package io.github.jason07289.svn.mcp.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jason07289.svn.mcp.svn.api.DiffFileRequest;
import io.github.jason07289.svn.mcp.svn.api.DiffFileResult;
import io.github.jason07289.svn.mcp.svn.api.DiffRevisionRequest;
import io.github.jason07289.svn.mcp.svn.api.RepositoryCatalog;
import io.github.jason07289.svn.mcp.svn.api.SvnAccessException;
import io.github.jason07289.svn.mcp.svn.api.SvnRepositoryOperations;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SvnMcpTools {

    private final ObjectMapper objectMapper;
    private final RepositoryCatalog repositoryCatalog;
    private final SvnRepositoryOperations svn;

    public SvnMcpTools(
            ObjectMapper objectMapper,
            RepositoryCatalog repositoryCatalog,
            SvnRepositoryOperations svn) {
        this.objectMapper = objectMapper;
        this.repositoryCatalog = repositoryCatalog;
        this.svn = svn;
    }

    public McpSchema.CallToolResult listRepositories(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            return jsonOk(repositoryCatalog.listRepositories());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    public McpSchema.CallToolResult listPath(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            String repoId = ToolArguments.requireString(args, "repository_id");
            String path = ToolArguments.optionalString(args, "path", "");
            Long revision = ToolArguments.optionalLong(args, "revision");
            Long pegRevision = ToolArguments.optionalLong(args, "peg_revision");
            String viewMode = ToolArguments.optionalString(args, "view_mode", "tree");
            Integer flatMaxDepth = ToolArguments.optionalInt(args, "flat_max_depth");
            Integer flatMaxEntries = ToolArguments.optionalInt(args, "flat_max_entries");
            return jsonOk(
                    svn.listPath(
                            repoId, path, revision, pegRevision, viewMode, flatMaxDepth, flatMaxEntries));
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    public McpSchema.CallToolResult getFile(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            String repoId = ToolArguments.requireString(args, "repository_id");
            String path = ToolArguments.requireString(args, "path");
            Long revision = ToolArguments.optionalLong(args, "revision");
            Long pegRevision = ToolArguments.optionalLong(args, "peg_revision");
            return jsonOk(svn.getFile(repoId, path, revision, pegRevision));
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    public McpSchema.CallToolResult getLog(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            String repoId = ToolArguments.requireString(args, "repository_id");
            String path = ToolArguments.optionalString(args, "path", "");
            Long start = ToolArguments.optionalLong(args, "start_revision");
            Long end = ToolArguments.optionalLong(args, "end_revision");
            Integer limit = ToolArguments.optionalInt(args, "limit");
            Boolean stopOnCopy = ToolArguments.optionalBoolean(args, "stop_on_copy");
            Date startDate = ToolArguments.optionalIsoDate(args, "start_date");
            Date endDate = ToolArguments.optionalIsoDate(args, "end_date");
            String author = ToolArguments.optionalStringNullable(args, "author");
            String authorMatch = ToolArguments.optionalStringNullable(args, "author_match");
            return jsonOk(
                    svn.getLog(
                            repoId,
                            path,
                            start,
                            end,
                            limit,
                            stopOnCopy,
                            startDate,
                            endDate,
                            author,
                            authorMatch));
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    public McpSchema.CallToolResult getRevision(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            String repoId = ToolArguments.requireString(args, "repository_id");
            long revision = ToolArguments.requireLong(args, "revision");
            return jsonOk(svn.getRevision(repoId, revision));
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    public McpSchema.CallToolResult diffFile(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            String repoId = ToolArguments.requireString(args, "repository_id");
            String path = ToolArguments.requireString(args, "path");
            Long from = ToolArguments.optionalLong(args, "from_revision");
            Long to = ToolArguments.optionalLong(args, "to_revision");
            Boolean ignoreWs = ToolArguments.optionalBoolean(args, "ignore_whitespace");
            DiffFileRequest limits =
                    new DiffFileRequest(
                            ToolArguments.optionalInt(args, "max_total_lines"),
                            ToolArguments.optionalInt(args, "line_offset"),
                            Boolean.TRUE.equals(
                                    ToolArguments.optionalBoolean(args, "write_spill_file")));
            DiffFileResult diff =
                    svn.diffFile(
                            repoId,
                            path,
                            from,
                            to,
                            ignoreWs != null && ignoreWs,
                            limits);
            return jsonOk(diff);
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    public McpSchema.CallToolResult resolveRevisionRange(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            String repoId = ToolArguments.requireString(args, "repository_id");
            String path = ToolArguments.optionalString(args, "path", "");
            Date start =
                    ToolArguments.parseIsoToDate(
                            ToolArguments.requireString(args, "start_inclusive"));
            Date end =
                    ToolArguments.parseIsoToDate(
                            ToolArguments.requireString(args, "end_inclusive"));
            return jsonOk(svn.resolveRevisionRange(repoId, path, start, end));
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    public McpSchema.CallToolResult diffRevision(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            String repoId = ToolArguments.requireString(args, "repository_id");
            String path = ToolArguments.optionalString(args, "path", "");
            long revision = ToolArguments.requireLong(args, "revision");
            Boolean ignoreWs = ToolArguments.optionalBoolean(args, "ignore_whitespace");
            DiffRevisionRequest request =
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
                                    ToolArguments.optionalBoolean(args, "write_spill_file")));
            return jsonOk(svn.diffRevision(repoId, path, revision, request));
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    public McpSchema.CallToolResult repositoryAuthorStats(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            String repoId = ToolArguments.requireString(args, "repository_id");
            String pathPrefix = ToolArguments.optionalString(args, "path_prefix", "");
            String calendarDate = ToolArguments.optionalStringNullable(args, "calendar_date");
            Date start;
            Date end;
            if (calendarDate != null) {
                String tz =
                        ToolArguments.optionalStringNullable(args, "timezone");
                String zoneId =
                        tz != null && !tz.isBlank() ? tz : "UTC";
                Date[] bounds =
                        ToolArguments.calendarDayInclusiveBounds(calendarDate, zoneId);
                start = bounds[0];
                end = bounds[1];
            } else {
                Date startArg = ToolArguments.optionalIsoDate(args, "start_inclusive");
                Date endArg = ToolArguments.optionalIsoDate(args, "end_inclusive");
                if (startArg == null || endArg == null) {
                    return textError(
                            "Provide either calendar_date (optional timezone, default UTC) "
                                    + "or both start_inclusive and end_inclusive (ISO-8601).");
                }
                start = startArg;
                end = endArg;
            }
            Integer maxRev = ToolArguments.optionalInt(args, "max_revisions_to_analyze");
            return jsonOk(svn.repositoryAuthorStats(repoId, pathPrefix, start, end, maxRev));
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    public McpSchema.CallToolResult blameFile(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            String repoId = ToolArguments.requireString(args, "repository_id");
            String path = ToolArguments.requireString(args, "path");
            Long revision = ToolArguments.optionalLong(args, "revision");
            return jsonOk(svn.blameFile(repoId, path, revision));
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    public McpSchema.CallToolResult searchInPath(
            McpSyncServerExchange exchange, Map<String, Object> args) {
        try {
            String repoId = ToolArguments.requireString(args, "repository_id");
            String path = ToolArguments.requireString(args, "path");
            String keyword = ToolArguments.requireString(args, "keyword");
            Long revision = ToolArguments.optionalLong(args, "revision");
            String extStr = ToolArguments.optionalStringNullable(args, "file_extensions");
            List<String> exts =
                    extStr != null && !extStr.isBlank()
                            ? List.of(extStr.split("[,\\s]+"))
                            : List.of();
            Boolean cs = ToolArguments.optionalBoolean(args, "case_sensitive");
            Integer maxScan = ToolArguments.optionalInt(args, "max_files_to_scan");
            Integer maxMatches = ToolArguments.optionalInt(args, "max_matches");
            int scanLimit = maxScan != null && maxScan > 0 ? Math.min(maxScan, 500) : 200;
            int matchLimit = maxMatches != null && maxMatches > 0 ? Math.min(maxMatches, 200) : 50;
            return jsonOk(
                    svn.searchInPath(
                            repoId,
                            path,
                            keyword,
                            revision,
                            exts,
                            Boolean.TRUE.equals(cs),
                            scanLimit,
                            matchLimit));
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return jsonError("Serialization error: " + e.getOriginalMessage());
        }
    }

    private McpSchema.CallToolResult jsonOk(Object value) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(value);
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(json)))
                .isError(false)
                .build();
    }

    private McpSchema.CallToolResult jsonError(String message) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(message)))
                .isError(true)
                .build();
    }

    private McpSchema.CallToolResult textError(String message) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(message)))
                .isError(true)
                .build();
    }
}
