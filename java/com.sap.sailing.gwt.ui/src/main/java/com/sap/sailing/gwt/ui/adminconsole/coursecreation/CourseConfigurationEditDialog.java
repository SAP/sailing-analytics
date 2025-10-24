package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import java.util.List;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.gwt.ui.adminconsole.CourseManagementWidget;
import com.sap.sailing.gwt.ui.adminconsole.MarkEditDialog;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceMappingDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.ControlPointWithMarkConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.CourseConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.CourseTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.FreestyleMarkConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkPairWithConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkPropertiesBasedMarkConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkPropertiesDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkRoleDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkTemplateBasedMarkConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.RegattaMarkConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.WaypointTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.WaypointWithMarkConfigurationDTO;
import com.sap.sailing.shared.server.SharedSailingData;
import com.sap.sse.common.RepeatablePart;
import com.sap.sse.gwt.client.controls.IntegerBox;
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
 * If the user edits the course configuration in a way incompatible with a previously selected course template, the
 * course template selection is reset so that the user can know that the course is no longer governed by the template.
 * <p>
 * 
 * As long as the course configuration is "in sync" with a selected course template, the mark roles of which the course
 * consists will be displayed for each mark configuration. When the course configuration runs "out of sync" with the
 * course template, the mark role labels disappear.
 * <p>
 * 
 * The center piece of the editing dialog is a sequence of {@link WaypointWithMarkConfigurationDTO}s. In the background,
 * a set of {@link MarkConfigurationDTO}s and the links of those to {@link MarkRoleDTO}s from the optional
 * {@link CourseTemplateDTO} is maintained.
 * <p>
 * 
 * Existing and new regatta marks can be assigned to the mark roles and to additional (spare) mark templates. Missing
 * marks for mark roles will be initialized from the mark templates assigned to the respective mark role and will lead
 * to the respective regatta marks to be created. Optionally, the user may select the spare mark templates from which
 * regatta marks shall be created or to which existing regatta marks shall be associated.
 * <p>
 * 
 * When offering the user the possible assignments to a mark role, the marks linked to the mark templates which in turn
 * act as spares for the mark role to be assigned will be shown at the top of the list.
 * <p>
 * 
 * Warnings may be emitted in case a mark configuration is used in multiple places where the course template does not
 * use the same mark template for those. This suggests the user accidentally used the same mark at incorrect places.
 * <p>
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
 * Possible user interactions:
 * <ul>
 * <li>Select a course template: This initializes the waypoints list from the course template, using the default number
 * of laps (but at least one) if a repeatable part is defined, so that all {@link WaypointTemplateDTO waypoint
 * templates} {@link CourseTemplateDTO#getWaypointTemplates() used in the course template} will show as a
 * {@link WaypointWithMarkConfigurationDTO} exactly once. For all waypoints' mark roles, a default
 * {@link MarkConfigurationDTO mark configuration} will be determined, and all these mark configurations will then be
 * contained in {@link CourseConfigurationDTO#getAllMarks()}, and their mapping to {@link MarkRoleDTO}s is captured in
 * {@link CourseConfigurationDTO#setAssociatedRoles(java.util.HashMap)}. Existing regatta marks will be used in
 * {@link RegattaMarkConfigurationDTO}s if the {@link SharedSailingData} service lists recent usages of those marks for
 * the mark templates that belong to the mark role for which a mark configuration needs to be provided. Should
 * {@link MarkPropertiesDTO}s from the user's inventory have been used recently for any of the remaining mark roles,
 * they will be suggested, newest first, possibly restricted by a tag filter. If neither regatta marks nor mark
 * properties from the user's inventory qualify, a default mark configuration is created based on the
 * {@link MarkTemplateDTO} serving as the {@link CourseTemplateDTO#getDefaultMarkTemplatesForMarkRoles() default} for
 * the role and hence requesting the creation of a regatta mark with its properties defined by the mark template. Extra
 * {@link MarkTemplateDTO}s not being the default for any mark role are shown as "spares." Should the course template
 * contain a repeatable part, the lap count field will be enabled and set to the default number of laps, or 1 in case
 * the default lap count is 0.</li>
 * <li>Switch a mark configuration to an existing regatta mark; regatta marks will show in the selection list sorted by
 * their last usage time point for the mark template(s) associated with the mark role to be filled, from most recently
 * used to not used at all.</li>
 * <li>Use a {@link MarkPropertiesDTO} from the user's inventory to request a new regatta mark with these properties,
 * including the tracking properties defined by the {@link MarkPropertiesDTO} object.</li>
 * <li>Switch to a spare {@link MarkTemplateDTO} which requests the creation of a corresponding regatta mark. (Should we
 * allow for this if a regatta mark already exists that links back to the mark template selected? Or should we then
 * suggest or even force the use of that regatta mark already existing?)</li>
 * <li>Create a "free-style" mark configuration from scratch in the waypoint sequence; the editor may be the same used
 * for creating a new regatta mark, collecting name, short name, color, pattern, shape, and type (see
 * {@link MarkEditDialog}). If the mark configuration refers to a mark role from the course template, the freestyle mark
 * configuration is {@link CourseConfigurationDTO#setAssociatedRoles(java.util.HashMap) linked to the mark role} in the
 * course configuration under edit.</li>
 * <li>Change the number of laps (assuming a course template is selected and the waypoint sequence still conforms to the
 * course template) to any number greater than zero; this will adjust the occurrence of the repeatable part in the
 * sequence of {@link WaypointWithMarkConfigurationDTO}s. While doing so, smart logic needs to apply that when adding
 * one or more laps clones the last lap's waypoint configurations, and when removing one or more laps removes them
 * starting at the last lap, moving backwards. This will probably not work with the
 * {@link RepeatablePart#createSequence(int, Iterable)} method.</li>
 * <li>Add or remove a waypoint; this will disconnect the course configuration from any course template that may still
 * be selected at that time, and the course template selector will switch to "none/empty." The lap count selector will
 * be disabled. All {@link CourseConfigurationDTO#getAssociatedRoles() associations of mark configurations to roles}
 * will be removed. All {@link MarkTemplateBasedMarkConfigurationDTO} mark configurations are replaced by content-wise
 * equivalent {@link FreestyleMarkConfigurationDTO}s. The addition of a waypoint brings up the challenge of now
 * selecting from the {@link ControlPointWithMarkConfigurationDTO}s or allowing the user to create a new one. The user
 * may be presented with a selection of all existing {@link ControlPointWithMarkConfigurationDTO}s coming from all
 * {@link MarkConfigurationDTO}s and the {@link MarkPairWithConfigurationDTO} (TODO this may need at least a getter in
 * the {@link CourseConfigurationDTO} class that collects the mark pairs created/used so far) assembled for the course
 * so far; additionally, users may want to pick from the {@link MarkPropertiesDTO} inventory (creating a
 * {@link MarkPropertiesBasedMarkConfigurationDTO} as the mark configuration), or create a new "free-style" mark (see
 * {@link FreestyleMarkConfigurationDTO}).</li>
 * </ul>
 * <p>
 * 
 * Implications for UI elements required, as well as their behavior:
 * <ul>
 * <li>Each {@link WaypointWithMarkConfigurationDTO} is represented as a line in a grid or table, showing information
 * about the one or two {@link MarkConfigurationDTO}s forming the control point underlying the waypoint, as well as the
 * {@link PassingInstruction}.</li>
 * <li>Each {@link WaypointWithMarkConfigurationDTO} may be removed (delete button?).</li>
 * <li>Between each pair of {@link WaypointWithMarkConfigurationDTO} lines and before the first and after the last, a
 * "+" / "Insert" control is shown, allowing the user to manually add another {@link WaypointWithMarkConfigurationDTO}
 * to the course configuration. (This will most likely, at least temporarily, break the conformance with any course
 * template selected.)</li>
 * <li>Each {@link MarkConfigurationDTO} is displayed as a drop-down list box which contains---ideally sorted into
 * labeled categories and with an iconic representation---the existing regatta marks (sorting those to the top whose
 * optional reference to a mark template fits any mark template linking to the mark role to fill), the
 * {@link MarkTemplateDTO mark templates} taken from {@link CourseTemplateDTO#getDefaultMarkRolesForMarkTemplates()}
 * with the one returned by {@link CourseTemplateDTO#getDefaultMarkTemplatesForMarkRoles()} for the {@link MarkRoleDTO}
 * that the mark is filling sorted to the top; furthermore, all {@link MarkPropertiesDTO}s from the user's inventory,
 * sorted by the {@link SharedSailingData#getUsedMarkProperties(MarkRole)} time points.</li>
 * <li>A tag filter field allows the user to restrict suggestions for {@link MarkPropertiesDTO}s</li>
 * </ul>
 * <p>
 *
 * Further thoughts: compared to our existing AdminConsole-style course editor which first requires users to set up the
 * control points with two marks which can only then be used in new waypoint definitions, we should let users assemble
 * gates and lines dynamically as they go (similar to the "by-marks" course editor from the Race Manager App),
 * remembering and offering for selection those mark pairs that have already been used in the course configuration. So,
 * instead of first having to create the "control point with two marks", when the user select "line" or "gate" (or
 * "offset") the dialog would present existing {@link MarkPairWithConfigurationDTO}s for selection, and alternatively
 * allow for the selection / creation of two mark configurations (all existing regatta mark or free-style or mark
 * properties-based mark configurations, or creating a new mark properties-based mark configuration from the mark
 * properties inventory, or allow for another free-style mark configuration creation). Ideally, we would have a unified
 * way of creating new mark configuration for filling one of the two mark pair's "slots" as well as for the creation of
 * a new single mark configuration.
 * <p>
 *
 * This edit dialog could potentially replace {@link CourseManagementWidget} and its specializations at some point,
 * harmonizing the course editing across at least the AdminConsole. Should we try to evolve
 * {@link CourseManagementWidget} into this? Or shall we start over and when done try to make this usable in the
 * contexts where currently {@link CourseManagementWidget} has been deployed?
 * <p>
 * 
 * Equal {@link MarkConfigurationDTO}s should also be <em>identical</em> in order to have the back-end represent equal
 * mark pairs by the same object. This means that for all regatta {@link MarkDTO}s and inventory
 * {@link MarkPropertiesDTO} and all {@link MarkTemplateDTO}s there must be a canonicalizing cache so that creating a
 * configuration for the same regatta mark results in the <em>same</em> {@link RegattaMarkConfigurationDTO}, etc. For
 * {@link FreestyleMarkConfigurationDTO}s this seems a bit more straightforward because they stand for their own and
 * ultimately lead to the creation of a regatta mark on the server. As such, we should even wonder if it makes sense to
 * distinguish between regatta mark configurations and free-style mark configurations from a user's perspective. We
 * could display the free-style marks to the user just in the same way we show regatta marks, only that those free-style
 * marks that have not yet led to the creation of a regatta mark can still be deleted from the course configuration.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CourseConfigurationEditDialog extends DataEntryDialog<CourseConfigurationDTO> {
    private final TextBox nameBox;
    private final TextBox shortNameBox;
    private final ListBox courseTemplateListBox;
    private final IntegerBox numberOfLapsBox;
    
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
        courseTemplateListBox = createGenericListBox((CourseTemplateDTO courseTemplateDTO)->
            courseTemplateDTO.getName()+(courseTemplateDTO.getShortName()==null?"":(" - "+courseTemplateDTO.getShortName())),
            /* isMultipleSelect */ false);
        nameBox = createTextBox(courseConfigurationToEdit == null ? "" : courseConfigurationToEdit.getName());
        shortNameBox = createTextBox(courseConfigurationToEdit == null ? "" : courseConfigurationToEdit.getShortName());
        numberOfLapsBox = createIntegerBox(courseConfigurationToEdit == null ? null : courseConfigurationToEdit.getNumberOfLaps(), 2);
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
