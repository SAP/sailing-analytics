package com.sap.sailing.domain.queclinkadapter;

/**
 * From the documentation:
 * <pre>
"I/O Status": A hexadecimal value to indicate the I/O status. If Bit 0 of the parameter
"Report ID / Append Mask" in the report is 1, this field will be present in the report message
+RESP:GTFRI. If Bit 0 of the parameter "Report ID / Append Mask" is 0, there is no "I/O
Status" field. Below is the detailed information of "I/O Status".
Bit I/O Status
Bit 0 (0001) Input 0 status
Bit 1 (0002) Ignition on/off status
Bit 2 (0004) Reserved
Bit 3 (0008) Reserved
Bit 4 (0010) Reserved
Bit 5 (0020) Reserved
Bit 6 (0040) Reserved
Bit 7 (0080) Reserved
Bit 8 (0100) Reserved
Bit 9 (0200) Reserved
Bit 10 (0400) Reserved
Bit 11 (0800) Reserved
Bit 12 (1000) Reserved
Bit 13 (2000) Reserved
Bit 14 (4000) Reserved
Bit 15 (8000) Reserved
 * </pre>
 * 
 * @author Axel Uhl
 *
 */
public class IOStatus {
    private final short bitMask;
    private final short INPUT_STATUS    = 1<<0;
    private final short IGNITION_STATUS = 1<<1;
    
    public IOStatus(String hexRepresentation) {
        bitMask = Short.parseShort(hexRepresentation, 16);
    }
    
    public boolean isInputStatus() {
        return (bitMask & INPUT_STATUS) != 0;
    }

    public boolean isIgnitionStatus() {
        return (bitMask & IGNITION_STATUS) != 0;
    }
}
