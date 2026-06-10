package io.github.jason07289.svn.mcp.svn.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PathEntry(String name, String relativePath, String kind, long size) {}
