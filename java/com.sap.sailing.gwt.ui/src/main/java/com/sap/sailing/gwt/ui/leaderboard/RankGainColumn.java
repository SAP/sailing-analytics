package com.sap.sailing.gwt.ui.leaderboard;

import com.google.gwt.cell.client.AbstractSafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.client.ui.ImageResourceRenderer;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.ui.shared.LeaderboardRowDTO;

public class RankGainColumn extends LegDetailColumn<Integer, Integer> {
    private static final LeaderboardResources leaderboardResources = GWT.create(LeaderboardResources.class);
	
    public RankGainColumn(String title, LegDetailField<Integer> field, String headerStyle, String columnStyle) {
        super(title, null, field, new RankGainCell(), DetailType.RANK_GAIN.getDefaultSortingOrder(), headerStyle, columnStyle);
    }

    @Override
    public Integer getValue(LeaderboardRowDTO row) {
        return getField().get(row);
    }

    private static class RankGainCell extends AbstractSafeHtmlCell<Integer> {
        public RankGainCell() {
            super(new SafeHtmlRenderer<Integer>() {
                @Override
                public SafeHtml render(Integer rank) {
                    SafeHtmlBuilder builder = new SafeHtmlBuilder();
                    render(rank, builder);
                    return builder.toSafeHtml();
                }

                @Override
                public void render(Integer rankDelta, SafeHtmlBuilder builder) {
                    if (rankDelta != null) {
                        builder.append(Math.abs(rankDelta));
                        builder.appendHtmlConstant("&nbsp;");
                        ImageResourceRenderer imgRenderer = new ImageResourceRenderer();
                        if (rankDelta < 0) {
                        	builder.append(imgRenderer.render(leaderboardResources.arrowGainIcon()));
                        } else if (rankDelta > 0) {
                        	builder.append(imgRenderer.render(leaderboardResources.arrowLossIcon()));
                        } else {
                        	builder.append(imgRenderer.render(leaderboardResources.arrowGainLossIcon()));
                        }
                    }
                }
            });
        }

        @Override
        protected void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml data, SafeHtmlBuilder sb) {
            if (data != null) {
                sb.append(data);
            }
        }
    }
}
