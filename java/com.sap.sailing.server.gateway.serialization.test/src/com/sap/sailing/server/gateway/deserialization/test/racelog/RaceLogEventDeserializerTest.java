package com.sap.sailing.server.gateway.deserialization.test.racelog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCCertificateAssignmentEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCLegDataEventImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogExcludeWindSourcesEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEndOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogExcludeWindSourcesEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogTagEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogUseCompetitorsFromRaceLogEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.orc.ORCCertificatesImporter;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogEndOfTrackingEventDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogEventDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogExcludeWindSourceEventDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogFlagEventDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogORCCertificateAssignmentEventDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogORCImpliedWindSourceEventDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogORCLegDataEventDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogResultsAreOfficialEventDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogTagEventDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogUseCompetitorsFromRaceLogEventDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogEventSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.shared.json.JsonDeserializer;

public class RaceLogEventDeserializerTest {
    
    JsonDeserializer<DynamicCompetitor> competitorDeserializer = CompetitorJsonDeserializer.create(DomainFactory.INSTANCE);
    
    protected RaceLogFlagEventDeserializer mockitoRaceLogFlagEventDeserializer = Mockito.spy(new RaceLogFlagEventDeserializer(competitorDeserializer));
    protected RaceLogUseCompetitorsFromRaceLogEventDeserializer mockitoRaceLogUseCompetitorsFromRaceLogEventDeserializer = Mockito.spy(new RaceLogUseCompetitorsFromRaceLogEventDeserializer(competitorDeserializer));
    protected RaceLogEndOfTrackingEventDeserializer mockitoRaceLogEndOfTrackingEventDeserializer = Mockito.spy(new RaceLogEndOfTrackingEventDeserializer(competitorDeserializer));
    protected RaceLogTagEventDeserializer mockitoRaceLogTagEventDeserializer = Mockito.spy(new RaceLogTagEventDeserializer(competitorDeserializer));
    
    public class InnerRaceLogEventDeserializer extends RaceLogEventDeserializer{
        public InnerRaceLogEventDeserializer() {
            super(mockitoRaceLogFlagEventDeserializer, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    mockitoRaceLogUseCompetitorsFromRaceLogEventDeserializer,
                    mockitoRaceLogEndOfTrackingEventDeserializer, mockitoRaceLogTagEventDeserializer,
                    new RaceLogORCLegDataEventDeserializer(competitorDeserializer),
                    null, new RaceLogORCCertificateAssignmentEventDeserializer(competitorDeserializer),
                    new RaceLogORCImpliedWindSourceEventDeserializer(competitorDeserializer),
                    new RaceLogExcludeWindSourceEventDeserializer(competitorDeserializer),
                    new RaceLogResultsAreOfficialEventDeserializer(competitorDeserializer));
        }
    }
    
    //Used for creating RaceLogEvents which get serialized, then deserialized and afterwards compared
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
    private TimePoint timePoint = TimePoint.BeginningOfTime;
    private TimePoint timePoint2 = TimePoint.EndOfTime;
    private InnerRaceLogEventDeserializer deserializer = new InnerRaceLogEventDeserializer();
    
    @Test
    public void testSerializationAndDeserializationForRaceLogFlagEvent() throws Exception{
        RaceLogFlagEventImpl originalEvent = new RaceLogFlagEventImpl(timePoint, author, 0, Flags.BLACK, Flags.ESSONE, false);
        RaceLogEventSerializer serializer = (RaceLogEventSerializer) RaceLogEventSerializer.create(CompetitorJsonSerializer.create());
        JSONObject object = serializer.serialize(originalEvent); 
        RaceLogEvent raceLogEvent = deserializer.deserialize(object);
        //assert correct deserializer was used
        Mockito.verify(mockitoRaceLogFlagEventDeserializer).deserialize(object);
        RaceLogFlagEventImpl newEvent = (RaceLogFlagEventImpl) raceLogEvent;
        //assert raceLogEvent has correct values
        assertEquals(originalEvent.getTimePoint().toString(), newEvent.getTimePoint().toString());
        assertEquals(originalEvent.getAuthor().toString(), newEvent.getAuthor().toString());
        assertEquals(originalEvent.getPassId(), newEvent.getPassId());
        assertEquals(originalEvent.getClass(), newEvent.getClass());
        assertEquals(originalEvent.getId(), newEvent.getId());
        assertEquals(originalEvent.getShortInfo(), newEvent.getShortInfo());
        assertEquals(originalEvent.getLowerFlag(), newEvent.getLowerFlag());
        assertEquals(originalEvent.getUpperFlag(), newEvent.getUpperFlag());
        assertEquals(originalEvent.getCreatedAt(), newEvent.getCreatedAt());
    }
    
