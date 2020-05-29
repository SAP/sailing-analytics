package com.sap.sailing.server.gateway.serialization.impl;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.Team;
import com.sap.sailing.domain.common.tracking.impl.CompetitorJsonConstants;
import com.sap.sailing.server.gateway.deserialization.impl.BoatClassJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sse.common.Color;
import com.sap.sse.common.CountryCode;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.PublicReadableActions;

/**
 * Serializes a {@link Competitor} or {@link CompetitorWithBoat} object. If a {@link CompetitorWithBoat} object,
 * the {@link CompetitorJsonConstants#FIELD_BOAT} field will be filled with a boat document as serialized by the
 * {@link BoatJsonSerializer}. For {@link Competitor} objects with {@link Competitor#hasBoat()}{@code == false} the
 * {@link CompetitorJsonConstants#FIELD_BOAT} field will not be set. This can also be used by the corresponding
 * de-serializer to identify which class implementing the {@link Competitor} interface to instantiate. 
 */
public class CompetitorJsonSerializer implements JsonSerializer<Competitor> {
    public static final String FIELD_ID = "id";
    
    private final JsonSerializer<Team> teamJsonSerializer;
    private final JsonSerializer<Boat> boatJsonSerializer;
    
    /**
     * The {@link DefaultActions#READ} permission is required for certain fields such as the e-mail address;
     * for other fields the {@link PublicReadableActions#READ_PUBLIC} permission suffices. If this field is set to
     * {@code true}, also the non-public fields will be serialized.
     */
    private final boolean serializeNonPublicFields;

    /**
     * Creates a serializer for {@link Competitor} objects which by default, if the competitor
     * {@link Competitor#hasBoat() has a boat attached} will serialize the boat in the
     * {@link CompetitorJsonConstants#FIELD_BOAT} field of the resulting document. See {@link #create(boolean)} for a
     * variant where callers can choose whether the {@link CompetitorJsonConstants#FIELD_BOAT} field shall be filled or
     * not.
     */
    public static CompetitorJsonSerializer create() {
        return create(/* serialize boat */ true);
    }

    /**
     * Creates a serializer for {@link Competitor} objects which if {@code serializeBoat==true} and if the competitor
     * {@link Competitor#hasBoat() has a boat attached} will serialize the boat in the
     * {@link CompetitorJsonConstants#FIELD_BOAT} field of the resulting document.
     */
    public static CompetitorJsonSerializer create(boolean serializeBoat) {
        return new CompetitorJsonSerializer(TeamJsonSerializer.create(), serializeBoat?BoatJsonSerializer.create():null,
                /* serializeNonPublicFields */ false);
    }

    /**
     * Creates a serializer for {@link Competitor} objects which if {@code serializeBoat==true} and if the competitor
     * {@link Competitor#hasBoat() has a boat attached} will serialize the boat in the
     * {@link CompetitorJsonConstants#FIELD_BOAT} field of the resulting document. The boat class will be
     * serialized in verbose format. See also {@link #create(boolean, boolean, boolean)}.
     */
    public static CompetitorJsonSerializer create(boolean serializeBoat, boolean serializeNonPublicCompetitorFields) {
        return create(serializeBoat, serializeNonPublicCompetitorFields, /* verboseBoatClassSerializer */ true);
    }

    /**
     * Creates a serializer for {@link Competitor} objects which if {@code serializeBoat==true} and if the competitor
     * {@link Competitor#hasBoat() has a boat attached} will serialize the boat in the
     * {@link CompetitorJsonConstants#FIELD_BOAT} field of the resulting document. If {@code verboseBoatClassSerializer}
     * is {@code true}, a verbose set of fields describing the boat class is serialized with every boat. If
     * {@code false}, the serialized boat class will still have enough information to be de-serialized by
     * {@link BoatClassJsonDeserializer}.
     */
    public static CompetitorJsonSerializer create(boolean serializeBoat, boolean serializeNonPublicCompetitorFields,
            boolean verboseBoatClassSerializer) {
        return new CompetitorJsonSerializer(TeamJsonSerializer.create(),
                serializeBoat ? new BoatJsonSerializer(new BoatClassJsonSerializer(verboseBoatClassSerializer)) : null,
                serializeNonPublicCompetitorFields);
    }

    public CompetitorJsonSerializer() {
        this(null, null, /* serializeNonPublicFields */ false);
    }

    public CompetitorJsonSerializer(JsonSerializer<Team> teamJsonSerializer, JsonSerializer<Boat> boatJsonSerializer, boolean serializeNonPublicFields) {
        this.teamJsonSerializer = teamJsonSerializer;
        this.boatJsonSerializer = boatJsonSerializer;
        this.serializeNonPublicFields = serializeNonPublicFields;
    }
    
