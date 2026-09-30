package com.sap.sse.landscape.ssh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class SshCommandChannelImpl implements SshCommandChannel {
    private static final Logger logger = Logger.getLogger(SshCommandChannelImpl.class.getName());
    private final ChannelExec channel;
    private InputStream stdout;
    
    /**
     * @param channel
     *            the result of a {@link Session#openChannel(String)} call; the channel is assumed to not yet be
     *            {@link Channel#connect() connected}
     */
    public SshCommandChannelImpl(ChannelExec channel) throws IOException, JSchException, InterruptedException {
        this.channel = channel;
    }

    @Override
    public int getExitStatus() {
        return channel.getExitStatus();
    }

    @Override
    public String runCommandAndReturnStdoutAndLogStderr(String commandLine, String stderrLogPrefix, Level stderrLogLevel) throws IOException, InterruptedException, JSchException {
        final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try {
            sendCommandLineSynchronously(commandLine, stderr);
            final String result = getStreamContentsAsString();
            if (stderrLogLevel != null && stderr.size() > 0) {
                logger.log(stderrLogLevel, (stderrLogPrefix==null?"":(stderrLogPrefix+": "))+stderr.toString());
            }
            return result;
        } finally {
            disconnect();
        }
    }

    @Override
    public InputStream sendCommandLineSynchronously(String commandLine, OutputStream stderr) throws IOException, InterruptedException, JSchException {
        stdout = channel.getInputStream();
        channel.setCommand(commandLine);
        channel.setErrStream(stderr);
        channel.connect(/* timeout in milliseconds */ 5000);
        return stdout;
    }
    
    @Override
    public byte[] getStreamContentsAsByteArray() throws IOException, JSchException {
        final ByteArrayOutputStream bos = readStdout();
        return bos.toByteArray();
    }

    @Override
    public String getStreamContentsAsString() throws IOException, JSchException {
        final ByteArrayOutputStream bos = readStdout();
        return bos.toString();
    }

    private ByteArrayOutputStream readStdout() throws IOException, JSchException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        int read;
        while ((read=stdout.read(buf, 0, buf.length)) >= 0) {
            bos.write(buf, 0, read);
        }
        stdout.close();
        while (!channel.isClosed()) {
            try {
                Thread.sleep(100L);
            } catch (Exception exc) {
                logger.warning("Warning: Error closing session to host "+getHost()+": " + exc.getMessage());
            }
        }
        channel.disconnect();
        return bos;
    }
    
    private String getHost() throws JSchException {
        return channel.getSession().getHost();
    }

    @Override
    public void disconnect() throws JSchException {
        if (channel.getSession() != null) {
            channel.getSession().disconnect();
        }
    }
}
