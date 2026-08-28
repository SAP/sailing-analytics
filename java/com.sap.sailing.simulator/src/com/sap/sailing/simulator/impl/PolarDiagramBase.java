package com.sap.sailing.simulator.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.simulator.BoatDirection;
import com.sap.sailing.simulator.PointOfSail;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.RadianBearingImpl;

public class PolarDiagramBase implements PolarDiagram, Serializable {

    private static final long serialVersionUID = 7465253094290674423L;

    protected BoatClass boatClass;
    // the current speed and direction of the wind
    protected SpeedWithBearing windprev = new KnotSpeedWithBearingImpl(6, new DegreeBearingImpl(180));
    protected SpeedWithBearing wind = new KnotSpeedWithBearingImpl(6, new DegreeBearingImpl(180));
    protected SpeedWithBearing trueWind = new KnotSpeedWithBearingImpl(6, new DegreeBearingImpl(180));
    protected SpeedWithBearing current = null; //new KnotSpeedWithBearingImpl(0, new DegreeBearingImpl(180));

    // private static Logger logger = Logger.getLogger("com.sap.sailing");
    protected Bearing targetDirection = new DegreeBearingImpl(0); // bearing of target may deviate from 0 degrees, if target is not exactly windward
    protected NavigableMap<Speed, NavigableMap<Bearing, Speed>> speedTable;
    protected NavigableMap<Double,Object> extTable = null;
    protected NavigableMap<Speed, Bearing> beatAngles;
    protected NavigableMap<Speed, Bearing> jibeAngles;
    protected NavigableMap<Speed, Speed> beatSOG;
    protected NavigableMap<Speed, Speed> jibeSOG;
    
    protected double scaleBearing = 1.0;
    protected double scaleSpeed = 1.0;

    @Override
    public void setSpeedScale(double scaleSpeed) {
        this.scaleSpeed = scaleSpeed;
    }
    
    @Override
    public double getSpeedScale() {
        return this.scaleSpeed;
    }
    
    @Override
    public void setBearingScale(double scaleBearing) {
        this.scaleBearing = scaleBearing;
    }

    @Override
    public double getBearingScale() {
        return this.scaleBearing;
    }
    
    @Override
    public NavigableMap<Speed, NavigableMap<Bearing, Speed>> getSpeedTable() {
        return this.speedTable;
    }

    @Override
    public NavigableMap<Speed, Bearing> getBeatAngles() {
        return this.beatAngles;
    }

    @Override
    public NavigableMap<Speed, Bearing> getJibeAngles() {
        return this.jibeAngles;
    }

    @Override
    public NavigableMap<Speed, Speed> getBeatSOG() {
        return this.beatSOG;
    }

    @Override
    public NavigableMap<Speed, Speed> getJibeSOG() {
        return this.jibeSOG;
    }

    // this constructor creates an instance with a hard-coded set of values
    public PolarDiagramBase() {
    }

    public PolarDiagramBase(PolarDiagramBase pd) {
        boatClass = pd.boatClass;
        speedTable = pd.speedTable;
        beatAngles = pd.beatAngles;
        beatSOG = pd.beatSOG;
        jibeAngles = pd.jibeAngles;
        jibeSOG = pd.jibeSOG;
        current = pd.current;
        extTable = pd.extTable;
    }

    // a constructor that allows a generic set of parameters
    public PolarDiagramBase(NavigableMap<Speed, NavigableMap<Bearing, Speed>> speeds,
            NavigableMap<Speed, Bearing> beats, NavigableMap<Speed, Bearing> jibes,
            NavigableMap<Speed, Speed> beatSOGs, NavigableMap<Speed, Speed> jibeSOGs) {
        wind = new KnotSpeedWithBearingImpl(0, new DegreeBearingImpl(180));
        boatClass = null;
        speedTable = speeds;
        beatAngles = beats;
        jibeAngles = jibes;
        beatSOG = beatSOGs;
        jibeSOG = jibeSOGs;
        for (Speed s : speedTable.keySet()) {
            if (beatAngles.containsKey(s) && !speedTable.get(s).containsKey(beatAngles.get(s))) {
                speedTable.get(s).put(beatAngles.get(s), beatSOG.get(s));
            }
            if (jibeAngles.containsKey(s) && !speedTable.get(s).containsKey(jibeAngles.get(s))) {
                speedTable.get(s).put(jibeAngles.get(s), jibeSOG.get(s));
            }
        }
    }

    @Override
    public SpeedWithBearing getWind() {
        return wind;
    }

    @Override
    public void setWind(SpeedWithBearing newWind) {
        if ((windprev.getKnots() != newWind.getKnots())||(windprev.getBearing().getDegrees() != newWind.getBearing().getDegrees())) {
            windprev = newWind;
            if ((current == null)||(current.getKnots() == 0.0)) {
                wind = newWind;
                trueWind = newWind;
            } else {
                wind = getApparentWindFromCurrent(newWind);
                trueWind = newWind;
            }
        }
    }

