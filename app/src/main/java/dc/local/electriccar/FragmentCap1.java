package dc.local.electriccar;

import android.annotation.SuppressLint;
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

import java.text.DecimalFormat;
import java.util.ArrayList;


public class FragmentCap1 extends Fragment {
    private static final String TAG = "FragmentCap1:";
    private static Context appContext;
    private static final TextView[] cap1View = new TextView[25];
    private static final ListView[] instructions = new ListView[1];
    // The number of instructions in the listInstructions must agree with the
    // number of steps in the process
    private static final String[] listInstructions = new String[4];

    private final static DecimalFormat decFix0 = new DecimalFormat("##0");

    static FragmentCap1 newInstance() {
        return new FragmentCap1();
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cap1, container, false);

        try {
            appContext = getContext();
        } catch (Exception e) {
            Log.e(TAG, "onCreateView " + e);
        }

        cap1View[0] = view.findViewById(R.id.cap1102);
        cap1View[1] = view.findViewById(R.id.cap1103);
        cap1View[2] = view.findViewById(R.id.cap1104);

        cap1View[3] = view.findViewById(R.id.cap111);
        cap1View[4] = view.findViewById(R.id.cap112);
        cap1View[5] = view.findViewById(R.id.cap113);
        cap1View[6] = view.findViewById(R.id.cap114);
        cap1View[7] = view.findViewById(R.id.cap115);
        cap1View[8] = view.findViewById(R.id.cap116);

        cap1View[9] = view.findViewById(R.id.cap132);
        cap1View[10] = view.findViewById(R.id.cap133);

        cap1View[11] = view.findViewById(R.id.cap152);
        cap1View[12] = view.findViewById(R.id.cap153);
        cap1View[13] = view.findViewById(R.id.cap154);
        cap1View[14] = view.findViewById(R.id.cap155);
        cap1View[15] = view.findViewById(R.id.cap156);

        cap1View[16] = view.findViewById(R.id.cap164);
        cap1View[17] = view.findViewById(R.id.cap165);
        cap1View[18] = view.findViewById(R.id.cap166);

        cap1View[19] = view.findViewById(R.id.cap172);
        cap1View[20] = view.findViewById(R.id.cap173);
        cap1View[21] = view.findViewById(R.id.cap174);
        cap1View[22] = view.findViewById(R.id.cap175);
        cap1View[23] = view.findViewById(R.id.cap176);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        instructions[0] = view.findViewById(R.id.instructions1);

        listInstructions[0] = ("Measuring Battery Capacity\n\n" +
                "Ah used must be more than 20 Ah\n" +
                "The car must be turned on to ready\n" +
                "Charging the car is not a step in this procedure.\n");
        listInstructions[1] = ("Running the heater will increase the Ah used.\n");
        listInstructions[2] = ("Now the SoCs of the cells are\n" +
                "measured. This requires 15 minutes at\n" +
                "low load = amps less than 1.\n");
        StepInstructions(0);

        Refresh(MainActivity.arrayOBD, 0);

    }

    private static String computeMinutesWithHeater() {
        double usedAh;
        if (MainActivity.i_Chem.equals("LEV")) usedAh = MainActivity.c_Ah.used();
        else usedAh = MainActivity.nmc_Ah.used();
        if (usedAh < 20) return decFix0.format(60 * (20 - usedAh) / 15.0);
        else return ("0");
    }

    private static void StepInstructions(final int instruction) {

        listInstructions[1] = ("Run the heater for about " + computeMinutesWithHeater() + " min.\n");

        if (instruction == 3) {
            listInstructions[3] = ("The measurement is complete.\n" +
                    "The Ah used since charging to 100% were\n" +
                    "computed using the bmu's estimates of\n" +
                    "the battery capacity and the Ah remaining.\n" +
                    "The cell SoCs were computed using the cell volts.\n" +
                    "The cell capacities are 100 times the used Ah\n" +
                    "dividing by (100 minus the cell SoC).\n" +
                    "The accuracy of the measurement improves the\n" +
                    "more Ah used.\n" +
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
            int arrayLen = Math.min(cap1View.length, arrayCap1.size());
            for (int i = 0; i < arrayLen; i++) cap1View[i].setText(arrayCap1.get(i));
            StepInstructions(instruction);
        } catch (Exception e) {
            Log.e(TAG, "refreshing" + e);
        }
    }
}