package com.sap.sailing.domain.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sse.common.Distance;


public enum BoatClassMasterdata {
    _18Footer ("18Footer", true, 8.90, 2.00, BoatHullType.MONOHULL, true, "18.Footer", "18ft", "18ft Skiff", "18. Footer"),
    _2_4M ("2.4 Meter", true, 4.11, 0.81, BoatHullType.MONOHULL, false, "2.4M", "2.4mR", "2.4 Metre", "2.4-metre", "24MR"),
    _12M ("12 Meter", true, 21.50, 3.60, BoatHullType.MONOHULL, true, "12M", "12mR", "12SQM", "12-metre", "12 metre"),
    _29ER ("29er", true, 4.45, 1.70, BoatHullType.MONOHULL, true, "29-ER"),
    _49ER ("49er", true, 4.88, 1.93, BoatHullType.MONOHULL, true, "49-ER"),
    _49ERFX ("49erFX", true, 4.88, 1.93, BoatHullType.MONOHULL, true, "49FX"),
    _420 ("420", true, 4.20, 1.65, BoatHullType.MONOHULL, true, "420er", "420M", "420W"),
    _470 ("470", true, 4.70, 1.68, BoatHullType.MONOHULL, true, "470er", "470M", "470W", "470 - M", "470 - W", "470 Men", "470 Women", "470 Mixed"),
    _5O5 ("5O5", true, 5.03, 1.88, BoatHullType.MONOHULL, true, "505", "5o5", "505er", "5o5er"),
    _5_5MR ("5.5mR", true, 9.50, 1.92, BoatHullType.MONOHULL, true, "5.5 Meter", "5.5 Metre", "5.5", "5.5M", "5.5-metre", "5.5 metre"),
    _6MR ("6mR", true, 11, 2.2, BoatHullType.MONOHULL, true, "6 Meter", "6 Metre", "6", "6M", "6-metre", "6 metre"),
    _8MR ("8mR", true, 9.62, 2.45, BoatHullType.MONOHULL, true, "8 Meter", "8 Metre", "8M", "8-metre"),
    A_CAT ("A-Catamaran", true, 5.49, 2.30, BoatHullType.CATAMARAN, false, "A-Cat", "ACat", "A-Class Catamaran"),
    ALBIN_EXPRESS ("Albin Express", true, 7.77, 2.50, BoatHullType.MONOHULL, false),
    ALBIN_BALLAD ("Albin Ballad", true, 9.12, 2.95, BoatHullType.MONOHULL, false),
    B_ONE ("B/ONE", true, 7.80, 2.49, BoatHullType.MONOHULL, true, "B-ONE"),
    BAVARIA_CRUISER_41S ("Bavaria Cruiser 41S", true, 12.35, 3.96, BoatHullType.MONOHULL, true, "B41S", "B 41S", "BAVARIACRUISER41S"),
    BAVARIA_CRUISER_45 ("Bavaria Cruiser 45", true, 14.27, 4.35, BoatHullType.MONOHULL, true, "B45", "B 45", "BAVARIACRUISER45"),
    BAVARIA_CRUISER_46 ("Bavaria Cruiser 46", true, 14.27, 4.35, BoatHullType.MONOHULL, true, "B46", "B 46", "BAVARIACRUISER46"),
    BB10M ("BB 10m", true, 10.00, 2.30, BoatHullType.MONOHULL, true, "Dansk BB10M klub"),
    BENETEAU_FIRST_35 ("Benetau First 35", true, 10.66, 3.636, BoatHullType.MONOHULL, true, "First 35"),
    BENETEAU_FIRST_36 ("Benetau First 36", true, 11.98, 3.80, BoatHullType.MONOHULL, true, "First 36"),
    BENETEAU_FIRST_45 ("Benetau First 45", true, 13.68, 4.202, BoatHullType.MONOHULL, true, "First 45"),
    BRASSFAHRT_I ("Brassfahrt I", true, 12.00, 3.50, BoatHullType.MONOHULL, true, "Brassfahrt 1"),
    BRASSFAHRT_II ("Brassfahrt II", true, 12.00, 3.50, BoatHullType.MONOHULL, true, "Brassfahrt 2"),
    BRASSFAHRT_III ("Brassfahrt III", true, 12.00, 3.50, BoatHullType.MONOHULL, true, "Brassfahrt 3"),
    BRASSFAHRT_IV ("Brassfahrt IV", true, 12.00, 3.50, BoatHullType.MONOHULL, true, "Brassfahrt 4"),
    BRASSFAHRT_V ("Brassfahrt V", true, 12.00, 3.50, BoatHullType.MONOHULL, true, "Brassfahrt 5"),
    BRASSFAHRT_VI ("Brassfahrt VI", true, 12.00, 3.50, BoatHullType.MONOHULL, true, "Brassfahrt 6"),
    CADET ("Cadet", true, 3.20, 1.38, BoatHullType.MONOHULL, true),
    CANOE_IC ("International Canoe", true, 5.20, 1.01, BoatHullType.MONOHULL, true, "Canoe IC", "Canoe-IC", "IC", "Kanu IC", "International Canoe", "International 10 Sq.m. Sailing Canoe"),
    CANOE_TAIFUN ("Canoe Taifun", true, 5.20, 1.32, BoatHullType.MONOHULL, false, "Taifun", "Taifun Kanu", "Kanu Taifun"),
    CONTENDER ("Contender", true, 4.88, 1.42, BoatHullType.MONOHULL, false),
    CC_30 ("C&C 30", true, 9.12, 3.25, BoatHullType.MONOHULL, true),
    CLUB_SWAN_50 ("Club Swan 50", true, 16.74, 4.20, BoatHullType.MONOHULL, true, "ClubSwan50"),
    D_ONE ("D-One", true, 4.23, 2.31, BoatHullType.MONOHULL, true, "Devoti D-One", "DOne", "D_One"),
    DRAGON_INT ("Dragon Int.", true, 8.89, 1.96, BoatHullType.MONOHULL, true, "Drachen", "Dragon"),
    DELPHIA_24 ("Delphia 24", true, 7.70, 2.50, BoatHullType.MONOHULL, true, "Delphia 24 One Design", "Delphia 24 OD"),
    DYAS("Dyas", true, 7.15, 1.95, BoatHullType.MONOHULL, true),
    ELAN350("Elan 350", true, 10.6, 3.5, BoatHullType.MONOHULL, true, "Elan 350 Performance"),
    ELAN_E4("Elan E4", true, 10.6, 3.5, BoatHullType.MONOHULL, true, "Elan E4"),
    EXTREME_40 ("Extreme 40", false, 12.2, 6.60, BoatHullType.CATAMARAN, true, "Extreme-40", "Extreme40", "ESS40", "ess"),
    D_35 ("D35", false, 10.81, 6.89, BoatHullType.CATAMARAN, false),
    ELLIOTT_6M ("Elliott 6m", true, 6.0, 2.35, BoatHullType.MONOHULL, true, "Elliott6m"),
    EUROPE_INT ("Europe Int.", true, 3.35, 1.35, BoatHullType.MONOHULL, false, "Europe"),
    F_18 ("Formula 18", true, 6.85, 2.25, BoatHullType.CATAMARAN, true, "F18", "F-18"),
    FARR_30 ("Farr 30", true, 9.42, 3.08, BoatHullType.MONOHULL, true, "F30", "F-30", "Farr-30"),
    FARR_280 ("Farr 280", true, 8.72, 2.87, BoatHullType.MONOHULL, true, "F280", "F-280", "Farr-280"),
    FINN ("Finn", true, 4.50, 1.51, BoatHullType.MONOHULL, false),
    FIRST_CLASS_7_5 ("First Class 7.5", true, 7.86, 2.54, BoatHullType.MONOHULL, true, "Beneteau First Class 7.5"),
    FLYING_DUTCHMAN ("Flying Dutchman", true, 6.10, 1.80, BoatHullType.MONOHULL, true, "FD"),
    FLYING_JUNIOR ("Flying Junior", true, 4.03, 1.50, BoatHullType.MONOHULL, true, "FJ"),
    FLYING_PHANTOM ("Flying Phantom", false, 5.52, 3.00, BoatHullType.CATAMARAN, true),
    FOLKBOAT ("Folkboat", true, 7.68, 2.20, BoatHullType.MONOHULL, false, "Folke", "Folkeboot"),
    FUN ("FUN", true, 7.20, 2.45, BoatHullType.MONOHULL, true, "FUN O.D.", "FUN OD", "Open FUN"),
    F_16 ("Formula 16", true, 5.00, 2.50, BoatHullType.CATAMARAN, true, "F16", "F-16"),
    GC_32 ("GC 32", false, 10.0, 6.00, BoatHullType.CATAMARAN, true, "GC32", "GC-32"),
    GP_26 ("GP 26", true, 7.90, 2.55, BoatHullType.MONOHULL, true, "GP26", "GP-26"),
    HANSE_418 ("Hanse 418", true, 12.4, 4.17, BoatHullType.MONOHULL, true, "Hanse-418"),
    HOBIE_16 ("Hobie 16", true, 5.05, 2.41, BoatHullType.CATAMARAN, false, "H16"),
    H_BOAT ("H-Boat", true, 8.28, 2.18, BoatHullType.MONOHULL, true, "HB", "H-Boot"),
    HANSA_303 ("Hansa 303", true, 3.03, 1.35, BoatHullType.MONOHULL, false, "Hansa-303", "Hansa303", "303"),
    HOBIE_TIGER ("Hobie Tiger", true, 5.51, 2.60, BoatHullType.CATAMARAN, true),
    HOBIE_WILD_CAT ("Hobie Wild Cat", true, 5.49, 2.59, BoatHullType.CATAMARAN, true, "Hobie Wild Cat F18"),
    IMOCA("IMOCA", true, 20.12, 5.5, BoatHullType.MONOHULL, true, "IMOCA 60"),
    INTERNATIONAL_14("International 14", true, 4.27, 1.83, BoatHullType.MONOHULL, true, "I14", "Int.14", "Int14"),
    IQFOIL_MEN("iQFOil Men", true, 2.20, 0.95, BoatHullType.SURFERBOARD, false, "iQFOil 95 Men", "iQF95", "IQF Men", "IQFOIL"),
    IQFOIL_WOMEN("iQFOil Women", true, 2.20, 0.95, BoatHullType.SURFERBOARD, false, "iQFOil 95 Women", "IQF Women"),
    IQFOIL_YOUTH("iQFOil Youth", true, 2.15, 0.85, BoatHullType.SURFERBOARD, false, "iQFOil 85 Youth", "IQF Youth"),
    IRC("IRC", true, 15.00, 4.00, BoatHullType.MONOHULL, false),
    J22 ("J/22", true, 6.86, 2.44, BoatHullType.MONOHULL, true, "J22", "J-22"),
    J24 ("J/24", true, 7.32, 2.67, BoatHullType.MONOHULL, true, "J24", "J-24"),
    J70 ("J/70", true, 6.93, 2.25, BoatHullType.MONOHULL, true, "J70", "J-70"),
    J80 ("J/80", true, 8.0, 2.51, BoatHullType.MONOHULL, true, "J80", "J-80"),
    J88 ("J/88", true, 8.9, 1.89, BoatHullType.MONOHULL, true, "J88", "J-88"),
    J92 ("J/92", true, 9.14, 3.05, BoatHullType.MONOHULL, true, "J92", "J-92"),
    J92S ("J/92S", true, 9.14, 3.05, BoatHullType.MONOHULL, true, "J92S", "J-92S"),
    J105 ("J/105", true, 10.52, 3.35, BoatHullType.MONOHULL, true, "J105", "J-105"),
    J111 ("J/111", true, 11.1, 3.29, BoatHullType.MONOHULL, true, "J111", "J-111"),
    JK_20 ("20qm Jollenkreuzer", true, 7.75, 2.50, BoatHullType.MONOHULL, true, "JK 20", "JK20", "20er"),
    KIELZUGVOGEL ("Kielzugvogel", true, 5.80, 1.88, BoatHullType.MONOHULL, false, "KZV"), 
    FORMULA_KITE ("Kite", true, 1.85, 0.68, BoatHullType.MONOHULL, false, "Formula Kite", "FK"),
    LASER_2 ("Laser 2", true, 4.39, 1.42, BoatHullType.MONOHULL, false, "Laser II", "Laser2", "Laser-2", "Laser-II"),
    LASER_4_7 ("Laser 4.7", true, 4.20, 1.39, BoatHullType.MONOHULL, false, "L4.7", "ILCA 4", "ILCA4"),
    LASER_RADIAL ("Laser Radial", true, 4.19, 1.41, BoatHullType.MONOHULL, false, "LAR", "Laser RAD", "RAD", "Radial", "ILCA 6", "ILCA6"),
    LASER_INT ("Laser Int.", true, 4.19, 1.39, BoatHullType.MONOHULL, false, "Laser", "LSR", "Laser Standard", "ILCA 7", "ILCA7"),
    LASER_SB3 ("Laser SB3", true, 6.15, 2.15, BoatHullType.MONOHULL, false, "LSB3", "SB20"),
    LAGO_26 ("Lago 26", true, 7.95, 2.50, BoatHullType.MONOHULL, true, "Lago26"),
    LONGTZE ("Longtze", true, 6.85, 2.57, BoatHullType.MONOHULL, true, "Swiss Longtze Class"),
    M32 ("M32", false, 9.70, 5.50, BoatHullType.CATAMARAN, true, "M/32", "M32 Catamaran", "M/32 Catamaran"),
    MELGES_20 ("Melges 20", true, 6.10, 2.13, BoatHullType.MONOHULL, true, "Melges-20", "M20"),
    MELGES_24 ("Melges 24", true, 7.32, 2.50, BoatHullType.MONOHULL, true, "Melges-24", "M24"),
    MOCRA ("MOCRA", true, 12, 8, BoatHullType.CATAMARAN, true, "MOCRA Cat", "MOCRA Catamaran", "MOCRA Katamaran", "MOCRA Kat"),
    MINI_TRANSAT ("Mini Transat 6.50", true, 6.50, 3.00, BoatHullType.MONOHULL, true, "Mini Transat"),
    MUSTO_SKIFF ("Musto Skiff", true, 4.55, 1.35, BoatHullType.MONOHULL, true, "Musto Performance Skiff", "MPS", "Musto"),
    NACRA_15 ("Nacra 15", true, 4.70, 2.35, BoatHullType.CATAMARAN, true, "N15", "Nacra-15"),
    NACRA_17 ("Nacra 17", true, 5.25, 2.59, BoatHullType.CATAMARAN, true, "N17", "Nacra-17"),
    NACRA_17_FOIL ("Nacra 17 Foiling", true, 5.25, 2.59, BoatHullType.CATAMARAN, true, "N17F", "Nacra-17-Foiling"),
    O_JOLLE ("O-Jolle", true, 5.00, 1.66, BoatHullType.MONOHULL, false, "O Jolle", "OJolle", "Olympiajolle"),
    OK ("OK Dinghy", true, 5.25, 2.59, BoatHullType.MONOHULL, false, "OK-Dinghy", "OK-Jolle", "OK"),
    OPEN_BIC("O'pen BIC", true, 2.75, 1.14, BoatHullType.MONOHULL, false, "OpenBIC"),
    OPTIMIST ("Optimist", true, 2.34, 1.07, BoatHullType.MONOHULL, false, "Opti", "Optimist Dinghy"),
    PIRATE ("Pirate", true, 5.00, 1.61, BoatHullType.MONOHULL, false, "Pirat", "Piraten"),
    PLATU_25 ("Platu 25", true, 7.53, 2.62, BoatHullType.MONOHULL, true, "Platu", "Platu-25", "PLA", "B25", "Platu25"),
    PWA ("PWA", true, 2.4, 0.6, BoatHullType.MONOHULL, true, "Professional Windsurfers Association", "PWA World Tour"),
    RC44 ("RC44", true, 13.35, 2.75, BoatHullType.MONOHULL, true),
    RS100 ("RS 100", true, 4.30, 1.83, BoatHullType.MONOHULL, true, "RS100", "RS_100"),
    RS200 ("RS 200", true, 4.00, 1.83, BoatHullType.MONOHULL, true, "RS200", "RS_200"),
    RS21 ("RS 21", true, 6.67, 2.20, BoatHullType.MONOHULL, true, "RS21", "RS_21"),
    RS400 ("RS 400", true, 4.52, 1.83, BoatHullType.MONOHULL, true, "RS400", "RS_400"),
    RS500 ("RS 500", true, 4.34, 1.58, BoatHullType.MONOHULL, true, "RS500", "RS_500"),
    RS800 ("RS 800", true, 4.80, 1.88, BoatHullType.MONOHULL, true, "RS800", "RS_800"),
    RS_AERO ("RS Aero", true, 4.00, 1.40, BoatHullType.MONOHULL, false, "RSAERO", "RS_Aero"),
    RS_X ("RS:X", true, 2.86, 0.93, BoatHullType.SURFERBOARD, false, "RS-X", "RSX", "RS:X", "RS:X Men", "RS:X Woman", "RS:X Women"),
    RS_FEVA ("RS Feva", true, 3.64, 1.42, BoatHullType.MONOHULL, true, "RSFeva"),
    RS_TERA ("RS Tera", true, 2.87, 1.23, BoatHullType.MONOHULL, false, "RSTera"),
    RS_VAREO ("RS Vareo", true, 4.25, 1.57, BoatHullType.MONOHULL, true, "RS_VAREO", "RSVareo", "RS Vareo"),
    RS_VENTURE ("RS Venture", true, 4.9, 2.0, BoatHullType.MONOHULL, true, "RSVenture", "RS Venture Connect"),
    SALONA_46 ("Salona 46", true, 14.14, 4.2, BoatHullType.MONOHULL, true, "Salona-46"),
    SCAN_KAP_99 ("Scan-kap 99", true, 9.86, 2.61, BoatHullType.MONOHULL, true, "Scan Kap 99"),
    SK_22 ("SK 22", true, 12, 2, BoatHullType.MONOHULL, true, "22er Schärenkreuzer", "22", "SK22", "22 sq.m."),
    SKUD_18 ("SKUD 18", true, 5.8, 2.29, BoatHullType.MONOHULL, true),
    SONAR ("Sonar", true, 7.01, 2.39, BoatHullType.MONOHULL, true),
    SOLING ("Soling", true, 8.15, 1.91, BoatHullType.MONOHULL, true),
    SPAEKHUGGER ("Spaekhugger", true, 7.44, 2.33, BoatHullType.MONOHULL, false, "Spækhugger", "Spækhuggeren"),
    SPLASH_BLUE ("Splash Blue", true, 3.50, 1.50, BoatHullType.MONOHULL, false, "Splash_Blue"),
    SPLASH_RED ("Splash Red", true, 3.50, 1.50, BoatHullType.MONOHULL, false, "Splash_Red"),
    SPLASH_GREEN ("Splash Green", true, 3.50, 1.50, BoatHullType.MONOHULL, false, "Splash_Green"),
    SRS ("Svenskt Respitsystem", true, 15.00, 4.00, BoatHullType.MONOHULL, false, "SRS", "LYS", "Leading Yard Stick", "Lidingö Yard Stick"),
    STAR ("Star", true, 6.92, 1.74, BoatHullType.MONOHULL, false, "STR", "STARBOOT", "STARBOAT"),
    STREAMLINE ("Streamline", true, 7.15, 2.55, BoatHullType.MONOHULL, true),
    SUNBEAM_22 ("Sunbeam 22", true, 6.70, 2.15, BoatHullType.MONOHULL, true, "Sunbeam 22.1"),
    SWAN_45 ("Swan 45", true, 13.83, 3.91, BoatHullType.MONOHULL, true, "Swan", "Swan-45"),
    TARTAN_10 ("Tartan 10", true, 10.10, 2.82, BoatHullType.MONOHULL, true),
    TECHNO_293 ("Techno 293", true, 2.93, 0.79, BoatHullType.SURFERBOARD, false, "Techno-293", "Bic Techno 293", "Bic Techno-293", "Bic-Techno-293"),
    TECHNO_293_PLUS ("Techno 293 Plus", true, 2.93, 0.79, BoatHullType.SURFERBOARD, false, "Techno-293-Plus", "Techno 293+", "Bic Techno-293-Plus", "Bic Techno 293+", "Bic Techno 293 Plus"),
    TEENY ("Teeny", true, 3.15, 1.37, BoatHullType.MONOHULL, true),
    TEMPEST ("Tempest", true, 6.66, 1.92, BoatHullType.MONOHULL, true),
    TORNADO ("Tornado Catamaran", true, 6.10, 3.02, BoatHullType.CATAMARAN, true, "Tornado", "Tornado Cat"),
    TOM_28_MAX ("Tom 28 MAX", true, 8.48, 2.48, BoatHullType.MONOHULL, true, "Tom 28"),
    TRIAS ("Trias", true, 9.20, 2.12, BoatHullType.MONOHULL, true),
    TP52 ("TP52", true, 15.85, 4.35, BoatHullType.MONOHULL, true, "TP 52", "Transpac 52", "Transpac52"),
    VARIANTA ("Varianta", true, 6.40, 2.10, BoatHullType.MONOHULL, true),
    VAURIEN ("Vaurien", true, 4.08, 1.47, BoatHullType.MONOHULL, true),
    VENT_D_OUEST ("Vent d'Ouest", true, 5.85, 1.75, BoatHullType.MONOHULL, true, "VENTDOUEST", "VENTD'OUEST"),
    VIPER_640 ("Viper 640", true, 6.43, 2.49, BoatHullType.MONOHULL, true),
    VO60 ("VO60", true, 19.5, 5.25, BoatHullType.MONOHULL, true, "Volvo Ocean 60", "VolvoOcean60", "VO 60", "W60", "Whitbread60", "Whitbread 60"),
    VO65 ("VO65", true, 22.14, 5.60, BoatHullType.MONOHULL, true, "Volvo Ocean 65", "VolvoOcean65", "VO 65"),
    VX_ONE ("VX ONE", true, 5.79, 2.19, BoatHullType.MONOHULL, true, "VX-ONE"),
    WASZP ("WASZP", true, 3.35, 2.25, BoatHullType.MONOHULL, false, "WASZPs"),
    WAYFARER ("Wayfarer", true, 4.82, 1.85, BoatHullType.MONOHULL, false),
    WETA ("Weta", true, 4.4, 3.5, BoatHullType.TRIMARAN, true, "Weta Trimaran"),
    WINGFOIL ("Wingfoil", true, 1.95, 0.8, BoatHullType.SURFERBOARD, false, "Wing Foil", "X-15"),
    X_332 ("X-332", true, 10.06, 3.30, BoatHullType.MONOHULL, true, "X332"),
    X_99 ("X-99", true, 9.96, 2.95, BoatHullType.MONOHULL, true, "X99"),
    ZOOM8 ("Zoom8", true, 2.65, 1.45, BoatHullType.MONOHULL, false, "Zoom 8", "Zoom_8", "Zoom-8"),
    
