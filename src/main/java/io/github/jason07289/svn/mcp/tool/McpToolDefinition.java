package io.github.jason07289.svn.mcp.tool;

import io.modelcontextprotocol.spec.McpSchema;

public record McpToolDefinition(String name, String description, McpSchema.JsonSchema inputSchema) {

    public McpSchema.Tool toSchemaTool() {
        return McpSchema.Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }
}
