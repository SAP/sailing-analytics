package com.sap.sailing.landscape.procedures;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jcraft.jsch.JSchException;
import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.impl.SailingAnalyticsHostImpl;
import com.sap.sailing.landscape.impl.SailingAnalyticsProcessImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.aws.AmazonMachineImage;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsInstance;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.HostSupplier;
import com.sap.sse.landscape.aws.Tags;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.aws.orchestration.StartAwsHost;
import com.sap.sse.landscape.aws.orchestration.StartEmptyServer;
import com.sap.sse.landscape.orchestration.Procedure;
import com.sap.sse.shared.util.Wait;

import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.ImageState;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Snapshot;

/**
 * Upgrades an existing Amazon Machine Image that is expected to be prepared for such an upgrade, by
 * invoking it with very specific user data that trigger the automatic upgrade. The resulting AMI can
 * be obtained after this procedure has completed by calling {@link #getUpgradedAmi()}.<p>
 * 
 * The procedure uses a new name for the image and its snapshots. This name can either be explicitly
 * configured using the {@link Builder}, or it is inferred from the name of the image to upgrade by looking
 * for a version number in the form {@code [0-9]+\.[0-9]+(\.[0-9]+)}, so that major.minor and major.minor.micro
 * will all be considered matches. The {@link Builder} can be provided only with a new version string. By default,
 * it will increment the last version part matched (minor in case of major.minor and micro in case of
 * major.minor.micro) by one.<p>
 * 
 * The volume snapshot names are expected to follow the pattern {@code "^.*\(.*\)$"} where the snapshot's
 * base name is found between the parentheses. It is used by default to name the updated snapshots if no
 * {@link Builder#setSnapshotBaseName(String, String) explicit snapshot names} are provided.<p>
 * 
 * The upgrade procedure is based on setting the {@code image-upgrade} (see {@link StartEmptyServer#IMAGE_UPGRADE_USER_DATA})
 * line in the user data; optionally there can be another line {@code no-shutdown} (see {@link StartEmptyServer#NO_SHUTDOWN_USER_DATA})
 * which instructs the instance to not shut down after performing its upgrade. Any image that can handle these two user data lines
 * appropriately can be the target of this procedure.<p>
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <ShardingKey>
 * @param <HostT>
 */
