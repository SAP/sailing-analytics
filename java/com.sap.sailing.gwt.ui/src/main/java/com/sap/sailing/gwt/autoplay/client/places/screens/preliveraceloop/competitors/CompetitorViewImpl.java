package com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.competitors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.leaderboard.PreLiveRaceLeaderBoardWithImageViewImpl.ImageProvider;

public class CompetitorViewImpl extends ResizeComposite {
    private static CompetitorViewImplUiBinder uiBinder = GWT.create(CompetitorViewImplUiBinder.class);

    interface CompetitorViewImplUiBinder extends UiBinder<Widget, CompetitorViewImpl> {
    }

    @UiField
    Image image1;
    @UiField
    Label subline1;
    @UiField
    Label subline2;

    public CompetitorViewImpl(ImageProvider provider, CompetitorWithBoatDTO competitor) {
        initWidget(uiBinder.createAndBindUi(this));
        if (competitor.getBoat() != null && competitor.getBoat().getName() != null) {
            subline1.setText(competitor.getBoat().getName());
        } else {
            subline1.setText("");
        }

        if (competitor.getName() != null) {
            subline2.setText(competitor.getName());
        } else {
            subline2.setText("");
        }
        image1.setUrl(provider.getImageUrl(competitor));
    }
}
