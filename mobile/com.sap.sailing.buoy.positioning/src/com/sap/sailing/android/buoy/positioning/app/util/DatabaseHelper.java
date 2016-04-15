package com.sap.sailing.android.buoy.positioning.app.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;

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
        if (lc != null) {
            if (lc.moveToFirst()) {
                leaderboard.rowId = lc.getInt(lc.getColumnIndex(BaseColumns._ID));
                leaderboard.name = lc.getString(lc.getColumnIndex(Leaderboard.LEADERBOARD_NAME));
                leaderboard.serverUrl = lc.getString(lc.getColumnIndex(Leaderboard.LEADERBOARD_SERVER_URL));
            }

            lc.close();
        }

        return leaderboard;
    }

    public CheckinUrlInfo getCheckinUrl(Context context, String checkinDigest) {
        CheckinUrlInfo checkinUrlInfo = new CheckinUrlInfo();
        checkinUrlInfo.checkinDigest = checkinDigest;

        Cursor uc = context.getContentResolver().query(CheckinUri.CONTENT_URI, null,
            CheckinUri.CHECKIN_URI_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest }, null);
        if (uc != null) {
            if (uc.moveToFirst()) {
                checkinUrlInfo.rowId = uc.getInt(uc.getColumnIndex(BaseColumns._ID));
                checkinUrlInfo.urlString = uc.getString(uc.getColumnIndex(CheckinUri.CHECKIN_URI_VALUE));
            }

            uc.close();
        }

        return checkinUrlInfo;
    }

    public List<MarkInfo> getMarks(Context context, String checkinDigest) {
        List<MarkInfo> marks = new ArrayList<>();
        Cursor mc = context.getContentResolver().query(Mark.CONTENT_URI, null,
            Mark.MARK_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest }, null);
        if (mc != null) {
            mc.moveToFirst();
            while (!mc.isAfterLast()) {
                final String markName = mc.getString((mc.getColumnIndex(Mark.MARK_NAME)));
                final String markIdAsString = mc.getString(mc.getColumnIndex(Mark.MARK_ID));
                final Serializable markId = Helpers.tryUuidConversion(markIdAsString);
                MarkInfo markInfo = new MarkInfo(markId, markName,
                        mc.getString((mc.getColumnIndex(Mark.MARK_CLASS_NAME))),
                        checkinDigest);
                marks.add(markInfo);
                mc.moveToNext();
            }
            mc.close();
        }
        return marks;
    }

    public List<MarkPingInfo> getMarkPings(Context context, String markID) {
        List<MarkPingInfo> marks = new ArrayList<>();
        Cursor mpc = context.getContentResolver().query(MarkPing.CONTENT_URI, null,
            MarkPing.MARK_ID + " = ?", new String[] { markID }, MarkPing.MARK_PING_TIMESTAMP + " DESC");
        if (mpc != null) {
            mpc.moveToFirst();
            while (!mpc.isAfterLast()) {
                long timeStamp = Long.parseLong(mpc.getString((mpc.getColumnIndex(MarkPing.MARK_PING_TIMESTAMP))));
                double longitude = Double.parseDouble(mpc.getString((mpc.getColumnIndex(MarkPing.MARK_PING_LONGITUDE))));
                double latitude = Double.parseDouble(mpc.getString((mpc.getColumnIndex(MarkPing.MARK_PING_LATITUDE))));
                MarkPingInfo markPingInfo = new MarkPingInfo(markID, GPSFixImpl.create(longitude, latitude, timeStamp),
                        mpc.getDouble((mpc.getColumnIndex(MarkPing.MARK_PING_ACCURACY))));
                marks.add(markPingInfo);
                mpc.moveToNext();
            }
            mpc.close();
        }
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
            deletePingsFromDataBase(context, mark.getId().toString());
        }

        int d2 = cr.delete(Leaderboard.CONTENT_URI, Leaderboard.LEADERBOARD_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest });
        int d3 = cr.delete(Mark.CONTENT_URI, Mark.MARK_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest });
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
     * @param context android context
     * @param markList the list of marks to be stored
     * @param leaderboard leaderboard to be stored
     * @param checkinURL the checkin url to be stored
     * @param pings the list of mark pings to be stored
     * @throws GeneralDatabaseHelperException
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

        ArrayList<ContentProviderOperation> opList = new ArrayList<>();

        // marks
        for (MarkInfo mark : markList) {
            ContentValues cmv = new ContentValues();
            cmv.put(Mark.MARK_CHECKIN_DIGEST, mark.getCheckinDigest());
            cmv.put(Mark.MARK_ID, mark.getId().toString());
            cmv.put(Mark.MARK_NAME, mark.getName());
            cmv.put(Mark.MARK_TYPE, mark.getType().toString());
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

    private void deleteMark(Context context, MarkInfo markInfo) {
        ContentResolver cr = context.getContentResolver();
        cr.delete(Mark.CONTENT_URI, Mark.MARK_ID + " = ?", new String[] { markInfo.getId().toString() });

        if (BuildConfig.DEBUG) {
            ExLog.i(context, TAG, "Deleted mark with id: " + markInfo.getId());
        }
    }

    public void updateMarks(Context context, List<MarkInfo> markList, LeaderboardInfo leaderboard) throws GeneralDatabaseHelperException {
        ContentResolver cr = context.getContentResolver();

        // Update Leaderboard
        ContentValues leaderboardValues = new ContentValues();
        leaderboardValues.put(Leaderboard.LEADERBOARD_NAME, leaderboard.name);
        leaderboardValues.put(Leaderboard.LEADERBOARD_SERVER_URL, leaderboard.serverUrl);
        leaderboardValues.put(Leaderboard.LEADERBOARD_CHECKIN_DIGEST, leaderboard.checkinDigest);
        cr.update(Leaderboard.CONTENT_URI, leaderboardValues, Leaderboard.LEADERBOARD_CHECKIN_DIGEST + " = ?", new String[] {leaderboard.checkinDigest});
        List<MarkInfo> currentMarks = getMarks(context, leaderboard.checkinDigest);
        // Delete marks which where deleted on the server
        for (MarkInfo currentMark : currentMarks){
            if (!markList.contains(currentMark)) {
                deleteMark(context, currentMark);
            }
        }
        // Update marks
        for (MarkInfo mark : markList) {
            ContentValues cmv = new ContentValues();
            cmv.put(Mark.MARK_CHECKIN_DIGEST, mark.getCheckinDigest());
            cmv.put(Mark.MARK_ID, mark.getId().toString().toString());
            cmv.put(Mark.MARK_NAME, mark.getName());
            cmv.put(Mark.MARK_TYPE, mark.getType().toString());
            cmv.put(Mark.MARK_CLASS_NAME, mark.getClassName());
            if (!dataBaseContainsMark(context, mark)) {
                cr.insert(Mark.CONTENT_URI, cmv);
            } else {
                cr.update(Mark.CONTENT_URI, cmv, Mark.MARK_ID + " = ?", new String[] { mark.getId().toString() });
            }
        }
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(new Intent(context.getString(R.string.database_changed)));
    }

    private boolean dataBaseContainsMark(Context context, MarkInfo mark) {
        Cursor contentResolver = context.getContentResolver().query(Mark.CONTENT_URI, null, Mark.MARK_CHECKIN_DIGEST + " = ?" +
            " AND " + Mark.MARK_ID + " = ? ", new String[] { mark.getCheckinDigest(), mark.getId().toString() }, "");
        int count = 0;
        if (contentResolver != null) {
            count = contentResolver.getCount();

            contentResolver.close();
        }
        return count != 0;
    }

    public void storeMarkPing(Context context, MarkPingInfo markPing) throws GeneralDatabaseHelperException {
        ContentResolver cr = context.getContentResolver();
        deletePingsFromDataBase(context, markPing.getMarkId().toString());
        ArrayList<ContentProviderOperation> opList = new ArrayList<>();
        ContentValues mpcv = new ContentValues();
        mpcv.put(MarkPing.MARK_ID, markPing.getMarkId().toString());
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

    public boolean markLeaderboardCombinationAvailable(Context context, String checkinDigest) {

        Cursor lc = context.getContentResolver().query(Leaderboard.CONTENT_URI, null,
            Leaderboard.LEADERBOARD_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest }, null);
        int count = 0;
        if (lc != null) {
            count = lc.getCount();

            lc.close();
        }
        return count == 0;
    }

    public class GeneralDatabaseHelperException extends Exception {
        private static final long serialVersionUID = 4333494334720305541L;

        public GeneralDatabaseHelperException(String message) {
            super(message);
        }
    }

}
