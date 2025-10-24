package com.sap.sailing.gwt.ui.client.shared.filter;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * Used to create or edit a {@link FilterSet} for {@link TagDTO}s
 */
public abstract class AbstractTagsFilterSetDialog extends DataEntryDialog<FilterSet<TagDTO, FilterWithUI<TagDTO>>> {
    private final FilterSet<TagDTO, FilterWithUI<TagDTO>> tagsFilterSet;
    private final StringMessages stringMessages;
    private final List<String> availableTagFilterNames;

    private ListBox filterListBox;
    private final Button addFilterButton;

    protected TextBox filterSetNameTextBox;
    private Grid tagsFiltersGrid;
    private Label tagsFiltersGridHeadline;
    private Label tagsFiltersGridFooter;
    private VerticalPanel mainPanel;

    private final List<Widget> filterEditWidgets;
    private final List<FilterUIFactory<TagDTO>> filterUIFactories;
    private final List<Label> filterNameLabels;
    private final List<String> filterNames;
    private final List<Button> filterDeleteButtons;

    protected static class TagsFilterSetValidator implements Validator<FilterSet<TagDTO, FilterWithUI<TagDTO>>> {
        private final StringMessages stringMessages;
        private final List<String> existingFilterSetNames;

        public TagsFilterSetValidator(List<String> existingFilterSetNames, StringMessages stringMessages) {
            super();
            this.existingFilterSetNames = existingFilterSetNames;
            this.stringMessages = stringMessages;
        }

        @Override
        public String getErrorMessage(FilterSet<TagDTO, FilterWithUI<TagDTO>> tagsFilterSet) {
            String errorMessage = null;
            int filterCount = tagsFilterSet.getFilters().size();
            boolean nameNotEmpty = tagsFilterSet.getName() != null && !tagsFilterSet.getName().isEmpty();
            if (!nameNotEmpty) {
                errorMessage = stringMessages.pleaseEnterAName();
            } else if (existingFilterSetNames.contains(tagsFilterSet.getName())) {
                errorMessage = stringMessages.filterThisNameAlreadyExists();
            } else if (filterCount < 1) {
                errorMessage = stringMessages.addAtLeastOneFilterCriteria();
            } else {
                for (FilterWithUI<TagDTO> filter : tagsFilterSet.getFilters()) {
                    errorMessage = filter.validate(stringMessages);
                    if (errorMessage != null) {
                        break;
                    }
                }
            }

            return errorMessage;
        }
    }

    public AbstractTagsFilterSetDialog(FilterSet<TagDTO, FilterWithUI<TagDTO>> tagsFilterSet,
            List<String> availableTagFilterNames, List<String> existingFilterSetNames, String dialogTitle,
            StringMessages stringMessages, DialogCallback<FilterSet<TagDTO, FilterWithUI<TagDTO>>> callback) {
        super(dialogTitle, null, stringMessages.ok(), stringMessages.cancel(),
                new TagsFilterSetValidator(existingFilterSetNames, stringMessages), callback);
        this.tagsFilterSet = tagsFilterSet;
        this.availableTagFilterNames = availableTagFilterNames;
        this.stringMessages = stringMessages;
        tagsFiltersGrid = new Grid(0, 0);
        tagsFiltersGridHeadline = new Label();
        tagsFiltersGridFooter = new Label();
        filterEditWidgets = new ArrayList<Widget>();
        filterNameLabels = new ArrayList<Label>();
        filterNames = new ArrayList<String>();
        filterUIFactories = new ArrayList<>();
        filterDeleteButtons = new ArrayList<>();
        addFilterButton = new Button(stringMessages.add());
        addFilterButton.addStyleName("inlineButton");
        addFilterButton.addStyleName("btn-secondary");
        filterListBox = createListBox(false);
    }