    @Test
    public void testSerializationAndDeserializationForRaceLogORCLegDataEvent() throws Exception{
        final RaceLogORCLegDataEventImpl originalEvent = new RaceLogORCLegDataEventImpl(timePoint, timePoint, author,
                UUID.randomUUID(), 0, /* leg number */ 2, new DegreeBearingImpl(123), new MeterDistance(2345),
                ORCPerformanceCurveLegTypes.TWA);
        RaceLogEventSerializer serializer = (RaceLogEventSerializer) RaceLogEventSerializer.create(CompetitorJsonSerializer.create());
        JSONObject object = serializer.serialize(originalEvent); 
        RaceLogEvent raceLogEvent = deserializer.deserialize(object);
        RaceLogORCLegDataEventImpl newEvent = (RaceLogORCLegDataEventImpl) raceLogEvent;
        //assert raceLogEvent has correct values
        assertEquals(originalEvent.getTimePoint().toString(), newEvent.getTimePoint().toString());
        assertEquals(originalEvent.getAuthor().toString(), newEvent.getAuthor().toString());
        assertEquals(originalEvent.getPassId(), newEvent.getPassId());
        assertEquals(originalEvent.getClass(), newEvent.getClass());
        assertEquals(originalEvent.getId(), newEvent.getId());
        assertEquals(originalEvent.getShortInfo(), newEvent.getShortInfo());
        assertEquals(originalEvent.getOneBasedLegNumber(), newEvent.getOneBasedLegNumber());
        assertEquals(originalEvent.getTwa(), newEvent.getTwa());
        assertEquals(originalEvent.getType(), newEvent.getType());
        assertEquals(originalEvent.getLength(), newEvent.getLength());
    }
    
