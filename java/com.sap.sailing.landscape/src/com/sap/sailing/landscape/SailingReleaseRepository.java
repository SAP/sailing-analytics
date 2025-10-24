package com.sap.sailing.landscape;

import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.landscape.impl.ReleaseRepositoryImpl;

public interface SailingReleaseRepository extends ReleaseRepository {
    ReleaseRepository INSTANCE = new ReleaseRepositoryImpl("https://releases.sapsailing.com", /* master release name prefix */ "main");
}
