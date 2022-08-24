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

import java.util.ArrayList;

public class FragmentPIDs extends Fragment {
    private static final String TAG = "FragmentPID:";
    private static Context appContext;
    private static final ListView[] list = new ListView[1];

    static FragmentPIDs newInstance() {
        return new FragmentPIDs();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pid, container, false);
        list[0] = view.findViewById(R.id.list_pid);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appContext = getContext();
    }

    static void Refresh(PID[] listPIDs) {
        int position = list[0].getFirstVisiblePosition();
        ArrayList<String> arrayPIDs = new ArrayList<>();
        arrayPIDs.add("PID hx hx hx hx hx hx hx hx");
        for (PID aPID : listPIDs) {
            if (aPID.isFound) arrayPIDs.add(aPID.linePID);
        }
        arrayPIDs.add("");
        try {
            ArrayAdapter<String> listAdapter = new ArrayAdapter<>(appContext,
                    R.layout.list_text_15left, arrayPIDs);
            list[0].setAdapter(listAdapter);
            list[0].setSelection(position);
        } catch (Exception e) {
            Log.e(TAG, "refreshing" + e);
        }
    }
}
