package io.github.jason07289.cicd.mcp.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "io.github.jason07289.cicd.mcp")
public class CicdMcpProperties {

    @NestedConfigurationProperty
    private List<RepositoryEntry> repositories = new ArrayList<>();

    private String authzFile;

    @NestedConfigurationProperty
    private Defaults defaults = new Defaults();

    public List<RepositoryEntry> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepositoryEntry> repositories) {
        this.repositories = repositories != null ? repositories : new ArrayList<>();
    }

    public String getAuthzFile() {
        return authzFile;
    }

    public void setAuthzFile(String authzFile) {
        this.authzFile = authzFile;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults != null ? defaults : new Defaults();
    }

    public static class RepositoryEntry {

        private String id;
        private String name;
        private String rootUrl;
        private String group;

        @NestedConfigurationProperty
        private Credentials credentials = new Credentials();

        @NestedConfigurationProperty
        private Bugtraq bugtraq = new Bugtraq();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRootUrl() {
            return rootUrl;
        }

        public void setRootUrl(String rootUrl) {
            this.rootUrl = rootUrl;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public Credentials getCredentials() {
            return credentials;
        }

        public void setCredentials(Credentials credentials) {
            this.credentials = credentials != null ? credentials : new Credentials();
        }

        public Bugtraq getBugtraq() {
            return bugtraq;
        }

        public void setBugtraq(Bugtraq bugtraq) {
            this.bugtraq = bugtraq != null ? bugtraq : new Bugtraq();
        }
    }

    public static class Credentials {

        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Bugtraq {

        private String logRegex;

        public String getLogRegex() {
            return logRegex;
        }

        public void setLogRegex(String logRegex) {
            this.logRegex = logRegex;
        }
    }

    public static class Defaults {

        private int logLimitMax = 500;
        private long fileContentMaxBytes = 2_000_000L;
        /** Max revisions to run diff stats on in repository_author_stats. */
        private int maxRevisionsForStats = 500;
        /** Max unified-diff lines returned per diff_revision / diff_file (after line truncation). */
        private int diffMaxTotalLines = 3500;
        /** Max lines kept per Index section in unified diff responses. */
        private int diffMaxLinesPerFile = 1000;
        /** Max number of Index (file) sections in one diff response. */
        private int diffMaxFilesPerResponse = 15;
        /** Max characters per line in unified diff (long lines are ellipsized for LLM-facing responses). */
        private int diffMaxCharsPerLine = 800;
        /** Max UTF-8 byte length of unified_diff in diff_revision / diff_file responses (after line caps). */
        private long diffMaxResponseBytes = 300_000L;
        /**
         * When set and the client requests write_spill_file, full diff text is written here before line
         * truncation; response includes spill_file_path.
         */
        private String diffSpillDirectory = "";

        public int getLogLimitMax() {
            return logLimitMax;
        }

        public void setLogLimitMax(int logLimitMax) {
            this.logLimitMax = logLimitMax;
        }

        public long getFileContentMaxBytes() {
            return fileContentMaxBytes;
        }

        public void setFileContentMaxBytes(long fileContentMaxBytes) {
            this.fileContentMaxBytes = fileContentMaxBytes;
        }

        public int getMaxRevisionsForStats() {
            return maxRevisionsForStats;
        }

        public void setMaxRevisionsForStats(int maxRevisionsForStats) {
            this.maxRevisionsForStats = maxRevisionsForStats;
        }

        public int getDiffMaxTotalLines() {
            return diffMaxTotalLines;
        }

        public void setDiffMaxTotalLines(int diffMaxTotalLines) {
            this.diffMaxTotalLines = diffMaxTotalLines;
        }

        public int getDiffMaxLinesPerFile() {
            return diffMaxLinesPerFile;
        }

        public void setDiffMaxLinesPerFile(int diffMaxLinesPerFile) {
            this.diffMaxLinesPerFile = diffMaxLinesPerFile;
        }

        public int getDiffMaxFilesPerResponse() {
            return diffMaxFilesPerResponse;
        }

        public void setDiffMaxFilesPerResponse(int diffMaxFilesPerResponse) {
            this.diffMaxFilesPerResponse = diffMaxFilesPerResponse;
        }

        public int getDiffMaxCharsPerLine() {
            return diffMaxCharsPerLine;
        }

        public void setDiffMaxCharsPerLine(int diffMaxCharsPerLine) {
            this.diffMaxCharsPerLine = diffMaxCharsPerLine;
        }

        public long getDiffMaxResponseBytes() {
            return diffMaxResponseBytes;
        }

        public void setDiffMaxResponseBytes(long diffMaxResponseBytes) {
            this.diffMaxResponseBytes = diffMaxResponseBytes;
        }

        public String getDiffSpillDirectory() {
            return diffSpillDirectory;
        }

        public void setDiffSpillDirectory(String diffSpillDirectory) {
            this.diffSpillDirectory = diffSpillDirectory != null ? diffSpillDirectory : "";
        }
    }
}
