package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiffRevisionResult(
        String path,
        long revision,
        @JsonProperty("from_revision") long fromRevision,
        @JsonProperty("unified_diff") String unifiedDiff,
        boolean truncated,
        @JsonProperty("changed_paths") Map<String, String> changedPaths,
        @JsonProperty("spill_file_path") String spillFilePath,
        DiffRevisionTruncation truncation) {

    public DiffRevisionResult(
            String path, long revision, long fromRevision, String unifiedDiff, boolean truncated) {
        this(path, revision, fromRevision, unifiedDiff, truncated, null, null, null);
    }
}
