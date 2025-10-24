package com.sap.sailing.landscape.procedures;

import com.sap.sse.landscape.ProcessConfigurationVariable;

public enum SailingProcessConfigurationVariables implements ProcessConfigurationVariable {
    /**
     * The user data variable that sets the UDP port to listen on, particularly for messages coming
     * from the navigation tool "Expedition"
     */
    EXPEDITION_PORT,
    
    /**
     * The user data variable that sets the TCP port to listen on for Igtimi Riot (WindBot) connections; a typical port
     * would be 6000 which is the default for the WindBot devices. If not specified, the Riot server will listen on any
     * available, unused server port. Maps to the {@code igtimi.riot.port} property.
     */
    IGTIMI_RIOT_PORT,
    
    /**
     * Use this variable to override the default {@code https://wind.sapsailing.com} base URL for obtaining wind data
     * from Igtimi devices. Authentication to this service by default will assume shared security and therefore shared
     * user bases with shared access tokens. If this does not apply to your set-up, consider using
     * {@link #IGTIMI_BEARER_TOKEN} in addition. Maps to the {@code igtimi.base.url} property.
     */
    IGTIMI_BASE_URL,
    
    /**
     * Overrides the default authentication scheme for requests against the remote "Riot" service for Igtimi wind
     * connectivity whose base URL may be overridden using {@link #IGTIMI_BASE_URL}. Specify a bearer token valid in the
     * context of the security service of the remote Riot service. Maps to the {@code igtimi.bearer.token} property.
     */
    IGTIMI_BEARER_TOKEN,
    
    /**
     * Provides a Google Maps JavaScript API key, typically in the format "key=..." or "client=...&channel=...".
     * You can obtain one from your <a href="https://console.cloud.google.com">Google Cloud Console</a>
     */
    GOOGLE_MAPS_AUTHENTICATION_PARAMS,
    
    /**
     * A Google / YouTube API key, just as the token itself, without any "key=..." prefix. Used, e.g., for
     * accessing the metadata of a YouTube video during upload and linking to races.
     */
    YOUTUBE_API_KEY;
}
