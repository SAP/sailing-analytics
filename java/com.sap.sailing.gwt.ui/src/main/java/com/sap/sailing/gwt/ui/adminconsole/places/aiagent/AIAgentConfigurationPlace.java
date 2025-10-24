package com.sap.sailing.gwt.ui.adminconsole.places.aiagent;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.sap.sse.gwt.adminconsole.AbstractAdminConsolePlace;

public class AIAgentConfigurationPlace extends AbstractAdminConsolePlace {
    public AIAgentConfigurationPlace(String token) {
        super(token);
    }

    public static class Tokenizer implements PlaceTokenizer<AIAgentConfigurationPlace> {
        @Override
        public String getToken(final AIAgentConfigurationPlace place) {
            return place.getParametersAsToken();
        }

        @Override
        public AIAgentConfigurationPlace getPlace(final String token) {
            return new AIAgentConfigurationPlace(token);
        }
    }
}
