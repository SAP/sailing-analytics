# igtimi riot protocol
Telemetry protocol for igtimi devices based on the [protobuf](https://developers.google.com/protocol-buffers) serialization format.

Here goes a description shared by Igtimi on 2024-11-28:

```
            1 | pos         | {"description": "GNSS Position", "dimension": "longitude, latitude, altitude msl", "unit": "degree, degree, meter", "precision": "double, double, double, double", "notes": "Altitude optional"}
            2 | satq        | {"description": "GNSS Quality", "dimension": "-", "unit": "number", "precision": "integer"}
            3 | satc        | {"description": "GNSS SV Count", "dimension": "-", "unit": "number", "precision": "integer"}
            4 | len         | {"description": "Length", "dimensions": "length", "unit": "meter", "precision": "double"}
            5 | ang         | {"description": "Angle", "dimensions": "angle", "unit": "radian", "precision": "double"}
            6 | cog         | {"description": "Course Over Ground", "dimensions": "angle", "unit": "degree", "precision": "double"}
            7 | hdgm        | {"description": "Heading Magnetic", "dimensions": "angle", "unit": "degree", "precision": "double"}
            8 | hdg         | {"description": "Heading", "dimensions": "angle", "unit": "degree", "precision": "double"}
            9 | sog         | {"description": "Speed Over Ground", "dimensions": "speed", "unit": "Km/h", "precision": "double"}
           10 | stw         | {"description": "Speed Through Water", "dimensions": "speed", "unit": "Km/h", "precision": "double"}
           11 | awa         | {"description": "Apparent Wind Angle", "dimensions": "angle", "unit": "degree", "precision": "double"}
           12 | aws         | {"description": "Apparent Wind Speed", "dimensions": "speed", "unit": "Km/h", "precision": "double"}
           13 | hr          | {"description": "Heart Rate", "dimensions": "event time, HB count, HR", "unit": "milliseconds, number, cpm", "precision": "integer, integer, integer", "notes":"HR minimum"}
           14 | freq        | {"description": "Frequency", "dimensions": "frequency", "unit": "Hz/cpm", "precision": "double/double"}
           15 | temp        | {"description": "Temperature", "dimensions": "temperature", "unit": "&deg;C", "precision": "double"}
           16 | file        | {"description": "File", "dimensions": "filename, md5, content type, size", "unit": "text, text, text, bytes", "precision": "char(255), char(255), char(255), integer", "notes":"All fields required"}
           17 | ori         | {"description": "Orientation", "dimensions": "X,Y,Z,W", "unit": "rad, rad, rad, rad", "precision": "double, double, double, double", "notes": "Any of XYZ or XYZW"}
           18 | acc         | {"description": "Acceleration", "dimensions": "X,Y,Z", "unit": "[m/s/s, m/s/s, m/s/s] or [rad/s/s, rad/s/s, rad/s/s] or [g, g, g]", "precision": "double, double, double", "notes": "Any one field minimum"}
           19 | spd         | {"description": "Speed", "dimensions": "X,Y,Z", "unit": "[m/s, m/s, m/s] or [rad/s, rad/s, rad/s]", "precision": "double,double,double", "notes": "Any one field minimum"}
           20 | for         | {"description": "Force", "dimensions": "X,Y,Z", "unit": "N,N,N", "precision": "double, double, double", "notes": "Any one field minimum"}
           21 | torq        | {"description": "Torque", "dimensions": "torque", "unit": "Nm", "precision": "double"}
           22 | twd         | {"description": "True Wind Direction (grid north)", "dimensions": "Angle", "unit": "degree", "precision": "double"}
           23 | tws         | {"description": "True Wind Speed", "dimensions": "speed", "unit": "Km/h", "precision": "double"}
           24 | press       | {"description": "Pressure", "dimensions": "pressure", "unit": "Pa", "precision": "double"}
           25 | pwr         | {"description": "Power", "dimensions": "power", "unit": "W", "precision": "double"}
           26 | volt        | {"description": "Electrical Potential", "dimensions": "electrical potential", "unit": "V", "precision": "double"}
           27 | amp         | {"description": "Electrical Current", "dimensions": "electrical current", "unit": "A", "precision": "double"}
           28 | time        | {"description": "Time Interval", "dimensions": "time", "unit": "second", "precision": "double"}
           50 | num         | {"description": "Number", "dimensions": "-", "precision": "double"}
           51 | int         | {"description": "Integer", "dimensions": "-", "precision": "Integer"}
           52 | txt         | {"description": "Text", "dimensions": "-", "precision": "char"}
           53 | bool        | {"description": "Boolean", "dimensions": "-", "precision": "boolean"}
           54 | json        | {"description": "JSON", "dimensions": "-", "precision": "JSON"}
           55 | event       | {"description": "Event", "dimensions": "-", "precision": "char"}
           56 | log         | {"description": "Logging", "dimensions": "Log message, Priority", "precision": "char, integer"}
           57 | cmd         | {"description": "Command", "precision":"Text(c)"}
          100 | session_log | {"description": "Session Log","dimension":"-"}
```

It seems there once was a change regarding how battery status/level/strength was encoded, formerly using field 54 as a JSON document, now sending the state of charge as a percent number using message type 50. Igtimi / Riedel suggest that message type 50 (Number) is used only for battery charge.

```
166-                            if (message["54"]) { // Battery
167:                                    if(message["54"]["1"].length > 0 && ("battery_level" in message["54"]["1"][0]) ) {
168:                                                    // this.setBatteryStrength(message["54"]["1"][0].battery_level * 4);
169:                                                    var strength = message["54"]["1"][0].battery_level * 4;
170:                                                    this.$(".battery-status").attr("class", "battery-status state-" + strength);
171-                                    }
172-                            }
173-
174:                            if (message["50"]) { // Number (battery %)
175-                                    if(message["50"]["1"].length > 0) {
176-                                            var percentage = message["50"]["1"][0];
177-                        var strength = Math.round(percentage / 25);
178-
179:                                            this.$(".battery-status").attr("class", "battery-status state-" + strength);
180-                                    }
181-                            }
```