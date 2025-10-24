package com.sap.sse.security.userstore.mongodb.impl.sessionwrapper;

import java.util.List;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.ListSearchIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateIndexOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.DropCollectionOptions;
import com.mongodb.client.model.DropIndexOptions;
import com.mongodb.client.model.EstimatedDocumentCountOptions;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.RenameCollectionOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.SearchIndexModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

public class MongoCollectionWrapperWithClientSession<TDocument> implements MongoCollection<TDocument> {
    private final MongoCollection<TDocument> delegate;
    private final ClientSession clientSession;

    public MongoCollectionWrapperWithClientSession(ClientSession clientSession, MongoCollection<TDocument> delegate) {
        super();
        this.delegate = delegate;
        this.clientSession = clientSession;
    }

    public MongoNamespace getNamespace() {
        return delegate.getNamespace();
    }

    public Class<TDocument> getDocumentClass() {
        return delegate.getDocumentClass();
    }

    public CodecRegistry getCodecRegistry() {
        return delegate.getCodecRegistry();
    }

    public ReadPreference getReadPreference() {
        return delegate.getReadPreference();
    }

    public WriteConcern getWriteConcern() {
        return delegate.getWriteConcern();
    }

    public ReadConcern getReadConcern() {
        return delegate.getReadConcern();
    }

    public <NewTDocument> MongoCollection<NewTDocument> withDocumentClass(Class<NewTDocument> clazz) {
        return wrap(delegate.withDocumentClass(clazz));
    }

    public MongoCollection<TDocument> withCodecRegistry(CodecRegistry codecRegistry) {
        return wrap(delegate.withCodecRegistry(codecRegistry));
    }

    public MongoCollection<TDocument> withReadPreference(ReadPreference readPreference) {
        return wrap(delegate.withReadPreference(readPreference));
    }

    public MongoCollection<TDocument> withWriteConcern(WriteConcern writeConcern) {
        return wrap(delegate.withWriteConcern(writeConcern));
    }

    public MongoCollection<TDocument> withReadConcern(ReadConcern readConcern) {
        return wrap(delegate.withReadConcern(readConcern));
    }

    public long countDocuments() {
        return delegate.countDocuments(clientSession);
    }

    public long countDocuments(Bson filter) {
        return delegate.countDocuments(clientSession, filter);
    }

    public long countDocuments(Bson filter, CountOptions options) {
        return delegate.countDocuments(clientSession, filter, options);
    }

    public long countDocuments(ClientSession clientSession) {
        return delegate.countDocuments(clientSession);
    }

    public long countDocuments(ClientSession clientSession, Bson filter) {
        return delegate.countDocuments(clientSession, filter);
    }

    public long countDocuments(ClientSession clientSession, Bson filter, CountOptions options) {
        return delegate.countDocuments(clientSession, filter, options);
    }

    public long estimatedDocumentCount() {
        return delegate.estimatedDocumentCount();
    }

    public long estimatedDocumentCount(EstimatedDocumentCountOptions options) {
        return delegate.estimatedDocumentCount(options);
    }

    public <TResult> DistinctIterable<TResult> distinct(String fieldName, Class<TResult> resultClass) {
        return delegate.distinct(clientSession, fieldName, resultClass);
    }

    public <TResult> DistinctIterable<TResult> distinct(String fieldName, Bson filter, Class<TResult> resultClass) {
        return delegate.distinct(clientSession, fieldName, filter, resultClass);
    }

