package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.InMemoryDataStore;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.ui.adapters.coursedesign.CourseElementListAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.coursedesign.CourseListDataElement;
import com.sap.sailing.racecommittee.app.ui.adapters.coursedesign.DraggableCourseElementListAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.coursedesign.MarkGridAdapter;
import com.sap.sailing.racecommittee.app.ui.comparators.NaturalNamedComparator;
import com.sap.sailing.racecommittee.app.utils.ESSMarkImageHelper;

public class ESSCourseDesignDialogFragment extends RaceDialogFragment {
	// private final static String TAG =
	// ESSCourseDesignDialogFragment.class.getName();

	private static int MarksLoaderId = 0;

	private List<Mark> aMarkList;
	private MarkGridAdapter gridAdapter;
	private List<CourseListDataElement> courseElements;
	private List<CourseListDataElement> previousCourseElements;
	private DraggableCourseElementListAdapter courseElementAdapter;
	private CourseElementListAdapter previousCourseElementAdapter;
	private DragSortListView newCourseListView;
	private ListView previousCourseListView;

	private DragSortController dragSortController;

	private Button publishButton;
	private Button unpublishButton;
	private Button takePreviousButton;

	private ReadonlyDataManager dataManager;

	private int dragStartMode = DragSortController.ON_DRAG;
	private boolean removeEnabled = true;
	private int removeMode = DragSortController.FLING_REMOVE;
	private boolean sortEnabled = true;
	private boolean dragEnabled = true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.race_choose_ess_course_design_view,
				container);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getDialog().setTitle(
				getActivity().getText(R.string.course_design_dialog_title));

		dataManager = DataManager.create(getActivity());

		aMarkList = new ArrayList<Mark>();
		courseElements = new ArrayList<CourseListDataElement>();
		previousCourseElements = new ArrayList<CourseListDataElement>();

		gridAdapter = new MarkGridAdapter(getActivity(), aMarkList,
				ESSMarkImageHelper.getInstance());
		courseElementAdapter = new DraggableCourseElementListAdapter(
				getActivity(), courseElements, ESSMarkImageHelper.getInstance());
		previousCourseElementAdapter = new CourseElementListAdapter(
				getActivity(), previousCourseElements,
				ESSMarkImageHelper.getInstance());

		loadMarks();
		fillPreviousCourseElementsWithLastPublishedCourseDesign();

		GridView gridView = (GridView) getView().findViewById(
				R.id.gridViewAssets);
		gridView.setAdapter(gridAdapter);

		gridView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View view,
					int position, long arg3) {
				Mark mark = (Mark) gridAdapter.getItem(position);
				Log.i("CourseDesign", "Grid is clicked at position " + position
						+ " for buoy " + mark.getName());
				onMarkClickedOnGrid(mark);
			}

		});

		newCourseListView = (DragSortListView) getView().findViewById(
				R.id.listViewNewCourse);
		newCourseListView.setAdapter(courseElementAdapter);
		newCourseListView.setDropListener(new DragSortListView.DropListener() {

			@Override
			public void drop(int from, int to) {
				if (from != to) {
					CourseListDataElement item = courseElementAdapter
							.getItem(from);
					courseElementAdapter.remove(item);
					courseElementAdapter.insert(item, to);
				}
			}
		});

		newCourseListView
				.setRemoveListener(new DragSortListView.RemoveListener() {

					@Override
					public void remove(int toBeRemoved) {
						CourseListDataElement item = courseElementAdapter
								.getItem(toBeRemoved);
						courseElementAdapter.remove(item);
					}
				});

		newCourseListView
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> arg0,
							View arg1, int position, long arg3) {
						CourseListDataElement courseElement = courseElementAdapter
								.getItem(position);
						createRoundingDirectionDialog(courseElement);
						return true;
					}
				});

		dragSortController = buildDragSortController(newCourseListView);
		newCourseListView.setFloatViewManager(dragSortController);
		newCourseListView.setOnTouchListener(dragSortController);
		newCourseListView.setDragEnabled(dragEnabled);

		if (getRace().getCourseDesign() != null) {
			fillCourseElementsInList();
		}

		previousCourseListView = (ListView) getView().findViewById(
				R.id.listViewPreviousCourse);
		previousCourseListView.setAdapter(previousCourseElementAdapter);

		takePreviousButton = (Button) getView().findViewById(
				R.id.takePreviousCourseDesignButton);
		takePreviousButton.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				if (!previousCourseElements.isEmpty()) {
					if (!courseElements.isEmpty()) {
						createUsePreviousCourseDialog();
					} else {
						copyPreviousToNewCourseDesign();
					}
				} else {
					Toast.makeText(getActivity(),
							"No course available to copy", Toast.LENGTH_LONG)
							.show();
				}
			}
		});

		publishButton = (Button) getView().findViewById(
				R.id.publishCourseDesignButton);
		publishButton.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				try {
					CourseBase courseData = convertCourseElementsToACourseData();
					sendCourseDataAndDismiss(courseData);
				} catch (IllegalStateException ex) {
					if (ex.getMessage().equals("Gate or Line has no right buoy")) {
						Toast.makeText(
								getActivity(),
								"A right buoy is missing for a gate or line. Please select the right buoy.",
								Toast.LENGTH_LONG).show();
					} else if (ex.getMessage().equals(
							"Each waypoints needs passing Instructions")) {
						Toast.makeText(
								getActivity(),
								"A waypoint has no passing Instructions. Please select the passing side by clicking long on the waypoint.",
								Toast.LENGTH_LONG).show();
					}

				} catch (IllegalArgumentException ex) {
					Toast.makeText(
							getActivity(),
							"The course design has to have at least one waypoint.",
							Toast.LENGTH_LONG).show();
				}
			}

		});

		unpublishButton = (Button) getView().findViewById(
				R.id.unpublishCourseDesignButton);
		unpublishButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				CourseBase emptyCourse = new CourseDataImpl(
						"Unpublished course design");
				sendCourseDataAndDismiss(emptyCourse);
			}
		});
	}

	/**
	 * Creates a DragSortController and thereby defines the behavior of the drag
	 * sort list
	 */
	public DragSortController buildDragSortController(
			DragSortListView dragSortListView) {
		DragSortController controller = new DragSortController(dragSortListView);
		controller.setDragHandleId(R.id.drag_handle);
		controller.setFlingHandleId(R.id.drag_handle);
		controller.setRemoveEnabled(removeEnabled);
		controller.setSortEnabled(sortEnabled);
		controller.setDragInitMode(dragStartMode);
		controller.setRemoveMode(removeMode);
		controller.setBackgroundColor(getActivity().getResources().getColor(
				R.color.welter_medium_blue));
		return controller;
	}

	private void fillPreviousCourseElementsWithLastPublishedCourseDesign() {
		CourseBase lastPublishedCourseDesign = InMemoryDataStore.INSTANCE
				.getLastPublishedCourseDesign();
		if (lastPublishedCourseDesign != null) {
			fillPreviousCourseElementsInList(lastPublishedCourseDesign);
		}
	}

	private void loadMarks() {
		Loader<?> marksLoader = getLoaderManager().restartLoader(
				MarksLoaderId,
				null,
				dataManager.createMarksLoader(getRace(),
						new LoadClient<Collection<Mark>>() {
							@Override
							public void onLoadFailed(Exception reason) {
								Toast.makeText(
										getActivity(),
										String.format("Marks: %s",
												reason.toString()),
										Toast.LENGTH_LONG).show();
							}

							@Override
							public void onLoadSucceded(Collection<Mark> data,
									boolean isCached) {
								onLoadMarksSucceeded(data);
							}
						}));
		// ignore any cached results and force a remote load of the marks,
		// because we always need the most current.
		marksLoader.forceLoad();
	}

	protected void onLoadMarksSucceeded(Collection<Mark> data) {
		aMarkList.clear();
		aMarkList.addAll(data);
		Collections.sort(aMarkList, new NaturalNamedComparator());
		gridAdapter.notifyDataSetChanged();
	}

	private void fillPreviousCourseElementsInList(CourseBase previousCourseData) {
		if (previousCourseData != null) {
			previousCourseElements.clear();
			previousCourseElements
					.addAll(convertCourseDesignToCourseElements(previousCourseData));
			previousCourseElementAdapter.notifyDataSetChanged();
		}
	}

	private void fillCourseElementsInList() {
		courseElements.clear();
		courseElements.addAll(convertCourseDesignToCourseElements(getRace()
				.getCourseDesign()));
		courseElementAdapter.notifyDataSetChanged();
	}

	protected List<CourseListDataElement> convertCourseDesignToCourseElements(
			CourseBase courseData) {
		List<CourseListDataElement> elementList = new ArrayList<CourseListDataElement>();

		for (Waypoint waypoint : courseData.getWaypoints()) {
			ControlPoint controlPoint = waypoint.getControlPoint();

			if (controlPoint instanceof Mark) {
				CourseListDataElement element = new CourseListDataElement();
				element.setLeftMark((Mark) controlPoint);
				element.setPassingInstructions(waypoint
						.getPassingInstructions());
				elementList.add(element);
			} else if (controlPoint instanceof ControlPointWithTwoMarks) {
				ControlPointWithTwoMarks controlPointTwoMarks = (ControlPointWithTwoMarks) controlPoint;
				CourseListDataElement element = new CourseListDataElement();
				element.setLeftMark(controlPointTwoMarks.getLeft());
				element.setRightMark(controlPointTwoMarks.getRight());
				element.setPassingInstructions(waypoint
						.getPassingInstructions());
				elementList.add(element);
			}
		}

		return elementList;
	}

	protected void sendCourseDataAndDismiss(CourseBase courseDesign) {
		getRace().getState().setCourseDesign(courseDesign);
		saveChangedCourseDesignInCache(courseDesign);
		dismiss();
	}

	private void saveChangedCourseDesignInCache(CourseBase courseDesign) {
		if (!Util.isEmpty(courseDesign.getWaypoints())) {
			InMemoryDataStore.INSTANCE
					.setLastPublishedCourseDesign(courseDesign);
		}
	}

	protected CourseBase convertCourseElementsToACourseData()
			throws IllegalStateException, IllegalArgumentException {
		// TODO find a proper name for the highly flexible ESS courses to be
		// shown on the regatta overview page
		CourseBase design = new CourseDataImpl("Course");
		List<Waypoint> waypoints = new ArrayList<Waypoint>();

		for (CourseListDataElement courseElement : courseElements) {
			if ((courseElement.getPassingInstructions().equals(PassingInstruction.Gate) 
					|| courseElement.getPassingInstructions().equals(PassingInstruction.Line))) {
				if (courseElement.getRightMark() != null) {
					String controlPointName = courseElement.getPassingInstructions()+ " "
							+ courseElement.getLeftMark().getName()+ " / "
							+ courseElement.getRightMark().getName();
					ControlPointWithTwoMarks controlPoint = new ControlPointWithTwoMarksImpl(courseElement.getLeftMark(),
							courseElement.getRightMark(), controlPointName);
					Waypoint waypoint = new WaypointImpl(controlPoint, courseElement.getPassingInstructions());

					waypoints.add(waypoint);
				} else if (courseElement.getRightMark() == null) {
					throw new IllegalStateException("Gate or Line has no right buoy");
				}
			} else if (courseElement.getPassingInstructions().equals(
					PassingInstruction.None)) {
				throw new IllegalStateException(
						"Each waypoints needs passing Instructions");
			} else {
				Waypoint waypoint = new WaypointImpl(
						courseElement.getLeftMark(),
						courseElement.getPassingInstructions());
				waypoints.add(waypoint);
			}
		}
		if (waypoints.isEmpty()) {
			throw new IllegalArgumentException(
					"The course design to be published has no waypoints.");
		}
		int i = 0;
		for (Waypoint waypoint : waypoints) {
			design.addWaypoint(i++, waypoint);
		}
		return design;
	}

	private void createUsePreviousCourseDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Use previous course design");
		builder.setMessage("Use previously published course design?");
		builder.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						copyPreviousToNewCourseDesign();
					}
				});
		builder.setNegativeButton(R.string.no,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do nothing
					}
				});
		builder.create();
		builder.show();
	}

	protected void copyPreviousToNewCourseDesign() {
		courseElements.clear();
		courseElements.addAll(previousCourseElements);
		courseElementAdapter.notifyDataSetChanged();
	}

	protected void onMarkClickedOnGrid(Mark mark) {
		if (courseElements.isEmpty()) {
			addNewCourseElementToList(mark);
		} else {
			CourseListDataElement gateCourseElement = getFirstGateOrLineCourseElementWithoutRightMark();
			if (gateCourseElement != null) {
				gateCourseElement.setRightMark(mark);
				courseElementAdapter.notifyDataSetChanged();
			} else {
				addNewCourseElementToList(mark);
			}
		}
	}

	private CourseListDataElement getFirstGateOrLineCourseElementWithoutRightMark() {
		for (CourseListDataElement courseElement : courseElements) {
			if ((courseElement.getPassingInstructions().equals(
					PassingInstruction.Gate) || courseElement
					.getPassingInstructions().equals(PassingInstruction.Line))
					&& courseElement.getRightMark() == null) {
				return courseElement;
			}
		}
		return null;
	}

	private void addNewCourseElementToList(Mark mark) {
		CourseListDataElement courseElement = new CourseListDataElement();
		courseElement.setLeftMark(mark);
		createRoundingDirectionDialog(courseElement);
	}

	private void createRoundingDirectionDialog(
			final CourseListDataElement courseElement) {
		List<String> directions = new ArrayList<String>();
		for (PassingInstruction direction : PassingInstruction.relevantValues()) {
			directions.add(direction.name());
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.pick_a_rounding_direction).setItems(
				R.array.rounding_directions,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int position) {
						PassingInstruction pickedDirection = PassingInstruction
								.relevantValues()[position];
						onRoundingDirectionPicked(courseElement,
								pickedDirection);
					}
				});
		builder.create();
		builder.show();
	}

	protected void onRoundingDirectionPicked(
			CourseListDataElement courseElement,
			PassingInstruction pickedDirection) {
		courseElement.setPassingInstructions(pickedDirection);
		if (!courseElements.contains(courseElement)) {
			courseElements.add(courseElement);
		}
		courseElementAdapter.notifyDataSetChanged();
	}

}
