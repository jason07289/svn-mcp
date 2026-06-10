package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiffFileResult(
        @JsonProperty("unified_diff") String unifiedDiff,
        boolean truncated,
        @JsonProperty("spill_file_path") String spillFilePath,
        DiffRevisionTruncation truncation) {

    public static DiffFileResult legacy(String unifiedDiff, boolean truncated) {
        return new DiffFileResult(unifiedDiff, truncated, null, null);
    }
}
