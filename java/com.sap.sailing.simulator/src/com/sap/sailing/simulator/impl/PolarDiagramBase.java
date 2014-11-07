package com.sap.sailing.simulator.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.RadianBearingImpl;
import com.sap.sailing.simulator.PolarDiagram;

public class PolarDiagramBase implements PolarDiagram, Serializable {

    private static final long serialVersionUID = 7465253094290674423L;

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
    protected NavigableMap<Speed, Bearing> gybeAngles;
    protected NavigableMap<Speed, Speed> beatSOG;
    protected NavigableMap<Speed, Speed> gybeSOG;
    
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
    public NavigableMap<Speed, Bearing> getGybeAngles() {
        return this.gybeAngles;
    }

    @Override
    public NavigableMap<Speed, Speed> getBeatSOG() {
        return this.beatSOG;
    }

    @Override
    public NavigableMap<Speed, Speed> getGybeSOG() {
        return this.gybeSOG;
    }

    // this constructor creates an instance with a hard-coded set of values
    public PolarDiagramBase() {
        // do nothing
    }

    // a constructor that allows a generic set of parameters
    public PolarDiagramBase(NavigableMap<Speed, NavigableMap<Bearing, Speed>> speeds,
            NavigableMap<Speed, Bearing> beats, NavigableMap<Speed, Bearing> gybes,
            NavigableMap<Speed, Speed> beatSOGs, NavigableMap<Speed, Speed> gybeSOGs) {

        wind = new KnotSpeedWithBearingImpl(0, new DegreeBearingImpl(180));

        speedTable = speeds;
        beatAngles = beats;
        gybeAngles = gybes;
        beatSOG = beatSOGs;
        gybeSOG = gybeSOGs;

        for (Speed s : speedTable.keySet()) {

            if (beatAngles.containsKey(s) && !speedTable.get(s).containsKey(beatAngles.get(s))) {
                speedTable.get(s).put(beatAngles.get(s), beatSOG.get(s));
            }

            if (gybeAngles.containsKey(s) && !speedTable.get(s).containsKey(gybeAngles.get(s))) {
                speedTable.get(s).put(gybeAngles.get(s), gybeSOG.get(s));
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
                    for(double boatBearSMF=minBear; boatBearSMF<=maxBear; boatBearSMF+=stepSize) {

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
        if (level==3) {
            if (hiDouble == null) {
                hiDouble = map.ceilingKey(crValue-360);
            }
        }
        double hiValue = hiDouble.doubleValue();
        double hiResult = 0.0;
        if (level < (values.length-1)) {
            tmp = (NavigableMap<Double,Object>)map.get(hiValue);
            hiResult = interpolate(values,level+1,tmp);
        } else {
            hiResult = (Double)map.get(hiValue);
        }

        //System.out.println(""+level+", "+crValue+", "+map);
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
        //System.out.println("bearC: "+(bearC*180/Math.PI));
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

        // Taylor estimations of order 1
        Speed floorSpeed1 = floorSpeeds.floorEntry(relativeBearing).getValue();
        Speed floorSpeed2 = floorSpeeds.ceilingEntry(relativeBearing).getValue();
        Bearing floorBearing1 = floorSpeeds.floorKey(relativeBearing);
        Bearing floorBearing2 = floorSpeeds.ceilingKey(relativeBearing);
        double floorSpeed;
        if (floorSpeed1.equals(floorSpeed2)) {
            floorSpeed = floorSpeed1.getKnots();
        } else {
            floorSpeed = floorSpeed1.getKnots() + (relativeBearing.getRadians() - floorBearing1.getRadians())
                    * (floorSpeed2.getKnots() - floorSpeed1.getKnots())
                    / (floorBearing2.getRadians() - floorBearing1.getRadians());
        }

        Speed ceilingSpeed1 = ceilingSpeeds.floorEntry(relativeBearing).getValue();
        Speed ceilingSpeed2 = ceilingSpeeds.ceilingEntry(relativeBearing).getValue();
        Bearing ceilingBearing1 = ceilingSpeeds.floorKey(relativeBearing);
        Bearing ceilingBearing2 = ceilingSpeeds.ceilingKey(relativeBearing);
        double ceilingSpeed;
        if (ceilingSpeed1.equals(ceilingSpeed2)) {
            ceilingSpeed = ceilingSpeed1.getKnots();
        } else {
            ceilingSpeed = ceilingSpeed1.getKnots() + (relativeBearing.getRadians() - ceilingBearing1.getRadians())
                    * (ceilingSpeed2.getKnots() - ceilingSpeed1.getKnots())
                    / (ceilingBearing2.getRadians() - ceilingBearing1.getRadians());
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
            // target is aligned with wind, i.e. target bearing = 0�
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
            // target is not aligned with wind, i.e. target bearing != 0�
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
        Bearing estGybeAngleRight = null;
        Bearing estGybeAngleLeft = null;
        if (getTargetDirection().equals(new DegreeBearingImpl(0))) {
            windBearing = wind.getBearing().reverse();
            estGybeAngleRight = null;
            estGybeAngleLeft = null;
            // lookup gybe-angles based on wind-values
            Map.Entry<Speed, Bearing> floorEntry = gybeAngles.floorEntry(wind);
            Bearing floorGybeAngle = null;
            if (floorEntry != null) {
            	floorGybeAngle = floorEntry.getValue();
            }
            Map.Entry<Speed, Bearing> ceilingEntry = gybeAngles.ceilingEntry(wind);            
            Bearing ceilingGybeAngle = null;
            if (ceilingEntry != null) {
            	ceilingGybeAngle = ceilingEntry.getValue();
            }
            // handle gybe-angles for out-of-definition-range wind-values 
            if (floorGybeAngle == null) {
                floorGybeAngle = ceilingGybeAngle;
            }
            if (ceilingGybeAngle == null) {
                ceilingGybeAngle = floorGybeAngle;
            }
            // lookup wind-support-values based on wind-values
            Speed floorSpeed = gybeAngles.floorKey(wind);
            Speed ceilingSpeed = gybeAngles.ceilingKey(wind);
            // handle out-of-definition-range wind-values 
            if (floorSpeed == null) {
                floorSpeed = ceilingSpeed;
            }
            if (ceilingSpeed == null) {
                ceilingSpeed = floorSpeed;
            }
            double gybeAngle;
            if (floorSpeed.equals(ceilingSpeed)) {
                gybeAngle = floorGybeAngle.getRadians();
            } else {
                gybeAngle = floorGybeAngle.getRadians() + (wind.getKnots() - floorSpeed.getKnots())
                        * (ceilingGybeAngle.getRadians() - floorGybeAngle.getRadians())
                        / (ceilingSpeed.getKnots() - floorSpeed.getKnots());
            }
            // Bearing estGybeAngle = new RadianBearingImpl(gybeAngle);

            double scaledGybeAngle = Math.PI-(Math.PI-gybeAngle)*this.scaleBearing;
            estGybeAngleRight = windBearing.add(new RadianBearingImpl(+scaledGybeAngle));
            estGybeAngleLeft = windBearing.add(new RadianBearingImpl(-scaledGybeAngle));

            return new Bearing[] { estGybeAngleRight, estGybeAngleLeft };

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
                        estGybeAngleRight = b;
                    }
                } else if (getSpeedAtBearing(b).getKnots()
                        * Math.cos(b.getDifferenceTo(getTargetDirection().reverse()).getRadians()) > maxSpeedLeft) {
                    maxSpeedLeft = getSpeedAtBearing(b).getKnots()
                            * Math.cos(b.getDifferenceTo(getTargetDirection().reverse()).getRadians());
                    estGybeAngleLeft = b;
                }
            }

            return new Bearing[] { estGybeAngleLeft, estGybeAngleRight };
        }
    }

    // a Bearing Comparator useful in the creation of sorted sets of Bearing
    public static Comparator<Bearing> bearingComparator = new Comparator<Bearing>() {

        @Override
        public int compare(Bearing o1, Bearing o2) {
            Double d1 = o1.getDegrees();
            if (d1 < 0) {
                d1 = 360 + d1;
            }
            Double d2 = o2.getDegrees();
            if (d2 < 0) {
                d2 = 360 + d2;
            }
            return d1.compareTo(d2);
        }

    };

    @Override
    public long getTurnLoss() {
        // TODO Auto-generated method stub
        return 4000;
    }

    // TO BE REVIEWED
    // not sure I use the right terms and conventions
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

}
