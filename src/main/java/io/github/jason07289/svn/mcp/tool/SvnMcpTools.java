package io.github.jason07289.svn.mcp.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jason07289.svn.mcp.svn.api.RepositoryCatalog;
import io.github.jason07289.svn.mcp.svn.api.SvnAccessException;
import io.github.jason07289.svn.mcp.svn.api.SvnRepositoryOperations;
import io.modelcontextprotocol.spec.McpSchema;
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

    public McpSchema.CallToolResult listRepositories(Map<String, Object> args) {
        return invokeJson(() -> repositoryCatalog.listRepositories());
    }

    public McpSchema.CallToolResult listPath(Map<String, Object> args) {
        return invokeJson(
                () -> {
                    SvnToolRequests.ListPath r = SvnToolRequests.ListPath.from(args);
                    return svn.listPath(
                            r.repositoryId(),
                            r.path(),
                            r.revision(),
                            r.pegRevision(),
                            r.viewMode(),
                            r.flatMaxDepth(),
                            r.flatMaxEntries());
                });
    }

    public McpSchema.CallToolResult getFile(Map<String, Object> args) {
        return invokeJson(
                () -> {
                    SvnToolRequests.GetFile r = SvnToolRequests.GetFile.from(args);
                    return svn.getFile(r.repositoryId(), r.path(), r.revision(), r.pegRevision());
                });
    }

    public McpSchema.CallToolResult getLog(Map<String, Object> args) {
        return invokeJson(
                () -> {
                    SvnToolRequests.GetLog r = SvnToolRequests.GetLog.from(args);
                    return svn.getLog(
                            r.repositoryId(),
                            r.path(),
                            r.startRevision(),
                            r.endRevision(),
                            r.limit(),
                            r.stopOnCopy(),
                            r.startDate(),
                            r.endDate(),
                            r.author(),
                            r.authorMatch());
                });
    }

    public McpSchema.CallToolResult getRevision(Map<String, Object> args) {
        return invokeJson(
                () -> {
                    SvnToolRequests.GetRevision r = SvnToolRequests.GetRevision.from(args);
                    return svn.getRevision(r.repositoryId(), r.revision());
                });
    }

    public McpSchema.CallToolResult diffFile(Map<String, Object> args) {
        return invokeJson(
                () -> {
                    SvnToolRequests.DiffFile r = SvnToolRequests.DiffFile.from(args);
                    return svn.diffFile(
                            r.repositoryId(),
                            r.path(),
                            r.fromRevision(),
                            r.toRevision(),
                            r.ignoreWhitespace(),
                            r.limits());
                });
    }

    public McpSchema.CallToolResult resolveRevisionRange(Map<String, Object> args) {
        return invokeJson(
                () -> {
                    SvnToolRequests.ResolveRevisionRange r =
                            SvnToolRequests.ResolveRevisionRange.from(args);
                    return svn.resolveRevisionRange(r.repositoryId(), r.path(), r.start(), r.end());
                });
    }

    public McpSchema.CallToolResult diffRevision(Map<String, Object> args) {
        return invokeJson(
                () -> {
                    SvnToolRequests.DiffRevision r = SvnToolRequests.DiffRevision.from(args);
                    return svn.diffRevision(
                            r.repositoryId(), r.path(), r.revision(), r.request());
                });
    }

    public McpSchema.CallToolResult repositoryAuthorStats(Map<String, Object> args) {
        return invokeJson(
                () -> {
                    SvnToolRequests.RepositoryAuthorStats r =
                            SvnToolRequests.RepositoryAuthorStats.from(args);
                    return svn.repositoryAuthorStats(
                            r.repositoryId(),
                            r.pathPrefix(),
                            r.start(),
                            r.end(),
                            r.maxRevisionsToAnalyze());
                });
    }

    public McpSchema.CallToolResult blameFile(Map<String, Object> args) {
        return invokeJson(
                () -> {
                    SvnToolRequests.BlameFile r = SvnToolRequests.BlameFile.from(args);
                    return svn.blameFile(r.repositoryId(), r.path(), r.revision());
                });
    }

    public McpSchema.CallToolResult searchInPath(Map<String, Object> args) {
        return invokeJson(
                () -> {
                    SvnToolRequests.SearchInPath r = SvnToolRequests.SearchInPath.from(args);
                    return svn.searchInPath(
                            r.repositoryId(),
                            r.path(),
                            r.keyword(),
                            r.revision(),
                            r.fileExtensions(),
                            r.caseSensitive(),
                            r.maxFilesToScan(),
                            r.maxMatches());
                });
    }

    private McpSchema.CallToolResult invokeJson(ToolInvocation invocation) {
        try {
            return jsonOk(invocation.invoke());
        } catch (IllegalArgumentException e) {
            return textError(e.getMessage());
        } catch (SvnAccessException e) {
            return textError(e.getMessage());
        } catch (JsonProcessingException e) {
            return textError("Serialization error: " + e.getOriginalMessage());
        }
    }

    private McpSchema.CallToolResult jsonOk(Object value) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(value);
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(json)))
                .isError(false)
                .build();
    }

    private McpSchema.CallToolResult textError(String message) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(message)))
                .isError(true)
                .build();
    }

    @FunctionalInterface
    private interface ToolInvocation {
        Object invoke() throws SvnAccessException, JsonProcessingException;
    }
}
