package com.sap.sailing.gwt.autoplay.client.places.autoplaystart;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.autoplay.client.configs.AutoPlayContextDefinition;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;

public interface AutoPlayStartView {
    public interface Presenter {
        void startRootNode(AutoPlayContextDefinition ctxDef,
                PerspectiveCompositeSettings<?> settings);

        void handleLocaleChange(String selectedLocale);
    }

    void setCurrentPresenter(Presenter currentPresenter);
    Widget asWidget();
    
    void setEvents(List<EventDTO> events);

}
