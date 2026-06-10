package io.github.jason07289.svn.mcp.svn.api;

/** Wraps SVN/network failures for MCP tool responses. */
public class SvnAccessException extends Exception {

    public SvnAccessException(String message) {
        super(message);
    }

    public SvnAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