    // initialize polar diagram with supporting points to represent mapping:
    //     trueWindSpeed -> { currentSpeed -> { currentBearingTW -> { boatBearingTW -> boatSpeed } } }
    // where currentBearingTW and boatBearingTW are bearings relative to true wind
    public NavigableMap<Double,Object> extendSpeedMap() {
        NavigableMap<Double,Object> extMap = new TreeMap<Double,Object>();
        for(Map.Entry<Speed,NavigableMap<Bearing,Speed>> windSpeedEntry : speedTable.entrySet()) {
            double windSpeed = windSpeedEntry.getKey().getKnots();
            if (windSpeed == 0.0) {
                continue;
            }
            NavigableMap<Double,Object> wcurSpeedMap = new TreeMap<Double,Object>();
            for(double wcurSpeed=0.0; wcurSpeed<=2.2; wcurSpeed+=0.2) {
                NavigableMap<Double,Object> wcurBearMap = new TreeMap<Double,Object>();
                for(double wcurBear=0.0; wcurBear<360.0; wcurBear+=10) {
                    SpeedWithBearing trueWind = new KnotSpeedWithBearingImpl(windSpeed, new DegreeBearingImpl(180.0));
                    this.setCurrent(new KnotSpeedWithBearingImpl(wcurSpeed, new DegreeBearingImpl(wcurBear)));
                    wind = getApparentWindFromCurrent(trueWind);
                    NavigableMap<Double,Double> boatBearMap = new TreeMap<Double,Double>();
                    Bearing[] optBear = this.optimalDirectionsUpwind();
                    int stepSize = 1;
                    // determine non-sailable area, i.e. bearings lower to the wind than beat angle
                    double minBear = optBear[1].getDegrees() - optBear[1].getDegrees() % stepSize;
                    double maxBear = optBear[0].getDegrees() + (stepSize - optBear[0].getDegrees() % stepSize);
                    Double minBearSOG = null;
                    Double maxBearSOG = null;
                    // calculate wind/current-specific polar diagram
                    for (double boatBearSMF=minBear; boatBearSMF<=maxBear; boatBearSMF+=stepSize) {
                        double boatBearSOG;
                        double boatSpeedSOG;
                        SpeedWithBearing rotSpeed = new KnotSpeedWithBearingImpl(this.getSpeedAtBearingRaw(new DegreeBearingImpl(boatBearSMF)).getKnots(), new DegreeBearingImpl(boatBearSMF));
                        SpeedWithBearing transSpeed = getSOGfromSMF(rotSpeed);
                        boatBearSOG = transSpeed.getBearing().getDegrees();
                        boatSpeedSOG = transSpeed.getKnots();
                        boatBearMap.put(boatBearSOG, boatSpeedSOG);
                        // calculate speed of boat floating with current in non-sailable area, i.e. no sail force
                        if (boatBearSMF == minBear) {
                            minBearSOG = boatBearSOG;
                        }
                        if (boatBearSMF == maxBear) {
                            maxBearSOG = boatBearSOG;
                        }
                    }
                    if (minBearSOG != null) {
                        for(double tmpBear=(Math.floor(minBearSOG)-1.0);tmpBear>=0.0; tmpBear-=1.0) {
                            boatBearMap.put(tmpBear, 0.0);
                        }
                    }
                    if (maxBearSOG != null) {
                        for(double tmpBear=(Math.ceil(maxBearSOG)+1.0);tmpBear<=360.0; tmpBear+=1.0) {
                            boatBearMap.put(tmpBear, 0.0);
                        }
                    }
                    wcurBearMap.put(wcurBear, boatBearMap);
                }
                wcurSpeedMap.put(wcurSpeed, wcurBearMap);
            }
            extMap.put(windSpeed, wcurSpeedMap);
        }
        return extMap;
    }

    // TODO: define wrapper method to make parameters less cryptic
    // calculate boatSpeed based on interpolation of supporting points
    // value[0]: trueWindSpeed
    // value[1]: currentSpeed
    // value[2]: currentBearingTW
    // value[3]: boatBearingTW
    // level: used for recursion termination, init with 0
    // map: map initialized by method this.extendSpeedMap()
    //
    @SuppressWarnings("unchecked")
    public double interpolate(double[] values, int level, NavigableMap<Double,Object> map) {
        NavigableMap<Double,Object> tmp;
        double crValue = values[level];
        Double hiDouble = map.ceilingKey(crValue);
        if ((level==2)||(level==3)) {
            if (hiDouble == null) {
                hiDouble = map.ceilingKey(crValue-360);
            }
        }
        double hiValue = hiDouble == null ? 0.0 : hiDouble.doubleValue();
        double hiResult = 0.0;
        if (level < (values.length-1)) {
            tmp = (NavigableMap<Double,Object>)map.get(hiValue);
            hiResult = interpolate(values,level+1,tmp);
        } else {
            hiResult = (Double)map.get(hiValue);
        }
        Double loDouble = map.floorKey(crValue);
        if (level==3) {
            if (loDouble == null) {
                loDouble = map.floorKey(crValue+360);
            }
        }
        double loValue = loDouble.doubleValue();
        double loResult = 0.0;
        if (level < (values.length-1)) {
            tmp = (NavigableMap<Double,Object>)map.get(loValue);
            loResult = interpolate(values,level+1,tmp);
        } else {
            loResult = (Double)map.get(loValue);
        }
        double interpolatedValue = (hiValue == loValue ? loResult : loResult + (hiResult - loResult)*(crValue - loValue)/(hiValue - loValue));
        return interpolatedValue;
    }

