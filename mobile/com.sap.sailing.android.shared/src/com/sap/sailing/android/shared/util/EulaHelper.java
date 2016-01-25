package com.sap.sailing.android.shared.util;

import android.support.annotation.StyleRes;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import com.sap.sailing.android.shared.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class EulaHelper {
    private static final String EULA_PREFERENCES = "eula.preferences";
    private static final String EULA_CONFIRMED = "confirmed";

    private static final int NO_THEME = 0;

    private Context mContext;

    private EulaHelper(Context context) {
        mContext = context;
    }

    public static EulaHelper with(Context context) {
        return new EulaHelper(context);
    }

    public void showEulaDialog() {
        showEulaDialog(NO_THEME);
    }

    public void showEulaDialog(@StyleRes int theme) {
        AlertDialog.Builder builder;
        switch (theme) {
            case NO_THEME:
                builder = new AlertDialog.Builder(mContext);
                break;

            default:
                builder = new AlertDialog.Builder(mContext, theme);
        }

        builder.setTitle(R.string.eula_title);
        builder.setMessage(Html.fromHtml(getContent(mContext, R.raw.license)));
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                storeEulaAccepted();
            }
        });
        AlertDialog alertDialog = builder.show();
        ((TextView)alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void storeEulaAccepted() {
        SharedPreferences preferences = mContext.getSharedPreferences(EULA_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(EULA_CONFIRMED, true);
        editor.commit();
    }

    public boolean isEulaAccepted() {
        SharedPreferences preferences = mContext.getSharedPreferences(EULA_PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getBoolean(EULA_CONFIRMED, false);
    }

    public void openEulaPage() {
        String url = mContext.getString(R.string.eula_url);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        mContext.startActivity(browserIntent);
    }

    protected String getContent(final Context context, final int contentResourceId) {
        BufferedReader reader = null;
        try {
            final InputStream inputStream = context.getResources().openRawResource(contentResourceId);
            if (inputStream != null) {
                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder builder = new StringBuilder();
                String aux;

                while ((aux = reader.readLine()) != null) {
                    builder.append(aux).append(System.getProperty("line.separator"));
                }

                return builder.toString();
            }
            throw new IOException("Error opening license file.");
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    // Don't care.
                }
            }
        }
    }
}
