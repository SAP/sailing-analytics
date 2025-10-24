package com.sap.sailing.landscape.procedures;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.impl.SailingAnalyticsProcessImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.concurrent.ConcurrentHashBag;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.aws.ApplicationProcessHost;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsInstance;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.orchestration.AbstractAwsProcedureImpl;
import com.sap.sse.landscape.aws.orchestration.AwsApplicationConfiguration;
import com.sap.sse.landscape.orchestration.Procedure;
import com.sap.sse.landscape.ssh.SshCommandChannel;

/**
 * Deploys a single {@link SailingAnalyticsProcess} to a given {@link ApplicationProcessHost} which ideally has been
 * launched using the {@link StartMultiServer} procedure, but may also work for hosts started in another way. The builder
 * has to be created using an {@link AwsApplicationConfiguration.Builder} which is used by the caller to define the key
 * properties of the application to be launched. From the server name property a default for the directory name to which
 * to deploy the application will be derived. If no ports are specified, the next available set of ports is determined
 * by looking at the other deployed processes and scanning for the next free port, starting from the default ports.<p>
 * 
 * The application is started after deployment as part of this procedure, and a default "Home-SSL" reverse proxy mapping is established
 * for a hostname <tt>${SERVER_NAME}.sapsailing.com</tt>.
 * 
 * @author Axel Uhl (D043530)
 */
