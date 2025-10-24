package com.sap.sailing.gwt.home.communication.event;

import java.io.Serializable;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sse.common.CountryCode;
import com.sap.sse.gwt.shared.DTO;
import com.sap.sse.security.shared.dto.NamedDTO;

public class SimpleCompetitorDTO extends NamedDTO implements DTO, Serializable, Comparable<SimpleCompetitorDTO> {

    private static final long serialVersionUID = -5743976446085202047L;
    
    private String shortInfo;
    private String twoLetterIsoCountryCode;
    private String flagImageURL;

    @Deprecated
    SimpleCompetitorDTO() {} // for GWT RPC serialization only
    
    @GwtIncompatible
    public SimpleCompetitorDTO(Competitor competitor) {
        super(competitor.getName());
        final Nationality nationality = competitor.getTeam().getNationality();
        CountryCode countryCode = nationality == null ? null : nationality.getCountryCode();
        this.twoLetterIsoCountryCode = countryCode == null ? null : countryCode.getTwoLetterISOCode();
        this.flagImageURL = competitor.getFlagImage() == null ? null : competitor.getFlagImage().toString();
        this.shortInfo = competitor.getShortInfo();
        if (shortInfo == null) {
            this.shortInfo = competitor.getName();
        }
    }

    @GwtIncompatible
    public SimpleCompetitorDTO(CompetitorDTO competitor) {
        super(competitor.getName());
        this.shortInfo = competitor.getShortInfo();
        if (shortInfo == null) {
            this.shortInfo = competitor.getName();
        }
        this.twoLetterIsoCountryCode = competitor.getTwoLetterIsoCountryCode();
        this.flagImageURL = competitor.getFlagImageURL();
    }

    public SimpleCompetitorDTO(String name, String shortInfo, String twoLetterIsoCountryCode, String flagImageURL) {
        super(name);
        this.shortInfo = shortInfo;
        this.twoLetterIsoCountryCode = twoLetterIsoCountryCode;
        this.flagImageURL = flagImageURL;
    }

    public String getShortInfo() {
        return shortInfo;
    }

    public void setShortInfo(String shortInfo) {
        this.shortInfo = shortInfo;
    }

    public String getTwoLetterIsoCountryCode() {
        return twoLetterIsoCountryCode;
    }

    public void setTwoLetterIsoCountryCode(String twoLetterIsoCountryCode) {
        this.twoLetterIsoCountryCode = twoLetterIsoCountryCode;
    }

    public String getFlagImageURL() {
        return flagImageURL;
    }

    public void setFlagImageURL(String flagImageURL) {
        this.flagImageURL = flagImageURL;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((shortInfo == null) ? 0 : shortInfo.hashCode());
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
        SimpleCompetitorDTO other = (SimpleCompetitorDTO) obj;
        if (shortInfo == null) {
            if (other.shortInfo != null)
                return false;
        } else if (!shortInfo.equals(other.shortInfo))
            return false;
        return true;
    }
    
    @Override
    public int compareTo(SimpleCompetitorDTO obj) {
        int compareShortInfos = this.shortInfo.compareTo(obj.shortInfo);
        return compareShortInfos == 0 ? this.getName().compareTo(obj.getName()): compareShortInfos;
    }
    
}
