package dc.local.electriccar;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class FragmentTemp extends Fragment {
    private static final TextView[] texts = new TextView[9];

    static FragmentTemp newInstance() {
        return new FragmentTemp();
    }

    @SuppressLint("SetTextI18n")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ah, container, false);
        texts[0] = view.findViewById(R.id.number_1);
        texts[0].setText(MainActivity.b_Temp.str());
        texts[1] = view.findViewById(R.id.units_1);
        texts[1].setText(" oC");
        texts[2] = view.findViewById(R.id.number_2);
        texts[2].setText(MainActivity.b_BatTmax.str());
        texts[3] = view.findViewById(R.id.units_2);
        texts[3].setText(" oC");
        texts[4] = view.findViewById(R.id.number_3);
        texts[4].setText(MainActivity.b_BatTmin.str());
        texts[5] = view.findViewById(R.id.units_3);
        texts[5].setText(" oC");

        texts[6] = view.findViewById(R.id.text_1);
        texts[6].setText("Battery Average");
        texts[7] = view.findViewById(R.id.text_2);
        texts[7].setText("Warmest Cell");
        texts[8] = view.findViewById(R.id.text_3);
        texts[8].setText("Coldest Cell");

        return view;
    }

    static void Refresh() {
        texts[0].setText(MainActivity.b_Temp.str());
        texts[2].setText(MainActivity.b_BatTmax.str());
        texts[4].setText(MainActivity.b_BatTmin.str());
    }
}