    // multi-class "boat classes"; to be replaced at some later point in time by something like a RegattaClass
    ORC ("ORC", true, 13.83, 3.91, BoatHullType.MONOHULL, true, "ORC I", "ORC II", "ORC III", "ORC IV", "ORC III+IV", "ORC A", "ORC B", "ORC C", "ORC D"),
    ORC_CLUB ("ORC Club", true, 13.83, 3.91, BoatHullType.MONOHULL, true),
    ORC_INTERNATIONAL ("ORC International", true, 13.83, 3.91, BoatHullType.MONOHULL, true, "ORC Int."),
    ORC_MULTIHULL ("ORC Multihull", true, 13.83, 3.91, BoatHullType.CATAMARAN, true, "ORC MH"),
    
    // Performance Handicap Racing Fleet (PHRF) is a handicap system used for racing in North America
    PHRF ("PHRF", true, 13.83, 3.91, BoatHullType.MONOHULL, true),

    // a 'boat class' to track runners at running events 
    RUNNING ("Runner", true, 1.0, 1.0, BoatHullType.NO_HULL, false, "Running");

    private final String displayName;
    private final String[] alternativeNames;
    private final double hullLengthInMeters;
    private final double hullBeamInMeters;
    private final BoatHullType hullType;
    private final boolean typicallyStartsUpwind;
    private final boolean hasAdditionalDownwindSail;

