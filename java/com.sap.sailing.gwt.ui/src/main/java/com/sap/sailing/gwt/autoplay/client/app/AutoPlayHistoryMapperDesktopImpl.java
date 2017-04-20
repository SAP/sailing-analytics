package com.sap.sailing.gwt.autoplay.client.app;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import com.sap.sailing.gwt.autoplay.client.place.player.PlayerPlace;
import com.sap.sailing.gwt.autoplay.client.places.startclassic.StartPlace;

@WithTokenizers({ StartPlace.Tokenizer.class, PlayerPlace.Tokenizer.class })
public interface AutoPlayHistoryMapperDesktopImpl extends PlaceHistoryMapper {
}
