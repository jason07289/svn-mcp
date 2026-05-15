package io.github.jason07289.cicd.mcp.svn.svnkit;

import io.github.jason07289.cicd.mcp.svn.api.DiffRevisionTruncation;
import java.util.ArrayList;
import java.util.List;

/** Line- and file-aware truncation for SVNKit-style unified diffs ({@code Index:} per path). */
public final class UnifiedDiffTruncation {

    private UnifiedDiffTruncation() {}

    public record Result(String text, DiffRevisionTruncation meta) {}

    /**
     * Include at most {@code maxFiles} file sections (lines starting with {@code Index: }), each capped
     * at {@code maxLinesPerFile} lines, and total emitted lines capped at {@code maxTotalLines}. Then skip
     * the first {@code lineOffset} lines of that assembly (pagination).
     */
    public static Result truncate(
            String fullUnifiedDiff,
            int maxTotalLines,
            int maxLinesPerFile,
            int maxFiles,
            int lineOffset) {
        if (fullUnifiedDiff == null || fullUnifiedDiff.isEmpty()) {
            int off = Math.max(0, lineOffset);
            return new Result(
                    "",
                    new DiffRevisionTruncation(
                            false,
                            0,
                            0,
                            0,
                            0,
                            off,
                            off,
                            false));
        }
        String normalized = fullUnifiedDiff.endsWith("\n") ? fullUnifiedDiff : fullUnifiedDiff + "\n";
        int totalLines = countLines(normalized);

        List<Section> sections = splitByIndex(normalized);
        int fileSectionsTotal = countFileSections(sections);

        List<String> cappedLines = new ArrayList<>();
        int fileSectionsStarted = 0;
        boolean lineTruncated = false;

        outer:
        for (Section sec : sections) {
            if (sec.fileSection()) {
                if (fileSectionsStarted >= maxFiles) {
                    lineTruncated = true;
                    break;
                }
                fileSectionsStarted++;
            }
            int linesFromThisSection = 0;
            for (String line : sec.lines()) {
                if (sec.fileSection() && linesFromThisSection >= maxLinesPerFile) {
                    lineTruncated = true;
                    break;
                }
                if (cappedLines.size() >= maxTotalLines) {
                    lineTruncated = true;
                    break outer;
                }
                cappedLines.add(line);
                linesFromThisSection++;
            }
        }

        int fileSectionsIncluded = fileSectionsStarted;
        int fileSectionsOmitted = Math.max(0, fileSectionsTotal - fileSectionsIncluded);

        int skipped = Math.max(0, lineOffset);
        List<String> window = new ArrayList<>();
        for (String line : cappedLines) {
            if (skipped > 0) {
                skipped--;
                continue;
            }
            window.add(line);
        }

        StringBuilder out = new StringBuilder();
        for (String l : window) {
            out.append(l).append('\n');
        }
        int linesEmitted = window.size();
        int lineOffsetApplied = Math.max(0, lineOffset);
        int cappedTotalLines = cappedLines.size();
        int nextLineOffset = lineOffsetApplied + linesEmitted;
        boolean hasMoreLines = nextLineOffset < cappedTotalLines || fileSectionsOmitted > 0 || lineTruncated;

        return new Result(
                out.toString(),
                new DiffRevisionTruncation(
                        lineTruncated,
                        linesEmitted,
                        totalLines,
                        fileSectionsIncluded,
                        fileSectionsOmitted,
                        lineOffsetApplied,
                        nextLineOffset,
                        hasMoreLines));
    }

    private static int countFileSections(List<Section> sections) {
        int n = 0;
        for (Section s : sections) {
            if (s.fileSection()) {
                n++;
            }
        }
        return n;
    }

    private static int countLines(String s) {
        if (s.isEmpty()) {
            return 0;
        }
        int n = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    private static List<Section> splitByIndex(String normalized) {
        List<Integer> starts = new ArrayList<>();
        if (normalized.startsWith("Index: ")) {
            starts.add(0);
        }
        int idx = 0;
        while (true) {
            int at = normalized.indexOf("\nIndex: ", idx);
            if (at < 0) {
                break;
            }
            starts.add(at + 1);
            idx = at + 1;
        }
        if (starts.isEmpty()) {
            return List.of(new Section(splitLines(normalized), normalized.trim().startsWith("Index: ")));
        }
        List<Section> sections = new ArrayList<>();
        int firstContentStart = starts.get(0);
        if (firstContentStart > 0) {
            String preamble = normalized.substring(0, firstContentStart);
            if (!preamble.isBlank()) {
                sections.add(new Section(splitLines(preamble), false));
            }
        }
        for (int i = 0; i < starts.size(); i++) {
            int from = starts.get(i);
            int to = i + 1 < starts.size() ? starts.get(i + 1) : normalized.length();
            sections.add(new Section(splitLines(normalized.substring(from, to)), true));
        }
        return sections;
    }

    private static List<String> splitLines(String block) {
        List<String> lines = new ArrayList<>();
        int len = block.length();
        int start = 0;
        for (int i = 0; i < len; i++) {
            if (block.charAt(i) == '\n') {
                lines.add(block.substring(start, i));
                start = i + 1;
            }
        }
        if (start < len) {
            lines.add(block.substring(start));
        }
        return lines;
    }

    private record Section(List<String> lines, boolean fileSection) {}
}
