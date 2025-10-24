package com.sap.sailing.gwt.home.shared.partials.filter;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sap.sailing.gwt.home.shared.refresh.RefreshableWidget;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.gwt.shared.DTO;

public abstract class FilterPresenter<T, C extends Comparable<C>> {
    private final Filter<T> alwaysMatchingFilter = new Filter<T>() {
        @Override
        public boolean matches(T object) {
            return true;
        }

        @Override
        public String getName() {
            return "alwaysMatchingFilter";
        }
    };
    private final FilterWidget<T, C> filterWidget;

    public FilterPresenter(FilterWidget<T, C> filterWidget) {
        this.filterWidget = filterWidget;
        this.filterWidget.asWidget().setVisible(false);
    }

    protected abstract List<FilterValueChangeHandler<T>> getCurrentValueChangeHandlers();

    protected abstract List<FilterValueProvider<C>> getCurrentValueProviders();

    protected void addHandler(FilterValueChangeHandler<T> handler) {
        this.filterWidget.addFilterValueChangeHandler(handler);
    }

    public void update() {
        Set<C> filterableValues = new TreeSet<>();
        getCurrentValueProviders().forEach(provider -> filterableValues.addAll(provider.getFilterableValues()));
        this.filterWidget.asWidget().setVisible(!filterableValues.isEmpty());
        this.filterWidget.setSelectableValues(filterableValues);
        Filter<T> filter = filterableValues.isEmpty() ? alwaysMatchingFilter : this.filterWidget.getFilter();
        getCurrentValueChangeHandlers().forEach(handler -> handler.onFilterValueChanged(filter));
    }
    
    public <D extends DTO> RefreshableWidget<D> getRefreshableWidgetWrapper(final RefreshableWidget<D> wrappedWidget) {
        return new RefreshableWidget<D>() {
            @Override
            public void setData(D data) {
                wrappedWidget.setData(data);
                FilterPresenter.this.update();
            }
        };
    }
    
}