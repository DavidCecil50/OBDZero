package dc.local.electriccar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


public class FragmentOBD extends Fragment {
    private static final String TAG = "FragmentOBD:";
    private static Context appContext;
    private static final ListView[] list = new ListView[1];

    static FragmentOBD newInstance() {
        return new FragmentOBD();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_obd, container, false);
        list[0] = view.findViewById(R.id.list_obd);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appContext = getContext();
    }

    static void Refresh(ArrayList<String> arrayOBD) {
        int position = list[0].getFirstVisiblePosition();
        arrayOBD.add("");
        try {
            ArrayAdapter<String> listAdapter = new ArrayAdapter<>(appContext,
                    R.layout.list_text_14left, arrayOBD);
            list[0].setAdapter(listAdapter);
            list[0].setSelection(position);
        } catch (Exception e) {
            Log.e(TAG, "refreshing" + e);
        }

        if (MainActivity.checkOdoUnits) {
            CharSequence text = "Check the if the Odometer shown above is in miles and not km. " +
                    "If so change the odometer units to miles in the initials values menu";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(appContext.getApplicationContext(), text, duration);
            toast.show();
        }

        if (MainActivity.checkRangeUnits) {
            CharSequence text = "Check the if the range shown above is in miles and not km. " +
                    "If so change the range units to miles in the initials values menu";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(appContext.getApplicationContext(), text, duration);
            toast.show();
        }

    }
}
