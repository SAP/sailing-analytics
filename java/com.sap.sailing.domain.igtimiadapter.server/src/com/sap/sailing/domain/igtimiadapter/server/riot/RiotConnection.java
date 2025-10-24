package com.sap.sailing.domain.igtimiadapter.server.riot;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import com.google.protobuf.CodedInputStream;
import com.igtimi.IgtimiStream.Msg;
import com.sap.sse.common.TimePoint;

public interface RiotConnection extends Closeable {
    String getSerialNumber();

    String getDeviceGroupToken();
    
    /**
     * The connected socket channel through which data can be read from and sent to the
     * device connected through this connection.
     */
    SocketChannel getSocketChannel();
    
    /**
     * Evaluates the {@code data} that was read from this connection's {@link SocketChannel}.
     * There is no general limit on protobuf message sizes, so we couldn't size a {@link ByteBuffer}
     * large enough to hold any message. However, the size of the next message will always be sent
     * through the socket as a protobuf {@code varint32} which can at most have five byte. Once
     * all bytes of a parsable {@code varint32} has been received, a new {@link ByteBuffer} is
     * allocated with the correct message size, and once it has been filled completely, a
     * {@link CodedInputStream} is used to parse the bytes into a {@link Msg} object. The next
     * byte then will again be looked at as a {@code varint32}, and so on.
     */
    void dataReceived(ByteBuffer data);

    /**
     * The list of possible commands can be found <a href="https://igtimi.github.io/yachtbot-docs/modules.html">here</a>.
     * Useful examples:
     * <table>
     * <tr><th>Command</th><th>Description</th></tr>
     * <tr><td>POWER OFF</td><td>Powers off the device.</td></tr>
     * <tr><td>BATTERY_VOLTAGE</td><td>Lists the current battery voltage. VOLTAGE is an alias.</td></tr>
     * <tr><td>GPS OFF</td><td>Turns the GPS off</td></tr>
     * <tr><td>GPS ON</td><td>Turns the GPS on</td></tr>
     * </table>
     * See {@link RiotStandardCommand} for convenience commands.
     * 
     * @return the log messages received within a 10s duration after having sent the command;
     * the queue will be filled asynchronously and will typically start out empty. The listener
     * filling it will be removed after the first 10s, so the queue will not grow indefinitely.
     * The result can be safely ignored, or the caller may wait up to 10s to collect, display,
     * forward, or analyze messages.
     */
    ConcurrentLinkedQueue<String> sendCommand(String command) throws IOException, InterruptedException, ExecutionException;

    TimePoint getLastHeartbeatReceivedAt();

    void enableOverTheAirLog() throws IOException;

    void disableOverTheAirLog() throws IOException;
}
