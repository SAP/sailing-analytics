package com.sap.sse.security.storemerging.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

/**
 * Can fill collections from JSON files as, e.g., obtained from a {@code mongoexport} command.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class MongoDBFiller {
    public void fill(MongoCollection<Document> collection, String resourceName) throws IOException {
        final BufferedReader resourceReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourceName)));
        String line;
        while ((line=resourceReader.readLine()) != null) {
            final Document document = Document.parse(line);
            collection.insertOne(document);
        }
    }
}
