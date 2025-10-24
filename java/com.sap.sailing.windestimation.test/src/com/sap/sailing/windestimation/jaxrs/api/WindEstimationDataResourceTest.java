package com.sap.sailing.windestimation.jaxrs.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.windestimation.integration.IncrementalMstHmmWindEstimationForTrackedRaceTest;
import com.sap.sailing.windestimation.integration.WindEstimationFactoryServiceImpl;
import com.sap.sailing.windestimation.model.store.ClassPathReadOnlyModelStoreImpl;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class WindEstimationDataResourceTest {

    @Test
    public void testImportingFromMockFile() throws Exception {
        WindEstimationFactoryServiceImpl windEstimationFactoryService = new WindEstimationFactoryServiceImpl();
        windEstimationFactoryService.clearState();
        assertFalse(windEstimationFactoryService.isReady());
        ClassPathReadOnlyModelStoreImpl modelStore = new ClassPathReadOnlyModelStoreImpl(
                "trained_wind_estimation_models", getClass().getClassLoader(),
                IncrementalMstHmmWindEstimationForTrackedRaceTest.modelFilesNames);
        final WindEstimationDataClientMock client = new WindEstimationDataClientMock(modelStore,
                windEstimationFactoryService);
        client.updateWindEstimationModels();
        assertTrue(windEstimationFactoryService.isReady());
    }

}