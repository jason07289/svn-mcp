package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiffFileRequest(Integer maxTotalLines, Integer lineOffset, boolean writeSpillFile) {

    public static DiffFileRequest defaults() {
        return new DiffFileRequest(null, null, false);
    }
}
