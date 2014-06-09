package com.sap.sailing.domain.tractracadapter;

import java.util.UUID;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.WithID;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.route.IControlPoint;

/**
 * An interface that hides the subtle differences between a {@link IControlPoint} as obtained through TTCM and a
 * {@link com.sap.sailing.domain.tractracadapter.impl.ClientParamsPHP.ControlPoint} as obtained by reading the
 * <code>clientparams.php</code> file.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface TracTracControlPoint extends WithID {
    /**
     * Refining the return type helps in looking up a control point by its UUID in {@link IEvent}.
     */
    UUID getId();
    
    String getName();
    
    String getMetadata();
    
    String getShortName();
    
    boolean getHasTwoPoints();
    
    Position getMark1Position();
    
    Position getMark2Position();
}
