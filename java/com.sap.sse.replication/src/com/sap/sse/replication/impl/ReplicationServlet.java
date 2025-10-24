package com.sap.sse.replication.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.json.simple.JSONObject;
import org.osgi.util.tracker.ServiceTracker;

import com.rabbitmq.client.Channel;
import com.sap.sse.ServerInfo;
import com.sap.sse.gateway.AbstractHttpServlet;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.replication.ReplicaDescriptor;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicablesProvider;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.replication.ReplicationServletActions;
import com.sap.sse.replication.ReplicationServletActions.Action;
import com.sap.sse.replication.ReplicationStatus;
import com.sap.sse.replication.interfaces.impl.ReplicaDescriptorImpl;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.util.HttpRequestUtils;
import com.sap.sse.util.impl.CountingOutputStream;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

/**
 * The servlet supports registering and de-registering a replica from a master and can send the serialized initial load
 * data of the {@link Replicable}s registered as replicable OSGi service from the master to a requesting replica. The
 * initial load is transmitted through a {@link RabbitOutputStream}, and the response to the servlet request is only the
 * name of the queue the replica can use to obtain the initial load from the message queuing system. This guarantees
 * a more robust transmission of the initial load, even in the face of a flaky connection between master and replica.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class ReplicationServlet extends AbstractHttpServlet {
    private static final Logger logger = Logger.getLogger(ReplicationServlet.class.getName());
    
    private static final long serialVersionUID = 4835516998934433846L;
    
    /**
     * The size of the packages into which the initial load is split
     */
    private static final int INITIAL_LOAD_PACKAGE_SIZE = 1024*1024;

    private final ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker;
    
    private final ReplicablesProvider replicablesProvider;

    public ReplicationServlet() throws Exception {
        this(new OSGiReplicableTracker(Activator.getDefaultContext()),
                new ServiceTracker<ReplicationService, ReplicationService>(Activator.getDefaultContext(),
                        ReplicationService.class.getName(), /* tracker customizer */ null));
    }

    public ReplicationServlet(ReplicablesProvider replicablesProvider, ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker) {
        this.replicablesProvider = replicablesProvider;
        this.replicationServiceTracker = replicationServiceTracker;
        if (replicationServiceTracker != null) {
            replicationServiceTracker.open();
        }
    }
    
    protected ReplicationService getReplicationService() {
        return replicationServiceTracker == null ? null : replicationServiceTracker.getService();
    }

    /**
     * The client identifies itself in the request. Two servlet operations are supported currently: registering the
     * client with the replication service (if not already created, the message exchange will be created by this
     * registration); and triggering sending an initial load stream through RabbitMQ that the replica can use to
     * initialize its {@link Replicable} s. The IDs of the replicables of which the initial load is to be sent is
     * expected as the {@link #REPLICABLES_IDS_AS_STRINGS_COMMA_SEPARATED} parameter value. The servlet response
     * consists of the name of the RabbitMQ queue name that the client can connect to and through which to receive
     * the LZ4-compressed initial load stream per replicable, using a {@link RabbitInputStreamProvider} and
     * an {@link LZ4BlockInputStream}..
     * <p>
     * 
     * The operation performed is selected by passing one of the {@link Action} enumeration values for the URL parameter
     * named {@link #ACTION}.
     */
    private void handleAction(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try {
            String action = req.getParameter(ReplicationServletActions.ACTION_PARAMETER_NAME);
            logger.info("Received replication-related request, action is "+action+", subject is "+
                    (SecurityUtils.getSubject()==null?null:SecurityUtils.getSubject().getPrincipal()));
            String[] replicableIdsAsStrings;
            switch (Action.valueOf(action)) {
            case REGISTER:
                logger.info("Received replica registration request");
                checkReplicatorPermission(ServerActions.REPLICATE);
                registerClientWithReplicationService(req, resp);
                break;
            case DEREGISTER:
                logger.info("Received replica deregistration request");
                checkReplicatorPermission(ServerActions.REPLICATE);
                deregisterClientWithReplicationService(req, resp);
                break;
            case INITIAL_LOAD:
                logger.info("Received replication initial load request");
                checkReplicatorPermission(ServerActions.REPLICATE);
                replicableIdsAsStrings = req.getParameter(ReplicationServletActions.REPLICABLES_IDS_AS_STRINGS_COMMA_SEPARATED_PARAMETER_NAME).split(",");
                Channel channel = getReplicationService().createMasterChannel();
                try {
                    RabbitOutputStream ros = new RabbitOutputStream(INITIAL_LOAD_PACKAGE_SIZE, channel,
                            /* queueName */ "initialLoad-for-"+HttpRequestUtils.getClientIP(req)+"@"+new Date()+"-"+UUID.randomUUID(),
                            /* syncAfterTimeout */ false);
                    PrintWriter br = new PrintWriter(new OutputStreamWriter(resp.getOutputStream()));
                    resp.setContentType("text/plain");
                    br.println(ros.getQueueName());
                    br.flush();
                    final CountingOutputStream countingOutputStream = new CountingOutputStream(
                            ros, /* log every megabyte */1024l * 1024l, Level.INFO,
                            "uncompressed output for initial load for " + HttpRequestUtils.getClientIP(req));
                    final LZ4BlockOutputStream compressingOutputStream = new LZ4BlockOutputStream(countingOutputStream);
                    for (String replicableIdAsString : replicableIdsAsStrings) {
                        logger.info("Serializing initial load for replicable "+replicableIdAsString+" for remote host "+HttpRequestUtils.getClientIP(req));
                        Replicable<?, ?> replicable = replicablesProvider.getReplicable(replicableIdAsString, /* wait */ false);
                        if (replicable == null) {
                            final String msg = "Couldn't find replicable with ID "+replicableIdAsString+". Aborting serialization of initial load.";
                            logger.severe(msg);
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, StringEscapeUtils.escapeHtml(msg));
                            break; // causing an error on the replica which is expecting the replica's initial load
                        }
                        try {
                            replicable.serializeForInitialReplication(compressingOutputStream);
                            logger.info("Done serializing initial load for replicable "+replicableIdAsString+" for remote host "+HttpRequestUtils.getClientIP(req));
                        } catch (Throwable e) {
                            logger.info("Error trying to serialize initial load for replication: " + e.getMessage());
                            logger.log(Level.SEVERE, "doGet", e);
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            resp.getWriter().append("Error obtaining status; see server logs for details.");
                        }
                    }
                    logger.info("Done serializing initial loads for remote host "+HttpRequestUtils.getClientIP(req));
                    compressingOutputStream.finish();
                    countingOutputStream.close();
                    break;
                } finally {
                    channel.getConnection().close();
                }
            case STATUS:
                // no permission check to read status; the same is available, e.g., through /gwt/status
                try {
                    reportStatus(req, resp);
                } catch (IllegalAccessException e) {
                    logger.info("Error obtaining replication status: " + e.getMessage());
                    logger.log(Level.SEVERE, "doGet", e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().append("Error obtaining status; see server logs for details.");
                }
                break;
            case STOP_REPLICATING:
                checkReplicatorPermission(ServerActions.REPLICATE);
                logger.info("Stopping replication from master upon "
                        +(SecurityUtils.getSubject()==null?null:SecurityUtils.getSubject().getPrincipal())
                        +"'s request.");
                getReplicationService().stopToReplicateFromMaster();
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_OK);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Action " + StringEscapeUtils.escapeHtml(action) + " not understood. Must be one of "
                        + Arrays.toString(Action.values()));
            }
        } catch (AuthorizationException e) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "The user is not authenticated or not permitted to manage replication. See server log for details.");
        }
    }
    
    private void checkReplicatorPermission(com.sap.sse.security.shared.HasPermissions.Action action) {
        SecurityUtils.getSubject()
                .checkPermission(SecuredSecurityTypes.SERVER.getStringPermissionForTypeRelativeIdentifier(action,
                        new TypeRelativeObjectIdentifier(ServerInfo.getName())));
    }

    /**
     * The status is reported as a JSON document. For each replicable it describes the status which tells
     * whether the replicable is still fetching its initial load, as well as the length of the queue of
     * inbound operations not yet processed. The JSON document is printed to the response object's writer.
     */
    private void reportStatus(HttpServletRequest req, HttpServletResponse resp) throws IllegalAccessException, IOException {
        final ReplicationStatus status = getReplicationService().getStatus();
        final JSONObject result = status.toJSONObject();
        resp.setContentType("application/json;charset=UTF-8");
        result.writeJSONString(resp.getWriter());
        if (status.isAvailable()) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Made public for test support
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter(ReplicationServletActions.ACTION_PARAMETER_NAME) != null) {
            try {
                handleAction(req, resp);
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "Exception occurred while trying to receive and apply operation initiated on replica", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Exception " + e
                        + " occurred while trying to receive and apply operation initiated on replica. Re-trying can make sense.");
            }
        } else {
            // no action --> a stream of serialized operations is expected in the POST request's body
            InputStream is = req.getInputStream();
            DataInputStream dis = new DataInputStream(is);
            String replicableIdAsString = dis.readUTF();
            Replicable<?, ?> replicable = replicablesProvider.getReplicable(replicableIdAsString, /* wait */ false);
            if (replicable != null) {
                logger.fine("Received request to apply and replicate an operation from a replica for replicable "+replicable);
                checkReplicatorPermission(ServerActions.REPLICATE);
                try {
                    applyOperationToReplicable(replicable, is);
                } catch (InvocationTargetException ite) {
                    Throwable originalException = ite.getTargetException();
                    if (originalException instanceof RuntimeException && originalException.getCause() != null) {
                        originalException = originalException.getCause();
                    }
                    logger.log(Level.SEVERE, "Unrecoverable error applying operation received from replica", originalException);
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Exception " + ite.getCause()
                            + " occurred while trying to receive and apply operation initiated on replica. Please do not re-send.");
                } catch (ClassNotFoundException cnfe) {
                    logger.log(Level.SEVERE,
                            "Exception occurred while trying to de-serialize operation", cnfe);
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Exception " + cnfe
                            + " occurred while trying to de-serialize operation initiated on replica. Please do not re-send");
                } catch (IOException ioe) {
                    logger.log(Level.SEVERE,
                            "Exception occurred while trying to receive and apply operation initiated on replica", ioe);
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Exception " + ioe
                            + " occurred while trying to receive and apply operation initiated on replica. Re-trying can make sense.");
                }
            } else {
                logger.warning("Received operation for replicable "+replicableIdAsString+
                        ", but a replicable with that ID couldn't be found. Ignoring the operation.");
            }
        }
    }

    /**
     * Applies <code>operation</code> to the replicable identified by <code>replicableIdAsString</code>. If such a
     * replicable cannot be found on this server instance, a warning is logged and the operation is ignored.
     * 
     * @throws ClassNotFoundException
     *             in case during the de-serialization of the operation received in the input stream {@code is} a type
     *             cannot be resolved. This then is a permanent error and it makes little sense for a replica trying to
     *             re-send the operation.
     * 
     * @throws IOException
     *             in case something went wrong with the de-serialization of the operation; there is a good chance for
     *             the client that sending it again succeeds because such an exception could mean an interrupted
     *             connection.
     *             
     * @throws InvocationTargetException
     *             in case an exception was thrown while executing the operation after successfully receiving and
     *             de-serializing it. Again, there is little sense in sending it again. The operation failed, hopefully
     *             it produced some useful log entry and the replica may choose to log this event, too, but it should
     *             not try sending the operation again, and in particular not at the expense of later operations that
     *             otherwise may keep queued forever (see also bug 5117). 
     */
    private <S, R, O extends OperationWithResult<S, ?>> void applyOperationToReplicable(Replicable<S, O> replicable, InputStream is)
            throws ClassNotFoundException, IOException, InvocationTargetException {
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(replicable.getClass().getClassLoader());
        final O operation;
        try {
            operation = replicable.readOperation(is, /* classLoaderCache */ new HashMap<>());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error trying to de-serialize an operation for replicable "+replicable.getId(), e);
            throw e;
        }
        Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        logger.fine("Applying operation of type " + operation.getClassForLogging().getName()
                + (operation.getOriginServerName() == null ? "" : (" originating from server \""+operation.getOriginServerName()+"\""))
                + " received from replica to replicable " + replicable.toString());
        try {
            replicable.apply(operation);
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }

    private void deregisterClientWithReplicationService(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final UUID replicaUuid = UUID.fromString(req.getParameter(ReplicationServletActions.SERVER_UUID_PARAMETER_NAME));
        final ReplicaDescriptor replica = getReplicationService().unregisterReplica(replicaUuid);
        if (replica != null) {
            logger.info("Deregistered replication client with this server " + replica.getIpAddress());
            resp.setContentType("text/plain");
            resp.getWriter().print(replica.getUuid());
        } else {
            logger.warning("Couldn't find replica to de-register with ID "+replicaUuid);
        }
    }

    private void registerClientWithReplicationService(HttpServletRequest req, HttpServletResponse resp)
            throws Exception {
        final ReplicaDescriptor replica = getReplicaDescriptor(req);
        getReplicationService().registerReplica(replica);
        logger.info("Registered new replica " + replica);
        resp.setContentType("text/plain");
        resp.getWriter().print(replica.getUuid());
    }

    private ReplicaDescriptor getReplicaDescriptor(HttpServletRequest req) throws UnknownHostException {
        final String forwardedFor = req.getHeader("X-Forwarded-For"); // could have come through a load balancer / reverse proxy
        final InetAddress ipAddress = forwardedFor != null && !forwardedFor.trim().isEmpty()
                ? InetAddress.getByName(forwardedFor.split(",")[0].trim())
                : InetAddress.getByName(req.getRemoteAddr());
        final UUID uuid = UUID.fromString(req.getParameter(ReplicationServletActions.SERVER_UUID_PARAMETER_NAME));
        final String additional = req.getParameter(ReplicationServletActions.ADDITIONAL_INFORMATION_PARAMETER_NAME);
        final String[] replicableIdsAsStrings = req.getParameter(ReplicationServletActions.REPLICABLES_IDS_AS_STRINGS_COMMA_SEPARATED_PARAMETER_NAME).split(",");
        final Integer port = req.getParameter(ReplicationServletActions.PORT_NAME) == null ? null : Integer.valueOf(req.getParameter(ReplicationServletActions.PORT_NAME));
        return new ReplicaDescriptorImpl(ipAddress, port, uuid, additional, replicableIdsAsStrings);
    }
}
