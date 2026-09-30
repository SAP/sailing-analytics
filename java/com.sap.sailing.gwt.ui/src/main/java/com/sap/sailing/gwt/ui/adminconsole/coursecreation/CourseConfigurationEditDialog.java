package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import java.util.List;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceMappingDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.CourseConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.CourseTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkRoleDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkTemplateDTO;
import com.sap.sse.common.Position;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * A dialog that lets the user edit a {@link CourseConfigurationDTO}. It may be initialized from a server-side
 * {@code CourseConfiguration} which may in turn have been initialized from a {@code Regatta}'s course, or from a
 * {@code CourseTemplate}. At any time the user can select a course template from the library and set up a course with
 * the {@code MarkProperties} from the library, or generate new {@code Mark}s from the {@code MarkTemplate}s coming with
 * the {@code CourseTemplate}. All marks placed in the course configuration may be selected for storing in the
 * "inventory" as {@code MarkProperties} objects for future use.
 * <p>
 * 
 * If the user edits the course configuration in a way incompatible with a previously selected course template,
 * the course template selection is reset so that the user can know that the course is no longer governed by the
 * template.<p>
 * 
 * As long as the course configuration is "in sync" with a selected course template, the mark roles of which the
 * course consists will be displayed for each mark configuration. When the course configuration runs "out of sync"
 * with the course template, the mark role labels disappear.<p>
 * 
 * Existing and new regatta marks can be assigned to the mark roles and to additional (spare) mark templates. Missing
 * marks for mark roles will be initialized from the mark templates assigned to the respective mark role and will lead
 * to the respective regatta marks to be created. Optionally, the user may select the spare mark templates from which
 * regatta marks shall be created or to which existing regatta marks shall be associated.<p>
 * 
 * When offering the user the possible assignments to a mark role, the marks linked to the mark templates which in turn
 * act as spares for the mark role to be assigned will be shown at the top of the list.<p>
 * 
 * Warnings may be emitted in case a mark configuration is used in multiple places where the course template does not
 * use the same mark template for those. This suggests the user accidentally used the same mark at incorrect places.<p>
 * 
 * Positioning information for marks may be provided, either as one or more {@link DeviceMappingDTO} objects (TODO show
 * a QR code that allows a user to bind a device; the binding would, once complete, have to be read from the server to
 * be shown in the UI again) and/or a fixed {@link Position} (TODO let the user pick on a map) indicating the last known
 * position or the position to set for, e.g., a fixed land mark. Other request/response attributes may also be
 * visualized and edited, such as whether a mark configuration shall be stored as {@code MarkProperties} in the user's
 * "inventory" or a {@code MarkRole} with name and short name shall be created in a new {@code CourseTemplate} for the
 * mark defined in the course.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CourseConfigurationEditDialog extends DataEntryDialog<CourseConfigurationDTO> {
    private static class Validator implements DataEntryDialog.Validator<CourseConfigurationDTO> {
        @Override
        public String getErrorMessage(CourseConfigurationDTO valueToValidate) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public CourseConfigurationEditDialog(final SailingServiceAsync sailingService, final StringMessages stringMessages,
            CourseConfigurationDTO courseConfigurationToEdit, List<CourseTemplateDTO> allCourseTemplates,
            List<MarkRoleDTO> allMarkRoles, List<MarkTemplateDTO> allMarkTemplates,
            DialogCallback<CourseConfigurationDTO> callback) {
        super(stringMessages.configureCourse(), stringMessages.configureCourse(), stringMessages.ok(), stringMessages.cancel(),
                new Validator(), callback);
    }

    @Override
    protected Widget getAdditionalWidget() {
        final Grid grid = new Grid(1, 2);
        
        // TODO Auto-generated method stub
        return grid;
    }

    @Override
    protected CourseConfigurationDTO getResult() {
        // TODO Auto-generated method stub
        return null;
    }
}
