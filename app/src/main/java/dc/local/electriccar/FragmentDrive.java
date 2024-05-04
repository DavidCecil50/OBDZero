package dc.local.electriccar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;


public class FragmentDrive extends Fragment {
    private static final String TAG = "FragmentDrive:";
    private static final EditText[] textDistance = new EditText[1];
    private static final TextView[] textDrive = new TextView[4];
    private static final Button[] buttons = new Button[2];
    private static Boolean mEditing = false;

    private final static DecimalFormat decFix0 = new DecimalFormat("##0");

    static FragmentDrive newInstance() {
        return new FragmentDrive();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drive, container, false);
        textDistance[0] = view.findViewById(R.id.text_distance);
        textDrive[0] = view.findViewById(R.id.distance_units);
        textDrive[1] = view.findViewById(R.id.text_margin);
        textDrive[2] = view.findViewById(R.id.text_speed);
        textDrive[3] = view.findViewById(R.id.title_speed);
        buttons[0] = view.findViewById(R.id.km_units);
        buttons[0].setTransformationMethod(null);
        buttons[0].setOnClickListener(v -> changeMiles());
        buttons[1] = view.findViewById(R.id.speed_units);
        buttons[1].setTransformationMethod(null);
        buttons[1].setOnClickListener(v -> changeMph());
        writeMiles();
        writeMph();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null)
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        Context appContext = getContext();

        Refresh();

        if (MainActivity.checkRRMiles && MainActivity.t_Margin.dbl > 10) {
            CharSequence text = "Check the if the range shown on the instrument panel is in miles. " +
                    "If so change the range units in the initials values menu";
            int duration = Toast.LENGTH_LONG;
            Toast toast = null;
            if (appContext != null) {
                toast = Toast.makeText(appContext.getApplicationContext(), text, duration);
            }
            if (toast != null) {
                toast.show();
            }
        }

        textDistance[0].setOnFocusChangeListener((v, hasFocus) -> {
            mEditing = hasFocus;
            textDistance[0].setCursorVisible(hasFocus);
        });

        textDistance[0].setOnEditorActionListener(
                (v, actionId, event) -> {
                    mEditing = true;
                    if (isSoftKeyboardFinishedAction(actionId, event)) {
                        double kmReq;

                        if (MainActivity.miles) kmReq = 0.621371192 * MainActivity.t_km.dbl;
                        else kmReq = MainActivity.t_km.dbl;

                        // the user is done typing.
                        String drvDistance = v.getText().toString();
                        try {
                            kmReq = Double.parseDouble(drvDistance);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "editing not a number" + e);
                        }

                        if (MainActivity.miles) {
                            MainActivity.t_km.dbl = 1.609344 * kmReq;
                        } else MainActivity.t_km.dbl = kmReq;
                        MainActivity.m_km.dbl = 0;

                        textDistance[0].setCursorVisible(false);
                        mEditing = false;

                        // the user is done typing.
                        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                        return true; // consume.
                    }
                    return false; // pass on to other listeners.}
                }
        );
    }

    void changeMiles() {
        MainActivity.miles = !MainActivity.miles;
        writeMiles();
    }

    @SuppressLint("SetTextI18n")
    void writeMiles() {
        if (MainActivity.miles) {
            buttons[0].setText("miles");
            textDistance[0].setText(decFix0.format(0.621371192 * MainActivity.t_km.dbl));
            textDrive[0].setText("miles");
            textDrive[1].setText(decFix0.format(0.621371192 * MainActivity.t_Margin.dbl));
        } else {
            buttons[0].setText("km");
            textDistance[0].setText(decFix0.format(MainActivity.t_km.dbl));
            textDrive[0].setText("km");
            textDrive[1].setText(decFix0.format(MainActivity.t_Margin.dbl));
        }
    }

    void changeMph() {
        MainActivity.mph = !MainActivity.mph;
        writeMph();
    }

    @SuppressLint("SetTextI18n")
    void writeMph() {
        if (MainActivity.mph) {
            buttons[1].setText("mph");
            textDrive[2].setText(decFix0.format(0.621371192 * MainActivity.t_Speed.dbl));
        } else {
            buttons[1].setText("km/h");
            textDrive[2].setText(decFix0.format(MainActivity.t_Speed.dbl));
        }
    }

    @SuppressLint("SetTextI18n")
    static void Refresh() {
        if (!mEditing) {
            String drvSpd;
            if (MainActivity.t_Speed.dbl < 10)
                if (MainActivity.mph) drvSpd = "6";
                else drvSpd = "10";
            else {
                if (MainActivity.t_Speed.dbl > 130) {
                    if (MainActivity.mph) drvSpd = "80";
                    else drvSpd = "130";
                } else {
                    if (MainActivity.mph)
                        drvSpd = decFix0.format(0.621371192 * MainActivity.t_Speed.dbl);
                    else drvSpd = decFix0.format(MainActivity.t_Speed.dbl);
                }
            }
            textDrive[2].setText(drvSpd);

            if (MainActivity.t_reqkm.dbl > 0) {
                if (MainActivity.t_Ah.RR() > 1.05 * MainActivity.t_reqkm.dbl)
                    textDrive[2].setTextColor(Color.rgb(100, 255, 100));
                else if (MainActivity.t_Ah.RR() > 0.95 * MainActivity.t_reqkm.dbl)
                    textDrive[2].setTextColor(Color.rgb(255, 255, 255));
                else if (MainActivity.t_Ah.RR() > 0.9 * MainActivity.t_reqkm.dbl)
                    textDrive[2].setTextColor(Color.rgb(255, 202, 28));
                else textDrive[2].setTextColor(Color.rgb(255, 100, 100));
            } else {
                textDrive[2].setTextColor(Color.rgb(255, 100, 100));
            }

            if (MainActivity.t_km.dbl > 0) {
                textDrive[3].setText("Suggested speed to the station");
            } else {
                textDrive[3].setText("Suggested speed so that true range = rest range");
            }

            if (MainActivity.miles) {
                textDistance[0].setText(decFix0.format(0.621371192 * MainActivity.t_km.dbl));
            } else textDistance[0].setText(decFix0.format(MainActivity.t_km.dbl));

            if (MainActivity.miles) {
                textDrive[1].setText(decFix0.format(0.621371192 * MainActivity.t_Margin.dbl));
            } else textDrive[1].setText(decFix0.format(MainActivity.t_Margin.dbl));

        }
    }

    private boolean isSoftKeyboardFinishedAction(int action, KeyEvent event) {
        // Some devices return null event on editor actions for Enter Button
        return action == EditorInfo.IME_ACTION_DONE ||
                action == EditorInfo.IME_ACTION_GO ||
                action == EditorInfo.IME_ACTION_SEND && event == null ||
                event.getAction() == KeyEvent.ACTION_DOWN;
    }

}
