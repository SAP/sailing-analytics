package com.sap.sailing.domain.queclinkadapter;

import java.util.HashMap;
import java.util.Map;

import com.sap.sse.common.Util.Pair;

/**
 * A Queclink GL 300 message type.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public enum MessageType {
    // commands to terminal with acknowledgement:
    BSI(new Pair<>(Direction.AT, BSICommand.FACTORY), new Pair<>(Direction.ACK, BSIAcknowledgement.FACTORY)),
    CFG(new Pair<>(Direction.AT, CFGCommand.FACTORY), new Pair<>(Direction.ACK, CFGAcknowledgement.FACTORY)),
    CMD(new Pair<>(Direction.AT, CMDCommand.FACTORY), new Pair<>(Direction.ACK, CMDAcknowledgement.FACTORY)),
    DAT(new Pair<>(Direction.AT, DATCommand.FACTORY), new Pair<>(Direction.ACK, DATAcknowledgement.FACTORY), new Pair<>(Direction.RESP, DATReport.FACTORY)),
    DIS(new Pair<>(Direction.AT, DISCommand.FACTORY), new Pair<>(Direction.ACK, DISAcknowledgement.FACTORY)),
    DOG(new Pair<>(Direction.AT, DOGCommand.FACTORY), new Pair<>(Direction.ACK, DOGAcknowledgement.FACTORY), new Pair<>(Direction.RESP, DOGReport.FACTORY)),
    FKS(new Pair<>(Direction.AT, FKSCommand.FACTORY), new Pair<>(Direction.ACK, FKSAcknowledgement.FACTORY)),
    FRI(new Pair<>(Direction.AT, FRICommand.FACTORY), new Pair<>(Direction.ACK, FRIAcknowledgement.FACTORY), new Pair<>(Direction.RESP, FRIReport.FACTORY)),
    GAM(new Pair<>(Direction.AT, GAMCommand.FACTORY), new Pair<>(Direction.ACK, GAMAcknowledgement.FACTORY)),
    GEO(new Pair<>(Direction.AT, GEOCommand.FACTORY), new Pair<>(Direction.ACK, GEOAcknowledgement.FACTORY), new Pair<>(Direction.RESP, GEOReport.FACTORY)),
    GLM(new Pair<>(Direction.AT, GLMCommand.FACTORY), new Pair<>(Direction.ACK, GLMAcknowledgement.FACTORY)),
    HBM(new Pair<>(Direction.AT, HBMCommand.FACTORY), new Pair<>(Direction.ACK, HBMAcknowledgement.FACTORY), new Pair<>(Direction.RESP, HBMReport.FACTORY)),
    JDC(new Pair<>(Direction.AT, JDCCommand.FACTORY), new Pair<>(Direction.ACK, JDCAcknowledgement.FACTORY)),
    NMD(new Pair<>(Direction.AT, NMDCommand.FACTORY), new Pair<>(Direction.ACK, NMDAcknowledgement.FACTORY)),
    NTS(new Pair<>(Direction.AT, NTSCommand.FACTORY), new Pair<>(Direction.ACK, NTSAcknowledgement.FACTORY)),
    OWN(new Pair<>(Direction.AT, OWNCommand.FACTORY), new Pair<>(Direction.ACK, OWNAcknowledgement.FACTORY)),
    PDS(new Pair<>(Direction.AT, PDSCommand.FACTORY), new Pair<>(Direction.ACK, PDSAcknowledgement.FACTORY)),
    PIN(new Pair<>(Direction.AT, PINCommand.FACTORY), new Pair<>(Direction.ACK, PINAcknowledgement.FACTORY)),
    QSS(new Pair<>(Direction.AT, QSSCommand.FACTORY), new Pair<>(Direction.ACK, QSSAcknowledgement.FACTORY)),
    RMD(new Pair<>(Direction.AT, RMDCommand.FACTORY), new Pair<>(Direction.ACK, RMDAcknowledgement.FACTORY), new Pair<>(Direction.RESP, RMDReport.FACTORY)),
    RTO(new Pair<>(Direction.AT, RTOCommand.FACTORY), new Pair<>(Direction.ACK, RTOAcknowledgement.FACTORY)),
    RVC(new Pair<>(Direction.AT, RVCCommand.FACTORY), new Pair<>(Direction.ACK, RVCAcknowledgement.FACTORY)),
    SPD(new Pair<>(Direction.AT, SPDCommand.FACTORY), new Pair<>(Direction.ACK, SPDAcknowledgement.FACTORY), new Pair<>(Direction.RESP, SPDReport.FACTORY)),
    SRI(new Pair<>(Direction.AT, SRICommand.FACTORY), new Pair<>(Direction.ACK, SRIAcknowledgement.FACTORY)),
    TEM(new Pair<>(Direction.AT, TEMCommand.FACTORY), new Pair<>(Direction.ACK, TEMAcknowledgement.FACTORY), new Pair<>(Direction.RESP, TEMReport.FACTORY)),
    TMA(new Pair<>(Direction.AT, TMACommand.FACTORY), new Pair<>(Direction.ACK, TMAAcknowledgement.FACTORY)),
    UPC(new Pair<>(Direction.AT, UPCCommand.FACTORY), new Pair<>(Direction.ACK, UPCAcknowledgement.FACTORY), new Pair<>(Direction.RESP, UPCReport.FACTORY)),
    WLT(new Pair<>(Direction.AT, WLTCommand.FACTORY), new Pair<>(Direction.ACK, WLTAcknowledgement.FACTORY)),
    // position-related reports:
    GCR(new Pair<>(Direction.RESP, GCRReport.FACTORY)),
    IGL(new Pair<>(Direction.RESP, IGLReport.FACTORY)),
    LBC(new Pair<>(Direction.RESP, LBCReport.FACTORY)),
    NMR(new Pair<>(Direction.RESP, NMRReport.FACTORY)),
    PNL(new Pair<>(Direction.RESP, PNLReport.FACTORY)),
    RTL(new Pair<>(Direction.RESP, RTLReport.FACTORY)),
    SOS(new Pair<>(Direction.RESP, SOSReport.FACTORY)),
    // device information report:
    INF(new Pair<>(Direction.RESP, INFReport.FACTORY)),
    // reports for querying:
    ALL(new Pair<>(Direction.RESP, ALLReport.FACTORY)),
    ALM(new Pair<>(Direction.RESP, ALMReport.FACTORY)),
    ALS(new Pair<>(Direction.RESP, ALSReport.FACTORY)),
    BAT(new Pair<>(Direction.RESP, BATReport.FACTORY)),
    BPL(new Pair<>(Direction.RESP, BPLReport.FACTORY)),
    BTC(new Pair<>(Direction.RESP, BTCReport.FACTORY)),
    CID(new Pair<>(Direction.RESP, CIDReport.FACTORY)),
    CSQ(new Pair<>(Direction.RESP, CSQReport.FACTORY)),
    EPF(new Pair<>(Direction.RESP, EPFReport.FACTORY)),
    EPN(new Pair<>(Direction.RESP, EPNReport.FACTORY)),
    EUC(new Pair<>(Direction.RESP, EUCReport.FACTORY)),
    GPS(new Pair<>(Direction.RESP, GPSReport.FACTORY)),
    GSM(new Pair<>(Direction.RESP, GSMReport.FACTORY)),
    IGF(new Pair<>(Direction.RESP, IGFReport.FACTORY)),
    IGN(new Pair<>(Direction.RESP, IGNReport.FACTORY)),
    JDR(new Pair<>(Direction.RESP, JDRReport.FACTORY)),
    JDS(new Pair<>(Direction.RESP, JDSReport.FACTORY)),
    PDP(new Pair<>(Direction.RESP, PDPReport.FACTORY)),
    PFA(new Pair<>(Direction.RESP, PFAReport.FACTORY)),
    PNA(new Pair<>(Direction.RESP, PNAReport.FACTORY)),
    STC(new Pair<>(Direction.RESP, STCReport.FACTORY)),
    STT(new Pair<>(Direction.RESP, STTReport.FACTORY)),
    SWG(new Pair<>(Direction.RESP, SWGReport.FACTORY)),
    TMZ(new Pair<>(Direction.RESP, TMZReport.FACTORY)),
    USW(new Pair<>(Direction.RESP, USWReport.FACTORY)),
    VER(new Pair<>(Direction.RESP, VERReport.FACTORY)),
    // heartbeat
    HBD(new Pair<>(Direction.ACK, HBDAcknowledgement.FACTORY), new Pair<>(Direction.SACK, HBDServerAcknowledgement.FACTORY))
    // there is a special "SACK" message for general server acknowledgement which differs from all other message formats
    // in that it doesn't contain the actual message type after the colon (":") but instead only has the count number as
    // a four-character HEX value after the colon. We don't provide a message type enum literal for this type of message
    // here.
    ;
    
    public enum Direction {
        /**
         * command sent to the "terminal" (tracking device)
         */
        AT,
        
        /**
         * message sent from the "terminal" (tracking device) to the server; in most cases an acknowledgement
         * of an {@link #AT} command, but in some cases a spontaneous emission, such as for a heart beat message;
         * see {@link MessageType#HBD}.
         */
        ACK,
        
        /**
         * response sent by the "terminal" (tracking device) to the server; for example, a single response to a request,
         * or a repetitive response sent by the device on a regular basis, such as position report.
         */
        RESP,
        
        /**
         * a server acknowledgement, sent from the server to the "terminal" (tracking device); without a message type
         * identifier, this 
         */
        SACK,
        
        /**
         * Basically the same as {@link #RESP}, but these message come from the device buffer after not having been
         * able to send them out at the time they were recorded.
         */
        BUFF;
        
        public static Direction fromMessageStart(String messageStart) {
            return valueOf(messageStart.replace("+", "").replace(":", ""));
        }
    }

    private static Map<Pair<Direction, MessageType>, MessageFactory> messageFactories;
    
    private synchronized static Map<Pair<Direction, MessageType>, MessageFactory> getMessageFactories() {
        if (messageFactories == null) {
            messageFactories = new HashMap<>();
        }
        return messageFactories;
    }
    
    static {
        // support special case: a server acknowledgement with no message type:
        getMessageFactories().put(new Pair<>(Direction.SACK, null), ServerAcknowledgement.FACTORY);
    }
    
    @SafeVarargs
    private MessageType(Pair<Direction, MessageFactory>... factoryForDirection) {
        for (final Pair<Direction, MessageFactory> directionAndFactory : factoryForDirection) {
//            assert directionAndFactory.getB() != null; // TODO uncomment when all message types have their factories
            putMessageFactory(directionAndFactory.getA(), directionAndFactory.getB());
        }
    }
    
    private void putMessageFactory(Direction direction, MessageFactory factory) {
        getMessageFactories().put(new Pair<>(direction, this), factory);
        if (direction == Direction.RESP) {
            putMessageFactory(Direction.BUFF, factory);
        }
    }
    
    public static MessageFactory getMessageFactory(Direction direction, MessageType messageType) {
        return getMessageFactories().get(new Pair<>(direction, messageType));
    }
}
