package com.sap.sse.landscape.aws;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.aws.impl.AwsInstanceImpl;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.landscape.mongodb.MongoProcess;
import com.sap.sse.landscape.mongodb.MongoProcessInReplicaSet;
import com.sap.sse.landscape.mongodb.MongoReplicaSet;
import com.sap.sse.landscape.mongodb.impl.MongoProcessImpl;
import com.sap.sse.landscape.mongodb.impl.MongoProcessInReplicaSetImpl;
import com.sap.sse.landscape.mongodb.impl.MongoReplicaSetImpl;

/**
 * Parses a MongoDB URI in the format <tt>"mongodb://{host[:port]}[,{host[:port]},...]/{databaseName}[?{key}={value}[,{key}={value},...]]"</tt>. The
 * database name is required. If one of the <tt>{key}={value}</tt> pairs uses the key {@code "replicaSet"} then the resulting {@link Database}'s
 * {@link Database#getEndpoint() endpoint} will be a {@link MongoReplicaSet} whose {@link MongoReplicaSet#getName() name} is determined
 * by the <tt>{value}</tt> of the {@code "replicaSet"} parameter; otherwise the result is a {@link MongoProcess}.
 */
public class MongoUriParser<ShardingKey> {
    private final static String SCHEME = "mongodb";
    private final AwsLandscape<ShardingKey> landscape;
    private final Region region;
    
    public MongoUriParser(AwsLandscape<ShardingKey> landscape, Region region) {
        super();
        this.landscape = landscape;
        this.region = region;
    }

    public Database parseMongoUri(String mongoUri) throws URISyntaxException, UnknownHostException {
        final MongoEndpoint endpoint;
        final URI mongoUriAsUri = new URI(mongoUri);
        if (!Util.equalsWithNull(SCHEME, mongoUriAsUri.getScheme())) {
            throw new IllegalArgumentException("Expected scheme "+SCHEME);
        }
        final String[] hostnamesAndOptionalPorts = mongoUriAsUri.getAuthority().split(",");
        final String dbName = mongoUriAsUri.getPath().replaceAll("^/", "");
        if (!Util.hasLength(dbName)) {
            throw new IllegalArgumentException("Expected non-empty database name");
        }
        final String query = mongoUriAsUri.getQuery();
        String replicaSetName = null;
        if (query != null) {
            for (final String keyValuePair : query.split("&")) {
                final String[] keyAndValue = keyValuePair.split("=", 2);
                if (Util.equalsWithNull(keyAndValue[0], "replicaSet")) {
                    replicaSetName = keyAndValue[1];
                }
            }
        }
        if (replicaSetName != null) {
            MongoReplicaSet replicaSet = new MongoReplicaSetImpl(replicaSetName);
            for (final String hostnameAndOptionalPort : hostnamesAndOptionalPorts) {
                final MongoProcessInReplicaSet mongoProcessInReplicaSet = getMongoProcessInReplicaSet(replicaSet, hostnameAndOptionalPort);
                if (mongoProcessInReplicaSet != null) {
                    replicaSet.addReplica(mongoProcessInReplicaSet);
                }
            }
            endpoint = replicaSet;
        } else {
            endpoint = getMongoProcess(hostnamesAndOptionalPorts[0]);
        }
        return endpoint == null ? null : endpoint.getDatabase(dbName);
    }
    
    public Database parseMongoDBConfigurationFromStatus(JSONObject mongoDBConfigurationJSON) throws UnknownHostException {
        final String databaseName = mongoDBConfigurationJSON.get("database").toString();
        final String replicaSetName = mongoDBConfigurationJSON.get("replicaSet").toString();
        final MongoEndpoint endpoint;
        final List<Pair<AwsInstance<ShardingKey>, Integer>> hostnamesAndPorts = new ArrayList<>();
        final JSONArray serversJSON = (JSONArray) mongoDBConfigurationJSON.get("servers");
        for (final Object serverObject : serversJSON) {
            final JSONObject serverJSON = (JSONObject) serverObject;
            final String hostname = serverJSON.get("host").toString();
            final int port = ((Number) serverJSON.get("port")).intValue();
            hostnamesAndPorts.add(getHostAndPort(hostname, port));
        }
        if (replicaSetName != null) {
            MongoReplicaSet replicaSet = new MongoReplicaSetImpl(replicaSetName);
            for (final Pair<AwsInstance<ShardingKey>, Integer> hostnameAndPort : hostnamesAndPorts) {
                final MongoProcessInReplicaSet mongoProcessInReplicaSet = getMongoProcessInReplicaSet(replicaSet, hostnameAndPort);
                if (mongoProcessInReplicaSet != null) {
                    replicaSet.addReplica(mongoProcessInReplicaSet);
                }
            }
            endpoint = replicaSet;
        } else {
            endpoint = getMongoProcess(hostnamesAndPorts.get(0));
        }
        return endpoint == null ? null : endpoint.getDatabase(databaseName);

    }

