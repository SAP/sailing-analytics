package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.impl.CourseListDataElementWithIdImpl;
import com.sap.sailing.racecommittee.app.ui.adapters.coursedesign.CourseMarkAdapter;
import com.sap.sailing.racecommittee.app.ui.utils.ESSMarkImageHelper;
import com.sap.sailing.racecommittee.app.ui.views.decoration.ItemStrokeDecoration;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;

import java.util.ArrayList;

public class CourseMarksDialogFragment extends DialogFragment {

    private final static String MARKS = "marks";
    private final static String TYPE = "type";
    private final static String ELEMENT = "element";

    private RecyclerView mMarkGrid;
    private CourseMarkAdapter mMarkAdapter;
    private ArrayList<Mark> mMarks;
    private CourseMarkAdapter.MarkClick mListener;

    public CourseMarksDialogFragment() {
    }

    public static CourseMarksDialogFragment newInstance(ArrayList<Mark> marks, CourseListDataElementWithIdImpl element, int type) {
        Bundle args = new Bundle();
        args.putSerializable(MARKS, marks);
        args.putInt(TYPE, type);
        args.putSerializable(ELEMENT, element);
        CourseMarksDialogFragment fragment = new CourseMarksDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.course_marks_fragment, container, false);

        mMarkGrid = (RecyclerView) layout.findViewById(R.id.assets);
        if (mMarkGrid != null) {
            if (getArguments() != null) {
                mMarks = (ArrayList<Mark>) getArguments().getSerializable(MARKS);
                GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 3);
                int padding = getActivity().getResources().getDimensionPixelSize(R.dimen.obesity_line);
                int strokeWidth = getActivity().getResources().getDimensionPixelSize(R.dimen.thin_line);
                int color = ThemeHelper.getColor(getActivity(), R.attr.sap_gray_white_20);
                mMarkGrid.setLayoutManager(layoutManager);
                mMarkGrid.addItemDecoration(new ItemStrokeDecoration(padding, strokeWidth, color));

                mMarkAdapter = new CourseMarkAdapter(getActivity(), mMarks, ESSMarkImageHelper.getInstance(getActivity()), getArguments().getInt(TYPE), (CourseListDataElementWithIdImpl) getArguments().getSerializable(ELEMENT));
                mMarkAdapter.setListener(mListener);
                mMarkGrid.setAdapter(mMarkAdapter);
            }
        }

        return layout;
    }

    public void setListener(CourseMarkAdapter.MarkClick listener) {
        mListener = listener;
    }
}
