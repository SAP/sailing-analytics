package com.sap.sailing.gwt.common.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.resources.client.ImageResource;
import com.sap.sailing.domain.common.BoatClassMasterdata;

/**
 * All images are expected to be 140x140px with transparent background.
 * 
 * @author Steffen Tobias Wagner, Axel Uhl (D043530)
 *
 */
public class BoatClassImageResolver {
    private static Map<String, ImageResource> boatClassIconsMap;
    private static BoatClassImageResources imageResources = BoatClassImageResources.INSTANCE;
    
    static {
        boatClassIconsMap = new HashMap<String, ImageResource>();

        // handicap sailing icons
        boatClassIconsMap.put(BoatClassMasterdata.ORC.getDisplayName(), imageResources.ORCIcon());
        boatClassIconsMap.put(BoatClassMasterdata.ORC_CLUB.getDisplayName(), imageResources.ORCIcon());
        boatClassIconsMap.put(BoatClassMasterdata.ORC_INTERNATIONAL.getDisplayName(), imageResources.ORCIcon());
        boatClassIconsMap.put(BoatClassMasterdata.PHRF.getDisplayName(), imageResources.PHRFIcon());
        boatClassIconsMap.put(BoatClassMasterdata._12M.getDisplayName(), imageResources._12mRIcon());
        boatClassIconsMap.put(BoatClassMasterdata._18Footer.getDisplayName(), imageResources._18FooterIcon());
        boatClassIconsMap.put(BoatClassMasterdata._2_4M.getDisplayName(), imageResources._2_4mIcon());
        boatClassIconsMap.put(BoatClassMasterdata._29ER.getDisplayName(), imageResources._29erIcon());
        boatClassIconsMap.put(BoatClassMasterdata._420.getDisplayName(), imageResources._420Icon());
        boatClassIconsMap.put(BoatClassMasterdata._470.getDisplayName(), imageResources._470Icon());
        boatClassIconsMap.put(BoatClassMasterdata._49ERFX.getDisplayName(), imageResources._49erFXIcon());
        boatClassIconsMap.put(BoatClassMasterdata._49ER.getDisplayName(), imageResources._49erIcon());
        boatClassIconsMap.put(BoatClassMasterdata._5O5.getDisplayName(), imageResources._505mIcon());
        boatClassIconsMap.put(BoatClassMasterdata._5_5MR.getDisplayName(), imageResources._5_5MRIcon());
        boatClassIconsMap.put(BoatClassMasterdata._6MR.getDisplayName(), imageResources._6MRIcon());
        boatClassIconsMap.put(BoatClassMasterdata._8MR.getDisplayName(), imageResources._8MRIcon());
        boatClassIconsMap.put(BoatClassMasterdata.A_CAT.getDisplayName(), imageResources.A_CatIcon());
        boatClassIconsMap.put(BoatClassMasterdata.ALBIN_EXPRESS.getDisplayName(), imageResources.AlbinExpressIcon());
        boatClassIconsMap.put(BoatClassMasterdata.ALBIN_BALLAD.getDisplayName(), imageResources.AlbinBalladIcon());
        boatClassIconsMap.put(BoatClassMasterdata.B_ONE.getDisplayName(), imageResources.BOne());
        boatClassIconsMap.put(BoatClassMasterdata.CADET.getDisplayName(), imageResources.CadetIcon());
        boatClassIconsMap.put(BoatClassMasterdata.CANOE_IC.getDisplayName(), imageResources.CanoeICIcon());
        boatClassIconsMap.put(BoatClassMasterdata.CANOE_TAIFUN.getDisplayName(), imageResources.CanoeTaifunIcon());
        boatClassIconsMap.put(BoatClassMasterdata.CONTENDER.getDisplayName(), imageResources.ContenderIcon());
        boatClassIconsMap.put(BoatClassMasterdata.CC_30.getDisplayName(), imageResources.CC30Icon());
        boatClassIconsMap.put(BoatClassMasterdata.DELPHIA_24.getDisplayName(), imageResources.Delphia24Icon());
        boatClassIconsMap.put(BoatClassMasterdata.DRAGON_INT.getDisplayName(), imageResources.DragonIcon());
        boatClassIconsMap.put(BoatClassMasterdata.DYAS.getDisplayName(), imageResources.DyasIcon());
        boatClassIconsMap.put(BoatClassMasterdata.EUROPE_INT.getDisplayName(), imageResources.EuropeIcon());
        boatClassIconsMap.put(BoatClassMasterdata.EXTREME_40.getDisplayName(), imageResources.Extreme40Icon());
        boatClassIconsMap.put(BoatClassMasterdata.D_ONE.getDisplayName(), imageResources.DOneIcon());
        boatClassIconsMap.put(BoatClassMasterdata.D_35.getDisplayName(), imageResources.D35Icon());
        boatClassIconsMap.put(BoatClassMasterdata.F_16.getDisplayName(), imageResources.F16Icon());
        boatClassIconsMap.put(BoatClassMasterdata.F_18.getDisplayName(), imageResources.F18Icon());
        boatClassIconsMap.put(BoatClassMasterdata.FARR_30.getDisplayName(), imageResources.Farr30Icon());
        boatClassIconsMap.put(BoatClassMasterdata.FARR_280.getDisplayName(), imageResources.Farr280Icon());
        boatClassIconsMap.put(BoatClassMasterdata.FINN.getDisplayName(), imageResources.FinnIcon());
        boatClassIconsMap.put(BoatClassMasterdata.FLYING_DUTCHMAN.getDisplayName(), imageResources.FlyingDutchmanIcon());
        boatClassIconsMap.put(BoatClassMasterdata.FOLKBOAT.getDisplayName(), imageResources.FolkBoatIcon());
        boatClassIconsMap.put(BoatClassMasterdata.FUN.getDisplayName(), imageResources.FUNIcon());
        boatClassIconsMap.put(BoatClassMasterdata.GC_32.getDisplayName(), imageResources.GC32Icon());
        boatClassIconsMap.put(BoatClassMasterdata.GP_26.getDisplayName(), imageResources.GP26Icon());
        boatClassIconsMap.put(BoatClassMasterdata.H_BOAT.getDisplayName(), imageResources.H_BoatIcon());
        boatClassIconsMap.put(BoatClassMasterdata.HANSA_303.getDisplayName(), imageResources.Hansa303Icon());
        boatClassIconsMap.put(BoatClassMasterdata.HOBIE_16.getDisplayName(), imageResources.HobieIcon());
        boatClassIconsMap.put(BoatClassMasterdata.HOBIE_TIGER.getDisplayName(), imageResources.HobieIcon());
        boatClassIconsMap.put(BoatClassMasterdata.HOBIE_WILD_CAT.getDisplayName(), imageResources.HobieIcon());
        boatClassIconsMap.put(BoatClassMasterdata.INTERNATIONAL_14.getDisplayName(), imageResources.International14Icon());
        boatClassIconsMap.put(BoatClassMasterdata.J22.getDisplayName(), imageResources.J22Icon());
        boatClassIconsMap.put(BoatClassMasterdata.J24.getDisplayName(), imageResources.J24Icon());
        boatClassIconsMap.put(BoatClassMasterdata.J70.getDisplayName(), imageResources.J70Icon());
        boatClassIconsMap.put(BoatClassMasterdata.J80.getDisplayName(), imageResources.J80Icon());
        boatClassIconsMap.put(BoatClassMasterdata.J88.getDisplayName(), imageResources.J88Icon());
        boatClassIconsMap.put(BoatClassMasterdata.J105.getDisplayName(), imageResources.J105Icon());
        boatClassIconsMap.put(BoatClassMasterdata.J111.getDisplayName(), imageResources.J111Icon());
        boatClassIconsMap.put(BoatClassMasterdata.KIELZUGVOGEL.getDisplayName(), imageResources.KielzugvogelIcon());
        boatClassIconsMap.put(BoatClassMasterdata.FORMULA_KITE.getDisplayName(), imageResources.FormulaKiteIcon());
        boatClassIconsMap.put(BoatClassMasterdata.LASER_2.getDisplayName(), imageResources.Laser2Icon());
        boatClassIconsMap.put(BoatClassMasterdata.LASER_SB3.getDisplayName(), imageResources.LaserSB3Icon());
        boatClassIconsMap.put(BoatClassMasterdata.LASER_INT.getDisplayName(), imageResources.LaserIcon());
        boatClassIconsMap.put(BoatClassMasterdata.LASER_4_7.getDisplayName(), imageResources.LaserIcon());
        boatClassIconsMap.put(BoatClassMasterdata.LASER_RADIAL.getDisplayName(), imageResources.LaserIcon());
        boatClassIconsMap.put(BoatClassMasterdata.LAGO_26.getDisplayName(), imageResources.Lago26Icon());
        boatClassIconsMap.put(BoatClassMasterdata.LONGTZE.getDisplayName(), imageResources.LongtzeIcon());
        boatClassIconsMap.put(BoatClassMasterdata.M32.getDisplayName(), imageResources.M32Icon());
        boatClassIconsMap.put(BoatClassMasterdata.MELGES_20.getDisplayName(), imageResources.Melges20Icon());
        boatClassIconsMap.put(BoatClassMasterdata.MELGES_24.getDisplayName(), imageResources.Melges24Icon());
        boatClassIconsMap.put(BoatClassMasterdata.MINI_TRANSAT.getDisplayName(), imageResources.MiniTransatIcon());
        boatClassIconsMap.put(BoatClassMasterdata.MUSTO_SKIFF.getDisplayName(), imageResources.MustoSkiffIcon());
        boatClassIconsMap.put(BoatClassMasterdata.NACRA_15.getDisplayName(), imageResources.Nacra15Icon());
        boatClassIconsMap.put(BoatClassMasterdata.NACRA_17.getDisplayName(), imageResources.Nacra17Icon());        
        boatClassIconsMap.put(BoatClassMasterdata.NACRA_17_FOIL.getDisplayName(), imageResources.Nacra17Icon());
        boatClassIconsMap.put(BoatClassMasterdata.O_JOLLE.getDisplayName(), imageResources.OJolleIcon());
        boatClassIconsMap.put(BoatClassMasterdata.OK.getDisplayName(), imageResources.OKIcon());
        boatClassIconsMap.put(BoatClassMasterdata.OPEN_BIC.getDisplayName(), imageResources.OpenBicIcon());
        boatClassIconsMap.put(BoatClassMasterdata.OPTIMIST.getDisplayName(), imageResources.OptimistIcon());
        boatClassIconsMap.put(BoatClassMasterdata.PIRATE.getDisplayName(), imageResources.PiratIcon());
        boatClassIconsMap.put(BoatClassMasterdata.PLATU_25.getDisplayName(), imageResources.Platu25Icon());
        boatClassIconsMap.put(BoatClassMasterdata.PWA.getDisplayName(), imageResources.PWAIcon());
        boatClassIconsMap.put(BoatClassMasterdata.RS100.getDisplayName(), imageResources.RS100Icon());
        boatClassIconsMap.put(BoatClassMasterdata.RS200.getDisplayName(), imageResources.RS200Icon());
        boatClassIconsMap.put(BoatClassMasterdata.RS400.getDisplayName(), imageResources.RS400Icon());
        boatClassIconsMap.put(BoatClassMasterdata.RS500.getDisplayName(), imageResources.RS500Icon());
        boatClassIconsMap.put(BoatClassMasterdata.RS800.getDisplayName(), imageResources.RS800Icon());
        boatClassIconsMap.put(BoatClassMasterdata.RS_AERO.getDisplayName(), imageResources.RSAEROIcon());
        boatClassIconsMap.put(BoatClassMasterdata.RS_X.getDisplayName(), imageResources.RSXIcon());
        boatClassIconsMap.put(BoatClassMasterdata.RS_FEVA.getDisplayName(), imageResources.RSFevaIcon());
        boatClassIconsMap.put(BoatClassMasterdata.SKUD_18.getDisplayName(), imageResources.Skud18Icon());
        boatClassIconsMap.put(BoatClassMasterdata.SONAR.getDisplayName(), imageResources.SonarIcon());
        boatClassIconsMap.put(BoatClassMasterdata.SOLING.getDisplayName(), imageResources.SolingIcon());
        boatClassIconsMap.put(BoatClassMasterdata.SPLASH_BLUE.getDisplayName(), imageResources.SplashBlueIcon());
        boatClassIconsMap.put(BoatClassMasterdata.SPLASH_RED.getDisplayName(), imageResources.SplashRedIcon());
        boatClassIconsMap.put(BoatClassMasterdata.SPLASH_GREEN.getDisplayName(), imageResources.SplashGreenIcon());
        boatClassIconsMap.put(BoatClassMasterdata.STAR.getDisplayName(), imageResources.StarIcon());
        boatClassIconsMap.put(BoatClassMasterdata.STREAMLINE.getDisplayName(), imageResources.StreamlineIcon());
        boatClassIconsMap.put(BoatClassMasterdata.SUNBEAM_22.getDisplayName(), imageResources.Sunbeam22Icon());
        boatClassIconsMap.put(BoatClassMasterdata.SWAN_45.getDisplayName(), imageResources.Swan45Icon());
        boatClassIconsMap.put(BoatClassMasterdata.TARTAN_10.getDisplayName(), imageResources.Tartan10Icon());
        boatClassIconsMap.put(BoatClassMasterdata.TECHNO_293.getDisplayName(), imageResources.Techno293Icon());
        boatClassIconsMap.put(BoatClassMasterdata.TECHNO_293_PLUS.getDisplayName(), imageResources.Techno293PlusIcon());
        boatClassIconsMap.put(BoatClassMasterdata.TEENY.getDisplayName(), imageResources.TeenyIcon());
        boatClassIconsMap.put(BoatClassMasterdata.TORNADO.getDisplayName(), imageResources.TornadoIcon());
        boatClassIconsMap.put(BoatClassMasterdata.TOM_28_MAX.getDisplayName(), imageResources.Tom28MaxIcon());
        boatClassIconsMap.put(BoatClassMasterdata.TP52.getDisplayName(), imageResources.TP52Icon());
        boatClassIconsMap.put(BoatClassMasterdata.TRIAS.getDisplayName(), imageResources.TriasIcon());
        boatClassIconsMap.put(BoatClassMasterdata.VIPER_640.getDisplayName(), imageResources.Viper640Icon());
        boatClassIconsMap.put(BoatClassMasterdata.VX_ONE.getDisplayName(), imageResources.VXOneIcon());
        boatClassIconsMap.put(BoatClassMasterdata.WETA.getDisplayName(), imageResources.WetaIcon());
        boatClassIconsMap.put(BoatClassMasterdata.X_99.getDisplayName(), imageResources.X99Icon());
        boatClassIconsMap.put(BoatClassMasterdata.RUNNING.getDisplayName(), imageResources.RunningIcon());
    }

    public static ImageResource getBoatClassIconResource(String displayName) {
        ImageResource imageResource = boatClassIconsMap.get(displayName);
        if(imageResource == null) {
            imageResource = BoatClassImageResources.INSTANCE.genericBoatClass(); 
        }
        return imageResource;
    }
}
