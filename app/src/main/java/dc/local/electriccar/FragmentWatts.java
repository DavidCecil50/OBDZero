package dc.local.electriccar;

import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;

public class FragmentWatts extends Fragment {
    private static final TextView[] textWatts = new TextView[5];
    private static final Button[] buttons = new Button[2];
    private final static DecimalFormat decFix0 = new DecimalFormat("##0");

    static FragmentWatts newInstance() {
        return new FragmentWatts();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watts, container, false);
        textWatts[0] = view.findViewById(R.id.text_watts);
        textWatts[0].setText(decFix0.format((MainActivity.b_WMovAvg.dbl + MainActivity.m_AuxW.dbl) / 1000));
        textWatts[1] = view.findViewById(R.id.text_whkm);
        textWatts[2] = view.findViewById(R.id.text_speedavg);
        buttons[0] = view.findViewById(R.id.whkm_units);
        buttons[1] = view.findViewById(R.id.speedavg_units);
        buttons[0].setOnClickListener(v -> changeMkWh());
        buttons[1].setOnClickListener(v -> changeMph());
        buttons[0].setTransformationMethod(null);
        buttons[1].setTransformationMethod(null);
        writeMkWh();
        writeMph();
        return view;
    }

    void changeMkWh() {
        MainActivity.milesPerkWh = !MainActivity.milesPerkWh;
        writeMkWh();
    }

    void writeMkWh() {
        if (MainActivity.milesPerkWh) {
            buttons[0].setText("miles/kWh");
            textWatts[1].setText(decFix0.format(621.37119 / MainActivity.b_WhkmAux.dbl));
        } else {
            buttons[0].setText("Wh/km");
            textWatts[1].setText(decFix0.format(MainActivity.b_WhkmAux.dbl));
        }
    }

    void changeMph() {
        MainActivity.mph = !MainActivity.mph;
        writeMph();
    }

    void writeMph() {
        if (MainActivity.mph) {
            buttons[1].setText("mph");
            textWatts[2].setText(decFix0.format(0.621371192 * MainActivity.c_SpdAvg.dbl));
        } else {
            buttons[1].setText("km/h");
            textWatts[2].setText(decFix0.format(MainActivity.c_SpdAvg.dbl));
        }
    }

    static void Refresh() {
        textWatts[0].setText(decFix0.format((MainActivity.b_WMovAvg.dbl + MainActivity.m_AuxW.dbl) / 1000));
        if (MainActivity.milesPerkWh) {
            textWatts[1].setText(decFix0.format(621.37119 / MainActivity.b_WhkmAux.dbl));
        } else {
            textWatts[1].setText(decFix0.format(MainActivity.b_WhkmAux.dbl));
        }
        if (MainActivity.mph) {
            textWatts[2].setText(decFix0.format(0.621371192 * MainActivity.c_SpdAvg.dbl));
        } else {
            textWatts[2].setText(decFix0.format(MainActivity.c_SpdAvg.dbl));
        }
    }
}
