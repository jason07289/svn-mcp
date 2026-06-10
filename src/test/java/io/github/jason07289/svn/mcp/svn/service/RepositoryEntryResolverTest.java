package io.github.jason07289.svn.mcp.svn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jason07289.svn.mcp.config.SvnMcpProperties;
import io.github.jason07289.svn.mcp.config.SvnMcpProperties.RepositoryEntry;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class RepositoryEntryResolverTest {

    @Test
    void require_returnsMatchingEntry() {
        SvnMcpProperties props = new SvnMcpProperties();
        RepositoryEntry a = new RepositoryEntry();
        a.setId("a");
        a.setRootUrl("file:///x");
        props.setRepositories(List.of(a));
        RepositoryEntryResolver resolver = new RepositoryEntryResolver(props);

        assertThat(resolver.require("a").getRootUrl()).isEqualTo("file:///x");
    }

    @Test
    void require_unknownId_throws() {
        SvnMcpProperties props = new SvnMcpProperties();
        props.setRepositories(List.of());
        RepositoryEntryResolver resolver = new RepositoryEntryResolver(props);

        assertThatThrownBy(() -> resolver.require("missing"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Unknown repository_id");
    }

    @Test
    void require_blankOrNull_throws() {
        SvnMcpProperties props = new SvnMcpProperties();
        props.setRepositories(List.of());
        RepositoryEntryResolver resolver = new RepositoryEntryResolver(props);

        assertThatThrownBy(() -> resolver.require(""))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("repository_id is required");
        assertThatThrownBy(() -> resolver.require("   "))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("repository_id is required");
        assertThatThrownBy(() -> resolver.require(null))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("repository_id is required");
    }
}
