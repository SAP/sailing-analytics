package com.sap.sailing.landscape;

import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.landscape.impl.GithubReleasesRepository;

public interface SailingReleaseRepository extends ReleaseRepository {
    ReleaseRepository INSTANCE = new GithubReleasesRepository(
            "SAP",               // owner
            "sailing-analytics", // repo name
            "main");             // main release name prefix
}
