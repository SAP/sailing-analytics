package com.sap.sailing.domain.common.dto;

/**
 * Equality and hash code are based on the {@link #getIdAsString() ID}, the {@link #getSailID() sail number}, the
 * {@link #getBoatClass() boat class} (whose equality and hash code, in turn, depends on its name) and the
 * {@link #getThreeLetterIocCountryCode() IOC country code}. Note that the three latter properties are subject
 * to change for a competitor while the ID remains unchanged.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface CompetitorDTO extends CompetitorWithoutBoatDTO {
    String getSailID();
    
    BoatDTO getBoat();

    void setBoat(BoatDTO boat);

    // TODO bug2822: Should we remove this?
    BoatClassDTO getBoatClass();

    /**
     * A regular instance will simply return this object. A compacted version may compute the result by looking it up
     * from the previous version of the enclosing leaderboard.
     */
    CompetitorDTO getCompetitorFromPrevious(LeaderboardDTO previousVersion);
}
