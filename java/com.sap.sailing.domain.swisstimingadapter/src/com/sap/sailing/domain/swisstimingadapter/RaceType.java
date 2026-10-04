package com.sap.sailing.domain.swisstimingadapter;

import com.sap.sailing.domain.base.BoatClass;


public interface RaceType {
    
    public enum OlympicRaceCode {

//        SAM102000 Men's Windsurfer = Windsufer Männer RS:X
//        SAW102000 Women's Windsurfer = Windsurfer Damen RS:X
//        SAM004000 Men's One Person Dinghy = Laser Männer
//        SAW103000 Women's One Person Dinghy = Laser Damen Laser Radial
//        SAM002000 Men's One Person Dinghy Heavy = Finn Dinghy Männer
//        SAM005000 Men's Two Person Dinghy = 470er Männer
//        SAW005000 Women's Two Person Dinghy = 470er Damen
//        SAM009000 Men's Skiff = 49er Männer
//        SAM007000 Men's Keelboat = Starboot Männer 
//        SAW010000 Women's Match Racing = Matchrace Damen Elliott 6M (modified)
        

        UNKNOWN("Unspecified", "Unspecified", Gender.unspecified),
        FINN("SAM002", "Finn", Gender.male), 
        LASER("SAM004", "Laser", Gender.male), 
        _470_WOMEN("SAW005", "470", Gender.female), 
        _470_MEN("SAM005", "470", Gender.male), 
        STAR("SAM007", "Star", Gender.male), 
        _49ER("SAM009", "49er", Gender.male), 
        _49ERFX("SAW019", "49erFX", Gender.female), 
        ELLIOTT6M("SAW007", "Elliott 6M", Gender.female), 
        RSX_WOMEN("SAW102", "RS:X", Gender.female), 
        RSX_MEN("SAM102", "RS:X", Gender.male), 
        NACRA17("SAX016", "Nacra 17", Gender.mixed),
        LASER_RADIAL("SAW103", "Laser Radial", Gender.female);        
        
        enum Gender {
            male, female, mixed, unspecified;
        }
        
        public final String swissTimingCode;
        
        public final String boatClassName;
        
        public final Gender gender;
        
        public final boolean typicallyStartsUpwind;

        private OlympicRaceCode(String swissTimingCode, String boatClassName, Gender gender) {
            this.swissTimingCode = swissTimingCode;
            this.boatClassName = boatClassName;
            this.gender = gender;
            this.typicallyStartsUpwind = true; 
        }
        
    }

    OlympicRaceCode getRaceCode();

    BoatClass getBoatClass();

}