    @Override
    public void initializeSOGwithCurrent() {
        this.setCurrent(null);
    }
    
    @Override
    public void setCurrent(SpeedWithBearing newCurrent) {
        if ((newCurrent == null)&&(extTable == null)) {
            extTable = this.extendSpeedMap();
        }
        current = newCurrent;
    }


    @Override
    public SpeedWithBearing getCurrent() {
        return current;
    }

    @Override
    public boolean hasCurrent() {
        if (current == null) {
            return false;
        }
        if (current.getKnots() > 0) {
            return true;
        } else {
            return false;
        }
    }
    
    
    public SpeedWithBearing addVectorSpeeds(SpeedWithBearing a, SpeedWithBearing b) {
        if (a.getKnots() == 0.0) {
            return b;
        }
        if (b.getKnots() == 0.0) {
            return a;
        }
        double xA = a.getKnots() * Math.sin( a.getBearing().getRadians() );
        double yA = a.getKnots() * Math.cos( a.getBearing().getRadians() );
        double xB = b.getKnots() * Math.sin( b.getBearing().getRadians() );
        double yB = b.getKnots() * Math.cos( b.getBearing().getRadians() );
        double xC = xA + xB;
        double yC = yA + yB;
        double bearC = Math.atan2(xC, yC);
        if (bearC < 0) {
            bearC += 2*Math.PI;
        }
        double lengthC = Math.sqrt( xC*xC + yC*yC );
        return new KnotSpeedWithBearingImpl(lengthC, new RadianBearingImpl(bearC));
    }

    public SpeedWithBearing getApparentWindFromCurrent(SpeedWithBearing newWind) {
        if (current == null) {
            return newWind;
        }
        if (current.getKnots() == 0.0) {
            return newWind;
        }
        return addVectorSpeeds(newWind, new KnotSpeedWithBearingImpl(current.getKnots(), current.getBearing().reverse()));
    }


    // convert speed in moving frame (SMF) to speed over ground (SOG)
    public SpeedWithBearing getSOGfromSMF(SpeedWithBearing smf) {
        if (current == null) {
            return smf;
        } else {
            return addVectorSpeeds(smf, current);
        }
    }

    @Override
    public SpeedWithBearing getSpeedAtBearingOverGround(Bearing bearing) {
        if ((current == null)||(current.getKnots() == 0.0)) {
            return getSpeedAtBearingRaw(bearing);
        } else {
            double[] values = new double[4];
            values[0] = trueWind.getKnots();
            values[1] = current.getKnots();
            values[2] = trueWind.getBearing().reverse().getDifferenceTo(current.getBearing()).getDegrees();
            if (values[2] < 0.0) {
                values[2] += 360.0;
            }
            values[3] = trueWind.getBearing().reverse().getDifferenceTo(bearing).getDegrees();
            if (values[3] < 0.0) {
                values[3] += 360.0;
            }
            double boatSpeed = this.interpolate(values, 0, extTable);
            return new KnotSpeedWithBearingImpl(boatSpeed*this.scaleSpeed, bearing);
        }
    }

    @Override
    public SpeedWithBearing getSpeedAtBearing(Bearing bearing) {
        if (current == null) {
            return getSpeedAtBearingRaw(bearing);
        } else {
            SpeedWithBearing rotSpeed = new KnotSpeedWithBearingImpl(this.getSpeedAtBearingRaw(bearing).getKnots(), bearing);
            SpeedWithBearing transSpeed = getSOGfromSMF(rotSpeed);
            return transSpeed;
        }
    }

