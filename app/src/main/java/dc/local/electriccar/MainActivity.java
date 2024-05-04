package dc.local.electriccar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static android.graphics.Color.BLACK;
import static android.os.Build.VERSION.SDK_INT;
import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static java.lang.System.currentTimeMillis;

/*
  This code is based on the BlueTerm code by pymasde.es found on GitHub and dated the 7th May 2014
*/
public class MainActivity extends AppCompatActivity {
    //Set to true to add debugging code and logging.
    static final String TAG = "Main Activity";
    static final boolean DEBUG = true;

    static final int PERMIT_NEARBY = 1;
    static final int PERMIT_STORAGE = 2;

    // Message types sent from the BluetoothReadService Handler
    static final int MESSAGE_STATE_CHANGE = 1;
    static final int MESSAGE_RECEIVED = 2;
    static final int MESSAGE_DEVICE_NAME = 4;

    // Key names received from the BluetoothChatService Handler
    static final String STATE = "state";
    static final String RECEIVED_LINE = "received_line";
    static final String DEVICE_NAME = "device_name";

    private BluetoothSerialService serviceSerial = null;
    private BluetoothAdapter adapterBluetooth = null;

    // Name of the connected device
    private static BluetoothDevice connectedDevice = null;
    private static String connectedDeviceName = null;
    private static String deviceMacAddress = null;

    private final Handler handlerMonitor = new Handler();

    private Button btnOne;
    private Button btnTwo;
    private Button btnThree;
    private Button btnFour;
    private Button btnFive;

    private View lineOne;
    private View lineTwo;
    private View lineThree;
    private View lineFour;
    private View lineFive;

    private TextView textCell;

    // Identifying the fragment in focus
    static final int FRAG_INFO = 1;
    static final int FRAG_PID = 2;
    static final int FRAG_CELLS = 3;
    static final int FRAG_OBD = 4;
    static final int FRAG_CALC = 5;
    static final int FRAG_WATTS = 6;
    static final int FRAG_DRV = 7;
    static final int FRAG_CHARGE = 8;
    static final int FRAG_CAP1 = 9;
    static final int FRAG_CAP2 = 10;
    static final int FRAG_AH = 11;
    static final int FRAG_WH = 12;
    static final int FRAG_TEMP = 13;
    static final int FRAG_VOLTS = 14;
    static final int FRAG_OPS = 15;

    private static int fragNo = FRAG_INFO;

    static final int clrDarkGreen = 0xFF047C14;

    static final String TRUE_SPEED = "speed";
    static final String PREFERRED_MARGIN = "margin";
    static final String CAR_LOAD = "load";
    static final String CAPACITY_AH = "charge";
    static final String CELL_CHEM = "LEV";
    static final String RANGE_UNITS = "km";
    static final String ODO_UNITS = "km";
    static final String OCV_TYPE = "old";
    static final String RECORD_TIME = "sec";

    private final static String[] collectedPIDs = {
            "012 5", "01C 8",
            "101 1", "119 8", "149 8", "156 8",
            "200 8", "208 8", "210 7", "212 8", "215 8", "231 8", "236 8", "285 8", "286 8", "288 8", "298 8", "29A 8", "2D0 8", "2F2 3",
            "300 8", "308 8", "325 2", "346 8", "373 8", "374 8", "375 8", "377 8", "384 8", "385 8", "389 8", "38A 8", "38D 8", "39B 8", "3A4 8",
            "408 8", "412 8", "418 7", "424 8",
            "564 8", "565 8", "568 8", "5A1 8",
            "695 8", "696 8", "697 8", "6D0 8", "6D1 8", "6D2 8", "6D3 8", "6D4 8", "6D5 8", "6D6 8", "6DA 8", "6FA 8",
            "75A 8", "75B 8"};
    // PIDs 6E1-6E4 and 762 are also collected but handled differently

    static final ArrayList<PID> allPIDs = new ArrayList<>();
    static final PID[] listPIDs = new PID[collectedPIDs.length + 80]; // PIDs 6E1-6E4 use 48 PIDs and 762 uses 27 PIDS

    static final CellSensor[] listSensors = new CellSensor[96];
    static final Cell[] listCells = new Cell[96];

    private ArrayList<String> listStoreInfo = new ArrayList<>();
    static ArrayList<String> listInfo = new ArrayList<>();

    static final ArrayList<String> arrayOBD = new ArrayList<>();

    private static File fileFolder = null;
    private static File appFolder = null;
    private static File fileInfo = null;
    private static File filePIDs = null;
    private static File filePIDInt = null;
    private static File fileCells = null;
    private static File fileSensors = null;
    private static File fileOBD = null;
    private static File fileCalc = null;

