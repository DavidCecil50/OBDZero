package dc.local.electriccar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;


public class FragmentCells extends Fragment {
    private static final String TAG = "FragmentCells:";
    private static Context appContext = null;
    private static final GridView[] gridView = new GridView[1];
    private static final String[] numbers = new String[250];
    private static final boolean[] high = new boolean[250];
    private static final boolean[] low = new boolean[250];
    private static final boolean[] highoC = new boolean[250];
    private static final boolean[] lowoC = new boolean[250];

    static FragmentCells newInstance() {
        return new FragmentCells();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cells, container, false);

        numbers[0] = ("mod.");
        numbers[1] = ("A");
        numbers[2] = ("B");
        numbers[3] = ("C");
        numbers[4] = ("D");
        numbers[5] = ("E");
        numbers[6] = ("F");
        numbers[7] = ("G");
        numbers[8] = ("H");
        numbers[9] = ("unit");

        for (int i = 10; i < 250; i++) {
            numbers[i] = "";
            high[i] = false;
            low[i] = false;
            highoC[i] = false;
            lowoC[i] = false;
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appContext = getContext();
        gridView[0] = view.findViewById(R.id.gridview_cells);
        if (!MainActivity.cellsData) {
            CharSequence text = "There is no cell data yet or " +
                    "this model and year does not provide cell data.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(appContext.getApplicationContext(), text, duration);
            toast.show();
        }
    }

    static void Refresh(final Cell[] cells, boolean cellsData) {
        if (cellsData) {
            int index = 10;
            double maxVolts = 0;
            double maxTemp = -50;
            double maxAh1 = 0;
            double maxAh2 = 0;
            double minVolts = 5;
            double minTemp = 100;
            double minAh1 = 200;
            double minAh2 = 200;
            for (Cell aCell : cells) {
                if (aCell.isFound) {
                    if (aCell.volts > maxVolts) maxVolts = aCell.volts;
                    if (aCell.temperature > maxTemp) maxTemp = aCell.temperature;
                    if (aCell.Ah1 > maxAh1) maxAh1 = aCell.Ah1;
                    if (aCell.Ah2 > maxAh2) maxAh2 = aCell.Ah2;
                    if (aCell.volts < minVolts) minVolts = aCell.volts;
                    if (aCell.temperature < minTemp) minTemp = aCell.temperature;
                    if (aCell.Ah1 < minAh1) minAh1 = aCell.Ah1;
                    if (aCell.Ah2 < minAh2) minAh2 = aCell.Ah2;
                }
            }
            for (int i = 0; i < 12; i++) {
                numbers[index] = cells[i * 8].strModule();
                index++;
                if (minAh2 > 0) {
                    for (int j = 0; j < 8; j++) {
                        numbers[index] = cells[i * 8 + j].strAh2();
                        high[index] = cells[i * 8 + j].Ah2 >= maxAh2;
                        low[index] = cells[i * 8 + j].Ah2 <= minAh2;
                        index++;
                    }
                    numbers[index] = "Ah2";
                } else if (minAh1 > 0) {
                    for (int j = 0; j < 8; j++) {
                        numbers[index] = cells[i * 8 + j].strAh1();
                        high[index] = cells[i * 8 + j].Ah1 >= maxAh1;
                        low[index] = cells[i * 8 + j].Ah1 <= minAh1;
                        index++;
                    }
                    numbers[index] = "Ah1";
                } else {
                    for (int j = 0; j < 8; j++) {
                        numbers[index] = cells[i * 8 + j].strVolts(2);
                        if (maxVolts - minVolts > 0.02) {
                            high[index] = cells[i * 8 + j].volts >= maxVolts;
                            low[index] = cells[i * 8 + j].volts <= minVolts;
                        } else {
                            high[index] = false;
                            low[index] = false;
                        }
                        index++;
                    }
                    numbers[index] = "V";
                }
                index++;
                numbers[index] = (cells[i * 8].strModule());
                index++;
                for (int j = 0; j < 8; j++) {
                    numbers[index] = cells[i * 8 + j].strTemperature();
                    if (maxTemp - minTemp > 1.5) {
                        highoC[index] = cells[i * 8 + j].temperature >= maxTemp - 0.25;
                        lowoC[index] = cells[i * 8 + j].temperature <= minTemp + 0.25;
                    } else {
                        highoC[index] = false;
                        lowoC[index] = false;
                    }
                    index++;
                }
                numbers[index] = ("oC");
                index++;
            }

            try {
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(appContext,
                        R.layout.list_text_12center, R.id.one_cell, numbers) {
                    @NonNull
                    @Override
                    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                        // Get the Item from ListView
                        View view = super.getView(position, convertView, parent);
                        // Initialize a TextView for ListView each Item
                        TextView tv = view.findViewById(R.id.one_cell);
                        if (high[position] || lowoC[position]) {
                            tv.setTextColor(Color.GREEN);
                        } else if (low[position] || highoC[position]) {
                            tv.setTextColor(0xFFFFD480);
                        } else {
                            tv.setTextColor(Color.WHITE);
                        }
                        return view;
                    }
                };

                gridView[0].setAdapter(adapter);

            } catch (Exception e) {
                Log.e(TAG, "refreshing" + e);
            }
        } else {
            CharSequence text = "There is no cell data yet or " +
                    "this model and year does not provide cell data.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(appContext.getApplicationContext(), text, duration);
            toast.show();
        }
    }
}
