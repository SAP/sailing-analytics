package com.sap.sailing.android.tracking.app.provider;

import java.util.Arrays;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.SelectionBuilder;
import com.sap.sailing.android.tracking.app.BuildConfig;
import com.sap.sailing.android.tracking.app.provider.AnalyticsContract.Boat;
import com.sap.sailing.android.tracking.app.provider.AnalyticsContract.Checkin;
import com.sap.sailing.android.tracking.app.provider.AnalyticsContract.Competitor;
import com.sap.sailing.android.tracking.app.provider.AnalyticsContract.Event;
import com.sap.sailing.android.tracking.app.provider.AnalyticsContract.Leaderboard;
import com.sap.sailing.android.tracking.app.provider.AnalyticsContract.Mark;
import com.sap.sailing.android.tracking.app.provider.AnalyticsDatabase.Tables;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;

public class AnalyticsProvider extends ContentProvider {

    private static final String TAG = AnalyticsProvider.class.getSimpleName();

    public static final String ALL_JOINED = "leaderboards_events_competitors_marks_joined";

    private AnalyticsDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int COMPETITOR = 100;
    private static final int COMPETITOR_ID = 101;

    private static final int EVENT = 200;
    private static final int EVENT_ID = 201;

    private static final int LEADERBOARD = 300;
    private static final int LEADERBOARD_ID = 301;

    private static final int CHECKIN_URI = 500;
    private static final int CHECKIN_URI_ID = 501;

    private static final int EVENT_LEADERBOARD_COMPETITOR_JOINED = 600;
    private static final int EVENT_LEADERBOARD_MARK_JOINED = 601;

    private static final int EVENT_GPS_FIXES_JOINED = 700;

    private static final int LEADERBOARDS_EVENTS_COMPETITORS_MARKS_BOATS_JOINED = 800;

    private static final int MARK = 900;
    private static final int MARK_ID = 901;

