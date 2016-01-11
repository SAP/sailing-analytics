package com.sap.sailing.android.buoy.positioning.app.util;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.support.v4.content.LocalBroadcastManager;

import com.sap.sailing.android.buoy.positioning.app.BuildConfig;
import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.provider.AnalyticsContract;
import com.sap.sailing.android.buoy.positioning.app.provider.AnalyticsContract.CheckinUri;
import com.sap.sailing.android.buoy.positioning.app.provider.AnalyticsContract.Leaderboard;
import com.sap.sailing.android.buoy.positioning.app.provider.AnalyticsContract.Mark;
import com.sap.sailing.android.buoy.positioning.app.provider.AnalyticsContract.MarkPing;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.MarkInfo;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.MarkPingInfo;
import com.sap.sailing.android.shared.data.CheckinUrlInfo;
import com.sap.sailing.android.shared.data.LeaderboardInfo;
import com.sap.sailing.android.shared.logging.ExLog;

public class DatabaseHelper {

    private final static String TAG = DatabaseHelper.class.getName();

    protected static DatabaseHelper mInstance;

    public static synchronized DatabaseHelper getInstance() {
        if (mInstance == null) {
            mInstance = new DatabaseHelper();
        }

        return mInstance;
    }

    public LeaderboardInfo getLeaderboard(Context context, String checkinDigest) {
        LeaderboardInfo leaderboard = new LeaderboardInfo();
        leaderboard.checkinDigest = checkinDigest;

        Cursor lc = context.getContentResolver().query(Leaderboard.CONTENT_URI, null,
                Leaderboard.LEADERBOARD_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest }, null);
        if (lc.moveToFirst()) {
            leaderboard.rowId = lc.getInt(lc.getColumnIndex(BaseColumns._ID));
            leaderboard.name = lc.getString(lc.getColumnIndex(Leaderboard.LEADERBOARD_NAME));
            leaderboard.serverUrl = lc.getString(lc.getColumnIndex(Leaderboard.LEADERBOARD_SERVER_URL));
        }

        lc.close();