    private MongoEndpoint getMongoProcess(final String hostnameAndOptionalPort) throws UnknownHostException {
        Pair<AwsInstance<ShardingKey>, Integer> hostAndOptionalPort = getHostAndPort(hostnameAndOptionalPort);
        return getMongoProcess(hostAndOptionalPort);
    }
    
    private MongoEndpoint getMongoProcess(Pair<AwsInstance<ShardingKey>, Integer> hostAndOptionalPort) {
        final MongoEndpoint endpoint;
        if (hostAndOptionalPort.getA() != null) {
            if (hostAndOptionalPort.getB() != null) {
                endpoint = new MongoProcessImpl(hostAndOptionalPort.getA(), hostAndOptionalPort.getB());
            } else {
                endpoint = new MongoProcessImpl(hostAndOptionalPort.getA());
            }
        } else { // host not found in landscape anymore; maybe replica set was re-configured
            endpoint = null;
        }
        return endpoint;
    }
    
    private MongoProcessInReplicaSet getMongoProcessInReplicaSet(final MongoReplicaSet replicaSet, final String hostnameAndOptionalPort) throws UnknownHostException {
        Pair<AwsInstance<ShardingKey>, Integer> hostAndOptionalPort = getHostAndPort(hostnameAndOptionalPort);
        return getMongoProcessInReplicaSet(replicaSet, hostAndOptionalPort);
    }
    
    private MongoProcessInReplicaSet getMongoProcessInReplicaSet(final MongoReplicaSet replicaSet, Pair<AwsInstance<ShardingKey>, Integer> hostAndOptionalPort) {
        final MongoProcessInReplicaSet endpoint;
        if (hostAndOptionalPort.getA() != null) {
            if (hostAndOptionalPort.getB() != null) {
                endpoint = new MongoProcessInReplicaSetImpl(replicaSet, hostAndOptionalPort.getB(), hostAndOptionalPort.getA());
            } else {
                endpoint = new MongoProcessInReplicaSetImpl(replicaSet, hostAndOptionalPort.getA());
            }
        } else { // host not found in landscape anymore; maybe replica set was re-configured
            endpoint = null;
        }
        return endpoint;
    }
    
    /**
     * If the host isn't found in the landscape, the {@link Pair#getA()} component of the pair returned will be {@code null}.
     */
    private Pair<AwsInstance<ShardingKey>, Integer> getHostAndPort(String hostnameAndOptionalPort) throws UnknownHostException {
        final String[] hostnameAndOptionalPortSplit = hostnameAndOptionalPort.split(":");
        final String hostname = hostnameAndOptionalPortSplit[0];
        final Integer port = hostnameAndOptionalPortSplit.length<2?null:Integer.valueOf(hostnameAndOptionalPortSplit[1]);
        return getHostAndPort(hostname, port);
    }

    /**
     * If the host isn't found in the landscape, the {@link Pair#getA()} component of the pair returned will be {@code null}.
     */
    private Pair<AwsInstance<ShardingKey>, Integer> getHostAndPort(String hostname, Integer optionalPort) throws UnknownHostException {
        final AwsInstance<ShardingKey> hostByPrivateDnsOrIp = landscape.getHostByPrivateDnsNameOrIpAddress(region, hostname, AwsInstanceImpl::new);
        return new Pair<>(hostByPrivateDnsOrIp, optionalPort);
    }
}
