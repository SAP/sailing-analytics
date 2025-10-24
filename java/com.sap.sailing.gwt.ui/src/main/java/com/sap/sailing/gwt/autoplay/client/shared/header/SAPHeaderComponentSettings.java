package com.sap.sailing.gwt.autoplay.client.shared.header;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.StringSetting;

public class SAPHeaderComponentSettings extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -4504543757235801224L;
    
    private StringSetting title;

    @Override
    protected void addChildSettings() {
        title = new StringSetting("title", this,"");
    }
    
    public SAPHeaderComponentSettings(String title) {
        super();
        this.title.setValue(title);
    }

    public String getTitle() {
        return title.getValue();
    }
}
