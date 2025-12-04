package dc.local.electriccar;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.text.DecimalFormat;

public class FragmentVolts extends Fragment {
    private static final TextView[] texts = new TextView[9];

    private final static DecimalFormat decFix0 = new DecimalFormat("##0");
    static FragmentVolts newInstance() {
        return new FragmentVolts();
    }

    @SuppressLint("SetTextI18n")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ah, container, false);
        texts[0] = view.findViewById(R.id.number_1);
        texts[0].setText(decFix0.format(MainActivity.b_Volts.dbl));
        texts[1] = view.findViewById(R.id.units_1);
        texts[1].setText(" V");
        texts[2] = view.findViewById(R.id.number_2);
        texts[2].setText(MainActivity.b_BatVmax.str());
        texts[3] = view.findViewById(R.id.units_2);
        texts[3].setText(" V");
        texts[4] = view.findViewById(R.id.number_3);
        texts[4].setText(MainActivity.b_BatVmin.str());
        texts[5] = view.findViewById(R.id.units_3);
        texts[5].setText(" V");

        texts[6] = view.findViewById(R.id.text_1);
        texts[6].setText("Battery");
        texts[7] = view.findViewById(R.id.text_2);
        texts[7].setText("Highest Cell");
        texts[8] = view.findViewById(R.id.text_3);
        texts[8].setText("Lowest Cell");

        return view;
    }

    static void Refresh() {
        texts[0].setText(decFix0.format(MainActivity.b_Volts.dbl));
        texts[2].setText(MainActivity.b_BatVmax.str());
        texts[4].setText(MainActivity.b_BatVmin.str());
    }
}
