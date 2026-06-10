package io.github.jason07289.svn.mcp.svn.svnkit;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;

record SvnKitRepositorySession(
        SVNRepository repository, SVNURL rootUrl, ISVNAuthenticationManager auth)
        implements AutoCloseable {

    @Override
    public void close() {
        repository.closeSession();
    }
}
