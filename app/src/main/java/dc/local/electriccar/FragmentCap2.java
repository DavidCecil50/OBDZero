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


public class FragmentCap2 extends Fragment {
    private static final String TAG = "FragmentCap2:";
    private static Context appContext;
    private static final TextView[] calcView = new TextView[25];
    private static final ListView[] instructions = new ListView[1];
    private static final String[] listInstructions = new String[9];

    static FragmentCap2 newInstance() {
        return new FragmentCap2();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cap2, container, false);

        calcView[0] = view.findViewById(R.id.cap2102);
        calcView[1] = view.findViewById(R.id.cap2103);
        calcView[2] = view.findViewById(R.id.cap2104);

        calcView[3] = view.findViewById(R.id.cap212);
        calcView[4] = view.findViewById(R.id.cap213);
        calcView[5] = view.findViewById(R.id.cap214);
        calcView[6] = view.findViewById(R.id.cap215);
        calcView[7] = view.findViewById(R.id.cap216);

        calcView[8] = view.findViewById(R.id.cap232);
        calcView[9] = view.findViewById(R.id.cap233);

        calcView[10] = view.findViewById(R.id.cap252);
        calcView[11] = view.findViewById(R.id.cap253);
        calcView[12] = view.findViewById(R.id.cap254);
        calcView[13] = view.findViewById(R.id.cap255);
        calcView[14] = view.findViewById(R.id.cap256);

        calcView[15] = view.findViewById(R.id.cap264);
        calcView[16] = view.findViewById(R.id.cap265);
        calcView[17] = view.findViewById(R.id.cap266);

        calcView[18] = view.findViewById(R.id.cap272);
        calcView[19] = view.findViewById(R.id.cap273);
        calcView[20] = view.findViewById(R.id.cap274);
        calcView[21] = view.findViewById(R.id.cap275);
        calcView[22] = view.findViewById(R.id.cap276);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appContext = getContext();

        instructions[0] = view.findViewById(R.id.instructions2);

        listInstructions[0] = ("Measuring Battery Capacity\n\n" +
                "Both SoC1 and SoC2 must be less than 15%,\n" +
                "equal to about 4 km left in the battery,\n" +
                "before the measurement can begin.\n" +
                "None of the steps in the procedure need to\n" +
                "be done immediately after the previous step.\n");
        listInstructions[1] = ("Running the heater will reduce the SoC.\n");
        listInstructions[2] = ("Now the SoC of all the cells is measured\n" +
                "This requires 30 minutes at low\n" +
                "load = amps less than 1.\n");
        listInstructions[3] = ("Please turn the car off and then plug\n" +
                "in the charger. Let the app run but do not\n" +
                "turn the car on.\n");
        listInstructions[4] = ("Please wait until the SoC is 100%\n" +
                "and charging stops by itself.\n" +
                "You do not need to be on hand.\n" +
                "The next steps can be done up to 2 hours\n" +
                "after charging stops.");
        listInstructions[5] = ("");
        listInstructions[6] = ("To complete the measurement unplug the car and\n" +
                "turn it on to ready.\n");
        listInstructions[7] = ("Now the SoC of all the cells are being measured\n" +
                "again. This may require 30 minutes\n" +
                "at low load = amps less than 1.\n");

        StepInstructions(0);

        Refresh(MainActivity.arrayOBD, 0);
    }

    private static void StepInstructions(final int instruction) {

        if (!MainActivity.computeMinutesWithHeater().equals("-1"))
            listInstructions[1] = ("Run the heater for about " + MainActivity.computeMinutesWithHeater() + " min.\n");

        if (instruction == 8) {
            listInstructions[8] = ("The measurement is complete.\n" +
                    "The capacitys of the highest and\n" +
                    "lowest cells are shown above.\n" +
                    "Each capacity is the Ah added divided by the SoC added\n" +
                    "times 100.\n" +
                    "It's a good idea to repeat the measurement\n" +
                    "before drawing any conclusions.\n\n" +
                    "See all cell capacities on the CELLS screen.");
        } else {
            listInstructions[8] = ("The measurement is incomplete.");
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

    static void Refresh(ArrayList<String> arrayCap2, int step) {
        int instruction;
        if (step > 8) instruction = 8;
        else instruction = Math.max(step, 0);
        int arrayLen = Math.min(calcView.length, arrayCap2.size());
        try {
            for (int i = 0; i < arrayLen; i++) calcView[i].setText(arrayCap2.get(i));
        } catch (Exception e) {
            Log.e(TAG, "refreshing" + e);
        }
        StepInstructions(instruction);
    }
}