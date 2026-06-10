package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiffRevisionTruncation(
        @JsonProperty("line_truncated") boolean lineTruncated,
        @JsonProperty("lines_emitted") int linesEmitted,
        @JsonProperty("lines_in_full_diff") int linesInFullDiff,
        @JsonProperty("file_sections_included") int fileSectionsIncluded,
        @JsonProperty("file_sections_omitted") int fileSectionsOmitted,
        @JsonProperty("line_offset_applied") int lineOffsetApplied,
        @JsonProperty("next_line_offset") int nextLineOffset,
        @JsonProperty("has_more") boolean hasMore,
        @JsonProperty("line_chars_truncated") boolean lineCharsTruncated,
        @JsonProperty("lines_char_capped") int linesCharCapped,
        @JsonProperty("bytes_truncated") boolean bytesTruncated) {}
