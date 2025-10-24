package com.sap.sailing.gwt.autoplay.client.configs.impl;

import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayContextImpl;
import com.sap.sailing.gwt.autoplay.client.configs.AutoPlayConfiguration;
import com.sap.sailing.gwt.autoplay.client.configs.AutoPlayContextDefinition;
import com.sap.sailing.gwt.autoplay.client.nodes.RootNodeSixtyInch;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;

public class AutoPlaySixtyInchConfiguration extends AutoPlayConfiguration {
    @Override
    public void startRootNode(AutoPlayClientFactory cf, AutoPlayContextDefinition context,
            PerspectiveCompositeSettings<?> settings, EventDTO initialEventData) {
        cf.setAutoPlayContext(new AutoPlayContextImpl(this, context, initialEventData));
        // start sixty inch slide loop nodes...
        RootNodeSixtyInch root = new RootNodeSixtyInch(cf);
        root.start(cf.getEventBus());
    }
}