    public SpeedWithBearing getSpeedAtBearingRaw(Bearing bearing) {
        Bearing relativeBearing = wind.getBearing().reverse().getDifferenceTo(bearing);
        if (relativeBearing.getDegrees() < 0) {
            relativeBearing = relativeBearing.getDifferenceTo(new DegreeBearingImpl(0));
        }
        Speed floorWind = speedTable.floorKey(wind);
        Speed ceilingWind = speedTable.ceilingKey(wind);
        if (ceilingWind == null) {
            ceilingWind = floorWind;
        }
        if (floorWind == null) {
            floorWind = ceilingWind;
        }
        NavigableMap<Bearing, Speed> floorSpeeds = speedTable.get(floorWind);
        NavigableMap<Bearing, Speed> ceilingSpeeds = speedTable.get(ceilingWind);
        double floorSpeed;
        if (floorSpeeds.size() == 0) {
            floorSpeed = 0.0;
        } else {
            Speed floorSpeed1 = null;
            Entry<Bearing, Speed> floorEntry = floorSpeeds.floorEntry(relativeBearing);
            if (floorEntry != null) {
                floorSpeed1 = floorEntry.getValue();
            }
            Speed floorSpeed2 = null;
            floorEntry = floorSpeeds.ceilingEntry(relativeBearing);
            if (floorEntry != null) {
                floorSpeed2 = floorEntry.getValue();
            }
            Bearing floorBearing1 = floorSpeeds.floorKey(relativeBearing);
            Bearing floorBearing2 = floorSpeeds.ceilingKey(relativeBearing);
            if (floorBearing1 == null) {
                // for gps-polar: if no floor-bearing known; extrapolate boat-speed constantly with bearing
                floorSpeed = floorSpeed2.getKnots();
            } else if (floorBearing2 == null) {
                // for gps-polar: if no ceiling-bearing known; extrapolate boat-speed constantly with bearing
                floorSpeed = floorSpeed1.getKnots();
            } else if (floorSpeed1.equals(floorSpeed2)) {
                floorSpeed = floorSpeed1.getKnots();
            } else {
                floorSpeed = floorSpeed1.getKnots() + (relativeBearing.getRadians() - floorBearing1.getRadians())
                        * (floorSpeed2.getKnots() - floorSpeed1.getKnots())
                        / (floorBearing2.getRadians() - floorBearing1.getRadians());
            }
        }
        double ceilingSpeed;
        if (ceilingSpeeds.size() == 0) {
            ceilingSpeed = 0.0;
        } else {
            Speed ceilingSpeed1 = null;
            Entry<Bearing, Speed> entry = ceilingSpeeds.floorEntry(relativeBearing);
            if (entry != null) {
                ceilingSpeed1 = entry.getValue();
            }
            Speed ceilingSpeed2 = null;
            entry = ceilingSpeeds.ceilingEntry(relativeBearing);
            if (entry != null) {
                ceilingSpeed2 = entry.getValue();
            }
            Bearing ceilingBearing1 = ceilingSpeeds.floorKey(relativeBearing);
            Bearing ceilingBearing2 = ceilingSpeeds.ceilingKey(relativeBearing);
            if (ceilingBearing1 == null) {
                // for gps-polar: if no floor-bearing known; extrapolate boat-speed constantly with bearing
                ceilingSpeed = ceilingSpeed2.getKnots();
            } else if (ceilingBearing2 == null) {
                // for gps-polar: if no ceiling-bearing known; extrapolate boat-speed constantly with bearing
                ceilingSpeed = ceilingSpeed1.getKnots();
            } else if (ceilingSpeed1.equals(ceilingSpeed2)) {
                ceilingSpeed = ceilingSpeed1.getKnots();
            } else {
                ceilingSpeed = ceilingSpeed1.getKnots() + (relativeBearing.getRadians() - ceilingBearing1.getRadians())
                        * (ceilingSpeed2.getKnots() - ceilingSpeed1.getKnots())
                        / (ceilingBearing2.getRadians() - ceilingBearing1.getRadians());
            }
        }
        if (floorSpeeds.size() == 0) {
        	// for gps-polar: if no boat-speeds known for floor-wind-speed, extrapolate linearly 
        	floorSpeed = ceilingSpeed * floorWind.getKnots() / ceilingWind.getKnots();
        }
        if (ceilingSpeeds.size() == 0) {
        	// for gps-polar: if no boat-speeds known for ceiling-wind-speed, extrapolate linearly 
        	ceilingSpeed = floorSpeed * ceilingWind.getKnots() / floorWind.getKnots();
        }
        double speed;
        if (floorWind.equals(ceilingWind)) {
            speed = floorSpeed;
        } else {
            speed = floorSpeed + (wind.getKnots() - floorWind.getKnots()) * (ceilingSpeed - floorSpeed)
                    / (ceilingWind.getKnots() - floorWind.getKnots());
        }
        return new KnotSpeedWithBearingImpl(speed*this.scaleSpeed, bearing);
    }

