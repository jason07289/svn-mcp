package io.github.jason07289.svn.mcp.svn.svnkit;

import java.util.LinkedHashMap;
import java.util.Map;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;

final class SvnKitPathSupport {

    private SvnKitPathSupport() {}

    static String normalizePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }
        String p = path.startsWith("/") ? path.substring(1) : path;
        while (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    static int depthOf(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        int d = 1;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                d++;
            }
        }
        return d;
    }

    static String joinRelative(String dir, String name) {
        if (dir == null || dir.isEmpty()) {
            return name;
        }
        return dir + "/" + name;
    }

    static Map<String, String> mapChangedPaths(Map<String, SVNLogEntryPath> changedPaths) {
        if (changedPaths == null || changedPaths.isEmpty()) {
            return Map.of();
        }
        Map<String, String> changed = new LinkedHashMap<>();
        for (Map.Entry<String, SVNLogEntryPath> e : changedPaths.entrySet()) {
            changed.put(e.getKey(), String.valueOf(e.getValue().getType()));
        }
        return changed;
    }

    static String kindName(SVNNodeKind kind) {
        if (kind == null) {
            return "unknown";
        }
        if (kind == SVNNodeKind.FILE) {
            return "file";
        }
        if (kind == SVNNodeKind.DIR) {
            return "dir";
        }
        if (kind == SVNNodeKind.NONE) {
            return "none";
        }
        return kind.toString();
    }
}
