package com.sap.sse.landscape.mongodb;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import com.sap.sse.common.Named;

public interface Database extends Named {
    /**
     * Obtains the database-specific connection URI that clients can use to connect to the replica set hosting this
     * database. See also {@link MongoReplicaSet#getConnectionURI()}.
     */
    default URI getConnectionURI() throws URISyntaxException {
        return getEndoint().getURI(Optional.of(this));
    }
    
    MongoEndpoint getEndoint();
    
    /**
     * @return the collections currently stored in this database
     */
    Iterable<Collection> getCollections();
    
    String getMD5Hash();
    
    /**
     * Drops this database with all its collections and all their contents. Use with care!
     */
    void drop();
}
