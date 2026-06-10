package io.github.jason07289.svn.mcp.transport;

import io.github.jason07289.svn.mcp.tool.SvnMcpToolDefinitions;
import io.github.jason07289.svn.mcp.tool.SvnMcpTools;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class McpServerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfiguration.class);

    private final McpSyncServer mcpSyncServer;

    public McpServerConfiguration(@org.springframework.context.annotation.Lazy McpSyncServer mcpSyncServer) {
        this.mcpSyncServer = mcpSyncServer;
    }

    @EventListener(ApplicationReadyEvent.class)
    void logActivatedTools() {
        List<McpSchema.Tool> tools = mcpSyncServer.listTools();
        StringBuilder sb = new StringBuilder();
        sb.append("Activated MCP tools (").append(tools.size()).append("):");
        int idx = 1;
        for (McpSchema.Tool tool : tools) {
            sb.append(String.format("%n  %2d. %s", idx++, tool.name()));
        }
        log.info(sb.toString());
    }

    @Bean
    WebMvcStreamableServerTransportProvider mcpStreamableTransport() {
        return WebMvcStreamableServerTransportProvider.builder().mcpEndpoint("/mcp").build();
    }

    @Bean
    McpSyncServer mcpSyncServer(
            WebMvcStreamableServerTransportProvider transport, SvnMcpTools tools) {
        var server =
                McpServer.sync(transport)
                        .serverInfo("jason07289-svn-mcp", "0.1.0")
                        .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build());
        for (var definition : SvnMcpToolDefinitions.all()) {
            server =
                    server.toolCall(
                            definition.toSchemaTool(),
                            (exchange, request) ->
                                    callTool(
                                            tools,
                                            definition.name(),
                                            request.arguments() != null
                                                    ? request.arguments()
                                                    : Map.of()));
        }
        return server.build();
    }

    private static McpSchema.CallToolResult callTool(
            SvnMcpTools tools, String toolName, Map<String, Object> args) {
        return switch (toolName) {
            case "list_repositories" -> tools.listRepositories(args);
            case "list_path" -> tools.listPath(args);
            case "get_file" -> tools.getFile(args);
            case "get_log" -> tools.getLog(args);
            case "get_revision" -> tools.getRevision(args);
            case "diff_file" -> tools.diffFile(args);
            case "blame_file" -> tools.blameFile(args);
            case "resolve_revision_range" -> tools.resolveRevisionRange(args);
            case "diff_revision" -> tools.diffRevision(args);
            case "repository_author_stats" -> tools.repositoryAuthorStats(args);
            case "search_in_path" -> tools.searchInPath(args);
            default -> throw new IllegalArgumentException("Unknown MCP tool: " + toolName);
        };
    }

    @Bean
    @DependsOn("mcpSyncServer")
    RouterFunction<ServerResponse> mcpRouterFunction(
            WebMvcStreamableServerTransportProvider transport) {
        return transport.getRouterFunction();
    }
}
