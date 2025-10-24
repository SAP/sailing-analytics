package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.gwt.ui.shared.racemap.BoatClassVectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap.CircleVectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap.DinghyWithSpinnakerVectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap.Extreme40VectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap.GC32VectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap.KeelBoatWithGennakerVectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap.LaserVectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap.SmallMultihullVectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap._49erVectorGraphics;

/**
 * A resolver utility for finding a suitable vector graphics for a given boat class
 * @author Frank Mittag (C5163874)
 */
public class BoatClassVectorGraphicsResolver {
    private static Map<BoatClassMasterdata, BoatClassVectorGraphics> compatibleBoatVectorGraphicsMap;
    private static BoatClassVectorGraphics defaultBoatVectorGraphics;
	
    static {
    	compatibleBoatVectorGraphicsMap = new HashMap<>();
    	
        BoatClassVectorGraphics laser = new LaserVectorGraphics(BoatClassMasterdata.LASER_INT,
                BoatClassMasterdata.LASER_RADIAL, BoatClassMasterdata.LASER_4_7, BoatClassMasterdata.LASER_2,
                BoatClassMasterdata.CONTENDER, BoatClassMasterdata.HANSA_303,
                BoatClassMasterdata.FINN, BoatClassMasterdata.MUSTO_SKIFF, BoatClassMasterdata.OPEN_BIC,
                BoatClassMasterdata.OPTIMIST, BoatClassMasterdata.PWA,
                BoatClassMasterdata.OK,
                BoatClassMasterdata.RS_AERO, BoatClassMasterdata.RS_X,
                BoatClassMasterdata.IQFOIL_MEN, BoatClassMasterdata.IQFOIL_WOMEN, BoatClassMasterdata.IQFOIL_YOUTH, 
                BoatClassMasterdata.SPLASH_BLUE, BoatClassMasterdata.SPLASH_RED, BoatClassMasterdata.SPLASH_GREEN,
                BoatClassMasterdata.STAR, BoatClassMasterdata.TECHNO_293, BoatClassMasterdata.TECHNO_293_PLUS,
                BoatClassMasterdata.WINGFOIL, BoatClassMasterdata.ZOOM8);
        BoatClassVectorGraphics _49er = new _49erVectorGraphics(BoatClassMasterdata._49ER, BoatClassMasterdata._49ERFX,
                BoatClassMasterdata._29ER, BoatClassMasterdata._18Footer, BoatClassMasterdata.WASZP);
        BoatClassVectorGraphics extreme40 = new Extreme40VectorGraphics(BoatClassMasterdata.EXTREME_40,
                BoatClassMasterdata.D_35, BoatClassMasterdata.SKUD_18, BoatClassMasterdata.MOCRA, BoatClassMasterdata.WETA);
        BoatClassVectorGraphics gc32 = new GC32VectorGraphics(BoatClassMasterdata.GC_32, BoatClassMasterdata.M32);
        BoatClassVectorGraphics smallMultihull = new SmallMultihullVectorGraphics(BoatClassMasterdata.NACRA_15, BoatClassMasterdata.NACRA_17,
                BoatClassMasterdata.NACRA_17_FOIL, BoatClassMasterdata.F_16, BoatClassMasterdata.F_18,
                BoatClassMasterdata.HOBIE_WILD_CAT, BoatClassMasterdata.HOBIE_16, BoatClassMasterdata.HOBIE_TIGER,
                BoatClassMasterdata.A_CAT, BoatClassMasterdata.TORNADO, BoatClassMasterdata.FLYING_PHANTOM, BoatClassMasterdata.ORC_MULTIHULL);
        BoatClassVectorGraphics keelBoatWithGennaker = new KeelBoatWithGennakerVectorGraphics(BoatClassMasterdata.ELAN350,
                BoatClassMasterdata.ELAN_E4, BoatClassMasterdata.FIRST_CLASS_7_5,
                BoatClassMasterdata.J70, BoatClassMasterdata.J80, BoatClassMasterdata.J92, BoatClassMasterdata.J92S,
                BoatClassMasterdata.B_ONE, BoatClassMasterdata.IRC, BoatClassMasterdata.LASER_SB3, BoatClassMasterdata.LONGTZE,
                BoatClassMasterdata.RS_FEVA, BoatClassMasterdata.RS_TERA, BoatClassMasterdata.RS_VENTURE,
                BoatClassMasterdata.RS_VAREO,
                BoatClassMasterdata.RS100, BoatClassMasterdata.RS21, BoatClassMasterdata.TP52,
                BoatClassMasterdata.CLUB_SWAN_50, BoatClassMasterdata.BAVARIA_CRUISER_41S, BoatClassMasterdata.BAVARIA_CRUISER_45,
                BoatClassMasterdata.BAVARIA_CRUISER_46, BoatClassMasterdata.BENETEAU_FIRST_35, BoatClassMasterdata.BENETEAU_FIRST_36,
                BoatClassMasterdata.BENETEAU_FIRST_45, BoatClassMasterdata.SPAEKHUGGER, BoatClassMasterdata.SCAN_KAP_99,
                BoatClassMasterdata.BB10M, BoatClassMasterdata.WAYFARER, BoatClassMasterdata.X_332,
                BoatClassMasterdata.BRASSFAHRT_I, BoatClassMasterdata.BRASSFAHRT_II, BoatClassMasterdata.BRASSFAHRT_III,
                BoatClassMasterdata.BRASSFAHRT_IV, BoatClassMasterdata.BRASSFAHRT_V,
                BoatClassMasterdata.VO60, BoatClassMasterdata.VO65, BoatClassMasterdata.IMOCA,
                BoatClassMasterdata.HANSE_418, BoatClassMasterdata.SALONA_46, BoatClassMasterdata.SRS);
        BoatClassVectorGraphics dinghyWithSpinnaker = new DinghyWithSpinnakerVectorGraphics(BoatClassMasterdata._420,
                BoatClassMasterdata._470, BoatClassMasterdata._5O5, BoatClassMasterdata.CADET, BoatClassMasterdata.FLYING_DUTCHMAN,
                BoatClassMasterdata.FOLKBOAT, BoatClassMasterdata.DYAS, BoatClassMasterdata.DRAGON_INT,
                BoatClassMasterdata.ELLIOTT_6M, BoatClassMasterdata.H_BOAT, BoatClassMasterdata.ALBIN_EXPRESS,
                BoatClassMasterdata.FARR_30, BoatClassMasterdata.JK_20, BoatClassMasterdata.J24, BoatClassMasterdata.PLATU_25,
                BoatClassMasterdata.TOM_28_MAX, BoatClassMasterdata.DELPHIA_24, BoatClassMasterdata.RS200,
                BoatClassMasterdata.RS400, BoatClassMasterdata.RS500, BoatClassMasterdata.RS800,
                BoatClassMasterdata.SK_22, BoatClassMasterdata.STREAMLINE, BoatClassMasterdata.SWAN_45,
                BoatClassMasterdata.TEENY, BoatClassMasterdata.TEMPEST, BoatClassMasterdata.X_99, BoatClassMasterdata.TRIAS,
                BoatClassMasterdata.VENT_D_OUEST, BoatClassMasterdata.FLYING_JUNIOR, BoatClassMasterdata.VAURIEN,
                BoatClassMasterdata.VARIANTA);
        BoatClassVectorGraphics circle = new CircleVectorGraphics(BoatClassMasterdata.RUNNING);

        defaultBoatVectorGraphics = dinghyWithSpinnaker; // TODO see bug 2571; this should be a slup-rigged icon working for 470, 505, J/70 etc.
        for (BoatClassVectorGraphics g : new BoatClassVectorGraphics[] { laser, _49er, extreme40, smallMultihull, keelBoatWithGennaker, dinghyWithSpinnaker, gc32, circle}) {
            for (BoatClassMasterdata b : g.getCompatibleBoatClasses()) {
                compatibleBoatVectorGraphicsMap.put(b, g);
            }
        }
    }
	
    public static BoatClassVectorGraphics resolveBoatClassVectorGraphics(String boatClassName) {
        BoatClassMasterdata resolvedBoatClass = BoatClassMasterdata.resolveBoatClass(boatClassName);
        final BoatClassVectorGraphics result;
        if (resolvedBoatClass != null && compatibleBoatVectorGraphicsMap.containsKey(resolvedBoatClass)) {
            result = compatibleBoatVectorGraphicsMap.get(resolvedBoatClass);
        } else {
            result = defaultBoatVectorGraphics;
        }
        return result;
    }
}
