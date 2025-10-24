package com.sap.sse.landscape.aws.common.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ShardTargetGroupNameTest {
    private static final String TARGET_GROUP_NAME_PREFIX = "TG-";
    @Test
    public void testCreateAndParse() {
        final String replicaSetName = "abc";
        final String shardName = "def";
        final ShardTargetGroupName theShardName = ShardTargetGroupName.create(replicaSetName, shardName, TARGET_GROUP_NAME_PREFIX);
        final ShardTargetGroupName parsedShardName = ShardTargetGroupName.parse(theShardName.getTargetGroupName(), null);
        assertEquals(shardName, parsedShardName.getShardName());
        assertEquals(theShardName.getTargetGroupName(), parsedShardName.getTargetGroupName());
    }

    @Test
    public void testCreateAndParseWithSimpleSeparatorInNames() {
        final String replicaSetName = "abc"+ShardTargetGroupName.SEPARATOR+"def";
        final String shardName = "def"+ShardTargetGroupName.SEPARATOR+"ghi";
        assertThrows(IllegalArgumentException.class, ()->{
            ShardTargetGroupName.create(replicaSetName, shardName, TARGET_GROUP_NAME_PREFIX);
        });
    }

    @Test
    public void testCreateAndParseLongShardName() {
        final String replicaSetName = "abc";
        final String shardName = "deflawencawueclkajhfalskjfhlaskjdhfllakj";
        final ShardTargetGroupName theShardName = ShardTargetGroupName.create(replicaSetName, shardName, TARGET_GROUP_NAME_PREFIX);
        final ShardTargetGroupName parsedShardName = ShardTargetGroupName.parse(theShardName.getTargetGroupName(), null);
        assertEquals(theShardName.getTargetGroupName(), parsedShardName.getTargetGroupName());
        final String shardNamePrefix = parsedShardName.getShardName().substring(0,
                parsedShardName.getShardName().indexOf(ShardTargetGroupName.NAMESEPARATOR));
        final String shardNameSuffix = parsedShardName.getShardName()
                .substring(parsedShardName.getShardName().indexOf(ShardTargetGroupName.NAMESEPARATOR)
                        + ShardTargetGroupName.NAMESEPARATOR.length());
        assertTrue(shardName.startsWith(shardNamePrefix));
        assertTrue(shardName.endsWith(shardNameSuffix));
    }
    
    @Test
    public void testValidTargetGroupName() {
        assertTrue(ShardTargetGroupName.isValidShardTargetGroupName("S-Humba-Trala"));
        assertTrue(ShardTargetGroupName.isValidShardTargetGroupName("S-Humba-Tr--a"));
        assertTrue(ShardTargetGroupName.isValidShardTargetGroupName("S-Humba-Tr--"));
        assertTrue(ShardTargetGroupName.isValidShardTargetGroupName("Humba-Trala"));
        assertTrue(ShardTargetGroupName.isValidShardTargetGroupName("Humba-Tr--a"));
        assertTrue(ShardTargetGroupName.isValidShardTargetGroupName("Humba-Tr--"));
        assertFalse(ShardTargetGroupName.isValidShardTargetGroupName("S-Humba-Tr--a-a"));
        assertFalse(ShardTargetGroupName.isValidShardTargetGroupName("Humba-Tr--a-a"));
    }
}
