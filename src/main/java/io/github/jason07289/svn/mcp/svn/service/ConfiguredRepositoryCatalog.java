package io.github.jason07289.svn.mcp.svn.service;

import io.github.jason07289.svn.mcp.config.SvnMcpProperties;
import io.github.jason07289.svn.mcp.svn.api.RepositoryCatalog;
import io.github.jason07289.svn.mcp.svn.api.RepositorySummary;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConfiguredRepositoryCatalog implements RepositoryCatalog {

    private final SvnMcpProperties properties;

    public ConfiguredRepositoryCatalog(SvnMcpProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<RepositorySummary> listRepositories() {
        return properties.getRepositories().stream()
                .map(
                        r ->
                                RepositorySummary.fromConfig(
                                        r.getId(), r.getName(), r.getRootUrl(), r.getGroup()))
                .toList();
    }
}
