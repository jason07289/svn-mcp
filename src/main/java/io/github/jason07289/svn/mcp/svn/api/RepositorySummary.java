package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepositorySummary(
        String id,
        String name,
        String rootUrl,
        String group,
        String source,
        String discoveredUnder) {

    /** Configured repository from application.yml. */
    public static RepositorySummary fromConfig(
            String id, String name, String rootUrl, String group) {
        return new RepositorySummary(id, name, rootUrl, group, "config", null);
    }
}