    private static Map<String, BoatClassMasterdata> fromUnifiedDisplayAndAlternativeNamesToBoatClassMasterdata; 

    private BoatClassMasterdata(String displayName, boolean typicallyStartsUpwind, double hullLengthInMeters,
            double hullBeamInMeters, BoatHullType hullType, boolean hasAdditionalDownwindSail, String... alternativeNames) {
        this.displayName = displayName;
        this.typicallyStartsUpwind = typicallyStartsUpwind;
        this.hullLengthInMeters = hullLengthInMeters;
        this.hullBeamInMeters = hullBeamInMeters;
        this.hullType = hullType;
        this.hasAdditionalDownwindSail = hasAdditionalDownwindSail;
        this.alternativeNames = alternativeNames;
        addToCache(this);
    }

    private void addToCache(BoatClassMasterdata boatClassMasterdata) {
        if (fromUnifiedDisplayAndAlternativeNamesToBoatClassMasterdata == null) {
            fromUnifiedDisplayAndAlternativeNamesToBoatClassMasterdata = new HashMap<>();
        }
        fromUnifiedDisplayAndAlternativeNamesToBoatClassMasterdata.put(unifyBoatClassName(getDisplayName()), this);
        for (final String alternativeName : getAlternativeNames()) {
            fromUnifiedDisplayAndAlternativeNamesToBoatClassMasterdata.put(unifyBoatClassName(alternativeName), this);
        }
    }