    @Override
    public Bearing[] optimalDirectionsUpwind() {
        Bearing windBearing = wind.getBearing().reverse();
        Bearing estBeatAngleRight = null;
        Bearing estBeatAngleLeft = null;
        if (targetDirection.equals(new DegreeBearingImpl(0))) {
            Bearing floorBeatAngle;
            if (beatAngles.floorEntry(wind) == null) {
                floorBeatAngle = beatAngles.ceilingEntry(wind)==null?null:beatAngles.ceilingEntry(wind).getValue();
            } else {
                floorBeatAngle = beatAngles.floorEntry(wind).getValue();
            }
            Bearing ceilingBeatAngle;
            if (beatAngles.ceilingEntry(wind) == null) {
                ceilingBeatAngle = beatAngles.floorEntry(wind)==null?null:beatAngles.floorEntry(wind).getValue();
            } else {
                ceilingBeatAngle = beatAngles.ceilingEntry(wind).getValue();
            }
            if (floorBeatAngle == null) {
                floorBeatAngle = new DegreeBearingImpl(0);
            }
            if (ceilingBeatAngle == null) {
                ceilingBeatAngle = new DegreeBearingImpl(0);
            }
            Speed floorSpeed = beatAngles.floorKey(wind);
            if (floorSpeed == null) {
                floorSpeed = beatAngles.ceilingKey(wind);
            }
            Speed ceilingSpeed = beatAngles.ceilingKey(wind);
            if (beatAngles.ceilingKey(wind) == null) {
                ceilingSpeed = beatAngles.floorKey(wind);
            }
            double beatAngle;
            if (floorSpeed.equals(ceilingSpeed)) {
                beatAngle = floorBeatAngle.getRadians();
            } else {
                beatAngle = floorBeatAngle.getRadians() + (wind.getKnots() - floorSpeed.getKnots())
                        * (ceilingBeatAngle.getRadians() - floorBeatAngle.getRadians())
                        / (ceilingSpeed.getKnots() - floorSpeed.getKnots());
            }
            // get bearing to north based on beatAngle and windBearing
            double scaledBeatAngle = beatAngle*this.scaleBearing;
            estBeatAngleLeft = windBearing.add(new RadianBearingImpl(-scaledBeatAngle));
            estBeatAngleRight = windBearing.add(new RadianBearingImpl(+scaledBeatAngle));
            return new Bearing[] { estBeatAngleLeft, estBeatAngleRight };
        } else {
            Set<Bearing> allKeys = new TreeSet<Bearing>(bearingComparator);
            for (Double b = 0.0; b < 360.0; b += 5.0) {
                allKeys.add(new DegreeBearingImpl(b));
            }
            Bearing _targetDirection = targetDirection;
            // TODO: rework, adding optimalDirectionsUpwind will be bearing over ground including water current
            setTargetDirection(new DegreeBearingImpl(0.0));
            allKeys.addAll(Arrays.asList(optimalDirectionsUpwind()));
            allKeys.addAll(Arrays.asList(optimalDirectionsDownwind()));
            setTargetDirection(_targetDirection);
            Double maxSpeedRight = 0.0;
            Double maxSpeedLeft = 0.0;
            for (Bearing b : allKeys) {
                if (b.getDifferenceTo(getWind().getBearing()).getDegrees() > 0) {
                    double currentSpeedRight = getSpeedAtBearing(b).getKnots()
                            * Math.cos(b.getDifferenceTo(getTargetDirection()).getRadians());
                    if (currentSpeedRight > maxSpeedRight) {
                        maxSpeedRight = currentSpeedRight;
                        estBeatAngleRight = b;
                    }
                } else {
                    double currentSpeedLeft = getSpeedAtBearing(b).getKnots()
                            * Math.cos(b.getDifferenceTo(getTargetDirection()).getRadians());
                    if (currentSpeedLeft > maxSpeedLeft) {
                        maxSpeedLeft = currentSpeedLeft;
                        estBeatAngleLeft = b;
                    }
                }
            }
            return new Bearing[] { estBeatAngleLeft, estBeatAngleRight };
        }
    }

