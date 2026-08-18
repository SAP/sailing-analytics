package com.sap.sse.gwt.adminconsole;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;

@DefaultLocale("en")
public interface StringMessages extends com.sap.sse.gwt.client.StringMessages {
    public static final StringMessages INSTANCE = GWT.create(StringMessages.class);
    
    String upload();

    String removeResult(String status, String message);

    String uploadSuccessful();

    String fileUploadResult(String status, String message);

    String removeUploadedFile();
    
    String pleaseOnlyUploadContentYouHaveAllUsageRightsFor();
    
    String send();

    String version(String buildVersion);

    String unknown();

    String refresh();

    String explainReplicasRegistered();

    String explainConnectionsToMaster();

    String stopAllReplicas();

    String connectToMaster();

    String stopConnectionToMaster();

    String errorStartingReplication(String b, String c, String message);

    String loading();

    String registeredAt(String string);

    String registrationTime();

    String dropReplicaConnection();

    String dropReplicas();

    String replicaIdentifier();

    String numberOfOperations();

    String replicables();

    String averageNumberOfOperationsPerMessage();

    String numberOfQueueMessagesSent();

    String averageMessageSize();

    String totalSize();

    String totalNumberOfOperations();

    String explainNoConnectionsFromReplicas();

    String warningServerIsReplica();

    String replicatingFromMaster(String hostname, int messagingPort, int servletPort, String messagingHostname,
            String exchangeName, String string);

    String explainNoConnectionsToMaster();

    String errorFetchingReplicaData(String message);

    String connect();

    String enterMaster();

    String cancel();

    String hostname();

    String explainReplicationHostname();

    String exchangeHost();

    String explainExchangeHostName();

    String exchangeName();

    String explainReplicationExchangeName();

    String messagingPortNumber();

    String explainReplicationExchangePort();

    String servletPortNumber();

    String explainReplicationServletPort();

    String ok();
    
    String setUpStorageService();

    String usernameAndPasswordMustBothBeSet();

    String username();

    String explainUserName();

    String password();

    String explainPassword();

    String additionalInformation();

    String serverInformation();

    String serverName(String buildVersion);

    String validPassphrase();

    String invalidPassphrase();

    String passphraseCheckError();

    String replicaColumnIp();

    String replicaColumnId();

    String replicaColumnRegistered();

    String replicaColumnOpsPerMsg();

    String replicaColumnMessages();

    String replicaColumnAvgMsgSize();

    String replicaColumnTotalSize();

    String replicaColumnTotalOps();

    String reallyDropReplica(String additionalInformation, String name);
}
