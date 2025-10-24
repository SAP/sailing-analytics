package com.sap.sse.security.userstore.mongodb.impl.sessionwrapper;

import java.util.List;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.ListCollectionsIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.CreateViewOptions;

public class MongoDatabaseWrapperWithClientSession implements MongoDatabase {
    private final MongoDatabase delegate;
    private final ClientSession clientSession;

    public MongoDatabaseWrapperWithClientSession(ClientSession clientSession, MongoDatabase delegate) {
        super();
        this.delegate = delegate;
        this.clientSession = clientSession;
    }

    public String getName() {
        return delegate.getName();
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

    public MongoDatabase withCodecRegistry(CodecRegistry codecRegistry) {
        return wrap(delegate.withCodecRegistry(codecRegistry));
    }

    public MongoDatabase withReadPreference(ReadPreference readPreference) {
        return wrap(delegate.withReadPreference(readPreference));
    }

    public MongoDatabase withWriteConcern(WriteConcern writeConcern) {
        return wrap(delegate.withWriteConcern(writeConcern));
    }

    public MongoDatabase withReadConcern(ReadConcern readConcern) {
        return wrap(delegate.withReadConcern(readConcern));
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        return new MongoCollectionWrapperWithClientSession<Document>(clientSession, delegate.getCollection(collectionName));
    }

    public <TDocument> MongoCollection<TDocument> getCollection(String collectionName, Class<TDocument> documentClass) {
        return new MongoCollectionWrapperWithClientSession<TDocument>(clientSession, delegate.getCollection(collectionName, documentClass));
    }

    public Document runCommand(Bson command) {
        return delegate.runCommand(command);
    }

    public Document runCommand(Bson command, ReadPreference readPreference) {
        return delegate.runCommand(command, readPreference);
    }

    public <TResult> TResult runCommand(Bson command, Class<TResult> resultClass) {
        return delegate.runCommand(command, resultClass);
    }

    public <TResult> TResult runCommand(Bson command, ReadPreference readPreference, Class<TResult> resultClass) {
        return delegate.runCommand(command, readPreference, resultClass);
    }

    public Document runCommand(ClientSession clientSession, Bson command) {
        return delegate.runCommand(clientSession, command);
    }

    public Document runCommand(ClientSession clientSession, Bson command, ReadPreference readPreference) {
        return delegate.runCommand(clientSession, command, readPreference);
    }

    public <TResult> TResult runCommand(ClientSession clientSession, Bson command, Class<TResult> resultClass) {
        return delegate.runCommand(clientSession, command, resultClass);
    }

    public <TResult> TResult runCommand(ClientSession clientSession, Bson command, ReadPreference readPreference,
            Class<TResult> resultClass) {
        return delegate.runCommand(clientSession, command, readPreference, resultClass);
    }

    public void drop() {
        delegate.drop(clientSession);
    }

    public void drop(ClientSession clientSession) {
        delegate.drop(clientSession);
    }

    public MongoIterable<String> listCollectionNames() {
        return delegate.listCollectionNames(clientSession);
    }

    public ListCollectionsIterable<Document> listCollections() {
        return delegate.listCollections(clientSession);
    }

    public <TResult> ListCollectionsIterable<TResult> listCollections(Class<TResult> resultClass) {
        return delegate.listCollections(clientSession, resultClass);
    }

    public MongoIterable<String> listCollectionNames(ClientSession clientSession) {
        return delegate.listCollectionNames(clientSession);
    }

    public ListCollectionsIterable<Document> listCollections(ClientSession clientSession) {
        return delegate.listCollections(clientSession);
    }

    public <TResult> ListCollectionsIterable<TResult> listCollections(ClientSession clientSession,
            Class<TResult> resultClass) {
        return delegate.listCollections(clientSession, resultClass);
    }

    public void createCollection(String collectionName) {
        delegate.createCollection(clientSession, collectionName);
    }

    public void createCollection(String collectionName, CreateCollectionOptions createCollectionOptions) {
        delegate.createCollection(clientSession, collectionName, createCollectionOptions);
    }

    public void createCollection(ClientSession clientSession, String collectionName) {
        delegate.createCollection(clientSession, collectionName);
    }

    public void createCollection(ClientSession clientSession, String collectionName,
            CreateCollectionOptions createCollectionOptions) {
        delegate.createCollection(clientSession, collectionName, createCollectionOptions);
    }

    public void createView(String viewName, String viewOn, List<? extends Bson> pipeline) {
        delegate.createView(clientSession, viewName, viewOn, pipeline);
    }

    public void createView(String viewName, String viewOn, List<? extends Bson> pipeline,
            CreateViewOptions createViewOptions) {
        delegate.createView(clientSession, viewName, viewOn, pipeline, createViewOptions);
    }

    public void createView(ClientSession clientSession, String viewName, String viewOn, List<? extends Bson> pipeline) {
        delegate.createView(clientSession, viewName, viewOn, pipeline);
    }

    public void createView(ClientSession clientSession, String viewName, String viewOn, List<? extends Bson> pipeline,
            CreateViewOptions createViewOptions) {
        delegate.createView(clientSession, viewName, viewOn, pipeline, createViewOptions);
    }

    public ChangeStreamIterable<Document> watch() {
        return delegate.watch(clientSession);
    }

    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> resultClass) {
        return delegate.watch(clientSession, resultClass);
    }

    public ChangeStreamIterable<Document> watch(List<? extends Bson> pipeline) {
        return delegate.watch(clientSession, pipeline);
    }

    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> pipeline, Class<TResult> resultClass) {
        return delegate.watch(clientSession, pipeline, resultClass);
    }

    public ChangeStreamIterable<Document> watch(ClientSession clientSession) {
        return delegate.watch(clientSession);
    }

    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> resultClass) {
        return delegate.watch(clientSession, resultClass);
    }

    public ChangeStreamIterable<Document> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return delegate.watch(clientSession, pipeline);
    }

    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<TResult> resultClass) {
        return delegate.watch(clientSession, pipeline, resultClass);
    }

    public AggregateIterable<Document> aggregate(List<? extends Bson> pipeline) {
        return delegate.aggregate(clientSession, pipeline);
    }

    public <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> pipeline, Class<TResult> resultClass) {
        return delegate.aggregate(clientSession, pipeline, resultClass);
    }

    public AggregateIterable<Document> aggregate(ClientSession clientSession, List<? extends Bson> pipeline) {
        return delegate.aggregate(clientSession, pipeline);
    }

    public <TResult> AggregateIterable<TResult> aggregate(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<TResult> resultClass) {
        return delegate.aggregate(clientSession, pipeline, resultClass);
    }
    
    private MongoDatabase wrap(MongoDatabase db) {
        return new MongoDatabaseWrapperWithClientSession(clientSession, db);
    }
}
