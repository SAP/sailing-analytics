package com.sap.sailing.manage2sail;

import java.io.IOException;
import java.io.InputStream;

public interface Manage2SailEventResultsParser {
    public EventResultDescriptor getEventResult(InputStream is) throws IOException;
}
