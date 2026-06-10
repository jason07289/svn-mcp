package io.github.jason07289.svn.mcp;

import io.github.jason07289.svn.mcp.config.SvnMcpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SvnMcpProperties.class)
public class SvnMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SvnMcpApplication.class, args);
    }
}