    private BoatClassMasterdata(String displayName, boolean typicallyStartsUpwind, double hullLengthInMeter,
            double hullBeamInMeter, BoatHullType hullType, boolean hasAdditionalDownwindSail) {
        this.displayName = displayName;
        this.typicallyStartsUpwind = typicallyStartsUpwind;
        this.hullLengthInMeters = hullLengthInMeter;
        this.hullBeamInMeters = hullBeamInMeter;
        this.hullType = hullType;
        this.hasAdditionalDownwindSail = hasAdditionalDownwindSail;
        this.alternativeNames = null;
        addToCache(this);
    }

    public static BoatClassMasterdata resolveBoatClass(String boatClassName) {
        return fromUnifiedDisplayAndAlternativeNamesToBoatClassMasterdata.get(unifyBoatClassName(boatClassName));
    }

    /**
     * Maps the <code>boatClassName</code> string by removing all whitespace and converting to all upper case.
     * Example: "Laser Int." becomes "LASERINT."<p>
     * 
     * Note that the mapping is not related to the set of {@link BoatClassMasterdata} objects known and works the same
     * regardless of whether <code>boatClassName</code> matches any of the existing {@link BoatClassMasterdata} literals,
     * display names or alternative names.
     */
    public static String unifyBoatClassName(String boatClassName) {
        return boatClassName == null ? null : boatClassName.toUpperCase().replaceAll("\\s+","");
    }
    
