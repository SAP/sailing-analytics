package com.sap.sailing.domain.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Given a string that is assumed to contain a "sail number" or "sail ID" this class offers methods to canonicalize and
 * parse that string in order to make matches across different systems that handle such sail numbers / IDs. For example,
 * the canonicalization process ignores case and whitespace, and offers to use a default nationality when comparing in
 * case a sail number doesn't have a nationality.
 * <p>
 * 
 * Since the logic is intended to work for both, browser GWT client as well as OSGi Java back-end, and because GWT does
 * not have a regular expression library that is compatible with the Java default regexp library, the regular expression
 * constant needs to be turned into a regular expression matcher in a "platform-specific" way, making this class
 * abstract.
 * 
 * @param <CompetitorType>
 *            the type of competitor; different for GWT client where it will be the {@code CompetitorDTO}, and back-end
 *            where it will be the {@link Competitor}, leading to specific method implementations for extracting the
 *            sail number
 * 
 * @author Axel Uhl (D043530)
 *
 */
public abstract class AbstractSailNumberCanonicalizerAndMatcher<CompetitorType> {
    protected static final String sailIdRegexpPattern = "^([A-Z][A-Z][A-Z])?\\s*[^0-9]*([0-9]*)$";

    public static class SailNumberMatch {
        private final String iocCode;
        private final String number;
        public SailNumberMatch(String iocCode, String number) {
            super();
            this.iocCode = iocCode;
            this.number = number;
        }
        public String getIocCode() {
            return iocCode;
        }
        public String getNumber() {
            return number;
        }
    }
    
    /**
     * Parses a sail number into a nationality code expected at the beginning of the string but possibly empty,
     * and a numeric part assumed to be at the end of the string. Before matching, the {@code sailId}
     * string will be {@link String#trim() trimmed}, removing leading and trailing whitespace. If
     * the parameter matches the {@link #sailIdRegexpPattern} then a valid {@link SailNumberMatch}
     * object is returned which may, however, deliver empty or {@code null} parts for the
     * {@link SailNumberMatch#getIocCode() IOC code} or the {@link SailNumberMatch#getNumber() number}.
     * If the string is not matched, {@code null} is returned.
     */
    abstract protected SailNumberMatch match(String sailId);
    
    /**
     * Try to match three-letter country code and number, optionally separated by whitespaces. If there is no match,
     * use the first 20 characters of the sailID.
     */
    public String canonicalizeSailID(String sailID, String defaultNationality) {
        String result = null;
        SailNumberMatch m = match(sailID);
        if (m != null) {
            String iocCode = m.getIocCode();
            if (iocCode != null) {
                iocCode = iocCode.toUpperCase();
            }
            if (defaultNationality != null && (iocCode == null || iocCode.trim().length() == 0)) {
                iocCode = defaultNationality.toUpperCase();
            }
            if (iocCode != null && iocCode.trim().length() > 0) {
                String number = m.getNumber();
                result = iocCode + number;
            }
        }
        if (result == null) {
            result = sailID.substring(0, Math.min(20, sailID.length()));
        }
        return result;
    }
    
    /**
     * Maps the sail IDs contained in {@code sailNumbers} to the {@code competitors} passed. The match making ignores
     * all whitespaces in the sail IDs on both sides (see {@link #canonicalizeSailID(String, String)}). If a
     * competitor's sail number does not start with a letter it is assumed the country code is missing. In this case,
     * {@link #getThreeLetterIocCountryCode(Object) it} is prepended before comparing to the sail ID from
     * {@code sailNumbers}. The sail ID number is extracted by trimming and using all trailing digits.
     * @return 
     * 
     * @return a map mapping the sailIDs as found in {@code sailNumbers} to the {@code competitors}; values may be
     *         {@code null} if no matching competitor was found for the sail ID in the {@code competitors} collection
     */
    public Map<String, CompetitorType> mapCompetitorsAndInitializeAllOfficialRaceIDs(final Iterable<CompetitorType> competitors, Iterable<String> sailNumbers) {
        final Map<String, CompetitorType> result = new HashMap<>();
        final Map<String, CompetitorType> canonicalizedSailIDToCompetitors = canonicalizeLeaderboardSailIDs(competitors);
        for (final String sailNumber : sailNumbers) {
            final String canonicalizedSailNumber = canonicalizeSailID(sailNumber, /* defaultNationality */ null);
            final CompetitorType competitor = canonicalizedSailIDToCompetitors.get(canonicalizedSailNumber);
            result.put(sailNumber, competitor);
        }
        return result;
    }

    public Map<String, CompetitorType> canonicalizeLeaderboardSailIDs(final Iterable<CompetitorType> competitors) {
        final Map<String, CompetitorType> result = new HashMap<>();
        for (final CompetitorType competitor : competitors) {
            final String competitorIdentifyingText = getCompetitorIdentifyingText(competitor);
            final String canonicalizedSailID = canonicalizeSailID(competitorIdentifyingText.trim(), getThreeLetterIocCountryCode(competitor).trim());
            if (canonicalizedSailID != null) {
                result.put(canonicalizedSailID, competitor);
            }
        }
        return result;
    }

    abstract protected String getThreeLetterIocCountryCode(CompetitorType competitor);
    
    abstract protected String getCompetitorIdentifyingText(CompetitorType competitor);
}
