package com.sap.sailing.server.masterdata;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorStore;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.ObjectInputStreamResolvingAgainstDomainFactory;
import com.sap.sailing.domain.base.impl.MasterDataImportInformation;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.MasterDataImportObjectCreationCount;
import com.sap.sailing.domain.common.impl.MasterDataImportObjectCreationCountImpl;
import com.sap.sailing.domain.masterdataimport.TopLevelMasterData;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.operationaltransformation.ImportMasterDataOperation;

public class MasterDataImporter {
    private final DomainFactory baseDomainFactory;

    private final RacingEventService racingEventService;

    public MasterDataImporter(DomainFactory baseDomainFactory, RacingEventService racingEventService) {
        this.baseDomainFactory = baseDomainFactory;
        this.racingEventService = racingEventService;
    }

    public void importFromStream(InputStream inputStream, UUID importOperationId, boolean override) throws IOException,
            ClassNotFoundException {
        ObjectInputStreamResolvingAgainstDomainFactory objectInputStream = racingEventService.getBaseDomainFactory()
                .createObjectInputStreamResolvingAgainstThisFactory(inputStream);
        racingEventService
                .createOrUpdateDataImportProgressWithReplication(importOperationId, 0.03, "Reading Data", 0.5);

        RaceLogStore raceLogStore = MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(
                racingEventService.getMongoObjectFactory(), racingEventService.getDomainObjectFactory());
        SeriesImpl.setOngoingMasterDataImport(new MasterDataImportInformation(raceLogStore));

        @SuppressWarnings("unchecked")
        final List<Serializable> competitorIds = (List<Serializable>) objectInputStream.readObject();



        if (override) {
            setAllowCompetitorsDataToBeReset(competitorIds);
        }
        // Deserialize Regattas to make sure that Regattas are deserialized before Series
        objectInputStream.readObject();
        TopLevelMasterData topLevelMasterData = (TopLevelMasterData) objectInputStream.readObject();

        SeriesImpl.setOngoingMasterDataImport(null);

        racingEventService.createOrUpdateDataImportProgressWithReplication(importOperationId, 0.3,
                "Data-Transfer Complete, Initializing Import Operation", 0.5);

        applyMasterDataImportOperation(topLevelMasterData, importOperationId, override);
    }

    private void setAllowCompetitorsDataToBeReset(List<Serializable> competitorIds) {
        CompetitorStore store = baseDomainFactory.getCompetitorStore();
        for (Serializable id : competitorIds) {
            Competitor competitor = baseDomainFactory.getExistingCompetitorById(id);
            if (competitor != null) {
                store.allowCompetitorResetToDefaults(competitor);
            }
        }
    }

    private MasterDataImportObjectCreationCount applyMasterDataImportOperation(TopLevelMasterData topLevelMasterData,
            UUID importOperationId, boolean override) {
        MasterDataImportObjectCreationCountImpl creationCount = new MasterDataImportObjectCreationCountImpl();
        ImportMasterDataOperation op = new ImportMasterDataOperation(topLevelMasterData, importOperationId, override,
                creationCount);
        creationCount = racingEventService.apply(op);
        racingEventService.mediaTracksImported(topLevelMasterData.getAllMediaTracks(), override);
        return creationCount;
    }

}
