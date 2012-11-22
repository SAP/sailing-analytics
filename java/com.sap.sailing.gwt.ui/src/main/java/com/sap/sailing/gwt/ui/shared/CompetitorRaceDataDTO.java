package com.sap.sailing.gwt.ui.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.Util.Triple;

public class CompetitorRaceDataDTO implements IsSerializable {
    
    private CompetitorDTO competitor;
    private DetailType detailType;

    /**
     * A: Bouy-Name; B: mark passing time; C: data as defined by {@link #detailType} for {@link #competitor} for time point B
     */
    private List<Triple<String, Date, Double>> markPassingsData;
    /**
     * A: time for B; B: data as defined by {@link #detailType} for {@link #competitor} for time point A
     */
    private List<Pair<Date, Double>> raceData;
    
    CompetitorRaceDataDTO() {}
    
    public CompetitorRaceDataDTO(CompetitorDTO competitor, DetailType detailType, 
            Collection<Triple<String, Date, Double>> markPassingsData, Collection<Pair<Date, Double>> raceData) {
        this.competitor = competitor;
        this.detailType = detailType;
        this.markPassingsData = new ArrayList<Triple<String, Date, Double>>();
        addAllMarkPassingsData(markPassingsData);
        this.raceData = new ArrayList<Pair<Date, Double>>();
        addAllRaceData(raceData);
    }

    public CompetitorDTO getCompetitor() {
        return competitor;
    }

    public DetailType getDetailType() {
        return detailType;
    }
    
    /**
     * Takes the {@link Date} of the last elements in {@link #raceData} and {@link #markPassingsData} and returns the newer date.
     */
    public Date getDateOfNewestData() {
        Date dateOfNewestRaceData = !raceData.isEmpty() ? raceData.get(raceData.size() - 1).getA() : null;
            return dateOfNewestRaceData != null ? dateOfNewestRaceData : null;
    }
    
    /**
     * Clears the old {@link #markPassingsData} and {@link #addAllMarkPassingsData(Collection) adds} the data in <code>markPassingsData</code> afterwards.
     * @param markPassingsData
     */
    public void setMarkPassingsData(List<Triple<String, Date, Double>> markPassingsData) {
        this.markPassingsData = new ArrayList<Triple<String,Date,Double>>();
        addAllMarkPassingsData(markPassingsData);
    }

    /**
     * @return An unmodifiable list of the {@link #markPassingsData}.
     */
    public List<Triple<String, Date, Double>> getMarkPassingsData() {
        return Collections.unmodifiableList(markPassingsData);
    }
    
    /**
     * {@link #addMarkPassingsData(Triple) Adds} all elements in <code>markPassingsDataToAdd</code>.
     * Checks if the data is already contained, but not if the detailTypes fit. The data will not be added, if it's already contained.
     * 
     * @param markPassingsDataToAdd
     */
    public void addAllMarkPassingsData(Collection<Triple<String, Date, Double>> markPassingsDataToAdd) {
        for (Triple<String, Date, Double> data : markPassingsDataToAdd) {
            addMarkPassingsData(data);
        }
    }
    
    /**
     * Adds the data <code>markPassingsDataToAdd</code> at the correct index (via binary search) to {@link #markPassingsData}.
     * Checks if the data is already contained, but not if the detailTypes fit. The data will not be added, if it's already contained.
     * 
     * @param markPassingsDataToAdd
     */
    public void addMarkPassingsData(Triple<String, Date, Double> markPassingsDataToAdd) {
        int index = Collections.binarySearch(markPassingsData, markPassingsDataToAdd, new MarkPassingsByTime());
        //binarySearch returns a value smaller then 0 if the key isn't contained
        if (index < 0) {
            markPassingsData.add((-1 * index) - 1, markPassingsDataToAdd);
        }
    }

    /**
     * @return An unmodifiable list of the {@link #raceData}
     */
    public List<Pair<Date, Double>> getRaceData() {
        return Collections.unmodifiableList(raceData);
    }
    
    /**
     * {@link #addRaceData(Pair) Adds} all elements in <code>raceDataToAdd</code>.
     * Checks if the data is already contained, but not if the detailTypes fit. The data will not be added, if it's already contained.
     * 
     * @param raceDataToAdd
     */
    public void addAllRaceData(Collection<Pair<Date, Double>> raceDataToAdd) {
        for (Pair<Date, Double> data : raceDataToAdd) {
            addRaceData(data);
        }
    }

    /**
     * Adds the data <code>raceDataToAdd</code> at the correct index (via binary search) to {@link #raceData}.
     * Checks if the data is already contained, but not if the detailTypes fit. The data will not be added, if it's already contained.
     * 
     * @param raceDataToAdd
     */
    public void addRaceData(Pair<Date, Double> raceDataToAdd) {
        int index = Collections.binarySearch(raceData, raceDataToAdd, new RaceDataByTime());
        //binarySearch returns a value smaller then 0 if the key isn't contained
        if (index < 0) {
            raceData.add((-1 * index) - 1, raceDataToAdd);
        }
    }

    /**
     * Adds all data with {@link #addAllMarkPassingsData(Collection)} and {@link #addAllRaceData(Collection)}, if the
     * detailTypes fit. Checks if the data is already contained. The data will not be added, if it's already contained.
     * 
     * @param dataToAdd
     */
    public void addAllData(CompetitorRaceDataDTO dataToAdd) {
        if (detailType == dataToAdd.getDetailType()) {
            addAllMarkPassingsData(dataToAdd.getMarkPassingsData());
            addAllRaceData(dataToAdd.getRaceData());
        }
    }

    /**
     * @param after The date, exclusive, from where the data should be returned
     * @return An unmodifiable sublist of {@link #raceData}
     */
    public List<Pair<Date, Double>> getRaceDataAfterDate(Date after) {
        int from = Collections.binarySearch(raceData, new Pair<Date, Double>(after, 0.0), new RaceDataByTime());
        return Collections.unmodifiableList(raceData.subList(from + 1, raceData.size()));
    }
    
    public class RaceDataByTime implements Comparator<Pair<Date, Double>> {
        @Override
        public int compare(Pair<Date, Double> d1, Pair<Date, Double> d2) {
            return d1.getA().compareTo(d2.getA());
        }
    }
    
    public class MarkPassingsByTime implements Comparator<Triple<String, Date, Double>> {
        @Override
        public int compare(Triple<String, Date, Double> m1, Triple<String, Date, Double> m2) {
            return m1.getB().compareTo(m2.getB());
        }
    }

}