    @Test
    public void testSerializationAndDeserializationForRaceLogORCCertificateAssignmentEvent() throws IOException, ParseException {
        final Boat boat = new BoatImpl(UUID.randomUUID(), "Testboot", new BoatClassImpl(BoatClassMasterdata.ORC), "GER 007");
        final ORCCertificate certificate = ORCCertificatesImporter.INSTANCE.read(new StringReader("NATCERTN.FILE_ID SAILNUMB    NAME                    TYPE              BUILDER           DESIGNER          YEAR CLUB                                OWNER                               ADRS1                               ADRS2                               C_Type   D CREW DD_MM_yyYY HH:MM:SS  LOA   IMSL   DRAFT  BMAX   DSPL  INDEX    DA   GPH    TMF    ILCGA  PLT-O PLD-O   WL6    WL8    WL10   WL12   WL14   WL16   WL20   OL6    OL8    OL10   OL12   OL14   OL16   OL20   CR6    CR8    CR10   CR12   CR14   CR16   CR20   NSP6   NSP8   NSP10  NSP12  NSP14  NSP16  NSP20   OC6    OC8    OC10   OC12   OC14   OC16   OC20  UA6   UA8   UA10  UA12  UA14  UA16  UA20  DA6   DA8  DA10  DA12  DA14  DA16  DA20   UP6    UP8    UP10   UP12   UP14   UP16   UP20   R526   R528   R5210  R5212  R5214  R5216  R5220  R606   R608   R6010  R6012  R6014  R6016  R6020  R756   R758   R7510  R7512  R7514  R7516  R7520  R906   R908   R9010  R9012  R9014  R9016  R9020  R1106  R1108  R11010 R11012 R11014 R11016 R11020 R1206  R1208  R12010 R12012 R12014 R12016 R12020 R1356  R1358 R13510 R13512 R13514 R13516 R13520  R1506  R1508 R15010 R15012 R15014 R15016 R15020    D6     D8     D10    D12    D14    D16    D20 OTNLOW  OTNMED  OTNHIG  ITNLOW  ITNMED  ITNHIG DH_TOD DH_TOT  PLT-I PLD-I TMF-OF  PLT2H PLD2H    OSN ReferenceNo    CDL    DSPS     WSS    MAIN   GENOA     SYM    ASYM OTDLOW OTDMED OTDHIG ITDLOW ITDMED ITDHIG NS_TOD NS_TOT GNSTOD GNSTOT GDHTOD GDHTOT\r\n" + 
                "AUT017/19noi     AUT         NOI                     Sunbeam 40.1      Sunbeam Yachts    J&J               2014 Austria                                                                                                                                         CLUB     C  789 12 03 2019 15:54:33 11.990 10.960 2.067  3.99   8500. 122.3   0.33  655.6 0.9532  708.2  0.000   0.0  1148.6  906.3  768.6  681.6  629.2  597.0  570.9 1081.1  863.0  740.1  664.4  621.3  598.6  579.4  930.4  739.6  634.3  571.5  532.6  508.0  480.5  930.4  739.6  634.3  571.5  532.6  508.0  480.5 1153.3  850.5  688.3  589.6  538.7  504.5  456.8  44.5  42.6  42.1  41.2  40.5  40.0  40.5 154.1 160.1 167.4 171.2 174.1 175.7 178.3 1101.4  902.4  790.1  719.8  688.5  678.5  681.3  706.8  592.3  524.6  490.4  475.6  470.2  468.3  661.7  559.6  502.6  476.1  462.8  456.1  452.9  630.6  534.7  487.5  465.4  451.0  440.1  428.5  638.5  537.7  487.2  464.0  448.6  433.5  410.8  711.0  581.4  506.6  472.4  453.4  437.2  404.7  754.8  616.9  533.9  486.1  462.0  444.9  411.8  883.0  695.9  592.5  522.9  482.7  460.6  427.5 1036.8  794.8  661.5  576.5  516.0  480.1  442.8 1195.8  910.2  747.1  643.3  569.7  515.5  460.4 0.8624  1.1782  1.3607  0.6570  0.9425  1.1340    0.0 0.0000  0.000   0.0 0.9470  0.000   0.0  633.6 AUT00002508  9.784    9570    36.0    45.7    39.3     0.0     0.0  782.7  572.9  496.1 1027.4  716.2  595.2  633.6 0.9470  655.6 0.9152    0.0 0.0000")).getCertificates().iterator().next();
        final RaceLogORCCertificateAssignmentEventImpl originalEvent = new RaceLogORCCertificateAssignmentEventImpl(timePoint, timePoint, author, UUID.randomUUID(), 0, certificate, boat);
        RaceLogEventSerializer serializer = (RaceLogEventSerializer) RaceLogEventSerializer.create(CompetitorJsonSerializer.create());
        JSONObject object = serializer.serialize(originalEvent);
        RaceLogEvent raceLogEvent = deserializer.deserialize(object);
        RaceLogORCCertificateAssignmentEventImpl newEvent = (RaceLogORCCertificateAssignmentEventImpl) raceLogEvent;
        //assert raceLogEvent has correct values
        assertEquals(originalEvent.getTimePoint().toString(), newEvent.getTimePoint().toString());
        assertEquals(originalEvent.getAuthor().toString(), newEvent.getAuthor().toString());
        assertEquals(originalEvent.getPassId(), newEvent.getPassId());
        assertEquals(originalEvent.getClass(), newEvent.getClass());
        assertEquals(originalEvent.getId(), newEvent.getId());
        assertEquals(originalEvent.getBoatId(), newEvent.getBoatId());
        assertEquals(originalEvent.getCertificate().getGPH().asSeconds(), newEvent.getCertificate().getGPH().asSeconds(), 0.00001);
    }
    