    @Override
    public SpeedWithBearing[] optimalVMGUpwind() {
        Bearing windBearing = wind.getBearing().reverse();
        Bearing estBeatAngleRight = null;
        Bearing estBeatAngleLeft = null;
        Bearing diffWindTarget = windBearing.getDifferenceTo(targetDirection);
        if (diffWindTarget.equals(new DegreeBearingImpl(0))) {
            //
            // target is aligned with wind, i.e. target bearing = 0°
            //
            Bearing floorBeatAngle;
            if (beatAngles.floorEntry(wind) == null) {
                floorBeatAngle = beatAngles.ceilingEntry(wind).getValue();
            } else {
                floorBeatAngle = beatAngles.floorEntry(wind).getValue();
            }
            Bearing ceilingBeatAngle;
            if (beatAngles.ceilingEntry(wind) == null) {
                ceilingBeatAngle = beatAngles.floorEntry(wind).getValue();
            } else {
                ceilingBeatAngle = beatAngles.ceilingEntry(wind).getValue();
            }
            if (floorBeatAngle == null) {
                floorBeatAngle = new DegreeBearingImpl(0);
            }
            if (ceilingBeatAngle == null) {
                ceilingBeatAngle = new DegreeBearingImpl(0);
            }

            Speed floorSpeed = beatAngles.floorKey(wind);
            if (floorSpeed == null) {
                floorSpeed = beatAngles.ceilingKey(wind);
            }
            Speed ceilingSpeed = beatAngles.ceilingKey(wind);
            if (beatAngles.ceilingKey(wind) == null) {
                ceilingSpeed = beatAngles.floorKey(wind);
            }
            double beatAngle;
            if (floorSpeed.equals(ceilingSpeed)) {
                beatAngle = floorBeatAngle.getRadians();
            } else {
                beatAngle = floorBeatAngle.getRadians() + (wind.getKnots() - floorSpeed.getKnots())
                        * (ceilingBeatAngle.getRadians() - floorBeatAngle.getRadians())
                        / (ceilingSpeed.getKnots() - floorSpeed.getKnots());
            }
            estBeatAngleRight = new RadianBearingImpl(+beatAngle);
            estBeatAngleLeft = new RadianBearingImpl(-beatAngle);
            double speedLeft = this.getSpeedAtBearing(estBeatAngleLeft).getKnots()
                    * Math.cos(estBeatAngleLeft.getRadians());
            double speedRight = this.getSpeedAtBearing(estBeatAngleRight).getKnots()
                    * Math.cos(estBeatAngleRight.getRadians());
            SpeedWithBearing optVMGLeft = new KnotSpeedWithBearingImpl(speedLeft, windBearing.add(estBeatAngleLeft));
            SpeedWithBearing optVMGRight = new KnotSpeedWithBearingImpl(speedRight, windBearing.add(estBeatAngleRight));
            return new SpeedWithBearing[] { optVMGLeft, optVMGRight };
        } else {
            //
            // target is not aligned with wind, i.e. target bearing != 0°
            //
            Set<Bearing> allKeys = new TreeSet<Bearing>(bearingComparator);
            /*
             * for (Double b = 0.0; b < 360.0; b += 5.0) allKeys.add(new DegreeBearingImpl(b));
             */
            Bearing _targetDirection = targetDirection;
            setTargetDirection(new DegreeBearingImpl(0.0));
            Bearing[] optDirectionsUpwind = optimalDirectionsUpwind();

            allKeys.addAll(Arrays.asList(optDirectionsUpwind));
            for (int idx = 0; idx < optDirectionsUpwind.length; idx++) {
                for (int offset = 1; offset <= 5; offset++) {
                    allKeys.add(new DegreeBearingImpl(optDirectionsUpwind[idx].getDegrees() + offset));
                    allKeys.add(new DegreeBearingImpl(optDirectionsUpwind[idx].getDegrees() - offset));
                }
            }
            allKeys.addAll(Arrays.asList(optDirectionsUpwind));
            allKeys.addAll(Arrays.asList(optimalDirectionsDownwind()));
            setTargetDirection(_targetDirection);
            Double maxSpeedRight = 0.0;
            Double maxSpeedLeft = 0.0;
            for (Bearing b : allKeys) {
                if (b.getDifferenceTo(getWind().getBearing()).getDegrees() > 0) {
                    double currentSpeedRight = getSpeedAtBearing(b).getKnots()
                            * Math.cos(b.getDifferenceTo(getTargetDirection()).getRadians());
                    if (currentSpeedRight > maxSpeedRight) {
                        maxSpeedRight = currentSpeedRight;
                        estBeatAngleRight = b;
                    }
                } else {
                    double currentSpeedLeft = getSpeedAtBearing(b).getKnots()
                            * Math.cos(b.getDifferenceTo(getTargetDirection()).getRadians());
                    if (currentSpeedLeft > maxSpeedLeft) {
                        maxSpeedLeft = currentSpeedLeft;
                        estBeatAngleLeft = b;
                    }
                }
            }
            SpeedWithBearing optVMGLeft = new KnotSpeedWithBearingImpl(maxSpeedLeft, estBeatAngleLeft);
            SpeedWithBearing optVMGRight = new KnotSpeedWithBearingImpl(maxSpeedRight, estBeatAngleRight);
            return new SpeedWithBearing[] { optVMGLeft, optVMGRight };
        }
    }

