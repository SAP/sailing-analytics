package com.sap.sse.replication.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.operationaltransformation.Operation;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicationReceiver;
import com.sap.sse.replication.ReplicationService;

import net.jpountz.lz4.LZ4BlockOutputStream;

/**
 * Encapsulates the serialization (using an {@link ObjectOutputStream}) and the buffering with delayed sending for a
 * sequence of {@link Operation}s. For sending, a {@link Pair} of {@code byte[]} and {@code List<Class<?>>} is
 * {@link Deque#add(Object) added} to the {@link Deque} passed to the constructor. This assumes that a thread is trying
 * to {@link BlockingQueue#take() take} elements from that queue to send them out one by one to the replicas, logging
 * the metrics about the types of objects being sent this way.
 * <p>
 * 
 * Buffering is limited by two factors: maximum message size and maximum buffering duration. Limiting the message size
 * avoids clogging the heap with buffered messages and respects the underlying messaging framework's maximum message
 * size. Limiting the buffering duration even in the face of a continuous stream of incoming operations helps keeping
 * the replication delay below some threshold.
 * <p>
 * 
 * Besides the {@link Deque} to which to send complete messages, two other important objects form the core of the state
 * of this serializing buffer:
 * <ul>
 * <li>An {@link ObjectOutputStream} constructed freshly for each binary message assembled; it can serialize multiple
 * operations which has the benefit of reducing the metadata overhead (type information) that needs to be written to the
 * stream and of increasing sharing potential for common strings.</li>
 * <li>A {@link ByteArrayOutputStream} into which the {@link ObjectOutputStream} serializes the {@link Operation}s and
 * whose contents will be sent as the {@code byte[]} to the {@link Deque} passed to the constructor.</li>
 * </ul>
 * 
 * Time-limiting the buffering process is supported by a {@link Timer} passed to the constructor, and a
 * {@link TimerTask} constructed by an instance of this class once the first operation has been serialized to a new
 * buffer. The {@link TimerTask} is scheduled to run after the replication buffering delay, passed as a configuration
 * parameter to the constructor, too.
 * <p>
 * 
 * This serializing buffer is thread-safe. Threads can write operations to it at any time after object construction has
 * finished. The implementation takes care of synchronizing those writes and the subsequent object serialization to the
 * {@code byte[]} buffer with each other and with sending and clearing the buffer. Objects of this class can be
 * "recycled" through any number of message sends; "sending" another message by adding it to the {@link Deque} passed to
 * the constructor will empty the buffer; the next operation written to this buffer will create a new
 * {@link ObjectOutputStream} and {@code byte[]} buffer.
 * <p>
 * 
 * Use multiple instances of this class in conjunction with a thread pool to send out operations that allow for
 * {@link Operation#requiresSynchronousExecution() asynchronous execution} on the replica side. For these operations the
 * rather expensive object serialization may run in parallel, thus increasing replication throughput which in typical
 * configurations is limited by the single-threaded {@link ObjectOutputStream} and compression throughput and not so
 * much by the bandwidth offered by the messaging infrastructure. One instance should be reserved for operations that
 * {@link Operation#requiresSynchronousExecution() require synchronous execution}. Those need to be applied in the order
 * in which they were received by the {@link ReplicationService}. The number of additional such buffers should be
 * determined by the typical thread pool size (e.g., equaling the number of CPUs available to the VM). Operations
 * allowing for asynchronous execution can then be serialized truly in parallel, and their serialization may even happen
 * outside the thread requesting their broadcast. This can accelerate, e.g., the loading process of tracking data on the
 * master because the loading process no longer has to wait for the serialization to finish but may enqueue the
 * operations for serialization.
 * <p>
 * 
 * <em>Implementation Quirk:</em> When a buffer is sent because it reached the maximum message size, the
 * {@link TimerTask} scheduled will still fire at some later point; this will lead to a sending delay that can be
 * shorter than the timeout configured.
 * <p>
 * 
 * Invariants when outside of a {@code synchronized} block using this object's monitor:
 * <ul>
 * <li>If and only if there is a valid {@link #objectOutputStream} to which the next operation can be written, the
 * {@link #replicableIdAsString} is properly set.</li>
 * <li>The {@link #objectOutputStream}, the {@link #listOfClasses} and the {@link #bos} byte-array output stream hold
 * equivalent content; objects serialized through the output output stream are serialized to the byte-array output
 * stream</li>
 * <li>When there is at least one operation serialized in the streams, a {@link #timerTask} is scheduled with the
 * {@link #timer} which will guarantee sending the buffer after the {@link #timeout} has expired since writing the first
 * operation to the streams.</li>
 * </ul>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class OperationSerializerBufferImpl implements OperationSerializerBuffer {
    private static final Logger logger = Logger.getLogger(OperationSerializerBufferImpl.class.getName());
    
    /**
     * The replication protocol version (the only one supported) writes a
     * {@link ReplicationReceiver#VERSION_INDICATOR version indicator}, followed by a single byte containing this
     * protocol version, then followed by the replicable's ID as a {@link String}. Then, all operations follow in a
     * single {@link ObjectOutputStream} format. This has become possible by guaranteeing that {@link Operation}
     * objects do not contain references to objects that may change state after the operation has been scheduled for
     * sending.
     */
    private static final int PROTOCOL_VERSION = 2;

    /**
     * The {@link #objectOutputStream} contains serialized operations originating from a single
     * {@link Replicable} whose {@link Replicable#getId() ID} is written as its string value to the beginning of the
     * {@link #bos} stream using {@link DataOutputStream#writeUTF(String)}. When an operation of a {@link Replicable} with a
     * different ID is to be {@link #write(OperationWithResult) written}, the existing
     * {@link #bos} contents need to be transmitted and a new stream is started for the {@link Replicable} now wanting
     * to replicate an operation.
     */
    private String replicableIdAsString;
    
    private final ReplicationMessageSender sender;
    
    /**
     * An output stream that is initialized by the constructor and re-used (see {@link ByteArrayOutputStream#reset()}
     * thereafter.<p>
     * 
     * To send operations as serialized Java objects using binary RabbitMQ messages comes at an overhead. To reduce the
     * overhead, several operations can be serialized into a single message. The actual serialization of the buffer
     * happens after a short duration has passed since the last sending, managed by a {@link #timer} or when the
     * {@link #maximumBufferSizeInBytes} message size has been exceeded.
     */
    private final ByteArrayOutputStream bos;
    
    private DataOutputStream dos;
    
    /**
     * An object output stream that writes to {@link #bos}, through a compressing output stream. Operations are
     * serialized into this stream until the timer acquires the {@link #outboundBufferMonitor}, closes the stream and
     * transmits the contents of {@link #outboundBuffer} as a RabbitMQ message. While still holding the monitor, the
     * timer task creates a new {@link #outboundBuffer} and a new {@link #outboundObjectBuffer} wrapping the
     * {@link #outboundBuffer}.
     */
    private ObjectOutputStream objectOutputStream;
    
    /**
     * Remembers the classes of the operations serialized into {@link #objectOutputStream}. The list of classes in this
     * list matches with the sequence of objects written to {@link #objectOutputStream}. Used for logging and metrics.
     * See, e.g., {@link ReplicationInstancesManager#log(List, long)}. Passed to
     * {@link #broadcastOperations(byte[], List)} when sending out the current buffer.
     */
    private List<Class<?>> listOfClasses;
    
    private final Duration timeout;
    
    private final int maximumBufferSizeInBytes;
    
    /**
     * Used to schedule the sending of all operations in {@link #outboundBuffer} using the {@link #sendingTask}.
     */
    private final Timer timer;
    
    /**
     * Sends all operations in {@link #objectOutputStream}. While holding {@code this} object's monitor, the following
     * rules apply:
     * 
     * <ul>
     * <li>if <code>null</code>, adding an operation to {@link #objectOutputStream} needs to schedules a sending task with {@link #timer}.</li>
     * <li>if not <code>null</code>, an operation added to {@link #outboundBuffer} is guaranteed to be sent by the timer
     * </li>
     * </ul>
     */
    private TimerTask timerTask;
    
    public OperationSerializerBufferImpl(final ReplicationMessageSender sender, final Duration timeout,
            final int maximumBufferSizeInBytes, final Timer timer) throws IOException {
        this.sender = sender;
        this.bos = new ByteArrayOutputStream();
        this.timeout = timeout;
        this.timer = timer;
        this.maximumBufferSizeInBytes = maximumBufferSizeInBytes;
    }
    
    /**
     * Creates a new compressing object output stream which writes its compressed contents into {@link #bos} which is
     * {@link ByteArrayOutputStream#reset()reset} before creating the new object output stream. A new empty
     * {@link #listOfClasses} is created, too. As another side effect, cancels any scheduled {@link #timerTask} and
     * nulls it.
     */
    private synchronized void createNewObjectOutputStream(String replicableIdAsString) throws IOException {
        this.bos.reset();
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        listOfClasses = new ArrayList<>();
        LZ4BlockOutputStream zipper = new LZ4BlockOutputStream(this.bos);
        dos = new DataOutputStream(zipper);
        this.replicableIdAsString = replicableIdAsString;
        dos.writeUTF(ReplicationReceiver.VERSION_INDICATOR);
        dos.write(PROTOCOL_VERSION);
        dos.writeUTF(replicableIdAsString);
        final ObjectOutputStream compressingObjectOutputStream = new ObjectOutputStream(zipper);
        objectOutputStream = compressingObjectOutputStream;
    }
    
    /**
     * Extracts the binary message from {@link #bos} and, together with the {@link #listOfClasses} representing the
     * types of the objects that were written to the buffer since its creation or last sending, enqueues it for
     * sending out to the replicas by appending it to the {@link #queueToWriteTo} queue.
     */
    private synchronized void sendBuffer() throws IOException {
        if (replicableIdAsString != null && !listOfClasses.isEmpty()) {
            logger.fine("Preparing " + listOfClasses.size() + " operations for sending to RabbitMQ exchange");
            try {
                objectOutputStream.close();
                objectOutputStream = null;
                logger.fine("Sucessfully closed ObjectOutputStream");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error trying to replicate " + listOfClasses.size() + " operations", e);
            }
            final byte[] message = bos.toByteArray();
            logger.fine(()->"Successfully produced message array for replicable "+replicableIdAsString+" of length " + message.length);
            final List<Class<?>> listOfClasses = this.listOfClasses;
            this.listOfClasses = null;
            sender.send(message, listOfClasses);
        }
        replicableIdAsString = null;
    }
    
    /**
     * Serializes the operation into this buffer synchronously. If the {@link #maximumBufferSizeInBytes} is exceeded by
     * this, the buffer is sent. If this was the first operation to be serialized into a fresh object output stream, a
     * new {@link #timerTask} is scheduled which will send the buffer after {@link #timeout} even if the maximum size has not
     * been reached or exceeded until then.
     */
    public synchronized <S, O extends OperationWithResult<S, ?>> void write(final OperationWithResult<?, ?> operation, Replicable<S, O> replicable) throws IOException {
        if (replicableIdAsString == null) {
            createNewObjectOutputStream(replicable.getId().toString());
        } else if (!replicableIdAsString.equals(replicable.getId().toString())) {
            logger.fine(()->"Received operation for replicable "+replicable.getId().toString()+" which is different from "+replicableIdAsString+"; sending buffer first");
            sendBuffer();
            createNewObjectOutputStream(replicable.getId().toString());
        }
        objectOutputStream.writeObject(operation);
        listOfClasses.add(operation.getClassForLogging());
        if (bos.size() > maximumBufferSizeInBytes) {
            logger.info("Triggering replication for replicable ID "+replicableIdAsString+" because buffer holds " + bos.size()
                    + " bytes which exceeds trigger size " + maximumBufferSizeInBytes + " bytes");
            sendBuffer();
        } else {
            if (timerTask == null) {
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (OperationSerializerBufferImpl.this) {
                            try {
                                timerTask = null;
                                logger.fine("Running timer task for replicable ID "+replicableIdAsString+", flushing buffer");
                                sendBuffer();
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Exception while trying to replicate operations", e);
                            }
                        }
                    }
                };
                timer.schedule(timerTask, timeout.asMillis());
            }
        }
    }

    /**
     * @return the number of operations currently in the buffer
     */
    public int size() {
        return listOfClasses == null ? 0 : listOfClasses.size();
    }
}
