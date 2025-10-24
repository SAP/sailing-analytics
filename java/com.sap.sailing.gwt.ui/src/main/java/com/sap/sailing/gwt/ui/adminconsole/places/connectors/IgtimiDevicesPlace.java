package com.sap.sailing.gwt.ui.adminconsole.places.connectors;

import java.util.function.Function;

import com.sap.sse.gwt.adminconsole.AbstractFilterablePlace;

public class IgtimiDevicesPlace extends AbstractFilterablePlace {

    public IgtimiDevicesPlace(String token) {
        super(token);
    }
    
    public static class Tokenizer extends TablePlaceTokenizer<IgtimiDevicesPlace> {      

        @Override
        protected Function<String, IgtimiDevicesPlace> getPlaceFactory() {
            return IgtimiDevicesPlace::new;
        }
    }
}
