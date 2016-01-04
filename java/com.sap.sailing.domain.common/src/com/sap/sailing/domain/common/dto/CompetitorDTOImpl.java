package com.sap.sailing.domain.common.dto;

import java.io.Serializable;

import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;

/**
 * Equality and hash code are based on the {@link #getIdAsString() ID}, the {@link #getSailID() sail number}, the
 * {@link #getBoatClass() boat class} (whose equality and hash code, in turn, depends on its name) and the
 * {@link #getThreeLetterIocCountryCode() IOC country code}. Note that the three latter properties are subject
 * to change for a competitor while the ID remains unchanged.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class CompetitorDTOImpl extends NamedDTO implements CompetitorDTO, Serializable {
    private static final long serialVersionUID = -4997852354821083154L;
    private String countryName;
    private String twoLetterIsoCountryCode;
    private String threeLetterIocCountryCode;
    private Color color;
    private String email;
    private String idAsString;
    private BoatClassDTO boatClass;
    private BoatDTO boat;
    private String imageURL;
    private String flagImageURL;
    private Double timeOnTimeFactor;
    private Duration timeOnDistanceAllowancePerNauticalMile;
    
    public CompetitorDTOImpl() {}
    
    public CompetitorDTOImpl(String name, Color color, String email, String twoLetterIsoCountryCode, String threeLetterIocCountryCode,
            String countryName, String idAsString, String imageURL, String flagImageURL, 
            BoatDTO boat, BoatClassDTO boatClass, Double timeOnTimeFactor, Duration timeOnDistanceAllowancePerNauticalMile) {
        super(name);
        this.color = color;
        this.email = email;
        this.twoLetterIsoCountryCode = twoLetterIsoCountryCode;
        this.threeLetterIocCountryCode = threeLetterIocCountryCode;
        this.countryName = countryName;
        this.idAsString = idAsString;
        this.imageURL = imageURL;
        this.flagImageURL = flagImageURL;
        this.boat = boat;
        this.boatClass = boatClass;
        this.timeOnTimeFactor = timeOnTimeFactor;
        this.timeOnDistanceAllowancePerNauticalMile = timeOnDistanceAllowancePerNauticalMile;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((boatClass == null) ? 0 : boatClass.hashCode());
        result = prime * result + ((boat == null) ? 0 : boat.hashCode());
        result = prime * result + ((idAsString == null) ? 0 : idAsString.hashCode());
        result = prime * result + ((color == null) ? 0 : color.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((threeLetterIocCountryCode == null) ? 0 : threeLetterIocCountryCode.hashCode());
        result = prime * result + ((imageURL == null) ? 0 : imageURL.hashCode());
        result = prime * result + ((flagImageURL == null) ? 0 : flagImageURL.hashCode());
        result = prime * result + ((timeOnTimeFactor == null) ? 0 : timeOnTimeFactor.hashCode());
        result = prime * result + ((timeOnDistanceAllowancePerNauticalMile == null) ? 0 : timeOnDistanceAllowancePerNauticalMile.hashCode());
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
        CompetitorDTOImpl other = (CompetitorDTOImpl) obj;
        if (boatClass == null) {
            if (other.boatClass != null)
                return false;
        } else if (!boatClass.equals(other.boatClass))
            return false;
        if (boat == null) {
            if (other.boat != null)
                return false;
        } else if (!boat.equals(other.boat))
            return false;
        if (idAsString == null) {
            if (other.idAsString != null)
                return false;
        } else if (!idAsString.equals(other.idAsString))
            return false;
        if (threeLetterIocCountryCode == null) {
            if (other.threeLetterIocCountryCode != null)
                return false;
        } else if (!threeLetterIocCountryCode.equals(other.threeLetterIocCountryCode))
            return false;
        if (color == null) {
            if (other.color != null)
                return false;
        } else if (!color.equals(other.color))
            return false;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        if (imageURL == null) {
            if (other.imageURL != null)
                return false;
        } else if (!imageURL.equals(other.imageURL))
            return false;
        if (flagImageURL == null) {
            if (other.flagImageURL != null)
                return false;
        } else if (!flagImageURL.equals(other.flagImageURL))
            return false;
        if (timeOnTimeFactor == null) {
            if (other.timeOnTimeFactor != null)
                return false;
        } else if (!timeOnTimeFactor.equals(other.timeOnTimeFactor))
            return false;
        if (timeOnDistanceAllowancePerNauticalMile == null) {
            if (other.timeOnDistanceAllowancePerNauticalMile != null)
                return false;
        } else if (!timeOnDistanceAllowancePerNauticalMile.equals(other.timeOnDistanceAllowancePerNauticalMile))
            return false;
        return true;
    }

    @Override
    public String getTwoLetterIsoCountryCode() {
        return twoLetterIsoCountryCode;
    }

    @Override
    public String getThreeLetterIocCountryCode() {
        return threeLetterIocCountryCode;
    }

    @Override
    public String getCountryName() {
        return countryName;
    }

    @Override
    public String getSailID() {
        return boat==null?null:boat.getSailId();
    }
    
    @Override
    public String getImageURL() {
        return imageURL;
    }

    @Override
    public String getFlagImageURL() {
        return flagImageURL;
    }

    @Override
    public String getIdAsString() {
        return idAsString;
    }

    @Override
    public BoatClassDTO getBoatClass() {
        return boatClass;
    }

    @Override
    public CompetitorDTO getCompetitorFromPrevious(LeaderboardDTO previousVersion) {
        return this;
    }

    public Color getColor() {
        return color;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String email(){
        return email;
    }

    @Override
    public boolean hasEmail() {
        return email != null && !email.isEmpty();
    }

    @Override
    public Double getTimeOnTimeFactor() {
        return timeOnTimeFactor;
    }

    @Override
    public Duration getTimeOnDistanceAllowancePerNauticalMile() {
        return timeOnDistanceAllowancePerNauticalMile;
    }

    @Override
    public BoatDTO getBoat() {
        return boat;
    }
}
