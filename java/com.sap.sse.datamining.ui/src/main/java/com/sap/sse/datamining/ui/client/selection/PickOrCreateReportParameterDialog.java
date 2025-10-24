package com.sap.sse.datamining.ui.client.selection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util;
import com.sap.sse.datamining.shared.dto.DataMiningReportDTO;
import com.sap.sse.datamining.shared.dto.FilterDimensionParameter;
import com.sap.sse.datamining.shared.impl.dto.parameters.ValueListFilterParameter;
import com.sap.sse.datamining.ui.client.StringMessages;
import com.sap.sse.gwt.client.controls.GenericListBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * A dialog for picking or creating a query report parameter (see {@link FilterDimensionParameter}) that can be bound
 * to a dimension filter with matching {@link FilterDimensionParameter#getTypeName() type}. This dialog does not
 * modify any parameter values. The {@link DataMiningReportDTO report} passed is modified only when the dialog
 * is confirmed, not cancelled.
 */
public class PickOrCreateReportParameterDialog extends DataEntryDialog<FilterDimensionParameter> { 
    private final StringMessages stringMessages;
    private final String typeName;
    private final GenericListBox<FilterDimensionParameter> parametersListBox;
    private final Set<FilterDimensionParameter> parametersAdded;
    private final Set<FilterDimensionParameter> parametersRemoved;

    private static class ReportParameterValidator implements DataEntryDialog.Validator<FilterDimensionParameter> {
        private final StringMessages stringMessages;
        private final DataMiningReportDTO report;
        
        public ReportParameterValidator(StringMessages stringMessages, DataMiningReportDTO report) {
            super();
            this.stringMessages = stringMessages;
            this.report = report;
        }

        @Override
        public String getErrorMessage(FilterDimensionParameter valueToValidate) {
            String result = null;
            if (valueToValidate == null) {
                result = stringMessages.pleaseSelect();
            } else {
                for (final FilterDimensionParameter existingParameter : report.getParameters()) {
                    if (existingParameter != valueToValidate && Util.equalsWithNull(existingParameter.getName(), valueToValidate.getName())) {
                        result = stringMessages.parameterNamesMustBeUniqueInReport(valueToValidate.getName(), valueToValidate.getTypeName());
                    }
                }
            }
            return result;
        }
    }
    
    private static class Callback implements DialogCallback<FilterDimensionParameter> {
        private final DataMiningReportDTO report;
        private final DialogCallback<FilterDimensionParameter> callback;
        private Set<FilterDimensionParameter> parametersAdded;
        private Set<FilterDimensionParameter> parametersRemoved;
        
        public Callback(DataMiningReportDTO report, DialogCallback<FilterDimensionParameter> callback, Set<FilterDimensionParameter> parametersAdded, Set<FilterDimensionParameter> parametersRemoved) {
            super();
            this.report = report;
            this.callback = callback;
            this.parametersAdded = parametersAdded;
            this.parametersRemoved = parametersRemoved;
        }

        @Override
        public void ok(FilterDimensionParameter editedObject) {
            FilterDimensionParameter result = editedObject;
            for (final FilterDimensionParameter parameterAdded : parametersAdded) {
                final FilterDimensionParameter newParameter = report.createParameter(parameterAdded.getName(), parameterAdded.getTypeName(), Collections.emptySet());
                if (Util.equalsWithNull(editedObject.getName(), parameterAdded.getName())) {
                    // editedObject is the parameter created temporarily outside the report that now needs
                    // to be replaced in the result by the parameter really created within the scope of the report:
                    result = newParameter;
                }
            }
            for (final FilterDimensionParameter parameterRemoved : parametersRemoved) {
                report.removeParameter(parameterRemoved);
            }
            callback.ok(result);
        }

        @Override
        public void cancel() {
            callback.cancel();
        }
        
    }

    public PickOrCreateReportParameterDialog(DataMiningReportDTO report,
            String typeName, StringMessages stringMessages, DialogCallback<FilterDimensionParameter> callback) {
        this(report, typeName, stringMessages, callback, /* paramtersAdded */ new HashSet<>(), /* parametersRemoved */ new HashSet<>());
    }
        
    private PickOrCreateReportParameterDialog(DataMiningReportDTO report,
            String typeName, StringMessages stringMessages, DialogCallback<FilterDimensionParameter> callback,
            Set<FilterDimensionParameter> parametersAdded, Set<FilterDimensionParameter> parametersRemoved) {
        super(stringMessages.pickOrCreateReportParameter(), stringMessages.pickOrCreateReportParameterMessage(),
                stringMessages.ok(), stringMessages.cancel(), new ReportParameterValidator(stringMessages, report),
                new Callback(report, callback, parametersAdded, parametersRemoved));
        this.stringMessages = stringMessages;
        this.typeName = typeName;
        this.parametersAdded = parametersAdded;
        this.parametersRemoved = parametersRemoved;
        parametersListBox = createGenericListBox(param->param.getName(), /* isMultipleSelect */ false);
        for (final FilterDimensionParameter existingParameters : report.getParametersForTypeName(typeName)) {
            parametersListBox.addItem(existingParameters);
        }
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel mainPanel = new VerticalPanel();
        // Header Controls
        final Label parameterTypeSelectionBoxLabel = new Label(stringMessages.parameterType(typeName));
        mainPanel.add(parameterTypeSelectionBoxLabel);
        final HorizontalPanel listBoxAndButtons = new HorizontalPanel();
        mainPanel.add(listBoxAndButtons);
        listBoxAndButtons.add(parametersListBox);
        final Button addButton = new Button(stringMessages.add());
        listBoxAndButtons.add(addButton);
        final Button removeButton = new Button(stringMessages.remove());
        listBoxAndButtons.add(removeButton);
        addButton.addClickHandler(this::addParameterClicked);
        removeButton.addClickHandler(this::removeParameterClicked);
        return mainPanel;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return parametersListBox;
    }
    
    @Override
    public void show() {
        super.show();
        if (parametersListBox.getItemCount() == 0) {
            addParameterClicked(/* event */ null);
        }
    }

    @Override
    protected FilterDimensionParameter getResult() {
        return parametersListBox.getValue();
    }

    private class AddParameterDialog extends DataEntryDialog<String> {
        private final TextBox nameField;

        private AddParameterDialog(DialogCallback<String> callback) {
            super(stringMessages.newParameterName(), /* no message */ null, stringMessages.ok(), stringMessages.cancel(), new DataEntryDialog.Validator<String>() {
                @Override
                public String getErrorMessage(String valueToValidate) {
                    final String errorMessage;
                    if (!Util.hasLength(valueToValidate)) {
                        errorMessage = stringMessages.parameterNameMustNotBeEmpty();
                    } else if (PickOrCreateReportParameterDialog.this.parametersListBox.getValues().stream().anyMatch(p->Util.equalsWithNull(valueToValidate, p.getName()))) {
                        errorMessage = stringMessages.parameterNamesMustBeUniqueInReport(valueToValidate, PickOrCreateReportParameterDialog.this.typeName);
                    } else {
                        errorMessage = null;
                    }
                    return errorMessage;
                }
            }, callback);
            this.nameField = createTextBox("", 20);
        }
        
        @Override
        protected Focusable getInitialFocusWidget() {
            return nameField;
        }
        
        @Override
        protected Widget getAdditionalWidget() {
            return nameField;
        }

        @Override
        protected String getResult() {
            return nameField.getValue();
        }
    }
    
    private void addParameterClicked(ClickEvent e) {
        new AddParameterDialog(new DialogCallback<String>() {
            @Override
            public void ok(String newParameterName) {
                final FilterDimensionParameter newParameter = new ValueListFilterParameter(newParameterName, typeName, Collections.emptySet());
                parametersAdded.add(newParameter);
                parametersListBox.addItem(newParameter);
                parametersListBox.setSelectedIndex(parametersListBox.getItemCount()-1);
                validateAndUpdate();
                getOkButton().setFocus(true);
            }

            @Override
            public void cancel() {
            }
        }).show();
    }

    private void removeParameterClicked(ClickEvent e) {
        parametersRemoved.add(parametersListBox.getValue());
        parametersListBox.removeItem(parametersListBox.getSelectedIndex());
    }
}
