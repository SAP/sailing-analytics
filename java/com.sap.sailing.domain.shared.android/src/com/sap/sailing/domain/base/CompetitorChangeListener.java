package com.sap.sailing.domain.base;

import com.sap.sailing.domain.common.Color;

public interface CompetitorChangeListener extends BoatChangeListener, NationalityChangeListener {
    void colorChanged(Color oldColor, Color newColor);
    
    void nameChanged(String oldName, String newName);
}
