package io.github.jason07289.svn.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jason07289.svn.mcp.svn.api.RepositoryCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SvnMcpApplicationTests {

    @Autowired private RepositoryCatalog repositoryCatalog;

    @Test
    void contextLoads() {
        assertThat(repositoryCatalog.listRepositories()).isNotEmpty();
    }
}