    @Test
    public void testSerializationAndDeserializationForRaceLogEndOfTrackingEvent() throws Exception{
        RaceLogEndOfTrackingEventImpl originalEvent = new  RaceLogEndOfTrackingEventImpl(timePoint, timePoint2, author, UUID.randomUUID(), 3);
        RaceLogEventSerializer serializer = (RaceLogEventSerializer) RaceLogEventSerializer.create(CompetitorJsonSerializer.create());
        JSONObject object = serializer.serialize(originalEvent); 
        RaceLogEvent raceLogEvent = deserializer.deserialize(object);
        //assert correct deserializer was used
        Mockito.verify(mockitoRaceLogEndOfTrackingEventDeserializer).deserialize(object);
        RaceLogEndOfTrackingEventImpl newEvent = (RaceLogEndOfTrackingEventImpl) raceLogEvent;
        //assert raceLogEvent has correct value
        assertEquals(originalEvent.getTimePoint().toString(), newEvent.getTimePoint().toString());
        assertEquals(originalEvent.getAuthor().toString(), newEvent.getAuthor().toString());
        assertEquals(originalEvent.getPassId(), newEvent.getPassId());
        assertEquals(originalEvent.getClass(), newEvent.getClass());
        assertEquals(originalEvent.getId(), newEvent.getId());
        assertEquals(originalEvent.getShortInfo(), newEvent.getShortInfo());
        assertEquals(originalEvent.getCreatedAt(), newEvent.getCreatedAt());
    }   
    
    @Test
    public void testSerializationAndDeserializationForRaceLogUseCompetitorsFromRaceLogEvent() throws Exception{
        RaceLogUseCompetitorsFromRaceLogEventImpl originalEvent = new  RaceLogUseCompetitorsFromRaceLogEventImpl(timePoint, author, timePoint2, UUID.randomUUID(), 3);
        RaceLogEventSerializer serializer = (RaceLogEventSerializer) RaceLogEventSerializer.create(CompetitorJsonSerializer.create());
        JSONObject object = serializer.serialize(originalEvent); 
        RaceLogEvent raceLogEvent = deserializer.deserialize(object);
        //assert correct deserializer was used
        Mockito.verify(mockitoRaceLogUseCompetitorsFromRaceLogEventDeserializer).deserialize(object);
        RaceLogUseCompetitorsFromRaceLogEventImpl newEvent = (RaceLogUseCompetitorsFromRaceLogEventImpl) raceLogEvent;
        //assert raceLogEvent has correct value
        assertEquals(originalEvent.getTimePoint().toString(), newEvent.getTimePoint().toString());
        assertEquals(originalEvent.getAuthor().toString(), newEvent.getAuthor().toString());
        assertEquals(originalEvent.getPassId(), newEvent.getPassId());
        assertEquals(originalEvent.getClass(), newEvent.getClass());
        assertEquals(originalEvent.getId(), newEvent.getId());
        assertEquals(originalEvent.getShortInfo(), newEvent.getShortInfo());
        assertEquals(originalEvent.getCreatedAt(), newEvent.getCreatedAt());
    }  
    
