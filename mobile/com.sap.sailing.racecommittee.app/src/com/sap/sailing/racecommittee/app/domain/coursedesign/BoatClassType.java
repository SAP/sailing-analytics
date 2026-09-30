package com.sap.sailing.racecommittee.app.domain.coursedesign;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.common.Distance;

@SuppressWarnings("serial")
public enum BoatClassType {
    boatClass470erMen("470er Men", new HashMap<CourseLayouts, TargetTime>() {
        {
            put(TrapezoidCourseLayouts.innerLoopTrapezoid60, TargetTime.fifty);
            put(TrapezoidCourseLayouts.outerLoopTrapezoid60, TargetTime.fifty);
            put(WindWardLeeWardCourseLayouts.windWardLeewardLeeward, TargetTime.thirty);
            put(WindWardLeeWardCourseLayouts.windWardLeewardWindward, TargetTime.thirty);
        }
    }, new TreeMap<WindRange, Map<PointOfSail, Float>>() {
        {
            put(new WindRange(5, 8), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                {
                    put(PointOfSail.Upwind, 17.5f);
                    put(PointOfSail.Downwind, 15.5f);
                    put(PointOfSail.Reach, 11f);
                }
            });
            put(new WindRange(8, 12), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                {
                    put(PointOfSail.Upwind, 15.5f);
                    put(PointOfSail.Downwind, 10.5f);
                    put(PointOfSail.Reach, 8f);
                }
            });
            put(new WindRange(12, 15), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                {
                    put(PointOfSail.Upwind, 11.5f);
                    put(PointOfSail.Downwind, 8.5f);
                    put(PointOfSail.Reach, 5.5f);
                }
            });
            put(new WindRange(15, 99), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                {
                    put(PointOfSail.Upwind, 11f);
                    put(PointOfSail.Downwind, 7.5f);
                    put(PointOfSail.Reach, 5.5f);
                }
            });
        }
    }, new MeterDistance(4.7), new MeterDistance(290)), boatClass470eromen("470er Women",
            new HashMap<CourseLayouts, TargetTime>() {
                {
                    put(TrapezoidCourseLayouts.innerLoopTrapezoid60, TargetTime.fifty);
                    put(TrapezoidCourseLayouts.outerLoopTrapezoid60, TargetTime.fifty);
                    put(WindWardLeeWardCourseLayouts.windWardLeewardLeeward, TargetTime.thirty);
                    put(WindWardLeeWardCourseLayouts.windWardLeewardWindward, TargetTime.thirty);
                }
            }, new TreeMap<WindRange, Map<PointOfSail, Float>>() {
                {
                    put(new WindRange(5, 8), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                        {
                            put(PointOfSail.Upwind, 17.5f);
                            put(PointOfSail.Downwind, 15.5f);
                            put(PointOfSail.Reach, 11f);
                        }
                    });
                    put(new WindRange(8, 12), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                        {
                            put(PointOfSail.Upwind, 15.5f);
                            put(PointOfSail.Downwind, 10.5f);
                            put(PointOfSail.Reach, 8f);
                        }
                    });
                    put(new WindRange(12, 15), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                        {
                            put(PointOfSail.Upwind, 11.5f);
                            put(PointOfSail.Downwind, 8.5f);
                            put(PointOfSail.Reach, 5.5f);
                        }
                    });
                    put(new WindRange(15, 99), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                        {
                            put(PointOfSail.Upwind, 11.5f);
                            put(PointOfSail.Downwind, 8.5f);
                            put(PointOfSail.Reach, 5.5f);
                        }
                    });
                }
            }, new MeterDistance(4.7),
            new MeterDistance(110)), boatClassLaserMen("Laser Men", new HashMap<CourseLayouts, TargetTime>() {
                {
                    put(TrapezoidCourseLayouts.innerLoopTrapezoid70, TargetTime.fifty);
                    put(TrapezoidCourseLayouts.outerLoopTrapezoid70, TargetTime.fifty);
                    put(WindWardLeeWardCourseLayouts.windWardLeewardLeeward, TargetTime.thirty);
                    put(WindWardLeeWardCourseLayouts.windWardLeewardWindward, TargetTime.thirty);
                }
            }, new TreeMap<WindRange, Map<PointOfSail, Float>>() {
                {
                    put(new WindRange(5, 8), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                        {
                            put(PointOfSail.Upwind, 19f);
                            put(PointOfSail.Downwind, 16f);
                            put(PointOfSail.Reach, 14.5f);
                        }
                    });
                    put(new WindRange(8, 12), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                        {
                            put(PointOfSail.Upwind, 17f);
                            put(PointOfSail.Downwind, 12f);
                            put(PointOfSail.Reach, 9.5f);
                        }
                    });
                    put(new WindRange(12, 15), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                        {
                            put(PointOfSail.Upwind, 16f);
                            put(PointOfSail.Downwind, 9f);
                            put(PointOfSail.Reach, 6f);
                        }
                    });
                    put(new WindRange(15, 99), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                        {
                            put(PointOfSail.Upwind, 16.5f);
                            put(PointOfSail.Downwind, 7f);
                            put(PointOfSail.Reach, 5.5f);
                        }
                    });
                }
            }, new MeterDistance(4.24), new MeterDistance(280)), boatClassLaserRadial("Laser Radial",
                    new HashMap<CourseLayouts, TargetTime>() {
                        {
                            put(TrapezoidCourseLayouts.innerLoopTrapezoid70, TargetTime.fifty);
                            put(TrapezoidCourseLayouts.outerLoopTrapezoid70, TargetTime.fifty);
                            put(WindWardLeeWardCourseLayouts.windWardLeewardLeeward, TargetTime.thirty);
                            put(WindWardLeeWardCourseLayouts.windWardLeewardWindward, TargetTime.thirty);
                        }
                    }, new TreeMap<WindRange, Map<PointOfSail, Float>>() {
                        {
                            put(new WindRange(5, 8), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                {
                                    put(PointOfSail.Upwind, 21f);
                                    put(PointOfSail.Downwind, 17f);
                                    put(PointOfSail.Reach, 15f);
                                }
                            });
                            put(new WindRange(8, 12), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                {
                                    put(PointOfSail.Upwind, 18f);
                                    put(PointOfSail.Downwind, 12.5f);
                                    put(PointOfSail.Reach, 10f);
                                }
                            });
                            put(new WindRange(12, 15), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                {
                                    put(PointOfSail.Upwind, 16.5f);
                                    put(PointOfSail.Downwind, 9.5f);
                                    put(PointOfSail.Reach, 6f);
                                }
                            });
                            put(new WindRange(15, 99), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                {
                                    put(PointOfSail.Upwind, 17f);
                                    put(PointOfSail.Downwind, 7f);
                                    put(PointOfSail.Reach, 5.5f);
                                }
                            });
                        }
                    }, new MeterDistance(4.24),
                    new MeterDistance(260)), boatClassFinn("Finn", new HashMap<CourseLayouts, TargetTime>() {
                        {
                            put(TrapezoidCourseLayouts.innerLoopTrapezoid70, TargetTime.fifty);
                            put(TrapezoidCourseLayouts.outerLoopTrapezoid70, TargetTime.fifty);
                            put(WindWardLeeWardCourseLayouts.windWardLeewardLeeward, TargetTime.fifty);
                            put(WindWardLeeWardCourseLayouts.windWardLeewardWindward, TargetTime.fifty);
                        }
                    }, new TreeMap<WindRange, Map<PointOfSail, Float>>() {
                        {
                            put(new WindRange(5, 8), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                {
                                    put(PointOfSail.Upwind, 16f);
                                    put(PointOfSail.Downwind, 15f);
                                    put(PointOfSail.Reach, 15f);
                                }
                            });
                            put(new WindRange(8, 12), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                {
                                    put(PointOfSail.Upwind, 15f);
                                    put(PointOfSail.Downwind, 11f);
                                    put(PointOfSail.Reach, 10f);
                                }
                            });
                            put(new WindRange(12, 15), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                {
                                    put(PointOfSail.Upwind, 15f);
                                    put(PointOfSail.Downwind, 9f);
                                    put(PointOfSail.Reach, 7f);
                                }
                            });
                            put(new WindRange(15, 99), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                {
                                    put(PointOfSail.Upwind, 15f);
                                    put(PointOfSail.Downwind, 7f);
                                    put(PointOfSail.Reach, 6f);
                                }
                            });
                        }
                    }, new MeterDistance(4.54), new MeterDistance(255)), boatClass49er("49er",
                            new HashMap<CourseLayouts, TargetTime>() {
                                {
                                    put(WindWardLeeWardCourseLayouts.windWardLeewardLeeward, TargetTime.thirty);
                                    put(WindWardLeeWardCourseLayouts.windWardLeewardWindward, TargetTime.thirty);
                                }
                            }, new TreeMap<WindRange, Map<PointOfSail, Float>>() {
                                {
                                    put(new WindRange(5, 8), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                        {
                                            put(PointOfSail.Upwind, 15f);
                                            put(PointOfSail.Downwind, 11f);
                                            put(PointOfSail.Reach, 8f);
                                        }
                                    });
                                    put(new WindRange(8, 12), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                        {
                                            put(PointOfSail.Upwind, 11f);
                                            put(PointOfSail.Downwind, 5.5f);
                                            put(PointOfSail.Reach, 7f);
                                        }
                                    });
                                    put(new WindRange(12, 15), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                        {
                                            put(PointOfSail.Upwind, 9f);
                                            put(PointOfSail.Downwind, 4.5f);
                                            put(PointOfSail.Reach, 5.5f);
                                        }
                                    });
                                    put(new WindRange(15, 99), new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                        {
                                            put(PointOfSail.Upwind, 7.5f);
                                            put(PointOfSail.Downwind, 4f);
                                            put(PointOfSail.Reach, 5f);
                                        }
                                    });
                                }
                            }, new MeterDistance(4.9), new MeterDistance(255)), boatClass49erFX("49er FX",
                                    new HashMap<CourseLayouts, TargetTime>() {
                                        {
                                            put(WindWardLeeWardCourseLayouts.windWardLeewardLeeward, TargetTime.thirty);
                                            put(WindWardLeeWardCourseLayouts.windWardLeewardWindward,
                                                    TargetTime.thirty);
                                        }
                                    }, new TreeMap<WindRange, Map<PointOfSail, Float>>() {
                                        {
                                            put(new WindRange(5, 8),
                                                    new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                                        {
                                                            put(PointOfSail.Upwind, 15f);
                                                            put(PointOfSail.Downwind, 11f);
                                                            put(PointOfSail.Reach, 8f);
                                                        }
                                                    });
                                            put(new WindRange(8, 12),
                                                    new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                                        {
                                                            put(PointOfSail.Upwind, 13f);
                                                            put(PointOfSail.Downwind, 8f);
                                                            put(PointOfSail.Reach, 7f);
                                                        }
                                                    });
                                            put(new WindRange(12, 15),
                                                    new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                                        {
                                                            put(PointOfSail.Upwind, 11f);
                                                            put(PointOfSail.Downwind, 5.5f);
                                                            put(PointOfSail.Reach, 5.5f);
                                                        }
                                                    });
                                            put(new WindRange(15, 99),
                                                    new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                                        {
                                                            put(PointOfSail.Upwind, 10f);
                                                            put(PointOfSail.Downwind, 5f);
                                                            put(PointOfSail.Reach, 5f);
                                                        }
                                                    });
                                        }
                                    }, new MeterDistance(4.9), new MeterDistance(140)), boatClassNacra17("Nacra 17",
                                            new HashMap<CourseLayouts, TargetTime>() {
                                                {
                                                    put(WindWardLeeWardCourseLayouts.windWardLeewardLeeward,
                                                            TargetTime.fourty);
                                                    put(WindWardLeeWardCourseLayouts.windWardLeewardWindward,
                                                            TargetTime.fourty);
                                                }
                                            }, new TreeMap<WindRange, Map<PointOfSail, Float>>() {
                                                {
                                                    put(new WindRange(5, 8),
                                                            new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                                                {
                                                                    put(PointOfSail.Upwind, 16f);
                                                                    put(PointOfSail.Downwind, 9f);
                                                                    put(PointOfSail.Reach, 7f);
                                                                }
                                                            });
                                                    put(new WindRange(8, 12),
                                                            new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                                                {
                                                                    put(PointOfSail.Upwind, 12f);
                                                                    put(PointOfSail.Downwind, 7f);
                                                                    put(PointOfSail.Reach, 5f);
                                                                }
                                                            });
                                                    put(new WindRange(12, 15),
                                                            new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                                                {
                                                                    put(PointOfSail.Upwind, 9f);
                                                                    put(PointOfSail.Downwind, 5f);
                                                                    put(PointOfSail.Reach, 3f);
                                                                }
                                                            });
                                                    put(new WindRange(15, 99),
                                                            new EnumMap<PointOfSail, Float>(PointOfSail.class) {
                                                                {
                                                                    put(PointOfSail.Upwind, 8f);
                                                                    put(PointOfSail.Downwind, 4.5f);
                                                                    put(PointOfSail.Reach, 3f);
                                                                }
                                                            });
                                                }
                                            }, new MeterDistance(5.25), new MeterDistance(280)), boatClassOther("Other",
                                                    new HashMap<CourseLayouts, TargetTime>() {
                                                        {
                                                            put(WindWardLeeWardCourseLayouts.windWardLeewardLeeward,
                                                                    TargetTime.thirty);
                                                            put(WindWardLeeWardCourseLayouts.windWardLeewardWindward,
                                                                    TargetTime.thirty);
                                                            put(TrapezoidCourseLayouts.innerLoopTrapezoid70,
                                                                    TargetTime.sixty);
                                                            put(TrapezoidCourseLayouts.outerLoopTrapezoid70,
                                                                    TargetTime.sixty);
                                                            put(TrapezoidCourseLayouts.innerLoopTrapezoid60,
                                                                    TargetTime.sixty);
                                                            put(TrapezoidCourseLayouts.outerLoopTrapezoid60,
                                                                    TargetTime.sixty);
                                                        }
                                                    }, new TreeMap<WindRange, Map<PointOfSail, Float>>(),
                                                    new MeterDistance(0.0), new MeterDistance(0));

    private String displayName;
    private Map<CourseLayouts, TargetTime> possipleCourseLayoutsWithStandardTargetTime;
    private Map<WindRange, Map<PointOfSail, Float>> boatSpeedTable;
    private Distance hullLength;
    private Distance startLineLength;

    private BoatClassType(String displayName,
            Map<CourseLayouts, TargetTime> possipleCourseLayoutsWithStandardTargetTime,
            Map<WindRange, Map<PointOfSail, Float>> boatSpeedTable, Distance hullLength,
            Distance startLineLengthInMeters) {
        this.displayName = displayName;
        this.possipleCourseLayoutsWithStandardTargetTime = possipleCourseLayoutsWithStandardTargetTime;
        this.boatSpeedTable = boatSpeedTable;
        this.hullLength = hullLength;
        this.startLineLength = startLineLengthInMeters;
    }

    public Map<WindRange, Map<PointOfSail, Float>> getBoatSpeedTable() {
        return boatSpeedTable;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public Map<CourseLayouts, TargetTime> getPossibleCourseLayoutsWithTargetTime() {
        return possipleCourseLayoutsWithStandardTargetTime;
    }

    public Distance getHullLength() {
        return hullLength;
    }

    public Distance getStartLineLength() {
        return startLineLength;
    }
}
