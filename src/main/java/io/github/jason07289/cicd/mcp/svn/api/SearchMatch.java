package io.github.jason07289.cicd.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchMatch(
        String modulePath,
        String filePath,
        String fileName,
        String lastAuthor,
        long lastRevision,
        int matchCount,
        List<String> matchedLines) {}
