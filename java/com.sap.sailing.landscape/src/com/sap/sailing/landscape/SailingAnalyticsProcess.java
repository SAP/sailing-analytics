package com.sap.sailing.landscape;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.jcraft.jsch.JSchException;
import com.sap.sailing.landscape.procedures.SailingProcessConfigurationVariables;
import com.sap.sse.common.Duration;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.aws.AwsApplicationProcess;
import com.sap.sse.util.ThreadPoolUtil;

public interface SailingAnalyticsProcess<ShardingKey> extends AwsApplicationProcess<ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>> {
    static Logger logger = Logger.getLogger(SailingAnalyticsProcess.class.getName());

    int getExpeditionUdpPort(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * The Igtimi Riot port as obtained from the {@link SailingProcessConfigurationVariables#IGTIMI_RIOT_PORT} variable,
     * or {@code null} if that variable is not configured in this process. Note: even if the
     * {@link SailingProcessConfigurationVariables#IGTIMI_RIOT_PORT IGTIMI_RIOT_PORT} variable is not set, the
     * application server will still listen on some randomly-selected port that was available upon start-up. But this
     * port will not be returned by this method. This method is only about the explicit configuration through the
     * environment variables.
     */
    Integer getIgtimiRiotPort(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    @Override
    SailingAnalyticsHost<ShardingKey> getHost();
    
    /**
     * Refreshes this process to the {@code release} specified. This happens by connecting to the {@link #getHost() instance} by
     * SSH, changing into the {@link #getServerDirectory(Optional) server directory} for this process, running the {@code refreshInstance.sh}
     * script there with the {@code install-release} subcommand parameterized with the {@code release} to install, and then
     * runs {@code ./stop; ./start} to activate the {@code release}.
     */
    void refreshToRelease(Release release, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws IOException, InterruptedException, JSchException, Exception;

    double getLastMinuteSystemLoadAverage(Optional<Duration> optionalTimeout) throws TimeoutException, Exception;

    /**
     * The count of <em>all</em> tasks in the default background thread pool executor's queue, including those scheduled
     * with a delay, such as rate limiter and cache clean-up tasks or periodic fetching of remote server content.
     * 
     * @see ThreadPoolUtil#getDefaultBackgroundTaskThreadPoolExecutor()
     */
    int getDefaultBackgroundThreadPoolExecutorQueueSize(Optional<Duration> optionalTimeout) throws TimeoutException, Exception;

    /**
     * The count of <em>all</em> tasks in the default foreground thread pool executor's queue, including those scheduled
     * with a delay, such as rate limiter and cache clean-up tasks or periodic fetching of remote server content.
     * 
     * @see ThreadPoolUtil#getDefaultForegroundTaskThreadPoolExecutor()
     */
    int getDefaultForegroundThreadPoolExecutorQueueSize(Optional<Duration> optionalTimeout) throws TimeoutException, Exception;

    /**
     * The count of only those tasks in the default background thread pool executor's queue that have no scheduling
     * delay or a delay below {@link Duration#ONE_SECOND one second}. This helps exclude from the count those rate
     * limiter and cache clean-up tasks or periodic fetching of remote server content. It is as such as good approximation
     * of the "immediate" tasks that will keep the server busy in the immediate future.
     * 
     * @see ThreadPoolUtil#getDefaultBackgroundTaskThreadPoolExecutor()
     */
    int getDefaultBackgroundThreadPoolExecutorQueueSizeNondelayed(Optional<Duration> optionalTimeout) throws TimeoutException, Exception;

    /**
     * The count of only those tasks in the default foreground thread pool executor's queue that have no scheduling
     * delay or a delay below {@link Duration#ONE_SECOND one second}. This helps exclude from the count those rate
     * limiter and cache clean-up tasks or periodic fetching of remote server content. It is as such as good approximation
     * of the "immediate" tasks that will keep the server busy in the immediate future.
     * 
     * @see ThreadPoolUtil#getDefaultForegroundTaskThreadPoolExecutor()
     */
    int getDefaultForegroundThreadPoolExecutorQueueSizeNondelayed(Optional<Duration> optionalTimeout) throws TimeoutException, Exception;
}
