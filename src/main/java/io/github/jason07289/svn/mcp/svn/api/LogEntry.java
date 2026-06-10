package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LogEntry(
        long revision,
        String author,
        Date date,
        String message,
        Map<String, String> changedPaths) {}