    private final static SimpleDateFormat fileDate = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
    private final static SimpleDateFormat dataDateDot = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.US);
    private final static SimpleDateFormat dataDateComma = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss,SSS", Locale.US);
    final static SimpleDateFormat displayDate = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    final static SimpleDateFormat displayTime = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private final static DecimalFormat decFix0 = new DecimalFormat("##0");
    final static DecimalFormat decFix1 = new DecimalFormat("##0.0");
    final static DecimalFormat decFix2 = new DecimalFormat("##0.00");

    private Dialog dialogAbout;

    private boolean runReset = false;
    private boolean isReset = false;
    private boolean runRestart = false;
    private boolean runCollector = false;
    private boolean iniComputing = false;
    private boolean runComputing = false;
    private boolean iniRecording = false;
    private boolean runRecording = false;
    private boolean cells88 = true;
    private boolean errorAC = false;
    private boolean isSleeping = false;
    private boolean iniShown = false;
    private boolean introShn = false;

    static boolean cellsData = false;
    static boolean milesPerkWh = false;
    static boolean miles = false;
    static boolean mph = false;
    static boolean checkRRMiles = false;
    static boolean checkOdoMiles = false;

    static int stepCap1 = 0;
    static int stepCap2 = 0;
    static int m_CellsNo = 88;

    private int m_newPIDs = 0;
    private int monitor = 0;
    private int menuTabs = 0;

    private long previousTime = 0;
    private long stepTime = 0;
    private long cycleTime = 0;
    private long recordTime = 0;
    private long bmuTime = 20000L;
    static long i_Time = 0;

    static final double KmPerMile = 1.609344;

    static Date stepDateTime = new Date();

    private static long p_Time = currentTimeMillis();
    private static double d_Hour = 0.0056;

    static double d_Second = 1.0;

    private final String[] strVIN = {"V", "I", "N"};

    static final OBD i_Spd100 = new OBD(95, "km/h", 0);
    static final OBD i_Margin = new OBD(10, "km", 0);
    static final OBD i_Load = new OBD(150, "kg", 0);
    static final OBD i_Record = new OBD(5, "sec", 0);
    static final OBD i_Capacity = new OBD(90, "Ah", 1);
    static final OBD i_RemAh = new OBD(0, "Ah", 1);

    static String i_Chem = "NMC";
    static String i_RangeUnits = "km";
    static String i_OdoUnits = "km";
    static String i_OCV = "old";

    static final OBD c_Mass = new OBD(0, "kg", 0);
    static final OBD c_Roll = new OBD(0, "", 2);
    static final OBD c_Drag = new OBD(0, "", 2);
    static final OBD p_Amps = new OBD(0, "A", 2);
    static final OBD c_OdoShown = new OBD(0, "km", 0);
    static final OBD p_Odo = new OBD(0, "km", 0);
    static final OBD c_SpdShown = new OBD(0, "km/h", 0);
    static final OBD c_SpdAvgRR = new OBD(0, "km/h", 1);
    static final OBD c_Speed0 = new OBD(0, "km/h", 2);
    static final OBD c_Speed1 = new OBD(0, "km/h", 2);
    static final OBD c_Speed2 = new OBD(0, "km/h", 2);
    static final OBD c_Speed3 = new OBD(0, "km/h", 2);
    static final OBD c_Speed4 = new OBD(0, "km/h", 2);
    static final OBD p_Speed = new OBD(0, "km/h", 2);
    static final OBD c_Acc = new OBD(0, "m/s2", 3);
    static final OBD c_Pedal = new OBD(0, "%", 2);
    static final OBD c_Steering = new OBD(0, "deg", 0);
    static final OBD c_Rotation = new OBD(0, "%", 2);
    static final OBD c_BrakeOn = new OBD(0, "", 0);
    static final OBD c_Brake = new OBD(0, "", 0);
    static final OBD c_RRshown = new OBD(0, "km", 0);
    static final OBD c_RRtest = new OBD(0, "km", 1);
    static final OBD c_kmTest = new OBD(0, "km", 1);
    static final OBD c_RPM = new OBD(0, "rpm", 0);
    static final OBD c_MotorA = new OBD(0, "A", 2);
    static final OBD c_RegA = new OBD(0, "A", 2);
    static final OBD c_RegW = new OBD(0, "W", 0);
    static final OBD c_QuickCharge = new OBD(0, "", 0);
    static final OBD c_QCprocent = new OBD(0, "%", 0);
    static final OBD c_QCAmps = new OBD(0, "A", 1);
    static final OBD c_ChargeVDC = new OBD(0, "V", 0);
    static final OBD c_ChargeVAC = new OBD(0, "V", 0);
    static final OBD c_ChargeADC = new OBD(0, "A", 2);
    static final OBD c_ChargeTemp1 = new OBD(0, "oC", 0);
    static final OBD c_ChargeTemp2 = new OBD(0, "oC", 0);
    static final OBD c_ChargeAAC = new OBD(0, "A", 2);
    static final OBD c_KeyOn = new OBD(0, "", 0);
    static final OBD c_AirSensor = new OBD(0, "oC", 0);
    static final OBD c_MotorTemp0 = new OBD(0, "oC", 0);
    static final OBD c_MotorTemp1 = new OBD(0, "oC", 0);
    static final OBD c_MotorTemp2 = new OBD(0, "oC", 0);
    static final OBD c_MotorTemp3 = new OBD(0, "oC", 0);
    static final OBD c_Model = new OBD(0, "", 0);
    static final OBD c_Gear = new OBD(0, "", 0);
    static final OBD c_Gear285 = new OBD(0, "", 0);
    static final OBD c_SpdShnAvg = new OBD(0, "km/h", 1);

    static final OBD c_Odo = new OBD(0, "km", 0);
    static final OBD c_AmpsCal = new OBD(0, "A", 2);
    static final OBD c_WattsCal = new OBD(0, "W", 0);
    static final OBD c_Speed0Avg = new OBD(0, "km/h", 1);
    static final OBD t_Margin = new OBD(0, "km", 0);

    static final OBD d_AhCal = new OBD(0, "Ah", 2);

    static final OBD b_Amps68 = new OBD(0, "A", 2);

    static final OBD b_SoC1 = new OBD(-1, "%", 1);
    static final OBD b_SoC2 = new OBD(-1, "%", 1);
    static final OBD b_Wavg = new OBD(0, "W", 0);
    static final OBD b_WavgRR = new OBD(0, "W", 0);
    static final OBD b_Whkm = new OBD(0, "Wh/km", 0);
    static final OBD b_BatTmax = new OBD(0, "oC", 0);
    static final OBD b_BatTmin = new OBD(0, "oC", 0);
    static final OBD b_Temp = new OBD(0, "oC", 1);
    static final OBD b_BatVavg = new OBD(0, "V", 2);
    static final OBD b_BatVmax = new OBD(0, "V", 2);
    static final OBD b_BatVmin = new OBD(0, "V", 2);
    static final OBD b_Volts = new OBD(0, "V", 1);
    static final OBD b_CapEst = new OBD(0, "Ah", 1);

    static final Ah c_Ah = new Ah(0, 0, 0, -1);
    static final Ah b_Ah = new Ah(0, 0, 0, -1);
    static final Ah m_Ah = new Ah(0, 0, 0, -1);
    static final Ah t_Ah = new Ah(0, 0, 0, -1);
    static final Ah bmu_Ah = new Ah(0, 0, 0, -1);
    static final Ah nmc_Ah = new Ah(0, 0, 0, -1);

    static boolean isCharging = false;
    static final OBD bmu_CapAh0 = new OBD(0, "Ah", 1);
    static final OBD bmu_RemAh0 = new OBD(0, "Ah", 1);
    static final OBD b_CapAh0 = new OBD(0, "Ah", 1);
    static final OBD b_RemAh0 = new OBD(0, "Ah", 1);
    static final OBD b_Volts0 = new OBD(0, "V", 1);
    static final OBD b_SoC10 = new OBD(0, "%", 1);
    static final OBD bmu_CapAh1 = new OBD(0, "Ah", 1);
    static final OBD bmu_RemAh1 = new OBD(0, "Ah", 1);
    static final OBD b_CapAh1 = new OBD(0, "Ah", 1);
    static final OBD b_RemAh1 = new OBD(0, "Ah", 1);
    static final OBD b_Volts1 = new OBD(0, "V", 1);
    static final OBD b_SoC11 = new OBD(0, "%", 1);

    static boolean pause1 = false;
    static final OBD p1_Time = new OBD(0, "min", 1);
    static final OBD p1_Volts = new OBD(0, "V", 0);
    static final OBD p1_Ah = new OBD(0, "Ah", 1);
    static final OBD p1_SoC = new OBD(0, "%", 1);
    static final OBD p2_Time = new OBD(0, "min", 1);
    static final OBD p2_Volts = new OBD(0, "V", 0);
    static final OBD p2_Ah = new OBD(0, "Ah", 1);
    static final OBD p2_SoC = new OBD(0, "%", 1);
    static final OBD p12_CapAh = new OBD(0, "Ah", 1);

    static boolean chargeFinished = false;

    static final Cell b_Cellmin = new Cell();
    static final Cell b_Cellavg = new Cell();
    static final Cell b_Cellmax = new Cell();

    static Cell m_CAh2min = new Cell();
    static Cell m_CAh2avg = new Cell();
    static Cell m_CAh2max = new Cell();

    static final OBD m_Error = new OBD(0, "kg/s", 1);
    static final OBD e_N = new OBD(0, "N", 0);
    static final OBD e_Watts = new OBD(0, "W", 0);

    static final OBD m_AccW = new OBD(0, "W", 1);
    static final OBD m_Amps = new OBD(0, "A", 2);
    static final OBD m_Odo = new OBD(0, "km", 1);
    static final OBD m_kmTest = new OBD(0, "km", 1);
    static final OBD m_Wind = new OBD(0, "m/s", 1);
    static final OBD mp_Amps = new OBD(0, "A", 1);
    static final OBD m_Watts = new OBD(0, "W", 0);
    static final OBD m_Whkm = new OBD(0, "Wh/km", 0);
    static final OBD m_Wavg = new OBD(0, "W", 0);
    static final OBD m_WavgRR = new OBD(0, "W", 0);
    static final OBD m_CapTemp = new OBD(0, "oC", 1);
    static final OBD m_SoCavg = new OBD(0, "oC", 1);

    static final OBD m_km = new OBD(0, "km", 1);
    static final OBD m_AuxW = new OBD(0, "W", 0);
    static final OBD m_OCtimer = new OBD(0, "mins", 1);

    static final OBD t_W = new OBD(0, "W", 0);
    static final OBD t_km = new OBD(0, "km", 1);
    static final OBD t_reqkm = new OBD(0, "km", 1);
    static final OBD t_Speed = new OBD(0, "km/h", 1);
    static final OBD t_Slope = new OBD(0, "", 1);

    static final OBD h_Amps = new OBD(0, "A", 1);
    static final OBD h_Watts = new OBD(0, "W", 0);
    static final OBD ac_Amps = new OBD(0, "A", 2);
    static final OBD ac_Watts = new OBD(0, "W", 0);
    static final OBD h_Level = new OBD(7, "", 0);
    static final OBD a_Fan = new OBD(0, "", 0);
    static final OBD a_Dirc = new OBD(4, "", 0);
    static final OBD ac_On = new OBD(0, "", 0);
    static final OBD a_Max = new OBD(0, "", 0);
    static final OBD a_Reci = new OBD(0, "", 0);
    static final OBD c_12vAmps = new OBD(0, "A", 2);
    static final OBD c_12vWatts = new OBD(0, "W", 0);

    static final OBD l_Park = new OBD(0, "", 0);
    static final OBD l_Drive = new OBD(0, "", 0);
    static final OBD l_FogFront = new OBD(0, "", 0);
    static final OBD l_FogRear = new OBD(0, "", 0);
    static final OBD l_High = new OBD(0, "", 0);
    static final OBD w_DeRear = new OBD(0, "", 0);
    static final OBD w_WiperF = new OBD(0, "", 0);

    private static MenuItem itemMenuConnect;
    private static MenuItem itemMenuStartStopData;
    private static MenuItem itemMenuStartStopComputing;
    private static MenuItem itemMenuStartStopRecording;

    private final SpannableString menu_initial = new SpannableString("Enter initial values");
    private final SpannableString menu_connect = new SpannableString("Connect dongle");
    private final SpannableString menu_disconnect = new SpannableString("Disconnect device");
    private final SpannableString menu_reset = new SpannableString("Reset OBD");
    private final SpannableString menu_start_all = new SpannableString("Start all");
    private final SpannableString menu_start_data = new SpannableString("Start data");
    private final SpannableString menu_stop_data = new SpannableString("Stop data");
    private final SpannableString menu_start_computing = new SpannableString("Start computing");
    private final SpannableString menu_stop_computing = new SpannableString("Stop computing");
    private final SpannableString menu_start_recording = new SpannableString("Start recording");
    private final SpannableString menu_stop_recording = new SpannableString("Stop recording");
    private final SpannableString menu_about = new SpannableString("About");

    static final int MENU_INT = 2;
    static final int MENU_CON = 3;
    static final int MENU_RST = 4;
    static final int MENU_ALL = 5;
    static final int MENU_CLT = 6;
    static final int MENU_CMP = 7;
    static final int MENU_REC = 8;
    static final int MENU_ABT = 9;

    private ActivityResultLauncher<Intent> DeviceListLauncher;
    private ActivityResultLauncher<Intent> InitialValuesLauncher;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (DEBUG) Log.i(TAG, "--- ON CREATE ---");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        for (int i = 0; i < listPIDs.length; i++) {
            PID aPID = new PID();
            listPIDs[i] = aPID;
        }

        for (int i = 0; i < listCells.length; i++) {
            Cell aCell = new Cell();
            listCells[i] = aCell;
        }

        for (int i = 0; i < listSensors.length; i++) {
            CellSensor aSensor = new CellSensor();
            listSensors[i] = aSensor;
        }

        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 8; j++) {
                listCells[i * 8 + j].module = i + 1;
                listCells[i * 8 + j].cell = j + 1;
                listCells[i * 8 + j].volts = 0;
                listCells[i * 8 + j].temperature = -50;
                listCells[i * 8 + j].SoC = -1;
                listSensors[i * 8 + j].module = i + 1;
                listSensors[i * 8 + j].sensor = j + 1;
                listSensors[i * 8 + j].temperature = -50;
            }
        }


        b_Cellmin.isFound = true;
        b_Cellmin.isNew = false;

        b_Cellavg.isFound = true;
        b_Cellavg.isNew = false;

        b_Cellmax.isFound = true;
        b_Cellmax.isNew = false;

        btnOne = findViewById(R.id.button1);
        btnTwo = findViewById(R.id.button2);
        btnThree = findViewById(R.id.button3);
        btnFour = findViewById(R.id.button4);
        btnFive = findViewById(R.id.button5);

        btnOne.setOnClickListener(v -> selectOne());
        btnTwo.setOnClickListener(v -> selectTwo());
        btnThree.setOnClickListener(v -> selectThree());
        btnFour.setOnClickListener(v -> selectFour());
        btnFive.setOnClickListener(v -> selectFive());

        btnOne.setText("Info");
        btnTwo.setText("Wh");
        btnThree.setText("Ah");
        btnFour.setText("Watts");
        btnFive.setText("Drive");

        btnOne.setTransformationMethod(null);
        btnTwo.setTransformationMethod(null);
        btnThree.setTransformationMethod(null);
        btnFour.setTransformationMethod(null);
        btnFive.setTransformationMethod(null);

        btnOne.setBackgroundColor(BLACK);
        btnTwo.setBackgroundColor(BLACK);
        btnThree.setBackgroundColor(BLACK);
        btnFour.setBackgroundColor(BLACK);
        btnFive.setBackgroundColor(BLACK);

        lineOne = findViewById(R.id.line1);
        lineTwo = findViewById(R.id.line2);
        lineThree = findViewById(R.id.line3);
        lineFour = findViewById(R.id.line4);
        lineFive = findViewById(R.id.line5);

        textCell = findViewById(R.id.text_celltype);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        DeviceListLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // When the request to enable Bluetooth returns
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Get the device MAC address
                        Intent iniValues = result.getData();
                        if (iniValues != null) {
                            deviceMacAddress = iniValues.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                            if (deviceMacAddress != null && deviceMacAddress.length() == 17) {
                                try {
                                    connectedDevice = adapterBluetooth.getRemoteDevice(deviceMacAddress);
                                    serviceSerial.connect(connectedDevice);
                                    storeInitialValues();
                                } catch (Exception e) {
                                    if (DEBUG) Log.i(TAG, "DeviceListLauncher " + e);
                                    updateInfo("app:Connection to the dongle");
                                    updateInfo("app:failed, cause unknown.");
                                }
                            } else {
                                updateInfo("app:Connection to the dongle failed");
                                updateInfo("app:Not a valid device address.");
                            }
                        }
                    } else {
                        updateInfo("app:Bluetooth device not chosen.");
                    }
                });

        InitialValuesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent iniValues = result.getData();
                        if (iniValues != null) {
                            String value = iniValues.getStringExtra(TRUE_SPEED);
                            if (value != null)
                                try {
                                    i_Spd100.dbl = parseDouble(value.replace(',', '.'));
                                } catch (NumberFormatException e) {
                                    if (DEBUG) Log.i(TAG, "NumberFormatException " + e);
                                    updateInfo("app:Initial value error speed correction");
                                }
                            value = iniValues.getStringExtra(PREFERRED_MARGIN);
                            if (value != null)
                                try {
                                    i_Margin.dbl = parseDouble(value.replace(',', '.'));
                                } catch (NumberFormatException e) {
                                    if (DEBUG) Log.i(TAG, "NumberFormatException " + e);
                                    updateInfo("app:Initial value error range remaining at station");
                                }
                            value = iniValues.getStringExtra(CAR_LOAD);
                            if (value != null)
                                try {
                                    i_Load.dbl = parseDouble(value.replace(',', '.'));
                                } catch (NumberFormatException e) {
                                    if (DEBUG) Log.i(TAG, "NumberFormatException " + e);
                                    updateInfo("app:Initial value error load");
                                }
                            value = iniValues.getStringExtra(CAPACITY_AH);
                            if (value != null)
                                try {
                                    i_Capacity.dbl = parseDouble(value.replace(',', '.'));
                                } catch (NumberFormatException e) {
                                    if (DEBUG) Log.i(TAG, "NumberFormatException " + e);
                                    updateInfo("app:Initial value error capacity");
                                }
                            value = iniValues.getStringExtra(RECORD_TIME);
                            if (value != null)
                                try {
                                    i_Record.dbl = parseDouble(value.replace(',', '.'));
                                } catch (NumberFormatException e) {
                                    if (DEBUG) Log.i(TAG, "NumberFormatException " + e);
                                    updateInfo("app:Initial value error record sec.");
                                }
                            if (i_Record.dbl > 60) i_Record.dbl = 60;
                            else if (i_Record.dbl < 0) i_Record.dbl = 0;

                            value = iniValues.getStringExtra(CELL_CHEM);
                            if (value != null) i_Chem = value;
                            value = iniValues.getStringExtra(RANGE_UNITS);
                            if (value != null) i_RangeUnits = value;
                            value = iniValues.getStringExtra(ODO_UNITS);
                            if (value != null) i_OdoUnits = value;
                            value = iniValues.getStringExtra(OCV_TYPE);
                            if (value != null) i_OCV = value;
                            storeInitialValues();
                            iniShown = false;
                            showInitialValues("New initial values...");
                        }
                    }
                });

        fragNo = 0;
        selectOne();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "--- ON START ---");

        if (SDK_INT > 30) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                serviceSerial = new BluetoothSerialService(handlerBT);
                BluetoothManager managerBluetooth = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
                if (managerBluetooth != null) {
                    adapterBluetooth = managerBluetooth.getAdapter();
                    adapterBluetooth.enable();
                } else {
                    updateInfo("app:Unknown Bluetooth error");
                    updateInfo("app:Check Bluetooth setting");
                }
                checkStorePermission();
            } else {
                updateInfo("app:OBDZero needs nearby device");
                updateInfo("app:permission for Bluetooth");
                updateInfo("app:connections.");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        PERMIT_NEARBY);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    == PackageManager.PERMISSION_GRANTED) {
                serviceSerial = new BluetoothSerialService(handlerBT);
                adapterBluetooth = BluetoothAdapter.getDefaultAdapter();
                adapterBluetooth.enable();
                checkStorePermission();
            } else {
                updateInfo("app:OBDZero requires Bluetooth");
                updateInfo("app:permission. Please grant this");
                updateInfo("app:in phone settings and restart");
                updateInfo("app:OBDZero.");
            }
        }
        updateFrag(FRAG_INFO);
    }

    private void checkStorePermission() {
        if (SDK_INT < 30) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                updateInfo("app:OBDZero needs this permission");
                updateInfo("app:to store data and initial values.");
                updateFrag(FRAG_INFO);
                // Permission is not granted
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMIT_STORAGE);
                // request Code PERMIT_STORAGE is an app-defined int constant.
                // The callback method, onRequestPermissionsResult,
                // gets the result of the request.

            } else {
                createFileFolders();
            }
        } else {
            createFileFolders();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMIT_NEARBY:
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    serviceSerial = new BluetoothSerialService(handlerBT);
                    adapterBluetooth = BluetoothAdapter.getDefaultAdapter();
                    adapterBluetooth.enable();
                    checkStorePermission();
                } else {
                    updateInfo("app:OBDZero does not have");
                    updateInfo("app:Bluetooth permission and");
                    updateInfo("app:will not work.");
                    updateFrag(FRAG_INFO);
                }
                break;
            case PERMIT_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createFileFolders();
                } else {
                    updateInfo("app:OBDZero does not have storage");
                    updateInfo("app:permission so stored initial");
                    updateInfo("app:values could not be retrieved");
                    updateInfo("app:and values and data collected");
                    updateInfo("app:by the app cannot be stored.");
                    updateInfo("app:Restart will not work either.");
                    updateInfo("app:But OBDZero should still work.");
                    updateFrag(FRAG_INFO);
                    finishOnStart();
                }
                break;
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "--- ON RESUME ---");
        monitor = 0;
        monitorOBD.run();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "--- ON PAUSE ---");
        handlerMonitor.removeCallbacks(monitorOBD);
        storeInitialValues();
        runRestart = false;
        if (runComputing || iniComputing) stopComputing();
        if (runCollector) stopData();
        if (serviceSerial != null && serviceSerial.getState() == BluetoothSerialService.STATE_CONNECTED)
            stopConnection();
        if (runRecording) StoreInfo();
        if (iniRecording || runRecording) stopRecording();
        exposeFiles();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "--- ON STOP ---");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.i(TAG, "--- ON DESTROY ---");
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);

        CharSequence rawTitle = "rawTitle";

        menu_initial.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_initial.length(), 0);
        menu_connect.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_connect.length(), 0);
        menu_disconnect.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_disconnect.length(), 0);
        menu_reset.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_reset.length(), 0);
        menu_start_all.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_start_all.length(), 0);
        menu_start_data.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_start_data.length(), 0);
        menu_stop_data.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_stop_data.length(), 0);
        menu_start_computing.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_start_computing.length(), 0);
        menu_stop_computing.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_stop_computing.length(), 0);
        menu_start_recording.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_start_recording.length(), 0);
        menu_stop_recording.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_stop_recording.length(), 0);
        menu_about.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_about.length(), 0);

        MenuItem itemMenuInitial = menu.getItem(MENU_INT);
        MenuItem itemMenuReset = menu.getItem(MENU_RST);
        MenuItem itemMenuAll = menu.getItem(MENU_ALL);
        itemMenuConnect = menu.getItem(MENU_CON);
        itemMenuStartStopData = menu.getItem(MENU_CLT);
        itemMenuStartStopComputing = menu.getItem(MENU_CMP);
        itemMenuStartStopRecording = menu.getItem(MENU_REC);
        MenuItem itemMenuAbout = menu.getItem(MENU_ABT);

        itemMenuInitial.setTitleCondensed(rawTitle);
        itemMenuConnect.setTitleCondensed(rawTitle);
        itemMenuReset.setTitleCondensed(rawTitle);
        itemMenuAll.setTitleCondensed(rawTitle);
        itemMenuStartStopData.setTitleCondensed(rawTitle);
        itemMenuStartStopComputing.setTitleCondensed(rawTitle);
        itemMenuStartStopRecording.setTitleCondensed(rawTitle);
        itemMenuAbout.setTitleCondensed(rawTitle);

        itemMenuInitial.setTitle(menu_initial);
        itemMenuReset.setTitle(menu_reset);
        itemMenuConnect.setTitle(menu_connect);
        itemMenuAll.setTitle(menu_start_all);
        itemMenuStartStopData.setTitle(menu_start_data);
        itemMenuStartStopComputing.setTitle(menu_start_computing);
        itemMenuStartStopRecording.setTitle(menu_start_recording);
        itemMenuAbout.setTitle(menu_about);
        return true;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        runRestart = false;
        int itemId = item.getItemId();
        if (itemId == R.id.initial_values) {
            doInitialValues();
            return true;
        } else if (itemId == R.id.connect) {
            toggleConnect();
            return true;
        } else if (itemId == R.id.reset) {
            doReset();
            return true;
        } else if (itemId == R.id.start_all) {
            if (!runCollector) startData();
            if (!iniComputing && !runComputing) startComputing();
            if (!iniRecording && !runRecording) startRecording();
            return true;
        } else if (itemId == R.id.start_stop_data) {
            toggleData();
            return true;
        } else if (itemId == R.id.start_stop_computing) {
            toggleComputing();
            return true;
        } else if (itemId == R.id.start_stop_recording) {
            toggleRecording();
            return true;
        } else if (itemId == R.id.menu_about) {
            showAboutDialog();
            return true;
        } else if (itemId == R.id.icon_tabulate) {
            menuTabs++;
            if (menuTabs > 2) menuTabs = 0;
            switch (menuTabs) {
                case 0:
                    btnOne.setText("Info");
                    btnTwo.setText("Wh");
                    btnThree.setText("Ah");
                    btnFour.setText("Watts");
                    btnFive.setText("Drive");
                    break;
                case 1:
                    btnOne.setText("Ops");
                    btnTwo.setText("OBD");
                    btnThree.setText("Cells");
                    btnFour.setText("Volts");
                    btnFive.setText("oC");
                    break;
                case 2:
                    btnOne.setText("PIDs");
                    btnTwo.setText("Calc");
                    btnThree.setText("Chrg");
                    btnFour.setText("Cap1");
                    btnFive.setText("Cap2");
            }
            return true;
        } else if (itemId == R.id.icon_restart) {
            doRestart();
            return true;
        }
        return false;
    }

    public void createFileFolders() {
        if (fileFolder == null || !fileFolder.exists()) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                if (SDK_INT < 30) {
                    fileFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/OBDZero");
                    appFolder = fileFolder;
                } else {
                    fileFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() +
                            "/OBDZero");
                    appFolder = new File(Objects.requireNonNull(getApplicationContext().getExternalFilesDir("")).getAbsolutePath());
                }

                if (!fileFolder.exists()) {
                    if (!fileFolder.mkdirs()) {
                        updateInfo("app:The folder " + fileFolder.toString() + "could");
                        updateInfo("app:not be created.");
                    }
                }
                if (!appFolder.exists()) {
                    if (!appFolder.mkdirs()) {
                        updateInfo("app:The folder " + appFolder.toString() + "could");
                        updateInfo("app:not be created.");
                    }
                }

            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                updateInfo("app:Recording failed.");
                updateInfo("app:Storage or SDCard are read only.");
            } else {
                updateInfo("app:Recording failed.");
                updateInfo("app:No user storage or");
                updateInfo("app:SDCard were found.");
                updateInfo("app:Stored initial values");
                updateInfo("app:could not be retrieved.");
            }
        }

        if (fileFolder != null && fileFolder.exists()) {
            if (!introShn) {
                updateInfo("app:Download a user manual at");
                updateInfo("app: OBDZero.dk");
                updateInfo("app:Data are stored in the");
                if (SDK_INT < 30)
                    updateInfo("app:OBDZero folder");
                else
                    updateInfo("app:Download/OBDZero folder");
                updateInfo("app:on the phone or on");
                updateInfo("app:an sdcard depending on how");
                updateInfo("app:the phone is setup.");
                introShn = true;
            }
        } else {
            updateInfo("app:OBDZero should still work.");
        }
        updateFrag(FRAG_INFO);
        finishOnStart();
    }

    public void finishOnStart() {
        getInitialValues();
        showInitialValues("Initial values...");
        if (serviceSerial.getState() != BluetoothSerialService.STATE_CONNECTED) {
            updateInfo("app:Use the menu to");
            updateInfo("app:connect the dongle.");
        } else if (!isReset) {
            updateInfo("app:Use the menu to");
            updateInfo("app:reset the dongle.");
        } else {
            updateInfo("app:Use the menu to");
            updateInfo("app:start data services.");
        }
        updateFrag(FRAG_INFO);
    }

    private void doRestart() {
        updateInfo("app:Restarting");
        if (serviceSerial != null) {
            runRestart = true;
            if (serviceSerial.getState() != BluetoothSerialService.STATE_CONNECTED) {
                if (deviceMacAddress != null && deviceMacAddress.length() == 17) {
                    try {
                        connectedDevice = adapterBluetooth.getRemoteDevice(deviceMacAddress);
                        serviceSerial.connect(connectedDevice);
                    } catch (Exception e) {
                        if (DEBUG) Log.e(TAG, "doRestart " + e);
                        runRestart = false;
                        updateInfo("app:The previously used dongle");
                        updateInfo("app:was not found.");
                        updateInfo("app:Please use the menu.");
                        updateInfo("app:The restart button should work");
                        updateInfo("app:next time.");
                    }
                } else {
                    runRestart = false;
                    updateInfo("app:No previously used dongle recorded");
                    updateInfo("app:Please use the menu.");
                    updateInfo("app:The restart button should work");
                    updateInfo("app:next time.");
                }
            } else if (!isReset) {
                doReset();
            } else {
                if (!runCollector) startData();
                if (!iniComputing && !runComputing) startComputing();
                if (!iniRecording && !runRecording) startRecording();
                runRestart = false;
            }
        } else {
            updateInfo("app:Bluetooth service is not");
            updateInfo("app:running. ");
            updateInfo("app:Please start Bluetooth and");
            updateInfo("app:start OBDZero again.");
            runRestart = false;
        }
        updateFrag(FRAG_INFO);
    }


    private void getInitialValues() {
        if (appFolder != null && appFolder.exists()) {
            boolean ok = true;
            File fileInitial = new File(appFolder, "OBDZero.ini");
            if (fileInitial.exists()) {
                ArrayList<String> linesToRead = new ArrayList<>();
                try {
                    FileReader fr = new FileReader(fileInitial);
                    BufferedReader br = new BufferedReader(fr);
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.length() > 0) linesToRead.add(line);
                    }
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "getInitialValues " + e);
                    updateInfo("app:Error reading initial values file");
                    ok = false;
                }

                if (ok) {
                    for (String aLine : linesToRead) {
                        if (aLine != null && aLine.contains(";")) {
                            String[] split = aLine.split(";");
                            if (split[1] != null && split[1].length() > 0) {
                                if (split[0].contains("True")) {
                                    try {
                                        i_Spd100.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        updateInfo("app:True speed at 100 km/h is not a number");
                                    }
                                } else if (split[0].contains("Remaining")) {
                                    try {
                                        i_Margin.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        updateInfo("app:The preferred margin is not a number");
                                    }
                                } else if (split[0].contains("Load")) {
                                    try {
                                        i_Load.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        updateInfo("app:The load in the car is not a number");
                                    }
                                } else if (split[0].contains("Range")) {
                                    i_RangeUnits = split[1];
                                    if (!i_RangeUnits.equals("km") && !i_RangeUnits.equals("miles")) {
                                        i_RangeUnits = "km";
                                        updateInfo("app:The range units are km or miles.");
                                    }
                                } else if (split[0].contains("Cell")) {
                                    i_Chem = split[1];
                                    if (!i_Chem.equals("LEV") && !i_Chem.equals("NMC")) {
                                        i_Chem = "LEV";
                                        updateInfo("app:The cell chemistry is LEV or NMC.");
                                    }
                                } else if (split[0].contains("capacity")) {
                                    try {
                                        i_Capacity.dbl = parseDouble(split[1].replace(',', '.'));
                                        nmc_Ah.cap = i_Capacity.dbl;
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        updateInfo("app:The battery capacity is not a number");
                                    }
                                } else if (split[0].contains("Odometer")) {
                                    i_OdoUnits = split[1];
                                    if (!i_OdoUnits.equals("km") && !i_OdoUnits.equals("miles")) {
                                        i_RangeUnits = "km";
                                        updateInfo("app:The odometer units are km or miles.");
                                    }
                                } else if (split[0].contains("Record")) {
                                    try {
                                        i_Record.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        updateInfo("app:The minimum time between data records not a number");
                                    }
                                    if (i_Record.dbl > 60) i_Record.dbl = 60;
                                    else if (i_Record.dbl < 0) i_Record.dbl = 0;
                                } else if (split[0].contains("OCV")) {
                                    i_OCV = split[1];
                                } else if (split[0].contains("Dongle")) {
                                    if (split[1].length() == 17) {
                                        deviceMacAddress = split[1];
                                    } else {
                                        deviceMacAddress = null;
                                    }
                                } else if (split[0].contains("time")) {
                                    try {
                                        i_Time = parseLong(split[1]);
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        updateInfo("app:error previous time");
                                    }
                                } else if (split[0].contains("remaining Ah")) {
                                    try {
                                        i_RemAh.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        updateInfo("app:Stored remaining Ah not a number.");
                                    }
                                }
                            }

                        } else if (aLine != null && aLine.contains(":")) {
                            updateInfo("app:" + aLine);
                            String[] split = aLine.split(":");
                            if (split[1] != null && split[1].length() > 0) {
                                if (split[0].contains("True")) {
                                    try {
                                        i_Spd100.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        updateInfo("app:True speed at 100 km/h is not a number");
                                    }
                                } else if (split[0].contains("Remaining")) {
                                    try {
                                        i_Margin.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        updateInfo("app:The preferred margin is not a number");
                                    }
                                } else if (split[0].contains("Load")) {
                                    try {
                                        i_Load.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        updateInfo("app:The load in the car is not a number");
                                    }
                                } else if (split[0].contains("Range")) {
                                    i_RangeUnits = split[1];
                                    if (!i_RangeUnits.equals("km") && !i_RangeUnits.equals("miles")) {
                                        i_RangeUnits = "km";
                                        updateInfo("app:The range units are km or miles.");
                                    }
                                } else if (split[0].contains("Odometer")) {
                                    i_OdoUnits = split[1];
                                    if (!i_OdoUnits.equals("km") && !i_OdoUnits.equals("miles")) {
                                        i_OdoUnits = "km";
                                        updateInfo("app:The odometer units are km or miles.");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    updateInfo("app:The previous initial values");
                    updateInfo("app:file OBDZero.ini could not");
                    updateInfo("app:be read due to Android 11+");
                    updateInfo("app:rules.");
                    updateInfo("app:A new file will be created");
                    updateInfo("app:with standard initial values.");
                }
            } else {
                updateInfo("app:The OBDZero.ini file");
                updateInfo("app:was not found.");
                updateInfo("app:A new file will be created");
            }
            storeInitialValues();
        } else {
            if (appFolder != null) {
                updateInfo("app:The " + appFolder + " folder");
                updateInfo("app:was not available and the");
                updateInfo("app:initial values were not retrieved.");
                updateInfo("app:Try closing and reopening OBDZero.");
            }
        }
        updateFrag(FRAG_INFO);
    }

    private void storeInitialValues() {
        if (appFolder != null && appFolder.exists()) {
            File fileInitial = new File(appFolder, "OBDZero.ini");
            boolean ok = true;
            if (fileInitial.exists()) ok = fileInitial.delete();
            if (ok) {
                try {
                    FileOutputStream out = new FileOutputStream(fileInitial);
                    OutputStreamWriter osw = new OutputStreamWriter(out);
                    String textToWrite;
                    textToWrite = "Number of initial values;10;\r\n";
                    textToWrite += "True speed at 100 km/h;" + i_Spd100.str() + ";\r\n";
                    textToWrite += "Remaining km at charging station;" + i_Margin.str() + ";\r\n";
                    textToWrite += "Load in the car kg;" + i_Load.str() + ";\r\n";
                    textToWrite += "Cell chemistry;" + i_Chem + ";\r\n";
                    textToWrite += "NMC capacity Ah;" + i_Capacity.str() + ";\r\n";
                    textToWrite += "Range units;" + i_RangeUnits + ";\r\n";
                    textToWrite += "Odometer units;" + i_OdoUnits + ";\r\n";
                    textToWrite += "OCV calibration;" + i_OCV + ";\r\n";
                    textToWrite += "Record minimum sec;" + i_Record.str() + ";\r\n";
                    if (deviceMacAddress != null) {
                        textToWrite += "Dongle in use;" + deviceMacAddress + ";\r\n";
                    } else {
                        textToWrite += "Dongle in use;none;\r\n";
                    }
                    textToWrite += "Stored at time;" + currentTimeMillis() + ";\r\n";
                    textToWrite += "NMC remaining Ah;" + nmc_Ah.remStr() + ";\r\n";
                    osw.write(textToWrite);
                    osw.flush();
                    osw.close();
                    MediaScannerConnection.scanFile(getApplicationContext(),
                            new String[]{
                                    fileInitial.toString()},
                            null,
                            (path, uri) -> Log.i(TAG,
                                    "file was scanned successfully: " + uri));

                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "storeInitialValues " + e);
                    updateInfo("app:" + e);
                    updateInfo("app:The OBDZero.ini file");
                    updateInfo("app:could not be created.");
                    updateInfo("app:The initial values were not saved.");
                }
            } else {
                updateInfo("app:The OBDZero.ini file");
                updateInfo("app:could not be deleted and recreated.");
                updateInfo("app:The initial values were not saved.");
            }
            updateFrag(FRAG_INFO);
        }
    }

    private void doInitialValues() {
        if (!runCollector) {
            Intent intent = new Intent(this, InitialValuesActivity.class);
            InitialValuesLauncher.launch(intent);
        } else {
            updateInfo("app:Please stop data before");
            updateInfo("app:changing the initial values.");
            updateFrag(FRAG_INFO);
        }
    }

    private void showInitialValues(String lineFirst) {
        if (!iniShown) {
            updateInfo("app:" + lineFirst);
            updateInfo("app:True speed at 100 km/h or mph: " + i_Spd100.str());
            updateInfo("app:Remaining km before charging: " + i_Margin.str());
            updateInfo("app:Load in the car kg: " + i_Load.str());
            updateInfo("app:Cell type LEV or NMC: " + i_Chem);
            updateInfo("app:Capacity NMC only Ah: " + i_Capacity.str());
            updateInfo("app:Range units (dashboard): " + i_RangeUnits);
            updateInfo("app:Odometer units (OBDscreen): " + i_OdoUnits);
            updateInfo("app:OCV calibration: " + i_OCV);
            updateInfo("app:Recording minimum seconds: " + i_Record.str());
            updateInfo("app:Use the menu to");
            updateInfo("app:change initial values.");
            updateFrag(FRAG_INFO);
            iniShown = true;
        }
    }

    private void toggleConnect() {
        if (serviceSerial != null && serviceSerial.getState() == BluetoothSerialService.STATE_CONNECTED) {
            stopConnection();
        } else {
            startConnection();
        }
    }

    private void startConnection() {
        if (serviceSerial != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (serviceSerial.getState() == BluetoothSerialService.STATE_NONE) {
                // Start the Bluetooth chat services
                serviceSerial.start();
            }
            // Launch the DeviceListActivity to see devices and do scan
            Intent intent = new Intent(this, DeviceListActivity.class);
            DeviceListLauncher.launch(intent);
        }
    }

    private void stopConnection() {
        isReset = false;
        if (itemMenuConnect != null) itemMenuConnect.setTitle(menu_connect);
        if (runComputing || iniComputing) stopComputing();
        if (runCollector) stopData();
        if (serviceSerial != null) serviceSerial.disconnect();
        updateFrag(FRAG_INFO);
    }

    /*
      This sends the command series "strReset" in Bluetooth serial service to the OBD device
     */
    private void doReset() {
        if (serviceSerial != null && serviceSerial.getState() == BluetoothSerialService.STATE_CONNECTED) {
            if (runComputing || iniComputing) stopComputing();
            if (runCollector) stopData();
            updateInfo("app:Resetting please wait");
            previousTime = currentTimeMillis();
            stepTime = 0;
            cycleTime = 0;
            runReset = true;
            isReset = false;
            serviceSerial.startReset();
        } else {
            updateInfo("app:Please connect to the dongle.");
        }
        updateFrag(FRAG_INFO);
    }

    private void toggleData() {
        if (runCollector) {
            stopData();
        } else {
            startData();
        }
    }

    private void startData() {
        if (serviceSerial != null && serviceSerial.getState() == BluetoothSerialService.STATE_CONNECTED) {
            itemMenuStartStopData.setTitle(menu_stop_data);
            updateInfo("app:OBD data collection started");
            runCollector = true;
            btnTwo.setBackgroundColor(clrDarkGreen);
            for (PID aPID : listPIDs) {
                aPID.isFound = false;
                aPID.isNew = false;
            }
            for (Cell aCell : listCells) {
                aCell.isFound = false;
                aCell.isNew = false;
            }
            for (CellSensor aSensor : listSensors) aSensor.isNew = false;
            previousTime = currentTimeMillis();
            stepTime = 0;
            cycleTime = 0;
            serviceSerial.startCollector();
        } else {
            updateInfo("app:Please connect to the dongle");
        }
        updateFrag(FRAG_INFO);
    }

    private void processData() {
        stepDateTime = new Date();
        calcOBDs();
        if (iniComputing) iniComputations();
        if (runComputing) doComputations();
        if (iniRecording) iniStorage();
        if (runRecording) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                String storedDateTime = strDateTime(stepDateTime);
                StorePIDs(storedDateTime);
                StorePIDIntegers(storedDateTime);
                if (recordTime > 1000L * i_Record.in() || bmuTime == 0) {
                    recordTime = 0;
                    StoreOBD(storedDateTime);
                    if (runComputing) StoreCalc(storedDateTime);
                    if (cellsData) {
                        StoreCells(storedDateTime);
                        StoreCellTemperatures(storedDateTime);
                    }
                }
            }
        }
        cycleTimer();
        updateInfo("app:Step " + stepTime + " ms.");
        stepTime = 0;
        recordTime += cycleTime;
        bmuTime += cycleTime;
        updateInfo("app:This cycle took " + cycleTime + " ms.");
        cycleTime = 0;
        updateFrag(fragNo);
        for (PID aPID : listPIDs) aPID.isNew = false;
        for (Cell aCell : listCells) aCell.isNew = false;
        for (CellSensor aSensor : listSensors) aSensor.isNew = false;
        allPIDs.clear();
        if (serviceSerial != null)
            if (bmuTime > 30000L) {
                bmuTime = 0;
                serviceSerial.startBMU();
            } else {
                serviceSerial.startCollector();
            }
    }

    private void stopData() {
        if (runComputing || iniComputing) stopComputing();
        if (serviceSerial != null) serviceSerial.stopCollector();
        itemMenuStartStopData.setTitle(menu_start_data);
        runCollector = false;
        btnTwo.setBackgroundColor(BLACK);
        btnThree.setBackgroundColor(BLACK);
        updateInfo("app:OBD data collection stopped");
        updateFrag(FRAG_INFO);
    }

    private void toggleComputing() {
        if (runComputing || iniComputing) {
            stopComputing();
        } else {
            startComputing();
        }
    }

    private void startComputing() {
        if (!runCollector) startData();
        updateInfo("app:Calc is waiting for data.");
        itemMenuStartStopComputing.setTitle(menu_stop_computing);
        iniComputing = true;
    }

    private void stopComputing() {
        itemMenuStartStopComputing.setTitle(menu_start_computing);
        iniComputing = false;
        runComputing = false;
        btnFour.setBackgroundColor(BLACK);
        updateInfo("app:Calculations stopped");
        updateFrag(FRAG_INFO);
    }

    private void toggleRecording() {
        if (iniRecording || runRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (fileFolder != null && fileFolder.exists()) {
            iniRecording = true;
        } else {
            iniRecording = false;
            updateInfo("app:OBDZero does not have");
            updateInfo("app:access to storage.");
            updateFrag(FRAG_INFO);
        }
    }

    private void stopRecording() {
        itemMenuStartStopRecording.setTitle(menu_start_recording);
        iniRecording = false;
        runRecording = false;
        btnFive.setBackgroundColor(BLACK);
        updateInfo("app:Recording stopped");
        updateFrag(FRAG_INFO);
    }

    @SuppressLint("SetTextI18n")
    private void showAboutDialog() {
        dialogAbout = new Dialog(this);
        dialogAbout.setContentView(R.layout.about);

        TextView name_version = dialogAbout.findViewById(R.id.app_version);
        name_version.setText("OBDZero Version " + BuildConfig.VERSION_NAME);

        Button buttonOpen = dialogAbout.findViewById(R.id.buttonDialog);
        buttonOpen.setOnClickListener(v -> dialogAbout.dismiss());
        dialogAbout.show();
    }

    private void readLine(String lineReceived) {
        if (lineReceived.contains(" ")) {
            String[] strSpace = lineReceived.split(" ");
            if (strSpace.length > 0) {
                if (strSpace[0].length() == 3 && strSpace.length < 10) {
                    monitor = 0;
                    PID aPID = new PID();
                    aPID.linePID = lineReceived;
                    aPID.str[0] = strSpace[0];
                    aPID.nBytes = strSpace.length - 1;
                    boolean allBytes = true;
                    for (int i = 0; i < Math.min(strSpace.length - 1, 8); i++) {
                        aPID.str[i + 1] = strSpace[i + 1];
                        aPID.intr[i] = convertHex(strSpace[i + 1]);
                        if (aPID.intr[i] == -1) {
                            allBytes = false;
                        }
                    }
                    if (allBytes) {
                        if (strSpace.length < 9) {
                            for (int i = strSpace.length - 1; i < 8; i++) {
                                aPID.str[i + 1] = "";
                                aPID.intr[i] = 0;
                            }
                        }
                        allPIDs.add(aPID);
                        sortPIDs(aPID);
                    }
                }
            }
        }
    }

    private int convertHex(String hh) {
        if (hh.length() == 2) {
            try {
                return Integer.parseInt(hh, 16);
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "converting Hex " + e);
                updateInfo("app:Error converting Hex to Integer");
                return -1;
            }
        } else {
            return -1;
        }
    }

    private void sortPIDs(PID aPID) {
        int module = aPID.intr[0];
        if (module > 96) module = module - 96;
        switch (aPID.str[0]) {
            case "6E1":
                if (module > 0 && module < 13 && aPID.nBytes == 8) {
                    aPID.intr[0] = module;
                    aPID.isNew = true;
                    aPID.isFound = true;
                    listPIDs[collectedPIDs.length + module] = aPID;
                }
                break;
            case "6E2":
                if (module > 0 && module < 13 && aPID.nBytes == 8) {
                    aPID.intr[0] = module;
                    aPID.isNew = true;
                    aPID.isFound = true;
                    listPIDs[collectedPIDs.length + 12 + module] = aPID;
                }
                break;
            case "6E3":
                if (module > 0 && module < 13 && aPID.nBytes == 8) {
                    aPID.intr[0] = module;
                    aPID.isNew = true;
                    aPID.isFound = true;
                    listPIDs[collectedPIDs.length + 24 + module] = aPID;
                }
                break;
            case "6E4":
                if (module > 0 && module < 13 && aPID.nBytes == 8) {
                    aPID.intr[0] = module;
                    aPID.isNew = true;
                    aPID.isFound = true;
                    listPIDs[collectedPIDs.length + 36 + module] = aPID;
                }
                break;
            case "762":
                if (aPID.intr[0] > 15 && aPID.intr[0] < 40 && aPID.nBytes == 8) {
                    aPID.isNew = true;
                    aPID.isFound = true;
                    listPIDs[collectedPIDs.length + 49 + aPID.intr[0] - 15] = aPID;
                }
                break;
            default:
                for (int j = 0; j < collectedPIDs.length; j++) {
                    String[] str = collectedPIDs[j].split(" ");
                    if (aPID.str[0].equals(str[0]) && aPID.nBytes == Integer.parseInt(str[1])) {
                        aPID.isNew = true;
                        aPID.isFound = true;
                        listPIDs[j] = aPID;
                    }
                }
                break;
        }
    }

    private void calcOBDs() {
        for (PID aPID : listPIDs) {
            switch (aPID.str[0]) {
                case "149":
                    c_Rotation.dbl = (aPID.intr[5] * 256 + aPID.intr[4] - 32934) / 32.934;
                    break;
                case "200":
                    if (aPID.intr[2] < 255)
                        c_Speed1.dbl = (aPID.intr[2] * 256 + aPID.intr[3] - 49152) / 19.0;
                    if (aPID.intr[4] < 255)
                        c_Speed2.dbl = (aPID.intr[4] * 256 + aPID.intr[5] - 49152) / 19.0;
                    break;
                case "208":
                    c_Brake.dbl = aPID.intr[3];
                    if (aPID.intr[4] < 255)
                        c_Speed3.dbl = (aPID.intr[4] * 256 + aPID.intr[5] - 49152) / 19.0;
                    if (aPID.intr[6] < 255)
                        c_Speed4.dbl = (aPID.intr[6] * 256 + aPID.intr[7] - 49152) / 19.0;
                    break;
                case "210":
                    c_Pedal.dbl = 100 * aPID.intr[2] / 256.0;
                    break;
                case "215":
                    if (aPID.intr[0] < 255)
                        c_Speed0.dbl = (256 * aPID.intr[0] + aPID.intr[1]) / 128.0;
                    break;
                case "231":
                    c_BrakeOn.dbl = aPID.intr[4];
                    break;
                case "236":
                    c_Steering.dbl = (aPID.intr[0] * 256 + aPID.intr[1] - 4096) / 30.0;
                    break;
                case "285":
                    c_Acc.dbl = ((aPID.intr[0] * 256 + aPID.intr[1]) - 2000) / 400.0;
                    calcGear(aPID);
                    break;
                case "286":
                    if (aPID.intr[3] > 0) c_AirSensor.dbl = aPID.intr[3] - 50.0;
                    break;
                case "298":
                    c_RPM.dbl = aPID.intr[6] * 256 + aPID.intr[7] - 10000;
                    c_MotorTemp0.dbl = aPID.intr[0] - 50;
                    c_MotorTemp1.dbl = aPID.intr[1] - 50;
                    c_MotorTemp2.dbl = aPID.intr[2] - 50;
                    c_MotorTemp3.dbl = aPID.intr[3] - 50;
                    break;
                case "29A":
                case "6FA":
                    calcVIN(aPID);
                    break;
                case "346":
                    c_RRshown.dbl = aPID.intr[7];
                    double RR;
                    if (c_RRshown.dbl < 255 && c_RRshown.dbl > 0) {
                        if (i_RangeUnits.equals("miles"))
                            RR = KmPerMile * c_RRshown.dbl;
                        else RR = c_RRshown.dbl;
                        if (RR > 0) c_Ah.Whkm = c_Ah.remWh10() / RR;
                    }
                    break;
                case "373":
                    if (aPID.intr[0] > 60 && aPID.intr[0] < 220)
                        b_BatVmax.dbl = (aPID.intr[0] + 210) / 100.0;
                    if (aPID.intr[1] > 60 && aPID.intr[1] < 220)
                        b_BatVmin.dbl = (aPID.intr[1] + 210) / 100.0;
                    if (b_BatVmin.dbl > 0 && b_BatVmax.dbl > 0)
                        b_BatVavg.dbl = (b_BatVmin.dbl + b_BatVmax.dbl) / 2.0;
                    b_Amps68.dbl = (aPID.intr[2] * 256 + aPID.intr[3] - 32768) / 100.0;
                    if (b_Amps68.dbl > -200 && b_Amps68.dbl < 200) {
                        c_AmpsCal.dbl = -(aPID.intr[2] * 256 + aPID.intr[3] - 32700) / 100.0;
                    }
                    if (aPID.intr[4] > 9) {
                        b_Volts.dbl = (aPID.intr[4] * 256 + aPID.intr[5]) / 10.0;
                        c_WattsCal.dbl = c_AmpsCal.dbl * b_Volts.dbl;
                    }
                    break;
                case "374":
                    if (aPID.intr[0] > 10) b_SoC1.dbl = (aPID.intr[0] - 10.0) / 2.0;
                    if (aPID.intr[1] > 10) b_SoC2.dbl = (aPID.intr[1] - 10.0) / 2.0;
                    b_BatTmax.dbl = (aPID.intr[4] - 50.0);
                    b_BatTmin.dbl = (aPID.intr[5] - 50.0);
                    if (aPID.intr[6] > 0) {
                        c_Ah.cap = aPID.intr[6] / 2.0;
                        if (b_SoC2.dbl > -1) c_Ah.rem = b_SoC2.dbl * c_Ah.cap / 100.0;
                    }
                    break;
                case "384":
                    if (aPID.intr[0] < 255) {
                        ac_Amps.dbl = (aPID.intr[0] * 256 + aPID.intr[1]) / 1000.0;
                        errorAC = false;
                    } else {
                        if (ac_On.in() == 1 && (a_Fan.in() > 0 || a_Max.in() > 0)) {
                            ac_Amps.dbl = 1;
                            errorAC = true;
                        } else {
                            ac_Amps.dbl = 0;
                            errorAC = false;
                        }
                    }
                    ac_Watts.dbl = ac_Amps.dbl * b_Volts.dbl;
                    c_12vAmps.dbl = aPID.intr[3] / 100.0;
                    c_12vWatts.dbl = c_12vAmps.dbl * b_Volts.dbl;
                    h_Amps.dbl = aPID.intr[4] / 10.0;
                    h_Watts.dbl = h_Amps.dbl * b_Volts.dbl;
                    break;
                case "389":
                    c_ChargeVDC.dbl = 2 * (aPID.intr[0] + 0.5);
                    c_ChargeVAC.dbl = aPID.intr[1];
                    c_ChargeADC.dbl = aPID.intr[2] / 10.0;
                    c_ChargeTemp1.dbl = aPID.intr[3] - 50.0;
                    c_ChargeTemp2.dbl = aPID.intr[4] - 50.0;
                    c_ChargeAAC.dbl = aPID.intr[6] / 10.0;
                    break;
                case "3A4":
                    calcAir(aPID);
                    break;
                case "412":
                    c_SpdShown.dbl = aPID.intr[1];
                    if (aPID.intr[2] < 255)
                        c_OdoShown.dbl = 256 * (256 * aPID.intr[2] + aPID.intr[3]) + aPID.intr[4];
                    if (i_OdoUnits.equals("miles")) c_Odo.dbl = KmPerMile * c_OdoShown.dbl;
                    else c_Odo.dbl = c_OdoShown.dbl;
                    if (aPID.intr[0] == 254) c_KeyOn.dbl = 1;
                    else c_KeyOn.dbl = 0;
                    break;
                case "418":
                    if (aPID.intr[0] < 255) {
                        c_Gear.dbl = aPID.intr[0];
                    }
                case "424":
                    calcLights(aPID);
                    calcRearDefrost(aPID);
                    calcWipers(aPID);
                    break;
                case "696":
                    if (aPID.intr[2] > 0 && aPID.intr[2] < 7)
                        c_MotorA.dbl = (256 * aPID.intr[2] + aPID.intr[3] - 500) / 20.0;
                    if (c_MotorA.dbl < 0) c_MotorA.dbl = 0;
                    if (aPID.intr[6] > 37 && aPID.intr[6] < 40)
                        c_RegA.dbl = (256 * aPID.intr[6] + aPID.intr[7] - 10000) / 5.0;
                    break;
                case "697":
                    c_QuickCharge.dbl = aPID.intr[0];
                    c_QCprocent.dbl = aPID.intr[1];
                    c_QCAmps.dbl = aPID.intr[2];
                    break;
                case "6E1":
                case "6E2":
                case "6E3":
                case "6E4":
                    cellsData = true;
                    calcCells(aPID);
                    break;
                case "762":
                    if (aPID.intr[0] == 36) {
                        if (aPID.intr[3] > 0 || aPID.intr[4] > 0) {
                            bmu_Ah.cap = (aPID.intr[3] * 256 + aPID.intr[4]) / 10.0;
                        }
                        if (aPID.intr[5] > 0 || aPID.intr[6] > 0) {
                            bmu_Ah.rem = (aPID.intr[5] * 256 + aPID.intr[6]) / 10.0;
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        int newPIDs = 0;
        int foundPIDs = 0;
        for (PID aPID : listPIDs) {
            if (aPID.isFound) foundPIDs++;
            if (aPID.isNew) newPIDs++;
        }
        if (newPIDs == 0 && m_newPIDs == 0) c_AmpsCal.dbl = 0;
        m_newPIDs = newPIDs;

        updateInfo("app:PIDs detected since start: " + foundPIDs);
        updateInfo("app:PIDs updated in this cycle: " + newPIDs);

        calcCellNumber();
        cells88 = m_CellsNo == 88;

        int newCells = 0;
        int foundCells = 0;
        if (cellsData) {
            for (Cell aCell : listCells) {
                if (aCell.isFound) foundCells++;
                if (aCell.isNew) newCells++;
            }
            if (foundCells == m_CellsNo) {
                btnThree.setBackgroundColor(clrDarkGreen);
            } else {
                btnThree.setBackgroundColor(BLACK);
            }
        } else {
            btnThree.setBackgroundColor(BLACK);
        }

        calcCellTemperatures();
        calcCellSoC();
        calcCellVMaxMin();

        updateInfo("app:Cells detected since start: " + foundCells);
        updateInfo("app:Cells updated in this cycle: " + newCells);
    }

    private void calcGear(PID aPID) {
        if (aPID.intr[6] == 12) {
            if (aPID.intr[7] == 16) {
                c_Gear285.dbl = 1;
            } else {
                c_Gear285.dbl = 3;
            }
        } else {
            if (aPID.intr[6] == 14) {
                if (aPID.intr[7] == 16) {
                    c_Gear285.dbl = 4;
                } else {
                    c_Gear285.dbl = 2;
                }
            } else {
                c_Gear285.dbl = 0;
            }
        }
    }

    private String convertIntegerToBinary(int i) {
        String bin;
        try {
            bin = String.format("%8s", Integer.toBinaryString(i)).replace(" ", "0");
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "converting integer " + e);
            updateInfo("app:Error converting integer to binary");
            bin = "";
        }
        return bin;
    }

    private void calcAir(PID aPID) {
        String bin = convertIntegerToBinary(aPID.intr[0]);
        if (bin.length() == 8) {
            if (bin.charAt(2) == '1') { //2,32
                a_Max.dbl = 1;
            } else {
                a_Max.dbl = 0;
            }
            if (bin.charAt(1) == '1') { //4,64
                a_Reci.dbl = 1;
            } else {
                a_Reci.dbl = 0;
            }
            if (bin.charAt(0) == '1') { //8,128
                ac_On.dbl = 1;
            } else {
                ac_On.dbl = 0;
            }
        }

        if (aPID.str[1].length() == 2) {
            try {
                h_Level.dbl = Integer.parseInt("0" + aPID.str[1].charAt(1), 16);
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "heat/cool " + e);
                updateInfo("app:Error computing the heat/cool position");
            }
        }

        if (aPID.str[2].length() == 2) {
            try {
                a_Dirc.dbl = Integer.parseInt("0" + aPID.str[2].charAt(0), 16);
                a_Fan.dbl = Integer.parseInt("0" + aPID.str[2].charAt(1), 16);
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "fan direction " + e);
                updateInfo("app:Error computing the fan direction and speed");
            }
        }
    }

    private void calcLights(PID aPID) {
        String bin = convertIntegerToBinary(aPID.intr[1]);
        if (bin.length() == 8) {
            if (bin.charAt(5) == '1') { //4
                l_High.dbl = 1;
            } else {
                l_High.dbl = 0;
            }
            if (bin.charAt(2) == '1') { //2,32
                l_Drive.dbl = 1;
            } else {
                l_Drive.dbl = 0;
            }
            if (bin.charAt(1) == '1') { //4,64
                l_Park.dbl = 1;
            } else {
                l_Park.dbl = 0;
            }
        }

        bin = convertIntegerToBinary(aPID.intr[0]);
        if (bin.length() == 8) {
            if (bin.charAt(4) == '1') { //8
                l_FogFront.dbl = 1;
            } else {
                l_FogFront.dbl = 0;
            }
            if (bin.charAt(3) == '1') { //1,16
                l_FogRear.dbl = 1;
            } else {
                l_FogRear.dbl = 0;
            }
        }
    }

    private void calcRearDefrost(PID aPID) {
        String bin = convertIntegerToBinary(aPID.intr[6]);
        if (bin.length() == 8) {
            if (bin.charAt(4) == '1') { //8
                w_DeRear.dbl = 1;
            } else {
                w_DeRear.dbl = 0;
            }
        }
    }

    private void calcWipers(PID aPID) {
        String bin = convertIntegerToBinary(aPID.intr[1]);
        if (bin.length() == 8) {
            if (bin.charAt(4) == '1') { //8
                w_WiperF.dbl = 1;
            } else {
                w_WiperF.dbl = 0;
            }
        }
    }

    private void calcCells(PID aPID) {
        if (cellsData)
            if (aPID.isNew) {
                int module = aPID.intr[0];
                switch (aPID.str[0]) {
                    case "6E1":
                        int index = (module - 1) * 8;
                        listCells[index].module = module;
                        listCells[index].cell = 1;
                        if ((aPID.intr[4] == 0 && aPID.intr[5] > 60) || aPID.intr[4] > 0)
                            listCells[index].volts = (aPID.intr[4] * 256 + aPID.intr[5] + 420) / 200.0;
                        listCells[index].isFound = true;
                        listCells[index].isNew = true;

                        listSensors[index].module = module;
                        listSensors[index].sensor = 1;
                        listSensors[index].temperature = aPID.intr[2] - 50;
                        listSensors[index].isNew = true;

                        index = (module - 1) * 8 + 1;
                        listCells[index].module = module;
                        listCells[index].cell = 2;
                        if ((aPID.intr[6] == 0 && aPID.intr[7] > 60) || aPID.intr[6] > 0)
                            listCells[index].volts = (aPID.intr[6] * 256 + aPID.intr[7] + 420) / 200.0;
                        listCells[index].isFound = true;
                        listCells[index].isNew = true;

                        listSensors[index].module = module;
                        listSensors[index].sensor = 2;
                        listSensors[index].temperature = aPID.intr[3] - 50;
                        listSensors[index].isNew = true;
                        break;
                    case "6E2":
                        index = (module - 1) * 8 + 2;
                        listCells[index].module = module;
                        listCells[index].cell = 3;
                        if ((aPID.intr[4] == 0 && aPID.intr[5] > 60) || aPID.intr[4] > 0)
                            listCells[index].volts = (aPID.intr[4] * 256 + aPID.intr[5] + 420) / 200.0;
                        listCells[index].isFound = true;
                        listCells[index].isNew = true;

                        listSensors[index].module = module;
                        listSensors[index].sensor = 3;
                        listSensors[index].temperature = aPID.intr[1] - 50;
                        listSensors[index].isNew = true;

                        index = (module - 1) * 8 + 3;
                        listCells[index].module = module;
                        listCells[index].cell = 4;
                        if ((aPID.intr[6] == 0 && aPID.intr[7] > 60) || aPID.intr[6] > 0)
                            listCells[index].volts = (aPID.intr[6] * 256 + aPID.intr[7] + 420) / 200.0;
                        listCells[index].isFound = true;
                        listCells[index].isNew = true;

                        if (module != 6 && module != 12) {
                            listSensors[index].module = module;
                            listSensors[index].sensor = 4;
                            listSensors[index].temperature = aPID.intr[2] - 50;
                            listSensors[index].isNew = true;
                        }
                        break;
                    case "6E3":
                        if (module != 6 && module != 12) {
                            index = (module - 1) * 8 + 4;
                            listCells[index].module = module;
                            listCells[index].cell = 5;
                            if ((aPID.intr[4] == 0 && aPID.intr[5] > 60) || aPID.intr[4] > 0)
                                listCells[index].volts = (aPID.intr[4] * 256 + aPID.intr[5] + 420) / 200.0;
                            listCells[index].isFound = true;
                            listCells[index].isNew = true;

                            listSensors[index].module = module;
                            listSensors[index].sensor = 5;
                            listSensors[index].temperature = aPID.intr[1] - 50;
                            listSensors[index].isNew = true;

                            index = (module - 1) * 8 + 5;
                            listCells[index].module = module;
                            listCells[index].cell = 6;
                            if ((aPID.intr[6] == 0 && aPID.intr[7] > 60) || aPID.intr[6] > 0)
                                listCells[index].volts = (aPID.intr[6] * 256 + aPID.intr[7] + 420) / 200.0;
                            listCells[index].isFound = true;
                            listCells[index].isNew = true;

                            listSensors[index].module = module;
                            listSensors[index].sensor = 6;
                            listSensors[index].temperature = aPID.intr[2] - 50;
                            listSensors[index].isNew = true;
                        }
                        break;
                    case "6E4":
                        if (module != 6 && module != 12) {
                            index = (module - 1) * 8 + 6;
                            listCells[index].module = module;
                            listCells[index].cell = 7;
                            if ((aPID.intr[4] == 0 && aPID.intr[5] > 60) || aPID.intr[4] > 0)
                                listCells[index].volts = (aPID.intr[4] * 256 + aPID.intr[5] + 420) / 200.0;
                            listCells[index].isFound = true;
                            listCells[index].isNew = true;

                            index = (module - 1) * 8 + 7;
                            listCells[index].module = module;
                            listCells[index].cell = 8;
                            if ((aPID.intr[6] == 0 && aPID.intr[7] > 60) || aPID.intr[6] > 0)
                                listCells[index].volts = (aPID.intr[6] * 256 + aPID.intr[7] + 420) / 200.0;
                            listCells[index].isFound = true;
                            listCells[index].isNew = true;

                        }
                        break;
                }
            }
    }

    private void calcCellNumber() {
        if (cellsData)
            if (b_Volts.dbl > 0 && b_BatVmax.dbl > 0 && b_BatVmin.dbl > 0) {
                double cells = b_Volts.dbl / ((b_BatVmax.dbl + b_BatVmin.dbl) / 2.0);
                if (cells > 79 && cells < 89)
                    if (cells < 84) {
                        m_CellsNo = 80;
                        for (Cell aCell : listCells) {
                            if (aCell.module == 6 || aCell.module == 12) {
                                aCell.volts = 0;
                                aCell.temperature = -50;
                                aCell.isFound = false;
                                aCell.isNew = false;
                            }
                        }
                        for (CellSensor aSensor : listSensors) {
                            if (aSensor.module == 6 || aSensor.module == 12) {
                                aSensor.temperature = -50;
                                aSensor.isNew = false;
                            }
                        }
                    } else {
                        m_CellsNo = 88;
                    }
            }
    }

    private void calcCellTemperatures() {
        if (cellsData) {
            double sum = 0;
            int k = 0;
            for (int i = 0; i < 96; i++)
                if (listCells[i].isFound)
                    switch (listCells[i].cell) {
                        case 1:
                            listCells[i].temperature = listSensors[i].temperature;
                            break;
                        case 2:
                        case 3:
                            listCells[i].temperature = (listSensors[i - 1].temperature + listSensors[i].temperature) / 2.0;
                            break;
                        case 4:
                        case 5:
                            listCells[i].temperature = listSensors[i - 1].temperature;
                            break;
                        case 6:
                        case 7:
                            listCells[i].temperature = (listSensors[i - 2].temperature + listSensors[i - 1].temperature) / 2.0;
                            break;
                        case 8:
                            listCells[i].temperature = listSensors[i - 2].temperature;
                            break;
                        default:
                            listCells[i].temperature = -50;
                            break;
                    }

            for (Cell aCell : listCells)
                if (aCell.isFound && aCell.temperature > -50) {
                    sum += aCell.temperature;
                    k++;
                }
            if (k > 79) b_Temp.dbl = sum / k;
            else b_Temp.dbl = (b_BatTmax.dbl + b_BatTmin.dbl) / 2.0;
        }
    }

    private void calcCellSoC() {
        if (cellsData) {
            double SoC = 0;
            double sum = 0;
            int i = 0;
            for (Cell aCell : listCells) {
                if (aCell.isFound) SoC = OCV.model(aCell.SoC, aCell.volts, i_Chem, i_OCV);
                if (SoC > -1) {
                    aCell.SoC = SoC;
                    sum += aCell.SoC;
                    i++;
                }
            }
            if (i > 79) m_SoCavg.dbl = sum / i;
            else m_SoCavg.dbl = -1;
        }
    }

    private void calcCellVMaxMin() {
        if (cellsData) {
            double maxVolts = 0.0;
            double minVolts = 5.0;
            for (Cell aCell : listCells) {
                if (aCell.isFound) {
                    if ((cells88 || aCell.module != 6) && (cells88 || aCell.module != 12)) {
                        if (aCell.volts > maxVolts) {
                            maxVolts = aCell.volts;
                            b_Cellmax.volts = aCell.volts;
                            b_Cellmax.module = aCell.module;
                            b_Cellmax.cell = aCell.cell;
                            b_Cellmax.SoC = aCell.SoC;
                            b_Cellmax.temperature = aCell.temperature;
                            b_Cellmax.isFound = true;
                            b_Cellmax.isNew = true;
                        } else if (aCell.volts < minVolts) {
                            minVolts = aCell.volts;
                            b_Cellmin.volts = aCell.volts;
                            b_Cellmin.module = aCell.module;
                            b_Cellmin.cell = aCell.cell;
                            b_Cellmin.SoC = aCell.SoC;
                            b_Cellmin.temperature = aCell.temperature;
                            b_Cellmin.isFound = true;
                            b_Cellmin.isNew = true;
                        }
                    }
                }
            }
        } else {
            b_Cellmax.volts = b_BatVmax.dbl;
            b_Cellmax.temperature = b_Temp.dbl;
            b_Cellmax.SoC = OCV.model(b_Cellmax.SoC, b_Cellmax.volts, i_Chem, i_OCV);
            b_Cellmax.isFound = true;
            b_Cellmax.isNew = true;
            b_Cellmin.volts = b_BatVmin.dbl;
            b_Cellmin.temperature = b_Temp.dbl;
            b_Cellmin.SoC = OCV.model(b_Cellmin.SoC, b_Cellmin.volts, i_Chem, i_OCV);
            b_Cellmin.isFound = true;
            b_Cellmin.isNew = true;
        }
        b_Cellavg.volts = b_Volts.dbl / m_CellsNo;
        b_Cellavg.temperature = b_Temp.dbl;
        b_Cellavg.SoC = OCV.model(b_Cellavg.SoC, b_Cellavg.volts, i_Chem, i_OCV);
        b_Cellavg.isFound = true;
        b_Cellavg.isNew = true;
    }

    private void calcVIN(PID aPID) {
        char[] VIN = new char[7];
        switch (aPID.intr[0]) {
            case 0:
                VIN[0] = (char) aPID.intr[1];
                VIN[1] = (char) aPID.intr[2];
                VIN[2] = (char) aPID.intr[3];
                VIN[3] = (char) aPID.intr[4];
                VIN[4] = (char) aPID.intr[5];
                VIN[5] = (char) aPID.intr[6];
                VIN[6] = (char) aPID.intr[7];
                strVIN[0] = String.valueOf(VIN);
                break;
            case 1:
                VIN[0] = (char) aPID.intr[1];
                VIN[1] = (char) aPID.intr[2];
                VIN[2] = (char) aPID.intr[3];
                VIN[3] = (char) aPID.intr[4];
                VIN[4] = (char) aPID.intr[5];
                VIN[5] = (char) aPID.intr[6];
                VIN[6] = (char) aPID.intr[7];
                strVIN[1] = String.valueOf(VIN);
                switch (aPID.intr[3]) {
                    case 56:
                        c_Model.dbl = 2008;
                        break;
                    case 57:
                        c_Model.dbl = 2009;
                        break;
                    case 65:
                        c_Model.dbl = 2010;
                        break;
                    case 66:
                        c_Model.dbl = 2011;
                        break;
                    case 67:
                        c_Model.dbl = 2012;
                        break;
                    case 68:
                        c_Model.dbl = 2013;
                        break;
                    case 69:
                        c_Model.dbl = 2014;
                        break;
                    case 70:
                        c_Model.dbl = 2015;
                        break;
                    case 71:
                        c_Model.dbl = 2016;
                        break;
                    case 72:
                        c_Model.dbl = 2017;
                        break;
                    case 74:
                        c_Model.dbl = 2018;
                        break;
                    case 75:
                        c_Model.dbl = 2019;
                        break;
                    case 76:
                        c_Model.dbl = 2020;
                        break;
                    case 77:
                        c_Model.dbl = 2021;
                        break;
                    case 78:
                        c_Model.dbl = 2022;
                        break;
                    default:
                        break;
                }
                break;
            case 2:
                VIN[0] = (char) aPID.intr[1];
                VIN[1] = (char) aPID.intr[2];
                VIN[2] = (char) aPID.intr[3];
                strVIN[2] = String.valueOf(VIN);
                break;
            default:
                break;
        }
    }

    private void iniComputations() {
        m_Odo.dbl = c_Odo.dbl;
        p_Odo.dbl = c_Odo.dbl;
        c_kmTest.dbl = 5;
        m_kmTest.dbl = 5;
        p_Amps.dbl = c_AmpsCal.dbl;
        mp_Amps.dbl = c_AmpsCal.dbl;
        d_AhCal.dbl = 0;
        p_Speed.dbl = c_Speed0.dbl;
        m_OCtimer.dbl = 0;

        b_Ah.Whkm = c_Ah.Whkm;
        m_Ah.Whkm = c_Ah.Whkm;
        t_Ah.Whkm = c_Ah.Whkm;
        bmu_Ah.Whkm = c_Ah.Whkm;
        nmc_Ah.Whkm = c_Ah.Whkm;

        b_Whkm.dbl = c_Ah.Whkm;
        m_Whkm.dbl = c_Ah.Whkm;

        t_Ah.cap = c_Ah.cap;
        t_Ah.rem = c_Ah.rem;

        c_RRtest.dbl = c_Ah.RR();

        c_Speed0Avg.dbl = 20;
        c_SpdAvgRR.dbl = c_Speed0Avg.dbl;

        b_Wavg.dbl = c_Ah.Whkm * c_Speed0Avg.dbl;
        m_Wavg.dbl = b_Wavg.dbl;

        computeAuxW();
        b_WavgRR.dbl = b_Wavg.dbl - m_AuxW.dbl;
        m_WavgRR.dbl = b_WavgRR.dbl;

        t_Speed.dbl = c_SpdAvgRR.dbl;

        boolean found = false;
        double SoC = 0;
        double rem;
        if (i_RemAh.dbl > 0) {
            if ((currentTimeMillis() - i_Time) < 30000) {
                nmc_Ah.rem = i_RemAh.dbl;
                found = true;
            } else if (m_SoCavg.dbl > 0) {
                if (nmc_Ah.cap > 0) SoC = 100.0 * i_RemAh.dbl / nmc_Ah.cap;
                if (Math.abs(m_SoCavg.dbl - SoC) < 5) {
                    nmc_Ah.rem = i_RemAh.dbl;
                    found = true;
                }
            } else if (bmu_Ah.cap > 0 && bmu_Ah.rem > 0) {
                rem = nmc_Ah.cap + bmu_Ah.cap - bmu_Ah.rem;
                if (Math.abs(i_RemAh.dbl - rem) < 5) {
                    nmc_Ah.rem = i_RemAh.dbl;
                    found = true;
                }
            } else if (c_Ah.cap > 0 && c_Ah.rem > 0) {
                rem = nmc_Ah.cap - c_Ah.cap + c_Ah.rem;
                if (Math.abs(i_RemAh.dbl - rem) < 5) {
                    nmc_Ah.rem = i_RemAh.dbl;
                    found = true;
                }
            }
        } else {
            if (bmu_Ah.cap > 0 && bmu_Ah.rem > 0) {
                nmc_Ah.rem = nmc_Ah.cap - bmu_Ah.used();
            } else if (c_Ah.cap > 0 && c_Ah.rem > 0) {
                nmc_Ah.rem = nmc_Ah.cap - c_Ah.used();
            } else if (m_SoCavg.dbl > 0) {
                nmc_Ah.rem = m_SoCavg.dbl * nmc_Ah.cap / 100.0;
            }
        }

        if (i_Chem.equals("NMC")) {
            b_Ah.cap = nmc_Ah.cap;
            b_Ah.rem = nmc_Ah.rem;
        } else {
            b_Ah.cap = c_Ah.cap;
            b_Ah.rem = c_Ah.rem;
        }

        if (c_Ah.cap == b_Ah.cap && c_Ah.rem > 0) {
            b_CapEst.dbl = b_Ah.cap * b_Ah.rem / c_Ah.rem;
        } else {
            b_CapEst.dbl = -1;
        }

        m_Ah.cap = b_Ah.cap;
        m_Ah.rem = b_Ah.rem;

        m_Error.dbl = 10;
        e_N.dbl = m_Error.dbl * c_Speed0Avg.dbl / 3.6;
        e_Watts.dbl = e_N.dbl * c_Speed0Avg.dbl / 3.6;

        c_Mass.dbl = 1120 + i_Load.dbl;
        c_Roll.dbl = 9.89 * 0.018 * c_Mass.dbl;

        stepCap1 = 0;
        stepCap2 = 0;

        pause1 = false;
        p1_Ah.dbl = 0;
        p1_SoC.dbl = 0;
        p2_Ah.dbl = 0;
        p2_SoC.dbl = 0;
        p12_CapAh.dbl = 0;

        if (c_Ah.cap > 0 && c_Ah.rem > 0 && c_Ah.Whkm > 0 && m_SoCavg.dbl > -1 && b_SoC2.dbl > -1 && c_Odo.dbl > 0) {
            if (i_Chem.equals("NMC") && !found) {
                CharSequence text = "Please check the remaining Ah" +
                        "OBDZero was not able to find a good value.";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(this.getApplicationContext(), text, duration);
                toast.show();
            }
            iniComputing = false;
            runComputing = true;
            btnFour.setBackgroundColor(clrDarkGreen);
            updateInfo("app:Calculations started");
        }

    }

    private void doComputations() {

        long time = currentTimeMillis();
        d_Second = (time - p_Time) / 1000.0; //time since the last computation in seconds
        d_Hour = d_Second / 3600.0; //time since the last computation in hours

        if (d_Second > 0.5) {

            updateLowAmpTimer();

            computeAuxW();

            computeAccW();

            computeRegW();

            if (c_Speed0.dbl > 0 && !errorAC) {
                m_Watts.dbl = computeModel(c_Speed0.dbl, m_AccW.dbl, m_AuxW.dbl);
                if (d_Second < 10)
                    // The difference between the effect reported by the car and the effect computed by the model
                    m_Error.dbl += 0.00002 * d_Second * (c_WattsCal.dbl - m_Watts.dbl);
                if (m_Error.dbl > 100) m_Error.dbl = 100;
                if (m_Error.dbl < -50) m_Error.dbl = -50;
                if (b_Volts.dbl > 0) m_Amps.dbl = m_Watts.dbl / b_Volts.dbl;
            } else {
                m_Amps.dbl = c_AmpsCal.dbl;
                m_Watts.dbl = c_WattsCal.dbl;
            }
            e_N.dbl = m_Error.dbl * c_Speed0Avg.dbl / 3.6;
            e_Watts.dbl = e_N.dbl * c_Speed0Avg.dbl / 3.6;

            computeAh();

            computeSpeedWhkm();

            computeRRWhkm();

            checkRR();

            computeWind();

            computeDistances();

            computeBestSpeed();

            processCap1();

            processCap2();

            processSlowCharge();

            if (c_Ah.cap == b_Ah.cap && c_Ah.rem > 0) {
                b_CapEst.dbl = b_Ah.cap * b_Ah.rem / c_Ah.rem;
            } else {
                b_CapEst.dbl = -1;
            }

            if (c_Gear.in() == 68 || c_Gear.in() == 131 || c_Gear.in() == 50) {
                p_Speed.dbl = c_Speed0.dbl;
            } else {
                p_Speed.dbl = 0;
            }
            p_Time = time;

        }
    }

    private void updateLowAmpTimer() {
        if (c_AmpsCal.dbl > -1.0 && c_AmpsCal.dbl < 1.0) {
            if (d_Second < 10) m_OCtimer.dbl += d_Second / 60.0;
        } else {
            m_OCtimer.dbl = 0;
        }
    }

    private void computeAuxW() {
        double auxW = 160.0;
        if (c_BrakeOn.in() == 2) auxW += 100.0;
        if (l_Drive.in() == 1) auxW += 160.0;
        if (l_High.in() == 1) auxW += 170.0;
        if (l_Park.in() == 1 && l_FogFront.in() == 1) auxW += 100.0;
        if (l_FogRear.in() == 1) auxW += 50.0;
        if (w_DeRear.in() == 1) auxW += 180.0;
        if (w_WiperF.in() == 1) auxW += 80.0;
        if (a_Max.in() == 1) {
            auxW += 150.0;
        } else {
            auxW += a_Fan.dbl * 14.0;
        }
        auxW += h_Amps.dbl * b_Volts.dbl;
        if (errorAC) {
            ac_Watts.dbl = c_WattsCal.dbl - computeModel(c_Speed0.dbl, m_AccW.dbl, auxW);
            if (ac_Watts.dbl < 0) ac_Watts.dbl = 0;
            if (b_Volts.dbl > 0) ac_Amps.dbl = ac_Watts.dbl / b_Volts.dbl;
        }
        auxW += ac_Amps.dbl * b_Volts.dbl;
        m_AuxW.dbl = auxW;
    }

    private void computeAccW() {
        double m_v = c_Speed0.dbl / 3.6; //Convert to m/s.
        double p_v = p_Speed.dbl / 3.6; //Convert to m/s.
        if (d_Second > 0) m_AccW.dbl = (m_v + p_v) / 2.0 * c_Mass.dbl * (m_v - p_v) / d_Second;
        if (m_AccW.dbl > 100000) m_AccW.dbl = 100000;
    }

    private void computeRegW() {
        if (c_RegA.dbl < 0) {
            c_RegW.dbl = c_RegA.dbl * b_Volts.dbl;
        } else if (m_AccW.dbl < 0) {
            c_RegW.dbl = 0.6 * m_AccW.dbl;
        } else {
            c_RegW.dbl = 0;
        }
    }

    private double computeModel(double speed, double accW, double auxW) {
        c_Drag.dbl = 0.75 * (1.2978 - 0.0046 * c_AirSensor.dbl) / 2.0;
        double m_v = speed / 3.6; //Convert to m/s.
        return auxW + c_Roll.dbl * m_v + m_Error.dbl * m_v * m_v + c_Drag.dbl * m_v * m_v * m_v + accW;
    }

    private void computeAh() {
        if (d_Second < 180) {
            d_AhCal.dbl = (c_AmpsCal.dbl + p_Amps.dbl) * d_Hour / 2.0;
            m_Ah.rem -= ((m_Amps.dbl + mp_Amps.dbl) * d_Hour / 2.0);
        } else {
            d_AhCal.dbl = (c_AmpsCal.dbl + p_Amps.dbl) * 180.0 / 3600.0 / 2.0;
            m_Ah.rem -= ((m_Amps.dbl + mp_Amps.dbl) * 180.0 / 3600.0 / 2.0);
        }
        p_Amps.dbl = c_AmpsCal.dbl;
        mp_Amps.dbl = m_Amps.dbl;

        c_Ah.rem -= d_AhCal.dbl;
        b_Ah.rem -= d_AhCal.dbl;
        bmu_Ah.rem -= d_AhCal.dbl;

        if (m_OCtimer.dbl > 10.0) {
            if (m_SoCavg.dbl > -1 && m_SoCavg.dbl < 112) {
                nmc_Ah.rem = m_SoCavg.dbl * nmc_Ah.cap / 100.0;
            }
        } else {
            nmc_Ah.rem -= d_AhCal.dbl;
        }
    }

    private void computeSpeedWhkm() {
        if ((c_Gear.in() == 68 || c_Gear.in() == 131 || c_Gear.in() == 50) && d_Second < 10) {
            double aAdd = 0.002 * d_Second;
            double aKeep = 1 - aAdd;
            c_SpdShnAvg.dbl = aKeep * c_SpdShnAvg.dbl + aAdd * c_SpdShown.dbl;
            if (c_SpdShnAvg.dbl < 1) c_SpdShnAvg.dbl = 1;
            c_Speed0Avg.dbl = aKeep * c_Speed0Avg.dbl + aAdd * c_Speed0.dbl;
            if (c_Speed0Avg.dbl < 1) c_Speed0Avg.dbl = 1;
            b_Wavg.dbl = aKeep * b_Wavg.dbl + aAdd * c_WattsCal.dbl; //compute the average measured watts while in drive.
            m_Wavg.dbl = aKeep * m_Wavg.dbl + aAdd * m_Watts.dbl; //compute the average model watts while in drive.
            b_Whkm.dbl = b_Wavg.dbl / c_Speed0Avg.dbl;
            m_Whkm.dbl = m_Wavg.dbl / c_Speed0Avg.dbl;
        }
    }

    private void computeRRWhkm() {
        if ((c_Gear.in() == 68 || c_Gear.in() == 131 || c_Gear.in() == 50) && d_Second < 10) {
            double aAdd = 0.0002 * d_Second;
            double aKeep = 1 - aAdd;
            c_SpdAvgRR.dbl = aKeep * c_SpdAvgRR.dbl + aAdd * c_Speed0.dbl;
            if (c_SpdAvgRR.dbl < 1) c_SpdAvgRR.dbl = 1;
            b_WavgRR.dbl = aKeep * b_WavgRR.dbl + aAdd * (c_WattsCal.dbl - m_AuxW.dbl); //compute the average measured watts while in drive.
            m_WavgRR.dbl = aKeep * m_WavgRR.dbl + aAdd * (m_Watts.dbl - m_AuxW.dbl); //compute the average model watts while in drive.
            b_Ah.Whkm = (b_WavgRR.dbl + m_AuxW.dbl) / c_SpdAvgRR.dbl;
            m_Ah.Whkm = (m_WavgRR.dbl + m_AuxW.dbl) / c_SpdAvgRR.dbl;
            bmu_Ah.Whkm = b_Ah.Whkm;
            nmc_Ah.Whkm = b_Ah.Whkm;
        }
    }

    private void checkRR() {
        if (i_RangeUnits.equals("km") && m_AuxW.dbl < 500) {
            double test;
            if (b_Ah.Whkm > 0) c_RRtest.dbl = c_Ah.remWh10() / b_Ah.Whkm;
            if (c_Ah.RR() > 0 && c_RRtest.dbl > 0) test = c_RRtest.dbl / c_Ah.RR();
            else test = 1.0;
            checkRRMiles = test > 0.58 && test < 0.68;
        } else {
            checkRRMiles = false;
        }
    }


    private void computeWind() {
        double v = c_Speed0Avg.dbl / 3.6; //Convert to m/s.
        if (c_Drag.dbl > 0)
            m_Wind.dbl = (Math.cbrt(-(c_Drag.dbl * c_Drag.dbl) * (c_Roll.dbl * v + m_AuxW.dbl - m_Wavg.dbl)) - c_Drag.dbl * v) / c_Drag.dbl;
    }


    private void computeDistances() {
        if (d_Second < 180) { // If there is less than 3 minutes since the last step then assume no data has been lost.
            double dx = (c_Speed0.dbl + p_Speed.dbl) * d_Hour / 2.0;
            m_Odo.dbl += dx;//The odometer increased by the true distance traveled during d_hours
            t_km.dbl -= dx; //The distance to the next charging station is reduced by the true distance traveled during d_hours
            m_km.dbl += dx; //True distance since starting
            if (i_OdoUnits.equals("km")) {
                c_kmTest.dbl += c_Odo.dbl - p_Odo.dbl;
                p_Odo.dbl = c_Odo.dbl;
                m_kmTest.dbl += dx;
                double test = 1;
                if (m_kmTest.dbl > 0) test = c_kmTest.dbl / m_kmTest.dbl;
                checkOdoMiles = test > 0.58 && test < 0.68;
            }
        } else {
            m_km.dbl += c_Odo.dbl - m_Odo.dbl;
            t_km.dbl -= c_Odo.dbl - m_Odo.dbl;
            m_Odo.dbl = c_Odo.dbl;
        }
        if (t_km.dbl < 0) t_km.dbl = 0;
    }

    private void computeBestSpeed() {
        if (t_km.dbl > 0) {
            t_reqkm.dbl = t_km.dbl + i_Margin.dbl;
            t_Margin.dbl = c_Ah.RR() - t_km.dbl; //RR minus the true distance to the next charging station
        } else if (c_Ah.RR() > 0) {
            t_reqkm.dbl = c_Ah.RR();
            t_Margin.dbl = c_Ah.RR();
        } else {
            t_reqkm.dbl = 0;
            t_Margin.dbl = 0;
        }

        t_Ah.cap = c_Ah.cap;
        t_Ah.rem = c_Ah.rem;

        t_W.dbl = computeModel(t_Speed.dbl, 0, m_AuxW.dbl);
        if (t_Speed.dbl > 0) t_Ah.Whkm = t_W.dbl / t_Speed.dbl;

        double v = t_Speed.dbl / 3.6;
        double a = c_Drag.dbl * v * v * v + m_Error.dbl * v * v + c_Roll.dbl * v - m_AuxW.dbl;
        t_Slope.dbl = -t_Ah.remWh10() * (2 * c_Drag.dbl * v * v * v + m_Error.dbl * v * v - m_AuxW.dbl) / (a * a);


        if (t_Ah.RR() > 0 && t_reqkm.dbl > 0) {
            if (t_Ah.RR() > t_reqkm.dbl || t_Slope.dbl > 0) {
                t_Speed.dbl += 0.02 * d_Second * Math.abs(t_Ah.RR() - t_reqkm.dbl);
            } else {
                t_Speed.dbl -= 0.02 * d_Second * Math.abs(t_Ah.RR() - t_reqkm.dbl);
            }
        } else {
            if (t_Slope.dbl > 0) {
                t_Speed.dbl += 0.02 * d_Second;
            } else {
                t_Speed.dbl -= 0.02 * d_Second;
            }
        }

        if (t_Speed.dbl < 10) t_Speed.dbl = 10;
        else if (t_Speed.dbl > 130) t_Speed.dbl = 130;

        t_W.dbl = computeModel(t_Speed.dbl, 0, m_AuxW.dbl);
        t_Ah.Whkm = t_W.dbl / t_Speed.dbl;
    }


    private void processCap1() {
        switch (stepCap1) {
            case 0:
                if (b_Ah.used() > 17.5) stepCap1 = 1;
                break;
            case 1:
                iniCap1();
                if (b_Ah.used() > 20) stepCap1 = 2;
                break;
            case 2:
                if (m_OCtimer.dbl > 10) computeCap1();
                if (m_OCtimer.dbl > 15) stepCap1 = 3;
                break;
            case 3:
                computeCells1();
                if (Math.abs(c_AmpsCal.dbl) > 2) stepCap1 = 0;
                break;
            case 4:
                break;
            default:
                stepCap1 = 0;
                break;
        }
    }

    private void iniCap1() {
        b_Cellmax.Ah1 = 0;
        b_Cellavg.Ah1 = 0;
        b_Cellmin.Ah1 = 0;
        for (Cell aCell : listCells) {
            aCell.Ah1 = 0;
        }
    }

    private void computeCap1() {
        if (100 - b_Cellmax.SoC > 0)
            b_Cellmax.Ah1 = 100 * b_Ah.used() / (100 - b_Cellmax.SoC);
        if (100 - b_Cellavg.SoC > 0)
            b_Cellavg.Ah1 = 100 * b_Ah.used() / (100 - b_Cellavg.SoC);
        if (100 - b_Cellmin.SoC > 0)
            b_Cellmin.Ah1 = 100 * b_Ah.used() / (100 - b_Cellmin.SoC);
        m_CapTemp.dbl = 0.99 * m_CapTemp.dbl + 0.01 * b_Temp.dbl;
    }

    private void computeCells1() {
        if (cellsData)
            for (Cell aCell : listCells) {
                if (100 - aCell.SoC > 0) {
                    aCell.Ah1 = 100 * b_Ah.used() / (100 - aCell.SoC);
                }
            }
    }

    private void processCap2() {
        switch (stepCap2) {
            case 0:
                iniCap2();
                if (b_SoC1.dbl < 20 && b_SoC2.dbl < 20) stepCap2 = 1;
                break;
            case 1:
                iniCap2();
                if (b_SoC1.dbl < 15 && b_SoC2.dbl < 15) stepCap2 = 2;
                break;
            case 2:
                iniCap2();
                if (m_OCtimer.dbl > 30) {
                    stepCap2 = 3;
                }
                break;
            case 3:
                updateSums2();
                if (c_AmpsCal.dbl < -1) stepCap2 = 4;
                break;
            case 4:
                updateSums2();
                if ((b_SoC1.dbl > 98 || b_SoC2.dbl > 98) && c_AmpsCal.dbl > -0.1) stepCap2 = 5;
                break;
            case 5:
                updateSums2();
                if (m_OCtimer.dbl > 0) stepCap2 = 6;
                break;
            case 6:
                updateSums2();
                if (m_OCtimer.dbl > 25) stepCap2 = 7;
                break;
            case 7:
                updateSums2();
                if (m_OCtimer.dbl > 10)computeCapacities2();
                if (m_OCtimer.dbl > 30) stepCap2 = 8;
                break;
            case 8:
                computeCells2();
                if (Math.abs(c_AmpsCal.dbl) > 2) stepCap2 = 0;
                break;
            case 9:
                break;
            default:
                stepCap2 = 0;
                break;
        }
    }

    private void iniCap2() {
        m_CAh2max = b_Cellmax;
        m_CAh2avg = b_Cellavg;
        m_CAh2min = b_Cellmin;
        m_CAh2max.p_SoC = m_CAh2max.SoC;
        m_CAh2avg.p_SoC = m_CAh2avg.SoC;
        m_CAh2min.p_SoC = m_CAh2min.SoC;
        m_CAh2max.SoCsum = 0;
        m_CAh2avg.SoCsum = 0;
        m_CAh2min.SoCsum = 0;
        m_CAh2max.Ah2 = 0;
        m_CAh2avg.Ah2 = 0;
        m_CAh2min.Ah2 = 0;
        m_Ah.sum = 0;
        if (cellsData) {
            for (Cell aCell : listCells) {
                aCell.SoCsum = 0;
                aCell.p_SoC = aCell.SoC;
                aCell.Ah2 = 0;
            }
        }
    }

    private void updateSums2() {
        if (cellsData) {
            for (Cell aCell : listCells) {
                aCell.SoCsum += aCell.SoC - aCell.p_SoC;
                aCell.p_SoC = aCell.SoC;
                if (aCell.module == m_CAh2max.module && aCell.cell == m_CAh2max.cell)
                    m_CAh2max = aCell;
                if (aCell.module == m_CAh2min.module && aCell.cell == m_CAh2min.cell)
                    m_CAh2min = aCell;
            }
        } else {
            m_CAh2max.SoC = b_Cellmax.SoC;
            m_CAh2min.SoC = b_Cellmin.SoC;
            m_CAh2max.SoCsum += m_CAh2max.SoC - m_CAh2max.p_SoC;
            m_CAh2min.SoCsum += m_CAh2min.SoC - m_CAh2min.p_SoC;
            m_CAh2max.p_SoC = m_CAh2max.SoC;
            m_CAh2min.p_SoC = m_CAh2min.SoC;
        }
        m_CAh2avg.SoC = b_Cellavg.SoC;
        m_CAh2avg.SoCsum += m_CAh2avg.SoC - m_CAh2avg.p_SoC;
        m_CAh2avg.p_SoC = m_CAh2avg.SoC;
        m_Ah.sum -= d_AhCal.dbl;
        m_CapTemp.dbl = 0.99 * m_CapTemp.dbl + 0.01 * m_CAh2min.temperature;
    }

    private void computeCapacities2() {
        if (m_CAh2max.SoCsum > 0 && m_Ah.sum > 0)
            m_CAh2max.Ah2 = 100 * m_Ah.sum / (m_CAh2max.SoCsum);
        if (m_CAh2avg.SoCsum > 0 && m_Ah.sum > 0)
            m_CAh2avg.Ah2 = 100 * m_Ah.sum / (m_CAh2avg.SoCsum);
        if (m_CAh2min.SoCsum > 0 && m_Ah.sum > 0)
            m_CAh2min.Ah2 = 100 * m_Ah.sum / (m_CAh2min.SoCsum);
    }

    private void computeCells2() {
        if (cellsData) {
            double maxAh2 = 0;
            double minAh2 = 200;
            double sum = 0;
            int n = 0;
            for (Cell aCell : listCells) {
                if (aCell.isFound)
                    if (aCell.SoCsum > 0 && m_Ah.sum > 0) {
                        aCell.Ah2 = 100.0 * m_Ah.sum / aCell.SoCsum;
                        if (aCell.Ah2 > maxAh2) {
                            maxAh2 = aCell.Ah2;
                            m_CAh2max.module = aCell.module;
                            m_CAh2max.cell = aCell.cell;
                            m_CAh2max.volts = aCell.volts;
                            m_CAh2max.Ah2 = aCell.Ah2;
                            m_CAh2max.SoCsum = aCell.SoCsum;
                        }
                        if (aCell.Ah2 < minAh2) {
                            minAh2 = aCell.Ah2;
                            m_CAh2min.module = aCell.module;
                            m_CAh2min.cell = aCell.cell;
                            m_CAh2min.volts = aCell.volts;
                            m_CAh2min.Ah2 = aCell.Ah2;
                            m_CAh2min.SoCsum = aCell.SoCsum;
                        }
                        sum += aCell.Ah2;
                        n += 1;
                    }
            }
            if (n > 0) {
                m_CAh2avg.Ah2 = sum / n;
                if (m_CAh2avg.Ah2 > 0)
                    m_CAh2avg.SoCsum = 100.0 * m_Ah.sum / m_CAh2avg.Ah2;
            }
        }
    }

    public void processSlowCharge() {

        if (!chargeFinished) {
            if (c_ChargeVAC.dbl > 100 && c_AmpsCal.dbl < -0.5 && !isCharging) {
                isCharging = true;
                bmu_CapAh0.dbl = bmu_Ah.cap;
                bmu_RemAh0.dbl = bmu_Ah.rem;
                b_CapAh0.dbl = b_Ah.cap;
                b_RemAh0.dbl = b_Ah.rem;
                b_Volts0.dbl = b_Volts.dbl;
                b_SoC10.dbl = b_SoC1.dbl;
                p1_Time.dbl = 0;
                p2_Time.dbl = 0;
            }

            if (isCharging && bmu_RemAh0.dbl == 0) {
                bmu_CapAh0.dbl = bmu_Ah.cap;
                bmu_RemAh0.dbl = bmu_Ah.rem;
            }

            if (isCharging && p1_Time.dbl == 0) p1_Volts.dbl = b_Volts.dbl;

            if (isCharging && c_AmpsCal.dbl > -0.1 && !pause1) {
                p1_Time.dbl = p1_Time.dbl + d_Hour * 60;
                p1_Ah.dbl = b_Ah.rem;
                p1_SoC.dbl = b_SoC1.dbl;
            }

            if (isCharging && c_AmpsCal.dbl < -0.5 && p1_Time.dbl > 0) pause1 = true;

            if (isCharging && pause1 && p2_Time.dbl == 0) {
                p2_Volts.dbl = b_Volts.dbl;
            }

            if (isCharging && pause1 && c_AmpsCal.dbl > -0.1) {
                p2_Time.dbl = p2_Time.dbl + d_Hour * 60;
                p2_Ah.dbl = b_Ah.rem;
                p2_SoC.dbl = b_SoC1.dbl;
            }

            if (isCharging && pause1 && p2_Time.dbl > 0 && m_newPIDs == 0) {
                isCharging = false;
                bmu_CapAh1.dbl = bmu_Ah.cap;
                bmu_RemAh1.dbl = bmu_Ah.rem;
                b_CapAh1.dbl = b_Ah.cap;
                b_RemAh1.dbl = b_Ah.rem;
                b_Volts1.dbl = b_Volts.dbl;
                b_SoC11.dbl = b_SoC1.dbl;
                p12_CapAh.dbl = 100 * (p2_Ah.dbl - p1_Ah.dbl) / (p2_SoC.dbl - p1_SoC.dbl);
                chargeFinished = true;
            }
        }
    }

    @SuppressLint("SetTextI18n")
    public void selectOne() {
        switch (menuTabs) {
            case 0:
                if (fragNo == FRAG_INFO) {
                    updateFrag(FRAG_INFO);
                } else {
                    clrLines();
                    lineOne.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
                    textCell.setText("LEV and NMC");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentInfo.newInstance())
                            .commitNow();
                    fragNo = FRAG_INFO;
                }
                break;
            case 1:
                if (fragNo == FRAG_OPS) {
                    updateFrag(FRAG_OPS);
                } else {
                    clrLines();
                    lineOne.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                    textCell.setText("Type: " + i_Chem);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentOps.newInstance())
                            .commitNow();
                    fragNo = FRAG_OPS;
                }
                break;
            case 2:
                if (fragNo == FRAG_PID) {
                    updateFrag(FRAG_PID);
                } else {
                    clrLines();
                    lineOne.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
                    textCell.setText("LEV and NMC");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentPIDs.newInstance())
                            .commitNow();
                    fragNo = FRAG_PID;
                }
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    public void selectTwo() {
        switch (menuTabs) {
            case 0:
                if (fragNo == FRAG_WH) {
                    updateFrag(FRAG_WH);
                } else {
                    clrLines();
                    lineTwo.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                    textCell.setText("Type: " + i_Chem);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentWh.newInstance())
                            .commitNow();
                    fragNo = FRAG_WH;
                }
                break;
            case 1:
                if (fragNo == FRAG_OBD) {
                    updateFrag(FRAG_OBD);
                } else {
                    prepOBD();
                    clrLines();
                    lineTwo.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
                    textCell.setText("warning capacity, range SoC etc. LEV only");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentOBD.newInstance())
                            .commitNow();
                    fragNo = FRAG_OBD;
                }
                break;
            case 2:
                if (fragNo == FRAG_CALC) {
                    updateFrag(FRAG_CALC);
                } else {
                    prepCalc();
                    clrLines();
                    lineTwo.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
                    textCell.setText("warning LEV only");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentCalc.newInstance())
                            .commitNow();
                    fragNo = FRAG_CALC;
                }
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    public void selectThree() {
        switch (menuTabs) {
            case 0:
                if (fragNo == FRAG_AH) {
                    updateFrag(FRAG_AH);
                } else {
                    clrLines();
                    lineThree.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                    textCell.setText("Type: " + i_Chem);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentAh.newInstance())
                            .commitNow();
                    fragNo = FRAG_AH;
                }
                break;
            case 1:
                if (fragNo == FRAG_CELLS) {
                    updateFrag(FRAG_CELLS);
                } else {
                    clrLines();
                    lineThree.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
                    textCell.setText("LEV and NMC");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentCells.newInstance())
                            .commitNow();
                    fragNo = FRAG_CELLS;
                }
                break;
            case 2:
                if (fragNo == FRAG_CHARGE) {
                    updateFrag(FRAG_CHARGE);
                } else {
                    prepCalc();
                    clrLines();
                    lineThree.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
                    textCell.setText("warning LEV only");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentCharge.newInstance())
                            .commitNow();
                    fragNo = FRAG_CHARGE;
                }
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    public void selectFour() {
        switch (menuTabs) {
            case 0:
                if (fragNo == FRAG_WATTS) {
                    updateFrag(FRAG_WATTS);
                } else {
                    clrLines();
                    lineFour.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                    textCell.setText("LEV and NMC");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentWatts.newInstance())
                            .commitNow();
                    fragNo = FRAG_WATTS;
                }
                break;
            case 1:
                if (fragNo == FRAG_VOLTS) {
                    updateFrag(FRAG_VOLTS);
                } else {
                    clrLines();
                    lineFour.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                    textCell.setText("LEV and NMC");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentVolts.newInstance())
                            .commitNow();
                    fragNo = FRAG_VOLTS;
                }
                break;
            case 2:
                if (fragNo == FRAG_CAP1) {
                    updateFrag(FRAG_CAP1);
                } else {
                    prepCap1();
                    clrLines();
                    lineFour.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
                    textCell.setText("Type: " + i_Chem);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentCap1.newInstance())
                            .commitNow();
                    fragNo = FRAG_CAP1;
                }
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    public void selectFive() {
        switch (menuTabs) {
            case 0:
                if (fragNo == FRAG_DRV) {
                    updateFrag(FRAG_DRV);
                } else {
                    clrLines();
                    lineFive.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                    textCell.setText("warning LEV only");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentDrive.newInstance())
                            .commitNow();
                    fragNo = FRAG_DRV;
                }
                break;
            case 1:
                if (fragNo == FRAG_TEMP) {
                    updateFrag(FRAG_TEMP);
                } else {
                    clrLines();
                    lineFive.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                    textCell.setText("LEV and NMC");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentTemp.newInstance())
                            .commitNow();
                    fragNo = FRAG_TEMP;
                }
                break;
            case 2:
                if (fragNo == FRAG_CAP2) {
                    updateFrag(FRAG_CAP2);
                } else {
                    prepCap2();
                    clrLines();
                    lineFive.setBackgroundColor(Color.WHITE);
                    textCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
                    textCell.setText("warning LEV only");
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_place, FragmentCap2.newInstance())
                            .commitNow();
                    fragNo = FRAG_CAP2;
                }
                break;
        }
    }

    private void clrLines() {
        lineOne.setBackgroundColor(BLACK);
        lineTwo.setBackgroundColor(BLACK);
        lineThree.setBackgroundColor(BLACK);
        lineFour.setBackgroundColor(BLACK);
        lineFive.setBackgroundColor(BLACK);
    }

    private void updateFrag(int f) {
        if (f == fragNo) {
            switch (f) {
                case (FRAG_INFO):
                    FragmentInfo.Refresh(listInfo);
                    listInfo.clear();
                    break;
                case (FRAG_OBD):
                    prepOBD();
                    FragmentOBD.Refresh(arrayOBD);
                    break;
                case (FRAG_CELLS):
                    FragmentCells.Refresh(listCells, cellsData);
                    break;
                case (FRAG_PID):
                    FragmentPIDs.Refresh(listPIDs);
                    break;
                case (FRAG_CALC):
                    prepCalc();
                    FragmentCalc.Refresh(arrayOBD);
                    break;
                case (FRAG_CHARGE):
                    FragmentCharge.Refresh();
                    break;
                case (FRAG_CAP1):
                    prepCap1();
                    FragmentCap1.Refresh(arrayOBD, stepCap1);
                    break;
                case (FRAG_CAP2):
                    prepCap2();
                    FragmentCap2.Refresh(arrayOBD, stepCap2);
                    break;
                case (FRAG_WATTS):
                    FragmentWatts.Refresh();
                    break;
                case (FRAG_DRV):
                    FragmentDrive.Refresh();
                    break;
                case (FRAG_AH):
                    FragmentAh.Refresh();
                    break;
                case (FRAG_WH):
                    FragmentWh.Refresh();
                    break;
                case (FRAG_VOLTS):
                    FragmentVolts.Refresh();
                    break;
                case (FRAG_TEMP):
                    FragmentTemp.Refresh();
                    break;
                case (FRAG_OPS):
                    FragmentOps.Refresh();
                    break;
                default:
                    break;
            }
        }
    }

    private void prepOBD() {
        arrayOBD.clear();
        arrayOBD.add("Time          " + displayDate.format(stepDateTime) + " " + displayTime.format(stepDateTime));
        arrayOBD.add("Odometer      " + c_Odo.unit() + " " + decFix0.format(c_Odo.dbl / KmPerMile) + " miles");
        arrayOBD.add("Speed         " + c_Speed0.unit() + " " + decFix2.format(c_Speed0.dbl / KmPerMile) + " mph");
        arrayOBD.add("Acc. Pedal    " + c_Pedal.unit());
        arrayOBD.add("Acceleration  " + c_Acc.unit());
        arrayOBD.add("Air sensor    " + c_AirSensor.unit());
        arrayOBD.add("Key           " + c_KeyOn.strOnOff());
        arrayOBD.add("Brake         " + c_BrakeOn.strOnOff() + " pressure " + c_Brake.str());
        arrayOBD.add("eStability    ");
        arrayOBD.add("  Steering    " + c_Steering.unit());
        arrayOBD.add("  Rotation    " + c_Rotation.unit());
        arrayOBD.add("  Wheel       ");
        arrayOBD.add("    speed 1   " + c_Speed1.unit());
        arrayOBD.add("    speed 2   " + c_Speed2.unit());
        arrayOBD.add("    speed 3   " + c_Speed3.unit());
        arrayOBD.add("    speed 4   " + c_Speed4.unit());
        arrayOBD.add("    average   " + c_Speed0.unit());

        switch (c_Gear.in()) {
            case 80:
                arrayOBD.add("Gear shift    P");
                break;
            case 82:
                arrayOBD.add("Gear shift    R");
                break;
            case 78:
                arrayOBD.add("Gear shift    N");
                break;
            case 68:
                arrayOBD.add("Gear shift    D");
                break;
            case 131:
                arrayOBD.add("Gear shift    B");
                break;
            case 50:
                arrayOBD.add("Gear shift    C");
                break;
            default:
                arrayOBD.add("Gear shift    na");
                break;
        }

        arrayOBD.add("Motor         " + c_MotorA.unit() + " " + decFix0.format(c_MotorA.dbl * b_Volts.dbl) + " W " + c_RPM.unit());
        arrayOBD.add("Motor temps.  " + c_MotorTemp0.unit() + " " + c_MotorTemp1.unit() + " " + c_MotorTemp2.unit() + " " + c_MotorTemp3.unit());
        arrayOBD.add("Regeneration  " + c_RegA.unit() + " " + c_RegW.unit());
        arrayOBD.add("Battery");
        arrayOBD.add("  Voltage     " + b_Volts.unit());
        arrayOBD.add("  Current out " + c_AmpsCal.unit() + " calibrated");
        arrayOBD.add("  Watts   out " + c_WattsCal.unit() + " calibrated");
        arrayOBD.add("  SoC         (1) " + b_SoC1.unit() + " (2) " + b_SoC2.unit());
        arrayOBD.add("  Capacity    " + c_Ah.capUnit() + " @ 100% SoC");
        arrayOBD.add("  SoH         " + decFix0.format(100 * c_Ah.cap / 50.0) + " % of 50Ah");
        arrayOBD.add("Battery Management Unit");
        arrayOBD.add("  Capacity    " + bmu_Ah.capUnit() + " @ 100% SoC");
        arrayOBD.add("  SoH         " + decFix0.format(100 * bmu_Ah.cap / 48.0) + " % of 48Ah");
        arrayOBD.add("  Ah          " + bmu_Ah.remUnit());
        arrayOBD.add("  SoC         " + bmu_Ah.SoCUnit());

        arrayOBD.add("Cells");
        if (cellsData) {
            arrayOBD.add("  Voltage     max " + b_Cellmax.strVolts(3) + " min " + b_Cellmin.strVolts(3));
        } else {
            arrayOBD.add("  Voltage     max " + b_BatVmax.unit() + " min " + b_BatVmin.unit());
        }
        arrayOBD.add("  Temperature max " + b_BatTmax.unit() + " min " + b_BatTmin.unit());

        arrayOBD.add("Rest Range    " + c_Ah.RRUnit() + " " + decFix0.format(c_Ah.RR() / KmPerMile) + " miles");
        arrayOBD.add("Heat/Cool     " + h_Level.str());
        arrayOBD.add("Heater        " + h_Amps.unit() + " " + h_Watts.unit());

        arrayOBD.add("AC            " + ac_On.strOnOff() + " " + ac_Amps.unit() + " " + ac_Watts.unit());

        arrayOBD.add("Recirculation " + a_Reci.strOnOff());

        if (a_Max.in() == 1) {
            arrayOBD.add("Fan           speed max" + " direction " + a_Dirc.str());
        } else {
            arrayOBD.add("Fan           speed " + a_Fan.str() + " direction " + a_Dirc.str());
        }

        arrayOBD.add("Charging");
        arrayOBD.add("  Battery DC  " + c_ChargeVDC.unit() + " " + c_ChargeADC.unit() + " " + decFix0.format(c_ChargeVDC.dbl * c_ChargeADC.dbl) + " W");
        arrayOBD.add("  Mains   AC  " + c_ChargeVAC.unit() + " " + c_ChargeAAC.unit() + " " + decFix0.format(c_ChargeVAC.dbl * c_ChargeAAC.dbl) + " W");
        arrayOBD.add("  Temperature " + c_ChargeTemp1.unit() + " " + c_ChargeTemp2.unit());

        if (c_QuickCharge.in() == 1)
            arrayOBD.add("Chademo       " + c_QCAmps.unit() + " " + decFix0.format(c_QCAmps.dbl * b_Volts.dbl) + " W " + c_QCprocent.unit());
        else arrayOBD.add("Chademo       off");

        arrayOBD.add("Lights");
        arrayOBD.add("  Park        " + l_Park.strOnOff());

        if (l_FogFront.in() == 1) {
            if (l_FogRear.in() == 1) {
                arrayOBD.add("  Fog        " + "front on rear on");
            } else {
                arrayOBD.add("  Fog        " + "front on rear off");
            }
        } else {
            arrayOBD.add("  Fog        " + " front off rear off");
        }

        if (l_High.in() == 1) {
            arrayOBD.add("  Drive       " + l_Drive.strOnOff() + " high-beams on");
        } else {
            arrayOBD.add("  Drive       " + l_Drive.strOnOff() + " high-beams off");
        }

        arrayOBD.add("Rear defrost  " + w_DeRear.strOnOff());
        arrayOBD.add("Wipers        " + w_WiperF.strOnOff());
        arrayOBD.add("Charge 12vBat " + c_12vAmps.unit() + " " + c_12vWatts.unit());
        arrayOBD.add("Model Year    " + c_Model.str());
        arrayOBD.add("VIN           " + strVIN[0] + strVIN[1] + strVIN[2]);
    }

    private void prepCalc() {
        arrayOBD.clear();
        arrayOBD.add(displayDate.format(stepDateTime));
        arrayOBD.add(displayTime.format(stepDateTime));
        arrayOBD.add(decFix2.format(d_Second) + " sec");

        arrayOBD.add(c_Odo.str());
        arrayOBD.add(m_Odo.str());
        arrayOBD.add(c_WattsCal.str());
        arrayOBD.add(m_Watts.str());
        arrayOBD.add(t_W.str());
        arrayOBD.add(c_Ah.remWhStr());
        arrayOBD.add(b_Ah.remWhStr());
        arrayOBD.add(m_Ah.remWhStr());
        arrayOBD.add(t_Ah.remWhStr());
        arrayOBD.add(c_Ah.WhkmStr());
        arrayOBD.add(b_Ah.WhkmStr());
        arrayOBD.add(m_Ah.WhkmStr());
        arrayOBD.add(t_Ah.WhkmStr());
        arrayOBD.add(c_Ah.RRStr());
        arrayOBD.add(b_Ah.RRStr());
        arrayOBD.add(m_Ah.RRStr());
        arrayOBD.add(t_Ah.RRStr());

        arrayOBD.add(m_AuxW.str());
        arrayOBD.add(e_N.str());
        arrayOBD.add(e_Watts.str());
        arrayOBD.add(m_Wind.str());

        arrayOBD.add(m_km.str());
        arrayOBD.add(t_km.str());
        arrayOBD.add(i_Margin.str());
        arrayOBD.add(t_Ah.RRStr());

        arrayOBD.add(c_SpdShown.str());
        arrayOBD.add(c_Speed0.str());
        arrayOBD.add(c_Speed0Avg.str());
        arrayOBD.add(t_Speed.str());

        arrayOBD.add(c_Ah.capStr());
        arrayOBD.add(c_Ah.remStr());
        arrayOBD.add(b_Ah.capStr());
        arrayOBD.add(b_Ah.remStr());
        arrayOBD.add(b_CapEst.str());
    }

    private void prepCap1() {
        arrayOBD.clear();
        arrayOBD.add(displayDate.format(stepDateTime)); //0
        arrayOBD.add(displayTime.format(stepDateTime)); //1
        arrayOBD.add(decFix2.format(d_Second) + " sec");//2
        arrayOBD.add(i_Chem);//3
        arrayOBD.add(b_Ah.usedStr());//4
        arrayOBD.add("");//5
        arrayOBD.add("");//6
        arrayOBD.add("");//7
        arrayOBD.add("");//8
        arrayOBD.add(c_AmpsCal.str());//9
        arrayOBD.add(m_OCtimer.str());//10
        if (cellsData) {
            arrayOBD.add(b_Cellmax.strModule());//11
            arrayOBD.add(b_Cellmax.strCellLetter());//12
            arrayOBD.add(b_Cellmax.strVolts(3));//13
            arrayOBD.add(b_Cellmax.strSoC());//14
            arrayOBD.add(b_Cellmax.strAh1());//15
            arrayOBD.add(b_Cellavg.strVolts(3));//16
            arrayOBD.add(b_Cellavg.strSoC());//17
            arrayOBD.add(b_Cellavg.strAh1());//18
            arrayOBD.add(b_Cellmin.strModule());//19
            arrayOBD.add(b_Cellmin.strCellLetter());//20
            arrayOBD.add(b_Cellmin.strVolts(3));//21
            arrayOBD.add(b_Cellmin.strSoC());//22
            arrayOBD.add(b_Cellmin.strAh1());//23
        } else {
            arrayOBD.add("");//11
            arrayOBD.add("");//12
            arrayOBD.add(b_Cellmax.strVolts(3));//13
            arrayOBD.add(b_Cellmax.strSoC());//14
            arrayOBD.add(b_Cellmax.strAh1());//15
            arrayOBD.add(b_Cellavg.strVolts(3));//16
            arrayOBD.add(b_Cellavg.strSoC());//17
            arrayOBD.add(b_Cellavg.strAh1());//18
            arrayOBD.add("");//19
            arrayOBD.add("");//20
            arrayOBD.add(b_Cellmin.strVolts(3));//21
            arrayOBD.add(b_Cellmin.strSoC());//22
            arrayOBD.add(b_Cellmin.strAh1());//23
        }
    }

    private void prepCap2() {
        arrayOBD.clear();
        arrayOBD.add(displayDate.format(stepDateTime));
        arrayOBD.add(displayTime.format(stepDateTime));
        arrayOBD.add(decFix2.format(d_Second) + " sec");
        arrayOBD.add(b_SoC1.str());
        arrayOBD.add(b_SoC2.str());
        arrayOBD.add(b_Ah.capStr());
        arrayOBD.add(b_Temp.str());
        arrayOBD.add(m_Ah.sumStr());
        arrayOBD.add(c_AmpsCal.str());
        arrayOBD.add(m_OCtimer.str());
        if (cellsData) {
            arrayOBD.add(m_CAh2max.strModule());
            arrayOBD.add(m_CAh2max.strCellLetter());
            arrayOBD.add(m_CAh2max.strVolts(3));
            arrayOBD.add(m_CAh2max.strSoCsum());
            arrayOBD.add(m_CAh2max.strAh2());
            arrayOBD.add(m_CAh2avg.strVolts(3));
            arrayOBD.add(m_CAh2avg.strSoCsum());
            arrayOBD.add(m_CAh2avg.strAh2());
            arrayOBD.add(m_CAh2min.strModule());
            arrayOBD.add(m_CAh2min.strCellLetter());
            arrayOBD.add(m_CAh2min.strVolts(3));
            arrayOBD.add(m_CAh2min.strSoCsum());
            arrayOBD.add(m_CAh2min.strAh2());
        } else {
            arrayOBD.add("");
            arrayOBD.add("");
            arrayOBD.add(m_CAh2max.strVolts(3));
            arrayOBD.add(m_CAh2max.strSoCsum());
            arrayOBD.add(m_CAh2max.strAh2());
            arrayOBD.add(m_CAh2avg.strVolts(3));
            arrayOBD.add(m_CAh2avg.strSoCsum());
            arrayOBD.add(m_CAh2avg.strAh2());
            arrayOBD.add("");
            arrayOBD.add("");
            arrayOBD.add(m_CAh2min.strVolts(3));
            arrayOBD.add(m_CAh2min.strSoCsum());
            arrayOBD.add(m_CAh2min.strAh2());
        }
    }

    // The following methods get information back from the BluetoothService
    private final IStaticHandler handlerBT = new IStaticHandler() {
        @Override
        public void handleMessage(Message msg) {
            cycleTimer();
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.getData().getInt(STATE)) {
                        case BluetoothSerialService.STATE_CONNECTED:
                            btnOne.setBackgroundColor(clrDarkGreen);
                            if (itemMenuConnect != null)
                                itemMenuConnect.setTitle(menu_disconnect);
                            if (connectedDevice != null)
                                updateInfo("app:Connected to " + connectedDeviceName);
                            else updateInfo("app:Connected to an unknown device.");
                            updateFrag(FRAG_INFO);
                            if (runCollector) {
                                monitor = 0;
                                if (runRecording) StoreInfo();
                                if (serviceSerial != null) serviceSerial.startCollector();
                            } else if (runRestart) doRestart();
                            else finishOnStart();
                            break;

                        case BluetoothSerialService.STATE_CONNECTING:
                            if (connectedDevice != null)
                                updateInfo("app:Connecting to " + connectedDeviceName);
                            else updateInfo("app:Connecting to an unknown device.");
                            updateInfo("app:Please wait");
                            updateFrag(FRAG_INFO);
                            break;

                        case BluetoothSerialService.STATE_FAILED:
                            runRestart = false;
                            btnOne.setBackgroundColor(BLACK);
                            if (itemMenuConnect != null) itemMenuConnect.setTitle(menu_connect);
                            if (connectedDevice != null)
                                updateInfo("app:Connection to " + connectedDeviceName + " failed ");
                            updateFrag(FRAG_INFO);
                            break;

                        case BluetoothSerialService.STATE_LOST:
                            runRestart = false;
                            btnOne.setBackgroundColor(BLACK);
                            if (itemMenuConnect != null)
                                itemMenuConnect.setTitle(menu_connect);
                            if (connectedDevice != null) {
                                updateInfo("app:" + connectedDeviceName + " connection lost @ " + cycleTime + " ms.");
                            }
                            updateFrag(FRAG_INFO);
                            if (runRecording) StoreInfo();
                            break;

                        case BluetoothSerialService.STATE_NONE:
                            runRestart = false;
                            btnOne.setBackgroundColor(BLACK);
                            if (itemMenuConnect != null)
                                itemMenuConnect.setTitle(menu_connect);
                            if (connectedDevice != null)
                                updateInfo("app:" + connectedDeviceName + " state to none");
                            updateFrag(FRAG_INFO);
                            break;
                    }
                    break;

                case MESSAGE_RECEIVED:
                    String lineReceived = msg.getData().getString(RECEIVED_LINE);
                    if (lineReceived != null) {
                        updateInfo("OBD:" + lineReceived + " ");
                        if (lineReceived.contains("Exception")) {
                            updateFrag(FRAG_INFO);
                            if (runRecording) StoreInfo();
                        } else if (lineReceived.contains("RESET OK")) {
                            monitor = 0;
                            runReset = false;
                            isReset = true;
                            updateInfo("app:Reset took " + cycleTime + " ms.");
                            cycleTime = 0;
                            if (runRestart) doRestart();
                            else finishOnStart();
                        } else if (lineReceived.equals("RESET FAILED")) {
                            monitor = 0;
                            runReset = false;
                            isReset = false;
                            runRestart = false;
                            updateInfo("app:Reset failed @ " + cycleTime + " ms.");
                            cycleTime = 0;
                            updateInfo("app:Please reset again.");
                            updateFrag(FRAG_INFO);
                        } else if (lineReceived.equals("STEP")) {
                            updateInfo("app:Step " + stepTime + " ms.");
                            stepTime = 0;
                        } else if (lineReceived.contains("AT")) {
                            monitor = 0;
                        } else if (lineReceived.equals("OK")) {
                            monitor = 0;
                        } else if (lineReceived.equals("BMU OK")) {
                            monitor = 0;
                            updateInfo("app:Step " + stepTime + " ms.");
                            stepTime = 0;
                            if (serviceSerial != null) serviceSerial.resetFlow();
                        } else if (lineReceived.equals("PROCESS")) {
                            monitor = 0;
                            updateInfo("app:Step " + stepTime + " ms.");
                            stepTime = 0;
                            processData();
                        } else if (lineReceived.equals("FLOW OK")) {
                            monitor = 0;
                            updateInfo("app:Step " + stepTime + " ms.");
                            stepTime = 0;
                            processData();
                        } else if (lineReceived.equals("FLOW FAILED")) {
                            monitor = 0;
                            updateInfo("app:Step " + stepTime + " ms.");
                            stepTime = 0;
                            updateFrag(FRAG_INFO);
                            if (serviceSerial != null) serviceSerial.startBMU();
                        } else if (lineReceived.contains("ELM327")) {
                            if (isSleeping) {
                                monitor = 0;
                                isSleeping = false;
                                isReset = false;
                                updateInfo("app:Dongle awake " + cycleTime + " ms.");
                                if (runCollector) doRestart();
                                else finishOnStart();
                            }
                        } else {
                            if (runCollector) readLine(lineReceived);
                        }
                    }
                    break;

                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    connectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
            }
        }
    };

    private final Runnable monitorOBD = new Runnable() {
        public void run() {
            cycleTimer();
            long time_OBD = 600;
            if (serviceSerial != null) {
                switch (serviceSerial.getState()) {
                    case BluetoothSerialService.STATE_CONNECTED:
                        if (runReset) {
                            if (monitor == 12) {
                                runReset = false;
                                runRestart = false;
                                updateInfo("app:Reset timeout @ " + cycleTime + " ms.");
                                updateInfo("app:Please reset again.");
                                serviceSerial.timeoutReset();
                                updateFrag(FRAG_INFO);
                            }
                        } else if (runCollector) {
                            if (monitor == 1) {
                                updateInfo("app:Data collector stepped @ " + cycleTime + " ms.");
                                serviceSerial.stepCollector();
                            } else if (monitor == 16) {
                                updateInfo("app:Data collector restarted @ " + cycleTime + " ms.");
                                updateFrag(FRAG_INFO);
                                serviceSerial.startCollector();
                            } else if (monitor == 32) {
                                updateInfo("app:Dongle asleep @ " + cycleTime + " ms.");
                                updateFrag(FRAG_INFO);
                                isSleeping = true;
                                serviceSerial.wakeUp();
                            } else if (monitor == 48) {
                                updateInfo("app:Dongle disconnect @ " + cycleTime + " ms.");
                                updateFrag(FRAG_INFO);
                                serviceSerial.disconnect();
                            }
                        }
                        break;
                    case BluetoothSerialService.STATE_CONNECTING:
                        break;
                    case BluetoothSerialService.STATE_FAILED:
                    case BluetoothSerialService.STATE_LOST:
                    case BluetoothSerialService.STATE_NONE:
                        if (runCollector) {
                            if (monitor == 64) {
                                updateInfo("app:OBD will try to reconnect 3 times.");
                                updateInfo("app:Please wait. This takes time.");
                                updateFrag(FRAG_INFO);
                            } else if (monitor == 96 || monitor == 160 || monitor == 224) {
                                updateInfo("app:Reconnecting @ " + cycleTime + " ms.");
                                updateFrag(FRAG_INFO);
                                if (connectedDevice != null)
                                    serviceSerial.connect(connectedDevice);
                            } else if (monitor > 288) {
                                updateInfo("app:Data collection stopped @ " + cycleTime + " ms.");
                                updateFrag(FRAG_INFO);
                                monitor = 0;
                                stopData();
                            }
                        }
                        break;
                }
                handlerMonitor.postDelayed(monitorOBD, time_OBD);
            }
            monitor++;
        }
    };

    private void cycleTimer() {
        long time_ms = currentTimeMillis() - previousTime;
        previousTime = currentTimeMillis();
        stepTime += time_ms;
        cycleTime += time_ms;
    }

    private void iniStorage() {
        if (fileFolder != null && fileFolder.exists()) {
            boolean noExceptions = true;
            boolean Ok = true;
            String currentDateTime = fileDate.format(new Date());

            String file = "Info_" + currentDateTime + ".txt";
            fileInfo = new File(fileFolder, file);
            if (fileInfo.exists()) Ok = fileInfo.delete();
            String textToWrite = "Info Screen messages" + "\r\n";
            try {
                FileOutputStream out = new FileOutputStream(fileInfo);
                OutputStreamWriter osw = new OutputStreamWriter(out);
                osw.write(textToWrite);
                osw.flush();
                osw.close();
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "iniStorage " + e);
                updateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "PID_" + currentDateTime + ".txt";
            filePIDs = new File(fileFolder, file);
            if (filePIDs.exists()) Ok = filePIDs.delete();
            textToWrite = "Time;PID;hex0;hex1;hex2;hex3;hex4;hex5;hex6;hex7" + "\r\n";
            try {
                FileOutputStream out = new FileOutputStream(filePIDs);
                OutputStreamWriter osw = new OutputStreamWriter(out);
                osw.write(textToWrite);
                osw.flush();
                osw.close();
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "iniStorage " + e);
                updateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "PIDInt_" + currentDateTime + ".txt";
            filePIDInt = new File(fileFolder, file);
            if (filePIDInt.exists()) Ok = filePIDInt.delete();
            textToWrite = "Time;PID;int0;int1;int2;int3;int4;int5;int6;int7" + "\r\n";
            try {
                FileOutputStream out = new FileOutputStream(filePIDInt);
                OutputStreamWriter osw = new OutputStreamWriter(out);
                osw.write(textToWrite);
                osw.flush();
                osw.close();
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "iniStorage " + e);
                updateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "Cells_" + currentDateTime + ".txt";
            fileCells = new File(fileFolder, file);
            if (fileCells.exists()) Ok = fileCells.delete();
            textToWrite = "Time;Module;Cell;Volts;InterpolatedTemperature;SoC;Capacity1;SoCsum;Capacity2" + "\r\n";
            try {
                FileOutputStream out = new FileOutputStream(fileCells);
                OutputStreamWriter osw = new OutputStreamWriter(out);
                osw.write(textToWrite);
                osw.flush();
                osw.close();
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "iniStorage " + e);
                updateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "CellTemperatures_" + currentDateTime + ".txt";
            fileSensors = new File(fileFolder, file);
            if (fileSensors.exists()) Ok = fileSensors.delete();
            textToWrite = "Time;Module;Sensor;Temperature" + "\r\n";
            try {
                FileOutputStream out = new FileOutputStream(fileSensors);
                OutputStreamWriter osw = new OutputStreamWriter(out);
                osw.write(textToWrite);
                osw.flush();
                osw.close();
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "iniStorage " + e);
                updateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "OBD_" + currentDateTime + ".txt";
            fileOBD = new File(fileFolder, file);
            if (fileOBD.exists()) Ok = fileOBD.delete();
            textToWrite = "Time;Parameter;Value" + "\r\n";
            try {
                FileOutputStream out = new FileOutputStream(fileOBD);
                OutputStreamWriter osw = new OutputStreamWriter(out);
                osw.write(textToWrite);
                osw.flush();
                osw.close();
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "iniStorage " + e);
                updateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "Calc_" + currentDateTime + ".txt";
            fileCalc = new File(fileFolder, file);
            if (fileCalc.exists()) Ok = fileCalc.delete();
            textToWrite = "Time;Parameter;Value" + "\r\n";
            try {
                FileOutputStream out = new FileOutputStream(fileCalc);
                OutputStreamWriter osw = new OutputStreamWriter(out);
                osw.write(textToWrite);
                osw.flush();
                osw.close();
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "iniStorage " + e);
                updateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            if (noExceptions && Ok) {
                updateInfo("app:Recording started");
                if (!runCollector) updateFrag(FRAG_INFO);
                iniRecording = false;
                runRecording = true;
                btnFive.setBackgroundColor(clrDarkGreen);
                StoreInfo();
            } else {
                iniRecording = false;
                runRecording = false;
                updateInfo("app:Recording failed for reasons unknown.");
                updateFrag(FRAG_INFO);
                stopRecording();
            }
        }

    }

    private String strDateTime(Date datetime) {
        final String strDT;
        String testNumber = decFix1.format(1.0);
        if (testNumber.contains(".")) {
            strDT = dataDateDot.format(datetime);
        } else {
            strDT = dataDateComma.format(datetime);
        }
        switch (strDT.length()) {
            case 21:
                return strDT + "00";
            case 22:
                return strDT + "0";
            case 23:
                return strDT;
            default:
                return "01-01-2020 00:00:00";
        }
    }

    private void updateInfo(String info) {
        listInfo.add(info);
        int i = listInfo.size();
        if (i > 1000) {
            listInfo = new ArrayList<>(listInfo.subList(i - 800, i));
        }
        String date = strDateTime(new Date());
        listStoreInfo.add(date + " " + info);
        i = listStoreInfo.size();
        if (i > 1000) {
            listStoreInfo = new ArrayList<>(listStoreInfo.subList(i - 800, i));
        }
    }

    private void StoreInfo() {
        if ((fileInfo != null && fileInfo.exists() && fileInfo.length() < 5000000L))
            if (listStoreInfo != null && listStoreInfo.size() > 0) {
                String[] str = new String[listStoreInfo.size() + 2];
                str[0] = fileInfo.toString();
                int i = 1;
                for (String aInfo : listStoreInfo) {
                    str[i] = aInfo + "\r\n";
                    i++;
                }
                str[i] = "Stop";
                listStoreInfo.clear();

                new BackgroundTask(MainActivity.this) {
                    @Override
                    public void doInBackground() {
                        try {
                            File file = new File(str[0]);
                            FileOutputStream f = new FileOutputStream(file, true);
                            PrintWriter pw = new PrintWriter(f);
                            for (int i = 1; i < str.length; i++) {
                                if (str[i].equals("Stop")) break;
                                if (str[i] != null) pw.print(str[i]);
                            }
                            pw.flush();
                            pw.close();
                            f.close();
                        } catch (Exception e) {
                            if (DEBUG) Log.e(TAG, "storeInfo" + e);
                            updateInfo("app:Error storing Info in background");

                        }
                    }
                }.execute();
            }
    }

    private void StorePIDs(String datetime) {
        if (filePIDs != null && filePIDs.exists()) {
            int nPID = 200;
            String[] str = new String[nPID + 2];
            str[0] = filePIDs.toString();
            int i = 1;
            ArrayList<String[]> PIDnames = new ArrayList<>();
            PIDnames.add(new String[]{"000", "00"});
            for (PID aPID : allPIDs) {
                boolean found = false;
                for (String[] aName : PIDnames) {
                    if (aPID.str[0].equals(aName[0]) && aPID.str[1].equals(aName[1])) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    PIDnames.add(new String[]{aPID.str[0], aPID.str[1]});
                    StringBuilder strOut = new StringBuilder(datetime);
                    for (String strHex : aPID.str) strOut.append(";").append(strHex);
                    strOut.append("\r\n");
                    str[i] = strOut.toString();
                    i++;
                }
                if (i > nPID) break;
            }
            str[i] = "Stop";

            new BackgroundTask(MainActivity.this) {
                @Override
                public void doInBackground() {
                    try {
                        File file = new File(str[0]);
                        FileOutputStream f = new FileOutputStream(file, true);
                        PrintWriter pw = new PrintWriter(f);
                        for (int i = 1; i < str.length; i++) {
                            if (str[i].equals("Stop")) break;
                            if (str[i] != null) pw.print(str[i]);
                        }
                        pw.flush();
                        pw.close();
                        f.close();
                    } catch (Exception e) {
                        if (DEBUG) Log.e(TAG, "StorePIDs " + e);
                        updateInfo("app:Error storing PID data in background");
                    }
                }
            }.execute();
        }
    }

    private void StorePIDIntegers(String datetime) {
        if (filePIDInt != null && filePIDInt.exists()) {
            int nPID = 200;
            String[] str = new String[nPID + 2];
            str[0] = filePIDInt.toString();
            int i = 1;
            for (PID aPID : listPIDs) {
                if (aPID.isNew) {
                    StringBuilder strOut = new StringBuilder(datetime);
                    strOut.append(";").append(aPID.str[0]);
                    for (int aInt : aPID.intr) strOut.append(";").append(aInt);
                    strOut.append("\r\n");
                    str[i] = strOut.toString();
                    i++;
                }
                if (i > nPID) break;
            }
            str[i] = "Stop";

            new BackgroundTask(MainActivity.this) {
                @Override
                public void doInBackground() {
                    try {
                        File file = new File(str[0]);
                        FileOutputStream f = new FileOutputStream(file, true);
                        PrintWriter pw = new PrintWriter(f);
                        for (int i = 1; i < str.length; i++) {
                            if (str[i].equals("Stop")) break;
                            if (str[i] != null) pw.print(str[i]);
                        }
                        pw.flush();
                        pw.close();
                        f.close();
                    } catch (Exception e) {
                        if (DEBUG) Log.e(TAG, "StorePIDIntegers " + e);
                        updateInfo("app:Error storing PID integers in background");
                    }
                }
            }.execute();
        }
    }

    private void StoreCells(String datetime) {
        if (fileCells != null && fileCells.exists()) {
            int nCells = 118;
            String[] str = new String[nCells + 2];
            str[0] = fileCells.toString();
            int i = 1;
            for (Cell aCell : listCells)
                if (aCell.isNew) {
                    if ((cells88 || aCell.module != 6) && (cells88 || aCell.module != 12)) {
                        str[i] = datetime +
                                ";" + aCell.strModule() +
                                ";" + aCell.strCellLetter() +
                                ";" + aCell.strVolts(3) +
                                ";" + aCell.strTemperature() +
                                ";" + aCell.strSoC() +
                                ";" + aCell.strAh1() +
                                ";" + aCell.strSoCsum() +
                                ";" + aCell.strAh2() +
                                "\r\n";
                        i++;
                    }
                    if (i > nCells) break;
                }
            str[i] = "Stop";

            new BackgroundTask(MainActivity.this) {
                @Override
                public void doInBackground() {
                    try {
                        File file = new File(str[0]);
                        FileOutputStream f = new FileOutputStream(file, true);
                        PrintWriter pw = new PrintWriter(f);
                        for (int i = 1; i < str.length; i++) {
                            if (str[i].equals("Stop")) break;
                            if (str[i] != null) pw.print(str[i]);
                        }
                        pw.flush();
                        pw.close();
                        f.close();
                    } catch (Exception e) {
                        if (DEBUG) Log.e(TAG, "StoreCells " + e);
                        updateInfo("app:Error storing cells data in background");
                    }
                }
            }.execute();
        }
    }

    private void StoreCellTemperatures(String datetime) {
        if (fileSensors != null && fileSensors.exists()) {
            String[] str = new String[120];
            str[0] = fileSensors.toString();
            int i = 1;
            for (CellSensor aSensor : listSensors) {
                if (aSensor.isNew) {
                    if (aSensor.module == 6 || aSensor.module == 12) {
                        if (cells88 && aSensor.sensor < 4) {
                            str[i] = datetime + ";" + aSensor.module + ";" + aSensor.sensor + ";" + aSensor.strTemperature() + "\r\n";
                            i++;
                        }
                    } else {
                        str[i] = datetime + ";" + aSensor.module + ";" + aSensor.sensor + ";" + aSensor.strTemperature() + "\r\n";
                        i++;
                    }
                }
            }
            str[i] = "Stop";

            new BackgroundTask(MainActivity.this) {
                @Override
                public void doInBackground() {
                    try {
                        File file = new File(str[0]);
                        FileOutputStream f = new FileOutputStream(file, true);
                        PrintWriter pw = new PrintWriter(f);
                        for (int i = 1; i < str.length; i++) {
                            if (str[i].equals("Stop")) break;
                            if (str[i] != null) pw.print(str[i]);
                        }
                        pw.flush();
                        pw.close();
                        f.close();
                    } catch (Exception e) {
                        if (DEBUG) Log.e(TAG, "StoreCellTemperatures " + e);
                        updateInfo("app:Error storing temperature in background");
                    }
                }
            }.execute();
        }
    }

    private void StoreOBD(String datetime) {
        if (fileOBD != null && fileOBD.exists()) {
            ArrayList<String> strArray = new ArrayList<>();
            strArray.add(fileOBD.toString());
            strArray.add(datetime + ";Version;" + BuildConfig.VERSION_NAME.replace(".", "") + "\r\n");
            strArray.add(datetime + ";Odometer;" + c_Odo.str() + "\r\n");
            strArray.add(datetime + ";Speed;" + c_Speed0.str() + "\r\n");
            strArray.add(datetime + ";SpeedShown;" + c_SpdShown.str() + "\r\n");
            strArray.add(datetime + ";Speed1;" + c_Speed1.str() + "\r\n");
            strArray.add(datetime + ";Speed2;" + c_Speed2.str() + "\r\n");
            strArray.add(datetime + ";Speed3;" + c_Speed3.str() + "\r\n");
            strArray.add(datetime + ";Speed4;" + c_Speed4.str() + "\r\n");
            strArray.add(datetime + ";Acceleration;" + c_Acc.str() + "\r\n");
            strArray.add(datetime + ";AccPedal;" + c_Pedal.str() + "\r\n");
            strArray.add(datetime + ";KeyOn/Off;" + c_KeyOn.str() + "\r\n");
            strArray.add(datetime + ";Brake;" + c_Brake.str() + "\r\n");
            strArray.add(datetime + ";BrakeOn/Off;" + c_BrakeOn.str() + "\r\n");
            strArray.add(datetime + ";Steering;" + c_Steering.str() + "\r\n");
            strArray.add(datetime + ";Rotation;" + c_Rotation.str() + "\r\n");
            strArray.add(datetime + ";MotorRPM;" + c_RPM.str() + "\r\n");
            strArray.add(datetime + ";BatteryV;" + b_Volts.str() + "\r\n");
            strArray.add(datetime + ";BatteryA;" + b_Amps68.str() + "\r\n");
            strArray.add(datetime + ";BatACalOut;" + c_AmpsCal.str() + "\r\n");
            strArray.add(datetime + ";BatWCalOut;" + c_WattsCal.str() + "\r\n");
            strArray.add(datetime + ";BatteryT;" + b_Temp.str() + "\r\n");
            strArray.add(datetime + ";BatCapAh;" + c_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";BMURemAh;" + bmu_Ah.remStr() + "\r\n");
            strArray.add(datetime + ";BMUCapAh;" + bmu_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";RestRange;" + c_Ah.RRStr() + "\r\n");
            strArray.add(datetime + ";RangeShown;" + c_RRshown.str() + "\r\n");
            strArray.add(datetime + ";SoC1;" + b_SoC1.str() + "\r\n");
            strArray.add(datetime + ";SoC2;" + b_SoC2.str() + "\r\n");
            strArray.add(datetime + ";HeaterA;" + h_Amps.str() + "\r\n");
            strArray.add(datetime + ";HeaterW;" + h_Watts.str() + "\r\n");
            strArray.add(datetime + ";Heat/Cool;" + h_Level.str() + "\r\n");
            strArray.add(datetime + ";FanSpeed;" + a_Fan.str() + "\r\n");
            strArray.add(datetime + ";FanDirect;" + a_Dirc.str() + "\r\n");
            strArray.add(datetime + ";AC;" + ac_On.str() + "\r\n");
            strArray.add(datetime + ";ACAmps;" + ac_Amps.str() + "\r\n");
            strArray.add(datetime + ";ACWatts;" + ac_Watts.str() + "\r\n");
            strArray.add(datetime + ";Charge12Amps;" + c_12vAmps.str() + "\r\n");
            strArray.add(datetime + ";AirRec;" + a_Reci.str() + "\r\n");
            strArray.add(datetime + ";FanMax;" + a_Max.str() + "\r\n");
            strArray.add(datetime + ";LPark;" + l_Park.str() + "\r\n");
            strArray.add(datetime + ";LDrive;" + l_Drive.str() + "\r\n");
            strArray.add(datetime + ";LFrontFog;" + l_FogFront.str() + "\r\n");
            strArray.add(datetime + ";LRearFog;" + l_FogRear.str() + "\r\n");
            strArray.add(datetime + ";LHigh;" + l_High.str() + "\r\n");
            strArray.add(datetime + ";RearDefrost;" + w_DeRear.str() + "\r\n");
            strArray.add(datetime + ";WindWiper;" + w_WiperF.str() + "\r\n");
            strArray.add(datetime + ";Gear;" + c_Gear285.str() + "\r\n");
            strArray.add(datetime + ";Gear418;" + c_Gear.str() + "\r\n");
            strArray.add(datetime + ";AirTemp;" + c_AirSensor.str() + "\r\n");
            strArray.add(datetime + ";AirSensor;" + c_AirSensor.str() + "\r\n");
            strArray.add(datetime + ";Speed100;" + i_Spd100.str() + "\r\n");
            strArray.add(datetime + ";Margin;" + i_Margin.str() + "\r\n");
            strArray.add(datetime + ";Loadkg;" + i_Load.str() + "\r\n");
            strArray.add(datetime + ";MotorA;" + c_MotorA.str() + "\r\n");
            strArray.add(datetime + ";RegenA;" + c_RegA.str() + "\r\n");
            strArray.add(datetime + ";QuickChargeOn/Off;" + c_QuickCharge.str() + "\r\n");
            strArray.add(datetime + ";QuickOn/Off;" + c_QuickCharge.str() + "\r\n");
            strArray.add(datetime + ";QuickCharge%;" + c_QCprocent.str() + "\r\n");
            strArray.add(datetime + ";QuickChargeA;" + c_QCAmps.str() + "\r\n");
            strArray.add(datetime + ";MotorTemp0;" + c_MotorTemp0.str() + "\r\n");
            strArray.add(datetime + ";MotorTemp1;" + c_MotorTemp1.str() + "\r\n");
            strArray.add(datetime + ";MotorTemp2;" + c_MotorTemp2.str() + "\r\n");
            strArray.add(datetime + ";MotorTemp3;" + c_MotorTemp3.str() + "\r\n");
            strArray.add(datetime + ";BatteryTmax;" + b_BatTmax.str() + "\r\n");
            strArray.add(datetime + ";BatteryTmin;" + b_BatTmin.str() + "\r\n");
            strArray.add(datetime + ";BatteryVmax;" + b_BatVmax.str() + "\r\n");
            strArray.add(datetime + ";BatteryVavg;" + b_BatVavg.str() + "\r\n");
            strArray.add(datetime + ";BatteryVmin;" + b_BatVmin.str() + "\r\n");
            strArray.add(datetime + ";BatSoCmax;" + b_Cellmax.strSoC() + "\r\n");
            strArray.add(datetime + ";BatSoCavg;" + b_Cellavg.strSoC() + "\r\n");
            strArray.add(datetime + ";BatSoCmin;" + b_Cellmin.strSoC() + "\r\n");
            strArray.add(datetime + ";CellVmaxMod;" + b_Cellmax.strModule() + "\r\n");
            strArray.add(datetime + ";CellVmaxCell;" + b_Cellmax.strCell() + "\r\n");
            strArray.add(datetime + ";CellVmaxVolt;" + b_Cellmax.strVolts(3) + "\r\n");
            strArray.add(datetime + ";CellVmaxTemp;" + b_Cellmax.strTemperature() + "\r\n");
            strArray.add(datetime + ";CellVminMod;" + b_Cellmin.strModule() + "\r\n");
            strArray.add(datetime + ";CellVminCell;" + b_Cellmin.strCell() + "\r\n");
            strArray.add(datetime + ";CellVminVolt;" + b_Cellmin.strVolts(3) + "\r\n");
            strArray.add(datetime + ";CellVminTemp;" + b_Cellmin.strTemperature() + "\r\n");
            strArray.add(datetime + ";ChargeVDC;" + c_ChargeVDC.str() + "\r\n");
            strArray.add(datetime + ";ChargeVAC;" + c_ChargeVAC.str() + "\r\n");
            strArray.add(datetime + ";ChargeADC;" + c_ChargeADC.str() + "\r\n");
            strArray.add(datetime + ";ChargeAAC;" + c_ChargeAAC.str() + "\r\n");
            strArray.add(datetime + ";ChargeTemp1;" + c_ChargeTemp1.str() + "\r\n");
            strArray.add(datetime + ";ChargeTemp2;" + c_ChargeTemp2.str() + "\r\n");
            strArray.add(datetime + ";BMUCapAh0;" + bmu_CapAh0.str() + "\r\n");
            strArray.add(datetime + ";BMURemAh0;" + bmu_RemAh0.str() + "\r\n");
            strArray.add(datetime + ";BMURemAh1;" + bmu_RemAh1.str() + "\r\n");
            strArray.add(datetime + ";CapAh0;" + b_CapAh0.str() + "\r\n");
            strArray.add(datetime + ";RemAh0;" + b_RemAh0.str() + "\r\n");
            strArray.add(datetime + ";CapAh1;" + b_CapAh1.str() + "\r\n");
            strArray.add(datetime + ";RemAh1;" + b_RemAh1.str() + "\r\n");
            strArray.add(datetime + ";BatteryV0;" + b_Volts0.str() + "\r\n");
            strArray.add(datetime + ";BatteryV1;" + b_Volts1.str() + "\r\n");
            strArray.add(datetime + ";SoC10;" + b_SoC10.str() + "\r\n");
            strArray.add(datetime + ";SoC11;" + b_SoC11.str() + "\r\n");
            strArray.add(datetime + ";Pause1min;" + p1_Time.str() + "\r\n");
            strArray.add(datetime + ";Pause1Volts;" + p1_Volts.str() + "\r\n");
            strArray.add(datetime + ";Pause1Ah;" + p1_Ah.str() + "\r\n");
            strArray.add(datetime + ";Pause1SoC;" + p1_SoC.str() + "\r\n");
            strArray.add(datetime + ";Pause2min;" + p2_Time.str() + "\r\n");
            strArray.add(datetime + ";Pause2Volts;" + p2_Volts.str() + "\r\n");
            strArray.add(datetime + ";Pause2Ah;" + p2_Ah.str() + "\r\n");
            strArray.add(datetime + ";Pause2SoC;" + p2_SoC.str() + "\r\n");
            strArray.add(datetime + ";ChargeCapAh;" + p12_CapAh.str() + "\r\n");

            strArray.add(datetime + ";PIDCount;" + m_newPIDs + "\r\n");
            if (i_OCV.equals("old")) strArray.add(datetime + ";OCVNew;" + "0" + "\r\n");
            else strArray.add(datetime + ";OCVNew;" + "1" + "\r\n");
            if (i_RangeUnits.equals("km")) strArray.add(datetime + ";RangeMiles;" + "0" + "\r\n");
            else strArray.add(datetime + ";RangeMiles;" + "1" + "\r\n");
            if (i_OdoUnits.equals("km")) strArray.add(datetime + ";OdoMiles;" + "0" + "\r\n");
            else strArray.add(datetime + ";OdoMiles;" + "1" + "\r\n");
            strArray.add("Stop");
            String[] str = new String[strArray.size()];
            int i = 0;
            for (String aString : strArray) {
                str[i] = aString;
                i++;
            }

            new BackgroundTask(MainActivity.this) {
                @Override
                public void doInBackground() {
                    try {
                        File file = new File(str[0]);
                        FileOutputStream f = new FileOutputStream(file, true);
                        PrintWriter pw = new PrintWriter(f);
                        for (int i = 1; i < str.length; i++) {
                            if (str[i].equals("Stop")) break;
                            if (str[i] != null) pw.print(str[i]);
                        }
                        pw.flush();
                        pw.close();
                        f.close();
                    } catch (Exception e) {
                        if (DEBUG) Log.e(TAG, "StoreOBD " + e);
                        updateInfo("app:Error storing OBD data in background");
                    }
                }
            }.execute();
        }
    }

    private void StoreCalc(String datetime) {
        if (fileCalc != null && fileCalc.exists()) {
            ArrayList<String> strArray = new ArrayList<>();
            strArray.add(fileCalc.toString());
            strArray.add(datetime + ";B A;" + c_AmpsCal.str() + "\r\n");
            strArray.add(datetime + ";B W;" + c_WattsCal.str() + "\r\n");
            strArray.add(datetime + ";B WAvg;" + b_Wavg.str() + "\r\n");
            strArray.add(datetime + ";M W;" + m_Watts.str() + "\r\n");
            strArray.add(datetime + ";M WAvg;" + m_Wavg.str() + "\r\n");
            strArray.add(datetime + ";T W;" + t_W.str() + "\r\n");
            strArray.add(datetime + ";C CapAh;" + c_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";B CapAh;" + b_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";M CapAh;" + m_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";T CapAh;" + t_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";BMU CapAh;" + bmu_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";NMC CapAh;" + nmc_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";C Ah;" + c_Ah.remStr() + "\r\n");
            strArray.add(datetime + ";B Ah;" + b_Ah.remStr() + "\r\n");
            strArray.add(datetime + ";M Ah;" + m_Ah.remStr() + "\r\n");
            strArray.add(datetime + ";T Ah;" + t_Ah.remStr() + "\r\n");
            strArray.add(datetime + ";BMU Ah;" + bmu_Ah.remStr() + "\r\n");
            strArray.add(datetime + ";NMC Ah;" + nmc_Ah.remStr() + "\r\n");
            strArray.add(datetime + ";C Wh;" + c_Ah.remWhStr() + "\r\n");
            strArray.add(datetime + ";B Wh;" + b_Ah.remWhStr() + "\r\n");
            strArray.add(datetime + ";M Wh;" + m_Ah.remWhStr() + "\r\n");
            strArray.add(datetime + ";T Wh;" + t_Ah.remWhStr() + "\r\n");
            strArray.add(datetime + ";BMU Wh;" + bmu_Ah.remWhStr() + "\r\n");
            strArray.add(datetime + ";NMC Wh;" + nmc_Ah.remWhStr() + "\r\n");
            strArray.add(datetime + ";C Wh10;" + c_Ah.remWh10Str() + "\r\n");
            strArray.add(datetime + ";B Wh10;" + b_Ah.remWh10Str() + "\r\n");
            strArray.add(datetime + ";M Wh10;" + m_Ah.remWh10Str() + "\r\n");
            strArray.add(datetime + ";T Wh10;" + t_Ah.remWh10Str() + "\r\n");
            strArray.add(datetime + ";BMU Wh10;" + bmu_Ah.remWh10Str() + "\r\n");
            strArray.add(datetime + ";NMC Wh10;" + nmc_Ah.remWh10Str() + "\r\n");
            strArray.add(datetime + ";C CapWh;" + c_Ah.capWhStr() + "\r\n");
            strArray.add(datetime + ";B CapWh;" + b_Ah.capWhStr() + "\r\n");
            strArray.add(datetime + ";M CapWh;" + m_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";T CapWh;" + t_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";BMU CapWh;" + bmu_Ah.capWhStr() + "\r\n");
            strArray.add(datetime + ";NMC CapWh;" + nmc_Ah.capWhStr() + "\r\n");
            strArray.add(datetime + ";M SoCavg;" + m_SoCavg.str() + "\r\n");
            strArray.add(datetime + ";C SoC;" + b_Ah.SoCStr() + "\r\n");
            strArray.add(datetime + ";B SoC;" + b_Ah.SoCStr() + "\r\n");
            strArray.add(datetime + ";M SoC;" + m_Ah.SoCStr() + "\r\n");
            strArray.add(datetime + ";T SoC;" + t_Ah.SoCStr() + "\r\n");
            strArray.add(datetime + ";BMU SoC;" + bmu_Ah.SoCStr() + "\r\n");
            strArray.add(datetime + ";NMC SoC;" + nmc_Ah.SoCStr() + "\r\n");
            strArray.add(datetime + ";C Wh/km;" + c_Ah.WhkmStr() + "\r\n");
            strArray.add(datetime + ";C Wh/kmAux;" + c_Ah.WhkmStr() + "\r\n");
            strArray.add(datetime + ";B Wh/km;" + b_Whkm.str() + "\r\n");
            strArray.add(datetime + ";B Wh/kmAux;" + b_Ah.WhkmStr() + "\r\n");
            strArray.add(datetime + ";M Wh/km;" + m_Whkm.str() + "\r\n");
            strArray.add(datetime + ";M Wh/kmAux;" + m_Ah.WhkmStr() + "\r\n");
            strArray.add(datetime + ";T Wh/km;" + t_Ah.WhkmStr() + "\r\n");
            strArray.add(datetime + ";T Wh/kmAux;" + t_Ah.WhkmStr() + "\r\n");
            strArray.add(datetime + ";BMU Wh/km;" + bmu_Ah.WhkmStr() + "\r\n");
            strArray.add(datetime + ";BMU Wh/kmAux;" + bmu_Ah.WhkmStr() + "\r\n");
            strArray.add(datetime + ";NMC Wh/kmAux;" + nmc_Ah.WhkmStr() + "\r\n");
            strArray.add(datetime + ";C RR;" + c_Ah.RRStr() + "\r\n");
            strArray.add(datetime + ";C RRtest;" + c_RRtest.str() + "\r\n");
            strArray.add(datetime + ";B RR;" + b_Ah.RRStr() + "\r\n");
            strArray.add(datetime + ";M RR;" + m_Ah.RRStr() + "\r\n");
            strArray.add(datetime + ";T RR;" + t_Ah.RRStr() + "\r\n");
            strArray.add(datetime + ";BMU RR;" + bmu_Ah.RRStr() + "\r\n");
            strArray.add(datetime + ";NMC RR;" + nmc_Ah.RRStr() + "\r\n");
            strArray.add(datetime + ";M Odometer;" + m_Odo.str() + "\r\n");
            strArray.add(datetime + ";C kmTest;" + c_kmTest.str() + "\r\n");
            strArray.add(datetime + ";M km;" + m_km.str() + "\r\n");
            strArray.add(datetime + ";M kmTest;" + m_kmTest.str() + "\r\n");
            strArray.add(datetime + ";T km;" + t_km.str() + "\r\n");
            strArray.add(datetime + ";T reqR;" + t_reqkm.str() + "\r\n");
            strArray.add(datetime + ";T Slope;" + t_Slope.str() + "\r\n");
            strArray.add(datetime + ";M Wind;" + m_Wind.str() + "\r\n");
            strArray.add(datetime + ";M Aux;" + m_AuxW.str() + "\r\n");
            strArray.add(datetime + ";E N;" + e_N.str() + "\r\n");
            strArray.add(datetime + ";E W;" + e_Watts.str() + "\r\n");
            strArray.add(datetime + ";M ekg/s;" + m_Error.str() + "\r\n");
            strArray.add(datetime + ";M eN;" + e_N.str() + "\r\n");
            strArray.add(datetime + ";M eW;" + e_Watts.str() + "\r\n");
            strArray.add(datetime + ";C Margin;" + i_Margin.str() + "\r\n");
            strArray.add(datetime + ";T Margin;" + t_Margin.str() + "\r\n");
            strArray.add(datetime + ";T RRChg;" + t_Margin.str() + "\r\n");
            strArray.add(datetime + ";T Speed;" + t_Speed.str() + "\r\n");
            strArray.add(datetime + ";Avg Speed;" + c_SpdShnAvg.str() + "\r\n");
            strArray.add(datetime + ";C AvgSpeed;" + c_Speed0Avg.str() + "\r\n");
            strArray.add(datetime + ";C AvgSpdRR;" + c_SpdAvgRR.str() + "\r\n");
            strArray.add(datetime + ";B CapEst;" + b_CapEst.str() + "\r\n");
            strArray.add(datetime + ";B AhDis;" + b_Ah.remStr() + "\r\n");
            strArray.add(datetime + ";B AhChg;" + b_Ah.remStr() + "\r\n");
            strArray.add(datetime + ";C SoCDis;" + b_SoC2.str() + "\r\n");
            strArray.add(datetime + ";C SoCChg;" + b_SoC2.str() + "\r\n");
            strArray.add(datetime + ";B CapDisAh;" + c_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";B CapChgAh;" + c_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";B CapAhChk;" + c_Ah.capStr() + "\r\n");
            strArray.add(datetime + ";B AavgRR;0\r\n");
            strArray.add(datetime + ";C Load;" + i_Load.str() + "\r\n");
            strArray.add(datetime + ";C Roll;" + c_Roll.str() + "\r\n");
            strArray.add(datetime + ";C Drag;" + c_Drag.str() + "\r\n");
            strArray.add(datetime + ";C RegW;" + c_RegW.str() + "\r\n");
            strArray.add(datetime + ";M Acc;" + m_AccW.str() + "\r\n");
            strArray.add(datetime + ";M AccAvg;" + m_AccW.str() + "\r\n");
            strArray.add(datetime + ";M SoC;" + m_SoCavg.str() + "\r\n");
            strArray.add(datetime + ";M LowAmins;" + m_OCtimer.str() + "\r\n");
            strArray.add(datetime + ";M CapTemp;" + m_CapTemp.str() + "\r\n");
            strArray.add(datetime + ";M Cap1AhUsed;" + b_Ah.usedStr() + "\r\n");
            strArray.add(datetime + ";M Cap1Ahmax;" + b_Cellmax.strAh1() + "\r\n");
            strArray.add(datetime + ";M Cap1Ahavg;" + b_Cellavg.strAh1() + "\r\n");
            strArray.add(datetime + ";M Cap1Ahmin;" + b_Cellmin.strAh1() + "\r\n");
            strArray.add(datetime + ";M Cap2Ahsum;" + m_Ah.sumStr() + "\r\n");
            strArray.add(datetime + ";M Cap2Ahmax;" + m_CAh2max.strAh2() + "\r\n");
            strArray.add(datetime + ";M Cap2Ahavg;" + m_CAh2avg.strAh2() + "\r\n");
            strArray.add(datetime + ";M Cap2Ahmin;" + m_CAh2min.strAh2() + "\r\n");
            strArray.add("Stop");
            String[] str = new String[strArray.size()];
            int i = 0;
            for (String aString : strArray) {
                str[i] = aString;
                i++;
            }

            new BackgroundTask(MainActivity.this) {
                @Override
                public void doInBackground() {
                    try {
                        File file = new File(str[0]);
                        FileOutputStream f = new FileOutputStream(file, true);
                        PrintWriter pw = new PrintWriter(f);
                        for (int i = 1; i < str.length; i++) {
                            if (str[i].equals("Stop")) break;
                            if (str[i] != null) pw.print(str[i]);
                        }
                        pw.flush();
                        pw.close();
                        f.close();
                    } catch (Exception e) {
                        if (DEBUG) Log.e(TAG, "StoreCalc " + e);
                        updateInfo("app:Error storing Calc data in background");
                    }
                }
            }.execute();
        }
    }

    private void exposeFiles() {
        if (fileInfo != null && fileInfo.exists())
            MediaScannerConnection.scanFile(getApplicationContext(),
                    new String[]{
                            fileInfo.toString()},
                    null,
                    (path, uri) -> Log.i(TAG,
                            "file was scanned successfully: " + uri));
        if (fileOBD != null && fileOBD.exists())
            MediaScannerConnection.scanFile(getApplicationContext(),
                    new String[]{
                            fileOBD.toString()},
                    null,
                    (path, uri) -> Log.i(TAG,
                            "file was scanned successfully: " + uri));
        if (fileCells != null && fileCells.exists())
            MediaScannerConnection.scanFile(getApplicationContext(),
                    new String[]{
                            fileCells.toString()},
                    null,
                    (path, uri) -> Log.i(TAG,
                            "file was scanned successfully: " + uri));
        if (fileSensors != null && fileSensors.exists())
            MediaScannerConnection.scanFile(getApplicationContext(),
                    new String[]{
                            fileSensors.toString()},
                    null,
                    (path, uri) -> Log.i(TAG,
                            "file was scanned successfully: " + uri));
        if (fileCalc != null && fileCalc.exists())
            MediaScannerConnection.scanFile(getApplicationContext(),
                    new String[]{
                            fileCalc.toString()},
                    null,
                    (path, uri) -> Log.i(TAG,
                            "file was scanned successfully: " + uri));
        if (filePIDs != null && filePIDs.exists())
            MediaScannerConnection.scanFile(getApplicationContext(),
                    new String[]{
                            filePIDs.toString()},
                    null,
                    (path, uri) -> Log.i(TAG,
                            "file was scanned successfully: " + uri));
        if (filePIDInt != null && filePIDInt.exists())
            MediaScannerConnection.scanFile(getApplicationContext(),
                    new String[]{
                            filePIDInt.toString()},
                    null,
                    (path, uri) -> Log.i(TAG,
                            "file was scanned successfully: " + uri));
    }
}
