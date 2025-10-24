package com.sap.sailing.server.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.sharding.ShardingType;

public class ShardingLeaderBoardEncodingTest {
    @Test
    public void testEncoding() {
        Assertions.assertEquals("/leaderboard/pureascistring", ShardingType.LEADERBOARDNAME.encodeShardingInfo("pureascistring"));
        Assertions.assertEquals("/leaderboard/unpure_asci_string", ShardingType.LEADERBOARDNAME.encodeShardingInfo("unpure asci string"));
        Assertions.assertEquals("/leaderboard/c_dille", ShardingType.LEADERBOARDNAME.encodeShardingInfo("cédille"));
        Assertions.assertEquals("/leaderboard/Hello_World", ShardingType.LEADERBOARDNAME.encodeShardingInfo("Hello+World"));
        Assertions.assertEquals("/leaderboard/Hello_World_", ShardingType.LEADERBOARDNAME.encodeShardingInfo("Hello(World)"));
    }
}
