package com.sap.sse.landscape.mongodb.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.RawBsonDocument;
import org.bson.codecs.DocumentCodec;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

/**
 * Regression tests for the archive-copy path in {@link MongoEndpointImpl#importDatabase(MongoDatabase)}, which caused
 * heap exhaustion when archiving large replica sets (issue #6357). The original importer streamed through a cursor but
 * retained up to {@link MongoEndpointImpl#IMPORT_BATCH_SIZE} fully decoded {@code Document} objects per write, so a
 * document-count limit alone could not bound the heap for large nested BSON documents, and it never closed its cursors.
 * <p>
 * These tests pin down the fix: copying as raw BSON, bounding each write batch by both document count and encoded BSON
 * bytes, bounding the driver read batch, and closing the collection-name and document cursors on success and failure.
 * <p>
 * This test lives in the same package as {@link MongoEndpointImpl} and runs from the {@code com.sap.sse.landscape.test}
 * fragment, which contributes to the host bundle's packages. It can therefore read the package-visible
 * {@link MongoEndpointImpl#IMPORT_BATCH_SIZE} and {@link MongoEndpointImpl#IMPORT_BATCH_BYTES} budgets and derive its
 * expectations from them, so the assertions stay correct if those budgets are retuned rather than silently asserting
 * stale literals.
 */
public class TestMongoDatabaseImport {
    /**
     * More than two full count-limited batches of tiny documents are copied in several batches, each within the
     * count budget, in their original order, with the first write happening before the whole collection has been read
     * (proving the copy streams rather than materializing every document first), and both cursors are closed.
     */
    @Test
    public void testStreamsSmallDocumentsInOrder() throws URISyntaxException {
        final int documentCount = 2 * MongoEndpointImpl.IMPORT_BATCH_SIZE + 5;
        final List<RawBsonDocument> documents = new ArrayList<>();
        for (int i = 0; i < documentCount; i++) {
            documents.add(new RawBsonDocument(new Document("_id", i), new DocumentCodec()));
        }
        final ImportFixture fixture = new ImportFixture(documents);
        assertSame(fixture.targetDatabase, fixture.endpoint.importDatabase(fixture.sourceDatabase));
        final int expectedBatchCount = (documentCount + MongoEndpointImpl.IMPORT_BATCH_SIZE - 1) / MongoEndpointImpl.IMPORT_BATCH_SIZE;
        assertEquals(expectedBatchCount, fixture.batches.size());
        for (final List<RawBsonDocument> batch : fixture.batches) {
            assertTrue(batch.size() <= MongoEndpointImpl.IMPORT_BATCH_SIZE);
        }
        assertTrue(fixture.readCountsAtInsert.get(/* index */ 0) < documents.size(), "Must write before consuming the whole collection");
        assertEquals(documents, fixture.batches.stream().flatMap(List::stream).collect(Collectors.toList()));
        fixture.verifyCursorsClosed();
    }

    /**
     * Documents small enough to stay under the count limit but large enough that two together approach the byte budget
     * are split across more than one batch, each batch staying within {@link MongoEndpointImpl#IMPORT_BATCH_BYTES},
     * demonstrating that a document-count limit alone does not bound retained bytes. All documents are preserved in
     * order and both cursors are closed.
     */
    @Test
    public void testLimitsBatchesByBsonBytes() throws URISyntaxException {
        final RawBsonDocument document = documentWithPayload(/* payloadBytes */ MongoEndpointImpl.IMPORT_BATCH_BYTES * 2 / 5);
        final List<RawBsonDocument> documents = Collections.nCopies(/* n */ 5, document);
        final ImportFixture fixture = new ImportFixture(documents);
        fixture.endpoint.importDatabase(fixture.sourceDatabase);
        assertTrue(fixture.batches.size() > 1, "A document-count limit alone does not bound retained bytes");
        for (final List<RawBsonDocument> batch : fixture.batches) {
            assertTrue(batch.stream().mapToInt((final RawBsonDocument d) -> d.getByteBuffer().remaining()).sum() <= MongoEndpointImpl.IMPORT_BATCH_BYTES);
        }
        assertEquals(documents, fixture.batches.stream().flatMap(List::stream).collect(Collectors.toList()));
        fixture.verifyCursorsClosed();
    }