    @Override
    public Bearing[] optimalDirectionsDownwind() {
        Bearing windBearing = wind.getBearing().reverse();
        Bearing estJibeAngleRight = null;
        Bearing estJibeAngleLeft = null;
        if (getTargetDirection().equals(new DegreeBearingImpl(0))) {
            windBearing = wind.getBearing().reverse();
            estJibeAngleRight = null;
            estJibeAngleLeft = null;
            // lookup jibe-angles based on wind-values
            Map.Entry<Speed, Bearing> floorEntry = jibeAngles.floorEntry(wind);
            Bearing floorJibeAngle = null;
            if (floorEntry != null) {
            	floorJibeAngle = floorEntry.getValue();
            }
            Map.Entry<Speed, Bearing> ceilingEntry = jibeAngles.ceilingEntry(wind);            
            Bearing ceilingJibeAngle = null;
            if (ceilingEntry != null) {
            	ceilingJibeAngle = ceilingEntry.getValue();
            }
            // handle jibe-angles for out-of-definition-range wind-values 
            if (floorJibeAngle == null) {
                floorJibeAngle = ceilingJibeAngle;
            }
            if (ceilingJibeAngle == null) {
                ceilingJibeAngle = floorJibeAngle;
            }
            // lookup wind-support-values based on wind-values
            Speed floorSpeed = jibeAngles.floorKey(wind);
            Speed ceilingSpeed = jibeAngles.ceilingKey(wind);
            // handle out-of-definition-range wind-values 
            if (floorSpeed == null) {
                floorSpeed = ceilingSpeed;
            }
            if (ceilingSpeed == null) {
                ceilingSpeed = floorSpeed;
            }
            double jibeAngle;
            if (floorSpeed.equals(ceilingSpeed)) {
                jibeAngle = floorJibeAngle.getRadians();
            } else {
                jibeAngle = floorJibeAngle.getRadians() + (wind.getKnots() - floorSpeed.getKnots())
                        * (ceilingJibeAngle.getRadians() - floorJibeAngle.getRadians())
                        / (ceilingSpeed.getKnots() - floorSpeed.getKnots());
            }
            // Bearing estJibeAngle = new RadianBearingImpl(jibeAngle);

            double scaledJibeAngle = Math.PI-(Math.PI-jibeAngle)*this.scaleBearing;
            estJibeAngleRight = windBearing.add(new RadianBearingImpl(+scaledJibeAngle));
            estJibeAngleLeft = windBearing.add(new RadianBearingImpl(-scaledJibeAngle));
            return new Bearing[] { estJibeAngleRight, estJibeAngleLeft };
        } else {
            Set<Bearing> allKeys = new TreeSet<Bearing>(bearingComparator);
            for (Double b = 0.0; b < 360.0; b += 5.0) {
                allKeys.add(new DegreeBearingImpl(b));
            }
            Bearing _targetDirection = targetDirection;
            setTargetDirection(new DegreeBearingImpl(0.0));
            allKeys.addAll(Arrays.asList(optimalDirectionsUpwind()));
            allKeys.addAll(Arrays.asList(optimalDirectionsDownwind()));
            setTargetDirection(_targetDirection);
            Double maxSpeedRight = 0.0;
            Double maxSpeedLeft = 0.0;
            for (Bearing b : allKeys) {
                if (b.getDifferenceTo(getWind().getBearing()).getDegrees() > 0) {
                    if (getSpeedAtBearing(b).getKnots()
                            * Math.cos(b.getDifferenceTo(getTargetDirection().reverse()).getRadians()) > maxSpeedRight) {
                        maxSpeedRight = getSpeedAtBearing(b).getKnots()
                                * Math.cos(b.getDifferenceTo(getTargetDirection().reverse()).getRadians());
                        estJibeAngleRight = b;
                    }
                } else if (getSpeedAtBearing(b).getKnots()
                        * Math.cos(b.getDifferenceTo(getTargetDirection().reverse()).getRadians()) > maxSpeedLeft) {
                    maxSpeedLeft = getSpeedAtBearing(b).getKnots()
                            * Math.cos(b.getDifferenceTo(getTargetDirection().reverse()).getRadians());
                    estJibeAngleLeft = b;
                }
            }
            return new Bearing[] { estJibeAngleLeft, estJibeAngleRight };
        }
    }

    // a Bearing Comparator useful in the creation of sorted sets of Bearing
    public static Comparator<Bearing> bearingComparator = new Comparator<Bearing>() {
        @Override
        public int compare(Bearing o1, Bearing o2) {
            double d1 = o1.getDegrees();
            if (d1 < 0) {
                d1 = 360 + d1;
            }
            double d2 = o2.getDegrees();
            if (d2 < 0) {
                d2 = 360 + d2;
            }
            return d1 < d2 ? -1 : d1 == d2 ? 0 : 1;
        }
    };

    @Override
    public long getTurnLoss() {
        // TODO: retrieve values from polar data mining
        int turnLoss;
        if ((this.boatClass.getName() != null) && (this.boatClass.getName().equals(BoatClassMasterdata.EXTREME_40.getDisplayName()))) {
            turnLoss = 2000;
        } else if ((this.boatClass.getName() != null) && (this.boatClass.getName().equals(BoatClassMasterdata.GC_32.getDisplayName()))) {
            // TODO: Needs to be validated
            turnLoss = 2000;
        } else if ((this.boatClass.getName() != null) && (this.boatClass.getName().equals(BoatClassMasterdata._5O5.getDisplayName()))) {
            turnLoss = 5000;
        } else {
            turnLoss = 4000; // default value
        }
        return turnLoss;
    }

    @Override
    public WindSide getWindSide(Bearing bearing) {
        WindSide windSide = null;
        if (bearingComparator.compare(bearing, wind.getBearing().reverse()) > 0) {
            windSide = WindSide.LEFT;
        }
        if (bearingComparator.compare(bearing, wind.getBearing().reverse()) < 0) {
            windSide = WindSide.RIGHT;
        }
        if (bearing.equals(wind.getBearing())) {
            windSide = WindSide.DOWNWIND;
        }
        if (bearing.equals(wind.getBearing().reverse())) {
            windSide = WindSide.UPWIND;
        }
        return windSide;
    }

