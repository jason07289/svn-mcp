package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListPathResult(
        long revision,
        String path,
        String viewMode,
        List<PathEntry> entries) {}