    @Test
    public void testSerializationAndDeserializationForRaceLogTagEvent() throws Exception {
        RaceLogTagEventImpl originalEvent = new RaceLogTagEventImpl("tag", "comment", "some hidden info", "a", "b", timePoint, timePoint2, author, UUID.randomUUID(), 3);
        RaceLogEventSerializer serializer = (RaceLogEventSerializer) RaceLogEventSerializer.create(CompetitorJsonSerializer.create());
        JSONObject object = serializer.serialize(originalEvent); 
        RaceLogEvent raceLogEvent = deserializer.deserialize(object);
        // assert correct deserializer was used
        Mockito.verify(mockitoRaceLogTagEventDeserializer).deserialize(object);
        RaceLogTagEventImpl newEvent = (RaceLogTagEventImpl) raceLogEvent;
        // assert raceLogEvent has correct value
        assertEquals(originalEvent.getTimePoint().toString(), newEvent.getTimePoint().toString());
        assertEquals(originalEvent.getAuthor().toString(), newEvent.getAuthor().toString());
        assertEquals(originalEvent.getPassId(), newEvent.getPassId());
        assertEquals(originalEvent.getClass(), newEvent.getClass());
        assertEquals(originalEvent.getId(), newEvent.getId());
        assertEquals(originalEvent.getShortInfo(), newEvent.getShortInfo());
        assertEquals(originalEvent.getCreatedAt(), newEvent.getCreatedAt());
        assertEquals(originalEvent.getTag(), newEvent.getTag());
        assertEquals(originalEvent.getComment(), newEvent.getComment());
        assertEquals(originalEvent.getHiddenInfo(), newEvent.getHiddenInfo());
        assertEquals(originalEvent.getImageURL(), newEvent.getImageURL());
        assertEquals(originalEvent.getResizedImageURL(), newEvent.getResizedImageURL());
        assertEquals(originalEvent.getUsername(), newEvent.getUsername());
    }
    
    @Test
    public void testSeralizationAndDeserializationForExcludeWindSourceEventWithId() throws Exception {
        RaceLogExcludeWindSourcesEventImpl originalEvent = new RaceLogExcludeWindSourcesEventImpl(timePoint, timePoint2,
                author, UUID.randomUUID(), 3, Collections.singleton(new WindSourceWithAdditionalID(WindSourceType.WEB, "123")));
        RaceLogEventSerializer serializer = (RaceLogEventSerializer) RaceLogEventSerializer.create(CompetitorJsonSerializer.create());
        JSONObject object = serializer.serialize(originalEvent);
        RaceLogEvent raceLogEvent = deserializer.deserialize(object);
        RaceLogExcludeWindSourcesEvent newEvent = (RaceLogExcludeWindSourcesEvent) raceLogEvent;
        // assert raceLogEvent has correct value
        assertEquals(originalEvent.getTimePoint().toString(), newEvent.getTimePoint().toString());
        assertEquals(1, Util.size(originalEvent.getWindSourcesToExclude()));
        assertEquals(originalEvent.getWindSourcesToExclude().iterator().next().getType(), newEvent.getWindSourcesToExclude().iterator().next().getType());
        assertEquals(originalEvent.getWindSourcesToExclude().iterator().next().getId(), newEvent.getWindSourcesToExclude().iterator().next().getId());
    }
    
    @Test
    public void testSeralizationAndDeserializationForExcludeWindSourceEventWithoutId() throws Exception {
        final Set<WindSource> originalWindSourcesToExclude = new HashSet<>(Arrays.asList(new WindSourceImpl(WindSourceType.EXPEDITION), new WindSourceWithAdditionalID(WindSourceType.WEB, "123")));
        RaceLogExcludeWindSourcesEventImpl originalEvent = new RaceLogExcludeWindSourcesEventImpl(timePoint, timePoint2,
                author, UUID.randomUUID(), 3, originalWindSourcesToExclude);
        RaceLogEventSerializer serializer = (RaceLogEventSerializer) RaceLogEventSerializer.create(CompetitorJsonSerializer.create());
        JSONObject object = serializer.serialize(originalEvent);
        RaceLogEvent raceLogEvent = deserializer.deserialize(object);
        RaceLogExcludeWindSourcesEvent newEvent = (RaceLogExcludeWindSourcesEvent) raceLogEvent;
        final Set<WindSource> deserializedWindSourcesToExclude = new HashSet<>();
        Util.addAll(newEvent.getWindSourcesToExclude(), deserializedWindSourcesToExclude);
        // assert raceLogEvent has correct value
        assertEquals(originalEvent.getTimePoint().toString(), newEvent.getTimePoint().toString());
        assertEquals(2, Util.size(originalEvent.getWindSourcesToExclude()));
        assertEquals(originalWindSourcesToExclude, deserializedWindSourcesToExclude);
    }
}