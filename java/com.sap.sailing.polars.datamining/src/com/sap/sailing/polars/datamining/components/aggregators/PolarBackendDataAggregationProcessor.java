package com.sap.sailing.polars.datamining.components.aggregators;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.polars.datamining.data.HasBackendPolarBoatClassContext;
import com.sap.sailing.polars.datamining.shared.PolarBackendData;
import com.sap.sailing.polars.datamining.shared.PolarBackendDataImpl;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.impl.components.SimpleAggregationProcessorDefinition;
import com.sap.sse.datamining.impl.components.aggregators.AbstractParallelGroupedDataAggregationProcessor;
import com.sap.sse.datamining.shared.GroupKey;

/**
 * Creates one {@link PolarBackendData} per element. Due to the nature of the backend data there will be only one element per groupkey (boatclass).
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class PolarBackendDataAggregationProcessor extends AbstractParallelGroupedDataAggregationProcessor<HasBackendPolarBoatClassContext, PolarBackendData> {

    private static final String POLARS_MESSAGE_KEY = "Polars";
    private final ConcurrentHashMap<GroupKey, PolarBackendData> resultMap = new ConcurrentHashMap<>();
    
    public PolarBackendDataAggregationProcessor(ExecutorService executor,
            Collection<Processor<Map<GroupKey, PolarBackendData>, ?>> resultReceivers) {
        super(executor, resultReceivers, POLARS_MESSAGE_KEY);
    }
    
    private static final AggregationProcessorDefinition<HasBackendPolarBoatClassContext, PolarBackendData> DEFINITION =
            new SimpleAggregationProcessorDefinition<>(HasBackendPolarBoatClassContext.class, PolarBackendData.class, POLARS_MESSAGE_KEY, PolarBackendDataAggregationProcessor.class);
    
    public static AggregationProcessorDefinition<HasBackendPolarBoatClassContext, PolarBackendData> getDefinition() {
        return DEFINITION;
    }
    
    @Override
    protected boolean needsSynchronization() {
        return false;
    }

    @Override
    protected void handleElement(GroupedDataEntry<HasBackendPolarBoatClassContext> element) {
        PolarBackendData polarAggregation = createPolarBackendAggregation(element.getDataEntry());
        resultMap.put(element.getKey(), polarAggregation);
    }

    private PolarBackendData createPolarBackendAggregation(HasBackendPolarBoatClassContext dataEntry) {
        boolean hasUpwindSpeedData = true;
        double[] upwindBoatSpeedOverWindSpeed = new double[30];
        
        boolean hasDownwindSpeedData = true;
        double[] downwindBoatSpeedOverWindSpeed = new double[30];
        
        boolean hasUpwindAngleData = true;
        double[] upwindBoatAngleOverWindSpeed = new double[30];
        
        boolean hasDownwindAngleData = true;
        double[] downwindBoatAngleOverWindSpeed = new double[30];
        
        boolean[] hasDataForAngle = new boolean[360];
        double[][] speedPerAnglePerWindSpeed = new double[360][30]; 
        
        PolarDataService polarDataService = dataEntry.getPolarDataService();
        BoatClass boatClass = dataEntry.getBoatClass();
        try {
            PolynomialFunction upwind = polarDataService.getSpeedRegressionFunction(boatClass, LegType.UPWIND);
            setArrayValuesForFunction(upwind, upwindBoatSpeedOverWindSpeed);
        } catch (NotEnoughDataHasBeenAddedException e) {
            hasUpwindSpeedData = false;
        }
        try {
            PolynomialFunction downwind = polarDataService.getSpeedRegressionFunction(boatClass, LegType.DOWNWIND);
            setArrayValuesForFunction(downwind, downwindBoatSpeedOverWindSpeed);
        } catch (NotEnoughDataHasBeenAddedException e) {
            hasDownwindSpeedData = false;
        }
        try {
            PolynomialFunction upwind = polarDataService.getAngleRegressionFunction(boatClass, LegType.UPWIND);
            setArrayValuesForFunction(upwind, upwindBoatAngleOverWindSpeed);
        } catch (NotEnoughDataHasBeenAddedException e) {
            hasUpwindAngleData = false;
        }
        try {
            PolynomialFunction downwind = polarDataService.getAngleRegressionFunction(boatClass, LegType.DOWNWIND);
            setArrayValuesForFunction(downwind, downwindBoatAngleOverWindSpeed);
        } catch (NotEnoughDataHasBeenAddedException e) {
            hasDownwindAngleData = false;
        }
        
        for (int angleInDeg = 0; angleInDeg < 360; angleInDeg++) {
            if (isAborted()) {
                break;
            }
            int convertedAngle = angleInDeg  > 180 ? angleInDeg  - 360 : angleInDeg ;
            try {
                for (int x = 0; x < 30; x++) {
                    if (isAborted()) {
                        break;
                    }
                    SpeedWithConfidence<Void> speed = polarDataService.getSpeed(boatClass, new KnotSpeedImpl(x), new DegreeBearingImpl(convertedAngle));
                    if (speed.getConfidence() > 0.1) {
                        hasDataForAngle[angleInDeg] = true;
                    } else {
                        hasDataForAngle[angleInDeg] = false;
                    }
                    speedPerAnglePerWindSpeed[angleInDeg][x] = speed.getObject().getKnots();
                }
            } catch (NotEnoughDataHasBeenAddedException e) {
                hasDataForAngle[angleInDeg] = false;
            }
        }
        return new PolarBackendDataImpl(hasUpwindSpeedData, upwindBoatSpeedOverWindSpeed, hasDownwindSpeedData, downwindBoatSpeedOverWindSpeed, hasUpwindAngleData,
                upwindBoatAngleOverWindSpeed, hasDownwindAngleData, downwindBoatAngleOverWindSpeed, hasDataForAngle, speedPerAnglePerWindSpeed);
    }

    @Override
    protected Map<GroupKey, PolarBackendData> getResult() {
        return resultMap;
    }
    
    private void setArrayValuesForFunction(PolynomialFunction function, double[] yOverWindSpeed) {
        for (int x = 0; x < 30; x++) {
            if (isAborted()) {
                break;
            }
            yOverWindSpeed[x] = function.value(x);
        }
    }

}