        return leaderboard;
    }

    public CheckinUrlInfo getCheckinUrl(Context context, String checkinDigest) {
        CheckinUrlInfo checkinUrlInfo = new CheckinUrlInfo();
        checkinUrlInfo.checkinDigest = checkinDigest;

        Cursor uc = context.getContentResolver().query(CheckinUri.CONTENT_URI, null,
                CheckinUri.CHECKIN_URI_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest }, null);
        if (uc.moveToFirst()) {
            checkinUrlInfo.rowId = uc.getInt(uc.getColumnIndex(BaseColumns._ID));
            checkinUrlInfo.urlString = uc.getString(uc.getColumnIndex(CheckinUri.CHECKIN_URI_VALUE));
        }

        uc.close();

        return checkinUrlInfo;
    }

    public List<MarkInfo> getMarks(Context context, String checkinDigest) {
        List<MarkInfo> marks = new ArrayList<MarkInfo>();
        Cursor mc = context.getContentResolver().query(Mark.CONTENT_URI, null,
                Mark.MARK_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest }, null);
        mc.moveToFirst();
        while (!mc.isAfterLast()) {
            MarkInfo markInfo = new MarkInfo();
            markInfo.setCheckinDigest(mc.getString((mc.getColumnIndex(Mark.MARK_CHECKIN_DIGEST))));
            markInfo.setId(mc.getString((mc.getColumnIndex(Mark.MARK_ID))));
            markInfo.setName(mc.getString((mc.getColumnIndex(Mark.MARK_NAME))));
            markInfo.setType(mc.getString((mc.getColumnIndex(Mark.MARK_TYPE))));
            markInfo.setClassName(mc.getString((mc.getColumnIndex(Mark.MARK_CLASS_NAME))));
            marks.add(markInfo);
            mc.moveToNext();
        }
        mc.close();
        return marks;
    }

    public List<MarkPingInfo> getMarkPings(Context context, String markID) {
        List<MarkPingInfo> marks = new ArrayList<MarkPingInfo>();
        Cursor mpc = context.getContentResolver().query(MarkPing.CONTENT_URI, null,
                MarkPing.MARK_ID + " = ?", new String[] { markID }, MarkPing.MARK_PING_TIMESTAMP + " DESC");
        mpc.moveToFirst();
        while (!mpc.isAfterLast()) {
            MarkPingInfo markPingInfo = new MarkPingInfo();
            markPingInfo.setMarkId(markID);
            markPingInfo.setTimestamp(mpc.getInt((mpc.getColumnIndex(MarkPing.MARK_PING_TIMESTAMP))));
            markPingInfo.setLongitude(mpc.getString((mpc.getColumnIndex(MarkPing.MARK_PING_LONGITUDE))));
            markPingInfo.setLatitude(mpc.getString((mpc.getColumnIndex(MarkPing.MARK_PING_LATITUDE))));
            markPingInfo.setAccuracy(mpc.getDouble((mpc.getColumnIndex(MarkPing.MARK_PING_ACCURACY))));
            marks.add(markPingInfo);
            mpc.moveToNext();
        }
        mpc.close();
        return marks;
    }

    public void deletePingsFromDataBase(Context context, String markID) {
        ContentResolver cr = context.getContentResolver();
        int d1 = cr.delete(MarkPing.CONTENT_URI, MarkPing.MARK_ID + " = ?", new String[] { markID });

        if (BuildConfig.DEBUG) {
            ExLog.i(context, TAG, "Checkout, number of markpings for mark: " + markID + " deleted: " + d1);
        }
    }

    public void deleteRegattaFromDatabase(Context context, String checkinDigest) {
        ContentResolver cr = context.getContentResolver();

        List<MarkInfo> marks = getMarks(context, checkinDigest);

        for (MarkInfo mark : marks) {
            deletePingsFromDataBase(context, mark.getId());
        }

        int d2 = cr.delete(Leaderboard.CONTENT_URI, Leaderboard.LEADERBOARD_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest });
        int d3 = cr.delete(Mark.CONTENT_URI, Mark.MARK_CHECKIN_DIGEST + " = \"" + checkinDigest + "\"", null);
        int d4 = cr.delete(CheckinUri.CONTENT_URI, CheckinUri.CHECKIN_URI_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest });

        if (BuildConfig.DEBUG) {
            ExLog.i(context, TAG, "Checkout, number of leaderbards deleted: " + d2);
            ExLog.i(context, TAG, "Checkout, number of marks deleted: " + d3);
            ExLog.i(context, TAG, "Checkout, number of checkinurls deleted: " + d4);
        }
    }

    /**
     * When checking in, store info on the event, the competitor and the leaderboard in the database.
     *
     * @param context
     * @param leaderboard
     * @return success or failure
     * @throws GeneralDatabaseHelperException
     * @throws OperationApplicationException
     * @throws RemoteException
     */
    public void storeCheckinRow(Context context, List<MarkInfo> markList, LeaderboardInfo leaderboard,
            CheckinUrlInfo checkinURL, List<MarkPingInfo> pings) throws GeneralDatabaseHelperException {

        if (BuildConfig.DEBUG) {
            ExLog.i(context, TAG, "New data stored");
        }
        // inserting leaderboard first


        ContentResolver cr = context.getContentResolver();

        ContentValues clv = new ContentValues();
        clv.put(Leaderboard.LEADERBOARD_NAME, leaderboard.name);
        clv.put(Leaderboard.LEADERBOARD_SERVER_URL, leaderboard.serverUrl);
        clv.put(Leaderboard.LEADERBOARD_CHECKIN_DIGEST, leaderboard.checkinDigest);
        cr.insert(Leaderboard.CONTENT_URI, clv);

        // now insert marks

        ArrayList<ContentProviderOperation> opList = new ArrayList<ContentProviderOperation>();

        // marks
        for (MarkInfo mark : markList) {
            ContentValues cmv = new ContentValues();
            cmv.put(Mark.MARK_CHECKIN_DIGEST, mark.getCheckinDigest());
            cmv.put(Mark.MARK_ID, mark.getId());
            cmv.put(Mark.MARK_NAME, mark.getName());
            cmv.put(Mark.MARK_TYPE, mark.getType());
            cmv.put(Mark.MARK_CLASS_NAME, mark.getClassName());

            opList.add(ContentProviderOperation.newInsert(Mark.CONTENT_URI).withValues(cmv).build());
        }

        for (MarkPingInfo ping : pings) {
            storeMarkPing(context, ping);
        }

        // checkin url

        ContentValues ccuv = new ContentValues();

        ccuv.put(CheckinUri.CHECKIN_URI_VALUE, checkinURL.urlString);
        ccuv.put(CheckinUri.CHECKIN_URI_CHECKIN_DIGEST, checkinURL.checkinDigest);
        cr.insert(CheckinUri.CONTENT_URI, ccuv);

        opList.add(ContentProviderOperation.newInsert(CheckinUri.CONTENT_URI).withValues(ccuv).build());

        try {
            cr.applyBatch(AnalyticsContract.CONTENT_AUTHORITY, opList);
            LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(new Intent(context.getString(R.string.database_changed)));

        } catch (RemoteException e) {
            throw new GeneralDatabaseHelperException(e.getMessage());
        } catch (OperationApplicationException e) {
            throw new GeneralDatabaseHelperException(e.getMessage());
        }
    }

    public void storeMarkPing(Context context, MarkPingInfo markPing) throws GeneralDatabaseHelperException {
        ContentResolver cr = context.getContentResolver();
        deletePingsFromDataBase(context, markPing.getMarkId());
        ArrayList<ContentProviderOperation> opList = new ArrayList<ContentProviderOperation>();
        ContentValues mpcv = new ContentValues();
        mpcv.put(MarkPing.MARK_ID, markPing.getMarkId());
        mpcv.put(MarkPing.MARK_PING_LATITUDE, markPing.getLatitude());
        mpcv.put(MarkPing.MARK_PING_LONGITUDE, markPing.getLongitude());
        mpcv.put(MarkPing.MARK_PING_ACCURACY, markPing.getAccuracy());
        mpcv.put(MarkPing.MARK_PING_TIMESTAMP, markPing.getTimestamp());
        opList.add(ContentProviderOperation.newInsert(MarkPing.CONTENT_URI).withValues(mpcv).build());

        try {
            cr.applyBatch(AnalyticsContract.CONTENT_AUTHORITY, opList);
        } catch (RemoteException e) {
            throw new GeneralDatabaseHelperException(e.getMessage());
        } catch (OperationApplicationException e) {
            throw new GeneralDatabaseHelperException(e.getMessage());
        }

    }

    public boolean markLeaderboardCombnationAvailable(Context context, String checkinDigest) {

        Cursor lc = context.getContentResolver().query(Leaderboard.CONTENT_URI, null,
                Leaderboard.LEADERBOARD_CHECKIN_DIGEST + " = \"" + checkinDigest + "\"", null, null);

        int count = lc.getCount();

        lc.close();
        return count == 0;
    }

    public class GeneralDatabaseHelperException extends Exception {
        private static final long serialVersionUID = 4333494334720305541L;

        public GeneralDatabaseHelperException(String message) {
            super(message);
        }
    }

}
