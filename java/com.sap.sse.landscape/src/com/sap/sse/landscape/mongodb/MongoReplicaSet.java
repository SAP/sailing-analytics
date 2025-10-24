package com.sap.sse.landscape.mongodb;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Named;
import com.sap.sse.landscape.common.shared.MongoDBConstants;

public interface MongoReplicaSet extends Named, MongoEndpoint {
    static Logger logger = Logger.getLogger(MongoReplicaSet.class.getName());
    
    Iterable<MongoProcessInReplicaSet> getInstances();
    
    /**
     * The {@code "mongodb://..."} URI that application use to connect to this replica set; not specific
     * to any particular database managed by this replica set; see also {@link Database#getConnectionURI()}.
     */
    @Override
    default URI getURI(Optional<Database> optionalDb) throws URISyntaxException {
        return getURI(optionalDb, mongoProcess->mongoProcess.getHost().getHostname());
    }

    default URI getURI(Optional<Database> optionalDb, Function<MongoProcess, String> addressSupplier) throws URISyntaxException {
        final StringBuilder result = new StringBuilder("mongodb://");
        final List<String> hostSpecs = new ArrayList<>();
        for (final MongoProcess mongoProcess : getInstances()) {
            final String hostname = addressSupplier.apply(mongoProcess);
            if (hostname != null) {
                logger.fine("Adding MongoDB process running on "+hostname+" to replica set "+this.getName());
                final StringBuilder hostSpec = new StringBuilder();
                hostSpec.append(hostname);
                if (mongoProcess.getPort() != MongoDBConstants.DEFAULT_PORT) {
                    hostSpec.append(":");
                    hostSpec.append(mongoProcess.getPort());
                }
                hostSpecs.add(hostSpec.toString());
            } else {
                logger.warning("Not adding MongoDB process running on instance "+mongoProcess.getHost()+" to replica set "+this.getName()+
                        " because its IP address cannot be determined. Probably it is not running.");
            }
        }
        result.append(String.join(",", hostSpecs));
        result.append("/");
        optionalDb.ifPresent(db->result.append(db.getName()));
        appendReplicaSetParametersToURI(getName(), result);
        return new URI(result.toString());
    }

    @Override
    default URI getURI(Optional<Database> optionalDb, Optional<Duration> timeoutEmptyMeansForever) throws URISyntaxException {
        return getURI(optionalDb, mongoProcess->{
            try {
                return mongoProcess.getHost().getPublicAddress(timeoutEmptyMeansForever).getHostAddress();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    void addReplica(MongoProcessInReplicaSet newReplica);
    
    void removeReplica(MongoProcessInReplicaSet replicaToRemove);
}
