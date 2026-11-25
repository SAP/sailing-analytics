package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * Offers to enable automatic tracking times before denoting a leaderboard for smartphone tracking.
 */
public class AutomaticTrackingTimesDialog extends DataEntryDialog<Void> {
    private final StringMessages stringMessages;
    private final String regattaName;
    private final boolean currentlyUsesStartTimeInference;

    public AutomaticTrackingTimesDialog(final StringMessages stringMessages, final String regattaName,
            final boolean currentlyUsesStartTimeInference, final DialogCallback<Void> callback) {
        super(stringMessages.automaticTrackingTimes(), /* message */ null,
                stringMessages.applyAutomaticTrackingTimes(), stringMessages.continueWithManualTrackingTimes(),
                /* validator */ null, callback);
        this.stringMessages = stringMessages;
        this.regattaName = regattaName;
        this.currentlyUsesStartTimeInference = currentlyUsesStartTimeInference;
        ensureDebugId("AutomaticTrackingTimesDialog");
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel panel = new VerticalPanel();
        panel.setWidth("600px");
        panel.getElement().getStyle().setProperty("maxWidth", "80vw");
        panel.setSpacing(/* spacing */ 10);
        panel.add(new HTML(new SafeHtmlBuilder().appendEscapedLines(
                stringMessages.suggestAutomaticTrackingTimes(regattaName,
                        stringMessages.controlTrackingFromStartAndFinishTimes())).toSafeHtml()));
        if (currentlyUsesStartTimeInference) {
            // The two settings are mutually exclusive on the server, so warn that applying automatic tracking times
            // will also switch off start-time inference for this regatta.
            panel.add(new HTML(new SafeHtmlBuilder().appendEscapedLines(
                    stringMessages.suggestAutomaticTrackingTimesAlsoDisablesInference(regattaName,
                            stringMessages.useStartTimeInference(),
                            stringMessages.controlTrackingFromStartAndFinishTimes())).toSafeHtml()));
        }
        final Anchor raceManagerLink = new Anchor(stringMessages.learnMoreAboutRaceManagerApp(),
                "https://www.sapsailing.com/gwt/Home.html#SolutionsPlace:navigationTab=RaceManagerApp", "_blank");
        raceManagerLink.getElement().setAttribute("rel", "noopener noreferrer");
        panel.add(raceManagerLink);
        return panel;
    }

    @Override
    protected Void getResult() {
        return null;
    }
}
