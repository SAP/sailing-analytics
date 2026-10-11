package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import java.util.ArrayList;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.impl.CourseListDataElementWithIdImpl;
import com.sap.sailing.racecommittee.app.ui.adapters.coursedesign.CourseMarkAdapter;
import com.sap.sailing.racecommittee.app.ui.utils.ESSMarkImageHelper;
import com.sap.sailing.racecommittee.app.ui.views.decoration.ItemStrokeDecoration;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;

import android.app.Dialog;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    public static CourseMarksDialogFragment newInstance(ArrayList<Mark> marks, CourseListDataElementWithIdImpl element,
            int type) {
        Bundle args = new Bundle();
        args.putSerializable(MARKS, marks);
        args.putInt(TYPE, type);
        args.putSerializable(ELEMENT, element);
        CourseMarksDialogFragment fragment = new CourseMarksDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.course_design_assets);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.course_marks_fragment, container, false);
        mMarkGrid = (RecyclerView) layout.findViewById(R.id.assets);
        if (mMarkGrid != null) {
            if (getArguments() != null) {
                @SuppressWarnings("unchecked")
                final ArrayList<Mark> markList = (ArrayList<Mark>) getArguments().getSerializable(MARKS);
                mMarks = markList;
                GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 3);
                int padding = getActivity().getResources().getDimensionPixelSize(R.dimen.obesity_line);
                int strokeWidth = getActivity().getResources().getDimensionPixelSize(R.dimen.thin_line);
                int color = ThemeHelper.getColor(getActivity(), R.attr.sap_gray_white_20);
                mMarkGrid.setLayoutManager(layoutManager);
                mMarkGrid.addItemDecoration(new ItemStrokeDecoration(padding, strokeWidth, color));

                mMarkAdapter = new CourseMarkAdapter(getActivity(), mMarks,
                        ESSMarkImageHelper.getInstance(getActivity()), getArguments().getInt(TYPE),
                        (CourseListDataElementWithIdImpl) getArguments().getSerializable(ELEMENT));
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
