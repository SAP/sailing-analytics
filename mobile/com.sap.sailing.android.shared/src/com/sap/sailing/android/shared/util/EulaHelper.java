package com.sap.sailing.android.shared.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.sap.sailing.android.shared.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class EulaHelper {

    public interface OnEulaAcceptedListener {
        void eulaAccepted();
    }

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

    public void showEulaDialogIfNotAccepted(OnEulaAcceptedListener acceptedHandler) {
        if (!this.isEulaAccepted()) {
            showEulaDialog(acceptedHandler);
            return;
        }
        acceptedHandler.eulaAccepted();
    }

    public void showEulaDialog(@Nullable final OnEulaAcceptedListener acceptedHandler) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.eula_title);
        builder.setMessage(getSpannableMessage(builder.getContext().getTheme()));
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.eula_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                storeEulaAccepted();
                if (acceptedHandler != null) {
                    acceptedHandler.eulaAccepted();
                }
            }
        });
        AlertDialog alertDialog = builder.show();
        alertDialog.setCanceledOnTouchOutside(false);
        ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void storeEulaAccepted() {
        SharedPreferences preferences = mContext.getSharedPreferences(EULA_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(EULA_CONFIRMED, true);
        editor.apply();
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

    private SpannableString getSpannableMessage(Resources.Theme theme) {
        String message = mContext.getString(R.string.eula_message);
        String clickableText = mContext.getString(R.string.linked_eula_message_part);

        SpannableString spannableString = new SpannableString(message);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                openEulaPage();
            }
        };

        spannableString.setSpan(clickableSpan, message.indexOf(clickableText),
                message.indexOf(clickableText) + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        spannableString.setSpan(new ForegroundColorSpan(typedValue.data), message.indexOf(clickableText),
                message.indexOf(clickableText) + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
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
