package dc.local.electriccar;

import androidx.annotation.NonNull;
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


public class FragmentInfo extends Fragment {
    private static final String TAG = "FragmentInfo:";
    private static Context appContext;
    private static final ListView[] list = new ListView[1];
    private static ArrayList<String> arrayShowInfo = new ArrayList<>();

    static FragmentInfo newInstance() {
        return new FragmentInfo();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        list[0] = view.findViewById(R.id.list_info);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appContext = getContext();
        Refresh(MainActivity.listInfo);
    }

    static void Refresh(ArrayList<String> arrayInfo) {
        arrayShowInfo.addAll(arrayInfo);
        try {
            if (!arrayShowInfo.isEmpty()) {
                ArrayAdapter<String> listAdapter = new ArrayAdapter<>(appContext,
                        R.layout.list_text_14left, arrayShowInfo);
                list[0].setAdapter(listAdapter);
                list[0].setSelection(listAdapter.getCount() - 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "refreshing" + e);
        }
        int i = arrayShowInfo.size();
        if (i > 256) {
            arrayShowInfo = new ArrayList<>(arrayShowInfo.subList(i - 256, i));
        }
    }
}
