package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.startprocedure.impl.GateStartProcedure;

public class RaceChooseGateLineOpeningTimeDialog extends RaceDialogFragment {

    public interface GateLineOpeningTimeSelectionListener {
        public void onLineOpeningTimeSelected();
    }

    private EditText lineOpeningTimeEditText;
    private Button chooseButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.race_choose_line_opening_time_view, container);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getDialog().setTitle(getString(R.string.set_gate_line_opening_time_title));

        lineOpeningTimeEditText = (EditText) getView().findViewById(R.id.lineOpeningTimeText);
        lineOpeningTimeEditText.setText(String
                .valueOf((GateStartProcedure.startPhaseGolfDownStandardInterval / (60 * 1000))));
        chooseButton = (Button) getDialog().findViewById(R.id.chooseLineOpeningTimeButton);

        chooseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                onChooseClicked(v);
            }
        });
    }

    protected void onChooseClicked(View view) {
        try {
            Integer lineOpeningTime = convertToInteger(lineOpeningTimeEditText.getText().toString());
            if (getRace().getState().getStartProcedure() instanceof GateStartProcedure) {
                ((GateStartProcedure) getRace().getState().getStartProcedure()).setGateLineOpeningTime(Long
                        .valueOf(60 * 1000 * lineOpeningTime));
            }
            Log.i("RACE_SET_GATELINE_OPENING_TIME", String.valueOf(lineOpeningTime));
            dismiss();
        } catch (NumberFormatException nfe) {
            Toast.makeText(getActivity(), "Please enter a valid number of minutes", Toast.LENGTH_LONG).show();
        }
    }

    protected Integer convertToInteger(String text) throws NumberFormatException {
        return Integer.parseInt(text);
    }
}