    private static final int BOAT = 1000;
    private static final int BOAT_ID = 1001;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = AnalyticsContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, Tables.COMPETITORS, COMPETITOR);
        matcher.addURI(authority, Tables.COMPETITORS + "/*", COMPETITOR_ID);

        matcher.addURI(authority, Tables.LEADERBOARDS, LEADERBOARD);
        matcher.addURI(authority, Tables.LEADERBOARDS + "/*", LEADERBOARD_ID);

        matcher.addURI(authority, Tables.CHECKIN_URIS, CHECKIN_URI);
        matcher.addURI(authority, Tables.CHECKIN_URIS + "/*", CHECKIN_URI_ID);

        matcher.addURI(authority, Tables.EVENTS, EVENT);
        matcher.addURI(authority, Tables.EVENTS + "/*", EVENT_ID);

        matcher.addURI(authority, "event_leaderboard_competitor_joined", EVENT_LEADERBOARD_COMPETITOR_JOINED);
        matcher.addURI(authority, "event_leaderboard_mark_joined", EVENT_LEADERBOARD_MARK_JOINED);

        matcher.addURI(authority, "event_gps_fix_joined", EVENT_GPS_FIXES_JOINED);

        matcher.addURI(authority, "leaderboards_events_competitors_marks_joined",
                LEADERBOARDS_EVENTS_COMPETITORS_MARKS_BOATS_JOINED);

        matcher.addURI(authority, Tables.MARKS, MARK);
        matcher.addURI(authority, Tables.MARKS + "/*", MARK_ID);

        matcher.addURI(authority, Tables.BOATS, BOAT);
        matcher.addURI(authority, Tables.BOATS + "/*", BOAT_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new AnalyticsDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (BuildConfig.DEBUG) {
            ExLog.i(getContext(), TAG,
                    "query() called with: uri = [" + uri + "], projection = [" + Arrays.toString(projection)
                            + "], selection = [" + selection + "], selectionArgs = [" + Arrays.toString(selectionArgs)
                            + "], sortOrder = [" + sortOrder + "]");
        }

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        String table = null;
        switch (sUriMatcher.match(uri)) {
        case EVENT_LEADERBOARD_MARK_JOINED:
            table = Tables.EVENTS_JOIN_LEADERBOARDS_JOIN_MARKS;
            break;

        case EVENT_LEADERBOARD_COMPETITOR_JOINED:
            table = Tables.EVENTS_JOIN_LEADERBOARDS_JOIN_COMPETITORS;
            break;

        case LEADERBOARDS_EVENTS_COMPETITORS_MARKS_BOATS_JOINED:
            table = Tables.LEADERBOARDS_JOIN_EVENTS_JOIN_COMPETITORS_JOIN_MARKS_JOINS_BOATS;
            break;

        default:
            // np-op
        }

        Cursor cursor;
        if (table != null) {
            builder.setTables(table);
            cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        } else {
            final SelectionBuilder selectionBuilder = buildExpandedSelection(uri);
            cursor = selectionBuilder.where(selection, selectionArgs).query(db, false, projection, sortOrder, null);
        }
        Context context = getContext();
        if (context != null) {
            cursor.setNotificationUri(context.getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case COMPETITOR:
            return Competitor.CONTENT_TYPE;

        case COMPETITOR_ID:
            return Competitor.CONTENT_ITEM_TYPE;

        case EVENT:
            return Event.CONTENT_TYPE;

        case EVENT_ID:
            return Event.CONTENT_ITEM_TYPE;

        case LEADERBOARD:
            return Leaderboard.CONTENT_TYPE;

        case LEADERBOARD_ID:
            return Leaderboard.CONTENT_ITEM_TYPE;

        case CHECKIN_URI:
            return Checkin.CONTENT_TYPE;

        case CHECKIN_URI_ID:
            return Checkin.CONTENT_ITEM_TYPE;

        case MARK:
            return Mark.CONTENT_TYPE;

        case MARK_ID:
            return Mark.CONTENT_ITEM_TYPE;

        case BOAT:
            return Boat.CONTENT_TYPE;

        case BOAT_ID:
            return Boat.CONTENT_ITEM_TYPE;

        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (BuildConfig.DEBUG) {
            String message = "insert: uri=" + uri + " values=[" + values.toString() + "]";
            ExLog.i(getContext(), TAG, message);
        }

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String table;
        switch (sUriMatcher.match(uri)) {
        case COMPETITOR:
            table = Tables.COMPETITORS;
            break;

        case EVENT:
            table = Tables.EVENTS;
            break;

        case LEADERBOARD:
            table = Tables.LEADERBOARDS;
            break;

        case CHECKIN_URI:
            table = Tables.CHECKIN_URIS;
            break;

        case MARK:
            table = Tables.MARKS;
            break;

        case BOAT:
            table = Tables.BOATS;
            break;

        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        long id = db.insertOrThrow(table, null, values);
        notifyChange(AnalyticsContract.LeaderboardsEventsCompetitorsMarksBoatsJoined.CONTENT_URI);
        notifyChange(uri);
        return uri.buildUpon().appendPath(String.valueOf(id)).build();
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        if (BuildConfig.DEBUG) {
            ExLog.i(getContext(), TAG, "delete() called with: uri = [" + uri + "], selection = [" + selection
                    + "], selectionArgs = [" + Arrays.toString(selectionArgs) + "]");
        }

        String table;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (sUriMatcher.match(uri)) {
        case COMPETITOR:
            table = Tables.COMPETITORS;
            break;

        case EVENT:
            table = Tables.EVENTS;
            break;

        case LEADERBOARD:
            table = Tables.LEADERBOARDS;
            break;

        case CHECKIN_URI:
            table = Tables.CHECKIN_URIS;
            break;

        case MARK:
            table = Tables.MARKS;
            break;

        case BOAT:
            table = Tables.BOATS;
            break;

        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        int rowsDeleted = db.delete(table, selection, selectionArgs);
        notifyChange(AnalyticsContract.LeaderboardsEventsCompetitorsMarksBoatsJoined.CONTENT_URI);
        notifyChange(uri);
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (BuildConfig.DEBUG) {
            ExLog.i(getContext(), TAG, "update() called with: uri = [" + uri + "], values = [" + values
                    + "], selection = [" + selection + "], selectionArgs = [" + Arrays.toString(selectionArgs) + "]");
        }

        switch (sUriMatcher.match(uri)) {
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    private void notifyChange(Uri uri) {
        if (getContext() != null) {
            ContentResolver cr = getContext().getContentResolver();
            if (cr != null) {
                cr.notifyChange(uri, null);
            }
        }
    }

    private SelectionBuilder buildExpandedSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder(getContext());

        switch (sUriMatcher.match(uri)) {
        case COMPETITOR:
            return builder.table(Tables.COMPETITORS);

        case COMPETITOR_ID:
            return builder.table(Tables.COMPETITORS).where(Competitor.COMPETITOR_ID + " = ?", uri.getLastPathSegment());

        case LEADERBOARD:
            return builder.table(Tables.LEADERBOARDS);

        case LEADERBOARD_ID:
            return builder.table(Tables.LEADERBOARDS).where(BaseColumns._ID + " = ?", uri.getLastPathSegment());

        case EVENT:
            return builder.table(Tables.EVENTS);

        case EVENT_ID:
            return builder.table(Tables.EVENTS).where(Event.EVENT_ID + " = ?", uri.getLastPathSegment());

        case CHECKIN_URI:
            return builder.table(Tables.CHECKIN_URIS);

        case CHECKIN_URI_ID:
            return builder.table(Tables.CHECKIN_URIS).where(BaseColumns._ID + " = ?", uri.getLastPathSegment());

        case MARK:
            return builder.table(Tables.MARKS);

        case MARK_ID:
            return builder.table(Tables.MARKS).where(BaseColumns._ID + " = ?", uri.getLastPathSegment());

        case BOAT:
            return builder.table(Tables.BOATS);

        case BOAT_ID:
            return builder.table(Tables.BOATS).where(BaseColumns._ID + " = ?", uri.getLastPathSegment());

        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }
}
