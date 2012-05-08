package com.sap.sailing.gwt.ui.client;

import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LongBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.DataEntryDialog.Validator;
import com.sap.sailing.gwt.ui.shared.components.SettingsDialogComponent;

public class TimePanelSettingsDialogComponent<T extends TimePanelSettings>
		implements SettingsDialogComponent<T> {
	protected LongBox timeDelayBox;
	protected DoubleBox refreshIntervalBox;
	protected final StringMessages stringMessages;
	protected final T initialSettings;
	protected FlowPanel mainContentPanel;

	public TimePanelSettingsDialogComponent(T settings,
			StringMessages stringMessages) {
		this.stringMessages = stringMessages;
		initialSettings = settings;
	}

	protected StringMessages getStringMessages() {
		return stringMessages;
	}

	@Override
	public Widget getAdditionalWidget(DataEntryDialog<?> dialog) {
		// mainContentPanel = new VerticalPanel();
		//
		// HorizontalPanel labelAndRefreshIntervalBoxPanel = new
		// HorizontalPanel();
		// labelAndRefreshIntervalBoxPanel.setSpacing(5);
		// labelAndRefreshIntervalBoxPanel.add(new
		// Label(stringMessages.refreshInterval() + ":"));
		// refreshIntervalBox = dialog.createDoubleBox(((double)
		// initialSettings.getRefreshInterval()) / 1000, 4);
		// labelAndRefreshIntervalBoxPanel.add(refreshIntervalBox);
		// mainContentPanel.add(labelAndRefreshIntervalBoxPanel);
		//
		// HorizontalPanel labelAndTimeDelayBoxPanel = new HorizontalPanel();
		// labelAndTimeDelayBoxPanel.setSpacing(5);
		// labelAndTimeDelayBoxPanel.add(new Label(stringMessages.timeDelay() +
		// ":"));
		// timeDelayBox =
		// dialog.createLongBox(initialSettings.getDelayToLivePlayInSeconds(),
		// 10);
		// labelAndTimeDelayBoxPanel.add(timeDelayBox);
		// mainContentPanel.add(labelAndTimeDelayBoxPanel);
		//
		// return mainContentPanel;

		mainContentPanel = new FlowPanel();

		FlowPanel labelAndRefreshIntervalBoxPanel = new FlowPanel();
		labelAndRefreshIntervalBoxPanel.add(new Label(stringMessages
				.refreshInterval() + ":"));
		refreshIntervalBox = dialog.createDoubleBox(
				((double) initialSettings.getRefreshInterval()) / 1000, 4);
		labelAndRefreshIntervalBoxPanel.setStyleName("labelAndRefresh");
		labelAndRefreshIntervalBoxPanel.add(refreshIntervalBox);

		FlowPanel labelAndTimeDelayBoxPanel = new FlowPanel();
		labelAndTimeDelayBoxPanel.add(new Label(stringMessages.timeDelay()
				+ ":"));
		timeDelayBox = dialog.createLongBox(
				initialSettings.getDelayToLivePlayInSeconds(), 10);
		labelAndTimeDelayBoxPanel.setStyleName("labelAndTime");
		labelAndTimeDelayBoxPanel.add(timeDelayBox);

		mainContentPanel.add(labelAndRefreshIntervalBoxPanel);
		mainContentPanel.add(labelAndTimeDelayBoxPanel);

		return mainContentPanel;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getResult() {
		T result = (T) new TimePanelSettings();
		result.setDelayToLivePlayInSeconds(timeDelayBox.getValue() == null ? -1
				: timeDelayBox.getValue());
		result.setRefreshInterval(refreshIntervalBox.getValue() == null ? -1
				: (long) (refreshIntervalBox.getValue() * 1000));
		return result;
	}

	@Override
	public Validator<T> getValidator() {
		return new Validator<T>() {
			@Override
			public String getErrorMessage(TimePanelSettings valueToValidate) {
				String errorMessage = null;
				if (valueToValidate.getDelayToLivePlayInSeconds() < 0) {
					errorMessage = stringMessages.delayMustBeNonNegative();
				}
				if (valueToValidate.getRefreshInterval() < 500) {
					errorMessage = stringMessages
							.refreshIntervalMustBeGreaterThanXSeconds("0.5");
				}
				return errorMessage;
			}
		};
	}

	@Override
	public FocusWidget getFocusWidget() {
		return timeDelayBox;
	}
}