    public static JSONObject getCompetitorIdQuery(Competitor competitor) {
        JSONObject result = new JSONObject();
        Serializable competitorId = competitor.getId() instanceof UUID ? competitor.getId().toString() : competitor.getId();
        result.put(FIELD_ID, competitorId);
        return result;
    }

    @Override
    public JSONObject serialize(Competitor competitor) {
        JSONObject result = new JSONObject();
        // Special treatment for UUIDs. They are represented as String because JSON doesn't have a way to represent them otherwise.
        // However, other, e.g., numeric, types used to encode a serializable ID must be preserved according to JSON semantics.
        // Also see the corresponding case distinction in the deserialized which first tries to parse a string as a UUID becore
        // returning the ID as is.
        result.put(CompetitorJsonConstants.FIELD_ID_TYPE, competitor.getId().getClass().getName());
        Set<Entry<Object, Object>> entries = getCompetitorIdQuery(competitor).entrySet();
        for (Entry<Object, Object> idKeyAndValue : entries) {
            result.put(idKeyAndValue.getKey(), idKeyAndValue.getValue());
        }
        result.put(CompetitorJsonConstants.FIELD_NAME, competitor.getName());
        result.put(CompetitorJsonConstants.FIELD_SHORT_NAME, competitor.getShortName());
        if (competitor.hasBoat()) {
            result.put(CompetitorJsonConstants.FIELD_SAIL_ID, ((CompetitorWithBoat) competitor).getBoat().getSailID());
        }
        Color color = getColor(competitor);
        result.put(CompetitorJsonConstants.FIELD_DISPLAY_COLOR, color == null ? null : color.getAsHtml());
        if (serializeNonPublicFields) {
            result.put(CompetitorJsonConstants.FIELD_EMAIL, competitor.getEmail());
        }
        result.put(CompetitorJsonConstants.FIELD_SEARCHTAG, competitor.getSearchTag());
        final Nationality nationality = competitor.getTeam() == null ? null : competitor.getTeam().getNationality();
        result.put(CompetitorJsonConstants.FIELD_NATIONALITY, nationality == null ? "" : nationality.getThreeLetterIOCAcronym());
        CountryCode countryCode = nationality == null ? null : nationality.getCountryCode();
        result.put(CompetitorJsonConstants.FIELD_NATIONALITY_ISO2, countryCode == null ? "" : countryCode.getTwoLetterISOCode());
        result.put(CompetitorJsonConstants.FIELD_NATIONALITY_ISO3, countryCode == null ? "" : countryCode.getThreeLetterISOCode());
        if (competitor.getFlagImage() != null) {
            result.put(CompetitorJsonConstants.FIELD_FLAG_IMAGE_URI, competitor.getFlagImage().toString());
        }
        if (teamJsonSerializer != null) {
            result.put(CompetitorJsonConstants.FIELD_TEAM, teamJsonSerializer.serialize(competitor.getTeam()));
        }
        if (boatJsonSerializer != null && competitor.hasBoat()) {
            result.put(CompetitorJsonConstants.FIELD_BOAT, boatJsonSerializer.serialize(((CompetitorWithBoat) competitor).getBoat()));
        }
        result.put(CompetitorJsonConstants.FIELD_TIME_ON_TIME_FACTOR, competitor.getTimeOnTimeFactor());
        result.put(CompetitorJsonConstants.FIELD_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE,
                competitor.getTimeOnDistanceAllowancePerNauticalMile() == null ? null :
                    competitor.getTimeOnDistanceAllowancePerNauticalMile().asSeconds());
        
        // adding redundant values to migrate CompetitorsResource to use this serializer instead of its own
        result.put(CompetitorJsonConstants.FIELD_COLOR, color == null ? null : color.getAsHtml());
        result.put(CompetitorJsonConstants.FIELD_COUNTRY_CODE,
                countryCode == null ? "" : countryCode.getTwoLetterISOCode());
        result.put(CompetitorJsonConstants.FIELD_FLAG_IMAGE,
                competitor.getFlagImage() != null ? competitor.getFlagImage().toString() : null);
        result.put(CompetitorJsonConstants.FIELD_TEAM_IMAGE_URI,
                competitor.getTeam() != null
                        ? competitor.getTeam().getImage() != null ? competitor.getTeam().getImage().toString() : null
                        : null);
        return result;
    }

    protected Color getColor(Competitor competitor) {
        return competitor.getColor();
    }
}
