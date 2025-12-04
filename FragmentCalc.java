package dc.local.electriccar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class FragmentCalc extends Fragment {
    private static final String TAG = "FragmentCalc:";
    private static final TextView[] calcView = new TextView[40];

    static FragmentCalc newInstance() {
        return new FragmentCalc();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calc, container, false);

        calcView[0] = view.findViewById(R.id.calc102);
        calcView[1] = view.findViewById(R.id.calc103);
        calcView[2] = view.findViewById(R.id.calc104);

        calcView[3] = view.findViewById(R.id.calc207);
        calcView[4] = view.findViewById(R.id.calc209);

        calcView[5] = view.findViewById(R.id.calc213);
        calcView[6] = view.findViewById(R.id.calc214);
        calcView[7] = view.findViewById(R.id.calc215);

        calcView[8] = view.findViewById(R.id.calc217);
        calcView[9] = view.findViewById(R.id.calc218);
        calcView[10] = view.findViewById(R.id.calc219);
        calcView[11] = view.findViewById(R.id.calc220);

        calcView[12] = view.findViewById(R.id.calc222);
        calcView[13] = view.findViewById(R.id.calc223);
        calcView[14] = view.findViewById(R.id.calc224);
        calcView[15] = view.findViewById(R.id.calc225);

        calcView[16] = view.findViewById(R.id.calc227);
        calcView[17] = view.findViewById(R.id.calc228);
        calcView[18] = view.findViewById(R.id.calc229);
        calcView[19] = view.findViewById(R.id.calc230);

        calcView[20] = view.findViewById(R.id.calc307);
        calcView[21] = view.findViewById(R.id.calc308);
        calcView[22] = view.findViewById(R.id.calc309);
        calcView[23] = view.findViewById(R.id.calc310);

        calcView[24] = view.findViewById(R.id.calc407);
        calcView[25] = view.findViewById(R.id.calc408);
        calcView[26] = view.findViewById(R.id.calc409);
        calcView[27] = view.findViewById(R.id.calc410);

        calcView[28] = view.findViewById(R.id.calc507);
        calcView[29] = view.findViewById(R.id.calc508);
        calcView[30] = view.findViewById(R.id.calc509);
        calcView[31] = view.findViewById(R.id.calc510);

        calcView[32] = view.findViewById(R.id.calc611);
        calcView[33] = view.findViewById(R.id.calc612);
        calcView[34] = view.findViewById(R.id.calc613);
        calcView[35] = view.findViewById(R.id.calc614);
        calcView[36] = view.findViewById(R.id.calc615);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Refresh(MainActivity.arrayOBD);
    }

    static void Refresh(ArrayList<String> arrayCalc) {
        try {
            for (int i = 0; i < 37; i++) calcView[i].setText(arrayCalc.get(i));
        } catch (Exception e) {
            Log.e(TAG, "refreshing" + e);
        }
    }
}
