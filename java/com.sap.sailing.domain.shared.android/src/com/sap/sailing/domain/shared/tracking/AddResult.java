package com.sap.sailing.domain.shared.tracking;

import javax.swing.plaf.basic.BasicSliderUI.TrackListener;

/**
 * The result of trying to add a fix to a {@link Track}. Used when notifying {@link TrackListener}s.
 * This allows listeners, in particular, to distinguish between the add and replace scenario.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public enum AddResult {
    NOT_ADDED, ADDED, REPLACED;
}
