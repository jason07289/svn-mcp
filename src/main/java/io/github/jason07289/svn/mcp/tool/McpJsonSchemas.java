package io.github.jason07289.svn.mcp.tool;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON Schema helpers for MCP {@link McpSchema.Tool} input schemas. */
public final class McpJsonSchemas {

    private McpJsonSchemas() {}

    public static McpSchema.JsonSchema emptyObject() {
        return new McpSchema.JsonSchema("object", Map.of(), List.of(), false, null, null);
    }

    public static McpSchema.JsonSchema object(
            Map<String, Map<String, Object>> properties, List<String> required) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.putAll(properties);
        return new McpSchema.JsonSchema("object", props, required, false, null, null);
    }

    public static Map<String, Object> stringProp(String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "string");
        m.put("description", description);
        return m;
    }

    public static Map<String, Object> integerProp(String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "integer");
        m.put("description", description);
        return m;
    }

    public static Map<String, Object> booleanProp(String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "boolean");
        m.put("description", description);
        return m;
    }

    public static Map<String, Object> stringEnumProp(String description, List<String> values) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "string");
        m.put("description", description);
        m.put("enum", values);
        return m;
    }
}
