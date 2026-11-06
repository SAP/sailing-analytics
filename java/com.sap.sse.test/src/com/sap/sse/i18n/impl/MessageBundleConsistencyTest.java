package com.sap.sse.i18n.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks the consistency of all message bundles ({@code .properties} files with at least one locale-specific sibling
 * such as {@code StringMessages_de.properties}) found in the bundles of the {@code java/} folder of the workspace. This
 * covers the GWT client bundles as well as the back-end resource bundles. The bundles are found relative to this test
 * bundle's directory, which is the working directory both in the Maven/Tycho build and in an Eclipse JUnit launch.
 * <p>
 * Two rules are checked:
 * <ul>
 * <li>No message bundle file in any language may contain a key more than once. Java and GWT silently use the last
 * occurrence, so duplicates hide earlier texts and usually indicate copy/paste or merge errors.</li>
 * <li>The German bundles, maintained by the development team together with the English source, must have exactly the
 * keys of their English counterpart. A German key that is not in its English bundle is only accepted if it occurs in
 * some other English bundle, as GWT allows a sub-interface's bundle to override messages inherited from a
 * super-interface. Intentional differences have to be listed in {@link #INTENTIONAL_GERMAN_DIFFERENCES}.</li>
 * </ul>
 * The other languages are delivered by the translation process with some delay and are therefore not checked for
 * completeness.
 */
public class MessageBundleConsistencyTest {
    private static final String GERMAN_SUFFIX = "_de";

    private static final Pattern LOCALIZED_FILE_NAME = Pattern.compile("(.+)_([a-z]{2}(?:_[A-Z]{2})?)\\.properties");

    private static final Set<String> IGNORED_DIRECTORY_NAMES = new HashSet<>(Arrays.asList("bin", "target", "build",
            "war", "node_modules", "gwt-unitCache"));

    /**
     * Keys by bundle base path (relative to the {@code java/} folder, without the {@code .properties} suffix) which are
     * intentionally missing in, or only present in, the German bundle.
     */
    private static final Map<String, Set<String>> INTENTIONAL_GERMAN_DIFFERENCES = Collections.singletonMap(
            // the English bundle explains: tests the fallback to the default locale
            "com.sap.sse.datamining.test/resources/stringmessages/Test_StringMessages",
            Collections.singleton("DefaultTestMessage"));

    /**
     * Base paths (without the {@code .properties} suffix) of all message bundles, mapped to the files of the bundle,
     * the base (English) file first
     */
    private static Map<Path, List<Path>> messageBundles;

    private static Path javaFolder;

    @BeforeAll
    public static void findMessageBundles() throws IOException {
        javaFolder = new File("..").getCanonicalFile().toPath();
        final Map<Path, List<Path>> result = new TreeMap<>();
        Files.walkFileTree(javaFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                final String name = dir.getFileName().toString();
                final boolean ignore = !dir.equals(javaFolder)
                        && (IGNORED_DIRECTORY_NAMES.contains(name) || name.startsWith("."));
                return ignore ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                final Matcher matcher = LOCALIZED_FILE_NAME.matcher(file.getFileName().toString());
                if (matcher.matches()) {
                    final Path baseFile = file.resolveSibling(matcher.group(1) + ".properties");
                    if (Files.isRegularFile(baseFile)) {
                        final List<Path> files = result.computeIfAbsent(file.resolveSibling(matcher.group(1)),
                                base -> new ArrayList<>(Collections.singletonList(baseFile)));
                        files.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        messageBundles = result;
    }

    @Test
    public void testMessageBundlesFound() {
        assertTrue(messageBundles.size() > 20, "Expected to find the workspace's message bundles relative to "
                + javaFolder + " but found only " + messageBundles.size());
        assertTrue(messageBundles.keySet().stream().anyMatch(base -> base.endsWith(
                "com.sap.sailing.gwt.ui/src/main/java/com/sap/sailing/gwt/ui/client/StringMessages")),
                "Expected to find the com.sap.sailing.gwt.ui StringMessages bundle");
    }

    @Test
    public void testNoDuplicateKeys() throws IOException {
        final List<String> problems = new ArrayList<>();
        for (final List<Path> files : messageBundles.values()) {
            for (final Path file : files) {
                final Map<String, List<Integer>> lineNumbersByKey = readKeysWithLineNumbers(file);
                for (final Map.Entry<String, List<Integer>> e : lineNumbersByKey.entrySet()) {
                    if (e.getValue().size() > 1) {
                        problems.add(javaFolder.relativize(file) + ": key " + e.getKey() + " occurs in lines "
                                + e.getValue());
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), "Duplicate keys in message bundles:\n" + String.join("\n", problems));
    }

    @Test
    public void testGermanBundlesHaveSameKeysAsEnglish() throws IOException {
        final Map<Path, Set<String>> englishKeysByBase = new HashMap<>();
        final Set<String> allEnglishKeys = new HashSet<>();
        for (final Map.Entry<Path, List<Path>> e : messageBundles.entrySet()) {
            final Set<String> englishKeys = readKeysWithLineNumbers(e.getValue().get(0)).keySet();
            englishKeysByBase.put(e.getKey(), englishKeys);
            allEnglishKeys.addAll(englishKeys);
        }
        final List<String> problems = new ArrayList<>();
        for (final Map.Entry<Path, List<Path>> e : messageBundles.entrySet()) {
            final Path germanFile = e.getKey().resolveSibling(e.getKey().getFileName() + GERMAN_SUFFIX + ".properties");
            if (e.getValue().contains(germanFile)) {
                final String bundleName = javaFolder.relativize(e.getKey()).toString().replace(File.separatorChar, '/');
                final Set<String> intentionalDifferences = INTENTIONAL_GERMAN_DIFFERENCES.getOrDefault(bundleName,
                        Collections.emptySet());
                final Set<String> englishKeys = englishKeysByBase.get(e.getKey());
                final Set<String> germanKeys = readKeysWithLineNumbers(germanFile).keySet();
                final Set<String> missingInGerman = new TreeSet<>(englishKeys);
                missingInGerman.removeAll(germanKeys);
                missingInGerman.removeAll(intentionalDifferences);
                final Set<String> unknownInGerman = new TreeSet<>(germanKeys);
                unknownInGerman.removeAll(allEnglishKeys);
                unknownInGerman.removeAll(intentionalDifferences);
                if (!missingInGerman.isEmpty()) {
                    problems.add(bundleName + ": missing German translations for " + missingInGerman);
                }
                if (!unknownInGerman.isEmpty()) {
                    problems.add(bundleName + ": German keys not found in any English bundle " + unknownInGerman);
                }
            }
        }
        assertTrue(problems.isEmpty(),
                "German and English message bundles are inconsistent:\n" + String.join("\n", problems));
    }

    /**
     * Reads the keys of a {@code .properties} file following the syntax of {@link java.util.Properties#load(java.io.Reader)}
     * regarding comments, line continuations and key/value separators. The keys are not unescaped, which is
     * sufficient for comparing them. The file is decoded as ISO-8859-1, which never fails and leaves the ASCII keys
     * intact regardless of the file's actual encoding.
     *
     * @return the keys in the order of their first occurrence, each mapped to the 1-based numbers of the lines in
     *         which the key occurs
     */
    private static Map<String, List<Integer>> readKeysWithLineNumbers(final Path file) throws IOException {
        final List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        final Map<String, List<Integer>> result = new LinkedHashMap<>();
        boolean continuation = false;
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final String trimmed = stripLeadingWhitespace(line);
            final boolean isCommentOrBlank = trimmed.isEmpty() || trimmed.charAt(0) == '#' || trimmed.charAt(0) == '!';
            if (!continuation && !isCommentOrBlank) {
                result.computeIfAbsent(extractKey(trimmed), k -> new ArrayList<>()).add(i + 1);
            }
            // a comment line never continues; otherwise an odd number of trailing backslashes continues the line
            continuation = (continuation || !isCommentOrBlank) && endsWithOddNumberOfBackslashes(line);
        }
        return result;
    }

    private static String extractKey(final String trimmedLine) {
        int end = 0;
        boolean escaped = false;
        while (end < trimmedLine.length() && (escaped || "=: \t\f".indexOf(trimmedLine.charAt(end)) < 0)) {
            escaped = !escaped && trimmedLine.charAt(end) == '\\';
            end++;
        }
        return trimmedLine.substring(/* beginIndex */ 0, end);
    }

    private static String stripLeadingWhitespace(final String line) {
        int start = 0;
        while (start < line.length() && " \t\f".indexOf(line.charAt(start)) >= 0) {
            start++;
        }
        return line.substring(start);
    }

    private static boolean endsWithOddNumberOfBackslashes(final String line) {
        int count = 0;
        while (count < line.length() && line.charAt(line.length() - 1 - count) == '\\') {
            count++;
        }
        return count % 2 == 1;
    }
}