    /**
     * A single document larger than the whole byte budget is copied in a batch of its own, flanked by the small
     * documents around it in separate batches, so one oversized but valid document neither blocks the copy nor is
     * merged with its neighbours. Both cursors are closed.
     */
    @Test
    public void testDocumentLargerThanBatchBudgetIsCopiedAlone() throws URISyntaxException {
        final RawBsonDocument small = documentWithPayload(/* payloadBytes */ 10);
        final RawBsonDocument large = documentWithPayload(/* payloadBytes */ MongoEndpointImpl.IMPORT_BATCH_BYTES + 1);
        final ImportFixture fixture = new ImportFixture(Arrays.asList(small, large, small));
        fixture.endpoint.importDatabase(fixture.sourceDatabase);
        assertEquals(/* expected */ 3, fixture.batches.size());
        assertEquals(Collections.singletonList(small), fixture.batches.get(/* index */ 0));
        assertEquals(Collections.singletonList(large), fixture.batches.get(/* index */ 1));
        assertEquals(Collections.singletonList(small), fixture.batches.get(/* index */ 2));
        fixture.verifyCursorsClosed();
    }

    /**
     * An empty source collection is still created on the target, so database hashes stay equal, and no write is
     * attempted. Both cursors are closed.
     */
    @Test
    public void testPreservesEmptyCollection() throws URISyntaxException {
        final ImportFixture fixture = new ImportFixture(Collections.emptyList());
        fixture.endpoint.importDatabase(fixture.sourceDatabase);
        verify(fixture.targetDatabase).createCollection("collection");
        verify(fixture.targetCollection, never()).insertMany(anyList());
        fixture.verifyCursorsClosed();
    }

    /**
     * When a write fails, the original exception propagates unchanged and both cursors are closed, so a failed archive
     * does not leak MongoDB cursors.
     */
    @Test
    public void testClosesCursorsOnWriteFailure() throws URISyntaxException {
        final ImportFixture fixture = new ImportFixture(Collections.singletonList(documentWithPayload(/* payloadBytes */ 10)));
        final RuntimeException failure = new IllegalStateException("write failed");
        doThrow(failure).when(fixture.targetCollection).insertMany(anyList());
        assertSame(failure, assertThrows(IllegalStateException.class, () -> fixture.endpoint.importDatabase(fixture.sourceDatabase)));
        fixture.verifyCursorsClosed();
    }

    /**
     * When a read fails, the original exception propagates unchanged and both cursors are closed, so a failed archive
     * does not leak MongoDB cursors.
     */
    @Test
    public void testClosesCursorsOnReadFailure() throws URISyntaxException {
        final ImportFixture fixture = new ImportFixture(Collections.singletonList(documentWithPayload(/* payloadBytes */ 10)));
        final RuntimeException failure = new IllegalStateException("read failed");
        doThrow(failure).when(fixture.documentCursor).next();
        assertSame(failure, assertThrows(IllegalStateException.class, () -> fixture.endpoint.importDatabase(fixture.sourceDatabase)));
        fixture.verifyCursorsClosed();
    }

    private static RawBsonDocument documentWithPayload(final int payloadBytes) {
        return new RawBsonDocument(new Document("_id", "id").append("payload", new byte[payloadBytes]), new DocumentCodec());
    }

    /**
     * Mocked MongoDB wiring for a single source database holding one collection named {@code "collection"} whose
     * documents are supplied to the constructor. The source and target databases, their collections, and the
     * collection-name and document cursors are all Mockito mocks; the importer runs against them as the real method via
     * {@code thenCallRealMethod()}.
     * <p>
     * The fixture records every {@code insertMany} batch in {@link #batches} and, in {@link #readCountsAtInsert}, how
     * many documents had been read when each batch was written, so tests can assert batch sizing, ordering and
     * streaming. {@link #verifyCursorsClosed()} checks that both cursors were closed.
     * <p>
     * Both the raw-BSON collection lookup used by the importer and the original decoded-{@code Document} lookup are
     * stubbed (see {@link #supportDecodedCollections()}), sharing one input and write sink, so a regression fails on
     * the intended batching or cursor-cleanup assertion rather than on an unstubbed collection lookup.
     */
    private static class ImportFixture {
        private final MongoDatabase sourceDatabase = mock(MongoDatabase.class);
        private final MongoDatabase targetDatabase = mock(MongoDatabase.class);
        private final MongoEndpointImpl endpoint = mock(MongoEndpointImpl.class);
        private final MongoCollection<RawBsonDocument> targetCollection;
        private final MongoCursor<String> collectionCursor;
        private final MongoCursor<RawBsonDocument> documentCursor;
        private final List<List<RawBsonDocument>> batches = new ArrayList<>();
        private final List<Integer> readCountsAtInsert = new ArrayList<>();

