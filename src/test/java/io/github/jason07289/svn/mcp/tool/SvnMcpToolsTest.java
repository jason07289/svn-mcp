package io.github.jason07289.svn.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jason07289.svn.mcp.svn.api.*;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SvnMcpToolsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private RepositoryCatalog repositoryCatalog;
    @Mock private SvnRepositoryOperations svn;

    private SvnMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new SvnMcpTools(objectMapper, repositoryCatalog, svn);
    }

    @Test
    void listRepositories_returnsJsonWithEntries() throws Exception {
        when(repositoryCatalog.listRepositories())
                .thenReturn(
                        List.of(
                                RepositorySummary.fromConfig(
                                        "demo",
                                        "Demo",
                                        "https://svn.example.com/demo",
                                        "default")));

        McpSchema.CallToolResult result = tools.listRepositories(null, Map.of());
        assertThat(result.isError()).isFalse();
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertThat(json)
                .contains("demo")
                .contains("https://svn.example.com/demo")
                .contains("\"source\":\"config\"");
    }

    @Test
    void listPath_missingRepositoryId_returnsError() {
        McpSchema.CallToolResult result = tools.listPath(null, Map.of("path", ""));
        assertThat(result.isError()).isTrue();
    }

    @Test
    void listPath_success_serializesListPathResult() throws Exception {
        when(svn.listPath(
                        eq("r1"),
                        eq("trunk"),
                        isNull(),
                        isNull(),
                        eq("tree"),
                        isNull(),
                        isNull()))
                .thenReturn(
                        new ListPathResult(
                                10L,
                                "trunk",
                                "tree",
                                List.of(
                                        new PathEntry(
                                                "README", "trunk/README", "file", 3L))));

        McpSchema.CallToolResult result =
                tools.listPath(
                        null,
                        Map.of(
                                "repository_id", "r1",
                                "path", "trunk",
                                "view_mode", "tree"));
        assertThat(result.isError()).isFalse();
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertThat(json).contains("\"revision\":10").contains("README");
    }

    @Test
    void listPath_svnAccessException_returnsError() throws Exception {
        when(svn.listPath(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new SvnAccessException("boom"));

        McpSchema.CallToolResult result =
                tools.listPath(null, Map.of("repository_id", "r1", "path", ""));
        assertThat(result.isError()).isTrue();
        assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("boom");
    }

    @Test
    void getFile_missingPath_returnsError() {
        McpSchema.CallToolResult result =
                tools.getFile(null, Map.of("repository_id", "r1"));
        assertThat(result.isError()).isTrue();
    }

    @Test
    void getFile_success() throws Exception {
        when(svn.getFile(eq("r1"), eq("f.txt"), isNull(), isNull()))
                .thenReturn(
                        new GetFileResult(
                                1L,
                                "f.txt",
                                "text/plain",
                                true,
                                "UTF-8",
                                false,
                                "hi",
                                null));

        McpSchema.CallToolResult result =
                tools.getFile(
                        null,
                        Map.of(
                                "repository_id", "r1",
                                "path", "f.txt"));
        assertThat(result.isError()).isFalse();
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertThat(json).contains("contentText").contains("hi");
    }

    @Test
    void getLog_success() throws Exception {
        when(svn.getLog(
                        eq("r1"),
                        eq(""),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull()))
                .thenReturn(
                        new GetLogResult(
                                "",
                                2L,
                                0L,
                                List.of(
                                        new LogEntry(
                                                2L,
                                                "alice",
                                                new Date(1L),
                                                "msg",
                                                Map.of("/x", "M")))));

        McpSchema.CallToolResult result =
                tools.getLog(null, Map.of("repository_id", "r1"));
        assertThat(result.isError()).isFalse();
        assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("alice");
    }

    @Test
    void getRevision_success() throws Exception {
        when(svn.getRevision("r1", 5L))
                .thenReturn(
                        new GetRevisionResult(
                                5L, "bob", new Date(2L), "m", Map.of("/a", "A")));

        McpSchema.CallToolResult result =
                tools.getRevision(
                        null,
                        Map.of(
                                "repository_id", "r1",
                                "revision", 5));
        assertThat(result.isError()).isFalse();
        assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("\"revision\":5");
    }

    @Test
    void diffFile_success_wrapsUnifiedDiff() throws Exception {
        when(svn.diffFile(
                        eq("r1"), eq("p"), eq(1L), eq(2L), eq(true), eq(DiffFileRequest.defaults())))
                .thenReturn(DiffFileResult.legacy("diff output", false));

        McpSchema.CallToolResult result =
                tools.diffFile(
                        null,
                        Map.of(
                                "repository_id", "r1",
                                "path", "p",
                                "from_revision", 1,
                                "to_revision", 2,
                                "ignore_whitespace", true));
        assertThat(result.isError()).isFalse();
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertThat(json).contains("unified_diff").contains("diff output");
        verify(svn).diffFile("r1", "p", 1L, 2L, true, DiffFileRequest.defaults());
    }

    @Test
    void blameFile_success() throws Exception {
        when(svn.blameFile(eq("r1"), eq("p"), isNull()))
                .thenReturn(List.of(new BlameLine(1, 3L, "c", "line")));

        McpSchema.CallToolResult result =
                tools.blameFile(
                        null,
                        Map.of(
                                "repository_id", "r1",
                                "path", "p"));
        assertThat(result.isError()).isFalse();
        assertThat(((McpSchema.TextContent) result.content().get(0)).text()).contains("line");
    }
}
