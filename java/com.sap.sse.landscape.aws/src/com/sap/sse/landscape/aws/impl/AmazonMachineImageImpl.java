package com.sap.sse.landscape.aws.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.sap.sse.common.TimePoint;
import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.aws.AmazonMachineImage;
import com.sap.sse.landscape.aws.AwsLandscape;

import software.amazon.awssdk.services.ec2.model.ArchitectureValues;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.ImageState;
import software.amazon.awssdk.services.ec2.model.Tag;

public class AmazonMachineImageImpl<ShardingKey> implements AmazonMachineImage<ShardingKey> {
    private static final long serialVersionUID = 1615200981492476022L;
    private final Image image;
    private final Region region;
    private final AwsLandscape<ShardingKey> landscape;
    
    public AmazonMachineImageImpl(Image image, Region region, AwsLandscape<ShardingKey> landscape) {
        this.image = image;
        this.region = region;
        this.landscape = landscape;
    }

    @Override
    public String getId() {
        return image.imageId();
    }

    @Override
    public String getName() {
        return image.name();
    }

    @Override
    public Region getRegion() {
        return region;
    }
    
    @Override
    public Iterable<Tag> getTags() {
        return image.tags();
    }

    @Override
    public TimePoint getCreatedAt() {
        Date date;
        try {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            date = dateFormat.parse(image.creationDate());
        } catch (ParseException e) {
            date = null;
        }
        return date == null ? null : TimePoint.of(date);
    }

    @Override
    public AmazonMachineImage<ShardingKey> updateAllPackages() {
        // launch with "image-upgrade" as the only user data, then produce the new image
        // TODO Implement MachineImage<AwsInstance>.updateAllPackages(...)
        return null;
    }

    @Override
    public void delete() {
        landscape.deleteImage(getRegion(), getId());
        for (final BlockDeviceMapping blockDeviceMapping : getBlockDeviceMappings()) {
            if (blockDeviceMapping.ebs() != null) {
                landscape.deleteSnapshot(getRegion(), blockDeviceMapping.ebs().snapshotId());
            }
        }
    }
    
    @Override
    public Iterable<BlockDeviceMapping> getBlockDeviceMappings() {
        return image.blockDeviceMappings();
    }

    @Override
    public ImageState getState() {
        return image.state();
    }
    
    @Override
    public ArchitectureValues getArchitecture() {
        return image.architecture();
    }

    @Override
    public String toString() {
        return "AMI [imageId=" + image.imageId() + ", region=" + region +"]";
    }
}
