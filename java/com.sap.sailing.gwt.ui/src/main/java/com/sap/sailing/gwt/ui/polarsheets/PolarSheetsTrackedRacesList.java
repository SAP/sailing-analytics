package com.sap.sailing.gwt.ui.polarsheets;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.sap.sailing.domain.common.PolarSheetGenerationSettings;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.RaceSelectionProvider;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.components.SettingsDialog;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;

public class PolarSheetsTrackedRacesList extends AbstractFilteredTrackedRacesList {

    private Button btnPolarSheetGeneration;
    private Anchor settingsAnchor;

    public PolarSheetsTrackedRacesList(SailingServiceAsync sailingService, ErrorReporter errorReporter,
            RegattaRefresher regattaRefresher, RaceSelectionProvider raceSelectionProvider,
            StringMessages stringMessages, boolean hasMultiSelection, RaceFilter filter,
            ClickHandler polarSheetsGenerationButtonClickedHandler) {
        super(sailingService, errorReporter, regattaRefresher, raceSelectionProvider, stringMessages,
                hasMultiSelection, filter);
        btnPolarSheetGeneration.addClickHandler(polarSheetsGenerationButtonClickedHandler);
    }

    @Override
    protected void addControlButtons(HorizontalPanel trackedRacesButtonPanel) {
        btnPolarSheetGeneration = new Button(stringMessages.generatePolarSheet());
        btnPolarSheetGeneration.ensureDebugId("PolarSheetGeneration");
        trackedRacesButtonPanel.add(btnPolarSheetGeneration);
        trackedRacesButtonPanel.add(createSettingsLink(stringMessages));
    }
    
    public Anchor createSettingsLink(final StringMessages stringMessages) {
        PolarSheetResources resources = GWT.create(PolarSheetResources.class);
        ImageResource leaderboardSettingsIcon = resources.settingsIcon();
        settingsAnchor = new Anchor(AbstractImagePrototype.create(leaderboardSettingsIcon).getSafeHtml());
        settingsAnchor.setTitle(stringMessages.settings());

        return settingsAnchor;
    }
    
    public void setSettingsHandler(final PolarSheetsPanel overallPanel) {
        settingsAnchor.addClickHandler(new ClickHandler() {
            
            @Override
            public void onClick(ClickEvent event) {
                SettingsDialog<PolarSheetGenerationSettings> dialog = new SettingsDialog<PolarSheetGenerationSettings>(
                        overallPanel, stringMessages);
                dialog.show();
            }
        });
    }

    @Override
    protected void makeControlsReactToSelectionChange(List<RaceDTO> selectedRaces) {
        if (selectedRaces.isEmpty()) {
            btnPolarSheetGeneration.setEnabled(false);
        } else {
            btnPolarSheetGeneration.setEnabled(true);
        }
    }

    @Override
    protected void makeControlsReactToFillRegattas(List<RegattaDTO> regattas) {
        if (regattas.isEmpty()) {
            btnPolarSheetGeneration.setVisible(false);
        } else {
            btnPolarSheetGeneration.setVisible(true);
            btnPolarSheetGeneration.setEnabled(false);
        }
    }

    /**
     * Changes the state of the generation-start button
     */
    public void changeGenerationButtonState(boolean enable) {
        btnPolarSheetGeneration.setEnabled(enable);
    }

}
