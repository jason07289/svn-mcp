package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetFileResult(
        long revision,
        String path,
        String mimeType,
        boolean text,
        String encodingHint,
        boolean truncated,
        String contentText,
        String contentBase64) {}
