package com.sap.sse.landscape.aws;

import com.sap.sse.common.Util;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.MachineImage;

import software.amazon.awssdk.services.ec2.model.ArchitectureValues;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.ImageState;
import software.amazon.awssdk.services.ec2.model.Tag;

public interface AmazonMachineImage<ShardingKey> extends MachineImage {
    Iterable<BlockDeviceMapping> getBlockDeviceMappings();

    @Override
    String getId();

    ImageState getState();
    
    default String getType() {
        return Util.first(Util.map(Util.filter(getTags(), t->t.key().equals(Landscape.IMAGE_TYPE_TAG_NAME)), Tag::value));
    }

    Iterable<Tag> getTags();
    
    ArchitectureValues getArchitecture();
}
