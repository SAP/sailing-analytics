package com.sap.sailing.landscape.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.impl.DataImportProgressImpl;
import com.sap.sailing.domain.common.impl.MasterDataImportObjectCreationCountImpl;
import com.sap.sailing.server.gateway.interfaces.CompareServersResult;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class ArchiveReplicaSetOutcomeTest {
    private final ResourceBundleStringMessages stringMessages = ResourceBundleStringMessages.create(
            "stringmessages/SailingLandscape_StringMessages", LandscapeServiceImpl.class.getClassLoader(),
            StandardCharsets.UTF_8.name());

    @Test
    public void testLazyDifferencesInLocalizedMail() throws ParseException {
        final JSONObject first = new JSONObject();
        first.put("event", "Kiel's \"Week\"\n{0}\\archive");
        final JSONObject second = new JSONObject();
        second.put("regatta", "49er");
        final JSONObject otherSide = new JSONObject();
        otherSide.put("event", "Travemünde");
        final CompareServersResult comparison = comparison(Util.map(Arrays.asList(first, second), diff -> diff),
                Util.map(Collections.singletonList(otherSide), diff -> diff));
        final Triple<DataImportProgress, CompareServersResult, String> result = new Triple<>(
                completedImport(/* withLeaderboardGroup */ true), comparison, /* c */ null);
        for (final Locale locale : Arrays.asList(Locale.US, Locale.GERMANY)) {
            final String outcome = describe(result, locale);
            assertTrue(outcome.startsWith(locale.equals(Locale.GERMANY)
                    ? "Der Serververgleich hat Unterschiede gefunden:" : "Server comparison found differences:"));
            final String[] lines = outcome.split("\n");
            assertEquals("Server source:", lines[2]);
            assertEquals(Arrays.asList(first, second), new JSONParser().parse(lines[3]));
            assertEquals("Server archive:", lines[5]);
            assertEquals(Collections.singletonList(otherSide), new JSONParser().parse(lines[6]));
            final String body = stringMessages.get(locale, "ArchiveReplicaSetOutcomeBody", "source", outcome);
            assertTrue(body.endsWith(outcome));
            assertFalse(body.contains("MappingIterable@"));
        }
    }

    @Test
    public void testDifferencesOnlyOnSecondServer() {
        final JSONObject diff = new JSONObject();
        diff.put("event", "Only in archive");
        final CompareServersResult comparison = comparison(Collections.emptyList(),
                Util.map(Collections.singletonList(diff), value -> value));
        final String outcome = describe(new Triple<>(completedImport(/* withLeaderboardGroup */ true), comparison,
                /* c */ null), Locale.US);
        assertTrue(outcome.contains("Server source:\n[]"));
        assertTrue(outcome.contains("Server archive:\n[" + diff.toJSONString() + "]"));
    }

    @Test
    public void testMissingProgressIsLocalized() {
        final Triple<DataImportProgress, CompareServersResult, String> result = new Triple<>(/* a */ null,
                /* b */ null, /* c */ null);
        assertEquals("Master data import failed: No progress available", describe(result, Locale.US));
        assertEquals("Stammdatenimport fehlgeschlagen: Keine Fortschrittsinformationen verfügbar",
                describe(result, Locale.GERMANY));
    }

    @Test
    public void testImportFailureKeepsErrorDetails() {
        final DataImportProgress progress = new DataImportProgressImpl(UUID.randomUUID());
        progress.setErrorMessage("Import error: {0} 'event'");
        progress.setFailed();
        final Triple<DataImportProgress, CompareServersResult, String> result = new Triple<>(progress,
                /* b */ null, /* c */ null);
        assertEquals("Master data import failed: Import error: {0} 'event'", describe(result, Locale.US));
        assertEquals("Stammdatenimport fehlgeschlagen: Import error: {0} 'event'", describe(result, Locale.GERMANY));
    }

    @Test
    public void testDatabaseFailureIsLocalized() {
        final Triple<DataImportProgress, CompareServersResult, String> result = new Triple<>(
                completedImport(/* withLeaderboardGroup */ true), comparison(Collections.emptyList(), Collections.emptyList()),
                "mongodump: exit 1");
        assertEquals("Database archiving failed: mongodump: exit 1", describe(result, Locale.US));
        assertEquals("Datenbankarchivierung fehlgeschlagen: mongodump: exit 1", describe(result, Locale.GERMANY));
    }

    @Test
    public void testMissingComparisonIsNotSuccess() {
        final Triple<DataImportProgress, CompareServersResult, String> result = new Triple<>(
                completedImport(/* withLeaderboardGroup */ true), /* b */ null, /* c */ null);
        assertEquals("Server comparison did not produce a result", describe(result, Locale.US));
        assertEquals("Der Serververgleich hat kein Ergebnis geliefert", describe(result, Locale.GERMANY));
    }

    @Test
    public void testSuccessfulArchiveIsLocalized() {
        final Triple<DataImportProgress, CompareServersResult, String> result = new Triple<>(
                completedImport(/* withLeaderboardGroup */ true), comparison(Collections.emptyList(), Collections.emptyList()),
                /* c */ null);
        assertEquals("Archiving completed successfully", describe(result, Locale.US));
        assertEquals("Archivierung erfolgreich abgeschlossen", describe(result, Locale.GERMANY));
    }

    @Test
    public void testEmptyImportDoesNotRequireComparison() {
        final Triple<DataImportProgress, CompareServersResult, String> result = new Triple<>(
                completedImport(/* withLeaderboardGroup */ false), /* b */ null, /* c */ null);
        assertEquals("Archiving completed successfully", describe(result, Locale.US));
        assertEquals("Archivierung erfolgreich abgeschlossen", describe(result, Locale.GERMANY));
    }

    @Test
    public void testComparisonTimeoutPreservesDifferencesForMail() throws Exception {
        final JSONObject diff = new JSONObject();
        diff.put("event", "Missing from archive");
        final CompareServersResult comparison = comparison(Util.map(Collections.singletonList(diff), value -> value),
                Collections.emptyList());
        final CompareServersResult lastResult = LandscapeServiceImpl.compareServersWithRetries(() -> comparison,
                Duration.ofMillis(/* millis */ 0), Duration.ofMillis(/* millis */ 0), "source -> archive");
        assertSame(comparison, lastResult);
        final Triple<DataImportProgress, CompareServersResult, String> result = new Triple<>(
                completedImport(/* withLeaderboardGroup */ true), lastResult, /* c */ null);
        for (final Locale locale : Arrays.asList(Locale.US, Locale.GERMANY)) {
            final String body = stringMessages.get(locale, "ArchiveReplicaSetOutcomeBody", "source", describe(result, locale));
            assertTrue(body.contains("Server source:\n[" + diff.toJSONString() + "]"));
            assertTrue(body.contains("Server archive:\n[]"));
            assertFalse(body.contains("TimeoutException"));
        }
    }

    @Test
    public void testComparisonWithoutDifferencesSucceeds() throws Exception {
        final CompareServersResult comparison = comparison(Collections.emptyList(), Collections.emptyList());
        assertSame(comparison, LandscapeServiceImpl.compareServersWithRetries(() -> comparison,
                Duration.ofMillis(/* millis */ 0), Duration.ofMillis(/* millis */ 0), "source -> archive"));
    }

    @Test
    public void testComparisonWithoutResponseKeepsContextForFailureMail() {
        final TimeoutException failure = assertThrows(TimeoutException.class,
                () -> LandscapeServiceImpl.compareServersWithRetries(() -> {
                    throw new IOException("Comparison server unavailable");
                }, Duration.ofMillis(/* millis */ 0), Duration.ofMillis(/* millis */ 0), "source -> archive"));
        for (final Locale locale : Arrays.asList(Locale.US, Locale.GERMANY)) {
            final String outcome = stringMessages.get(locale, "ArchiveReplicaSetFailed", failure.toString());
            final String body = stringMessages.get(locale, "ArchiveReplicaSetOutcomeBody", "source", outcome);
            assertTrue(body.contains("source -> archive"));
            assertTrue(body.contains("no comparison result received"));
        }
    }

    private String describe(final Triple<DataImportProgress, CompareServersResult, String> result, final Locale locale) {
        return LandscapeServiceImpl.describeArchiveResult(result, locale, stringMessages);
    }

    private DataImportProgress completedImport(final boolean withLeaderboardGroup) {
        final DataImportProgress progress = new DataImportProgressImpl(UUID.randomUUID());
        final MasterDataImportObjectCreationCountImpl count = new MasterDataImportObjectCreationCountImpl();
        if (withLeaderboardGroup) {
            count.addOneLeaderboardGroup("group");
        }
        progress.setResult(count);
        return progress;
    }

    private CompareServersResult comparison(final Iterable<JSONObject> aDiffs, final Iterable<JSONObject> bDiffs) {
        return new CompareServersResult() {
            @Override
            public String getServerA() {
                return "source";
            }

            @Override
            public String getServerB() {
                return "archive";
            }

            @Override
            public Iterable<JSONObject> getADiffs() {
                return aDiffs;
            }

            @Override
            public Iterable<JSONObject> getBDiffs() {
                return bDiffs;
            }
        };
    }
}
