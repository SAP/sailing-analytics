package com.sap.sailing.grib.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.sap.sailing.domain.confidence.ConfidenceFactory;
import com.sap.sailing.grib.GribWindField;
import com.sap.sse.common.Bounds;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.confidence.Weigher;
import com.sap.sse.common.impl.BoundsImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Wraps a {@link GridDataset GRIB data set} that either has the "Wind direction" parameter #31 and the
 * "Wind speed" parameter #32, or the "u-component of wind" parameter #33 and the "v-component of wind"
 * parameter #34. If the GRIB source has wind for more than one vertical layer/level (z-axis), only the
 * first one is used.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class AbstractGribWindFieldImpl implements GribWindField {
    private final FeatureDataset[] dataSets;
    
    private final Weigher<TimePoint> timeConfidenceWeigher;
    
    /**
     * The base confidence of the wind data coming from the underlying {@link #dataSets}, between 0 (not confident at
     * all) and 1 (really confident observation at this point in time)
     */
    private final double baseConfidence;

    public AbstractGribWindFieldImpl(double baseConfidence, FeatureDataset... dataSets) {
        this.dataSets = dataSets;
        this.baseConfidence = baseConfidence;
        timeConfidenceWeigher = ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(
                // use a minimum confidence to avoid the bearing to flip to 270deg in case all is zero
                Duration.ONE_SECOND.times(30).asMillis(), /* minimum confidence */0.0000000001);
    }
    
    protected static boolean hasVariable(VariableSpecification variableSpecification, FeatureDataset... dataSets) {
        return variableSpecification.appearsInAnyOf(dataSets);
    }
    
    protected boolean hasVariable(VariableSpecification variableSpecification) {
        return variableSpecification.appearsInAnyOf(dataSets);
    }

    protected double getBaseConfidence() {
        return baseConfidence;
    }

    @Override
    public Bounds getBounds() {
        Bounds result = null;
        for (final FeatureDataset dataSet : getDataSets()) {
            final Bounds dataSetBounds = toBounds(dataSet.getBoundingBox());
            if (result == null) {
                result = dataSetBounds;
            } else {
                result = result.extend(dataSetBounds);
            }
        }
        return result;
    }
    
    @Override
    public TimeRange getTimeRange() {
        return new TimeRangeImpl(getStartTime(), getEndTime());
    }

    /**
     * Based on the time difference to the {@link #getTimeRange() time range} of this GRIB data set computes
     * a confidence value. If within the time range, the confidence value is 1. Outside the time range a
     * weigher is used that reduces the confidence accordingly, like for other wind sources.
     */
    protected double getTimeConfidence(TimePoint timePoint) {
        final double result;
        final TimeRange timeRange = getTimeRange();
        if (timeRange.includes(timePoint)) {
            result = 1.0;
        } else if (timeRange.startsAtOrAfter(timePoint)) {
            result = timeConfidenceWeigher.getConfidence(timeRange.from(), timePoint);
        } else {
            assert timeRange.endsBefore(timePoint);
            result = timeConfidenceWeigher.getConfidence(timeRange.to(), timePoint);
        }
        return result;
    }
    
    protected double getTimeConfidence(TimePoint request, TimePoint fact) {
        return timeConfidenceWeigher.getConfidence(request, fact);
    }
    
    protected TimePoint toTimePoint(CalendarDate calendarDate) {
        return new MillisecondsTimePoint(calendarDate.getMillis());
    }

    protected TimePoint getStartTime() {
        return StreamSupport.stream(Arrays.asList(getDataSets()).spliterator(), /* parallel */ false).map(dataSet->toTimePoint(dataSet.getCalendarDateStart())).min(Comparator.naturalOrder()).get();
    }

    private TimePoint getEndTime() {
        return StreamSupport.stream(Arrays.asList(getDataSets()).spliterator(), /* parallel */ false).map(dataSet->toTimePoint(dataSet.getCalendarDateEnd())).max(Comparator.naturalOrder()).get();
    }

    private Bounds toBounds(LatLonRect boundingBox) {
        return new BoundsImpl(toPosition(boundingBox.getLowerLeftPoint()), toPosition(boundingBox.getUpperRightPoint()));
    }

    private Position toPosition(LatLonPoint point) {
        return new DegreePosition(point.getLatitude(), point.getLongitude());
    }

    protected FeatureDataset[] getDataSets() {
        return dataSets;
    }
    
    /**
     * Looks up a value in {@code grid} for the given time point and position.
     * 
     * @param timePoint
     *            may be outside of {@link #getTimeRange()}; in any case, the value time-wise closes to the time point
     *            requested is chosen
     * @param position
     *            may be outside of {@link #getBounds()}; in any case, the value at the position closest to
     *            {@code position} will be chosen
     * @return a non-interpolated, non-extrapolated value taken at the grid point that is time-wise and position-wise
     *         closes to those co-ordinates requested; as a triple whose first component is the value itself, the second
     *         component is the grid time point at which the value was taken, and the third component is the grid
     *         position where the value was obtained
     */
    protected Triple<Double, TimePoint, Position> getValue(GridDatatype grid, TimePoint timePoint, Position position) throws IOException {
        GridCoordSystem coordinateSystem = grid.getCoordinateSystem();
        int[] xy = coordinateSystem.findXYindexFromLatLon(position.getLatDeg(), position.getLngDeg(), new int[2]);
        Dimension timeDimension = grid.getTimeDimension();
        final int timeIndex;
        final TimePoint responseTimePoint;
        if (timeDimension != null) {
            final int numberOfTimepointsInGrid = timeDimension.getLength();
            if (numberOfTimepointsInGrid == 1) {
                timeIndex = 0;
                responseTimePoint = getStartTime();
            } else {
                final Duration timeBetweenSamples = getTimeRange().getDuration().divide(numberOfTimepointsInGrid);
                final Duration requestedDurationAfterStart = getStartTime().until(timePoint);
                final double steps = requestedDurationAfterStart.divide(timeBetweenSamples);
                if (steps < 0) {
                    timeIndex = 0;
                    responseTimePoint = getStartTime();
                } else if (steps > numberOfTimepointsInGrid-1) {
                    timeIndex = numberOfTimepointsInGrid-1;
                    responseTimePoint = getEndTime();
                } else {
                    timeIndex = (int) Math.round(steps);
                    responseTimePoint = getStartTime().plus(timeBetweenSamples.times(timeIndex));
                }
            }
        } else {
            timeIndex = -1; // time index doesn't matter if no time dimension exists for the grid
            responseTimePoint = getStartTime();
        }
        Pair<Double, Position> nearestNonNaNValueWithPosition = getNearestNonNaNValueWithPosition(
                coordinateSystem, grid, timeIndex, /* zIndex */ 0, xy[1], xy[0], /* distance */ 0);
        return new Triple<Double, TimePoint, Position>(nearestNonNaNValueWithPosition.getA(), responseTimePoint, nearestNonNaNValueWithPosition.getB());
    }
    
    /**
     * Tries to find a non-{@code NaN} value starting at a given "distance" from a grid point. "Distance" here means the
     * following: a grid point (x, y) has distance d from a grid point (a, b) if and only if {@code Math.abs(a-x)==d &&
     * Math.abs(b-y)<=d || Math.abs(a-x)<=d && Math.abs(b-y)==d}. These points are intersected with the bounds as
     * defined by the dimensions and then probed until a non-{@code NaN} value is found. The position for the grid point
     * where such a value was found is then returned. If no such point is found with the exact distance, the next higher
     * distance is probed recursively. If no such point exists within the bounds of the grid,
     * {@code new Pair<>(NaN, null)} will be returned.
     */
    private Pair<Double, Position> getNearestNonNaNValueWithPosition(GridCoordSystem coordinateSystem,
            GridDatatype grid, int timeIndex, int zIndex, int yIndex, int xIndex, int distance) throws IOException {
        final int minX = Math.max(0, xIndex-distance);
        final int maxX = Math.min(grid.getXDimension().getLength()-1, xIndex+distance);
        final int minY = Math.max(0, yIndex-distance);
        final int maxY = Math.min(grid.getYDimension().getLength()-1, yIndex+distance);
        if (minX>maxX || minY>maxY) { // empty range
            return new Pair<>(Double.NaN, null);
        }
        for (int x=minX; x<=maxX; x++) {
            for (int y = minY; y <= maxY; y += Math.max(1, maxY - minY)) { // scan upper and lower row; increment even if single-element range to trigger condition
                final double valueAsFloat = getValue(grid, timeIndex, zIndex, x, y);
                if (!Double.isNaN(valueAsFloat)) {
                    final Position responsePosition = toPosition(coordinateSystem.getLatLon(x, y));
                    return new Pair<>(valueAsFloat, responsePosition);
                }
            }
        }
        for (int y=minY+1; y<maxY; y++) {
            for (int x = minX; x <= maxX; x += Math.max(1, maxX - minX)) { // scan left and right border without top/bottom elements
                final double valueAsFloat = getValue(grid, timeIndex, zIndex, x, y);
                if (!Double.isNaN(valueAsFloat)) {
                    final Position responsePosition = toPosition(coordinateSystem.getLatLon(x, y));
                    return new Pair<>(valueAsFloat, responsePosition);
                }
            }
        }
        // nothing found so far; tail-recurse for next greater distance
        return getNearestNonNaNValueWithPosition(coordinateSystem, grid, timeIndex, zIndex, yIndex, xIndex, distance+1);
    }

    protected double getValue(GridDatatype grid, int timeIndex, int zIndex, final int x, final int y) throws IOException {
        final Array arrayForTimePointBefore = grid.readDataSlice(timeIndex, zIndex, y, x);
        final double valueAsFloat = (double) arrayForTimePointBefore.getFloat(0);
        return valueAsFloat;
    }
    
    /**
     * Instead of using {@link GridDatatype#readDataSlice(int, int, int, int)} which really reads from the file(s),
     * use an already read {@link Array} of mass data here.
     */
    protected double getValue(Array gridData, int zIndex, final int x, final int y) throws IOException {
        final double valueAsFloat;
        if (gridData.getRank() == 3) { // includes z dimension
            valueAsFloat = gridData.getDouble(Index.factory(new int[] { zIndex, y, x }));
        } else {
            valueAsFloat = gridData.getDouble(Index.factory(new int[] { y, x }));
        }
        return valueAsFloat;
    }
    
    @FunctionalInterface
    static interface ValueForCoordinateProvider<T> {
        T getValue(Array gridData, int timeIndex, Index index, TimePoint timePoint, Position position);
    }
    
    protected <T> Iterable<T> foreach(GridDatatype grid, ValueForCoordinateProvider<T> provider) throws IOException {
        final List<T> result = new ArrayList<>();
        final GridCoordSystem coordinateSystem = grid.getCoordinateSystem();
        final int timeDimLength = grid.getTimeDimension().getLength();
        for (int t=0; t<timeDimLength; t++) {
            final TimePoint timePoint = toTimePoint(coordinateSystem.getTimeAxis1D().getCalendarDate(t));
            final Array gridData = grid.readVolumeData(t);
            final long arraySize = gridData.getSize();
            final Index index = gridData.getIndex();
            for (long i=0; i<arraySize; i++) {
                final Position position = toPosition(coordinateSystem.getLatLon(/* x */ index.getCurrentCounter()[index.getCurrentCounter().length-1],
                                                                                /* y */ index.getCurrentCounter()[index.getCurrentCounter().length-2]));
                result.add(provider.getValue(gridData, t, index, timePoint, position));
                index.incr();
            }
        }
        return result;
    }
    
    protected Optional<String> getUnit(VariableDS variable) {
        final Optional<String> result;
        final ucar.nc2.Attribute attribute = variable.findAttribute("units");
        if (attribute != null) {
            result = Optional.of(attribute.getStringValue());
        } else {
            result = Optional.empty();
        }
        return result;
    }

    protected boolean isMetersPerSecond(String unit) {
        final String spacelessUnit = removeSpacesAndToLowercase(unit);
        return spacelessUnit.equals("m/s") || spacelessUnit.equals("ms^-1");
    }

    protected boolean isDegreesTrue(String unit) {
        final String spacelessUnit = removeSpacesAndToLowercase(unit).replaceAll("_", "");
        return spacelessUnit.equals("degreetrue") || spacelessUnit.equals("degtrue");
    }

    private String removeSpacesAndToLowercase(String unit) {
        return unit.replaceAll(" ", "").toLowerCase();
    }
}
