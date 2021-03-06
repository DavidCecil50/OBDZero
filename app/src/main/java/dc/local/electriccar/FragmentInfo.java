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
    private static final String TAG = "FragmentInfo";
    private static final boolean DEBUG = true;
    private static Context appContext;
    private static final ListView[] list = new ListView[1];

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
    }

    static void Refresh(ArrayList<String> arrayInfo) {
        try {
            if (arrayInfo.size() > 0) {
                ArrayAdapter<String> listAdapter = new ArrayAdapter<>(appContext,
                        R.layout.list_text_14left, arrayInfo);
                list[0].setAdapter(listAdapter);
                list[0].setSelection(listAdapter.getCount() - 1);
            }
        } catch (Exception e) {
            if (DEBUG) Log.i(TAG, " refreshing" + e);
        }
    }
}