    public <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName,
            Class<TResult> resultClass) {
        return delegate.distinct(clientSession, fieldName, resultClass);
    }

    public <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName, Bson filter,
            Class<TResult> resultClass) {
        return delegate.distinct(clientSession, fieldName, filter, resultClass);
    }

    public FindIterable<TDocument> find() {
        return delegate.find(clientSession);
    }

    public <TResult> FindIterable<TResult> find(Class<TResult> resultClass) {
        return delegate.find(clientSession, resultClass);
    }

    public FindIterable<TDocument> find(Bson filter) {
        return delegate.find(clientSession, filter);
    }

    public <TResult> FindIterable<TResult> find(Bson filter, Class<TResult> resultClass) {
        return delegate.find(clientSession, filter, resultClass);
    }

    public FindIterable<TDocument> find(ClientSession clientSession) {
        return delegate.find(clientSession);
    }

    public <TResult> FindIterable<TResult> find(ClientSession clientSession, Class<TResult> resultClass) {
        return delegate.find(clientSession, resultClass);
    }

    public FindIterable<TDocument> find(ClientSession clientSession, Bson filter) {
        return delegate.find(clientSession, filter);
    }

    public <TResult> FindIterable<TResult> find(ClientSession clientSession, Bson filter, Class<TResult> resultClass) {
        return delegate.find(clientSession, filter, resultClass);
    }

    public AggregateIterable<TDocument> aggregate(List<? extends Bson> pipeline) {
        return delegate.aggregate(clientSession, pipeline);
    }

    public <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> pipeline, Class<TResult> resultClass) {
        return delegate.aggregate(clientSession, pipeline, resultClass);
    }

    public AggregateIterable<TDocument> aggregate(ClientSession clientSession, List<? extends Bson> pipeline) {
        return delegate.aggregate(clientSession, pipeline);
    }

    public <TResult> AggregateIterable<TResult> aggregate(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<TResult> resultClass) {
        return delegate.aggregate(clientSession, pipeline, resultClass);
    }

    public ChangeStreamIterable<TDocument> watch() {
        return delegate.watch(clientSession);
    }

    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> resultClass) {
        return delegate.watch(clientSession, resultClass);
    }

    public ChangeStreamIterable<TDocument> watch(List<? extends Bson> pipeline) {
        return delegate.watch(clientSession, pipeline);
    }

    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> pipeline, Class<TResult> resultClass) {
        return delegate.watch(clientSession, pipeline, resultClass);
    }

    public ChangeStreamIterable<TDocument> watch(ClientSession clientSession) {
        return delegate.watch(clientSession);
    }

    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> resultClass) {
        return delegate.watch(clientSession, resultClass);
    }

    public ChangeStreamIterable<TDocument> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return delegate.watch(clientSession, pipeline);
    }

    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<TResult> resultClass) {
        return delegate.watch(clientSession, pipeline, resultClass);
    }

    @SuppressWarnings("deprecation")
    public com.mongodb.client.MapReduceIterable<TDocument> mapReduce(String mapFunction, String reduceFunction) {
        return delegate.mapReduce(clientSession, mapFunction, reduceFunction);
    }

    @SuppressWarnings("deprecation")
    public <TResult> com.mongodb.client.MapReduceIterable<TResult> mapReduce(String mapFunction, String reduceFunction,
            Class<TResult> resultClass) {
        return delegate.mapReduce(clientSession, mapFunction, reduceFunction, resultClass);
    }

    @SuppressWarnings("deprecation")
    public com.mongodb.client.MapReduceIterable<TDocument> mapReduce(ClientSession clientSession, String mapFunction,
            String reduceFunction) {
        return delegate.mapReduce(clientSession, mapFunction, reduceFunction);
    }

    @SuppressWarnings("deprecation")
    public <TResult> com.mongodb.client.MapReduceIterable<TResult> mapReduce(ClientSession clientSession, String mapFunction,
            String reduceFunction, Class<TResult> resultClass) {
        return delegate.mapReduce(clientSession, mapFunction, reduceFunction, resultClass);
    }

    public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends TDocument>> requests) {
        return delegate.bulkWrite(clientSession, requests);
    }

    public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends TDocument>> requests,
            BulkWriteOptions options) {
        return delegate.bulkWrite(clientSession, requests, options);
    }

    public BulkWriteResult bulkWrite(ClientSession clientSession,
            List<? extends WriteModel<? extends TDocument>> requests) {
        return delegate.bulkWrite(clientSession, requests);
    }

    public BulkWriteResult bulkWrite(ClientSession clientSession,
            List<? extends WriteModel<? extends TDocument>> requests, BulkWriteOptions options) {
        return delegate.bulkWrite(clientSession, requests, options);
    }

    public InsertOneResult insertOne(TDocument document) {
        return delegate.insertOne(clientSession, document);
    }

    public InsertOneResult insertOne(TDocument document, InsertOneOptions options) {
        return delegate.insertOne(clientSession, document, options);
    }

    public InsertOneResult insertOne(ClientSession clientSession, TDocument document) {
        return delegate.insertOne(clientSession, document);
    }

    public InsertOneResult insertOne(ClientSession clientSession, TDocument document, InsertOneOptions options) {
        return delegate.insertOne(clientSession, document, options);
    }

    public InsertManyResult insertMany(List<? extends TDocument> documents) {
        return delegate.insertMany(clientSession, documents);
    }

    public InsertManyResult insertMany(List<? extends TDocument> documents, InsertManyOptions options) {
        return delegate.insertMany(clientSession, documents, options);
    }

    public InsertManyResult insertMany(ClientSession clientSession, List<? extends TDocument> documents) {
        return delegate.insertMany(clientSession, documents);
    }

    public InsertManyResult insertMany(ClientSession clientSession, List<? extends TDocument> documents,
            InsertManyOptions options) {
        return delegate.insertMany(clientSession, documents, options);
    }

    public DeleteResult deleteOne(Bson filter) {
        return delegate.deleteOne(clientSession, filter);
    }

    public DeleteResult deleteOne(Bson filter, DeleteOptions options) {
        return delegate.deleteOne(clientSession, filter, options);
    }

    public DeleteResult deleteOne(ClientSession clientSession, Bson filter) {
        return delegate.deleteOne(clientSession, filter);
    }

    public DeleteResult deleteOne(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return delegate.deleteOne(clientSession, filter, options);
    }

    public DeleteResult deleteMany(Bson filter) {
        return delegate.deleteMany(clientSession, filter);
    }

    public DeleteResult deleteMany(Bson filter, DeleteOptions options) {
        return delegate.deleteMany(clientSession, filter, options);
    }

    public DeleteResult deleteMany(ClientSession clientSession, Bson filter) {
        return delegate.deleteMany(clientSession, filter);
    }

    public DeleteResult deleteMany(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return delegate.deleteMany(clientSession, filter, options);
    }

    public UpdateResult replaceOne(Bson filter, TDocument replacement) {
        return delegate.replaceOne(clientSession, filter, replacement);
    }

    public UpdateResult replaceOne(Bson filter, TDocument replacement, ReplaceOptions replaceOptions) {
        return delegate.replaceOne(clientSession, filter, replacement, replaceOptions);
    }

    public UpdateResult replaceOne(ClientSession clientSession, Bson filter, TDocument replacement) {
        return delegate.replaceOne(clientSession, filter, replacement);
    }

    public UpdateResult replaceOne(ClientSession clientSession, Bson filter, TDocument replacement,
            ReplaceOptions replaceOptions) {
        return delegate.replaceOne(clientSession, filter, replacement, replaceOptions);
    }

    public UpdateResult updateOne(Bson filter, Bson update) {
        return delegate.updateOne(clientSession, filter, update);
    }

    public UpdateResult updateOne(Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateOne(clientSession, filter, update, updateOptions);
    }

    public UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update) {
        return delegate.updateOne(clientSession, filter, update);
    }

    public UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateOne(clientSession, filter, update, updateOptions);
    }

    public UpdateResult updateOne(Bson filter, List<? extends Bson> update) {
        return delegate.updateOne(clientSession, filter, update);
    }

    public UpdateResult updateOne(Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return delegate.updateOne(clientSession, filter, update, updateOptions);
    }

    public UpdateResult updateOne(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return delegate.updateOne(clientSession, filter, update);
    }

    public UpdateResult updateOne(ClientSession clientSession, Bson filter, List<? extends Bson> update,
            UpdateOptions updateOptions) {
        return delegate.updateOne(clientSession, filter, update, updateOptions);
    }

    public UpdateResult updateMany(Bson filter, Bson update) {
        return delegate.updateMany(clientSession, filter, update);
    }

    public UpdateResult updateMany(Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateMany(clientSession, filter, update, updateOptions);
    }

    public UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update) {
        return delegate.updateMany(clientSession, filter, update);
    }

    public UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateMany(clientSession, filter, update, updateOptions);
    }

    public UpdateResult updateMany(Bson filter, List<? extends Bson> update) {
        return delegate.updateMany(clientSession, filter, update);
    }

    public UpdateResult updateMany(Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return delegate.updateMany(clientSession, filter, update, updateOptions);
    }

    public UpdateResult updateMany(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return delegate.updateMany(clientSession, filter, update);
    }

    public UpdateResult updateMany(ClientSession clientSession, Bson filter, List<? extends Bson> update,
            UpdateOptions updateOptions) {
        return delegate.updateMany(clientSession, filter, update, updateOptions);
    }

    public TDocument findOneAndDelete(Bson filter) {
        return delegate.findOneAndDelete(clientSession, filter);
    }

    public TDocument findOneAndDelete(Bson filter, FindOneAndDeleteOptions options) {
        return delegate.findOneAndDelete(clientSession, filter, options);
    }

    public TDocument findOneAndDelete(ClientSession clientSession, Bson filter) {
        return delegate.findOneAndDelete(clientSession, filter);
    }

    public TDocument findOneAndDelete(ClientSession clientSession, Bson filter, FindOneAndDeleteOptions options) {
        return delegate.findOneAndDelete(clientSession, filter, options);
    }

    public TDocument findOneAndReplace(Bson filter, TDocument replacement) {
        return delegate.findOneAndReplace(clientSession, filter, replacement);
    }

    public TDocument findOneAndReplace(Bson filter, TDocument replacement, FindOneAndReplaceOptions options) {
        return delegate.findOneAndReplace(clientSession, filter, replacement, options);
    }

    public TDocument findOneAndReplace(ClientSession clientSession, Bson filter, TDocument replacement) {
        return delegate.findOneAndReplace(clientSession, filter, replacement);
    }

    public TDocument findOneAndReplace(ClientSession clientSession, Bson filter, TDocument replacement,
            FindOneAndReplaceOptions options) {
        return delegate.findOneAndReplace(clientSession, filter, replacement, options);
    }

    public TDocument findOneAndUpdate(Bson filter, Bson update) {
        return delegate.findOneAndUpdate(clientSession, filter, update);
    }

    public TDocument findOneAndUpdate(Bson filter, Bson update, FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(clientSession, filter, update, options);
    }

    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update) {
        return delegate.findOneAndUpdate(clientSession, filter, update);
    }

    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update,
            FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(clientSession, filter, update, options);
    }

    public TDocument findOneAndUpdate(Bson filter, List<? extends Bson> update) {
        return delegate.findOneAndUpdate(clientSession, filter, update);
    }

    public TDocument findOneAndUpdate(Bson filter, List<? extends Bson> update, FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(clientSession, filter, update, options);
    }

    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return delegate.findOneAndUpdate(clientSession, filter, update);
    }

    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, List<? extends Bson> update,
            FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(clientSession, filter, update, options);
    }

    public void drop() {
        delegate.drop(clientSession);
    }

    public void drop(ClientSession clientSession) {
        delegate.drop(clientSession);
    }

    public void drop(DropCollectionOptions dropCollectionOptions) {
        delegate.drop(clientSession, dropCollectionOptions);
    }

    public void drop(ClientSession clientSession, DropCollectionOptions dropCollectionOptions) {
        delegate.drop(clientSession, dropCollectionOptions);
    }

    public String createSearchIndex(String indexName, Bson definition) {
        return delegate.createSearchIndex(indexName, definition);
    }

    public String createSearchIndex(Bson definition) {
        return delegate.createSearchIndex(definition);
    }

    public List<String> createSearchIndexes(List<SearchIndexModel> searchIndexModels) {
        return delegate.createSearchIndexes(searchIndexModels);
    }

    public void updateSearchIndex(String indexName, Bson definition) {
        delegate.updateSearchIndex(indexName, definition);
    }

    public void dropSearchIndex(String indexName) {
        delegate.dropSearchIndex(indexName);
    }

    public ListSearchIndexesIterable<Document> listSearchIndexes() {
        return delegate.listSearchIndexes();
    }

    public <TResult> ListSearchIndexesIterable<TResult> listSearchIndexes(Class<TResult> resultClass) {
        return delegate.listSearchIndexes(resultClass);
    }

    public String createIndex(Bson keys) {
        return delegate.createIndex(clientSession, keys);
    }

    public String createIndex(Bson keys, IndexOptions indexOptions) {
        return delegate.createIndex(clientSession, keys, indexOptions);
    }

    public String createIndex(ClientSession clientSession, Bson keys) {
        return delegate.createIndex(clientSession, keys);
    }

    public String createIndex(ClientSession clientSession, Bson keys, IndexOptions indexOptions) {
        return delegate.createIndex(clientSession, keys, indexOptions);
    }

    public List<String> createIndexes(List<IndexModel> indexes) {
        return delegate.createIndexes(clientSession, indexes);
    }

    public List<String> createIndexes(List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
        return delegate.createIndexes(clientSession, indexes, createIndexOptions);
    }

    public List<String> createIndexes(ClientSession clientSession, List<IndexModel> indexes) {
        return delegate.createIndexes(clientSession, indexes);
    }

    public List<String> createIndexes(ClientSession clientSession, List<IndexModel> indexes,
            CreateIndexOptions createIndexOptions) {
        return delegate.createIndexes(clientSession, indexes, createIndexOptions);
    }

    public ListIndexesIterable<Document> listIndexes() {
        return delegate.listIndexes(clientSession);
    }

    public <TResult> ListIndexesIterable<TResult> listIndexes(Class<TResult> resultClass) {
        return delegate.listIndexes(clientSession, resultClass);
    }

    public ListIndexesIterable<Document> listIndexes(ClientSession clientSession) {
        return delegate.listIndexes(clientSession);
    }

    public <TResult> ListIndexesIterable<TResult> listIndexes(ClientSession clientSession, Class<TResult> resultClass) {
        return delegate.listIndexes(clientSession, resultClass);
    }

    public void dropIndex(String indexName) {
        delegate.dropIndex(clientSession, indexName);
    }

    public void dropIndex(String indexName, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(clientSession, indexName, dropIndexOptions);
    }

    public void dropIndex(Bson keys) {
        delegate.dropIndex(clientSession, keys);
    }

    public void dropIndex(Bson keys, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(clientSession, keys, dropIndexOptions);
    }

    public void dropIndex(ClientSession clientSession, String indexName) {
        delegate.dropIndex(clientSession, indexName);
    }

    public void dropIndex(ClientSession clientSession, Bson keys) {
        delegate.dropIndex(clientSession, keys);
    }

    public void dropIndex(ClientSession clientSession, String indexName, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(clientSession, indexName, dropIndexOptions);
    }

    public void dropIndex(ClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(clientSession, keys, dropIndexOptions);
    }

    public void dropIndexes() {
        delegate.dropIndexes(clientSession);
    }

    public void dropIndexes(ClientSession clientSession) {
        delegate.dropIndexes(clientSession);
    }

    public void dropIndexes(DropIndexOptions dropIndexOptions) {
        delegate.dropIndexes(clientSession, dropIndexOptions);
    }

    public void dropIndexes(ClientSession clientSession, DropIndexOptions dropIndexOptions) {
        delegate.dropIndexes(clientSession, dropIndexOptions);
    }

    public void renameCollection(MongoNamespace newCollectionNamespace) {
        delegate.renameCollection(clientSession, newCollectionNamespace);
    }

    public void renameCollection(MongoNamespace newCollectionNamespace,
            RenameCollectionOptions renameCollectionOptions) {
        delegate.renameCollection(clientSession, newCollectionNamespace, renameCollectionOptions);
    }

    public void renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace) {
        delegate.renameCollection(clientSession, newCollectionNamespace);
    }

    public void renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace,
            RenameCollectionOptions renameCollectionOptions) {
        delegate.renameCollection(clientSession, newCollectionNamespace, renameCollectionOptions);
    }
    
    private <T> MongoCollection<T> wrap(MongoCollection<T> collection) {
        return new MongoCollectionWrapperWithClientSession<T>(clientSession, collection);
    }
}