    @Override
    protected Widget getAdditionalWidget() {
        mainPanel = new VerticalPanel();
        Grid hGrid = new Grid(1, 2);
        mainPanel.add(hGrid);
        hGrid.setWidget(0, 0, new Label(stringMessages.filterName() + ":"));
        Grid filterSetNameTextBoxWrapperGrid = new Grid(1,1);
        filterSetNameTextBoxWrapperGrid.setWidget(0, 0, filterSetNameTextBox);
        filterSetNameTextBoxWrapperGrid.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_MIDDLE);
        hGrid.setWidget(0, 1, filterSetNameTextBoxWrapperGrid);
        hGrid.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_MIDDLE);
        hGrid.getCellFormatter().setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_MIDDLE);
        filterListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                updateSelectedFilterInfo();
            }
        });
        filterListBox.addItem(stringMessages.selectAFilterCriteria() + "...");
        for (String filterName : availableTagFilterNames) {
            FilterWithUI<?> filter = TagFilterWithUIFactory.createFilter(filterName);
            filterListBox.addItem(filter.getLocalizedName(stringMessages));
        }
        updateSelectedFilterInfo();
        addFilterButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                FilterWithUI<TagDTO> newFilter = getFilterFromSelectedFilterName();
                if (newFilter != null) {
                    FilterUIFactory<TagDTO> filterUIFactory = newFilter.createUIFactory();
                    filterUIFactories.add(filterUIFactory);
                    createFilterNameAndLabel(newFilter);
                    createFilterEditWidget(filterUIFactory);
                    createFilterDeleteButton(newFilter);
                }
                updateTagsFiltersGrid(mainPanel);
                validateAndUpdate();
            }
        });
        Grid tagsFiltersGridHeadlineGrid = new Grid(1, 1);
        tagsFiltersGridHeadlineGrid.setWidget(0, 0, tagsFiltersGridHeadline);
        tagsFiltersGridHeadlineGrid.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_MIDDLE);
        mainPanel.add(tagsFiltersGridHeadlineGrid);
        mainPanel.add(tagsFiltersGrid);
        mainPanel.add(tagsFiltersGridFooter);
        for (FilterWithUI<TagDTO> existingFilter : tagsFilterSet.getFilters()) {
            FilterWithUI<TagDTO> filter = existingFilter.copy();
            FilterUIFactory<TagDTO> filterUIFactory = filter.createUIFactory();
            filterUIFactories.add(filterUIFactory);
            createFilterNameAndLabel(filter);
            createFilterEditWidget(filterUIFactory);
            createFilterDeleteButton(filter);
        }
        updateTagsFiltersGrid(mainPanel);
        Grid addFilterPanel = new Grid(1, 3);
        mainPanel.add(addFilterPanel);
        addFilterPanel.setWidget(0, 0, new Label(stringMessages.filterCriteria() + ":"));
        Grid filterListBoxWrapperGrid = new Grid(1,1);
        filterListBoxWrapperGrid.setWidget(0, 0, filterListBox);
        filterListBoxWrapperGrid.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_MIDDLE);
        addFilterPanel.setWidget(0, 1, filterListBoxWrapperGrid);
        addFilterPanel.setWidget(0, 2, addFilterButton);
        addFilterPanel.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_MIDDLE);
        addFilterPanel.getCellFormatter().setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_MIDDLE);
        addFilterPanel.getCellFormatter().setVerticalAlignment(0, 2, HasVerticalAlignment.ALIGN_MIDDLE);
        return mainPanel;
    }

    private Label createFilterNameAndLabel(FilterWithUI<TagDTO> filter) {
        Label filterNameLabel = new Label(filter.getLocalizedName(stringMessages) + ":");
        filterNameLabels.add(filterNameLabel);
        filterNames.add(filter.getName());
        return filterNameLabel;
    }

    private Widget createFilterEditWidget(FilterUIFactory<TagDTO> filterUIFactory) {
        Widget filterEditWidget = filterUIFactory.createFilterUIWidget(this);
        filterEditWidgets.add(filterEditWidget);
        return filterEditWidget;
    }

    private Button createFilterDeleteButton(FilterWithUI<TagDTO> filter) {
        final Button filterDeleteBtn = new Button(stringMessages.delete());
        filterDeleteBtn.addStyleName("inlineButton");
        filterDeleteBtn.addStyleName("btn-secondary");
        filterDeleteButtons.add(filterDeleteBtn);
        filterDeleteBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                int index = 0;
                for (Button btn : filterDeleteButtons) {
                    if (filterDeleteBtn == btn) {
                        break;
                    }
                    index++;
                }
                filterNames.remove(index);
                filterNameLabels.remove(index);
                filterEditWidgets.remove(index);
                filterDeleteButtons.remove(index);
                filterUIFactories.remove(index);
                updateTagsFiltersGrid(mainPanel);
                validateAndUpdate();
            }
        });
        return filterDeleteBtn;
    }

    private void updateSelectedFilterInfo() {
        Filter<TagDTO> selectedFilter = getFilterFromSelectedFilterName();
        if (selectedFilter != null) {
            addFilterButton.setEnabled(true);
        } else {
            addFilterButton.setEnabled(false);
        }
    }

    @Override
    protected FilterSet<TagDTO, FilterWithUI<TagDTO>> getResult() {
        FilterSet<TagDTO, FilterWithUI<TagDTO>> result = new FilterSet<>(filterSetNameTextBox.getText());
        for (FilterUIFactory<TagDTO> filterUIFactory : filterUIFactories) {
            result.addFilter(filterUIFactory.createFilterFromUI());
        }
        return result;
    }

    private FilterWithUI<TagDTO> getFilterFromSelectedFilterName() {
        FilterWithUI<TagDTO> result = null;
        int selectedIndex = filterListBox.getSelectedIndex();
        if (selectedIndex > 0) {
            String selectedItemValue = filterListBox.getValue(selectedIndex);
            for (String filterName : availableTagFilterNames) {
                FilterWithUI<TagDTO> filter = TagFilterWithUIFactory.createFilter(filterName);
                if (selectedItemValue.equals(filter.getLocalizedName(stringMessages))) {
                    result = filter;
                    break;
                }
            }
        }
        return result;
    }

    private void updateTagsFiltersGrid(VerticalPanel parentPanel) {
        int widgetIndex = parentPanel.getWidgetIndex(tagsFiltersGrid);
        parentPanel.remove(tagsFiltersGrid);
        int filterCount = filterNameLabels.size();
        boolean showGridHeadline = filterCount >= 1 ? true : false;
        boolean showGridFooter = filterCount >= 1 ? true : false;
        if (filterCount > 0) {
            tagsFiltersGrid = new Grid(filterCount, 3);
            tagsFiltersGrid.setCellSpacing(4);
            tagsFiltersGridHeadline.setVisible(showGridHeadline);
            tagsFiltersGridFooter.setVisible(showGridFooter);
            if (showGridHeadline) {
                tagsFiltersGridHeadline.setText(stringMessages.tagFilterExplanation());
            }
            if (showGridFooter) {
                tagsFiltersGridFooter.setText("");
            }
            for (int i = 0; i < filterCount; i++) {
                tagsFiltersGrid.setWidget(i, 0, filterNameLabels.get(i));
                tagsFiltersGrid.setWidget(i, 1, filterEditWidgets.get(i));
                tagsFiltersGrid.setWidget(i, 2, filterDeleteButtons.get(i));
                tagsFiltersGrid.getCellFormatter().setVerticalAlignment(i, 0, HasVerticalAlignment.ALIGN_MIDDLE);
                tagsFiltersGrid.getCellFormatter().setVerticalAlignment(i, 1, HasVerticalAlignment.ALIGN_MIDDLE);
                tagsFiltersGrid.getCellFormatter().setVerticalAlignment(i, 2, HasVerticalAlignment.ALIGN_MIDDLE);
            }
        } else {
            tagsFiltersGrid = new Grid(0, 0);
        }
        parentPanel.insert(tagsFiltersGrid, widgetIndex);
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return filterSetNameTextBox;
    }
}
