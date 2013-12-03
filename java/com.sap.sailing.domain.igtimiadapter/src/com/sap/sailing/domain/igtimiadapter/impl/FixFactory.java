package com.sap.sailing.domain.igtimiadapter.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.datatypes.Type;

public class FixFactory {
    private static final Logger logger = Logger.getLogger(FixFactory.class.getName());
    
    public Iterable<Fix> createFixes(JSONObject sensorsJson) {
        List<Fix> result = new ArrayList<>();
        for (Entry<Object, Object> e : sensorsJson.entrySet()) {
            String deviceSerialNumber = (String) e.getKey();
            JSONObject typesJson = (JSONObject) e.getValue();
            for (Entry<Object, Object> fixTypeAndFixesJson : typesJson.entrySet()) {
                final String[] fixTypeAndOptionalColonSeparatedSensorsSubId = ((String) fixTypeAndFixesJson.getKey()).split(":");
                int fixType = Integer.valueOf(fixTypeAndOptionalColonSeparatedSensorsSubId[0]);
                JSONObject fixesJson = (JSONObject) fixTypeAndFixesJson.getValue();
                JSONArray timePointsMillis = (JSONArray) fixesJson.get("t");
                int fixIndex = 0;
                for (Object timePointMillis : timePointsMillis) {
                    TimePoint timePoint = new MillisecondsTimePoint(((Number) timePointMillis).longValue());
                    Map<Integer, Object> valuesPerSubindex = new HashMap<>();
                    int i=1;
                    JSONArray values;
                    while ((values=(JSONArray) fixesJson.get(""+i)) != null) {
                        valuesPerSubindex.put(i, (Number) values.get(fixIndex));
                        i++;
                    }
                    Sensor sensor = new SensorImpl(deviceSerialNumber, fixTypeAndOptionalColonSeparatedSensorsSubId.length<2?null:fixTypeAndOptionalColonSeparatedSensorsSubId[1]);
                    Fix fix = createFix(sensor, Type.valueOf(fixType), timePoint, valuesPerSubindex);
                    result.add(fix);
                    fixIndex++;
                }
            }
        }
        return result;
    }

    private Fix createFix(Sensor sensor, Type fixType, TimePoint timePoint, Map<Integer, Object> valuesPerSubindex) {
        try {
            Constructor<? extends Fix> constructor = fixType.getFixClass().getConstructor(TimePoint.class, Sensor.class, Map.class);
            Fix fix = constructor.newInstance(timePoint, sensor, valuesPerSubindex);
            return fix;
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            logger.log(Level.SEVERE, "Internal error trying to find fix constructor for fix type "+fixType+" with class "+fixType.getFixClass());
            throw new RuntimeException(e);
        }
    }
}
