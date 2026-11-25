package com.sap.sailing.racecommittee.app.ui.adapters.coursedesign;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.impl.CourseListDataElementWithIdImpl;
import com.sap.sailing.racecommittee.app.ui.utils.MarkImageHelper;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class CourseMarkAdapter extends RecyclerView.Adapter<CourseMarkAdapter.ViewHolder> {

    private Context mContext;
    private List<Mark> mMarks;
    private MarkImageHelper mImageHelper;
    private MarkClick mListener;
    private int mType;
    private CourseListDataElementWithIdImpl mElement;

    public CourseMarkAdapter(Context context, ArrayList<Mark> marks, MarkImageHelper imageHelper, int type,
            CourseListDataElementWithIdImpl element) {
        mContext = context;
        mMarks = marks;
        mImageHelper = imageHelper;
        mType = type;
        mElement = element;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout.course_marks_item, parent, false);
        return new ViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Mark mark = mMarks.get(position);

        holder.imageView.setImageDrawable(mImageHelper.resolveMarkImage(mContext, mark));
        holder.textView.setText(mark.getName());
    }

    @Override
    public int getItemCount() {
        if (mMarks != null) {
            return mMarks.size();
        }
        return 0;
    }

    public void setListener(MarkClick listener) {
        mListener = listener;
    }

    public interface MarkClick {
        void onItemClick(Mark mark, int type, CourseListDataElementWithIdImpl element);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textView;
        public ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onItemClick(mMarks.get(getAdapterPosition()), mType, mElement);
                    }
                }
            });
            textView = (TextView) itemView.findViewById(R.id.cell_text);
            imageView = (ImageView) itemView.findViewById(R.id.cell_image);
        }
    }
}
