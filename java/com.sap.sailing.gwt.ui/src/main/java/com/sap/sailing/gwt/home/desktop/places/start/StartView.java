package com.sap.sailing.gwt.home.desktop.places.start;

import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.start.StartViewDTO;
import com.sap.sailing.gwt.home.shared.partials.anniversary.AnniversariesView;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public interface StartView {
    Widget asWidget();
    
    void setData(StartViewDTO data, TakedownNoticeService takedownNoticeService);

    AnniversariesView getAnniversariesView();
}
