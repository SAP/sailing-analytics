package com.sap.sailing.gwt.home.shared.refresh;

import com.sap.sse.gwt.shared.DTO;

/**
 * To be implemented by widgets that are automatically managed by a {@link RefreshManager} that (re)loads the data
 * needed by this widget.
 *
 * @param <D>
 *            The type of data accepted by this widget
 */
public interface RefreshableWidget<D extends DTO> {
    void setData(D data);
}
