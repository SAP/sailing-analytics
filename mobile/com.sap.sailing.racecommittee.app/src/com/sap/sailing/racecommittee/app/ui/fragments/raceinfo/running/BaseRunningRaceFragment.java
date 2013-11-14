package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.running;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.racelog.state.racingprocedure.FlagPoleState;
import com.sap.sailing.domain.racelog.state.racingprocedure.RacingProcedure;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.AbortTypeSelectionDialog;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.RaceDialogFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.BaseRaceInfoRaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.FlagPoleStateRenderer;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;

public abstract class BaseRunningRaceFragment<ProcedureType extends RacingProcedure> extends BaseRaceInfoRaceFragment<ProcedureType> {

    private ImageButton abortButton;
    private ImageButton finishingButton;
    private ImageButton generalRecallButton;
    private ImageButton individualRecallButton;
    private TextView startCountUpTextView;
    private TextView nextCountdownTextView;
    
    private FlagPoleStateRenderer flagRenderer;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.race_running_base_view, container, false);
        ViewStub actionsStub = (ViewStub) view.findViewById(R.id.race_running_base_actions);
        int actionsLayout = getActionsLayoutId();
        if (actionsLayout != 0) {
            actionsStub.setLayoutResource(actionsLayout);
            actionsStub.inflate();
        }
        return view;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        startCountUpTextView = (TextView) getView().findViewById(R.id.race_running_base_start_countup);
        nextCountdownTextView = (TextView) getView().findViewById(R.id.race_running_base_next_countdown);
        abortButton = (ImageButton) getView().findViewById(R.id.race_running_base_abort);
        finishingButton = (ImageButton) getView().findViewById(R.id.race_running_base_finishing);
        generalRecallButton = (ImageButton) getView().findViewById(R.id.race_running_base_general_recall);
        individualRecallButton = (ImageButton) getView().findViewById(R.id.race_running_base_individual_recall);
        
        if (getRacingProcedure().hasIndividualRecall()) {
            individualRecallButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    RacingProcedure procedure = getRacingProcedure();
                    if (procedure.isIndividualRecallDisplayed()) {
                        procedure.removeIndividualRecall(MillisecondsTimePoint.now());
                    } else {
                        procedure.displayIndividualRecall(MillisecondsTimePoint.now());
                    }
                }
            });
        } else {
            individualRecallButton.setVisibility(View.GONE);
            TextView individualRecallLabel = (TextView) getView().findViewById(R.id.race_running_base_individual_recall_label);
            individualRecallLabel.setVisibility(View.GONE);
        }
        
        generalRecallButton = (ImageButton) getView().findViewById(R.id.race_running_base_general_recall);
        generalRecallButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getRaceState().setGeneralRecall(MillisecondsTimePoint.now());
            }
        });
        
        finishingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getRaceState().setFinishingTime(MillisecondsTimePoint.now());
            }
        });
        
        abortButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                RaceDialogFragment fragment = new AbortTypeSelectionDialog();
                fragment.setArguments(getRecentArguments());
                fragment.show(getFragmentManager(), "dialogAPNovemberMode");
            }
        });
        
        flagRenderer = new FlagPoleStateRenderer(getActivity(),
                (LinearLayout) getView().findViewById(R.id.race_running_base_up_flags), 
                (LinearLayout) getView().findViewById(R.id.race_running_base_down_flags));
    }

    @Override
    public void notifyTick() {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint startTime = getRaceState().getStartTime();
        if (startTime != null) {
            long millisecondsSinceStart = now.minus(startTime.asMillis()).asMillis();
            
            startCountUpTextView.setText(String.format(
                    getString(R.string.race_running_since_template),
                    getRace().getName(), TimeUtils.formatDuration(millisecondsSinceStart)));
            
            ProcedureType procedure = getRacingProcedure();
            FlagPoleState flagState = procedure.getActiveFlags(startTime, now);
            if (flagState.hasNextState()) {
                // TODO: get changing flag and display on nextCountdownTextView
                long millisecondsTillChange = flagState.getNextStateValidFrom().minus(now.asMillis()).asMillis();
                nextCountdownTextView.setText(String.format("%s until next flag...", TimeUtils.formatDuration(millisecondsTillChange)));
            } else if (procedure.hasIndividualRecall()) {
                if (procedure.isIndividualRecallDisplayed()) {
                    TimePoint removalAt = procedure.getIndividualRecallRemovalTime();
                    if (removalAt != null) {
                        nextCountdownTextView.setText(String.format("%s until recall ends...", 
                                TimeUtils.formatDuration(TimeUtils.timeUntil(removalAt))));
                    }
                } else {
                    nextCountdownTextView.setText("");
                }
            } else {
                nextCountdownTextView.setText("");
            }
        }
        super.notifyTick();
    }

    protected int getActionsLayoutId() {
        return 0;
    }
    
    @Override
    protected void setupUi() {
        TimePoint startTime = getRaceState().getStartTime();
        if (startTime != null) {
            flagRenderer.render(getRacingProcedure().getActiveFlags(startTime, MillisecondsTimePoint.now()));
        }
    }

}
