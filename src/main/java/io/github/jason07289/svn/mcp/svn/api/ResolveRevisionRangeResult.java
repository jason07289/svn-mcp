package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;

/**
 * Maps a wall-clock interval to SVN revisions via {@code getDatedRevision}. Log entries may still
 * need strict date filtering for exact boundaries.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResolveRevisionRangeResult(
        String path,
        Date startDateInclusive,
        Date endDateInclusive,
        long startRevision,
        long endRevision,
        String note) {}