        @SuppressWarnings("unchecked")
        private ImportFixture(final List<RawBsonDocument> documents) throws URISyntaxException {
            final MongoCollection<RawBsonDocument> sourceCollection = mock(MongoCollection.class);
            targetCollection = mock(MongoCollection.class);
            collectionCursor = mock(MongoCursor.class);
            documentCursor = mock(MongoCursor.class);
            final MongoIterable<String> collectionNames = mock(MongoIterable.class);
            final FindIterable<RawBsonDocument> find = mock(FindIterable.class);
            final Iterator<RawBsonDocument> iterator = documents.iterator();
            final AtomicInteger readCount = new AtomicInteger();
            when(sourceDatabase.getName()).thenReturn("database");
            when(sourceDatabase.listCollectionNames()).thenReturn(collectionNames);
            when(collectionNames.iterator()).thenReturn(collectionCursor);
            when(collectionCursor.hasNext()).thenReturn(/* value */ true, /* values */ false);
            when(collectionCursor.next()).thenReturn("collection");
            when(sourceDatabase.getCollection("collection", RawBsonDocument.class)).thenReturn(sourceCollection);
            when(targetDatabase.getCollection("collection", RawBsonDocument.class)).thenReturn(targetCollection);
            when(sourceCollection.find()).thenReturn(find);
            when(find.batchSize(anyInt())).thenAnswer((final InvocationOnMock invocation) -> {
                final int batchSize = invocation.getArgument(/* index */ 0);
                assertTrue(batchSize > 0 && batchSize <= MongoEndpointImpl.IMPORT_BATCH_SIZE, "Read batches must also be bounded");
                return find;
            });
            when(find.iterator()).thenReturn(documentCursor);
            when(documentCursor.hasNext()).thenAnswer((final InvocationOnMock invocation) -> iterator.hasNext());
            when(documentCursor.next()).thenAnswer((final InvocationOnMock invocation) -> {
                readCount.incrementAndGet();
                return iterator.next();
            });
            doAnswer((final InvocationOnMock invocation) -> {
                batches.add(new ArrayList<>(invocation.<List<RawBsonDocument>>getArgument(/* index */ 0)));
                readCountsAtInsert.add(readCount.get());
                return null;
            }).when(targetCollection).insertMany(anyList());
            when(endpoint.getMongoDatabase("database")).thenReturn(targetDatabase);
            when(endpoint.importDatabase(sourceDatabase)).thenCallRealMethod();
            supportDecodedCollections();
        }

        /**
         * Also exercise the original Document-based importer, so regressions fail on batching and cursor cleanup
         * rather than on an unstubbed collection lookup. Both representations share the same input and write sink.
         */
        @SuppressWarnings("unchecked")
        private void supportDecodedCollections() {
            final MongoCollection<Document> sourceCollection = mock(MongoCollection.class);
            final MongoCollection<Document> decodedTargetCollection = mock(MongoCollection.class);
            final FindIterable<Document> find = mock(FindIterable.class);
            final MongoCursor<Document> decodedCursor = mock(MongoCursor.class);
            when(sourceDatabase.getCollection("collection")).thenReturn(sourceCollection);
            when(targetDatabase.getCollection("collection")).thenReturn(decodedTargetCollection);
            when(sourceCollection.find()).thenReturn(find);
            when(find.iterator()).thenReturn(decodedCursor);
            when(decodedCursor.hasNext()).thenAnswer((final InvocationOnMock invocation) -> documentCursor.hasNext());
            when(decodedCursor.next()).thenAnswer((final InvocationOnMock invocation) -> documentCursor.next().decode(new DocumentCodec()));
            doAnswer((final InvocationOnMock invocation) -> {
                documentCursor.close();
                return null;
            }).when(decodedCursor).close();
            doAnswer((final InvocationOnMock invocation) -> {
                final List<Document> documents = invocation.getArgument(/* index */ 0);
                final List<RawBsonDocument> rawDocuments = documents.stream()
                        .map((final Document document) -> new RawBsonDocument(document, new DocumentCodec()))
                        .collect(Collectors.toList());
                targetCollection.insertMany(rawDocuments);
                return null;
            }).when(decodedTargetCollection).insertMany(anyList());
        }

        private void verifyCursorsClosed() {
            verify(documentCursor).close();
            verify(collectionCursor).close();
        }
    }
}
