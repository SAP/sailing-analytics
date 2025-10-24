package com.sap.sailing.landscape.procedures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.SailingReleaseRepository;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sse.common.Util;
import com.sap.sse.debranding.ClientConfigurationListener;
import com.sap.sse.landscape.DefaultProcessConfigurationVariables;
import com.sap.sse.landscape.ProcessConfigurationVariable;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.aws.ApplicationProcessHost;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.aws.orchestration.AwsApplicationConfiguration;
import com.sap.sse.landscape.aws.orchestration.StartAwsHost;

public class SailingAnalyticsApplicationConfiguration<ShardingKey>
extends AwsApplicationConfiguration<ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>> {
    /**
     * A builder that helps building an instance of type {@link SailingAnalyticsApplicationConfiguration} or any subclass thereof (then
     * using specialized builders). The following default rules apply, in addition to the defaults rules of the builders
     * that this builder interface {@link StartAwsHost.Builder extends}.
     * <ul>
     * <li>If no {@link #setPort(int) port} is provided, the {@link #DEFAULT_PORT} is used (8888).</li>
     * <li>If no {@link #setTelnetPort(int) telnet port} is provided, the {@link #DEFAULT_TELNET_PORT} is used (14888).</li>
     * <li>If no {@link #setExpeditionPort(int) expedition UDP port} is provided, the {@link #DEFAULT_EXPEDITION_PORT} is used (2010).</li>
     * <li>If no {@link #setServerDirectory(String) server directory} is specified, it defaults to {@link ApplicationProcessHost#DEFAULT_SERVER_PATH}.</li>
     * <li>If no {@link #setRelease(Release) release} is specified, it defaults to {@link SailingReleaseRepository#getLatestMasterRelease()}.</li>
     * <li>The {@link DefaultProcessConfigurationVariables#ADDITIONAL_JAVA_ARGS} variable is extended by system properties that configure
     *     security, landscape data, and basic sailing master data to be shared across the {@link SharedLandscapeConstants#DEFAULT_DOMAIN_NAME} domain.</li>
     * </ul>
     * 
     * @author Axel Uhl (D043530)
     */
    public static interface Builder<BuilderT extends Builder<BuilderT, T, ShardingKey>,
    T extends AwsApplicationConfiguration<ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>>, ShardingKey>
    extends AwsApplicationConfiguration.Builder<BuilderT, T, ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>> {
        int DEFAULT_PORT = 8888;
        int DEFAULT_TELNET_PORT = 14888;
        int DEFAULT_EXPEDITION_PORT = 2010;
        
        BuilderT setPort(int port);
        
        BuilderT setTelnetPort(int telnetPort);
        
        BuilderT setExpeditionPort(int expeditionPort);
        
        BuilderT setIgtimiRiotPort(int igtimiRiotPort);

        BuilderT setServerDirectory(String serverDirectory);
    }
    
    /**
     * The builder needs to know the {@link AwsRegion} in which the application will be run. In this region, discovery
     * of default database and messaging endpoints is performed.
     */
    protected static class BuilderImpl<BuilderT extends Builder<BuilderT, T, ShardingKey>,
    T extends AwsApplicationConfiguration<ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>>, ShardingKey>
    extends AwsApplicationConfiguration.BuilderImpl<BuilderT, T, ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>>
    implements Builder<BuilderT, T, ShardingKey> {
        private Integer port;
        private Integer telnetPort;
        private Integer expeditionPort;
        private Integer igtimiRiotPort;
        private String serverDirectory;

        protected Integer getPort() {
            return port == null ? DEFAULT_PORT : port;
        }
        
        protected boolean isPortSet() {
            return port != null;
        }
        
        @Override
        public BuilderT setPort(int port) {
            this.port = port;
            return self();
        }
        
        protected Integer getTelnetPort() {
            return telnetPort == null ? DEFAULT_TELNET_PORT : telnetPort;
        }

        protected boolean isTelnetPortSet() {
            return telnetPort != null;
        }
        
        @Override
        public BuilderT setTelnetPort(int telnetPort) {
            this.telnetPort = telnetPort;
            return self();
        }

        protected Integer getExpeditionPort() {
            return expeditionPort == null ? DEFAULT_EXPEDITION_PORT : expeditionPort;
        }
        
        protected boolean isExpeditionPortSet() {
            return expeditionPort != null;
        }
        
        @Override
        public BuilderT setExpeditionPort(int expeditionPort) {
            this.expeditionPort = expeditionPort;
            return self();
        }

        protected Integer getIgtimiRiotPort() {
            return igtimiRiotPort;
        }
        
        protected boolean isIgtimiRiotPortSet() {
            return igtimiRiotPort != null;
        }
        
        @Override
        public BuilderT setIgtimiRiotPort(int igtimiRiotPort) {
            this.igtimiRiotPort = igtimiRiotPort;
            return self();
        }

        protected String getServerDirectory() {
            return serverDirectory == null ? ApplicationProcessHost.DEFAULT_SERVERS_PATH + "/" + getServerName() : serverDirectory;
        }
        
        protected boolean isServerDirectorySet() {
            return serverDirectory != null;
        }

        @Override
        public BuilderT setServerDirectory(String serverDirectory) {
            this.serverDirectory = serverDirectory;
            return self();
        }

        @Override
        protected Optional<Release> getRelease() {
            return Optional.of(super.getRelease().orElse(SailingReleaseRepository.INSTANCE.getLatestMasterRelease()));
        }

        /**
         * Expose for callers in same package as this class
         */
        @Override
        protected String getServerName() {
            return super.getServerName();
        }

        /**
         * Expose for callers in same package as this class
         */
        @Override
        protected AwsLandscape<ShardingKey> getLandscape() {
            return super.getLandscape();
        }

        /**
         * Expose for callers in same package as this class
         */
        @Override
        protected AwsRegion getRegion() {
            return super.getRegion();
        }

        @Override
        protected Map<ProcessConfigurationVariable, String> getUserData() {
            final Map<ProcessConfigurationVariable, String> result = new HashMap<>(super.getUserData());
            addUserDataForPort(result, DefaultProcessConfigurationVariables.SERVER_PORT, getPort());
            addUserDataForPort(result, DefaultProcessConfigurationVariables.TELNET_PORT, getTelnetPort());
            addUserDataForPort(result, SailingProcessConfigurationVariables.EXPEDITION_PORT, getExpeditionPort());
            if (getIgtimiRiotPort() != null) {
                addUserDataForPort(result, SailingProcessConfigurationVariables.IGTIMI_RIOT_PORT, getIgtimiRiotPort());
            }
            final List<String> newAdditionalJavaArgsVariableValue = new ArrayList<>();
            newAdditionalJavaArgsVariableValue.add("${"+DefaultProcessConfigurationVariables.ADDITIONAL_JAVA_ARGS.name()+"}");
            Util.addAll(getAdditionalJavaArgs(), newAdditionalJavaArgsVariableValue);
            result.put(DefaultProcessConfigurationVariables.ADDITIONAL_JAVA_ARGS, Util.joinStrings(" ", newAdditionalJavaArgsVariableValue));
            return result;
        }
        
        /**
         * Subclasses may re-define but should by default call this implementation to start with and return a new,
         * extended sequence of strings.
         * 
         * @return the additional strings to append, separated by spaced, to the
         *         {@link DefaultProcessConfigurationVariables#ADDITIONAL_JAVA_ARGS ${ADDITIONAL_JAVA_ARGS}} variable.
         *         An example string in the resulting sequence could be {@code "-Da=b"}. This implementation returns the
         *         system properties required to configure security shared across the
         *         {@link SharedLandscapeConstants#DEFAULT_DOMAIN_NAME} domain, as well as the activation of SAP branding,
         *         assuming that this runs under the {@link SharedLandscapeConstants#DEFAULT_DOMAIN_NAME default domain}.
         */
        protected Iterable<String> getAdditionalJavaArgs() {
            final List<String> result = new ArrayList<>();
            Util.addAll(getAdditionalJavaArgsForSharedSecurity(SharedLandscapeConstants.DEFAULT_DOMAIN_NAME, SharedLandscapeConstants.DEFAULT_SECURITY_SERVICE_REPLICA_SET_NAME), result);
            // TODO check whether we're really running under the default sapsailing.com domain, before activating SAP branding...
            result.add("-D"+ClientConfigurationListener.DEBRANDING_PROPERTY_NAME+"=false"); // activate branding when running under default SAP domain
            return result;
        }

        protected void addUserDataForPort(final Map<ProcessConfigurationVariable, String> result, ProcessConfigurationVariable variable, Integer port) {
            if (getPort() != null) {
                result.put(variable, port.toString());
            }
        }

        @Override
        public T build() throws Exception {
            @SuppressWarnings("unchecked")
            final T result = (T) new SailingAnalyticsApplicationConfiguration<ShardingKey>(this);
            return result;
        }
        
        protected String getDefaultSecurityServiceReplicaSetHostname(final String defaultDomainName, final String defaultSecurityServiceReplicaSetName) {
            return defaultSecurityServiceReplicaSetName+"."+defaultDomainName;
        }

        protected Iterable<String> getAdditionalJavaArgsForSharedSecurity(final String defaultDomainName, final String defaultSecurityServiceReplicaSetName) {
            return Arrays.asList(
                    "-Dsecurity.sharedAcrossSubdomainsOf="+defaultDomainName,
                    "-Dsecurity.baseUrlForCrossDomainStorage=https://"+getDefaultSecurityServiceReplicaSetHostname(defaultDomainName, defaultSecurityServiceReplicaSetName),
                    "-Dgwt.acceptableCrossDomainStorageRequestOriginRegexp=https?://(.*\\.)?"+(defaultDomainName.replaceAll("\\.", "\\\\."))+"(:[0-9]*)?$");
        }

        protected String getAdditionalJavaArgForWindEstimation(final String defaultDomainName) {
            return "-Dwindestimation.source.url=https://"+defaultDomainName;
        }

        protected String getAdditionalJavaArgForPolarData(final String defaultDomainName) {
            return "-Dpolardata.source.url=https://"+defaultDomainName;
        }
    }

    private final Integer port;
    private final Integer telnetPort;
    private final Integer expeditionPort;
    private final Integer igtimiRiotPort;
    private final String serverDirectory;
    
    public static <BuilderT extends Builder<BuilderT, T, ShardingKey>,
    T extends AwsApplicationConfiguration<ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>>, ShardingKey>
    BuilderT builder() {
        @SuppressWarnings("unchecked")
        final BuilderT result = (BuilderT) new BuilderImpl<BuilderT, T, ShardingKey>();
        return result;
    }

    protected SailingAnalyticsApplicationConfiguration(BuilderImpl<?, ?, ShardingKey> builder) {
        super(builder);
        this.port = builder.getPort();
        this.telnetPort = builder.getTelnetPort();
        this.expeditionPort = builder.getExpeditionPort();
        this.igtimiRiotPort = builder.getIgtimiRiotPort();
        this.serverDirectory = builder.getServerDirectory();
    }

    protected Integer getPort() {
        return port;
    }

    protected Integer getTelnetPort() {
        return telnetPort;
    }

    protected Integer getExpeditionPort() {
        return expeditionPort;
    }
    
    protected Integer getIgtimiRiotPort() {
        return igtimiRiotPort;
    }

    protected String getServerDirectory() {
        return serverDirectory;
    }
    
    /**
     * Expose the superclass method to other classes in the same package
     */
    protected String getServerName() {
        return super.getServerName();
    }
}
