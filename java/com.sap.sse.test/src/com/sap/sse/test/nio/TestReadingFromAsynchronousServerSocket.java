package com.sap.sse.test.nio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

public class TestReadingFromAsynchronousServerSocket {
    @Test
    public void testAccepting() throws IOException, InterruptedException, ExecutionException {
        final CompletableFuture<Byte> resultFuture = new CompletableFuture<>();
        final ByteBuffer buf = ByteBuffer.allocateDirect(1024);
        final AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(0));
        final SocketAddress localAddress = serverSocketChannel.getLocalAddress();
        serverSocketChannel.accept("Humba", new CompletionHandler<AsynchronousSocketChannel, String>() {
            @Override
            public void completed(AsynchronousSocketChannel result, String attachment) {
                assertEquals("Humba", attachment);
                handleClientConnections(result, buf, resultFuture);
            }

            @Override
            public void failed(Throwable exc, String attachment) {
                fail(exc.getMessage()+": "+attachment);
            }
        });
        final Socket s = new Socket("127.0.0.1", ((InetSocketAddress) localAddress).getPort());
        s.getOutputStream().write(42);
        s.close();
        assertEquals(42, resultFuture.get().byteValue());
    }

    private void handleClientConnections(AsynchronousSocketChannel socketChannel, ByteBuffer buf, CompletableFuture<Byte> resultFuture) {
        socketChannel.read(buf, "Trala", new CompletionHandler<Integer, String>() {
            @Override
            public void completed(Integer result, String attachment) {
                assertEquals("Trala", attachment);
                buf.flip();
                final byte[] bytesRead = new byte[result];
                buf.get(bytesRead);
                assertEquals(1, result.intValue());
                resultFuture.complete(bytesRead[0]);
            }

            @Override
            public void failed(Throwable exc, String attachment) {
                fail(exc.getMessage()+": "+attachment);
            }
        });
    }
}
