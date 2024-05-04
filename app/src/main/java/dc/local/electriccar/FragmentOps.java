package dc.local.electriccar;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FragmentOps extends Fragment {

    private static final TextView[] texts = new TextView[23];

    static FragmentOps newInstance() {
        return new FragmentOps();
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ops, container, false);
        texts[0] = view.findViewById(R.id.ops_date);
        texts[1] = view.findViewById(R.id.ops_time);
        texts[2] = view.findViewById(R.id.ops_step);
        texts[3] = view.findViewById(R.id.ops_cap);
        texts[4] = view.findViewById(R.id.ops_rem);
        texts[5] = view.findViewById(R.id.ops_pause);
        texts[6] = view.findViewById(R.id.ops_whkm);
        texts[7] = view.findViewById(R.id.ops_soc);
        texts[8] = view.findViewById(R.id.ops_rr);
        texts[9] = view.findViewById(R.id.ops_volts);
        texts[10] = view.findViewById(R.id.ops_amps);
        texts[11] = view.findViewById(R.id.ops_watts);
        texts[12] = view.findViewById(R.id.ops_cellv);
        texts[13] = view.findViewById(R.id.ops_cellt);

        Refresh();

        return view;
    }

    @SuppressLint("SetTextI18n")
    static void Refresh() {
        texts[0].setText(MainActivity.displayDate.format(MainActivity.stepDateTime));
        texts[1].setText(MainActivity.displayTime.format(MainActivity.stepDateTime));
        texts[2].setText(MainActivity.decFix2.format(MainActivity.d_Second) + " sec");
        if (MainActivity.i_Chem.equals("NMC")) {
            texts[3].setText(MainActivity.nmc_Ah.capUnit());
            texts[4].setText(MainActivity.nmc_Ah.remUnit());
            texts[5].setText(MainActivity.m_OCtimer.unit());
            texts[6].setText(MainActivity.nmc_Ah.WhkmStr());
            texts[7].setText(MainActivity.nmc_Ah.SoCUnit());
            texts[8].setText(MainActivity.nmc_Ah.RRUnit());
        } else {
            if (MainActivity.bmu_Ah.cap> 0) {
                texts[3].setText(MainActivity.bmu_Ah.capUnit());
                texts[4].setText(MainActivity.bmu_Ah.remUnit());
                texts[5].setText(MainActivity.m_OCtimer.unit());
                texts[6].setText(MainActivity.bmu_Ah.WhkmStr());
                texts[7].setText(MainActivity.bmu_Ah.SoCUnit());
                texts[8].setText(MainActivity.bmu_Ah.RRUnit());
            } else {
                texts[3].setText(MainActivity.c_Ah.capUnit());
                texts[4].setText(MainActivity.c_Ah.remUnit());
                texts[5].setText(MainActivity.m_OCtimer.unit());
                texts[6].setText(MainActivity.c_Ah.WhkmStr());
                texts[7].setText(MainActivity.c_Ah.SoCUnit());
                texts[8].setText(MainActivity.c_Ah.RRUnit());
            }
        }
        texts[9].setText(MainActivity.b_Volts.str());
        texts[10].setText(MainActivity.c_AmpsCal.str());
        texts[11].setText(MainActivity.decFix1.format(MainActivity.c_WattsCal.dbl / 1000.0));
        texts[12].setText(MainActivity.b_BatVmin.str());
        texts[13].setText(MainActivity.b_BatTmax.str());
    }

}