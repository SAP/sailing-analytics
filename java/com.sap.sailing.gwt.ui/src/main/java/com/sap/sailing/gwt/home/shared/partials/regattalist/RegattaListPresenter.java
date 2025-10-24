package com.sap.sailing.gwt.home.shared.partials.regattalist;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterValueChangeHandler;
import com.sap.sailing.gwt.home.shared.partials.filter.FilterValueProvider;
import com.sap.sailing.gwt.home.shared.partials.regattalist.RegattaListView.RegattaListItem;
import com.sap.sailing.gwt.home.shared.refresh.RefreshableWidget;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.gwt.shared.DTO;

public class RegattaListPresenter<D extends DTO>
        implements FilterValueProvider<String>, FilterValueChangeHandler<RegattaMetadataDTO> {

    private final RegattaListView view;
    private final Map<RegattaListItem, RegattaMetadataDTO> stucture = new HashMap<>();
    private Filter<RegattaMetadataDTO> latestLeaderboardGroupFilter;

    public <V extends RegattaListView & RefreshableWidget<D>> RegattaListPresenter(V view) {
        this.view = view;
    }

    @Override
    public void onFilterValueChanged(Filter<RegattaMetadataDTO> filter) {
        this.latestLeaderboardGroupFilter = filter;
        for (Entry<RegattaListItem, RegattaMetadataDTO> entry : stucture.entrySet()) {
            entry.getKey().doFilter(!filter.matches(entry.getValue()));
        }
    }

    @Override
    public Collection<String> getFilterableValues() {
        Set<String> filterableValues = new HashSet<>();
        for (RegattaMetadataDTO regattaMetadata : stucture.values()) {
            if (regattaMetadata.getLeaderboardGroupNames() != null) {
                Util.addAll(regattaMetadata.getLeaderboardGroupNames(), filterableValues);
            }
        }
        return filterableValues.size() > 1 ? filterableValues : Collections.<String> emptySet();
    }

    public RefreshableWidget<D> getRefreshableWidgetWrapper(final RefreshableWidget<D> wrappedWidget) {
        return new RefreshableWidget<D>() {
            @Override
            public void setData(D data) {
                stucture.clear();
                wrappedWidget.setData(data);
                stucture.putAll(view.getItemMap());
                if (latestLeaderboardGroupFilter != null) {
                    RegattaListPresenter.this.onFilterValueChanged(latestLeaderboardGroupFilter);
                }
            }
        };
    }

}
