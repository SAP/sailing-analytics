package com.sap.sailing.racecommittee.app.ui.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.racecommittee.app.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.List;

public class PhotoListAdapter extends RecyclerView.Adapter<PhotoListAdapter.ViewHolder> {

    private List<Uri> mUris;
    private float mHeight;
    private SimpleDateFormat mDateFormat;
    private Context mContext;

    public PhotoListAdapter(List<Uri> uriList) {
        mUris = uriList;
    }

    @Override
    public PhotoListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_list_item, parent, false);

        mContext = parent.getContext();
        mHeight = parent.getMeasuredHeight();
        mDateFormat = new SimpleDateFormat("dd.MM.yyyy / kk:mm:ss", parent.getResources().getConfiguration().locale);

        return new ViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(PhotoListAdapter.ViewHolder holder, int position) {
        String fileName = mUris.get(position).getEncodedPath();

        File file = new File(fileName);

        if (holder.dateTime != null) {
            holder.dateTime.setText(mDateFormat.format(file.lastModified()));
        }
        if (holder.imageView != null) {
            Bitmap bitmap = BitmapHelper.decodeSampleBitmapFromFile(fileName, (int) mHeight, (int) mHeight, null);
            holder.imageView.setImageBitmap(bitmap);
        }
        if (holder.deleteView != null) {
            holder.deleteView.setOnClickListener(new DeleteListener(this, mContext, mUris, file));
        }

        holder.itemView.setTag(fileName);
    }

    @Override
    public int getItemCount() {
        return mUris.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView dateTime;
        public ImageView imageView;
        public View deleteView;

        public ViewHolder(View itemView) {
            super(itemView);

            dateTime = ViewHelper.get(itemView, R.id.date_time);
            imageView = ViewHelper.get(itemView, R.id.photo_view);
            deleteView = ViewHelper.get(itemView, R.id.delete_photo);
        }
    }

    private static class DeleteListener implements View.OnClickListener {

        private final WeakReference<RecyclerView.Adapter<PhotoListAdapter.ViewHolder>> weakAdapter;
        private final WeakReference<Context> weakContext;
        private final WeakReference<List<Uri>> weakList;

        private final File file;

        public DeleteListener(RecyclerView.Adapter<PhotoListAdapter.ViewHolder> adapter, Context context,
                List<Uri> uris, File file) {
            weakAdapter = new WeakReference<>(adapter);
            weakContext = new WeakReference<>(context);
            weakList = new WeakReference<>(uris);
            this.file = file;
        }

        @Override
        public void onClick(View v) {
            Context context = weakContext.get();
            if (context != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getString(R.string.delete_file_title));
                builder.setMessage(context.getString(R.string.delete_file));
                builder.setNegativeButton(android.R.string.no, null);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RecyclerView.Adapter<PhotoListAdapter.ViewHolder> adapter = weakAdapter.get();
                        List<Uri> uris = weakList.get();
                        if (adapter != null && uris != null) {
                            if (uris.remove(Uri.fromFile(file))) {
                                if (file.delete()) {
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                    }
                });
                builder.create().show();
            }
        }
    }
}
