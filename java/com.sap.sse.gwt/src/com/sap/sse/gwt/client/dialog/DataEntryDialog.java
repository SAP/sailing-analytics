package com.sap.sse.gwt.client.dialog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.LongBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ColorTextBox;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.controls.GenericListBox;
import com.sap.sse.gwt.client.controls.GenericListBox.ValueBuilder;
import com.sap.sse.gwt.client.controls.IntegerBox;

/**
 * An abstract data entry dialog class, capturing data of type <code>T</code>, with generic OK/Cancel buttons, title and
 * message. Subclasses may override the {@link #show()} method to set the focus on their favorable initial entry field.
 * Subclasses can specify a widget to show in the dialog to capture properties specific to the result type <code>T</code> by
 * overriding the {@link #getAdditionalWidget()} method.<p>
 * 
 * Subclasses can override which elements initially gets the focus by redefining the {@link #getInitialFocusWidget} method.
 * By default, the OK button will have the focus.
 * 
 * @author Axel Uhl (d043530)
 */
public abstract class DataEntryDialog<T> {
    private final DialogBox dataEntryDialog;
    private Validator<T> validator;
    private final Button okButton;
    private final Button cancelButton;
    private final HTML statusLabel;
    private final FlowPanel errorListPanel;
    private final FlowPanel panelForAdditionalWidget;
    private final DockPanel buttonPanel;
    private final FlowPanel rightButtonPanel;
    private final FlowPanel leftButtonPanel;
    private final AsyncActionsExecutor validationExecutor;
    protected static final String VALIDATION_ACTION_CATEGORY = "validation";
    
    private boolean dialogInInvalidState = false;

    @FunctionalInterface
    public static interface Validator<T> {
        /**
         * @return <code>null</code> in case the <code>valueToValidate</code> is valid; a user-readable error message otherwise
         */
        String getErrorMessage(T valueToValidate);
        
        default void validate(T valueToValidate, AsyncCallback<String> callback, AsyncActionsExecutor validationExecutor) {
            validationExecutor.execute(cb->cb.onSuccess(getErrorMessage(valueToValidate)), VALIDATION_ACTION_CATEGORY, callback);
        }
    }
    
    public static interface DialogCallback<T> {
        void ok(T editedObject);
        void cancel();
    }

    /**
     * @param message
     *            Displayed beneath the title. May be {@code null}
     * @param cancelButtonName
     *            if {@code null}, no cancel button will be displayed
     * @param validator
     *            an optional validator; if <code>null</code>, no validation of data entered is performed; otherwise,
     *            data validation is triggered upon any noticeable change in any of the elements constructed by
     *            {@link #createCheckbox(String)}, {@link #createTextBox(String)}, etc.
     * @param callback
     *            will be called when the dialog if {@link AsyncCallback#onFailure(Throwable) cancelled} or
     *            {@link AsyncCallback#onSuccess(Object) confirmed}
     */
    public DataEntryDialog(String title, String message, String okButtonName, String cancelButtonName,
            Validator<T> validator, final DialogCallback<T> callback) {
        this(title, message, okButtonName, cancelButtonName, validator, /* animationEnabled */ true, callback);
    }
    