public class UpgradeAmi<ShardingKey>
extends StartEmptyServer<UpgradeAmi<ShardingKey>, ShardingKey, SailingAnalyticsHost<ShardingKey>>
implements Procedure<ShardingKey>, StartFromSailingAnalyticsImage {
    private static final Logger logger = Logger.getLogger(UpgradeAmi.class.getName());
    private static final Pattern imageNamePattern = Pattern.compile("^(.*) ([0-9]+)\\.([0-9]+)(\\.([0-9]+))?$");
    private static final String IMAGE_UPGRADE_USER_DATA = "image-upgrade";
    
    private final AwsRegion region;
    private final String upgradedImageName;
    private final String imageType;
    private final boolean keyPairIsTemporaryAndNeedsToBeRemovedWhenDone;
    private final boolean waitForShutdown; // no need to wait if no shutdown was requested
    private final Map<String, String> deviceNamesToSnapshotBaseNames;
    private final Optional<Duration> optionalTimeout;
    private AmazonMachineImage<ShardingKey> upgradedAmi;
    
    /**
     * Additional default rules in addition to what the {@link StartAwsHost.Builder parent builder} defines:
     * 
     * <ul>
     * <li>If no {@link #setInstanceName(String) instance name} is set, the default instance name will be constructed as
     * {@code IMAGE_UPGRADE+" for "+machineImage.getId()}</li>
     * <li>If no {@link #setInstanceType(software.amazon.awssdk.services.ec2.model.InstanceType) instance type} is
     * specified, it defaults to {@link InstanceType#T2_MEDIUM}.</li>
     * <li>If no {@link #setImageType(String) image type} is specified, it defaults to the value of the constant
     * {@link #IMAGE_TYPE_TAG_VALUE_SAILING} (expected to be {@code "sailing-analytics-server"}); note that the image
     * type is also used to tag the resulting upgraded image; an explicit
     * {@link #setMachineImage(com.sap.sse.landscape.MachineImage) machine image}, if provided, takes precedence
     * regarding the selection of the image to upgrade.</li>
     * <li>The user data are set to the string defined by {@link UpgradeAmi#IMAGE_UPGRADE_USER_DATA}, forcing the image
     * to boot without trying to launch a process instance.</li>
     * <li>The {@link #isNoShutdown()} property default is changed to {@code false} because when upgrading an image the
     * default is that after the upgrading activity the instance is shut down for image creation.</li>
     * <li>By default the last part of the version number found will be incremented.</li>
     * <li>If the {@link #setVersionPartToIncrement(VersionPart)} method is used to explicitly specify a part to
     * increment then if its {@link VersionPart#MICRO} and the {@link VersionPart#MICRO} part doesn't exist yet, it is
     * added and set to 0. If the part exists it is incremented and in case it's not the last part, tailing parts are
     * all set to 0. If not provided, the last existing part is increments if at least a minor version exists, otherwise
     * a major.minor scheme is used and the minor version is set to 0. With this, the new image name defaults to the old
     * image's base name plus a space plus the new version number.</li>
     * <li>If no {@link #setKeyName(String) SSH key} has been specified, a temporary key is
     * {@link AwsLandscape#createKeyPair(com.sap.sse.landscape.Region, String, byte[]) created} with a random
     * {@link #setPrivateKeyEncryptionPassphrase(byte[]) private key pass phrase}. The builder implementation must indicate
     * to the procedure that the key is temporary and needs to be removed after the procedure has finished executing.</li>
     * </ul>
     * 
     * @author Axel Uhl (D043530)
     */
    public static interface Builder<BuilderT extends Builder<BuilderT, ShardingKey, SailingAnalyticsProcess<ShardingKey>>, ShardingKey,
    ProcessT extends ApplicationProcess<ShardingKey, SailingAnalyticsMetrics, ProcessT>>
    extends StartEmptyServer.Builder<BuilderT, UpgradeAmi<ShardingKey>, ShardingKey, SailingAnalyticsHost<ShardingKey>> {
        enum VersionPart {
            MAJOR, MINOR, MICRO
        }

        BuilderT setUpgradedImageName(String upgradedImageName);

        BuilderT setVersionPartToIncrement(VersionPart versionPartToIncrement);
        
        /**
         * It is possible to assign base names for snapshots based on their device name in the AMI. For example, "/dev/sdc" may
         * be the "Swap" device, and "/dev/xvda" may be the "System" partition. The full name for the snapshot is then assembled from
         * the AMI's name including its version and this base name. If no such basename is provided for a device name for which
         * a block device mapping to a snapshot exists, the snapshot will only be named after the AMI's name, so when multiple snapshots
         * are connected to the AMI then their names will not be discernible.
         */
        BuilderT setSnapshotBaseName(String deviceName, String snapshotBaseName);
    }

    protected static class BuilderImpl<BuilderT extends Builder<BuilderT, ShardingKey, SailingAnalyticsProcess<ShardingKey>>, ShardingKey>
    extends StartEmptyServer.BuilderImpl<BuilderT, UpgradeAmi<ShardingKey>, ShardingKey, SailingAnalyticsHost<ShardingKey>>
    implements Builder<BuilderT, ShardingKey, SailingAnalyticsProcess<ShardingKey>> {
        private String upgradedImageName;
        private VersionPart versionPartToIncrement;
        private Map<String, String> deviceNamesToSnapshotBaseNames;
        private boolean keyPairIsTemporaryAndNeedsToBeRemovedWhenDone;

        private BuilderImpl() {
            super();
        }
        
        @Override
        protected String getImageType() {
            return super.getImageType() == null ? getMachineImage() == null ? SharedLandscapeConstants.IMAGE_TYPE_TAG_VALUE_SAILING :
                Util.stream(getMachineImage().getTags()).filter(tag->tag.key().equals(AwsLandscape.IMAGE_TYPE_TAG_NAME)).findAny()
                    .map(tag->tag.value()).orElse(SharedLandscapeConstants.IMAGE_TYPE_TAG_VALUE_SAILING)
                : super.getImageType();
        }

        @Override
        protected String getInstanceName() {
            return super.getInstanceName() == null ? IMAGE_UPGRADE_USER_DATA+" for "+getMachineImage().getId() : super.getInstanceName();
        }

        @Override
        protected InstanceType getInstanceType() {
            return super.getInstanceType() == null ? InstanceType.T2_MEDIUM : super.getInstanceType();
        }

        /**
         * Re-expose to this class's package
         */
        @Override
        protected boolean isNoShutdown() {
            return super.isNoShutdown();
        }

        /**
         * If no key pair name has been {@link #setKeyName(String) set} yet, a temporary key is
         * {@link AwsLandscape#createKeyPair(com.sap.sse.landscape.Region, String, byte[]) created} with a random name
         * and a random private key encryption passphrase. The passphrase is
         * {@link #setPrivateKeyEncryptionPassphrase(byte[]) set} so that {@link #getPrivateKeyEncryptionPassphrase()}
         * will return it. From there on, {@link #getKeyName()} will continue to deliver the same random key name
         * generated, and {@link #isKeyPairIsTemporaryAndNeedsToBeRemovedWhenDone()} will return {@code true},
         * indicating that when done with the key and the upgrade procedure, the key must be
         * {@link AwsLandscape#deleteKeyPair(com.sap.sse.landscape.Region, String) deleted} again.
         */
        @Override
        protected String getKeyName() {
            if (super.getKeyName() == null) {
                keyPairIsTemporaryAndNeedsToBeRemovedWhenDone = true;
                final String keyName = "MyKey-"+UUID.randomUUID();
                logger.info("No key name provided; creating temporary key "+keyName);
                setPrivateKeyEncryptionPassphrase(UUID.randomUUID().toString().getBytes());
                try {
                    getLandscape().createKeyPair(getRegion(), keyName, getPrivateKeyEncryptionPassphrase());
                } catch (JSchException e) {
                    throw new RuntimeException(e);
                }
                setKeyName(keyName);
            }
            return super.getKeyName();
        }

        boolean isKeyPairIsTemporaryAndNeedsToBeRemovedWhenDone() {
            return keyPairIsTemporaryAndNeedsToBeRemovedWhenDone;
        }

        @Override
        public BuilderT setVersionPartToIncrement(VersionPart versionPartToIncrement) {
            this.versionPartToIncrement = versionPartToIncrement;
            return self();
        }

        private String increaseVersionNumber(String imageName) {
            final String result;
            final Matcher versionNumberMatcher = imageNamePattern.matcher(imageName);
            if (versionNumberMatcher.matches()) {
                final String imageBaseName = versionNumberMatcher.group(1);
                final Integer oldMajorVersion = Integer.valueOf(versionNumberMatcher.group(2));
                final Integer oldMinorVersion = Integer.valueOf(versionNumberMatcher.group(3));
                final Integer oldMicroVersion = versionNumberMatcher.group(5) == null ? null : Integer.valueOf(versionNumberMatcher.group(5));
                final VersionPart partToEffectivelyIncrement = versionPartToIncrement == null
                        ? oldMicroVersion == null ? VersionPart.MINOR : VersionPart.MICRO
                        : versionPartToIncrement;
                final Integer newMajorVersion = partToEffectivelyIncrement == VersionPart.MAJOR ? oldMajorVersion + 1 : oldMajorVersion;
                final Integer newMinorVersion = partToEffectivelyIncrement == VersionPart.MINOR ? oldMinorVersion + 1 : oldMinorVersion;
                final Integer newMicroVersion = oldMicroVersion == null
                        ? partToEffectivelyIncrement == VersionPart.MICRO
                            ? Integer.valueOf(0)
                            : null
                        : partToEffectivelyIncrement == VersionPart.MICRO
                            ? Integer.valueOf(oldMicroVersion + 1)
                            : oldMicroVersion;
                final StringBuilder sb = new StringBuilder(imageBaseName);
                sb.append(' ');
                sb.append(newMajorVersion);
                sb.append('.');
                sb.append(newMinorVersion);
                if (newMicroVersion != null) {
                    sb.append('.');
                    sb.append(newMicroVersion);
                }
                result = sb.toString();
            } else {
                result = imageName+" (1)";
            }
            return result;
        }

        @Override
        public BuilderT setUpgradedImageName(String upgradedImageName) {
            this.upgradedImageName = upgradedImageName;
            return self();
        }

        @Override
        public BuilderT setSnapshotBaseName(String deviceName, String snapshotBaseName) {
            if (deviceNamesToSnapshotBaseNames == null) {
                deviceNamesToSnapshotBaseNames = new HashMap<>();
            }
            deviceNamesToSnapshotBaseNames.put(deviceName, snapshotBaseName);
            return self();
        }

        @Override
        public HostSupplier<ShardingKey, SailingAnalyticsHost<ShardingKey>> getHostSupplier() {
            return (String instanceId, AwsAvailabilityZone az, InetAddress privateIpAddress, TimePoint launchTimePoint, AwsLandscape<ShardingKey> landscape)->
                new SailingAnalyticsHostImpl<>(instanceId, az, privateIpAddress,
                        launchTimePoint, landscape, (host, port, serverDirectory, telnetPort, serverName, additionalProperties)->{
                            try {
                                final Number expeditionUdpPort = (Number) additionalProperties.get(SailingProcessConfigurationVariables.EXPEDITION_PORT.name());
                                final Number igtimiRiotPort = (Number) additionalProperties.get(SailingProcessConfigurationVariables.IGTIMI_RIOT_PORT.name());
                                return new SailingAnalyticsProcessImpl<ShardingKey>(port, host, serverDirectory, telnetPort, serverName,
                                        expeditionUdpPort == null ? null : expeditionUdpPort.intValue(),
                                        igtimiRiotPort == null ? null : igtimiRiotPort.intValue(), landscape);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
        }

        protected String getUpgradedImageName() {
            return upgradedImageName;
        }
        
        protected Map<String, String> getDeviceNamesToSnapshotBaseNames() {
            return Collections.unmodifiableMap(deviceNamesToSnapshotBaseNames);
        }
        
        /**
         * Make visible to procedure's constructor
         */
        protected AwsRegion getRegion() {
            return super.getRegion();
        }
        
        @Override
        public UpgradeAmi<ShardingKey> build() {
            if (upgradedImageName == null) {
                upgradedImageName = increaseVersionNumber(getMachineImage().getName());
            }
            if (deviceNamesToSnapshotBaseNames == null) {
                deviceNamesToSnapshotBaseNames = new HashMap<>();
                final AmazonMachineImage<ShardingKey> image = getLandscape().getImage(getRegion(), getMachineImage().getId());
                for (final BlockDeviceMapping blockDeviceMapping : image.getBlockDeviceMappings()) {
                    final Snapshot snapshot = getLandscape().getSnapshot(getRegion(), blockDeviceMapping.ebs().snapshotId());
                    snapshot.tags().stream().filter(t->t.key().equals("Name")).findAny().ifPresent(nameTag->{
                        final Pattern snapshotNamePattern = Pattern.compile("^.* \\((.*)\\) *$");
                        final Matcher matcher = snapshotNamePattern.matcher(nameTag.value());
                        if (matcher.matches()) {
                            final String baseName = matcher.group(1);
                            deviceNamesToSnapshotBaseNames.put(blockDeviceMapping.deviceName(), baseName);
                        }
                    });
                }
            }
            return new UpgradeAmi<>(this);
        }

        /**
         * Make visible in package
         */
        @Override
        protected Optional<Duration> getOptionalTimeout() {
            return super.getOptionalTimeout();
        }
    }
    
    public static <BuilderT extends Builder<BuilderT, ShardingKey, SailingAnalyticsProcess<ShardingKey>>, ShardingKey,
    HostT extends AwsInstance<ShardingKey>> Builder<BuilderT, ShardingKey, SailingAnalyticsProcess<ShardingKey>> builder() {
        return new BuilderImpl<>();
    }
    
    protected UpgradeAmi(BuilderImpl<?, ShardingKey> builder) {
        super(builder);
        optionalTimeout = builder.getOptionalTimeout();
        region = builder.getRegion();
        imageType = builder.getImageType();
        upgradedImageName = builder.getUpgradedImageName();
        waitForShutdown = !builder.isNoShutdown();
        deviceNamesToSnapshotBaseNames = builder.getDeviceNamesToSnapshotBaseNames();
        keyPairIsTemporaryAndNeedsToBeRemovedWhenDone = builder.isKeyPairIsTemporaryAndNeedsToBeRemovedWhenDone();
        addUserData(Collections.singleton(IMAGE_UPGRADE_USER_DATA));
    }
    
    @Override
    public void run() throws Exception {
        try {
            super.run(); // launches the machine in upgrade mode and shuts it down again, preparing for AMI creation
            final Instance[] instance = new Instance[1];
            if (waitForShutdown) {
                Wait.wait(
                        () -> (instance[0] = getLandscape().getInstance(getHost().getInstanceId(),
                                getHost().getRegion())) != null
                                && instance[0].state().name() == InstanceStateName.STOPPED,
                        result -> result, /* retryOnException */ true, optionalTimeout,
                        /* sleepBetweenAttempts */ Duration.ONE_SECOND.times(5), Level.INFO,
                        "Waiting for shutdown of instance " + getHost().getInstanceId());
            }
            upgradedAmi = getLandscape().createImage(getHost(), upgradedImageName, Optional.of(Tags.with(Landscape.IMAGE_TYPE_TAG_NAME, imageType)));
            Wait.wait(()->(upgradedAmi=getLandscape().getImage(upgradedAmi.getRegion(), upgradedAmi.getId())).getState() == ImageState.AVAILABLE,
                    optionalTimeout, /* sleepBetweenAttempts */ Duration.ONE_SECOND.times(5), Level.INFO, "Waiting for Image " + upgradedAmi.getId() + " to become "+ImageState.AVAILABLE);
            for (final BlockDeviceMapping blockDeviceMapping : upgradedAmi.getBlockDeviceMappings()) {
                if (blockDeviceMapping.ebs() != null) {
                    final String snapshotId = blockDeviceMapping.ebs().snapshotId();
                    final String deviceName = blockDeviceMapping.deviceName();
                    final String snapshotName = getSnapshotName(deviceName);
                    getLandscape().setSnapshotName(getHost().getRegion(), snapshotId, snapshotName);
                }
            }
        } finally {
            if (keyPairIsTemporaryAndNeedsToBeRemovedWhenDone) {
                logger.info("Removing temporary key "+getKeyName());
                getLandscape().deleteKeyPair(region, getKeyName());
            }
            if (getHost() != null) {
                getHost().terminate();
            }
        }
    }
    
    private String getSnapshotName(String deviceName) {
        final StringBuilder result = new StringBuilder();
        result.append(upgradedImageName);
        final String baseName = deviceNamesToSnapshotBaseNames.get(deviceName);
        if (baseName != null) {
            result.append(" (");
            result.append(baseName);
            result.append(")");
        }
        return result.toString();
    }

    /**
     * @return the resulting AMI that has the upgraded version of everything
     */
    public AmazonMachineImage<ShardingKey> getUpgradedAmi() {
        return upgradedAmi;
    }
}
