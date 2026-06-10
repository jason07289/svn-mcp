package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepositoryAuthorStatsResult(
        String repositoryId,
        String pathPrefix,
        Date startDateInclusive,
        Date endDateInclusive,
        long logStartRevision,
        long logEndRevision,
        int revisionCountInRange,
        int revisionsAnalyzed,
        boolean truncated,
        List<AuthorActivityRow> byAuthor,
        Map<String, String> rankings) {}
