package com.sap.sailing.gwt.home.shared.partials.multiselection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.shared.partials.filter.AbstractAsyncSuggestBoxFilter;
import com.sap.sailing.gwt.home.shared.partials.filter.AbstractFilterWidget;
import com.sap.sailing.gwt.home.shared.partials.filter.AbstractSuggestBoxFilter;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionPresenter.SuggestionItemsCallback;
import com.sap.sse.gwt.client.suggestion.AbstractSuggestOracle;

/**
 * UI component for multi-selection of entries via suggestion drop-down menu. Selected entries can be removed one by one
 * or in total. In addition, {@link #addNotificationToggle(Consumer, String) checkboxes} can be added optionally.
 * 
 * @param <T>
 *            actual class of selectable entries
 */
public final class SuggestedMultiSelection<T> extends Composite implements SuggestedMultiSelectionView<T>
{

    private static SuggestedMultiSelectionUiBinder uiBinder = GWT.create(SuggestedMultiSelectionUiBinder.class);

    interface SuggestedMultiSelectionUiBinder extends UiBinder<Widget, SuggestedMultiSelection<?>> {
    }

    @UiField
    DivElement contentSeparatorUi;
    @UiField(provided = true)
    AbstractFilterWidget<T, T> searchUi;
    @UiField
    Button removeAllButtonUi;
    @UiField
    FlowPanel selectedItemsUi;
    private final SuggestedMultiSelectionPresenter<T, ?> presenter;
    private final WidgetFactory<T> widgetFactory;

    public static interface WidgetFactory<T> {
        IsWidget generateItemDescriptionWidget(T item);
        AbstractSuggestBoxFilter<T, T> generateSuggestionSearchBar(Consumer<T> onSuggestionSelectedCallback);
    }

    public SuggestedMultiSelection(SuggestedMultiSelectionPresenter<T, ?> presenter,
            WidgetFactory<T> widgetFactory) {
        SuggestedMultiSelectionResources.INSTANCE.css().ensureInjected();
        this.presenter = presenter;
        this.widgetFactory = widgetFactory;
        this.searchUi = widgetFactory.generateSuggestionSearchBar(e -> {
            presenter.addSelection(e);
            selectedItemsUi.add(generateSelectedItemUi(e));
        });
        initWidget(uiBinder.createAndBindUi(this));
    }

    @UiHandler("removeAllButtonUi")
    void onRemoveAllButtonClicked(ClickEvent event) {
        presenter.clearSelection();
        selectedItemsUi.clear();
        UIObject.setVisible(contentSeparatorUi, false);
        removeAllButtonUi.setEnabled(false);
    }
    
    public Set<T> getSelection(){
        return new HashSet<>(presenter.getSelection());
    }  

    @Override
    public void setSelectedItems(Iterable<T> toBeSet) {
        selectedItemsUi.clear();
        UIObject.setVisible(contentSeparatorUi, true);
        presenter.initSelectedItems(toBeSet);
        toBeSet.forEach(e -> {
            selectedItemsUi.add(generateSelectedItemUi(e));
        });
        final boolean areItemsNonEmpty = toBeSet.iterator().hasNext();
        removeAllButtonUi.setEnabled(areItemsNonEmpty);
    }
    
    private SuggestedMultiSelectionItem generateSelectedItemUi(T selectedItem) {
        return new SuggestedMultiSelectionItem() {
            @Override
            protected IsWidget getItemDescriptionWidget() {
                return widgetFactory.generateItemDescriptionWidget(selectedItem);
            }

            @Override
            protected void onRemoveItemRequsted() {
                presenter.removeSelection(selectedItem);
                if (presenter.getSelection().isEmpty()) {
                    UIObject.setVisible(contentSeparatorUi, false);
                }
                this.removeFromParent();
                removeAllButtonUi.setEnabled(selectedItemsUi.getWidgetCount() > 0);
            }
        };
    }

    public static class SelectableSuggestion<T> extends AbstractAsyncSuggestBoxFilter<T, T> {
        private final Consumer<T> onPressedCallback;

        public SelectableSuggestion(final SuggestedMultiSelectionPresenter<T, ?> presenter,
                Consumer<T> onPressedCallback, String placeholderText) {
            super(buildOracle(presenter), placeholderText);
            this.onPressedCallback = onPressedCallback;
        }

        private static <T> AbstractSuggestOracle<T> buildOracle(final SuggestedMultiSelectionPresenter<T, ?> presenter) {
            return new AbstractSuggestOracle<T>() {
                @Override
                protected void getSuggestions(final Request request, final Callback callback,
                        final Iterable<String> queryTokens) {
                    final SuggestionItemsCallback<T> callback2 = new SuggestionItemsCallback<T>() {
                        @Override
                        public void setSuggestionItems(Collection<T> suggestionItems) {
                            setSuggestions(request, callback, suggestionItems, queryTokens);
                        }
                    };
                    presenter.getSuggestionItems(queryTokens, request.getLimit(), callback2);
                }

                @Override
                protected String createSuggestionKeyString(T value) {
                    return presenter.createSuggestionKeyString(value);
                }

                @Override
                protected String createSuggestionAdditionalDisplayString(T value) {
                    return presenter.createSuggestionAdditionalDisplayString(value);
                }
            };
        }

        @Override
        protected final void onSuggestionSelected(T selectedItem) {
            SelectableSuggestion.this.clear();
            onPressedCallback.accept(selectedItem);
        }
    }
}
