package dc.local.electriccar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import static dc.local.electriccar.MainActivity.TRUE_SPEED;
import static dc.local.electriccar.MainActivity.PREFERRED_MARGIN;
import static dc.local.electriccar.MainActivity.CAR_LOAD;
import static dc.local.electriccar.MainActivity.RANGE_UNITS;
import static dc.local.electriccar.MainActivity.ODO_UNITS;

public class InitialValuesActivity extends Activity {
    private EditText textValue1;
    private EditText textValue2;
    private EditText textValue3;
    private Button btnRangeUnits;
    private String rangeUnits;
    private Button btnOdoUnits;
    private String odoUnits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial_values);

        textValue1 = findViewById(R.id.value_speed);
        textValue1.setText(MainActivity.i_Spd100.str());
        textValue2 = findViewById(R.id.value_margin);
        textValue2.setText(MainActivity.i_Margin.str());
        textValue3 = findViewById(R.id.value_load);
        textValue3.setText(MainActivity.i_Load.str());

        btnRangeUnits = findViewById(R.id.range_units);
        btnRangeUnits.setText(MainActivity.i_RangeUnits);
        btnRangeUnits.setOnClickListener(v -> toogleRangeMiles());

        btnOdoUnits = findViewById(R.id.odo_units);
        btnOdoUnits.setText(MainActivity.i_OdoUnits);
        btnOdoUnits.setOnClickListener(v -> toogleOdoMiles());

        Button btnUpdate = findViewById(R.id.update);
        btnUpdate.setOnClickListener(v -> updateValues());

        // Set result CANCELED in case the user backs out
        setResult(RESULT_CANCELED);
    }

    private void toogleRangeMiles() {
        if (rangeUnits.equals("km")) {
            rangeUnits = "miles";
        } else {
            rangeUnits = "km";
        }
        btnRangeUnits.setText(rangeUnits);
    }

    private void toogleOdoMiles() {
        if (odoUnits.equals("km")) {
            odoUnits = "miles";
        } else {
            odoUnits = "km";
        }
        btnOdoUnits.setText(odoUnits);
    }

    public void updateValues() {
        Intent intent = new Intent();
        intent.putExtra(TRUE_SPEED, textValue1.getText().toString());
        intent.putExtra(PREFERRED_MARGIN, textValue2.getText().toString());
        intent.putExtra(CAR_LOAD, textValue3.getText().toString());
        intent.putExtra(RANGE_UNITS, rangeUnits);
        intent.putExtra(ODO_UNITS, odoUnits);

        setResult(RESULT_OK, intent);
        finish();
    }
}
