package io.github.jason07289.svn.mcp.svn.svnkit;

import io.github.jason07289.svn.mcp.config.SvnMcpProperties.RepositoryEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

final class SvnKitRepositorySessionFactory {

    SvnKitRepositorySession open(RepositoryEntry entry) throws SVNException {
        SVNURL root = SVNURL.parseURIEncoded(entry.getRootUrl());
        SVNRepository repository = SVNRepositoryFactory.create(root);
        ISVNAuthenticationManager auth = buildAuth(entry);
        repository.setAuthenticationManager(auth);
        return new SvnKitRepositorySession(repository, root, auth);
    }

    private ISVNAuthenticationManager buildAuth(RepositoryEntry entry) {
        String user = entry.getCredentials().getUsername();
        String pass = entry.getCredentials().getPassword();
        boolean hasUser = user != null && !user.isBlank();
        boolean hasPass = pass != null && !pass.isBlank();
        if (hasUser || hasPass) {
            return SVNWCUtil.createDefaultAuthenticationManager(
                    hasUser ? user : "", hasPass ? pass.toCharArray() : null);
        }
        return SVNWCUtil.createDefaultAuthenticationManager();
    }
}