    /**
     * If any of the existing {@link BoatClassMasterdata} objects has a matching {@link #unifyBoatClassName(String)
     * unified} display or alternative name, the unified display name of that object is returned. Otherwise, the
     * {@link #unifyBoatClassName(String) unified} <code>boatClassName</code> value is returned. Example: "LASER" and
     * "Laser" and "LSR" and "lsr" and "Laser Int." and "LASER INT ." and "LASERINT." will all be mapped to "LASERINT."
     * based on the boat class masterdata object whose display name is "Laser Int.". In turn, "xyz" and "x y z" will be
     * mapped to "XYZ" without any matching {@link BoatClassMasterdata} object existing, simply based on the string
     * mapping described for {@link #unifyBoatClassName(String)}.
     */
    public static String unifyBoatClassNameBasedOnExistingMasterdata(String boatClassName) {
        BoatClassMasterdata bcm = resolveBoatClass(boatClassName);
        final String result;
        if (bcm != null) {
            result = unifyBoatClassName(bcm.getDisplayName());
        } else {
            result = unifyBoatClassName(boatClassName);
        }
        return result;
    }
    
    public Distance getHullLength() {
        return new MeterDistance(hullLengthInMeters);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getAlternativeNames() {
        return alternativeNames == null ? new String[0] : alternativeNames;
    }

    public Distance getHullBeam() {
        return new MeterDistance(hullBeamInMeters);
    }

    public BoatHullType getHullType() {
        return hullType;
    }

    public boolean isTypicallyStartsUpwind() {
        return typicallyStartsUpwind;
    }
    
    public boolean hasAdditionalDownwindSail() {
        return hasAdditionalDownwindSail;
    }
    
    /**
     * Gets the list of names for this {@link BoatClassMasterdata} instance, containing its 
     * {@link #displayName display name} and the {@link #alternativeNames alternative names} (if any).
     * 
     * @return the list of names for the boat class
     */
    public Iterable<String> getBoatClassNames() {
        List<String> result = new ArrayList<>(getAlternativeNames().length + 1);
        result.add(this.getDisplayName());
        for (String alternativeName : this.getAlternativeNames()) {
            result.add(alternativeName);
        }
        return result;
    }

    public static Iterable<String> getAllBoatClassNames(boolean includeAlternativeNames) {
        final List<String> result = new ArrayList<>();
        for (BoatClassMasterdata bcmd : values()) {
            result.add(bcmd.getDisplayName());
            if (includeAlternativeNames) {
                for (String alternativeName : bcmd.getAlternativeNames()) {
                    result.add(alternativeName);
                }
            }
        }
        return result;
    }
}
