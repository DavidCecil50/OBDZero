package dc.local.electriccar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import static dc.local.electriccar.MainActivity.CAPACITY_AH;
import static dc.local.electriccar.MainActivity.TRUE_SPEED;
import static dc.local.electriccar.MainActivity.PREFERRED_MARGIN;
import static dc.local.electriccar.MainActivity.CAR_LOAD;
import static dc.local.electriccar.MainActivity.CELL_CHEM;
import static dc.local.electriccar.MainActivity.RANGE_UNITS;
import static dc.local.electriccar.MainActivity.ODO_UNITS;
import static dc.local.electriccar.MainActivity.OCV_TYPE;
import static dc.local.electriccar.MainActivity.RECORD_TIME;

public class InitialValuesActivity extends Activity {
    private EditText textValue1;
    private EditText textValue2;
    private EditText textValue3;
    private EditText textValue4;
    private EditText textValue5;
    private Button btnCellChem;
    private String cellChem;
    private Button btnRangeUnits;
    private String rangeUnits;
    private Button btnOdoUnits;
    private String odoUnits;
    private Button btnOCV;
    private String OCV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial_values);

        textValue1 = findViewById(R.id.value_speed);
        textValue1.setText(MainActivity.i_Spd100.str());
        textValue2 = findViewById(R.id.value_margin);
        textValue2.setText(MainActivity.i_Safety.str());
        textValue3 = findViewById(R.id.value_load);
        textValue3.setText(MainActivity.i_Load.str());
        textValue5 = findViewById(R.id.value_cap);
        textValue5.setText(MainActivity.i_Capacity.str());
        textValue4 = findViewById(R.id.record_sec);
        textValue4.setText(MainActivity.i_Record.str());

        cellChem = MainActivity.i_Chem;
        btnCellChem = findViewById(R.id.cell_chem);
        btnCellChem.setText(cellChem);
        btnCellChem.setOnClickListener(v -> toogleCellNMC());

        rangeUnits = MainActivity.i_RangeUnits;
        btnRangeUnits = findViewById(R.id.range_units);
        btnRangeUnits.setText(rangeUnits);
        btnRangeUnits.setOnClickListener(v -> toogleRangeMiles());

        odoUnits = MainActivity.i_OdoUnits;
        btnOdoUnits = findViewById(R.id.odo_units);
        btnOdoUnits.setText(odoUnits);
        btnOdoUnits.setOnClickListener(v -> toogleOdoMiles());

        OCV = MainActivity.i_OCV;
        btnOCV = findViewById(R.id.ocv);
        btnOCV.setText(OCV);
        btnOCV.setOnClickListener(v -> toogleOCV());

        Button btnUpdate = findViewById(R.id.update);
        btnUpdate.setOnClickListener(v -> updateValues());

        // Set result CANCELED in case the user backs out
        setResult(RESULT_CANCELED);
    }

    private void toogleCellNMC() {
        if (cellChem.equals("LEV")) {
            cellChem = "NMC";
        } else {
            cellChem = "LEV";
        }
        btnCellChem.setText(cellChem);
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

    private void toogleOCV() {
        if (OCV.equals("old")) {
            OCV = "new";
        } else {
            OCV = "old";
        }
        btnOCV.setText(OCV);
    }

    public void updateValues() {
        Intent intent = new Intent();
        intent.putExtra(TRUE_SPEED, textValue1.getText().toString());
        intent.putExtra(PREFERRED_MARGIN, textValue2.getText().toString());
        intent.putExtra(CAR_LOAD, textValue3.getText().toString());
        intent.putExtra(CAPACITY_AH, textValue5.getText().toString());
        intent.putExtra(CELL_CHEM, cellChem);
        intent.putExtra(RANGE_UNITS, rangeUnits);
        intent.putExtra(ODO_UNITS, odoUnits);
        intent.putExtra(OCV_TYPE, OCV);
        intent.putExtra(RECORD_TIME, textValue4.getText().toString());

        setResult(RESULT_OK, intent);
        finish();
    }
}
