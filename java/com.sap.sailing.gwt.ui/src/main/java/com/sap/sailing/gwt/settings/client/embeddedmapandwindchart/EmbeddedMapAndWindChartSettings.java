package com.sap.sailing.gwt.settings.client.embeddedmapandwindchart;

import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapSettings;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;

public class EmbeddedMapAndWindChartSettings extends AbstractGenericSerializableSettings {

    private static final long serialVersionUID = 272440351202821549L;

    private static final String PARAM_PLAY = "play";
    private static final String PARAM_SHOW_COMPETITORS = "showCompetitors";
    private static final String PARAM_SHOW_ALL_COMPETITORS_OF_EVENT = "showAllCompetitorsOfEvent";
    private static final String PARAM_SHOW_ALL_MARKS_OF_EVENT = "showAllMarksOfEvent";
    private static final String PARAM_SHOW_COURSE_AREA = "showCourseArea";
    private static final String PARAM_SHOW_ALL_COURSE_AREAS_OF_EVENT = "showAllCourseAreasOfEvent";
    

    private transient BooleanSetting play;
    private transient BooleanSetting showCompetitors;
    private transient BooleanSetting showAllCompetitorsOfEvent;
    private transient BooleanSetting showAllMarksOfEvent;
    private transient BooleanSetting showCourseArea;
    private transient BooleanSetting showAllCourseAreasOfEvent;
    private transient BooleanSetting showCourseGeometry;
    private transient BooleanSetting windUp;

    public EmbeddedMapAndWindChartSettings() {
        super();
    }

    public EmbeddedMapAndWindChartSettings(final boolean play) {
        this();
        this.play.setValue(play);;
    }

    @Override
    protected void addChildSettings() {
        play = new BooleanSetting(PARAM_PLAY, this, false);
        showCompetitors = new BooleanSetting(PARAM_SHOW_COMPETITORS, this, false);
        showAllCompetitorsOfEvent = new BooleanSetting(PARAM_SHOW_ALL_COMPETITORS_OF_EVENT, this, false);
        showAllMarksOfEvent = new BooleanSetting(PARAM_SHOW_ALL_MARKS_OF_EVENT, this, false);
        showCourseArea = new BooleanSetting(PARAM_SHOW_COURSE_AREA, this, false);
        showAllCourseAreasOfEvent = new BooleanSetting(PARAM_SHOW_ALL_COURSE_AREAS_OF_EVENT, this, false);
        showCourseGeometry = new BooleanSetting(RaceMapSettings.PARAM_SHOW_COURSE_GEOMETRY, this, true);
        windUp = new BooleanSetting(RaceMapSettings.PARAM_MAP_ORIENTATION_WIND_UP, this, true);
    }

    public boolean isPlay() {
        return play.getValue();
    }
    
    public boolean isShowCompetitors() {
        return showCompetitors.getValue();
    }
    
    public boolean isShowAllCompetitorsOfEvent() {
        return showAllCompetitorsOfEvent.getValue();
    }

    public boolean isShowAllMarksOfEvent() {
        return showAllMarksOfEvent.getValue();
    }

    public boolean isShowCourseArea() {
        return showCourseArea.getValue();
    }

    public boolean isShowAllCourseAreasOfEvent() {
        return showAllCourseAreasOfEvent.getValue();
    }

    public boolean isShowCourseGeometry() {
        return showCourseGeometry.getValue();
    }

    public boolean isWindUp() {
        return windUp.getValue();
    }
}
