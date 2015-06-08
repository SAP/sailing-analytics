package com.sap.sailing.server.gateway.deserialization.impl;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.UUID;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorFactory;
import com.sap.sailing.domain.base.CompetitorStore;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sse.common.Color;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.RGBColor;

public class CompetitorJsonDeserializer implements JsonDeserializer<Competitor> {
    protected final CompetitorFactory competitorStore;
    protected final JsonDeserializer<DynamicTeam> teamJsonDeserializer;
    protected final JsonDeserializer<DynamicBoat> boatJsonDeserializer;
    private static final Logger logger = Logger.getLogger(CompetitorJsonDeserializer.class.getName());

    public static CompetitorJsonDeserializer create(SharedDomainFactory baseDomainFactory) {
        return new CompetitorJsonDeserializer(baseDomainFactory, new TeamJsonDeserializer(new PersonJsonDeserializer(
                new NationalityJsonDeserializer(baseDomainFactory))), new BoatJsonDeserializer(new BoatClassJsonDeserializer(baseDomainFactory)));
    }

    public CompetitorJsonDeserializer(CompetitorStore store) {
        this(store, null, /* boatDeserializer */ null);
    }

    public CompetitorJsonDeserializer(CompetitorFactory competitorStore, JsonDeserializer<DynamicTeam> teamJsonDeserializer, JsonDeserializer<DynamicBoat> boatDeserializer) {
        this.competitorStore = competitorStore;
        this.teamJsonDeserializer = teamJsonDeserializer;
        this.boatJsonDeserializer = boatDeserializer;
    }

    @Override
    public Competitor deserialize(JSONObject object) throws JsonDeserializationException {
        Serializable competitorId = (Serializable) object.get(CompetitorJsonSerializer.FIELD_ID);
        try {
            Class<?> idClass = Class.forName((String) object.get(CompetitorJsonSerializer.FIELD_ID_TYPE));
            if (Number.class.isAssignableFrom(idClass)) {
                Constructor<?> constructorFromString = idClass.getConstructor(String.class);
                competitorId = (Serializable) constructorFromString.newInstance(competitorId.toString());
            } else if (UUID.class.isAssignableFrom(idClass)) {
                competitorId = Helpers.tryUuidConversion(competitorId);
            }
            String name = (String) object.get(CompetitorJsonSerializer.FIELD_NAME);
            String displayColorAsString = (String) object.get(CompetitorJsonSerializer.FIELD_DISPLAY_COLOR);
            String email = (String) object.get(CompetitorJsonSerializer.FIELD_EMAIL);
            
            URI flagImageURI = null;
            String flagImageURIAsString = (String) object.get(CompetitorJsonSerializer.FIELD_FLAG_IMAGE_URI);
            if (flagImageURIAsString != null) {
                try {
                    flagImageURI = URI.create(flagImageURIAsString);
                } catch (IllegalArgumentException e) {
                    logger.warning("Illegal flag image URI " + e.getMessage());
                }
            }

            final Color displayColor;
            if (displayColorAsString == null || displayColorAsString.isEmpty()) {
                displayColor = null;
            } else {
                displayColor = new RGBColor(displayColorAsString);
            }
            DynamicTeam team = null;
            DynamicBoat boat = null;
            if (teamJsonDeserializer != null && object.get(CompetitorJsonSerializer.FIELD_TEAM) != null) {
                team = teamJsonDeserializer.deserialize(Helpers.getNestedObjectSafe(object,
                        CompetitorJsonSerializer.FIELD_TEAM));
            }
            if (boatJsonDeserializer != null && object.get(CompetitorJsonSerializer.FIELD_BOAT) != null) {
                boat = boatJsonDeserializer.deserialize(Helpers.getNestedObjectSafe(object,
                        CompetitorJsonSerializer.FIELD_BOAT));
            }
            final Double timeOnTimeFactor = (Double) object.get(CompetitorJsonSerializer.FIELD_TIME_ON_TIME_FACTOR);
            final Double timeOnDistanceAllowanceInSecondsPerNauticalMile = (Double) object
                    .get(CompetitorJsonSerializer.FIELD_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE);
            Competitor competitor = competitorStore.getOrCreateCompetitor(competitorId, name, displayColor, email,
                    flagImageURI, team, boat, timeOnTimeFactor,
                    timeOnDistanceAllowanceInSecondsPerNauticalMile == null ? null :
                        new MillisecondsDurationImpl((long) (timeOnDistanceAllowanceInSecondsPerNauticalMile*1000)));
            return competitor;
        } catch (Exception e) {
            throw new JsonDeserializationException(e);
        }
    }
}
