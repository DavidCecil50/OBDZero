package dc.local.electriccar;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class FragmentAh extends Fragment {
    private static final TextView[] texts = new TextView[9];

    static FragmentAh newInstance() {
        return new FragmentAh();
    }

    @SuppressLint("SetTextI18n")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ah, container, false);
        texts[0] = view.findViewById(R.id.number_1);
        texts[1] = view.findViewById(R.id.units_1);
        texts[1].setText(" Ah");
        texts[2] = view.findViewById(R.id.number_2);
        texts[3] = view.findViewById(R.id.units_2);
        texts[3].setText(" Ah");
        texts[4] = view.findViewById(R.id.number_3);
        texts[5] = view.findViewById(R.id.units_3);
        texts[5].setText(" %");

        texts[6] = view.findViewById(R.id.text_1);
        texts[6].setText("Fully Charged Battery Capacity");
        texts[7] = view.findViewById(R.id.text_2);
        texts[7].setText("Present Charge");
        texts[8] = view.findViewById(R.id.text_3);
        texts[8].setText("Present SoC");

        Refresh();

        return view;
    }

    static void Refresh() {
        if (MainActivity.i_Chem.equals("LEV")) {
            if (MainActivity.bmu_Ah.cap > 0) {
                texts[0].setText(MainActivity.bmu_Ah.capStr());
                texts[2].setText(MainActivity.bmu_Ah.remStr());
                texts[4].setText(MainActivity.bmu_Ah.SoCStr());
            } else {
                texts[0].setText(MainActivity.c_Ah.capStr());
                texts[2].setText(MainActivity.c_Ah.remStr());
                texts[4].setText(MainActivity.c_Ah.SoCStr());
            }
        } else {
            texts[0].setText(MainActivity.nmc_Ah.capStr());
            texts[2].setText(MainActivity.nmc_Ah.remStr());
            texts[4].setText(MainActivity.nmc_Ah.SoCStr());
        }
    }
}
