package dc.local.electriccar;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;


public class FragmentCap1 extends Fragment {
    private static final String TAG = "FragmentCap1:";
    private static Context appContext;
    private static final TextView[] calcView = new TextView[25];
    private static final ListView[] instructions = new ListView[1];
    // The number of instructions in the listInstructions must agree with the
    // number of steps in the process
    private static final String[] listInstructions = new String[4];

    static FragmentCap1 newInstance() {
        return new FragmentCap1();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cap1, container, false);

        try {
            appContext = getContext();
        } catch (Exception e) {
            Log.e(TAG, "onCreateView " + e);
        }

        calcView[0] = view.findViewById(R.id.cap1102);
        calcView[1] = view.findViewById(R.id.cap1103);
        calcView[2] = view.findViewById(R.id.cap1104);

        calcView[3] = view.findViewById(R.id.cap112);
        calcView[4] = view.findViewById(R.id.cap113);
        calcView[5] = view.findViewById(R.id.cap114);
        calcView[6] = view.findViewById(R.id.cap115);
        calcView[7] = view.findViewById(R.id.cap116);

        calcView[8] = view.findViewById(R.id.cap132);
        calcView[9] = view.findViewById(R.id.cap133);

        calcView[10] = view.findViewById(R.id.cap152);
        calcView[11] = view.findViewById(R.id.cap153);
        calcView[12] = view.findViewById(R.id.cap154);
        calcView[13] = view.findViewById(R.id.cap155);
        calcView[14] = view.findViewById(R.id.cap156);

        calcView[15] = view.findViewById(R.id.cap164);
        calcView[16] = view.findViewById(R.id.cap165);
        calcView[17] = view.findViewById(R.id.cap166);

        calcView[18] = view.findViewById(R.id.cap172);
        calcView[19] = view.findViewById(R.id.cap173);
        calcView[20] = view.findViewById(R.id.cap174);
        calcView[21] = view.findViewById(R.id.cap175);
        calcView[22] = view.findViewById(R.id.cap176);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        instructions[0] = view.findViewById(R.id.instructions1);

        listInstructions[0] = ("Measuring Battery Capacity\n\n" +
                "Both SoC1 and SoC2 must be less than 15%\n" +
                "equal to about 4 km remaining range.\n" +
                "The car must be turned on to ready\n" +
                "Charging the car is not a step in this procedure.\n");
        listInstructions[1] = ("Running the heater will reduce the SoC.\n");
        listInstructions[2] = ("Now the SoCs of the cells are\n" +
                "measured. This requires 30 minutes at\n" +
                "low load = amps less than 1.\n");
        StepInstructions(0);

        Refresh(MainActivity.arrayOBD, 0);

    }

    private static void StepInstructions(final int instruction) {

        listInstructions[1] = ("Run the heater for about " + MainActivity.computeMinutesWithHeater() + " min.\n");

        if (instruction == 3) {
            listInstructions[3] = ("The measurement is complete.\n" +
                    "The Ah used since charging to 100% were\n" +
                    "computed using the car's estimates of\n" +
                    "the SoC and the battery capacity.\n" +
                    "The cell SoCs were computed using the cell volts.\n" +
                    "The cell capacities are 100 times the used Ah\n" +
                    "dividing by (100 minus the cell SoC).\n" +
                    "The accuracy of the measurement depends on whether\n" +
                    "the car's SoC has followed the Ah used precisely.\n" +
                    "If in doubt perform a CAP2 measurement.\n\n" +
                    "See all cell capacities on the CELLS screen.");
        } else {
            listInstructions[3] = ("The measurement is incomplete.");
        }

        try {
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(appContext,
                    R.layout.list_text_instructions, R.id.one_line, listInstructions) {

                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    // Get the Item from ListView
                    View view = super.getView(position, convertView, parent);

                    // Initialize a TextView for ListView each Item
                    TextView tv = view.findViewById(R.id.one_line);

                    if (position == instruction) {
                        // Set the text color of TextView (ListView Item)
                        tv.setTextColor(Color.GREEN);
                    } else {
                        tv.setTextColor(Color.WHITE);
                    }

                    return view;
                }
            };

            instructions[0].setAdapter(arrayAdapter);
            instructions[0].setSelection(instruction);

        } catch (Exception e) {
           Log.e(TAG, "instructions" + e);
        }
    }

    static void Refresh(ArrayList<String> arrayCap1, int step) {
        int instruction;
        if (step > 3) instruction = 3;
        else instruction = Math.max(step, 0);
        try {
            int arrayLen = Math.min(calcView.length, arrayCap1.size());
            for (int i = 0; i < arrayLen; i++) calcView[i].setText(arrayCap1.get(i));
            StepInstructions(instruction);
        } catch (Exception e) {
            Log.e(TAG, "refreshing" + e);
        }
    }
}