package dc.local.electriccar;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;



public class FragmentCharge extends Fragment {

    private static final TextView[] texts = new TextView[23];

    static FragmentCharge newInstance() {
        return new FragmentCharge();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_charge, container, false);
        texts[0] = view.findViewById(R.id.charge00);
        texts[1] = view.findViewById(R.id.charge01);
        texts[2] = view.findViewById(R.id.charge02);
        texts[3] = view.findViewById(R.id.charge10);
        texts[4] = view.findViewById(R.id.charge20);
        texts[5] = view.findViewById(R.id.charge30);
        texts[6] = view.findViewById(R.id.charge40);
        texts[7] = view.findViewById(R.id.charge50);
        texts[8] = view.findViewById(R.id.charge60);
        texts[9] = view.findViewById(R.id.charge70);
        texts[10] = view.findViewById(R.id.charge80);
        texts[11] = view.findViewById(R.id.charge85);
        texts[12] = view.findViewById(R.id.charge90);
        texts[13] = view.findViewById(R.id.charge100);
        texts[14] = view.findViewById(R.id.charge110);
        texts[15] = view.findViewById(R.id.charge120);
        texts[16] = view.findViewById(R.id.charge130);
        texts[17] = view.findViewById(R.id.charge140);
        texts[18] = view.findViewById(R.id.charge150);
        texts[19] = view.findViewById(R.id.charge160);
        texts[20] = view.findViewById(R.id.charge170);
        texts[21] = view.findViewById(R.id.charge180);
        texts[22] = view.findViewById(R.id.charge190);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Refresh();
    }

    static void Refresh() {
        texts[0].setText(MainActivity.displayDate.format(MainActivity.stepDateTime));
        texts[1].setText(MainActivity.displayTime.format(MainActivity.stepDateTime));
        texts[2].setText(MainActivity.decFix2.format(MainActivity.d_Second) + " sec");
        texts[3].setText(MainActivity.c_AmpsCal.unit());
        texts[4].setText(MainActivity.b_BatVavg.unit());
        texts[5].setText(MainActivity.b_BatVmax.unit());
        texts[6].setText(MainActivity.b_BatVmin.unit());
        texts[7].setText(MainActivity.bmu_Ah.remUnit());
        texts[8].setText(MainActivity.bmu_CapAh0.unit());
        texts[9].setText(MainActivity.b_CapAh0.unit());
        texts[10].setText(MainActivity.b_Volts0.unit());
        texts[11].setText(MainActivity.decFix2.format(MainActivity.b_Volts0.dbl/MainActivity.m_CellsNo) + "V" );
        texts[12].setText(MainActivity.p1_Time.unit());
        texts[13].setText(MainActivity.p1_Volts.unit());
        texts[14].setText(MainActivity.p1_SoC.unit());
        texts[15].setText(MainActivity.p1_Ah.unit());
        texts[16].setText(MainActivity.p2_Time.unit());
        texts[17].setText(MainActivity.p2_Volts.unit());
        texts[18].setText(MainActivity.p2_SoC.unit());
        texts[19].setText(MainActivity.p2_Ah.unit());
        texts[20].setText(MainActivity.bmu_CapAh1.unit());
        texts[21].setText(MainActivity.b_CapAh1.unit());
        texts[22].setText(MainActivity.p12_CapAh.unit());
    }

}