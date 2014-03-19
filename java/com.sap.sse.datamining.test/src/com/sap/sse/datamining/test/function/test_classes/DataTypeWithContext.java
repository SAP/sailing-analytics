package com.sap.sse.datamining.test.function.test_classes;

import com.sap.sse.datamining.shared.annotations.Dimension;

/*
 * DON'T CHANGE THE METHOD/CLASS NAMES!
 * The tests will fail, because they are reflected via constant strings.
 */

public interface DataTypeWithContext extends DataTypeInterface {
    
    @Dimension(messageKey="regattaName")
    public String getRegattaName();
    
    @Dimension(messageKey="raceName")
    public String getRaceName();
    
    @Dimension(messageKey="legNumber")
    public int getLegNumber();

}
