package com.sap.sailing.gwt.ui.shared;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapSettings;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.NamedSecuredObjectDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;

public class RegattaDTO extends NamedSecuredObjectDTO implements SecuredDTO {
    private static final long serialVersionUID = -4594784946348402759L;
    /**
     * May be <code>null</code> in case the boat class is not known
     */
    public BoatClassDTO boatClass;
    public Date startDate;
    public Date endDate;
    public List<RaceWithCompetitorsAndBoatsDTO> races;
    public List<SeriesDTO> series;
    public ScoringSchemeType scoringScheme;
    public List<CourseAreaDTO> courseAreas = new ArrayList<>();
    public DeviceConfigurationDTO.RegattaConfigurationDTO configuration;
    public boolean useStartTimeInference = false;
    public boolean controlTrackingFromStartAndFinishTimes = false;
    public boolean autoRestartTrackingUponCompetitorSetChange = false;
    public boolean canBoatsOfCompetitorsChangePerRace = false;
    public CompetitorRegistrationType competitorRegistrationType = CompetitorRegistrationType.CLOSED;
    public RankingMetrics rankingMetricType;
    public Double buoyZoneRadiusInHullLengths;
    public String registrationLinkSecret;
    
    @Deprecated
    RegattaDTO() {} // for GWT RPC serialization only
    
    public RegattaDTO(String name, ScoringSchemeType scoringScheme) {
        super(name);
        this.scoringScheme = scoringScheme;
    }
    
    /**
     * A clone / copy constructor, copying all field values, flat, from <code>other</code> to the new object, except for the
     * {@link #series} list which is creates as a new list with all its elements cloned using the {@link SeriesDTO#SeriesDTO(SeriesDTO)}
     * copy constructor.
     */
    public RegattaDTO(RegattaDTO other) {
        super(other.getName());
        this.boatClass = other.boatClass;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.races = other.races;
        this.series = new ArrayList<>();
        for (SeriesDTO otherSeries : other.series) {
            this.series.add(new SeriesDTO(otherSeries)); // clone the series; clients may replace / alter their fields
        }
        this.scoringScheme = other.scoringScheme;
        this.courseAreas = other.courseAreas;
        this.rankingMetricType = other.rankingMetricType;
        this.configuration = other.configuration;
        this.useStartTimeInference = other.useStartTimeInference;
        this.controlTrackingFromStartAndFinishTimes = other.controlTrackingFromStartAndFinishTimes;
        this.autoRestartTrackingUponCompetitorSetChange = other.autoRestartTrackingUponCompetitorSetChange;
        this.canBoatsOfCompetitorsChangePerRace = other.canBoatsOfCompetitorsChangePerRace;
        this.competitorRegistrationType = other.competitorRegistrationType;
        this.buoyZoneRadiusInHullLengths = other.buoyZoneRadiusInHullLengths;
        this.registrationLinkSecret = other.registrationLinkSecret;
    }
    
    public Pair<SeriesDTO, FleetDTO> getSeriesAndFleet(RegattaAndRaceIdentifier raceIdentifier) {
        for (SeriesDTO s : series) {
            for (RaceColumnDTO raceColumn : s.getRaceColumns()) {
                FleetDTO fleet = raceColumn.getFleet(raceIdentifier);
                if (fleet != null) {
                    return new Pair<SeriesDTO, FleetDTO>(s, fleet);
                }
            }
        }
        return null;
    }

    public RegattaIdentifier getRegattaIdentifier() {
        return new RegattaName(getName());
    }
    
    /**
     * @return The start date of the first {@link #races Race}, or <code>null</code> if the start date isn't set
     */
    public Date getStartDate() {
        if(races.size()>0){
            return races.get(0).startOfRace;
        }
        return null;
    }
    
    /**
     * @return <code>true</code> if at least one race of the regatta is currently tracked, else it returns <code>false</code>
     */
    public boolean currentlyTracked() {
        boolean tracked = false;
        for (RaceDTO race : races) {
            tracked = race.isTracked;
            if (tracked) {
                break;
            }
        }
        return tracked;
    }
    
    /**
     * @return whether this regatta defines its local per-series result discarding rules; if so, any leaderboard based
     *         on the regatta has to respect this and has to use a result discarding rule implementation that
     *         keeps discards local to each series rather than spreading them across the entire leaderboard.
     */
    public boolean definesSeriesDiscardThresholds() {
        for (SeriesDTO s : series) {
            if (s.definesSeriesDiscardThresholds()) {
                return true;
            }
        }
        return false;
    }

    public Distance getCalculatedBuoyZoneRadius() {
        Distance boatHullLength = boatClass == null ? null : boatClass.getHullLength();
        double hullLengthFactor = this.buoyZoneRadiusInHullLengths == null ? Regatta.DEFAULT_BUOY_ZONE_RADIUS_IN_HULL_LENGTHS : this.buoyZoneRadiusInHullLengths;
        return boatHullLength == null ? RaceMapSettings.DEFAULT_BUOY_ZONE_RADIUS : boatHullLength.scale(hullLengthFactor);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((scoringScheme == null) ? 0 : scoringScheme.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        RegattaDTO other = (RegattaDTO) obj;
        if (getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!getName().equals(other.getName()))
            return false;
        if (scoringScheme != other.scoringScheme)
            return false;
        return true;
    }
    
    @Override
    public HasPermissions getPermissionType() {
        return SecuredDomainType.REGATTA;
    }
    
    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    public TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return new TypeRelativeObjectIdentifier(getName());
    }

}
