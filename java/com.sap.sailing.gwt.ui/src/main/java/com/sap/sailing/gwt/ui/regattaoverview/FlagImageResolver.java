package com.sap.sailing.gwt.ui.regattaoverview;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.sap.sailing.domain.common.racelog.Flags;

public class FlagImageResolver {
    
    private Map<Flags, ImageResource> flagImageMap;
    
    private static RegattaRaceStatesFlagsResources resources = GWT.create(RegattaRaceStatesFlagsResources.class);
    
    public FlagImageResolver() {
        this.flagImageMap = new HashMap<Flags, ImageResource>();
        initializeFlagImageMap();
    }

    private void initializeFlagImageMap() {
        flagImageMap.put(Flags.ALPHA, resources.flagAlpha());
        flagImageMap.put(Flags.AP, resources.flagAP());
        flagImageMap.put(Flags.BLACK, resources.flagBlack());
        flagImageMap.put(Flags.BLUE, resources.flagBlue());
        flagImageMap.put(Flags.BRAVO, resources.flagBravo());
        flagImageMap.put(Flags.CLASS, resources.flagClass());
        flagImageMap.put(Flags.ESSONE, resources.flagEssOne());
        flagImageMap.put(Flags.ESSTWO, resources.flagEssTwo());
        flagImageMap.put(Flags.ESSTHREE, resources.flagEssThree());
        flagImageMap.put(Flags.FIRSTSUBSTITUTE, resources.flagFirstSubstitute());
        flagImageMap.put(Flags.GOLF, resources.flagGolf());
        flagImageMap.put(Flags.HOTEL, resources.flagHotel());
        flagImageMap.put(Flags.INDIA, resources.flagIndia());
        flagImageMap.put(Flags.NOVEMBER, resources.flagNovember());
        flagImageMap.put(Flags.PAPA, resources.flagPapa());
        flagImageMap.put(Flags.XRAY, resources.flagXray());
        flagImageMap.put(Flags.ZULU, resources.flagZulu());
    }
    
    public ImageResource resolveFlagToImage(Flags flag) {
        return flagImageMap.get(flag);
    }
    
    public ImageResource resolveFlagDirectionToImage(boolean isDisplayed) {
        ImageResource result;
        
        if (isDisplayed) {
            result = resources.arrowUp();
        } else {
            result = resources.arrowDown();
        }
        
        return result;
    }

}
