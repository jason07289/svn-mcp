package io.github.jason07289.svn.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SvnMcpToolDefinitionsTest {

    @Test
    void all_keepsExistingToolOrderAndNames() {
        assertThat(SvnMcpToolDefinitions.all())
                .extracting(McpToolDefinition::name)
                .containsExactly(
                        "list_repositories",
                        "list_path",
                        "get_file",
                        "get_log",
                        "get_revision",
                        "diff_file",
                        "blame_file",
                        "resolve_revision_range",
                        "diff_revision",
                        "repository_author_stats",
                        "search_in_path");
    }

    @Test
    void listRepositories_staysConfigOnlyEmptyObjectContract() {
        McpToolDefinition definition = definition("list_repositories");

        assertThat(definition.inputSchema().properties()).isEmpty();
        assertThat(definition.inputSchema().required()).isEmpty();
    }

    @Test
    void requiredArguments_matchCurrentMcpContract() {
        assertThat(definition("list_path").inputSchema().required())
                .containsExactly("repository_id");
        assertThat(definition("get_file").inputSchema().required())
                .containsExactly("repository_id", "path");
        assertThat(definition("diff_file").inputSchema().required())
                .containsExactly("repository_id", "path", "from_revision", "to_revision");
        assertThat(definition("diff_revision").inputSchema().required())
                .containsExactly("repository_id", "revision");
        assertThat(definition("search_in_path").inputSchema().required())
                .containsExactly("repository_id", "path", "keyword");
    }

    private static McpToolDefinition definition(String name) {
        return SvnMcpToolDefinitions.all().stream()
                .filter(d -> d.name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
