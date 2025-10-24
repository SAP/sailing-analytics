package com.sap.sailing.gwt.home.shared.places.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.gwt.http.client.URL;
import com.google.gwt.place.shared.Place;
import com.sap.sailing.gwt.common.client.AbstractMapTokenizer;
import com.sap.sailing.gwt.home.shared.app.HasLocationTitle;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Base64Utils;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.shared.ClientConfiguration;

public abstract class AbstractEventPlace extends Place implements HasLocationTitle {
    private static final Logger logger = Logger.getLogger(AbstractEventPlace.class.getName());

    private final EventContext ctx;

    protected AbstractEventPlace(EventContext ctx) {
        this.ctx = ctx;
    }

    public EventContext getCtx() {
        return ctx;
    }

    public AbstractEventPlace(String eventUuidAsString) {
        this.ctx = new EventContext();
        ctx.withId(eventUuidAsString);
    }

    public String getTitle(String eventName) {
        return (ClientConfiguration.getInstance().isBrandingActive() ? StringMessages.INSTANCE.sapSailing()
                : StringMessages.INSTANCE.whitelabelSailing()) + " - " + eventName;
    }

    @Override
    public String getLocationTitle() {
        return StringMessages.INSTANCE.events();
    }

    public String getEventUuidAsString() {
        return ctx.getEventId();
    }

    public String getRegattaId() {
        return ctx.getRegattaId();
    }

    public static abstract class Tokenizer<PLACE extends AbstractEventPlace> extends AbstractMapTokenizer<PLACE> {
        /**
         * See also {@code TokenizedHomePlaceUrlBuilder.EVENT_ID_PARAM}
         */
        private final static String PARAM_EVENTID = "eventId";

        /**
         * See also {@code TokenizedHomePlaceUrlBuilder.REGATTA_ID_PARAM}; expected to be a Base64 string that has been URL-encoded,
         * so that the optional trailing '=' characters are properly encoded as %3D, for example.
         */
        private final static String PARAM_REGATTAID = "regattaId";

        protected PLACE getPlaceFromParameters(Map<String, Set<String>> parameters) {
            final String encodedRegattaId = extractSingleParameter(parameters, PARAM_REGATTAID);
            String decodedRegattaId;
            try {
                decodedRegattaId = encodedRegattaId==null?null:new String(Base64Utils.fromBase64(encodedRegattaId));
            } catch (Throwable e) {
                logger.warning("Error trying to decode regatta ID "+encodedRegattaId+"; trying to use URL decoding to obtain regatta name");
                decodedRegattaId = URL.decodeQueryString(encodedRegattaId);
            }
            // see bug 6088: regatta names may contain any UTF character and therefore need encoding
            return getRealPlace(new EventContext().withId(extractSingleParameter(parameters, PARAM_EVENTID))
                    .withRegattaId(decodedRegattaId), parameters);
        }

        private String extractSingleParameter(Map<String, Set<String>> parameters, String key) {
            Set<String> param = parameters.get(key);
            return param == null ? null : param.stream().findFirst().orElse(null);
        }

        protected Map<String, Set<String>> getParameters(PLACE place) {
            Map<String, Set<String>> parameters = new HashMap<>();
            EventContext context = place.getCtx();
            Util.addToValueSet(parameters, PARAM_EVENTID, context.getEventId());
            String regattaId = context.getRegattaId(); // bug 6088: we assume that a regatta id/name can contain any UTF character and therefore needs encoding
            if (regattaId != null && !regattaId.isEmpty()) {
                Util.addToValueSet(parameters, PARAM_REGATTAID, Base64Utils.toBase64(context.getRegattaId().getBytes()));
            }
            return parameters;
        }

        protected PLACE getRealPlace(EventContext context, Map<String, Set<String>> parameters) {
            return getRealPlace(context);
        }

        protected abstract PLACE getRealPlace(EventContext context);
    }
}
