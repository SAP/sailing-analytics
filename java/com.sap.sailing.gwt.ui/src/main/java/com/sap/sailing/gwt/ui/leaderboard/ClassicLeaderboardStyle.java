package com.sap.sailing.gwt.ui.leaderboard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ImageResourceRenderer;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardPanel.LeaderBoardStyle;
import com.sap.sse.gwt.client.shared.components.ComponentResources;

public class ClassicLeaderboardStyle implements LeaderBoardStyle {
    private static final LeaderboardResources resources = GWT.create(LeaderboardResources.class);
    private static final ComponentResources componentResources = GWT.create(ComponentResources.class);
    private static final LeaderboardTableResources tableResources = GWT.create(LeaderboardTableResources.class);

    public LeaderboardResources getResources() {
        return resources;
    }

    public ComponentResources getComponentresources() {
        return componentResources;
    }

    public LeaderboardTableResources getTableresources() {
        return tableResources;
    }

    @Override
    public void renderNationalityFlag(ImageResource nationalityFlagImageResource, SafeHtmlBuilder sb) {
        ImageResourceRenderer renderer = new ImageResourceRenderer();
        sb.append(renderer.render(nationalityFlagImageResource));
    }

    @Override
    public void renderFlagImage(String flagImageURL, SafeHtmlBuilder sb, CompetitorDTO competitor) {
        sb.appendHtmlConstant("<img src=\"" + flagImageURL + "\" width=\"18px\" height=\"12px\" title=\""
                + competitor.getName() + "\"/>");
    }

    @Override
    public void processStyleForTotalNetPointsColumn(String textColor, SafeStylesBuilder ssb) {
        ssb.fontWeight(FontWeight.BOLD);
        ssb.trustedColor(textColor);
    }
    
    @Override
    public String determineBoatColorDivStyle(String competitorColor) {
        return "border-bottom: 2px solid " + competitorColor + ";";
    }
    
    @Override
    public void processStyleForRaceColumnWithoutReasonForMaxPoints(boolean isDiscarded, SafeStylesBuilder ssb) {
        if (isDiscarded) {
            ssb.opacity(0.5d);
        } else {
            ssb.fontWeight(FontWeight.BOLD);
        }
    }

    @Override
    public void afterConstructorHook(FlowPanel contentPanel, LeaderboardPanel leaderboardPanel) {
    }

    @Override
    public void afterLeaderboardUpdate(LeaderboardDTO leaderboard) {
    }

    @Override
    public boolean preUpdateToolbarHook(LeaderboardDTO leaderboard) {
        return true;
    }

    @Override
    public boolean hasRaceColumns() {
        return true;
    }

}
