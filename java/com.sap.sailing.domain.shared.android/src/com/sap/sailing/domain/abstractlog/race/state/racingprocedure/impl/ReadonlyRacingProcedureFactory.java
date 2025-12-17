package com.sap.sailing.domain.abstractlog.race.state.racingprocedure.impl;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.impl.NoAddingRaceLogWrapper;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedureFactory;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ReadonlyRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.basic.impl.BasicRacingProcedureImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ess.impl.ESSRacingProcedureImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.gate.impl.GateStartRacingProcedureImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.league.impl.LeagueRacingProcedureImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.impl.MediumSWCRacingProcedureImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.impl.RRS26RacingProcedureImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.impl.RRS26ThreeMinutesRacingProcedureImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.impl.SWCRacingProcedureImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.impl.ShortSWCRacingProcedureImpl;
import com.sap.sailing.domain.base.configuration.ConfigurationLoader;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;

/**
 * Factory for creating {@link ReadOnlyRacingProcedure}s.
 * 
 * The {@link ReadonlyRacingProcedureFactory} uses a {@link ConfigurationLoader} to ensure that any newly created
 * {@link RacingProcedure} is passed a recent and immutable configuration.
 */
public class ReadonlyRacingProcedureFactory implements RacingProcedureFactory {

    protected final ConfigurationLoader<RegattaConfiguration> configuration;

    public ReadonlyRacingProcedureFactory(ConfigurationLoader<RegattaConfiguration> configuration) {
        this.configuration = configuration;
    }

    @Override
    public RegattaConfiguration getConfiguration() {
        return configuration.load();
    }

    protected ReadonlyRacingProcedure createProcedure(RacingProcedureType type, RaceLog raceLog,
            AbstractLogEventAuthor author, RaceLogResolver raceLogResolver) {
        RegattaConfiguration loadedConfiguration = configuration.load();
        switch (type) {
        case ESS:
            return new ESSRacingProcedureImpl(raceLog, author, loadedConfiguration.getESSConfiguration(), raceLogResolver);
        case GateStart:
            return new GateStartRacingProcedureImpl(raceLog, author,
                    loadedConfiguration.getGateStartConfiguration(), raceLogResolver);
        case RRS26:
            return new RRS26RacingProcedureImpl(raceLog, author, loadedConfiguration.getRRS26Configuration(), raceLogResolver);
        case RRS26_3MIN:
            return new RRS26ThreeMinutesRacingProcedureImpl(raceLog, author, loadedConfiguration.getRRS26Configuration(), raceLogResolver);
        case SWC:
            return new SWCRacingProcedureImpl(raceLog, author, loadedConfiguration.getSWCStartConfiguration(), raceLogResolver);
        case SWC_4MIN:
            return new ShortSWCRacingProcedureImpl(raceLog, author, loadedConfiguration.getSWCStartConfiguration(), raceLogResolver);
        case SWC_5MIN:
            return new MediumSWCRacingProcedureImpl(raceLog, author, loadedConfiguration.getSWCStartConfiguration(), raceLogResolver);
        case BASIC:
            return new BasicRacingProcedureImpl(raceLog, author, loadedConfiguration.getBasicConfiguration(), raceLogResolver);
        case LEAGUE:
            return new LeagueRacingProcedureImpl(raceLog, author, loadedConfiguration.getLeagueConfiguration(), raceLogResolver);
        default:
            throw new UnsupportedOperationException("Unknown racing procedure " + type.toString());
        }
    }

    @Override
    public ReadonlyRacingProcedure createRacingProcedure(RacingProcedureType type, RaceLog raceLog, RaceLogResolver raceLogResolver) {
        // Just a mock author since we will never add anything to the racelog
        AbstractLogEventAuthor author = new LogEventAuthorImpl("Illegal Author", 128);
        // Wrap the racelog to disable adding...
        RaceLog wrappedRaceLog = new NoAddingRaceLogWrapper(raceLog);
        return createProcedure(type, wrappedRaceLog, author, raceLogResolver);
    }

}
