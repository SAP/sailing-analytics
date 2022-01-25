package com.sap.sse.landscape.aws;

import com.jcraft.jsch.JSchException;
import com.sap.sse.landscape.aws.impl.SSHKeyPairListener;
import com.sap.sse.landscape.ssh.SSHKeyPair;
import com.sap.sse.replication.Replicable;

public interface AwsLandscapeState extends Replicable<ReplicableAwsLandscapeState, AwsLandscapeOperation<?>> {
    void deleteKeyPair(String regionId, String keyName);

    void addSSHKeyPairListener(SSHKeyPairListener listener);

    void removeSSHKeyPairListener(SSHKeyPairListener listener);

    SSHKeyPair getSSHKeyPair(String regionId, String keyName);

    Iterable<SSHKeyPair> getSSHKeyPairs();

    byte[] getDecryptedPrivateKey(SSHKeyPair keyPair, byte[] privateKeyEncryptionPassphrase) throws JSchException;

    void addSSHKeyPair(SSHKeyPair result);

}