    // returns a table of Bearing-Speed pairs with a bearingStep granularity
    // for all Speeds in speedTable
    @Override
    public NavigableMap<Speed, NavigableMap<Bearing, Speed>> polarDiagramPlot(Double bearingStep, Set<Speed> extraSpeeds) {
        NavigableMap<Speed, NavigableMap<Bearing, Speed>> table = new TreeMap<Speed, NavigableMap<Bearing, Speed>>();
        Set<Speed> speedSet = new TreeSet<Speed>();
        speedSet.addAll(speedTable.keySet());
        if (extraSpeeds != null) {
            speedSet.addAll(extraSpeeds);
        }
        for (Speed s : speedSet) {
            setWind(new KnotSpeedWithBearingImpl(s.getKnots(), new DegreeBearingImpl(180)));
            NavigableMap<Bearing, Speed> currentTable = new TreeMap<Bearing, Speed>(bearingComparator);
            table.put(s, currentTable);
            for (Double b = 0.0; b < 360.0; b += bearingStep) {
                Bearing bearing = new DegreeBearingImpl(b);
                currentTable.put(bearing, getSpeedAtBearing(bearing));
            }
        }
        return table;
    }

    @Override
    public NavigableMap<Speed, NavigableMap<Bearing, Speed>> polarDiagramPlot(Double bearingStep) {
        NavigableMap<Speed, NavigableMap<Bearing, Speed>> table = new TreeMap<Speed, NavigableMap<Bearing, Speed>>();
        Set<Bearing> extraBearings = new HashSet<Bearing>();
        for (Speed s : speedTable.keySet()) {
            setWind(new KnotSpeedWithBearingImpl(s.getKnots(), new DegreeBearingImpl(180)));
            extraBearings.addAll(Arrays.asList(optimalDirectionsUpwind()));
            extraBearings.addAll(Arrays.asList(optimalDirectionsDownwind()));
        }
        for (Speed s : speedTable.keySet()) {
            setWind(new KnotSpeedWithBearingImpl(s.getKnots(), new DegreeBearingImpl(180)));
            NavigableMap<Bearing, Speed> currentTable = new TreeMap<Bearing, Speed>(bearingComparator);
            table.put(s, currentTable);
            for (Double b = 0.0; b < 360.0; b += bearingStep) {
                Bearing bearing = new DegreeBearingImpl(b);
                currentTable.put(bearing, getSpeedAtBearing(bearing));
            }
        }
        return table;
    }

    @Override
    public Bearing getTargetDirection() {
        return targetDirection;
    }

    @Override
    public void setTargetDirection(Bearing newTargetDirection) {
        targetDirection = newTargetDirection;
    }

    @Override
    public Pair<PointOfSail, BoatDirection> getPointOfSail(Bearing bearTarget) {
        double offSet = 1;
        Bearing[] bearOptimalUpwind = this.optimalDirectionsUpwind();
        Bearing[] bearOptimalDownwind = this.optimalDirectionsDownwind();
        // compare target bearing to upwind bearings
        Bearing upwindLeftRight = bearOptimalUpwind[0].getDifferenceTo(bearOptimalUpwind[1]);
        Bearing upwindLeftTarget = bearOptimalUpwind[0].getDifferenceTo(bearTarget);
        PointOfSail pointOfSail = PointOfSail.REACHING;
        BoatDirection reachingSide = BoatDirection.NONE;
        // check whether boat is in "tacking area"
        if ((upwindLeftTarget.getDegrees() >= -offSet) && (upwindLeftTarget.getDegrees() <= upwindLeftRight.getDegrees() + offSet)) {
            //logger.fine("point-of-sail: tacking (diffLeftTarget: " + upwindLeftTarget.getDegrees()
            //        + ", diffLeftRight: " + upwindLeftRight.getDegrees() + ", " + currentPosition + ")");
            pointOfSail = PointOfSail.TACKING;
        } else {
            Bearing downwindLeftRight = bearOptimalDownwind[0].getDifferenceTo(bearOptimalDownwind[1]);
            Bearing downwindLeftTarget = bearOptimalDownwind[0].getDifferenceTo(bearTarget);
            // check whether boat is in "non-sailable area"
            if ((downwindLeftTarget.getDegrees() >= -offSet) && (downwindLeftTarget.getDegrees() <= downwindLeftRight.getDegrees() + offSet)) {
                //logger.fine("point-of-sail: jibing (diffLeftTarget: " + downwindLeftTarget.getDegrees()
                //        + ", diffLeftRight: " + downwindLeftRight.getDegrees() + ", " + currentPosition + ")");
                pointOfSail = PointOfSail.JIBING;
            } else {
                // logger.info("path: "+path.path);
                Bearing windBoat = wind.getBearing().getDifferenceTo(bearTarget);
                if (windBoat.getDegrees() > 0) {
                    reachingSide = BoatDirection.REACH_LEFT; // left-sided reaching
                } else {
                    reachingSide = BoatDirection.REACH_RIGHT; // right-sided reaching
                }
            }
        }
        return new Pair<PointOfSail, BoatDirection>(pointOfSail, reachingSide);
    }

}
