package com.sap.sse.landscape.aws;

import java.util.Optional;

import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.MachineImage;
import com.sap.sse.landscape.aws.impl.AwsRegion;

import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Tag;

/**
 * An AWS EC2 host that has been created from a {@link MachineImage} that supports deploying an application,
 * has a {@link ReverseProxy} on it
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface AwsInstance<ShardingKey> extends Host {
    /**
     * tag {@link Tag#key() key} used by AWS auto-scaling groups to tag the instances launched by the group; the
     * tag's {@link Tag#value() value} equals the {@link AwsAutoScalingGroup auto-scaling group's} name. 
     */
    String AWS_AUTOSCALING_GROUP_NAME_TAG = "aws:autoscaling:groupName";
    
    AwsLandscape<ShardingKey> getLandscape();
    
    String getInstanceId();

    InstanceType getInstanceType();

    /**
     * Obtains a fresh copy of the instance by looking it up in the {@link #getRegion() region} by its {@link #getInstanceId() ID}.
     */
    default Instance getInstance() {
        return getLandscape().getInstance(getInstanceId(), getRegion());
    }

    /**
     * Finds out whether this instance is managed by an auto-scaling group. The implementation checks the
     * {@link #AWS_AUTOSCALING_GROUP_NAME_TAG} tag's value and compares it to the {@code autoScalingGroup}'s name.
     */
    boolean isManagedByAutoScalingGroup();

    /**
     * Finds out whether this instance is managed by any of the {@code autoScalingGroups} passed as parameter. The
     * implementation checks the {@link #AWS_AUTOSCALING_GROUP_NAME_TAG} tag's value and compares it to the
     * {@code autoScalingGroup}'s name.
     */
    boolean isManagedByAutoScalingGroup(Iterable<AwsAutoScalingGroup> autoScalingGroups);
    
    default String getId() {
        return getInstanceId();
    }
    
    void setTerminationProtection(boolean terminationProtection);

    void terminate();

    @Override
    AwsAvailabilityZone getAvailabilityZone();

    @Override
    default AwsRegion getRegion() {
        return (AwsRegion) Host.super.getRegion();
    }
    
    boolean verifySshKey(Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase); 
}