    /**
     * @param message
     *            may be {@code null}
     * @param cancelButtonName
     *            if {@code null}, no cancel button will be displayed
     * @param validator
     *            an optional validator; if <code>null</code>, no validation of data entered is performed; otherwise,
     *            data validation is triggered upon any noticeable change in any of the elements constructed by
     *            {@link #createCheckbox(String)}, {@link #createTextBox(String)}, etc.
     * @param callback
     *            will be called when the dialog if {@link AsyncCallback#onFailure(Throwable) cancelled} or
     *            {@link AsyncCallback#onSuccess(Object) confirmed}; may be {@code null}
     */
    public DataEntryDialog(String title, String message, String okButtonName, String cancelButtonName,
            Validator<T> validator, boolean animationEnabled, final DialogCallback<T> callback) {
        validationExecutor = new AsyncActionsExecutor();
        dataEntryDialog = new DialogBox();
        dataEntryDialog.setText(title);
        dataEntryDialog.setGlassEnabled(true);
        this.validator = validator;
        okButton = new Button(okButtonName);
        okButton.getElement().getStyle().setMargin(3, Unit.PX);
        okButton.ensureDebugId("OkButton");
        okButton.addStyleName("btn-lg");
        okButton.addStyleName("btn-primary");
        FlowPanel dialogFPanel = new FlowPanel();
        dialogFPanel.setWidth("100%");
        statusLabel = new HTML(SafeHtmlUtils.fromSafeConstant("&nbsp;"));
        statusLabel.ensureDebugId("StatusLabel");
        dialogFPanel.add(statusLabel);
        errorListPanel = new FlowPanel();
        dialogFPanel.add(errorListPanel);
        if (message != null) {
            Label messageLabel = new Label(message);
            messageLabel.addStyleName("dialogMessageLabel");
            dialogFPanel.add(messageLabel);
        }
        panelForAdditionalWidget = new FlowPanel();
        panelForAdditionalWidget.setWidth("100%");
        dialogFPanel.add(panelForAdditionalWidget);
        buttonPanel = new DockPanel();
        buttonPanel.setWidth("100%");
        dialogFPanel.add(buttonPanel);
        rightButtonPanel = new FlowPanel();
        leftButtonPanel = new FlowPanel();
        leftButtonPanel.setStyleName("additionalWidgetsLeft");
        rightButtonPanel.setStyleName("additionalWidgetsRight");
        rightButtonPanel.add(okButton);
        buttonPanel.add(rightButtonPanel, DockPanel.EAST);
        buttonPanel.add(leftButtonPanel, DockPanel.WEST);
        if (cancelButtonName != null) {
            cancelButton = new Button(cancelButtonName);
            cancelButton.getElement().getStyle().setMargin(3, Unit.PX);
            cancelButton.ensureDebugId("CancelButton");
            rightButtonPanel.add(cancelButton);
            cancelButton.addStyleName("btn-lg");
            cancelButton.addStyleName("btn-secondary");
            cancelButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    dataEntryDialog.hide();
                    if (callback != null) {
                        callback.cancel();
                    }
                }
            });
        } else {
            cancelButton = null;
        }
        dataEntryDialog.setWidget(dialogFPanel);
        okButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                // wait for any outstanding validation request and check last validation result; call OK only if the pending validation was OK
                ifLastValidationRequestSuccessful(()->{
                    dataEntryDialog.hide();
                    if (callback != null) {
                        callback.ok(getResult());
                    }
                });
            }
        });
    }
    
    /**
     * If the {@link #validationExecutor} has no more pending actions and the last validation was successful,
     * call {@code callback}. If an action is still pending in the {@link #validationExecutor}, wait until no more
     * action is pending and invoke {@code callback} if the last validation state was OK.
     */
    protected void ifLastValidationRequestSuccessful(Runnable callback) {
        validationExecutor.runAfterLastActionReturned(VALIDATION_ACTION_CATEGORY, ()->{
            if (!dialogInInvalidState) {
                callback.run();
            }
        });
    }

    public void setValidator(Validator<T> validator) {
        this.validator = validator;
    }
    
    protected void validateAndUpdate() {
        T result = getResult();
        if (validator != null) {
            validator.validate(result, new AsyncCallback<String>() {
                @Override
                public void onFailure(Throwable caught) {
                    onSuccess(caught.getMessage());
                }

                /**
                 * Note: if {@code errorMessage} is not {@code null}, the method name "onSuccess" is a bit misleading
                 * because it notifies an error condition.
                 */
                @Override
                public void onSuccess(String errorMessage) {
                    errorListPanel.clear();
                    boolean invalidState = errorMessage != null && !errorMessage.isEmpty();
                    if (invalidState != dialogInInvalidState) {
                        onInvalidStateChanged(invalidState);
                    }
                    if (invalidState) {
                        String[] errors = errorMessage.split("\\r?\\n|\\r");
                        if (errors.length > 1) {
                            for (int i=0; i< errors.length; i++) {
                                GWT.log(i + " - " + errors[i]);
                                HTML errorLabel = new HTML(SafeHtmlUtils.fromString(errors[i]));
                                errorLabel.setStyleName("errorLabel");
                                errorListPanel.add(errorLabel);
                            }
                            statusLabel.setHTML(SafeHtmlUtils.fromSafeConstant("&nbsp;"));
                        } else {
                            statusLabel.setHTML(SafeHtmlUtils.fromString(errorMessage));
                            statusLabel.setStyleName("errorLabel");
                        }
                        getOkButton().setEnabled(false);
                    } else {
                        statusLabel.setHTML(SafeHtmlUtils.fromSafeConstant("&nbsp;"));
                        onChange(result);
                        getOkButton().setEnabled(true);
                    }
                }
            }, validationExecutor);
        }
    }

    /**
     * Allows subclasses to listen to changes of the data shown in the dialog.
     */
    protected void onChange(T result) {
    }
    
    protected void onInvalidStateChanged(boolean invalidState) {
        getOkButton().setEnabled(!invalidState);
        dialogInInvalidState = invalidState;
    }

    protected abstract T getResult();

    /**
     * This methods creates a {@link MultiWordSuggestOracle} where the given suggest values are
     * {@link MultiWordSuggestOracle#addAll(java.util.Collection) added} and
     * {@link MultiWordSuggestOracle#setDefaultSuggestionsFromText(java.util.Collection) set as default suggestions},
     * first. Afterwards, this oracle is used to {@link #createSuggestBox(SuggestOracle) create} as {@link SuggestBox}.
     * 
     * @param suggestValues the plain text suggestions to use
     * @return the new {@link SuggestBox} instance
     * 
     * @see #createSuggestBox(SuggestOracle)
     */
    protected SuggestBox createSuggestBox(Iterable<String> suggestValues) {
        List<String> suggestValuesAsCollection = new ArrayList<>();
        Util.addAll(suggestValues, suggestValuesAsCollection);
        final MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
        oracle.addAll(suggestValuesAsCollection);
        oracle.setDefaultSuggestionsFromText(suggestValuesAsCollection);
        return createSuggestBox(oracle);
    }
    
    /**
     * Creates a {@link SuggestBox} using the given {@link SuggestOracle}, a {@link TextBox} and the
     * {@link com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay DefaultSuggestionDisplay} 
     * 
     * @param suggestOracle the {@link SuggestOracle} to in the {@link SuggestBox}
     * @return the new {@link SuggestBox} instance
     * 
     * @see SuggestBox#SuggestBox(SuggestOracle)
     */
    protected SuggestBox createSuggestBox(SuggestOracle suggestOracle) {
        final SuggestBox result = new SuggestBox(suggestOracle);
        ensureHasValueIsValidated(result.getValueBox());
        ensureChangeableIsValidated(result.getValueBox());
        result.getValueBox().addKeyUpHandler(event -> validateAndUpdate());
        ensureFocusWidgetIsLinkedToKeyStrokes(result.getValueBox());
        return result;
    }
    
    /**
     * Creates a text box with a key-up listener attached which ensures the value is updated after each
     * key-up event and the entire dialog is {@link #validateAndUpdate() validated} in this case.
     * 
     * @param initialValue initial value to show in text box; <code>null</code> is permissible
     */
    public TextBox createTextBox(String initialValue) {
        return createTextBox(initialValue, 30);
    }
    
    /**
     * Creates a text box with a key-up listener attached which ensures the value is updated after each
     * key-up event and the entire dialog is {@link #validateAndUpdate() validated} in this case.
     * 
     * @param initialValue initial value to show in text box; <code>null</code> is permissible
     * @param visibleLength the visible length of the text box
     */
    public TextBox createTextBox(String initialValue, int visibleLength) {
        return configureTextBox(new TextBox(), initialValue, visibleLength);
    }
        
    public ColorTextBox createColorTextBox(Color initialValue) {
        return createColorTextBox(initialValue, 30);
    }
    
    public ColorTextBox createColorTextBox(Color initialValue, int visibleLength) {
        return configureTextBox(new ColorTextBox(), initialValue == null ? null : initialValue.getAsHtml(), visibleLength);
    }
    
    private <TextBoxType extends TextBox> TextBoxType configureTextBox(TextBoxType textBox, String initialValue, int visibleLength) {
        textBox.setVisibleLength(visibleLength);
        textBox.setText(initialValue == null ? "" : initialValue);
        DialogUtils.addFocusUponKeyUpToggler(textBox);
        ensureChangeableIsValidated(textBox);
        ensureFocusWidgetIsLinkedToKeyStrokes(textBox);
        return textBox;
    }
    
    /**
     * Creates a password text box with a key-up listener attached which ensures the value is updated after each
     * key-up event and the entire dialog is {@link #validateAndUpdate() validated} in this case.
     * 
     * @param initialValue initial value to show in text box; <code>null</code> is permissible
     */
    public PasswordTextBox createPasswordTextBox(String initialValue) {
        return createPasswordTextBoxInternal(initialValue, 30);
    }

    /**
     * Creates a password text box with a key-up listener attached which ensures the value is updated after each
     * key-up event and the entire dialog is {@link #validateAndUpdate() validated} in this case.
     * 
     * @param initialValue initial value to show in text box; <code>null</code> is permissible
     * @param visibleLength the visible length of the text box
     */
    public PasswordTextBox createPasswordTextBox(String initialValue, int visibleLength) {
        return createPasswordTextBoxInternal(initialValue, visibleLength);
    }
        
    private PasswordTextBox createPasswordTextBoxInternal(String initialValue, int visibleLength) {
        PasswordTextBox passwordTextBox = new PasswordTextBox();
        passwordTextBox.setVisibleLength(visibleLength);
        passwordTextBox.setText(initialValue == null ? "" : initialValue);
        DialogUtils.addFocusUponKeyUpToggler(passwordTextBox);
        ensureChangeableIsValidated(passwordTextBox);
        ensureFocusWidgetIsLinkedToKeyStrokes(passwordTextBox);
        return passwordTextBox;
    }
    
    /**
     * Creates a text area with a key-up listener attached which ensures the value is updated after each
     * key-up event and the entire dialog is {@link #validateAndUpdate() validated} in this case.
     * 
     * @param initialValue Initial value to show in text area; <code>null</code> is permissible
     */
    public TextArea createTextArea(String initialValue) {
        TextArea textArea = new TextArea();
        textArea.setText(initialValue == null ? "" : initialValue);
        DialogUtils.addFocusUponKeyUpToggler(textArea);
        ensureChangeableIsValidated(textArea);
        DialogUtils.linkEscapeToButton(getCancelButton(), textArea);
        return textArea;
    }
    
    /**
     * Creates a box for a long value with a key-up listener attached which ensures the value is updated after each
     * key-up event and the entire dialog is {@link #validateAndUpdate() validated} in this case.
     * 
     * @param initialValue initial value to show in the long box; <code>null</code> is permissible
     */
    public LongBox createLongBox(long initialValue, int visibleLength) {
        LongBox longBox = new LongBox();
        longBox.setVisibleLength(visibleLength);
        longBox.setValue(initialValue);
        DialogUtils.addFocusUponKeyUpToggler(longBox);
        ensureChangeableIsValidated(longBox);
        ensureFocusWidgetIsLinkedToKeyStrokes(longBox);
        return longBox;
    }

    public DoubleBox createDoubleBox(Double initialValue, int visibleLength) {
        return createDoubleBoxInternal(initialValue, visibleLength);
    }

    public DoubleBox createDoubleBox(int visibleLength) {
        return createDoubleBoxInternal(null, visibleLength);
    }
    
    private DoubleBox createDoubleBoxInternal(Double initialValue, int visibleLength) {
        final DoubleBox doubleBox = new DoubleBox();
        doubleBox.setVisibleLength(visibleLength);
        doubleBox.setValue(initialValue);
        DialogUtils.addFocusUponKeyUpToggler(doubleBox);
        ensureChangeableIsValidated(doubleBox);
        ensureFocusWidgetIsLinkedToKeyStrokes(doubleBox);
        return doubleBox;
    }

    public DateBox createDateBox(Date initialDate, int visibleLength) {
        return createDateBoxInternal(initialDate, visibleLength);
    }

    public DateBox createDateBox(long initialTimeInMs, int visibleLength) {
        return createDateBoxInternal(new Date(initialTimeInMs), visibleLength);
    }

    public DateBox createDateBox(int visibleLength) {
        return createDateBoxInternal(null, visibleLength);
    }
    
    private DateBox createDateBoxInternal(Date initialDate, int visibleLength) {
        DateBox dateBox = new DateBox();
        dateBox.getTextBox().setVisibleLength(visibleLength);
        dateBox.setFireNullValues(true);
        dateBox.setValue(initialDate);
        DialogUtils.addFocusUponKeyUpToggler(dateBox.getTextBox());
        ensureHasValueIsValidated(dateBox);
        ensureFocusWidgetIsLinkedToKeyStrokes(dateBox.getTextBox());
        return dateBox;
    }

    /**
     * Creates a box for a integer value with a key-up listener attached which ensures the value is updated after each
     * key-up event and the entire dialog is {@link #validateAndUpdate() validated} in this case.
     * 
     * @param initialValue initial value to show in the integer box; <code>null</code> is permissible
     */
    public IntegerBox createIntegerBox(Integer initialValue, int visibleLength) {
        IntegerBox intBox = new IntegerBox();
        intBox.setVisibleLength(visibleLength);
        intBox.setValue(initialValue);
        DialogUtils.addFocusUponKeyUpToggler(intBox);
        ensureChangeableIsValidated(intBox);
        ensureFocusWidgetIsLinkedToKeyStrokes(intBox);
        return intBox;
    }

    public LongBox createLongBoxWithOptionalValue(Long initialValue, int visibleLength) {
        LongBox longBox = new LongBox();
        longBox.setVisibleLength(visibleLength);
        longBox.setValue(initialValue);
        DialogUtils.addFocusUponKeyUpToggler(longBox);
        ensureChangeableIsValidated(longBox);
        ensureFocusWidgetIsLinkedToKeyStrokes(longBox);
        return longBox;
    }

    public Label createHeadlineLabel(String headlineText) {
        Label headlineLabel = new Label(headlineText);
        headlineLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
        headlineLabel.getElement().getStyle().setPaddingTop(1, Unit.EM);
        return headlineLabel;
    }
    
    public FlowPanel createHeadline(String headlineText, boolean regularHeadline) {
        FlowPanel headlinePanel = new FlowPanel();
        Label headlineLabel = new Label(headlineText);
        if (regularHeadline) {
            headlinePanel.addStyleName("dialogInnerHeadline");
        } else {
            headlinePanel.addStyleName("dialogInnerHeadlineOther");
        }
        headlinePanel.add(headlineLabel);
        return headlinePanel;
    }

    public CheckBox createCheckbox(String checkboxLabel) {
        CheckBox result = new CheckBox(checkboxLabel);
        result.setWordWrap(false);
        ensureHasValueIsValidated(result);
        ensureFocusWidgetIsLinkedToKeyStrokes(result);
        return result;
    }

    public CheckBox registerCheckbox(CheckBox result) {
        result.setWordWrap(false);
        ensureHasValueIsValidated(result);
        ensureFocusWidgetIsLinkedToKeyStrokes(result);
        return result;
    }

    public <C extends HasValueChangeHandlers<?> & HasAllKeyHandlers> C create(final Supplier<C> factory) {
        final C control = factory.get();
        ensureHasValueIsValidated(control);
        ensureFocusWidgetIsLinkedToKeyStrokes(control);
        return control;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void ensureHasValueIsValidated(HasValueChangeHandlers hasValue){
        hasValue.addValueChangeHandler(event -> validateAndUpdate());
    }
    
    public void ensureChangeableIsValidated(HasChangeHandlers changeable) {
        changeable.addChangeHandler(event -> validateAndUpdate());
    }
    
    public void ensureFocusWidgetIsLinkedToKeyStrokes(HasAllKeyHandlers widget) {
        DialogUtils.linkEnterToButton(getOkButton(), widget);
        DialogUtils.linkEscapeToButton(getCancelButton(), widget);
    }
    
    public void addTooltip(IsWidget widget, String tooltip) {
        widget.asWidget().setTitle(tooltip);
    }

    public RadioButton createRadioButton(String radioButtonGroupName, String radioButtonLabel) {
        RadioButton result = new RadioButton(radioButtonGroupName, radioButtonLabel);
        result.setWordWrap(false);
        ensureHasValueIsValidated(result);
        ensureFocusWidgetIsLinkedToKeyStrokes(result);
        return result;
    }

    /**
     * Creates a standard label for input fields.
     * The label has some default formatting like "no wrap" and a colon right after the label text 
     */
    public Label createLabel(String name) {
        Label result = new Label(name + ":");
        result.setWordWrap(false);
        return result;
    }

    public <ListItemT> GenericListBox<ListItemT> createGenericListBox(ValueBuilder<ListItemT> valueBuilder,
            boolean isMultipleSelect) {
        GenericListBox<ListItemT> result = new GenericListBox<>(valueBuilder);
        result.setMultipleSelect(isMultipleSelect);
        ensureChangeableIsValidated(result);
        ensureFocusWidgetIsLinkedToKeyStrokes(result);
        return result;
    }
    
    public ListBox createListBox(boolean isMultipleSelect) {
        ListBox result = new ListBox();
        result.setMultipleSelect(isMultipleSelect);
        ensureChangeableIsValidated(result);
        ensureFocusWidgetIsLinkedToKeyStrokes(result);
        return result;
    }

    public void alignAllPanelWidgetsVertically(HorizontalPanel panel, HasVerticalAlignment.VerticalAlignmentConstant alignment) {
        for (int i = 0; i < panel.getWidgetCount(); i++) {
            panel.setCellVerticalAlignment(panel.getWidget(i), alignment);
        }
    }
    
    /**
     * Can contribute an additional widget to be displayed underneath the text entry field. If <code>null</code> is
     * returned, no additional widget will be displayed. This is the default behavior of this default implementation.
     */
    protected Widget getAdditionalWidget() {
        return null;
    }
    
    protected Button getOkButton() {
        return okButton;
    }

    protected Button getCancelButton() {
        return cancelButton;
    }

    /**
     * status label can be used to display status information within the dialog to the user
     */
    protected Label getStatusLabel() {
        return statusLabel;
    }

    protected void setCursor(Style.Cursor cursor) {
        dataEntryDialog.getElement().getStyle().setCursor(cursor);
    }

    public void center() {
        dataEntryDialog.center();
    }

    public void show() {
        Widget additionalWidget = getAdditionalWidget();
        if (additionalWidget != null) {
            panelForAdditionalWidget.add(additionalWidget);
        }
        validateAndUpdate();
        dataEntryDialog.center();
        final Focusable focusWidget = getInitialFocusWidget();
        if (focusWidget != null) {
            if (!(focusWidget instanceof HasEnabled) || ((HasEnabled) focusWidget).isEnabled()) {
                Scheduler.get().scheduleFinally(new ScheduledCommand() {
                    @Override
                    public void execute() {
                        focusWidget.setFocus(true);
                    }
                });
            } else {
                DialogUtils.linkEscapeToButton(okButton, okButton);
                Scheduler.get().scheduleFinally(new ScheduledCommand() {
                    @Override
                    public void execute() {
                        okButton.setFocus(true);
                    }
                });
            }
        } else {
            okButton.setFocus(true);
        }
    }
    
    /**
     * Defines the {@link #okButton} as the default initial focus widget. Subclasses may redefine. Return
     * {@code null} to not set the focus on any widget.
     */
    protected Focusable getInitialFocusWidget() {
        return okButton;
    }

    protected DialogBox getDialogBox() {
        return dataEntryDialog;
    }
    
    public void ensureDebugId(String debugId) {
        dataEntryDialog.ensureDebugId(debugId);
    }
    
    protected void addAutoHidePartner(Element element) {
        dataEntryDialog.addAutoHidePartner(element);
    }
    
    protected FlowPanel getLeftButtonPannel() {
        return leftButtonPanel;
    }
    
    protected FlowPanel getRightButtonPannel() {
        return rightButtonPanel;
    }
}
