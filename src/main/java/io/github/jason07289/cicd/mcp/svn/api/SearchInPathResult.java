package io.github.jason07289.cicd.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchInPathResult(
        long revision,
        String searchPath,
        String keyword,
        int filesScanned,
        int filesMatched,
        boolean scanLimitReached,
        List<SearchMatch> matches) {}
