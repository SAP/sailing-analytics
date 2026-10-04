package com.sap.sailing.gwt.ui.adminconsole.places.refresher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.Refresher;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;

public abstract class AbstractRefresher<T> implements Refresher<T> {
    private Logger logger = Logger.getLogger(getClass().getName());
    private final Set<Displayer<T>> displayers = new HashSet<Displayer<T>>();
    private List<T> dtos;
    private boolean loading;

    @Override
    public void addDisplayerAndCallFillOnInit(Displayer<T> displayer) {
        if (displayer == null) {
            throw new IllegalArgumentException("Displayer cannot be null.");
        }
        displayers.add(displayer);
        if (dtos == null) {
            reloadAndCallFillAll();
        } else {
            logger.fine("Call fill methods from displayers with data from cache.");
            fill(dtos, displayer);
        }
    }
    
    @Override
    public void removeDisplayer(Displayer<T> displayer) {
        displayers.remove(displayer);
    }

    @Override
    public void reloadAndCallFillOnly(Displayer<T> fillOnlyDisplayer) {
        reloadAndCallFillOnly(fillOnlyDisplayer, null);
    }

    public void reloadAndCallFillOnly(Displayer<T> fillOnlyDisplayer, Displayer<T> fillAdditionally) {
        AsyncCallback<Iterable<T>> callback = new AsyncCallback<Iterable<T>>() {
            @Override
            public void onFailure(Throwable caught) {
                // already logged. do nothing here
            }
            @Override
            public void onSuccess(Iterable<T> result) {
                if (result != null) {
                    dtos = new ArrayList<T>();
                    result.forEach(dto -> dtos.add(dto));
                } else {
                    dtos = null;
                }
                loading = false;
                logger.fine("Loading data finished. Call fill methods from displayers.");
                if (fillOnlyDisplayer != null) {
                    fill(result, fillOnlyDisplayer);
                } else {
                    callAllFill(result, null);
                }
                if (fillAdditionally != null && !displayers.contains(fillAdditionally)) {
                    fill(result, fillAdditionally);
                }
            }
        };
        if (!loading) {
            logger.fine("Start loading data from service.");
            loading = true;
            reload(new MarkedAsyncCallback<>(callback));
        } else {
            logger.fine("Data is already loading. Skip reload.");
        }
    }

    @Override
    public void reloadAndCallFillAll() {
        reloadAndCallFillOnly(null);
    }

    @Override
    public void updateAndCallFillForAll(Iterable<T> dtos, Displayer<T> origin) {
        callAllFill(dtos, origin);
    }

    @Override
    public void callFillAndReloadInitially(Displayer<T> displayer) {
        if (dtos == null) {
            reloadAndCallFillOnly(null, displayer);
        } else if (displayer != null) {
            fill(dtos, displayer);
        } else {
            // ignore this call because no displayer defined which can execute the fill method.
        }
    }

    /**
     * Exclude the origin from execution of fill method -> Filter
     */
    private void callAllFill(final Iterable<T> dtos, final Displayer<T> origin) {
        displayers.stream()
                .filter(displayer -> !Objects.equals(displayer, origin))
                .forEach(displayer -> fill(dtos, displayer));
    }
    
    @Override
    public void callAllFill() {
        displayers.stream()
                .forEach(displayer -> fill(dtos, displayer));
    }

    public abstract void reload(AsyncCallback<Iterable<T>> callback);

    protected void fill(Iterable<T> dtos, Displayer<T> displayer) {
        displayer.fill(dtos);
    }
    
    @Override
    public void addIfNotContainedElseReplace(T dto, EntityIdentityComparator<T> comp) {
        if (dto != null && dtos != null) {
            Optional<T> existingDtoOption = dtos.stream().filter(listDto -> comp.representSameEntity(listDto, dto))
                    .findFirst();
            if (existingDtoOption.isPresent()) {
                int index = dtos.indexOf(existingDtoOption.get());
                dtos.set(index, dto);
            } else {
                add(dto);
            }
        }
    }
    
    @Override
    public void add(T dto) {
        if (dto != null && dtos != null) {
            dtos.add(dto);
        }
    }
    
    @Override
    public void remove(T dto) {
        if (dto != null && dtos != null) {
            dtos.remove(dto);
        }
    }

    @Override
    public void removeAll(Predicate<T> filter) {
        if (dtos != null) {
            for (final Iterator<T> i=dtos.iterator(); i.hasNext(); ) {
                final T dto = i.next();
                if (filter.test(dto)) {
                    i.remove();
                }
            }
        }
    }
}