public class DeployProcessOnMultiServer<ShardingKey, HostT extends AwsInstance<ShardingKey>,
ApplicationConfigurationT extends SailingAnalyticsApplicationConfiguration<ShardingKey>,
ApplicationConfigurationBuilderT extends SailingAnalyticsApplicationConfiguration.Builder<ApplicationConfigurationBuilderT, ApplicationConfigurationT, ShardingKey>>
extends AbstractAwsProcedureImpl<ShardingKey>
implements Procedure<ShardingKey> {
    private static final Logger logger = Logger.getLogger(DeployProcessOnMultiServer.class.getName());
    private final SailingAnalyticsHost<ShardingKey> hostToDeployTo;
    private final ApplicationConfigurationT applicationConfiguration;
    private final Optional<Duration> optionalTimeout;
    private final Optional<String> optionalKeyName;
    private final byte[] privateKeyEncryptionPassphrase;

    /**
     * The process launched by this procedure. {@link #hostToDeployTo} is expected to be identical to
     * {@link SailingAnalyticsProcess#getHost() process.getHost()}.
     */
    private SailingAnalyticsProcess<ShardingKey> process;
    
    /**
     * When no explicit port specifications for HTTP, Telnet and Expedition/UDP have been provided by the application
     * configuration builder (which is the recommended default because otherwise conflicts with already deployed
     * processes are possible and likely), this builder will, upon {@link #build() building} the procedure, analyze the
     * ports already occupied by checking the already deployed processes and their configuration. For each of the three
     * port types (HTTP, Telnet, Expedition/UDP), the next available port starting from the respective default is
     * allocated in the application configuration.
     * <p>
     * 
     * The {@link SailingAnalyticsApplicationConfiguration#getServerDirectory() server directory} defaults to the
     * {@link {@link SailingAnalyticsApplicationConfiguration#getServerName() server name} appended to the
     * {@link ApplicationProcessHost#DEFAULT_SERVER_PATH default server directory}.
     * <p>
     * 
     * The {@link SailingAnalyticsApplicationConfiguration.Builder#setServerName(String) server name} must have been set
     * on the application configuration builder before invoking this builder's {@link #build()} method.
     * <p>
     * 
     * If no {@link Builder#setLandscape(Landscape) landscape} is explicitly set, the
     * {@link AwsApplicationConfiguration.Builder#setLandscape(AwsLandscape) landscape} of the application configuration
     * is used if provided, otherwise the {@link ApplicationProcessHost#getLandscape() landscape} of the
     * {@link #setHostToDeployTo(ApplicationProcessHost) host} is used as a default. The latter is then also used as the
     * default for the application configuration if no landscape has been provided for it explicitly.
     * <p>
     * 
     * If the application configuration does not
     * {@link AwsApplicationConfiguration.Builder#setRegion(com.sap.sse.landscape.aws.impl.AwsRegion) specify a region},
     * the host's region is used as the default region.
     * <p>
     * 
     * If no {@link #setKeyName(String) key name} is specified, the private key of the key pair used to launch the
     * {@link #setHostToDeployTo(ApplicationProcessHost) host} will be unlocked using the mandatory
     * {@link #setPrivateKeyEncryptionPassphrase(byte[]) decryption passphrase}.
     * 
     * @author Axel Uhl (D043530)
     */
    public static interface Builder<BuilderT extends Builder<BuilderT, ShardingKey, HostT, ApplicationConfigurationT, ApplicationConfigurationBuilderT>, ShardingKey, HostT extends AwsInstance<ShardingKey>,
    ApplicationConfigurationT extends SailingAnalyticsApplicationConfiguration<ShardingKey>,
    ApplicationConfigurationBuilderT extends SailingAnalyticsApplicationConfiguration.Builder<ApplicationConfigurationBuilderT, ApplicationConfigurationT, ShardingKey>>
    extends AbstractAwsProcedureImpl.Builder<BuilderT, DeployProcessOnMultiServer<ShardingKey, HostT, ApplicationConfigurationT, ApplicationConfigurationBuilderT>, ShardingKey> {
        BuilderT setKeyName(String keyName);
        BuilderT setPrivateKeyEncryptionPassphrase(byte[] privateKeyEncryptionPassphrase);
    }
    
    public static class BuilderImpl<BuilderT extends Builder<BuilderT, ShardingKey, HostT, ApplicationConfigurationT, ApplicationConfigurationBuilderT>, ShardingKey, HostT extends AwsInstance<ShardingKey>,
    ApplicationConfigurationT extends SailingAnalyticsApplicationConfiguration<ShardingKey>,
    ApplicationConfigurationBuilderT extends SailingAnalyticsApplicationConfiguration.Builder<ApplicationConfigurationBuilderT, ApplicationConfigurationT, ShardingKey>>
    extends AbstractAwsProcedureImpl.BuilderImpl<BuilderT, DeployProcessOnMultiServer<ShardingKey, HostT, ApplicationConfigurationT, ApplicationConfigurationBuilderT>, ShardingKey>
    implements Builder<BuilderT, ShardingKey, HostT, ApplicationConfigurationT, ApplicationConfigurationBuilderT> {
        private final SailingAnalyticsApplicationConfiguration.BuilderImpl<ApplicationConfigurationBuilderT, ApplicationConfigurationT, ShardingKey> applicationConfigurationBuilder;
        private final SailingAnalyticsHost<ShardingKey> hostToDeployTo;
        private Optional<String> optionalKeyName = Optional.empty();
        private byte[] privateKeyEncryptionPassphrase;

        protected BuilderImpl(SailingAnalyticsApplicationConfiguration.BuilderImpl<ApplicationConfigurationBuilderT, ApplicationConfigurationT, ShardingKey> applicationConfigurationBuilder,
                SailingAnalyticsHost<ShardingKey> hostToDeployTo) {
            super();
            this.applicationConfigurationBuilder = applicationConfigurationBuilder;
            this.hostToDeployTo = hostToDeployTo;
        }
        
        @Override
        public DeployProcessOnMultiServer<ShardingKey, HostT, ApplicationConfigurationT, ApplicationConfigurationBuilderT> build() throws Exception {
            assert getHostToDeployTo() != null;
            assert getApplicationConfigurationBuilder().getServerName() != null;
            if (getLandscape() == null) {
                if (getApplicationConfigurationBuilder().getLandscape() != null) {
                    setLandscape(getApplicationConfigurationBuilder().getLandscape());
                } else {
                    setLandscape(getHostToDeployTo().getLandscape());
                }
            }
            if (getApplicationConfigurationBuilder().getLandscape() == null) {
                getApplicationConfigurationBuilder().setLandscape(getLandscape());
            }
            if (getApplicationConfigurationBuilder().getRegion() == null) {
                getApplicationConfigurationBuilder().setRegion(getHostToDeployTo().getRegion());
            }
            if (!getApplicationConfigurationBuilder().isServerDirectorySet()) {
                getApplicationConfigurationBuilder().setServerDirectory(ApplicationProcessHost.DEFAULT_SERVERS_PATH+"/"+getApplicationConfigurationBuilder().getServerName());
            }
            if (!getApplicationConfigurationBuilder().isPortSet()) {
                getApplicationConfigurationBuilder().setPort(getNextAvailablePort(getHostToDeployTo(),
                        SailingAnalyticsApplicationConfiguration.Builder.DEFAULT_PORT,
                        SailingAnalyticsProcess::getPort));
            }
            if (!getApplicationConfigurationBuilder().isTelnetPortSet()) {
                getApplicationConfigurationBuilder().setTelnetPort(getNextAvailablePort(getHostToDeployTo(),
                        SailingAnalyticsApplicationConfiguration.Builder.DEFAULT_TELNET_PORT,
                        ap->{
                            try {
                                return ap.getTelnetPortToOSGiConsole(getOptionalTimeout(), optionalKeyName, privateKeyEncryptionPassphrase);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }));
            }
            if (!getApplicationConfigurationBuilder().isExpeditionPortSet()) {
                getApplicationConfigurationBuilder().setExpeditionPort(getNextAvailablePort(getHostToDeployTo(),
                        SailingAnalyticsApplicationConfiguration.Builder.DEFAULT_EXPEDITION_PORT,
                        ap->{
                            try {
                                return ap.getExpeditionUdpPort(getOptionalTimeout(), optionalKeyName, privateKeyEncryptionPassphrase);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }));
            }
            if (!getApplicationConfigurationBuilder().isIgtimiRiotPortSet() && getApplicationConfigurationBuilder().getServerName().equals(SharedLandscapeConstants.IGTIMI_DEFAULT_RIOT_REPLICA_SET_NAME)) {
                // we deploy a process instance for the Igtimi Riot replica set; so we'll set the port to the default value:
                getApplicationConfigurationBuilder().setIgtimiRiotPort(SharedLandscapeConstants.IGTIMI_DEFAULT_RIOT_PORT);
            }
            return new DeployProcessOnMultiServer<>(this);
        }

        private int getNextAvailablePort(final SailingAnalyticsHost<ShardingKey> hostToDeployTo, int defaultPort, Function<SailingAnalyticsProcess<ShardingKey>, Integer> portFetcher) throws Exception {
            logger.info("Scanning for available port on "+hostToDeployTo+", starting at "+defaultPort);
            final Set<Integer> occupiedPorts = new HashSet<>();
            final Set<SailingAnalyticsProcess<ShardingKey>> applicationProcessesToScan = new HashSet<>();
            Util.addAll(hostToDeployTo.getApplicationProcesses(getOptionalTimeout(), optionalKeyName, privateKeyEncryptionPassphrase), applicationProcessesToScan);
            for (final SailingAnalyticsProcess<ShardingKey> applicationProcess : applicationProcessesToScan) {
                occupiedPorts.add(portFetcher.apply(applicationProcess));
            }
            final AwsAvailabilityZone azOfHostToDeployTo = hostToDeployTo.getAvailabilityZone();
            int numberOfSharedHostsInOtherAZs = 0;
            final ConcurrentHashBag<Integer> portsOccupiedInSharedHostsInOtherAZs = new ConcurrentHashBag<>();
            for (final SailingAnalyticsHost<ShardingKey> sharedHost : getLandscape().getRunningHostsWithTagValue(getApplicationConfigurationBuilder().getRegion(),
                    SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG, SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_TAG_VALUE,
                    new SailingAnalyticsHostSupplier<ShardingKey>())) {
                // accept only ports for which not all other shared instances in other AZs have that port occupied
                if (!sharedHost.getAvailabilityZone().equals(azOfHostToDeployTo)) {
                    logger.info("...also scanning for available port on shared host "+sharedHost+" because it is in different availability zone");
                    numberOfSharedHostsInOtherAZs++;
                    for (final SailingAnalyticsProcess<ShardingKey> processOnSharedHostInOtherAZ : sharedHost.getApplicationProcesses(getOptionalTimeout(), optionalKeyName, privateKeyEncryptionPassphrase)) {
                        portsOccupiedInSharedHostsInOtherAZs.add(portFetcher.apply(processOnSharedHostInOtherAZ));
                    }
                }
            }
            int port = defaultPort;
            while (port<Integer.MAX_VALUE && (occupiedPorts.contains(port) ||
                    (numberOfSharedHostsInOtherAZs > 0 && portsOccupiedInSharedHostsInOtherAZs.count(port) == numberOfSharedHostsInOtherAZs))) {
                if (!occupiedPorts.contains(port)) {
                    logger.info("Didn't choose port "+port+" because all "+numberOfSharedHostsInOtherAZs+" in other AZs occupy it.");
                }
                port++;
            }
            logger.info("Identified "+port+" as the next available port, started at "+defaultPort);
            return port;
        }

        /**
         * Expose to subclasses
         */
        @Override
        protected AwsLandscape<ShardingKey> getLandscape() {
            return (AwsLandscape<ShardingKey>) super.getLandscape();
        }

        protected SailingAnalyticsHost<ShardingKey> getHostToDeployTo() {
            return hostToDeployTo;
        }

        protected SailingAnalyticsApplicationConfiguration.BuilderImpl<ApplicationConfigurationBuilderT, ApplicationConfigurationT, ShardingKey> getApplicationConfigurationBuilder() {
            return applicationConfigurationBuilder;
        }
        
        /**
         * Make visible in package
         */
        @Override
        protected Optional<Duration> getOptionalTimeout() {
            return super.getOptionalTimeout();
        }

        @Override
        public BuilderT setPrivateKeyEncryptionPassphrase(byte[] privateKeyEncryptionPassphrase) {
            this.privateKeyEncryptionPassphrase = privateKeyEncryptionPassphrase;
            return self();
        }
        
        protected byte[] getPrivateKeyEncryptionPassphrase() {
            return privateKeyEncryptionPassphrase;
        }

        @Override
        public BuilderT setKeyName(String keyName) {
            this.optionalKeyName = Optional.ofNullable(keyName);
            return self();
        }
        
        protected Optional<String> getOptionalKeyName() {
            return optionalKeyName;
        }
    }
    
    public static <BuilderT extends Builder<BuilderT, ShardingKey, HostT, ApplicationConfigurationT, ApplicationConfigurationBuilderT>, ShardingKey, HostT extends AwsInstance<ShardingKey>,
    ApplicationConfigurationT extends SailingAnalyticsApplicationConfiguration<ShardingKey>,
    ApplicationConfigurationBuilderT extends SailingAnalyticsApplicationConfiguration.Builder<ApplicationConfigurationBuilderT, ApplicationConfigurationT, ShardingKey>>
    Builder<BuilderT, ShardingKey, HostT, ApplicationConfigurationT, ApplicationConfigurationBuilderT> builder(ApplicationConfigurationBuilderT applicationConfigurationBuilder,
            SailingAnalyticsHost<ShardingKey> hostToDeployTo) {
        @SuppressWarnings("unchecked")
        final SailingAnalyticsApplicationConfiguration.BuilderImpl<ApplicationConfigurationBuilderT, ApplicationConfigurationT, ShardingKey> applicationConfigurationBuilderCast =
                (SailingAnalyticsApplicationConfiguration.BuilderImpl<ApplicationConfigurationBuilderT, ApplicationConfigurationT, ShardingKey>) applicationConfigurationBuilder;
        return new BuilderImpl<>(applicationConfigurationBuilderCast, hostToDeployTo);
    }
    
    protected DeployProcessOnMultiServer(BuilderImpl<?, ShardingKey, HostT, ApplicationConfigurationT, ApplicationConfigurationBuilderT> builder) throws Exception {
        super(builder);
        this.hostToDeployTo = builder.getHostToDeployTo();
        this.optionalTimeout = builder.getOptionalTimeout();
        this.optionalKeyName = builder.getOptionalKeyName();
        this.privateKeyEncryptionPassphrase = builder.getPrivateKeyEncryptionPassphrase();
        this.applicationConfiguration = builder.getApplicationConfigurationBuilder().build();
        assert getHostToDeployTo() != null;
    }
    
    @Override
    public void run() throws Exception {
        assert getHostToDeployTo() != null;
        final String serverDirectory = applicationConfiguration.getServerDirectory();
        {
            logger.info("Deploying process to multi-server "+getHostToDeployTo()+" into directory "+serverDirectory);
            logger.fine("Using configuration:\n"+applicationConfiguration.getAsEnvironmentVariableAssignments());
            final SshCommandChannel sshChannel = getHostToDeployTo().createRootSshChannel(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
            final String stdout = sshChannel.runCommandAndReturnStdoutAndLogStderr(
                    "su -l "+StartSailingAnalyticsHost.SAILING_USER_NAME+" -c \""+
                    "mkdir -p "+serverDirectory.replaceAll("\"", "\\\\\"")+"; "+
                    "sudo /usr/local/bin/cp_root_mail_properties "+applicationConfiguration.getServerName()+"; "+
                    "cd "+serverDirectory.replaceAll("\"", "\\\\\"")+"; "+
                    "echo '"+applicationConfiguration.getAsEnvironmentVariableAssignments().replaceAll("\"", "\\\\\"").replaceAll("\\$", "\\\\\\$")+
                    "' | /usr/local/bin/refreshInstance.sh auto-install-from-stdin; ./start\";", // SAILING_USER ends here
                    "stderr: ", Level.WARNING);
            logger.info("stdout: "+stdout);
        }
        process = new SailingAnalyticsProcessImpl<>(applicationConfiguration.getPort(), getHostToDeployTo(),
                serverDirectory, applicationConfiguration.getTelnetPort(),
                applicationConfiguration.getServerName(), applicationConfiguration.getExpeditionPort(),
                applicationConfiguration.getIgtimiRiotPort(), getLandscape());
    }
    
    public SailingAnalyticsProcess<ShardingKey> getProcess() {
        return process;
    }

    private SailingAnalyticsHost<ShardingKey> getHostToDeployTo() {
        return hostToDeployTo;
    }
}
