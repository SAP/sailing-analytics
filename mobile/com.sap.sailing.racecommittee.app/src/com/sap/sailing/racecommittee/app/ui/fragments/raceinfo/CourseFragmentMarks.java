package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.InMemoryDataStore;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.domain.impl.CourseListDataElementWithIdImpl;
import com.sap.sailing.racecommittee.app.ui.adapters.coursedesign.CourseElementAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.coursedesign.CourseListDataElement;
import com.sap.sailing.racecommittee.app.ui.adapters.coursedesign.CourseMarkAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.dragandswipelist.HolderAwareOnDragListener;
import com.sap.sailing.racecommittee.app.ui.adapters.dragandswipelist.ItemTouchHelperCallback;
import com.sap.sailing.racecommittee.app.ui.comparators.NaturalNamedComparator;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.CourseMarksDialogFragment;
import com.sap.sailing.racecommittee.app.ui.utils.ESSMarkImageHelper;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CourseFragmentMarks extends CourseFragment
        implements CourseMarkAdapter.MarkClick, HolderAwareOnDragListener {

    private static final String EACH_WAYPOINT_NEEDS_PASSING_INSTRUCTIONS = "Each waypoint needs passing instructions";
    private static final String MISSING_SECOND_MARK = "Missing second mark";
    private ReadonlyDataManager mDataManager;
    private ArrayList<CourseListDataElementWithIdImpl> mHistory;
    private ArrayList<CourseListDataElementWithIdImpl> mElements;
    private ArrayList<Mark> mMarks;
    private RecyclerView mHistoryCourse;
    private RecyclerView mCurrentCourse;
    private CourseElementAdapter mHistoryAdapter;
    private CourseElementAdapter mCourseAdapter;
    private CourseMarksDialogFragment mMarksDialog;
    private ItemTouchHelper mItemTouchHelper;
    private int mId;
    private Button mResetButton;
    private Button mUnpublishButton;

    public CourseFragmentMarks() {
        super();
        mId = 0;
        mHistory = new ArrayList<>();
        mElements = new ArrayList<>();
        mMarks = new ArrayList<>();
    }

    public static CourseFragmentMarks newInstance(@START_MODE_VALUES int startMode) {
        CourseFragmentMarks fragment = new CourseFragmentMarks();
        Bundle args = new Bundle();
        args.putInt(START_MODE, startMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @SuppressLint("MissingInflatedId")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_schedule_course_marks, container, false);
        mResetButton = layout.findViewById(R.id.resetCourse);
        mUnpublishButton = layout.findViewById(R.id.unpublishCourse);
        if (mResetButton != null) {
            mResetButton.setOnClickListener(view -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(mResetButton.getText());
                if (mResetButton.getTag() != null) {
                    builder.setMessage(getString(R.string.reset_message_1));
                    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        mElements.clear();
                        mCourseAdapter.notifyDataSetChanged();
                        mResetButton.setText(R.string.reset_state_2);
                        mResetButton.setTag(null);
                    });
                    builder.setNegativeButton(android.R.string.no, null);
                } else {
                    builder.setMessage(getString(R.string.reset_message_2));
                    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (getRace().getCourseDesign() != null && !getString(R.string.unpublished_course)
                                .equals(getRace().getCourseDesign().getName())) {
                            fillCourseElement();
                        } else {
                            copyPreviousToNewCourseDesign();
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, null);
                }
                builder.show();
            });
        }
        mHistoryCourse = layout.findViewById(R.id.previous_course);
        if (mHistoryCourse != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            mHistoryCourse.setLayoutManager(layoutManager);

            mHistoryAdapter = new CourseElementAdapter(getActivity(), mHistory,
                    ESSMarkImageHelper.getInstance(getActivity()), false, this);
            mHistoryCourse.setAdapter(mHistoryAdapter);
        }
        mCurrentCourse = ViewHelper.get(layout, R.id.new_course);
        if (mCurrentCourse != null) {

            mCourseAdapter = new CourseElementAdapter(getActivity(), mElements,
                    ESSMarkImageHelper.getInstance(getActivity()), true, this);
            mCourseAdapter.setDragListener(this);

            mCurrentCourse.setAdapter(mCourseAdapter);
            mCurrentCourse.setLayoutManager(new LinearLayoutManager(getActivity()));

            ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mCourseAdapter);
            mItemTouchHelper = new ItemTouchHelper(callback);
            mItemTouchHelper.attachToRecyclerView(mCurrentCourse);
        }

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDataManager = DataManager.create(getActivity());

        if (getRace().getCourseDesign() != null) {
            fillCourseElement();
        }

        loadMarks();
        fillPreviousCourseElementsWithLastPublishedCourseDesign();

        if (getView() != null) {
            if (getView().findViewById(R.id.course_layout) != null) {
                if (!mHistory.isEmpty() && mElements.isEmpty()) {
                    copyPreviousToNewCourseDesign();
                    if (mResetButton != null) {
                        mResetButton.setText(getString(R.string.reset_state_1));
                        mResetButton.setTag(mResetButton);
                    }
                }
            }

            Button takePrevious = getView().findViewById(R.id.takeHistoryCourse);
            if (takePrevious != null) {
                takePrevious.setOnClickListener(view -> {
                    if (!mHistory.isEmpty()) {
                        if (!mElements.isEmpty()) {
                            createUsePreviousCourseDialog();
                        } else {
                            copyPreviousToNewCourseDesign();
                        }
                    } else {
                        String toastText = getString(R.string.error_no_course_to_copy);
                        Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
                    }
                });
            }

            Button publish = getView().findViewById(R.id.publishCourse);
            if (publish != null) {
                publish.setOnClickListener(view -> {
                    try {
                        CourseBase courseData;
                        if (mElements.isEmpty() && getView().findViewById(R.id.course_layout) != null) {
                            courseData = new CourseDataImpl(getString(R.string.unpublished_course));
                        } else {
                            courseData = convertCourseElementsToACourseData();
                        }
                        sendCourseDataAndDismiss(courseData);
                    } catch (IllegalStateException ex) {
                        if (ex.getMessage().equals(MISSING_SECOND_MARK)) {
                            String toastText = getString(R.string.error_missing_second_mark);
                            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
                        } else if (ex.getMessage().equals(EACH_WAYPOINT_NEEDS_PASSING_INSTRUCTIONS)) {
                            String toastText = getString(R.string.error_missing_passing_instructions);
                            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
                        }
                    } catch (IllegalArgumentException ex) {
                        String toastText = getString(R.string.error_no_way_point);
                        Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (mUnpublishButton != null) {
                mUnpublishButton.setOnClickListener(view -> {
                    CourseBase emptyCourse = new CourseDataImpl(getString(R.string.unpublished_course));
                    sendCourseDataAndDismiss(emptyCourse);
                });
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mHistoryCourse != null) {
            mHistoryCourse.setAdapter(null);
            mHistoryCourse = null;
        }

        if (mCurrentCourse != null) {
            mCurrentCourse.setItemAnimator(null);
            mCurrentCourse.setAdapter(null);
            mCurrentCourse = null;
        }

        if (mCourseAdapter != null) {
            mCourseAdapter = null;
        }

        super.onDestroy();
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    private void loadMarks() {
        Loader<?> marksLoader = getLoaderManager().restartLoader(0, null,
                mDataManager.createMarksLoader(getRace(), new LoadClient<Collection<Mark>>() {
                    @Override
                    public void onLoadFailed(Exception reason) {
                        String toastText = getString(R.string.marks_w_placeholder);
                        Toast.makeText(getActivity(), String.format(toastText, reason.toString()), Toast.LENGTH_LONG)
                                .show();
                    }

                    @Override
                    public void onLoadSucceeded(Collection<Mark> data, boolean isCached) {
                        onLoadMarksSucceeded(data);
                    }
                }));
        marksLoader.forceLoad();
    }

    private void onLoadMarksSucceeded(Collection<Mark> data) {
        mMarks.clear();
        mMarks.addAll(data);
        Collections.sort(mMarks, new NaturalNamedComparator<>());
    }

    private void fillCourseElement() {
        mElements.clear();
        mElements.addAll(convertCourseDesignToCourseElements(getRace().getCourseDesign()));
        mCourseAdapter.notifyDataSetChanged();
    }

    protected List<CourseListDataElementWithIdImpl> convertCourseDesignToCourseElements(CourseBase courseData) {
        List<CourseListDataElementWithIdImpl> elementList = new ArrayList<>();
        for (Waypoint waypoint : courseData.getWaypoints()) {
            ControlPoint controlPoint = waypoint.getControlPoint();
            if (controlPoint instanceof Mark) {
                CourseListDataElementWithIdImpl element = new CourseListDataElementWithIdImpl();
                element.setId(mId);
                element.setLeftMark((Mark) controlPoint);
                element.setPassingInstructions(waypoint.getPassingInstructions());
                elementList.add(element);
            } else if (controlPoint instanceof ControlPointWithTwoMarks) {
                ControlPointWithTwoMarks controlPointTwoMarks = (ControlPointWithTwoMarks) controlPoint;
                CourseListDataElementWithIdImpl element = new CourseListDataElementWithIdImpl();
                element.setId(mId);
                element.setLeftMark(controlPointTwoMarks.getLeft());
                element.setRightMark(controlPointTwoMarks.getRight());
                element.setPassingInstructions(waypoint.getPassingInstructions());
                elementList.add(element);
            }
            mId++;
        }
        return elementList;
    }

    public void onItemEditClick(int type, CourseListDataElementWithIdImpl element) {
        createPassingInstructionDialog(element);
        mCourseAdapter.notifyDataSetChanged();
    }

    public void showMarkDialog(int type, CourseListDataElementWithIdImpl element) {
        mMarksDialog = CourseMarksDialogFragment.newInstance(mMarks, element, type);
        mMarksDialog.setListener(this);
        mMarksDialog.show(requireFragmentManager(), "course_marks");
    }

    @Override
    public void onItemClick(Mark mark, int type, CourseListDataElementWithIdImpl element) {
        switch (type) {
            case CourseElementAdapter.TOUCH_LEFT_AREA:
                element.setLeftMark(mark);
                break;

            case CourseElementAdapter.TOUCH_RIGHT_AREA:
                element.setRightMark(mark);
                break;

            default:
                if (mElements.isEmpty()) {
                    addNewCourseElementToList(mark);
                } else {
                    CourseListDataElement twoMarksCourseElement = getFirstTwoMarksCourseElementWithoutRightMark();
                    if (twoMarksCourseElement != null) {
                        twoMarksCourseElement.setRightMark(mark);
                    } else {
                        addNewCourseElementToList(mark);
                    }
                }
                break;
        }
        mCourseAdapter.notifyDataSetChanged();
        if (mMarksDialog.isVisible()) {
            mMarksDialog.dismiss();
        }
    }

    private CourseListDataElement getFirstTwoMarksCourseElementWithoutRightMark() {
        for (CourseListDataElement courseElement : mElements) {
            if ((courseElement.getPassingInstructions().equals(PassingInstruction.Gate)
                    || courseElement.getPassingInstructions().equals(PassingInstruction.Line)
                    || courseElement.getPassingInstructions().equals(PassingInstruction.Offset))
                    && courseElement.getRightMark() == null) {
                return courseElement;
            }
        }
        return null;
    }

    private void addNewCourseElementToList(Mark mark) {
        CourseListDataElementWithIdImpl courseElement = new CourseListDataElementWithIdImpl();
        courseElement.setId(mId);
        courseElement.setLeftMark(mark);
        createPassingInstructionDialog(courseElement);
        mId++;
        setResetButton();
    }

    private void setResetButton() {
        if (mResetButton != null && mResetButton.getTag() != null) {
            mResetButton.setText(R.string.reset_state_1);
            mResetButton.setTag(null);
        }
    }

    private void createPassingInstructionDialog(final CourseListDataElementWithIdImpl courseElement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final PassingInstruction[] passingInstructionsRelevantForUserEntry = PassingInstruction.relevantValues();
        final CharSequence[] i18NPassingInstructions = getI18NPassingInstructions(
                passingInstructionsRelevantForUserEntry);
        builder.setTitle(R.string.pick_a_rounding_direction).setItems(i18NPassingInstructions, (dialog, position) -> {
            PassingInstruction pickedDirection = passingInstructionsRelevantForUserEntry[position];
            onPassingInstructionPicked(courseElement, pickedDirection);
        });
        builder.create().show();
    }

    /**
     * Cosntructs a message text for each of the {@link PassingInstruction} values passed. The message strings returned
     * correspond in their order with the {@link PassingInstruction}s passed in the array.
     */
    private CharSequence[] getI18NPassingInstructions(PassingInstruction[] passingInstructionsRelevantForUserEntry) {
        final CharSequence[] result = new CharSequence[passingInstructionsRelevantForUserEntry.length];
        int i = 0;
        for (final PassingInstruction passingInstruction : passingInstructionsRelevantForUserEntry) {
            result[i++] = getI18NPassingInstruction(passingInstruction);
        }
        return result;
    }

    private CharSequence getI18NPassingInstruction(PassingInstruction passingInstruction) {
        CharSequence result = "";
        switch (passingInstruction) {
            case FixedBearing:
                result = getString(R.string.passing_instruction_fixed_bearing);
                break;
            case Gate:
                result = getString(R.string.passing_instruction_gate);
                break;
            case Line:
                result = getString(R.string.passing_instruction_line);
                break;
            case None:
                result = getString(R.string.passing_instruction_none);
                break;
            case Offset:
                result = getString(R.string.passing_instruction_offset);
                break;
            case Port:
                result = getString(R.string.passing_instruction_port);
                break;
            case Single_Unknown:
                result = getString(R.string.passing_instruction_single_unknown);
                break;
            case Starboard:
                result = getString(R.string.passing_instruction_starboard);
                break;
        }
        return result;
    }

    protected void onPassingInstructionPicked(CourseListDataElementWithIdImpl courseElement,
                                              PassingInstruction pickedDirection) {
        courseElement.setPassingInstructions(pickedDirection);
        if (!PassingInstruction.Gate.equals(pickedDirection) && !PassingInstruction.Line.equals(pickedDirection)) {
            courseElement.setRightMark(null);
        }
        if (!mElements.contains(courseElement)) {
            mElements.add(courseElement);
        }
        mCourseAdapter.notifyDataSetChanged();
    }

    private void fillPreviousCourseElementsWithLastPublishedCourseDesign() {
        CourseBase lastPublishedCourseDesign = InMemoryDataStore.INSTANCE
                .getLastPublishedCourseDesign(getRace().getIdentifier().getRaceGroup());
        if (lastPublishedCourseDesign != null) {
            fillPreviousCourseElementsInList(lastPublishedCourseDesign);
        }
    }

    private void fillPreviousCourseElementsInList(CourseBase previousCourseData) {
        if (previousCourseData != null) {
            mHistory.clear();
            mHistory.addAll(convertCourseDesignToCourseElements(previousCourseData));
            if (mHistoryAdapter != null) {
                mHistoryAdapter.notifyDataSetChanged();
            }
        }
    }

    private void createUsePreviousCourseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.use_previous_course_dialog_title));
        builder.setMessage(R.string.use_previous_course_dialog_message);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> copyPreviousToNewCourseDesign());
        builder.setNegativeButton(R.string.no, (dialog, which) -> {
            // do nothing
        });
        builder.create().show();
    }

    protected void copyPreviousToNewCourseDesign() {
        mElements.clear();
        mElements.addAll(mHistory);
        mCourseAdapter.notifyDataSetChanged();
    }

    protected CourseBase convertCourseElementsToACourseData() throws IllegalStateException, IllegalArgumentException {
        // TODO find a proper name for the highly flexible ESS courses to be shown on the regatta overview page
        CourseBase design = new CourseDataImpl("Course");
        List<Waypoint> waypoints = new ArrayList<>();

        for (CourseListDataElement courseElement : mElements) {
            if ((courseElement.getPassingInstructions().equals(PassingInstruction.Gate)
                    || courseElement.getPassingInstructions().equals(PassingInstruction.Line)
                    || courseElement.getPassingInstructions().equals(PassingInstruction.Offset))) {
                if (courseElement.getRightMark() != null) {
                    String cpwtmName = "ControlPointWithTwoMarks " + courseElement.getLeftMark().getName() + " / "
                            + courseElement.getRightMark().getName();
                    // Not providing a UUID for the new control point; instead, the name will be used as a (temporary?)
                    // ID.
                    ControlPointWithTwoMarks cpwtm = new ControlPointWithTwoMarksImpl(courseElement.getLeftMark(),
                            courseElement.getRightMark(), cpwtmName);
                    Waypoint waypoint = new WaypointImpl(cpwtm, courseElement.getPassingInstructions());
                    waypoints.add(waypoint);
                } else {
                    throw new IllegalStateException(MISSING_SECOND_MARK);
                }
            } else if (courseElement.getPassingInstructions().equals(PassingInstruction.None)) {
                throw new IllegalStateException(EACH_WAYPOINT_NEEDS_PASSING_INSTRUCTIONS);
            } else {
                Waypoint waypoint = new WaypointImpl(courseElement.getLeftMark(),
                        courseElement.getPassingInstructions());

                waypoints.add(waypoint);
            }

        }
        if (waypoints.isEmpty()) {
            throw new IllegalArgumentException("The course design to be published has no waypoints.");
        }

        int i = 0;
        for (Waypoint waypoint : waypoints) {
            design.addWaypoint(i++, waypoint);
        }

        return design;
    }

    private void saveChangedCourseDesignInCache(CourseBase courseDesign) {
        if (!Util.isEmpty(courseDesign.getWaypoints())) {
            RaceGroup raceGroup = getRace().getRaceGroup();
            InMemoryDataStore.INSTANCE.setLastPublishedCourseDesign(raceGroup, courseDesign);
        }
    }

    protected void sendCourseDataAndDismiss(CourseBase courseDesign) {
        getRaceState().setCourseDesign(MillisecondsTimePoint.now(), courseDesign, CourseDesignerMode.BY_MARKS);
        saveChangedCourseDesignInCache(courseDesign);
        final Bundle args = getArguments();
        final int startMode = args != null ? args.getInt(START_MODE, START_MODE_PRESETUP) : START_MODE_PRESETUP;
        switch (startMode) {
            case START_MODE_PRESETUP:
                openMainScheduleFragment();
                break;
            case START_MODE_PLANNED:
                sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                break;
        }
    }

    public void onItemRemoved() {
        setResetButton();
    }
}