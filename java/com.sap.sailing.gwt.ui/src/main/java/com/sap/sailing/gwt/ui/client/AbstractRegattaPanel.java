package com.sap.sailing.gwt.ui.client;

import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.text.client.DateTimeFormatRenderer;
import com.google.gwt.user.client.ui.FormPanel;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;

//TODO: Do not inherit from FormPanel since the provided functionality is never used!
public abstract class AbstractRegattaPanel extends FormPanel implements RegattaDisplayer {
    protected final SailingServiceAsync sailingService;
    protected DateTimeFormatRenderer dateFormatter = new DateTimeFormatRenderer(DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT));
    protected DateTimeFormatRenderer timeFormatter = new DateTimeFormatRenderer(DateTimeFormat.getFormat(PredefinedFormat.TIME_LONG));
    protected final RegattaRefresher regattaRefresher;
    protected ErrorReporter errorReporter;
    protected StringMessages stringMessages;
    
    public AbstractRegattaPanel(SailingServiceAsync sailingService,
            RegattaRefresher regattaRefresher, ErrorReporter errorReporter, StringMessages stringMessages) {
        super();
        this.sailingService = sailingService;
        this.regattaRefresher = regattaRefresher;
        this.errorReporter  = errorReporter;
        this.stringMessages = stringMessages;
    }

    @Override
    public abstract void fillRegattas(List<RegattaDTO> result);
    
}
