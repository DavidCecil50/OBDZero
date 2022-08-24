package dc.local.electriccar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

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

import static android.graphics.Color.BLACK;
import static android.os.Build.VERSION.SDK_INT;
import static java.lang.Double.parseDouble;
import static java.lang.System.currentTimeMillis;

/*
  This code is based on the BlueTerm code by pymasde.es found on GitHub and dated the 7th May 2014
*/
public class MainActivity extends AppCompatActivity {

    //Set to true to add debugging code and logging.
    private static final String TAG = "Main Activity";
    private static final boolean DEBUG = true;

    private static final int PERMIT_NEARBY = 1;
    private static final int PERMIT_STORAGE = 2;

    // Message types sent from the BluetoothReadService Handler
    static final int MESSAGE_STATE_CHANGE = 1;
    static final int MESSAGE_RECEIVED = 2;
    static final int MESSAGE_DEVICE_NAME = 4;

    // Key names received from the BluetoothChatService Handler
    static final String DEVICE_NAME = "device_name";
    static final String RECEIVED_LINE = "received_line";

    private BluetoothSerialService serviceSerial = null;
    private BluetoothAdapter adapterBluetooth = null;

    // Name of the connected device
    private static BluetoothDevice connectedDevice = null;
    private static String connectedDeviceName = null;
    private static String deviceMacAddress = null;
    private static int attemptNo = 0;
    private static final int attemptLast = 3;


    private long previousTime = 0;
    private long stepTime = 0;
    private long cycleTime = 0;
    private long recordTime = 0;
    private long bmuTime = 110000L;

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

    // Identifying the fragment in focus
    private static final int FRAG_INFO = 1;
    private static final int FRAG_PID = 2;
    private static final int FRAG_CELLS = 3;
    private static final int FRAG_OBD = 4;
    private static final int FRAG_CALC = 5;
    private static final int FRAG_WATTS = 6;
    private static final int FRAG_DRV = 7;
    private static final int FRAG_CAP1 = 8;
    private static final int FRAG_CAP2 = 9;

    private static int fragNo = FRAG_INFO;

    private static final int clrDarkGreen = 0xFF047C14;

    static final String TRUE_SPEED = "speed";
    static final String PREFERRED_MARGIN = "margin";
    static final String CAR_LOAD = "load";
    static final String RANGE_UNITS = "km";
    static final String ODO_UNITS = "km";
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

    private static final PID[] listPIDs = new PID[collectedPIDs.length + 80]; // PIDs 6E1-6E4 use 48 PIDs and 762 uses 27 PIDS
    private static final ArrayList<PID> allPIDs = new ArrayList<>();

    private static final Cell[] listCells = new Cell[96];
    private static final CellSensor[] listSensors = new CellSensor[96];

    private static File fileDir = null;
    private static File fileInfo = null;
    private static File filePIDs = null;
    private static File filePIDInt = null;
    private static File fileCells = null;
    private static File fileSensors = null;
    private static File fileOBD = null;
    private static File fileCalc = null;
    private static File fileInitial = null;
    private String strFileDir = "";


    private final static SimpleDateFormat fileDate = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
    private final static SimpleDateFormat dataDateDot = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.US);
    private final static SimpleDateFormat dataDateComma = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss,SSS", Locale.US);
    private final static SimpleDateFormat displayDate = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private final static SimpleDateFormat displayTime = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private final static DecimalFormat decFix0 = new DecimalFormat("##0");
    private final static DecimalFormat decFix1 = new DecimalFormat("##0.0");
    private final static DecimalFormat decFix2 = new DecimalFormat("##0.00");
    private final static DecimalFormat decFix00 = new DecimalFormat("00");

    private Dialog dialogAbout;

    private boolean okFileDir = false;
    private boolean okFileInitial = false;
    private boolean runReset = false;
    private boolean isReset = false;
    private boolean runRestart = false;
    private boolean runCollector = false;
    private boolean iniComputing = false;
    private boolean runComputing = false;
    private boolean iniRecording = false;
    private boolean runRecording = false;
    private boolean leftTabs = true;
    private boolean cells88 = true;
    private boolean cellsData = false;
    private boolean okOBDdata = false;
    private boolean errorAC = false;
    private boolean runBMU = false;

    static boolean milesPerkWh = false;
    static boolean miles = false;
    static boolean mph = false;
    static final double KmPerMile = 1.609344;

    static boolean checkRangeUnits = false;
    static boolean checkOdoUnits = false;

    static int m_CapStep = 0;
    static int m_CapCount = 0;
    private int m_newPIDs = 0;
    private int m_CellsNo = 88;
    private int iniFileNo = 0;

    private static Date stepDateTime = new Date();

    private static long p_Time = currentTimeMillis();
    private static double d_Second = 1.0;
    private static double d_Hour = 0.0056;

    private static final double b_Vavg = 320;
    private static final double s_Whkm = 110;

    private final String[] strVIN = {"V", "I", "N"};

    static final OBD i_Spd100 = new OBD(95, "km/h", 0);
    static final OBD i_Margin = new OBD(10, "km", 0);
    static final OBD i_Load = new OBD(150, "kg", 0);
    static final OBD i_RecordTime = new OBD(5, "sec", 0);
    static String i_RangeUnits = "km";
    static String i_OdoUnits = "km";

    private static final OBD c_Mass = new OBD(1120 + i_Load.dbl, "kg", 0);
    private static final OBD c_Roll = new OBD(9.89 * 0.018 * c_Mass.dbl, "", 2);
    private static final OBD c_Drag = new OBD(0.8 * (1.2978 - 0.0046 * 15) / 2.0, "", 2);
    private static final OBD c_CapAh = new OBD(0, "Ah", 1);
    private static final OBD c_CapWh = new OBD(c_CapAh.dbl * b_Vavg, "Ah", 0);
    private static final OBD c_Amps = new OBD(0, "A", 2);
    private static final OBD c_AmpsCal = new OBD(0, "A", 2);
    private static final OBD c_OdoShown = new OBD(0, "km", 0);
    private static final OBD c_Odo = new OBD(0, "km", 0);
    private static final OBD p_Odo = new OBD(0, "km", 0);
    private static final OBD c_SpdShown = new OBD(0, "km/h", 0);
    private static final OBD c_SpdAvgRR = new OBD(20, "km/h", 1);
    private static final OBD c_Speed0 = new OBD(0, "km/h", 1);
    private static final OBD c_Speed1 = new OBD(0, "km/h", 2);
    private static final OBD c_Speed2 = new OBD(0, "km/h", 2);
    private static final OBD c_Speed3 = new OBD(0, "km/h", 2);
    private static final OBD c_Speed4 = new OBD(0, "km/h", 2);
    private static final OBD p_Speed = new OBD(0, "km/h", 1);
    private static final OBD c_SpdTrueAvg = new OBD(20, "km/h", 1);
    private static final OBD c_Acceleration = new OBD(0, "m/s2", 3);
    private static final OBD c_Pedal = new OBD(0, "%", 2);
    private static final OBD c_SpdCor = new OBD(0.95, "", 2);
    private static final OBD c_Steering = new OBD(0, "deg", 0);
    private static final OBD c_Rotation = new OBD(0, "%", 2);
    private static final OBD c_Brake = new OBD(0, "", 0);
    private static final OBD c_RestRange = new OBD(0, "km", 0);
    private static final OBD c_RRshown = new OBD(c_RestRange.dbl, "km", 0);
    private static final OBD c_RR = new OBD(c_RestRange.dbl, "km", 1);
    private static final OBD c_RRtest = new OBD(c_RestRange.dbl, "km", 1);
    private static final OBD c_AhRem = new OBD(c_CapAh.dbl, "Ah", 2);
    private static final OBD d_AhCal = new OBD(0, "Ah", 2);
    private static final OBD c_kmTest = new OBD(5, "km", 0);
    private static final OBD c_Whkm = new OBD(s_Whkm, "Wh/km", 0);
    private static final OBD p_SoC = new OBD(0, "%", 1);
    private static final OBD c_SoC1 = new OBD(0, "%", 1);
    private static final OBD c_SoC2 = new OBD(0, "%", 1);
    private static final OBD c_RPM = new OBD(0, "rpm", 0);
    private static final OBD c_MotorA = new OBD(0, "A", 2);
    private static final OBD c_RegA = new OBD(0, "A", 1);
    private static final OBD c_RegW = new OBD(0, "W", 0);
    private static final OBD c_BrakeOn = new OBD(0, "", 0);
    private static final OBD c_QuickCharge = new OBD(0, "", 0);
    private static final OBD c_QCprocent = new OBD(0, "%", 0);
    private static final OBD c_QCAmps = new OBD(0, "A", 0);
    private static final OBD c_ChargeVDC = new OBD(0, "V", 0);
    private static final OBD c_ChargeVAC = new OBD(0, "V", 0);
    private static final OBD c_ChargeADC = new OBD(0, "A", 2);
    private static final OBD c_ChargeTemp1 = new OBD(0, "oC", 0);
    private static final OBD c_ChargeTemp2 = new OBD(0, "oC", 0);
    private static final OBD c_ChargeAAC = new OBD(0, "A", 2);
    private static final OBD c_KeyOn = new OBD(0, "", 0);
    private static final OBD c_AirSensor = new OBD(20, "oC", 0);
    private static final OBD c_MotorTemp0 = new OBD(20, "oC", 0);
    private static final OBD c_MotorTemp1 = new OBD(20, "oC", 0);
    private static final OBD c_MotorTemp2 = new OBD(20, "oC", 0);
    private static final OBD c_MotorTemp3 = new OBD(20, "oC", 0);
    private static final OBD c_Model = new OBD(2009, "", 0);
    private static final OBD c_Gear = new OBD(0, "", 0);
    private static final OBD c_Gear285 = new OBD(0, "", 0);

    static final OBD c_SpdAvg = new OBD(20, "km/h", 1);
    static final OBD c_WhRem = new OBD(c_CapWh.dbl, "Wh", 0);
    static final OBD c_Margin = new OBD(c_RestRange.dbl, "km", 0);

    private static final OBD b_CapAh = new OBD(c_CapAh.dbl, "Ah", 1);
    private static final OBD b_CapAhCheck = new OBD(c_CapAh.dbl, "Ah", 1);
    private static final OBD b_WhRem = new OBD(c_CapWh.dbl, "Wh", 0);
    private static final OBD b_AhRem = new OBD(c_CapAh.dbl, "Ah", 2);
    private static final OBD b_Wavg = new OBD(2200, "W", 0);
    private static final OBD b_WavgRR = new OBD(2200, "W", 0);
    private static final OBD b_WMovAvg = new OBD(2200, "W", 0);
    private static final OBD b_Whkm = new OBD(s_Whkm, "Wh/km", 0);
    private static final OBD b_Volts = new OBD(0, "V", 1);
    private static final OBD b_Amps = new OBD(0, "A", 2);
    private static final OBD p_AmpsCal = new OBD(0, "A", 2);
    private static final OBD b_RR = new OBD(c_RestRange.dbl, "km", 1);
    private static final OBD b_Temp = new OBD(15, "oC", 1);
    private static final OBD b_CellVsum = new OBD(242, "V", 3);
    private static final OBD b_CellVavg = new OBD(3.7, "V", 3);
    private static final OBD b_BatTmax = new OBD(20, "oC", 0);
    private static final OBD b_BatTmin = new OBD(20, "oC", 0);
    private static final OBD b_BatVmax = new OBD(3.6, "V", 2);
    private static final OBD b_BatVavg = new OBD(3.6, "V", 2);
    private static final OBD b_BatVmin = new OBD(3.6, "V", 2);
    private static final OBD b_BatSoCmax = new OBD(50, "%", 2);
    private static final OBD b_BatSoCavg = new OBD(50, "%", 2);
    private static final OBD b_BatSoCmin = new OBD(50, "%", 2);
    private static final OBD p_BatSoCmax = new OBD(50, "%", 2);
    private static final OBD p_BatSoCavg = new OBD(50, "%", 2);
    private static final OBD p_BatSoCmin = new OBD(50, "%", 2);

    static final OBD b_Watts = new OBD(2200, "W", 0);
    static final OBD b_WhkmAux = new OBD(s_Whkm, "Wh/km", 0);
    static final OBD b_WAvgAux = new OBD(2200, "W", 0);

    private static final Cell b_CellVmin = new Cell();
    private static final Cell b_CellVmax = new Cell();

    private static final OBD m_Error = new OBD(6, "kg/s", 1);
    private static final OBD e_N = new OBD(m_Error.dbl * c_SpdAvg.dbl / 3.6, "N", 0);
    private static final OBD e_Watts = new OBD(e_N.dbl * c_SpdAvg.dbl / 3.6, "W", 0);

    static final OBD m_km = new OBD(0, "km", 1);
    static final OBD m_AuxW = new OBD(0, "W", 0);

    private static final OBD m_BatSummax = new OBD(0, "%", 2);
    private static final OBD m_BatSumavg = new OBD(0, "%", 2);
    private static final OBD m_BatSummin = new OBD(0, "%", 2);
    private static final OBD m_BatAh1max = new OBD(0, "Ah", 2);
    private static final OBD m_BatAh1min = new OBD(0, "Ah", 2);
    private static final OBD m_BatAh1avg = new OBD(0, "Ah", 2);
    private static final OBD m_BatAh2max = new OBD(0, "Ah", 2);
    private static final OBD m_BatAh2min = new OBD(0, "Ah", 2);
    private static final OBD m_BatAh2avg = new OBD(0, "Ah", 2);
    private static final OBD m_AccW = new OBD(0, "W", 1);
    private static final OBD m_AccWavg = new OBD(0, "W", 1);
    private static final OBD m_AmpsCal = new OBD(c_AmpsCal.dbl, "A", 2);
    private static final OBD m_AmpsAvg = new OBD(c_AmpsCal.dbl, "A", 2);
    private static final OBD m_Odo = new OBD(c_Odo.dbl, "km", 1);
    private static final OBD m_kmTest = new OBD(5, "km", 1);
    private static final OBD m_Wind = new OBD(0, "m/s", 1);
    private static final OBD mp_AmpsCal = new OBD(0, "A", 1);
    private static final OBD m_WhRem = new OBD(c_CapWh.dbl, "Wh", 0);
    private static final OBD m_AhRem = new OBD(c_CapAh.dbl, "Ah", 2);
    private static final OBD m_Watts = new OBD(0, "W", 0);
    private static final OBD m_Whkm = new OBD(s_Whkm, "Wh/km", 0);
    private static final OBD m_Wavg = new OBD(2200, "W", 0);
    private static final OBD m_WavgRR = new OBD(2200, "W", 0);
    private static final OBD m_WAvgAux = new OBD(2200, "W", 0);
    private static final OBD m_WhkmAux = new OBD(s_Whkm, "Wh/km", 0);
    private static final OBD m_WMovAvg = new OBD(500, "W", 0);
    private static final OBD m_RR = new OBD(c_RestRange.dbl, "km", 1);
    private static final OBD m_SoCavg = new OBD(0, "%", 2);
    private static final OBD m_CapAhsum = new OBD(0, "Ah", 2);
    private static final OBD m_CapSoCUsed = new OBD(0, "%", 2);
    private static final OBD m_CapAhUsed = new OBD(0, "Ah", 2);
    private static final OBD m_Cap1Ahmax = new OBD(0, "Ah", 2);
    private static final OBD m_Cap1Ahavg = new OBD(0, "Ah", 2);
    private static final OBD m_Cap1AhavgDisplay = new OBD(0, "Ah", 2);
    private static final OBD m_Cap1Ahmin = new OBD(0, "Ah", 2);
    private static final OBD m_Cap2SoCsum = new OBD(0, "%", 2);
    private static final OBD m_Cap2Ahmax = new OBD(0, "Ah", 2);
    private static final OBD m_Cap2Ahavg = new OBD(0, "Ah", 2);
    private static final OBD m_Cap2AhavgDisplay = new OBD(0, "Ah", 2);
    private static final OBD m_Cap2Ahmin = new OBD(0, "Ah", 2);
    private static final OBD m_OCtimer = new OBD(0, "mins", 2);
    private static final OBD m_CapTemp = new OBD(0, "oC", 1);

    private static final Cell m_CellAhmax = new Cell();
    private static final Cell m_CellAhmin = new Cell();

    private static final OBD t_W = new OBD(15000, "W", 0);
    private static final OBD t_Whkm = new OBD(s_Whkm, "Wh/km", 0);
    private static final OBD t_RR = new OBD(i_Margin.dbl, "km", 1);

    static final OBD t_km = new OBD(0, "km", 1);
    static final OBD t_Speed = new OBD(30, "km/h", 1);
    static final OBD t_WhReq = new OBD(c_CapWh.dbl, "Wh", 0);

    private static final OBD h_Amps = new OBD(0, "A", 1);
    private static final OBD h_Watts = new OBD(0, "W", 0);
    private static final OBD ac_Amps = new OBD(0, "A", 2);
    private static final OBD ac_Watts = new OBD(0, "W", 0);
    private static final OBD h_Level = new OBD(7, "", 0);
    private static final OBD a_Fan = new OBD(0, "", 0);
    private static final OBD a_Dirc = new OBD(4, "", 0);
    private static final OBD ac_On = new OBD(0, "", 0);
    private static final OBD a_Max = new OBD(0, "", 0);
    private static final OBD a_Reci = new OBD(0, "", 0);
    private static final OBD c_12vAmps = new OBD(0, "A", 2);
    private static final OBD c_12vWatts = new OBD(0, "W", 0);

    private static final OBD l_Park = new OBD(0, "", 0);
    private static final OBD l_Drive = new OBD(0, "", 0);
    private static final OBD l_FogFront = new OBD(0, "", 0);
    private static final OBD l_FogRear = new OBD(0, "", 0);
    private static final OBD l_High = new OBD(0, "", 0);
    private static final OBD w_DeRear = new OBD(0, "", 0);
    private static final OBD w_WiperF = new OBD(0, "", 0);

    private final ArrayList<String> listInfo = new ArrayList<>();
    private ArrayList<String> listStoreInfo = new ArrayList<>();
    private final ArrayList<String> arrayOBD = new ArrayList<>();
    private final ArrayList<String> arrayCalc = new ArrayList<>();

    private static MenuItem itemMenuConnect;
    private static MenuItem itemMenuStartStopData;
    private static MenuItem itemMenuStartStopComputing;
    private static MenuItem itemMenuStartStopRecording;

    private final SpannableString menu_initial = new SpannableString("Enter initial values");
    private final SpannableString menu_connect = new SpannableString("Connect dongle");
    private final SpannableString menu_disConnect = new SpannableString("disConnect device");
    private final SpannableString menu_reset = new SpannableString("Reset OBD");
    private final SpannableString menu_start_all = new SpannableString("Start all");
    private final SpannableString menu_start_data = new SpannableString("Start data");
    private final SpannableString menu_stop_data = new SpannableString("Stop data");
    private final SpannableString menu_start_computing = new SpannableString("Start computing");
    private final SpannableString menu_stop_computing = new SpannableString("Stop computing");
    private final SpannableString menu_start_recording = new SpannableString("Start recording");
    private final SpannableString menu_stop_recording = new SpannableString("Stop recording");
    private final SpannableString menu_about = new SpannableString("About");

    private static final int MENU_INT = 2;
    private static final int MENU_CON = 3;
    private static final int MENU_RST = 4;
    private static final int MENU_ALL = 5;
    private static final int MENU_CLT = 6;
    private static final int MENU_CMP = 7;
    private static final int MENU_REC = 8;
    private static final int MENU_ABT = 9;

    private static final int idInitial = R.id.initial_values;
    private static final int idConnect = R.id.connect;
    private static final int idReset = R.id.reset;
    private static final int idStartAll = R.id.start_all;
    private static final int idData = R.id.start_stop_data;
    private static final int idComputing = R.id.start_stop_computing;
    private static final int idRecording = R.id.start_stop_recording;
    private static final int idAbout = R.id.menu_about;
    private static final int idTabulate = R.id.icon_tabulate;
    private static final int idRestart = R.id.icon_restart;

    private ActivityResultLauncher<Intent> DeviceListLauncher;
    private ActivityResultLauncher<Intent> InitialValuesLauncher;

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
                listCells[i * 8 + j].SoC = 0;
                listSensors[i * 8 + j].module = i + 1;
                listSensors[i * 8 + j].sensor = j + 1;
                listSensors[i * 8 + j].temperature = -50;
            }
        }

        m_CellAhmin.module = 0;
        m_CellAhmin.cell = 0;
        m_CellAhmin.volts = 3.7;
        m_CellAhmin.temperature = -50;
        m_CellAhmin.SoC = 0;
        m_CellAhmin.SoCsum = 0;
        m_CellAhmin.capAh1 = 0;
        m_CellAhmin.capAh2 = 0;
        m_CellAhmin.isFound = true;
        m_CellAhmin.isNew = false;

        m_CellAhmax.module = 0;
        m_CellAhmax.cell = 0;
        m_CellAhmax.volts = 3.7;
        m_CellAhmax.temperature = -50;
        m_CellAhmax.SoC = 0;
        m_CellAhmax.SoCsum = 0;
        m_CellAhmax.capAh1 = 0;
        m_CellAhmax.capAh2 = 0;
        m_CellAhmax.isFound = true;
        m_CellAhmax.isNew = false;

        b_CellVmin.module = 0;
        b_CellVmin.cell = 0;
        b_CellVmin.volts = 3.7;
        b_CellVmin.temperature = -50;
        b_CellVmin.SoC = 0;
        b_CellVmin.SoCsum = 0;
        b_CellVmin.capAh1 = 0;
        b_CellVmin.capAh2 = 0;
        b_CellVmin.isFound = true;
        b_CellVmin.isNew = false;

        b_CellVmax.module = 0;
        b_CellVmax.cell = 0;
        b_CellVmax.volts = 3.7;
        b_CellVmax.temperature = -50;
        b_CellVmax.SoC = 0;
        b_CellVmax.SoCsum = 0;
        b_CellVmax.capAh1 = 0;
        b_CellVmax.capAh2 = 0;
        b_CellVmax.isFound = true;
        b_CellVmax.isNew = false;

        m_SoCavg.dbl = 0;

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
        btnTwo.setText("OBD");
        btnThree.setText("Cells");
        btnFour.setText("Watts");
        btnFive.setText("Drive");

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
                                    if (okFileInitial) storeInitialValues();
                                } catch (Exception e) {
                                    if (DEBUG) Log.i(TAG, "DeviceListLauncher " + e);
                                    upDateInfo("app:Connection to the dongle");
                                    upDateInfo("app:failed, cause unknown.");
                                }
                            } else {
                                upDateInfo("app:Connection to the dongle failed");
                                upDateInfo("app:Not a valid device address.");
                            }
                        }
                    } else {
                        upDateInfo("app:Bluetooth device not chosen.");
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
                                    c_SpdCor.dbl = i_Spd100.dbl / 100.0;
                                } catch (NumberFormatException e) {
                                    if (DEBUG) Log.i(TAG, "NumberFormatException " + e);
                                    upDateInfo("app:Initial value error speed correction");
                                }
                            value = iniValues.getStringExtra(PREFERRED_MARGIN);
                            if (value != null)
                                try {
                                    i_Margin.dbl = parseDouble(value.replace(',', '.'));
                                } catch (NumberFormatException e) {
                                    if (DEBUG) Log.i(TAG, "NumberFormatException " + e);
                                    upDateInfo("app:Initial value error range remaining at station");
                                }
                            value = iniValues.getStringExtra(CAR_LOAD);

                            if (value != null)
                                try {
                                    i_Load.dbl = parseDouble(value.replace(',', '.'));
                                } catch (NumberFormatException e) {
                                    if (DEBUG) Log.i(TAG, "NumberFormatException " + e);
                                    upDateInfo("app:Initial value error load");
                                }
                            value = iniValues.getStringExtra(RECORD_TIME);
                            if (value != null)
                                try {
                                    i_RecordTime.dbl = parseDouble(value.replace(',', '.'));
                                } catch (NumberFormatException e) {
                                    if (DEBUG) Log.i(TAG, "NumberFormatException " + e);
                                    upDateInfo("app:Initial value error record sec.");
                                }
                            if (i_RecordTime.dbl > 60) i_RecordTime.dbl = 60;
                            else if (i_RecordTime.dbl < 0) i_RecordTime.dbl = 0;
                            value = iniValues.getStringExtra(RANGE_UNITS);
                            if (value != null) i_RangeUnits = value;
                            value = iniValues.getStringExtra(ODO_UNITS);
                            if (value != null) i_OdoUnits = value;
                            upDateInfo("app:New initial values...");
                            showInitialValues();
                            if (okFileInitial) storeInitialValues();
                        }
                    }
                });

        fragNo = 0;
        selectOne();

    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "--- ON START ---");
        listInfo.clear();
        upDateInfo("app:Download a user manual at:");
        upDateInfo("app: OBDZero.dk");

        int targetSdkVersion = getApplicationInfo().targetSdkVersion;
        if (SDK_INT > 30 && targetSdkVersion > 30) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                serviceSerial = new BluetoothSerialService(handlerBT);
                adapterBluetooth = BluetoothAdapter.getDefaultAdapter();
                adapterBluetooth.enable();
                checkStorePermission();
            } else {
                upDateInfo("app:OBDZero needs nearby device");
                upDateInfo("app:permission for Bluetooth");
                upDateInfo("app:connections.");
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
                upDateInfo("app:OBDZero requires Bluetooth");
                upDateInfo("app:permission. Please grant this in");
                upDateInfo("app:in phone settings and restart");
                upDateInfo("app:OBDZero");
            }
        }
        updateFrag(FRAG_INFO);
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
                    upDateInfo("app:OBDZero does not have");
                    upDateInfo("app:Bluetooth permission and");
                    upDateInfo("app:will not work.");
                    updateFrag(FRAG_INFO);
                }
                break;
            case PERMIT_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createFileDirectory();
                } else {
                    upDateInfo("app:OBDZero does not have storage");
                    upDateInfo("app:permission so stored initial");
                    upDateInfo("app:values could not be retrieved");
                    upDateInfo("app:and values and data collected");
                    upDateInfo("app:by the app cannot be stored.");
                    upDateInfo("app:Restart will not work either.");
                    upDateInfo("app:But OBDZero should still work.");
                    updateFrag(FRAG_INFO);
                    finishOnStart();
                }
                break;
        }
    }

    private void checkStorePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            upDateInfo("app:OBDZero needs this permission");
            upDateInfo("app:to store data and initial values.");
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
            createFileDirectory();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "--- ON RESUME ---");
        monitorOBD.run();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "--- ON PAUSE ---");
        handlerMonitor.removeCallbacks(monitorOBD);
        runRestart = false;
        if (runComputing || iniComputing) stopComputing();
        if (runCollector) stopData();
        if (serviceSerial != null && serviceSerial.getState() == BluetoothSerialService.STATE_CONNECTED)
            stopConnection();
        if (runRecording) StoreInfo();
        if (iniRecording || runRecording) stopRecording();
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
        menu_disConnect.setSpan(new ForegroundColorSpan(Color.WHITE), 0, menu_disConnect.length(), 0);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        runRestart = false;
        switch (item.getItemId()) {
            case idInitial:
                doInitialValues();
                return true;
            case idConnect:
                toggleConnect();
                return true;
            case idReset:
                doReset();
                return true;
            case idStartAll:
                if (!runCollector) startData();
                if (!iniComputing && !runComputing) startComputing();
                if (!iniRecording && !runRecording) startRecording();
                return true;
            case idData:
                toggleData();
                return true;
            case idComputing:
                toggleComputing();
                return true;
            case idRecording:
                toggleRecording();
                return true;
            case idAbout:
                showAboutDialog();
                return true;
            case idTabulate:
                if (leftTabs) {
                    leftTabs = false;
                    btnOne.setText("Info");
                    btnTwo.setText("PIDs");
                    btnThree.setText("Calc");
                    btnFour.setText("Cap1");
                    btnFive.setText("Cap2");
                } else {
                    leftTabs = true;
                    btnOne.setText("Info");
                    btnTwo.setText("OBD");
                    btnThree.setText("Cells");
                    btnFour.setText("Watts");
                    btnFive.setText("Drive");
                }
                return true;
            case idRestart:
                doRestart();
                return true;
            default:
                return false;
        }
    }

    public void createFileDirectory() {
        if (!okFileDir) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                if (SDK_INT < 30) {
                    fileDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/OBDZero");
                    strFileDir = "OBDZero";
                } else {
                    fileDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() +
                            "/OBDZero");
                    strFileDir = "Download/OBDZero";
                }
                if (!fileDir.exists()) {
                    okFileDir = fileDir.mkdirs();
                    if (!okFileDir) {
                        upDateInfo("app:The folder " + fileDir + "could");
                        upDateInfo("app:not be created.");
                    }
                } else {
                    okFileDir = true;
                }
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                okFileDir = false;
                upDateInfo("app:Recording failed.");
                upDateInfo("app:Storage or SDCard are read only.");
            } else {
                okFileDir = false;
                upDateInfo("app:Recording failed.");
                upDateInfo("app:No user storage or");
                upDateInfo("app:SDCard were found.");
                upDateInfo("app:Stored initial values");
                upDateInfo("app:could not be retrieved.");
            }
        }

        if (okFileDir) {
            upDateInfo("app:All data including initial");
            upDateInfo("app:values are stored in");
            upDateInfo("app:in the " + strFileDir + " folder");
            upDateInfo("app:on the phone or on");
            upDateInfo("app:an sdcard depending on how");
            upDateInfo("app:the phone is setup.");
            getInitialValues();
            showInitialValues();
            upDateInfo("app:Use the menu to");
            upDateInfo("app:change initial values.");
            storeInitialValues();
        } else {
            upDateInfo("app:OBDZero will still work.");
        }
        updateFrag(FRAG_INFO);
        finishOnStart();
    }

    public void finishOnStart() {
        if (serviceSerial.getState() != BluetoothSerialService.STATE_CONNECTED) {
            upDateInfo("app:Use the menu to");
            upDateInfo("app:connect the dongle.");
        } else if (!isReset) {
            upDateInfo("app:Use the menu to");
            upDateInfo("app:reset the dongle.");
        } else {
            upDateInfo("app:Use the menu to");
            upDateInfo("app:start data services.");
        }
        updateFrag(FRAG_INFO);
    }

    private void doRestart() {
        upDateInfo("app:Restarting");
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
                        upDateInfo("app:The previously used dongle");
                        upDateInfo("app:was not found.");
                        upDateInfo("app:Please use the menu.");
                        upDateInfo("app:The restart button should work");
                        upDateInfo("app:next time.");
                    }
                } else {
                    runRestart = false;
                    upDateInfo("app:No previously used dongle recorded");
                    upDateInfo("app:Please use the menu.");
                    upDateInfo("app:The restart button should work");
                    upDateInfo("app:next time.");
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
            upDateInfo("app:Bluetooth service is not");
            upDateInfo("app:running. ");
            upDateInfo("app:Please start Bluetooth and");
            upDateInfo("app:start OBDZero again.");
            runRestart = false;
        }
        updateFrag(FRAG_INFO);
    }

    private void getInitialValues() {
        if (okFileDir) {
            okFileInitial = true;
            iniFileNo = 0;
            String strFileInitial;
            if (SDK_INT < 30) {
                strFileInitial = "OBDZero.ini";
                fileInitial = new File(fileDir, "OBDZero.ini");
            } else {
                strFileInitial = "OBDZero00.ini";
                while (new File(fileDir, strFileInitial).exists()) {
                    iniFileNo++;
                    strFileInitial = "OBDZero" + decFix00.format(iniFileNo) + ".ini";
                }
                iniFileNo--;
                strFileInitial = "OBDZero" + decFix00.format(iniFileNo) + ".ini";
                fileInitial = new File(fileDir, strFileInitial);
            }

            if (fileInitial.exists()) {
                ArrayList<String> linesToRead = new ArrayList<>();
                try {
                    FileReader fr = new FileReader(fileInitial);
                    BufferedReader br = new BufferedReader(fr);
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.length() > 0) linesToRead.add(line);
                    }
                    okFileInitial = true;
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "getInitialValues " + e);
                    upDateInfo("app:Error reading initial values file");
                    okFileInitial = false;
                }

                if (okFileInitial) {
                    for (String aLine : linesToRead) {
                        if (aLine != null && aLine.contains(";")) {
                            String[] split = aLine.split(";");
                            if (split[1] != null && split[1].length() > 0) {
                                if (split[0].contains("True")) {
                                    try {
                                        i_Spd100.dbl = parseDouble(split[1].replace(',', '.'));
                                        c_SpdCor.dbl = i_Spd100.dbl / 100.0;
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        upDateInfo("app:True speed at 100 km/h is not a number");
                                    }
                                } else if (split[0].contains("Remaining")) {
                                    try {
                                        i_Margin.dbl = parseDouble(split[1].replace(',', '.'));
                                        t_RR.dbl = t_km.dbl + i_Margin.dbl;
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        upDateInfo("app:The preferred margin is not a number");
                                    }
                                } else if (split[0].contains("Load")) {
                                    try {
                                        i_Load.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        upDateInfo("app:The load in the car is not a number");
                                    }
                                } else if (split[0].contains("Range")) {
                                    i_RangeUnits = split[1];
                                    if (!i_RangeUnits.equals("km") && !i_RangeUnits.equals("miles")) {
                                        i_RangeUnits = "km";
                                        upDateInfo("app:The range units are km or miles.");
                                    }
                                } else if (split[0].contains("Odometer")) {
                                    i_OdoUnits = split[1];
                                    if (!i_OdoUnits.equals("km") && !i_OdoUnits.equals("miles")) {
                                        i_RangeUnits = "km";
                                        upDateInfo("app:The odometer units are km or miles.");
                                    }
                                } else if (split[0].contains("Record")) {
                                    try {
                                        i_RecordTime.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        upDateInfo("app:The minimum time between data records not a number");
                                    }
                                    if (i_RecordTime.dbl > 60) i_RecordTime.dbl = 60;
                                    else if (i_RecordTime.dbl < 0) i_RecordTime.dbl = 0;
                                } else if (split[0].contains("Dongle")) {
                                    if (split[1].length() == 17) {
                                        deviceMacAddress = split[1];
                                    } else {
                                        deviceMacAddress = null;
                                    }
                                }
                            }
                        } else if (aLine != null && aLine.contains(":")) {
                            upDateInfo("app:" + aLine);
                            String[] split = aLine.split(":");
                            if (split[1] != null && split[1].length() > 0) {
                                if (split[0].contains("True")) {
                                    try {
                                        i_Spd100.dbl = parseDouble(split[1].replace(',', '.'));
                                        c_SpdCor.dbl = i_Spd100.dbl / 100.0;
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        upDateInfo("app:True speed at 100 km/h is not a number");
                                    }
                                } else if (split[0].contains("Remaining")) {
                                    try {
                                        i_Margin.dbl = parseDouble(split[1].replace(',', '.'));
                                        t_RR.dbl = t_km.dbl + i_Margin.dbl;
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        upDateInfo("app:The preferred margin is not a number");
                                    }
                                } else if (split[0].contains("Load")) {
                                    try {
                                        i_Load.dbl = parseDouble(split[1].replace(',', '.'));
                                    } catch (NumberFormatException e) {
                                        if (DEBUG) Log.e(TAG, "NumberFormatException " + e);
                                        upDateInfo("app:The load in the car is not a number");
                                    }
                                } else if (split[0].contains("Range")) {
                                    i_RangeUnits = split[1];
                                    if (!i_RangeUnits.equals("km") && !i_RangeUnits.equals("miles")) {
                                        i_RangeUnits = "km";
                                        upDateInfo("app:The range units are km or miles.");
                                    }
                                } else if (split[0].contains("Odometer")) {
                                    i_OdoUnits = split[1];
                                    if (!i_OdoUnits.equals("km") && !i_OdoUnits.equals("miles")) {
                                        i_OdoUnits = "km";
                                        upDateInfo("app:The odometer units are km or miles.");
                                    }
                                }
                            }
                        }
                    }
                    upDateInfo("app:Initial values...");
                } else {
                    upDateInfo("app:The previous initial values");
                    upDateInfo("app:file, " + strFileInitial + ", could");
                    upDateInfo("app:not be read due to Android 11+");
                    upDateInfo("app:rules.");
                    upDateInfo("app:A new file will be created.");
                    upDateInfo("app:Standard initial values...");
                }
            } else {
                okFileInitial = false;
                if (SDK_INT < 30) {
                    strFileInitial = "OBDZero.ini";
                } else {
                    strFileInitial = "OBDZero" + decFix00.format(iniFileNo + 1) + ".ini";
                }
                upDateInfo("app:The " + strFileInitial + " file");
                upDateInfo("app:was not found.");
                upDateInfo("app:A new file will created");
            }
        } else {
            okFileInitial = false;
            upDateInfo("app:The " + strFileDir + " directory");
            upDateInfo("app:was not available and the");
            upDateInfo("app:initial values were not retrieved.");
            upDateInfo("app:Try closing and reopening OBDZero.");
        }
        updateFrag(FRAG_INFO);
    }

    private void storeInitialValues() {
        if (okFileDir) {
            String strFileInitial = "";
            if (!okFileInitial) {
                if (SDK_INT < 30) {
                    strFileInitial = "OBDZero.ini";
                    fileInitial = new File(fileDir, "OBDZero.ini");
                } else {
                    iniFileNo++;
                    strFileInitial = "OBDZero" + decFix00.format(iniFileNo) + ".ini";
                    fileInitial = new File(fileDir, strFileInitial);
                }
            }
            okFileInitial = true;
            if (fileInitial.exists()) okFileInitial = fileInitial.delete();
            if (okFileInitial) {
                try {
                    FileOutputStream out = new FileOutputStream(fileInitial);
                    OutputStreamWriter osw = new OutputStreamWriter(out);
                    String textToWrite;
                    textToWrite = "Number of initial values;7;\r\n";
                    textToWrite += "True speed at 100 km/h;" + i_Spd100.str() + ";\r\n";
                    textToWrite += "Remaining km at charging station;" + i_Margin.str() + ";\r\n";
                    textToWrite += "Load in the car kg;" + i_Load.str() + ";\r\n";
                    textToWrite += "Range units;" + i_RangeUnits + ";\r\n";
                    textToWrite += "Odometer units;" + i_OdoUnits + ";\r\n";
                    textToWrite += "Record minimum sec;" + i_RecordTime.str() + ";\r\n";
                    if (deviceMacAddress != null) {
                        textToWrite += "Dongle in use;" + deviceMacAddress + ";\r\n";
                    } else {
                        textToWrite += "Dongle in use;none;\r\n";
                    }
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
                    okFileInitial = false;
                    upDateInfo("app:" + e);
                    upDateInfo("app:The " + strFileInitial + " file");
                    upDateInfo("app:could not be created.");
                    upDateInfo("app:The initial values were not saved.");
                }
            } else {
                upDateInfo("app:The " + strFileInitial + " file");
                upDateInfo("app:could not be deleted and recreated.");
                upDateInfo("app:The initial values were not saved.");
            }
        } else {
            okFileInitial = false;
            upDateInfo("app:The " + strFileDir + " directory");
            upDateInfo("app:was not available.");
            upDateInfo("app:The initial values were not saved.");
            upDateInfo("app:Try closing and reopening OBDZero.");
        }
        updateFrag(FRAG_INFO);
    }

    private void doInitialValues() {
        if (!runCollector) {
            Intent intent = new Intent(this, InitialValuesActivity.class);
            InitialValuesLauncher.launch(intent);
        } else {
            upDateInfo("app:Stop data before changing");
            upDateInfo("app:the initial values.");
            updateFrag(FRAG_INFO);
        }
    }

    private void showInitialValues() {
        upDateInfo("app:True speed at 100 km/h: " + i_Spd100.str());
        upDateInfo("app:Remaining km before charging: " + i_Margin.str());
        upDateInfo("app:Load in the car kg: " + i_Load.str());
        upDateInfo("app:Range units (dashboard): " + i_RangeUnits);
        upDateInfo("app:Odometer units (OBDscreen): " + i_OdoUnits);
        upDateInfo("app:Recording minimum seconds: " + i_RecordTime.str());
        updateFrag(FRAG_INFO);
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
        if (serviceSerial != null) serviceSerial.disConnect();
        updateFrag(FRAG_INFO);
    }

    /*
      This sends the command series "strReset" in Bluetooth serial service to the OBD device
     */
    private void doReset() {
        if (serviceSerial != null && serviceSerial.getState() == BluetoothSerialService.STATE_CONNECTED) {
            if (runComputing || iniComputing) stopComputing();
            if (runCollector) stopData();
            upDateInfo("app:Resetting please wait");
            previousTime = currentTimeMillis();
            cycleTime = 0;
            runReset = true;
            isReset = false;
            serviceSerial.startReset();
        } else {
            upDateInfo("app:Please connect to the dongle.");
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
            upDateInfo("app:OBD data collection started");
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
            upDateInfo("app:Please connect to the dongle");
        }
        updateFrag(FRAG_INFO);
    }

    private void processData() {
        stepDateTime = new Date();
        calcOBDs();
        if (iniComputing) iniComputations();
        if (runComputing) doComputations();
        if (iniRecording) iniStorage();
        if (runRecording && (recordTime > 1000L * i_RecordTime.in() || bmuTime == 0)) {
            recordTime = 0;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                String storedDateTime = strDateTime(stepDateTime);
                StorePIDs(storedDateTime);
                if (okOBDdata && m_newPIDs > 0) {
                    StorePIDIntegers(storedDateTime);
                    StoreOBD(storedDateTime);
                    if (cellsData) {
                        StoreCells(storedDateTime);
                        StoreCellTemperatures(storedDateTime);
                    }
                    if (runComputing) StoreCalc(storedDateTime);
                }
            }
        }
        long time_ms = currentTimeMillis() - previousTime;
        stepTime += time_ms;
        upDateInfo("app:Step " + time_ms + " ms.");
        stepTime = 0;
        cycleTime += time_ms;
        recordTime += cycleTime;
        bmuTime += cycleTime;
        upDateInfo("app:This cycle took " + cycleTime + " ms.");
        cycleTime = 0;
        updateFrag(fragNo);
        for (PID aPID : listPIDs) aPID.isNew = false;
        for (Cell aCell : listCells) aCell.isNew = false;
        for (CellSensor aSensor : listSensors) aSensor.isNew = false;
        allPIDs.clear();
        if (serviceSerial != null)
            if (bmuTime > 120000L) {
                bmuTime = 0;
                runBMU = true;
                serviceSerial.startBMU();
            } else {
                runBMU = false;
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
        upDateInfo("app:OBD data collection stopped");
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
        upDateInfo("app:Calc is waiting for data.");
        itemMenuStartStopComputing.setTitle(menu_stop_computing);
        c_Odo.dbl = 0;
        b_Volts.dbl = 0;
        c_SoC2.dbl = 0;
        iniComputing = true;
    }

    private void stopComputing() {
        itemMenuStartStopComputing.setTitle(menu_start_computing);
        iniComputing = false;
        runComputing = false;
        btnFour.setBackgroundColor(BLACK);
        upDateInfo("app:Calculations stopped");
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
        if (okFileDir) {
            if (!iniRecording && !runRecording) {
                itemMenuStartStopRecording.setTitle(menu_stop_recording);
                iniRecording = true;
                upDateInfo("app:Recording initialised");
            }
        } else {
            upDateInfo("app:OBDZero does not have");
            upDateInfo("app:storage permission");
            updateFrag(FRAG_INFO);
        }

    }

    private void stopRecording() {
        if (runRecording) exposeFiles();
        itemMenuStartStopRecording.setTitle(menu_start_recording);
        iniRecording = false;
        runRecording = false;
        btnFive.setBackgroundColor(BLACK);
        upDateInfo("app:Recording stopped");
        updateFrag(FRAG_INFO);
    }

    private void showAboutDialog() {
        dialogAbout = new Dialog(this);
        dialogAbout.setContentView(R.layout.about);

        String version = BuildConfig.VERSION_NAME;
        TextView name_version = dialogAbout.findViewById(R.id.app_version);
        name_version.setText("OBDZero Version " + version);

        Button buttonOpen = dialogAbout.findViewById(R.id.buttonDialog);
        buttonOpen.setOnClickListener(v -> dialogAbout.dismiss());
        dialogAbout.show();
    }

    private void readLine(String lineReceived) {
        if (lineReceived.contains(" ")) {
            String[] strSpace = lineReceived.split(" ");
            if (strSpace.length > 0) {
                if (strSpace[0].length() == 3 && strSpace.length < 10) {
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
                upDateInfo("app:Error converting Hex to Integer");
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
                    c_Acceleration.dbl = ((aPID.intr[0] * 256 + aPID.intr[1]) - 2000) / 400.0;
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
                    if (i_RangeUnits.equals("miles"))
                        c_RestRange.dbl = KmPerMile * aPID.intr[7];
                    else c_RestRange.dbl = aPID.intr[7];
                    break;
                case "373":
                    b_BatVmax.dbl = (aPID.intr[0] + 210) / 100.0;
                    b_BatVmin.dbl = (aPID.intr[1] + 210) / 100.0;
                    b_Amps.dbl = (aPID.intr[2] * 256 + aPID.intr[3] - 32768) / 100.0;
                    c_Amps.dbl = -b_Amps.dbl;
                    c_AmpsCal.dbl = -(b_Amps.dbl + 0.66);
                    if (aPID.intr[4] > 9)
                        b_Volts.dbl = (aPID.intr[4] * 256 + aPID.intr[5]) / 10.0;
                    b_Watts.dbl = c_AmpsCal.dbl * b_Volts.dbl;
                    break;
                case "374":
                    c_SoC1.dbl = (aPID.intr[0] - 10.0) / 2.0;
                    c_SoC2.dbl = (aPID.intr[1] - 10.0) / 2.0;
                    b_BatTmax.dbl = (aPID.intr[4] - 50.0);
                    b_BatTmin.dbl = (aPID.intr[5] - 50.0);
                    if (aPID.intr[6] > 0) c_CapAh.dbl = aPID.intr[6] / 2.0;
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
                    c_OdoShown.dbl = (aPID.intr[2] * 256 + aPID.intr[3]) * 256 + aPID.intr[4];
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
                        c_MotorA.dbl = (aPID.intr[2] * 256 + aPID.intr[3] - 500) / 20.0;
                    if (c_MotorA.dbl < 0) c_MotorA.dbl = 0;
                    if (aPID.intr[6] > 37 && aPID.intr[6] < 40) {
                        c_RegA.dbl = (aPID.intr[6] * 256 + aPID.intr[7] - 10000) / 5.0;
                    } else {
                        c_RegA.dbl = 0;
                    }
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
                        b_CapAh.dbl = (aPID.intr[3] * 256 + aPID.intr[4]) / 10.0;
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
        if (newPIDs == 0 && m_newPIDs == 0) {
            b_Amps.dbl = 0;
            c_Amps.dbl = 0;
            c_AmpsCal.dbl = 0;
        }
        m_newPIDs = newPIDs;

        upDateInfo("app:PIDs detected since start: " + foundPIDs);
        upDateInfo("app:PIDs updated in this cycle: " + newPIDs);

        calcBatSoC();
        calcCellNumber();
        cells88 = m_CellsNo == 88;

        int newCells = 0;
        int foundCells = 0;
        if (cellsData) {
            for (Cell aCell : listCells) {
                if (aCell.isFound) foundCells++;
                if (aCell.isNew) newCells++;
            }
            calcCellTemperatures();
            calcCellVMaxMin();
            calcCellSoC();
            if (foundCells == m_CellsNo) {
                btnThree.setBackgroundColor(clrDarkGreen);
            } else {
                btnThree.setBackgroundColor(BLACK);
            }
        } else {
            btnThree.setBackgroundColor(BLACK);
        }

        okOBDdata = c_Odo.dbl > 0 && b_Volts.dbl > 220 && c_SoC2.dbl > 0;

        upDateInfo("app:Cells detected since start: " + foundCells);
        upDateInfo("app:Cells updated in this cycle: " + newCells);
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
            upDateInfo("app:Error converting integer to binary");
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
                upDateInfo("app:Error computing the heat/cool position");
            }
        }

        if (aPID.str[2].length() == 2) {
            try {
                a_Dirc.dbl = Integer.parseInt("0" + aPID.str[2].charAt(0), 16);
                a_Fan.dbl = Integer.parseInt("0" + aPID.str[2].charAt(1), 16);
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "fan direction " + e);
                upDateInfo("app:Error computing the fan direction and speed");
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
        if (aPID.isNew) {
            int module = aPID.intr[0];
            switch (aPID.str[0]) {
                case "6E1":
                    int index = (module - 1) * 8;
                    listCells[index].module = module;
                    listCells[index].cell = 1;
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
                        listCells[index].volts = (aPID.intr[4] * 256 + aPID.intr[5] + 420) / 200.0;
                        listCells[index].isFound = true;
                        listCells[index].isNew = true;

                        index = (module - 1) * 8 + 7;
                        listCells[index].module = module;
                        listCells[index].cell = 8;
                        listCells[index].volts = (aPID.intr[6] * 256 + aPID.intr[7] + 420) / 200.0;
                        listCells[index].isFound = true;
                        listCells[index].isNew = true;

                    }
                    break;
            }
        }
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

    private void calcCellNumber() {
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
            b_BatVavg.dbl = b_Volts.dbl / m_CellsNo;
        }
    }

    private void calcCellTemperatures() {
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
        if (k > 40) b_Temp.dbl = sum / k;
        else b_Temp.dbl = (b_BatTmax.dbl + b_BatTmin.dbl) / 2.0;
    }

    private void calcCellVMaxMin() {
        b_CellVmax.volts = 0;
        b_CellVmin.volts = 5;
        b_CellVsum.dbl = 0;
        int k = 0;
        for (Cell aCell : listCells) {
            if (aCell.isFound) {
                if ((cells88 || aCell.module != 6) && (cells88 || aCell.module != 12)) {
                    if (aCell.volts > b_CellVmax.volts) {
                        b_CellVmax.volts = aCell.volts;
                        b_CellVmax.module = aCell.module;
                        b_CellVmax.cell = aCell.cell;
                        b_CellVmax.temperature = aCell.temperature;
                        b_CellVmax.isFound = true;
                        b_CellVmax.isNew = true;
                    } else if (aCell.volts < b_CellVmin.volts) {
                        b_CellVmin.volts = aCell.volts;
                        b_CellVmin.module = aCell.module;
                        b_CellVmin.cell = aCell.cell;
                        b_CellVmin.temperature = aCell.temperature;
                        b_CellVmin.isFound = true;
                        b_CellVmin.isNew = true;
                    }
                    b_CellVsum.dbl += aCell.volts;
                }
                k++;
            }
        }

        if (k < m_CellsNo) {
            b_CellVsum.dbl = b_Volts.dbl;
            b_CellVavg.dbl = (b_BatVmax.dbl + b_BatVmin.dbl) / 2.0;
        } else {
            b_CellVavg.dbl = b_CellVsum.dbl / m_CellsNo;
        }
    }

    private void calcCellSoC() {
        if (c_SoC2.dbl > 0) {
            for (Cell aCell : listCells) {
                if (aCell.isFound && aCell.SoC > 0) {
                    aCell.SoC = OCVmodel(aCell.SoC, aCell.volts, aCell.temperature);
                    if (b_CellVmax.module == aCell.module && b_CellVmax.cell == aCell.cell) {
                        b_CellVmax.SoC = aCell.SoC;
                    }
                    if (b_CellVmin.module == aCell.module && b_CellVmin.cell == aCell.cell) {
                        b_CellVmin.SoC = aCell.SoC;
                    }
                } else {
                    aCell.SoC = c_SoC2.dbl;
                }
            }
            double sum = 0;
            int i = 0;
            for (Cell aCell : listCells) {
                if (aCell.isFound && aCell.SoC > 0) {
                    sum += aCell.SoC;
                    i++;
                }
            }
            if (i > 0) m_SoCavg.dbl = sum / i;
        }
    }

    private void calcBatSoC() {
        b_BatSoCmax.dbl = OCVmodel(b_BatSoCmax.dbl, b_BatVmax.dbl, b_BatTmax.dbl);
        b_BatSoCmin.dbl = OCVmodel(b_BatSoCmin.dbl, b_BatVmin.dbl, b_BatTmin.dbl);
        b_BatSoCavg.dbl = OCVmodel(b_BatSoCavg.dbl, b_BatVavg.dbl, b_Temp.dbl);
    }

    private double OCVmodel(double SoC, double volts, double temperature) {
        //based on the model and the fit in OCData2020_05_25Analyse.xlsx
        double a = 0.696993346051809;
        double b = 0.898293282771827;
        double c = 0.682268134515503;
        double d = 0.00112956323443326;
        double e = 0.000635559666923096;
        double aP = 0.445338179227382;
        double bP = 4.28440915656331;
        double aN = 0.0832;
        double bN = 0.385;
        double xC = SoC / 100.0;
        double xP = b - a * xC;
        double xN = c * xC + d;
        double vModel = bP - aP * xP - aN * Math.pow(xN, -bN) + e * (temperature - 25);
        double errorV = volts - vModel;
        if (Math.abs(errorV) < 0.01) {
            SoC = 100 * xC * (1 + errorV);
        } else {
            if (errorV > 0) SoC = 100 * xC * 1.01;
            else SoC = 100 * xC * 0.99;
        }
        return SoC;
    }

    private void iniComputations() {
        if (okOBDdata) {
            m_Odo.dbl = c_Odo.dbl;
            p_Odo.dbl = c_Odo.dbl;
            p_SoC.dbl = c_SoC2.dbl;
            p_AmpsCal.dbl = c_AmpsCal.dbl;
            mp_AmpsCal.dbl = p_AmpsCal.dbl;
            m_AmpsAvg.dbl = c_AmpsCal.dbl;
            d_AhCal.dbl = 0;
            p_Speed.dbl = c_Speed0.dbl;

            m_CellAhmin.volts = b_BatVmin.dbl;
            m_CellAhmin.temperature = b_BatTmin.dbl;
            m_CellAhmin.SoC = c_SoC2.dbl;

            m_CellAhmax.volts = b_BatVmax.dbl;
            m_CellAhmax.temperature = b_BatTmax.dbl;
            m_CellAhmax.SoC = c_SoC2.dbl;

            c_AhRem.dbl = c_SoC2.dbl * c_CapAh.dbl / 100.0;
            b_AhRem.dbl = c_AhRem.dbl;
            m_AhRem.dbl = c_AhRem.dbl;
            c_WhRem.dbl = c_AhRem.dbl * (b_Volts.dbl + 320) / 2.0;
            b_WhRem.dbl = c_WhRem.dbl;
            m_WhRem.dbl = c_WhRem.dbl;
            t_WhReq.dbl = c_WhRem.dbl;

            c_CapWh.dbl = c_CapAh.dbl * b_Vavg;
            computeCarRR();

            c_RRtest.dbl = c_RR.dbl;
            b_RR.dbl = c_RR.dbl;
            m_RR.dbl = c_RR.dbl;

            b_Whkm.dbl = c_Whkm.dbl;
            m_Whkm.dbl = c_Whkm.dbl;

            computeAuxW();

            b_Wavg.dbl = c_Whkm.dbl * c_SpdAvg.dbl;
            m_Wavg.dbl = b_Wavg.dbl;
            b_WMovAvg.dbl = b_Wavg.dbl - m_AuxW.dbl;
            m_WMovAvg.dbl = b_Wavg.dbl - m_AuxW.dbl;
            b_WhkmAux.dbl = (b_WMovAvg.dbl + m_AuxW.dbl) / c_SpdAvg.dbl;
            m_WhkmAux.dbl = (m_WMovAvg.dbl + m_AuxW.dbl) / c_SpdAvg.dbl;

            c_SpdAvgRR.dbl = c_SpdAvg.dbl;
            b_WavgRR.dbl = b_WMovAvg.dbl;
            m_WavgRR.dbl = b_WMovAvg.dbl;

            e_N.dbl = m_Error.dbl * c_SpdAvg.dbl / 3.6;
            e_Watts.dbl = e_N.dbl * c_SpdAvg.dbl / 3.6;

            c_Mass.dbl = 1120 + i_Load.dbl;
            c_Roll.dbl = 9.89 * 0.018 * c_Mass.dbl;

            m_OCtimer.dbl = 0;

            m_CapStep = 0;
            m_CapSoCUsed.dbl = 100 - c_SoC2.dbl;
            if (m_CapSoCUsed.dbl < 0 || m_CapSoCUsed.dbl > 100) m_CapSoCUsed.dbl = 0;
            m_CapAhUsed.dbl = c_CapAh.dbl * (m_CapSoCUsed.dbl) / 100.0;

            iniComputing = false;
            runComputing = true;
            btnFour.setBackgroundColor(clrDarkGreen);
            upDateInfo("app:Calculations started");
        }
    }

    private void doComputations() {

        long time = currentTimeMillis();
        d_Second = (time - p_Time) / 1000.0; //time since the last computation in seconds
        d_Hour = d_Second / 3600.0; //time since the last computation in hours

        if (d_Second > 0.5) {

            computeAuxW();

            computeAccW();

            computeRegW();

            if (c_Speed0.dbl > 0 && !errorAC) {
                m_Watts.dbl = computeModel(c_Speed0.dbl, m_AccW.dbl, m_AuxW.dbl);
                if (d_Second < 9)
                    m_Error.dbl += 0.00002 * d_Second * (b_Watts.dbl - m_Watts.dbl); // The difference between the effect reported by the car and the effect computed by the model
                if (m_Error.dbl > 100) m_Error.dbl = 100;
                if (m_Error.dbl < -50) m_Error.dbl = -50;
                if (b_Volts.dbl > 0) m_AmpsCal.dbl = m_Watts.dbl / b_Volts.dbl;
            } else {
                m_Watts.dbl = b_Watts.dbl;
                m_AmpsCal.dbl = c_AmpsCal.dbl;
            }
            e_N.dbl = m_Error.dbl * c_SpdAvg.dbl / 3.6;
            e_Watts.dbl = e_N.dbl * c_SpdAvg.dbl / 3.6;

            computeSpeed();

            computeWhkm();

            computeWindSpeed();

            computeAh();

            computeWh();

            computeDistances();

            computeCarRR();

            computeRR();

            computeSuggestedSpeed();

            updateLowATimer();

            processCapacity();

            if (c_Gear.in() == 68 || c_Gear.in() == 131 || c_Gear.in() == 50) {
                p_Speed.dbl = c_Speed0.dbl;
            } else {
                p_Speed.dbl = 0;
            }
            p_Odo.dbl = c_Odo.dbl;
            p_SoC.dbl = c_SoC2.dbl;
            p_Time = time;

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
            ac_Watts.dbl = b_Watts.dbl - computeModel(c_Speed0.dbl, m_AccW.dbl, auxW);
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

    private void computeSpeed() {
        if (c_Gear.in() == 68 || c_Gear.in() == 131 || c_Gear.in() == 50 && d_Second < 10) {
            double aAdd = 0.004 * d_Second;
            double aKeep = 1 - aAdd;
            c_SpdAvg.dbl = aKeep * c_SpdAvg.dbl + aAdd * c_Speed0.dbl;
            if (c_SpdAvg.dbl < 1) c_SpdAvg.dbl = 1;
            c_SpdTrueAvg.dbl = c_SpdCor.dbl * c_SpdAvg.dbl;
        }
    }

    private void computeWhkm() {
        if (c_Gear.in() == 68 || c_Gear.in() == 131 || c_Gear.in() == 50 && d_Second < 10) {
            double aAdd = 0.004 * d_Second;
            double aKeep = 1 - aAdd;
            m_AccWavg.dbl = aKeep * m_AccWavg.dbl + aAdd * m_AccW.dbl; //compute the average watts (returned) used to (de)accelerate.
            b_Wavg.dbl = aKeep * b_Wavg.dbl + aAdd * b_Watts.dbl; //compute the average measured watts while in drive.
            m_Wavg.dbl = aKeep * m_Wavg.dbl + aAdd * m_Watts.dbl; //compute the average model watts while in drive.
            b_WMovAvg.dbl = aKeep * b_WMovAvg.dbl + aAdd * (b_Watts.dbl - m_AuxW.dbl); //compute the average model watts while in drive.
            m_WMovAvg.dbl = aKeep * m_WMovAvg.dbl + aAdd * (m_Watts.dbl - m_AuxW.dbl); //compute the average model watts while in drive.
            b_WAvgAux.dbl = b_WMovAvg.dbl + m_AuxW.dbl; //compute the average model watts while in drive.
            m_WAvgAux.dbl = m_WMovAvg.dbl + m_AuxW.dbl; //compute the average model watts while in drive.
            if (c_SpdAvg.dbl > 0) {
                b_Whkm.dbl = b_Wavg.dbl / c_SpdAvg.dbl;
                m_Whkm.dbl = m_Wavg.dbl / c_SpdAvg.dbl;
                b_WhkmAux.dbl = b_WAvgAux.dbl / c_SpdAvg.dbl;
                m_WhkmAux.dbl = m_WAvgAux.dbl / c_SpdAvg.dbl;
            }
        }
    }

    private void computeWindSpeed() {
        double m_v = c_SpdAvg.dbl / 3.6; //Convert to m/s.
        if (c_Drag.dbl > 0)
            m_Wind.dbl = Math.cbrt((m_Error.dbl * m_v * m_v / c_Drag.dbl + m_v * m_v * m_v)) - m_v;
    }

    private void computeAh() {
        if (c_SoC2.dbl > 0 && c_SoC2.dbl < 110) c_AhRem.dbl = c_SoC2.dbl * c_CapAh.dbl / 100.0;
        if (d_Second < 180) {
            d_AhCal.dbl = (c_AmpsCal.dbl + p_AmpsCal.dbl) * d_Hour / 2.0;
            b_AhRem.dbl -= d_AhCal.dbl;
            m_AhRem.dbl -= (m_AmpsCal.dbl + mp_AmpsCal.dbl) * d_Hour / 2.0;
        } else {
            d_AhCal.dbl = (c_AmpsCal.dbl + p_AmpsCal.dbl) * 180 / 3600.0 / 2.0;
            b_AhRem.dbl = c_AhRem.dbl;
            m_AhRem.dbl = c_AhRem.dbl;
        }
        p_AmpsCal.dbl = c_AmpsCal.dbl;
        mp_AmpsCal.dbl = m_AmpsCal.dbl;
        if (c_SoC2.dbl > 0) b_CapAhCheck.dbl = 100 * b_AhRem.dbl / c_SoC2.dbl;
    }

    private void computeWh() {
        double b_V0 = 0.117 * b_Temp.dbl - 0.177 * c_AmpsCal.dbl + 320.1;
        double c_VRem = (b_Volts.dbl + b_V0) / 2.0;
        c_WhRem.dbl = c_VRem * c_AhRem.dbl;
        b_WhRem.dbl = c_VRem * b_AhRem.dbl;
        m_WhRem.dbl = c_VRem * m_AhRem.dbl;
        double b_V100 = 0.117 * b_Temp.dbl - 0.177 * c_AmpsCal.dbl + 356.8;
        c_CapWh.dbl = c_CapAh.dbl * (b_V100 + b_V0) / 2.0;
    }

    private void computeDistances() {
        if (d_Second < 180) { // If there is less than 3 minutes since the last step then assume no data has been lost.
            double dx = (c_Speed0.dbl + p_Speed.dbl) * d_Hour / 2.0;
            m_Odo.dbl += dx;
            t_km.dbl -= c_SpdCor.dbl * dx; //True distance to the next charging station is reduced by the distance traveled during hours
            m_km.dbl += c_SpdCor.dbl * dx; //test distance since charging, adjusted by the error in the car speed
            if (i_OdoUnits.equals("km")) {
                c_kmTest.dbl += c_Odo.dbl - p_Odo.dbl;
                m_kmTest.dbl += dx;
                double test = 1;
                if (m_kmTest.dbl > 0) test = c_kmTest.dbl / m_kmTest.dbl;
                checkOdoUnits = test > 0.58 && test < 0.68;
            }
        } else {
            m_km.dbl += c_SpdCor.dbl * (c_Odo.dbl - m_Odo.dbl);
            t_km.dbl -= c_SpdCor.dbl * (c_Odo.dbl - m_Odo.dbl);
            m_Odo.dbl = c_Odo.dbl;
        }
        if (t_km.dbl < 0) t_km.dbl = 0;
    }

    private void computeCarRR() {
        //the cars rest range and Wh/km based on the reported rest range
        if (c_RestRange.dbl > 0 && c_RestRange.dbl < 255) {
            c_RR.dbl = c_RestRange.dbl;
            c_Whkm.dbl = (c_WhRem.dbl - 0.1 * c_CapWh.dbl) / c_RR.dbl;
        } else {
            if (c_Whkm.dbl > 0) c_RR.dbl = (c_WhRem.dbl - 0.1 * c_CapWh.dbl) / c_Whkm.dbl;
        }
    }

    private void computeRR() {
        if (c_Gear.in() == 68 || c_Gear.in() == 131 || c_Gear.in() == 50 && d_Second < 10) {
            double aAdd = 0.0005 * d_Second;
            double aKeep = 1 - aAdd;
            b_WavgRR.dbl = aKeep * b_WavgRR.dbl + aAdd * (b_Watts.dbl - m_AuxW.dbl); //compute the average measured watts while in drive.
            m_WavgRR.dbl = aKeep * m_WavgRR.dbl + aAdd * (m_Watts.dbl - m_AuxW.dbl); //compute the average model watts while in drive.
            c_SpdAvgRR.dbl = aKeep * c_SpdAvgRR.dbl + aAdd * c_Speed0.dbl;
            if (c_SpdAvgRR.dbl < 1) c_SpdAvgRR.dbl = 1;
            double bWhkm = (b_WavgRR.dbl + m_AuxW.dbl) / c_SpdAvgRR.dbl;
            double mWhkm = (m_WavgRR.dbl + m_AuxW.dbl) / c_SpdAvgRR.dbl;
            if (bWhkm > 0) {
                c_RRtest.dbl = (c_WhRem.dbl - 0.1 * c_CapWh.dbl) / bWhkm;
                b_RR.dbl = (b_WhRem.dbl - 0.1 * c_CapWh.dbl) / bWhkm;
            }
            if (mWhkm > 0) m_RR.dbl = (m_WhRem.dbl - 0.1 * c_CapWh.dbl) / mWhkm;
            if (i_RangeUnits.equals("km")) {
                double test = 1;
                if (c_RR.dbl > 0) test = c_RRtest.dbl / c_RR.dbl;
                checkRangeUnits = h_Level.in() == 7 && m_AuxW.dbl < 500 && test > 0.58 && test < 0.68;
            }
        }
    }

    private void computeSuggestedSpeed() {
        // The t model computes the required power in W at the suggested speed t_Speed.
        if (t_km.dbl > 0) t_RR.dbl = t_km.dbl + i_Margin.dbl;
        else t_RR.dbl = c_RR.dbl;
        c_Margin.dbl = c_RR.dbl - t_km.dbl; //RR minus the true distance to the next charging station
        if (c_Gear.in() == 68 || c_Gear.in() == 131 || c_Gear.in() == 50) {
            //Required range: true distance to the next station plus the required margin
            double[] speed = new double[3];
            double[] watts = new double[speed.length];
            double[] wh = new double[speed.length];
            double hours = 0;
            double test = 20000;
            int min = 0;
            for (int i = 0; i < speed.length; i++) {
                speed[i] = t_Speed.dbl + i - 1;
                watts[i] = computeModel(speed[i], 0, m_AuxW.dbl);
                //the time to the station + margin at true t_speed
                if (speed[i] > 0) hours = t_RR.dbl / (speed[i] * c_SpdCor.dbl);
                wh[i] = watts[i] * hours + 0.1 * c_CapWh.dbl;
                if (Math.abs(c_WhRem.dbl - wh[i]) < test) {
                    test = Math.abs(c_WhRem.dbl - wh[i]);
                    min = i;
                }
            }
            if (c_WhRem.dbl > wh[2] || speed[min] > t_Speed.dbl) {
                t_Speed.dbl += 0.00006 * d_Second * Math.abs(c_WhRem.dbl - wh[1]);
                if (t_Speed.dbl > 130) t_Speed.dbl = 130;
            } else {
                t_Speed.dbl -= 0.00006 * d_Second * Math.abs(c_WhRem.dbl - wh[1]);
                if (t_Speed.dbl < 25) t_Speed.dbl = 25;
            }
            //the time to the station + margin at true t_speed
            if (t_Speed.dbl > 0) hours = t_RR.dbl / (t_Speed.dbl * c_SpdCor.dbl);

            t_W.dbl = computeModel(t_Speed.dbl, 0, m_AuxW.dbl);
            t_WhReq.dbl = t_W.dbl * hours + 0.1 * c_CapWh.dbl;

            if (t_Speed.dbl > 0)
                t_Whkm.dbl = t_W.dbl / t_Speed.dbl;     //Wh/km when the indicated speed of the car is t_Speed
        }
    }

    private void updateLowATimer() {
        m_AmpsAvg.dbl = 0.8 * m_AmpsAvg.dbl + 0.2 * c_AmpsCal.dbl;
        if (m_AmpsAvg.dbl > -0.1 && m_AmpsAvg.dbl < 1) {
            if (d_Second < 10) m_OCtimer.dbl += d_Second / 60.0;
        } else {
            m_OCtimer.dbl = 0;
        }
    }

    private void updateCapacity() {
        m_Cap1Ahavg.dbl = 0;
        m_Cap1AhavgDisplay.dbl = 0;
        if (c_SoC2.dbl > 0 && c_SoC2.dbl < 100) m_CapSoCUsed.dbl = 100 - c_SoC2.dbl;
        else m_CapSoCUsed.dbl = 0;
        m_CapAhUsed.dbl = c_CapAh.dbl * m_CapSoCUsed.dbl / 100.0;

        if (cellsData) {
            m_CellAhmax.module = b_CellVmax.module;
            m_CellAhmax.cell = b_CellVmax.cell;
            m_CellAhmax.volts = b_CellVmax.volts;
            m_CellAhmax.temperature = b_CellVmax.temperature;
            m_CellAhmax.SoC = b_CellVmax.SoC;
            m_CellAhmax.p_SoC = b_CellVmax.SoC;
            m_CellAhmax.capAh1 = 0;
            m_CellAhmax.SoCsum = 0;
            m_CellAhmax.capAh2 = 0;
            m_CellAhmin.module = b_CellVmin.module;
            m_CellAhmin.cell = b_CellVmin.cell;
            m_CellAhmin.volts = b_CellVmin.volts;
            m_CellAhmin.temperature = b_CellVmin.temperature;
            m_CapTemp.dbl = b_CellVmin.temperature;
            m_CellAhmin.SoC = b_CellVmin.SoC;
            m_CellAhmin.p_SoC = b_CellVmin.SoC;
            m_CellAhmin.capAh1 = 0;
            m_CellAhmin.SoCsum = 0;
            m_CellAhmin.capAh2 = 0;
            m_CapAhsum.dbl = 0;
            for (Cell aCell : listCells) {
                aCell.capAh1 = 0;
                aCell.SoCsum = 0;
                aCell.p_SoC = aCell.SoC;
                aCell.capAh2 = 0;
            }
        }

        p_BatSoCmax.dbl = b_BatSoCmax.dbl;
        p_BatSoCmin.dbl = b_BatSoCmin.dbl;
        p_BatSoCavg.dbl = b_BatSoCavg.dbl;
        m_BatSummax.dbl = 0;
        m_BatSummin.dbl = 0;
        m_BatSumavg.dbl = 0;
    }

    private void computeCapacities1() {
        m_CapSoCUsed.dbl = 100 - c_SoC2.dbl;
        if (m_CapSoCUsed.dbl < 0 || m_CapSoCUsed.dbl > 100) m_CapSoCUsed.dbl = 0;
        m_CapAhUsed.dbl = c_CapAh.dbl * (m_CapSoCUsed.dbl) / 100.0;
        m_CapTemp.dbl = 0.99 * m_CapTemp.dbl + 0.01 * m_CellAhmin.temperature;
        if (100 - b_BatSoCmax.dbl > 0)
            m_BatAh1max.dbl = 100 * m_CapAhUsed.dbl / (100 - b_BatSoCmax.dbl);
        if (100 - b_BatSoCavg.dbl > 0)
            m_BatAh1avg.dbl = 100 * m_CapAhUsed.dbl / (100 - b_BatSoCavg.dbl);
        if (100 - b_BatSoCmin.dbl > 0)
            m_BatAh1min.dbl = 100 * m_CapAhUsed.dbl / (100 - b_BatSoCmin.dbl);
        if (cellsData) {
            if (100 - m_CellAhmax.SoC > 0)
                m_CellAhmax.capAh1 = 100 * m_CapAhUsed.dbl / (100 - m_CellAhmax.SoC);
            if (100 - m_SoCavg.dbl > 0)
                m_Cap1AhavgDisplay.dbl = 100 * m_CapAhUsed.dbl / (100 - m_SoCavg.dbl);
            if (100 - m_CellAhmin.SoC > 0)
                m_CellAhmin.capAh1 = 100 * m_CapAhUsed.dbl / (100 - m_CellAhmin.SoC);
        } else {
            m_Cap1AhavgDisplay.dbl = m_BatAh1avg.dbl;
        }
    }

    private void storeCapacities1() {
        if (cellsData) {
            for (Cell aCell : listCells) {
                if (100 - aCell.SoC > 0)
                    aCell.capAh1 = 100 * m_CapAhUsed.dbl / (100 - aCell.SoC);
            }
            m_Cap1Ahmax.dbl = m_CellAhmax.capAh1;
            if (100 - m_SoCavg.dbl > 0)
                m_Cap1Ahavg.dbl = 100 * m_CapAhUsed.dbl / (100 - m_SoCavg.dbl);
            m_Cap1Ahmin.dbl = m_CellAhmin.capAh1;
        }
    }

    private void updateSums2() {
        if (cellsData) {
            double sumSum = 0;
            int n = 0;
            for (Cell aCell : listCells) {
                aCell.SoCsum += aCell.SoC - aCell.p_SoC;
                aCell.p_SoC = aCell.SoC;
                if (aCell.SoCsum > 0) {
                    sumSum += aCell.SoCsum;
                    n += 1;
                }
                if (m_CellAhmax.module == aCell.module && m_CellAhmax.cell == aCell.cell) {
                    m_CellAhmax.volts = aCell.volts;
                    m_CellAhmax.temperature = aCell.temperature;
                    m_CellAhmax.SoC = aCell.SoC;
                    m_CellAhmax.SoCsum += m_CellAhmax.SoC - m_CellAhmax.p_SoC;
                    m_CellAhmax.p_SoC = m_CellAhmax.SoC;
                }
                if (m_CellAhmin.module == aCell.module && m_CellAhmin.cell == aCell.cell) {
                    m_CellAhmin.volts = aCell.volts;
                    m_CellAhmin.temperature = aCell.temperature;
                    m_CellAhmin.SoC = aCell.SoC;
                    m_CellAhmin.SoCsum += m_CellAhmin.SoC - m_CellAhmin.p_SoC;
                    m_CellAhmin.p_SoC = m_CellAhmin.SoC;
                }
            }
            if (n > 0) m_Cap2SoCsum.dbl = sumSum / n;
        }
        m_BatSummax.dbl += b_BatSoCmax.dbl - p_BatSoCmax.dbl;
        m_BatSumavg.dbl += b_BatSoCavg.dbl - p_BatSoCavg.dbl;
        m_BatSummin.dbl += b_BatSoCmin.dbl - p_BatSoCmin.dbl;
        p_BatSoCmax.dbl = b_BatSoCmax.dbl;
        p_BatSoCavg.dbl = b_BatSoCavg.dbl;
        p_BatSoCmin.dbl = b_BatSoCmin.dbl;

        m_CapAhsum.dbl -= d_AhCal.dbl;
        m_CapTemp.dbl = 0.99 * m_CapTemp.dbl + 0.01 * m_CellAhmin.temperature;
    }

    private void computeCapacities2() {
        if (m_BatSummax.dbl > 0 && m_CapAhsum.dbl > 0)
            m_BatAh2max.dbl = 100 * m_CapAhUsed.dbl / (m_BatSummax.dbl);
        if (m_BatSumavg.dbl > 0 && m_CapAhsum.dbl > 0)
            m_BatAh2avg.dbl = 100 * m_CapAhUsed.dbl / (m_BatSumavg.dbl);
        if (m_BatSummin.dbl > 0 && m_CapAhsum.dbl > 0)
            m_BatAh2min.dbl = 100 * m_CapAhUsed.dbl / (m_BatSummin.dbl);
        if (cellsData) {
            if (m_CellAhmax.SoCsum > 0 && m_CapAhsum.dbl > 0)
                m_CellAhmax.capAh2 = 100 * m_CapAhsum.dbl / m_CellAhmax.SoCsum;
            if (m_CellAhmin.SoCsum > 0 && m_CapAhsum.dbl > 0)
                m_CellAhmin.capAh2 = 100 * m_CapAhsum.dbl / m_CellAhmin.SoCsum;
            m_Cap2AhavgDisplay.dbl = (m_CellAhmax.capAh2 + m_CellAhmin.capAh2) / 2.0;
        } else {
            m_Cap2AhavgDisplay.dbl = m_BatAh2avg.dbl;
        }
    }

    private void storeCapacities2() {
        if (cellsData) {
            double sumAh = 0;
            int n = 0;
            for (Cell aCell : listCells) {
                if (aCell.SoCsum > 0 && m_CapAhsum.dbl > 0) {
                    aCell.capAh2 = 100 * m_CapAhsum.dbl / aCell.SoCsum;
                    sumAh += aCell.capAh2;
                    n += 1;
                }
            }
            if (n > 0) m_Cap2Ahavg.dbl = sumAh / n;
            m_Cap2AhavgDisplay.dbl = m_Cap2Ahavg.dbl;
            m_Cap2Ahmax.dbl = m_CellAhmax.capAh2;
            m_Cap2Ahmin.dbl = m_CellAhmin.capAh2;
        } else {
            m_Cap2AhavgDisplay.dbl = m_BatAh2avg.dbl;
        }
    }

    static String computeMinutesWithHeater() {
        if (c_SoC2.dbl > 15) return decFix0.format(0.6 * (c_SoC2.dbl - 15) * c_CapAh.dbl / 3);
        else return ("0");
    }

    private void processCapacity() {
        switch (m_CapStep) {
            case 0:
                updateCapacity();
                if (c_SoC1.dbl < 25 && c_SoC2.dbl < 25) m_CapStep = 1;
                break;
            case 1:
                updateCapacity();
                if (c_SoC1.dbl < 15 && c_SoC2.dbl < 15) {
                    m_OCtimer.dbl = 0;
                    m_CapStep = 2;
                }
                break;
            case 2:
                updateCapacity();
                if (m_OCtimer.dbl > 5) computeCapacities1();
                if (m_OCtimer.dbl > 30) {
                    storeCapacities1();
                    m_CapStep = 3;
                }
                break;
            case 3:
                updateSums2();
                if (c_AmpsCal.dbl < -1) m_CapStep = 4;
                break;
            case 4:
                updateSums2();
                if ((c_SoC1.dbl > 98 || c_SoC2.dbl > 98) && c_AmpsCal.dbl > -0.1) {
                    m_OCtimer.dbl = 0;
                    m_CapStep = 5;
                }
                break;
            case 5:
                updateSums2();
                if (m_OCtimer.dbl > 0) m_CapStep = 6;
                break;
            case 6:
                updateSums2();
                if (m_OCtimer.dbl > 5) computeCapacities2();
                if (m_OCtimer.dbl > 30) {
                    if (cellsData) for (Cell aCell : listCells) aCell.isFound = false;
                    m_CapCount = 0;
                    m_CapStep = 7;
                }
                break;
            case 7:
                updateSums2();
                computeCapacities2();
                if (m_newPIDs > 0) m_CapCount++;
                if (m_CapCount > 40) {
                    if (cellsData) {
                        int i = 0;
                        for (Cell aCell : listCells) {
                            if (aCell.isFound) i++;
                        }
                        if (i == m_CellsNo) {
                            storeCapacities2();
                            m_CapStep = 8;
                        }
                    } else {
                        storeCapacities2();
                        m_CapStep = 8;
                    }
                }
                break;
            case 8:
                break;
            default:
                m_CapStep = 0;
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

    public void selectOne() {
        if (fragNo == FRAG_INFO) {
            updateFrag(FRAG_INFO);
        } else {
            clrLines();
            lineOne.setBackgroundColor(Color.WHITE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_place, FragmentInfo.newInstance())
                    .commitNow();
            fragNo = FRAG_INFO;
        }
    }

    public void selectTwo() {
        if (leftTabs) {
            if (fragNo == FRAG_OBD) {
                updateFrag(FRAG_OBD);
            } else {
                clrLines();
                lineTwo.setBackgroundColor(Color.WHITE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_place, FragmentOBD.newInstance())
                        .commitNow();
                fragNo = FRAG_OBD;
            }
        } else {
            if (fragNo == FRAG_PID) {
                updateFrag(FRAG_PID);
            } else {
                clrLines();
                lineTwo.setBackgroundColor(Color.WHITE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_place, FragmentPIDs.newInstance())
                        .commitNow();
                fragNo = FRAG_PID;
            }
        }
    }

    public void selectThree() {
        if (leftTabs) {
            if (fragNo == FRAG_CELLS) {
                updateFrag(FRAG_CELLS);
            } else {
                clrLines();
                lineThree.setBackgroundColor(Color.WHITE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_place, FragmentCells.newInstance())
                        .commitNow();
                fragNo = FRAG_CELLS;
            }
        } else {
            if (fragNo == FRAG_CALC) {
                updateFrag(FRAG_CALC);
            } else {
                clrLines();
                lineThree.setBackgroundColor(Color.WHITE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_place, FragmentCalc.newInstance())
                        .commitNow();
                fragNo = FRAG_CALC;
            }
        }
    }

    public void selectFour() {
        if (leftTabs) {
            if (fragNo == FRAG_WATTS) {
                updateFrag(FRAG_WATTS);
            } else {
                clrLines();
                lineFour.setBackgroundColor(Color.WHITE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_place, FragmentWatts.newInstance())
                        .commitNow();
                fragNo = FRAG_WATTS;
            }
        } else {
            if (fragNo == FRAG_CAP1) {
                updateFrag(FRAG_CAP1);
            } else {
                clrLines();
                lineFour.setBackgroundColor(Color.WHITE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_place, FragmentCap1.newInstance())
                        .commitNow();
                fragNo = FRAG_CAP1;
            }
        }
    }

    public void selectFive() {
        if (leftTabs) {
            if (fragNo == FRAG_DRV) {
                updateFrag(FRAG_DRV);
            } else {
                clrLines();
                lineFive.setBackgroundColor(Color.WHITE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_place, FragmentDrive.newInstance())
                        .commitNow();
                fragNo = FRAG_DRV;
            }
        } else {
            if (fragNo == FRAG_CAP2) {
                updateFrag(FRAG_CAP2);
            } else {
                clrLines();
                lineFive.setBackgroundColor(Color.WHITE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_place, FragmentCap2.newInstance())
                        .commitNow();
                fragNo = FRAG_CAP2;
            }
        }
    }

    private void prepOBD() {
        arrayOBD.clear();
        arrayOBD.add("Time          " + displayDate.format(stepDateTime) + " " + displayTime.format(stepDateTime));
        arrayOBD.add("Odometer      " + c_Odo.strUnit() + " " + decFix0.format(c_Odo.dbl / KmPerMile) + " miles");
        arrayOBD.add("Speed         " + c_Speed0.strUnit());
        arrayOBD.add("Acc. Pedal    " + c_Pedal.strUnit());
        arrayOBD.add("Acceleration  " + c_Acceleration.strUnit());
        arrayOBD.add("Air sensor    " + c_AirSensor.strUnit());
        arrayOBD.add("Key           " + c_KeyOn.strOnOff());
        arrayOBD.add("Brake         " + c_BrakeOn.strOnOff() + " pressure " + c_Brake.str());
        arrayOBD.add("eStability    ");
        arrayOBD.add("  Steering    " + c_Steering.strUnit());
        arrayOBD.add("  Rotation    " + c_Rotation.strUnit());
        arrayOBD.add("  Wheel       ");
        arrayOBD.add("    speed 1   " + c_Speed1.strUnit());
        arrayOBD.add("    speed 2   " + c_Speed2.strUnit());
        arrayOBD.add("    speed 3   " + c_Speed3.strUnit());
        arrayOBD.add("    speed 4   " + c_Speed4.strUnit());

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

        arrayOBD.add("Motor         " + c_MotorA.strUnit() + " " + decFix0.format(c_MotorA.dbl * b_Volts.dbl) + " W " + c_RPM.strUnit());
        arrayOBD.add("Motor temps.  " + c_MotorTemp0.strUnit() + " " + c_MotorTemp1.strUnit() + " " + c_MotorTemp2.strUnit() + " " + c_MotorTemp3.strUnit());
        arrayOBD.add("Regeneration  " + c_RegA.strUnit() + " " + c_RegW.strUnit());
        arrayOBD.add("Battery");
        arrayOBD.add("  Voltage     " + b_Volts.strUnit());
        arrayOBD.add("  Current out " + c_Amps.strUnit() + " calib. " + c_AmpsCal.strUnit());
        arrayOBD.add("  Watts   out " + b_Watts.strUnit() + " calibrated");
        arrayOBD.add("  SoC         (1) " + c_SoC1.strUnit() + " (2) " + c_SoC2.strUnit());
        arrayOBD.add("  Capacity    " + c_CapAh.strUnit() + " @ 100% SoC");
        arrayOBD.add("  SoH         " + decFix0.format(100 * c_CapAh.dbl / 50.0) + " % of 50Ah");
        arrayOBD.add("  BMU Capa.   " + b_CapAh.strUnit() + " @ 100% SoC");
        arrayOBD.add("  SoH         " + decFix0.format(100 * b_CapAh.dbl / 48.0) + " % of 48Ah");

        arrayOBD.add("Cells");
        if (cellsData) {
            arrayOBD.add("  Voltage     max " + b_CellVmax.strVoltage(3) + " min " + b_CellVmin.strVoltage(3));
        } else {
            arrayOBD.add("  Voltage     max " + b_BatVmax.strUnit() + " min " + b_BatVmin.strUnit());
        }
        arrayOBD.add("  Temperature max " + b_BatTmax.strUnit() + " min " + b_BatTmin.strUnit());

        arrayOBD.add("Rest Range    " + c_RestRange.strUnit() + " " + decFix0.format(c_RestRange.dbl / KmPerMile) + " miles");
        arrayOBD.add("Heat/Cool     " + h_Level.str());
        arrayOBD.add("Heater        " + h_Amps.strUnit() + " " + h_Watts.strUnit());

        arrayOBD.add("AC            " + ac_On.strOnOff() + " " + ac_Amps.strUnit() + " " + ac_Watts.strUnit());

        arrayOBD.add("Recirculation " + a_Reci.strOnOff());

        if (a_Max.in() == 1) {
            arrayOBD.add("Fan           speed max" + " direction " + a_Dirc.str());
        } else {
            arrayOBD.add("Fan           speed " + a_Fan.str() + " direction " + a_Dirc.str());
        }

        arrayOBD.add("Charging");
        arrayOBD.add("  Battery DC  " + c_ChargeVDC.strUnit() + " " + c_ChargeADC.strUnit() + " " + decFix0.format(c_ChargeVDC.dbl * c_ChargeADC.dbl) + " W");
        arrayOBD.add("  Mains   AC  " + c_ChargeVAC.strUnit() + " " + c_ChargeAAC.strUnit() + " " + decFix0.format(c_ChargeVAC.dbl * c_ChargeAAC.dbl) + " W");
        arrayOBD.add("  Temperature " + c_ChargeTemp1.strUnit() + " " + c_ChargeTemp2.strUnit());

        if (c_QuickCharge.in() == 1)
            arrayOBD.add("Chademo       " + c_QCAmps.strUnit() + " " + decFix0.format(c_QCAmps.dbl * b_Volts.dbl) + " W " + c_QCprocent.strUnit());
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
        arrayOBD.add("Charge 12vBat " + c_12vAmps.strUnit() + " " + c_12vWatts.strUnit());
        arrayOBD.add("Model Year    " + c_Model.str());
        arrayOBD.add("VIN           " + strVIN[0] + strVIN[1] + strVIN[2]);
    }

    private void prepCalc() {
        arrayCalc.clear();
        arrayCalc.add(displayDate.format(stepDateTime));
        arrayCalc.add(displayTime.format(stepDateTime));
        arrayCalc.add(decFix2.format(d_Second) + " sec");

        arrayCalc.add(c_Odo.str());
        arrayCalc.add(m_Odo.str());
        arrayCalc.add(b_Watts.str());
        arrayCalc.add(m_Watts.str());
        arrayCalc.add(t_W.str());
        arrayCalc.add(c_WhRem.str());
        arrayCalc.add(b_WhRem.str());
        arrayCalc.add(m_WhRem.str());
        arrayCalc.add(t_WhReq.str());
        arrayCalc.add(c_Whkm.str());
        arrayCalc.add(b_Whkm.str());
        arrayCalc.add(m_Whkm.str());
        arrayCalc.add(t_Whkm.str());
        arrayCalc.add(c_RR.str());
        arrayCalc.add(b_RR.str());
        arrayCalc.add(m_RR.str());
        arrayCalc.add(t_RR.str());

        arrayCalc.add(m_AuxW.str());
        arrayCalc.add(e_N.str());
        arrayCalc.add(e_Watts.str());
        arrayCalc.add(m_Wind.str());

        arrayCalc.add(m_km.str());
        arrayCalc.add(t_km.str());
        arrayCalc.add(i_Margin.str());
        arrayCalc.add(t_RR.str());

        arrayCalc.add(c_Speed0.str());
        arrayCalc.add(c_SpdAvg.str());
        arrayCalc.add(c_SpdTrueAvg.str());
        arrayCalc.add(t_Speed.str());

        arrayCalc.add(c_CapAh.str());
        arrayCalc.add(c_SoC2.str());
        arrayCalc.add(c_AhRem.str());
        arrayCalc.add(b_AhRem.str());
        arrayCalc.add(b_CapAhCheck.str());
    }

    private void prepCap1() {
        arrayCalc.clear();
        arrayCalc.add(displayDate.format(stepDateTime));
        arrayCalc.add(displayTime.format(stepDateTime));
        arrayCalc.add(decFix2.format(d_Second) + " sec");
        arrayCalc.add(c_SoC1.str());
        arrayCalc.add(c_SoC2.str());
        arrayCalc.add(m_CapSoCUsed.str());
        arrayCalc.add(c_CapAh.str());
        arrayCalc.add(m_CapAhUsed.str());
        arrayCalc.add(m_AmpsAvg.str());
        arrayCalc.add(m_OCtimer.str());
        if (cellsData) {
            arrayCalc.add(m_CellAhmax.strModule());
            arrayCalc.add(m_CellAhmax.strCellLetter());
            arrayCalc.add(m_CellAhmax.strVoltage(3));
            arrayCalc.add(m_CellAhmax.strSoC());
            arrayCalc.add(m_CellAhmax.strAh1());
            arrayCalc.add(b_CellVavg.str());
            arrayCalc.add(m_SoCavg.str());
            arrayCalc.add(m_Cap1AhavgDisplay.str());
            arrayCalc.add(m_CellAhmin.strModule());
            arrayCalc.add(m_CellAhmin.strCellLetter());
            arrayCalc.add(m_CellAhmin.strVoltage(3));
            arrayCalc.add(m_CellAhmin.strSoC());
            arrayCalc.add(m_CellAhmin.strAh1());
        } else {
            arrayCalc.add("");
            arrayCalc.add("");
            arrayCalc.add(b_BatVmax.str());
            arrayCalc.add(b_BatSoCmax.str());
            arrayCalc.add(m_BatAh1max.str());
            arrayCalc.add(b_BatVavg.str());
            arrayCalc.add(b_BatSoCavg.str());
            arrayCalc.add(m_Cap1AhavgDisplay.str());
            arrayCalc.add("");
            arrayCalc.add("");
            arrayCalc.add(b_BatVmin.str());
            arrayCalc.add(b_BatSoCmin.str());
            arrayCalc.add(m_BatAh1min.str());
        }
    }

    private void prepCap2() {
        arrayCalc.clear();
        arrayCalc.add(displayDate.format(stepDateTime));
        arrayCalc.add(displayTime.format(stepDateTime));
        arrayCalc.add(decFix2.format(d_Second) + " sec");
        arrayCalc.add(c_SoC1.str());
        arrayCalc.add(c_SoC2.str());
        arrayCalc.add(c_CapAh.str());
        arrayCalc.add(b_Temp.str());
        arrayCalc.add(m_CapAhsum.str());
        arrayCalc.add(m_AmpsAvg.str());
        arrayCalc.add(m_OCtimer.str());
        if (cellsData) {
            arrayCalc.add(m_CellAhmax.strModule());
            arrayCalc.add(m_CellAhmax.strCellLetter());
            arrayCalc.add(m_CellAhmax.strVoltage(3));
            arrayCalc.add(m_CellAhmax.strSoCsum());
            arrayCalc.add(m_CellAhmax.strAh2());
            arrayCalc.add(b_CellVavg.str());
            arrayCalc.add(m_Cap2SoCsum.str());
            arrayCalc.add(m_Cap2AhavgDisplay.str());
            arrayCalc.add(m_CellAhmin.strModule());
            arrayCalc.add(m_CellAhmin.strCellLetter());
            arrayCalc.add(m_CellAhmin.strVoltage(3));
            arrayCalc.add(m_CellAhmin.strSoCsum());
            arrayCalc.add(m_CellAhmin.strAh2());
        } else {
            arrayCalc.add("");
            arrayCalc.add("");
            arrayCalc.add(b_BatVmax.str());
            arrayCalc.add(m_BatSummax.str());
            arrayCalc.add(m_BatAh2max.str());
            arrayCalc.add(b_BatVavg.str());
            arrayCalc.add(m_BatSumavg.str());
            arrayCalc.add(m_Cap2AhavgDisplay.str());
            arrayCalc.add("");
            arrayCalc.add("");
            arrayCalc.add(b_BatVmin.str());
            arrayCalc.add(m_BatSummin.str());
            arrayCalc.add(m_BatAh2min.str());
        }
    }

    private void upDateInfo(String info) {
        listInfo.add(info);
        String date = strDateTime(new Date());
        listStoreInfo.add(date + " " + info);
        int i = listStoreInfo.size();
        if (i > 12000) {
            listStoreInfo = new ArrayList<>(listStoreInfo.subList(i - 10000, i));
        }
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
                    FragmentCells.Refresh(listCells, cellsData,
                            b_CellVmax.volts, b_CellVmin.volts,
                            b_BatTmax.dbl, b_BatTmin.dbl,
                            m_Cap1Ahmax.dbl, m_Cap1Ahmin.dbl,
                            m_Cap2Ahmax.dbl, m_Cap2Ahmin.dbl);
                    break;
                case (FRAG_PID):
                    FragmentPIDs.Refresh(listPIDs);
                    break;
                case (FRAG_CALC):
                    prepCalc();
                    FragmentCalc.Refresh(arrayCalc);
                    break;
                case (FRAG_CAP1):
                    prepCap1();
                    FragmentCap1.Refresh(arrayCalc, m_CapStep);
                    break;
                case (FRAG_CAP2):
                    prepCap2();
                    FragmentCap2.Refresh(arrayCalc, m_CapStep);
                    break;
                case (FRAG_WATTS):
                    FragmentWatts.Refresh();
                    break;
                case (FRAG_DRV):
                    FragmentDrive.Refresh();
                    break;
                default:
                    break;
            }
        }
    }

    // The following methods get information back from the BluetoothService
    private final IStaticHandler handlerBT = new IStaticHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (DEBUG) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothSerialService.STATE_CONNECTED:
                            btnOne.setBackgroundColor(clrDarkGreen);
                            if (itemMenuConnect != null) itemMenuConnect.setTitle(menu_disConnect);
                            upDateInfo("app:Connected to " + connectedDeviceName);
                            updateFrag(FRAG_INFO);
                            if (runCollector) {
                                attemptNo = 0;
                                if (runRecording) StoreInfo();
                                if (serviceSerial != null) serviceSerial.startCollector();
                            } else if (runRestart) doRestart();
                            else finishOnStart();
                            break;

                        case BluetoothSerialService.STATE_CONNECTING:
                            if (connectedDevice != null)
                                upDateInfo("app:Connecting to " + connectedDeviceName);
                            else upDateInfo("app:Connecting to unknown device.");
                            updateFrag(FRAG_INFO);
                            break;

                        case BluetoothSerialService.STATE_FAILED:
                            runRestart = false;
                            btnOne.setBackgroundColor(BLACK);
                            if (itemMenuConnect != null) itemMenuConnect.setTitle(menu_connect);
                            if (connectedDevice != null) {
                                String atext = "app:Connection failed ";
                                if (runCollector) {
                                    stepTime += currentTimeMillis() - previousTime;
                                    cycleTime += currentTimeMillis() - previousTime;
                                    previousTime = currentTimeMillis();
                                    atext = atext + " @ " + cycleTime + " ms.";
                                } else {
                                    atext = atext + " to " + connectedDeviceName;
                                }
                                upDateInfo(atext);
                            } else upDateInfo("app:Failed to connect to unknown device.");
                            updateFrag(FRAG_INFO);
                            serviceSerial.disConnect();
                            break;

                        case BluetoothSerialService.STATE_LOST:
                            runRestart = false;
                            btnOne.setBackgroundColor(BLACK);
                            if (itemMenuConnect != null)
                                itemMenuConnect.setTitle(menu_connect);
                            if (connectedDevice != null) {
                                String atext = "app:" + connectedDeviceName + " connection lost";
                                if (runCollector) {
                                    stepTime += currentTimeMillis() - previousTime;
                                    cycleTime += currentTimeMillis() - previousTime;
                                    previousTime = currentTimeMillis();
                                    atext = atext + " @ " + cycleTime + " ms.";
                                    StoreInfo();
                                }
                                upDateInfo(atext);
                            } else upDateInfo("app:Connection to unknown device lost");
                            updateFrag(FRAG_INFO);
                            break;

                        case BluetoothSerialService.STATE_NONE:
                            runRestart = false;
                            btnOne.setBackgroundColor(BLACK);
                            if (itemMenuConnect != null)
                                itemMenuConnect.setTitle(menu_connect);
                            if (connectedDevice != null) {
                                String atext = "app:" + connectedDeviceName + " state to none";
                                if (runCollector) {
                                    stepTime += currentTimeMillis() - previousTime;
                                    cycleTime += currentTimeMillis() - previousTime;
                                    previousTime = currentTimeMillis();
                                    atext = atext + " @ " + cycleTime + " ms.";
                                }
                                upDateInfo(atext);
                            } else upDateInfo("app:Device connection state changed to none");
                            updateFrag(FRAG_INFO);
                            break;
                    }
                    break;

                case MESSAGE_RECEIVED:
                    String lineReceived = msg.getData().getString(RECEIVED_LINE);
                    long time_ms = currentTimeMillis() - previousTime;
                    stepTime += time_ms;
                    cycleTime += time_ms;
                    previousTime = currentTimeMillis();
                    if (lineReceived != null) {
                        upDateInfo("OBD:" + lineReceived);
                        switch (lineReceived) {
                            case "RESET OK":
                                runReset = false;
                                isReset = true;
                                upDateInfo("app:Reset took " + cycleTime + " ms.");
                                cycleTime = 0;
                                if (runRestart) doRestart();
                                else finishOnStart();
                                break;
                            case "RESET FAILED":
                                runReset = false;
                                isReset = false;
                                runRestart = false;
                                upDateInfo("app:Reset failed @ " + cycleTime + " ms.");
                                cycleTime = 0;
                                upDateInfo("app:Please reset again.");
                                updateFrag(FRAG_INFO);
                                break;
                            case "STEP":
                                upDateInfo("app:Step " + stepTime + " ms.");
                                stepTime = 0;
                                break;
                            case "PROCESS":
                                upDateInfo("app:Step " + stepTime + " ms.");
                                stepTime = 0;
                                processData();
                                break;
                            case "BMU OK":
                                upDateInfo("app:Step " + stepTime + " ms.");
                                stepTime = 0;
                                if (serviceSerial != null) serviceSerial.resetFlow();
                                break;
                            case "FLOW OK":
                                runBMU = false;
                                upDateInfo("app:Step " + stepTime + " ms.");
                                stepTime = 0;
                                processData();
                                break;
                            case "FLOW FAILED":
                                runBMU = false;
                                upDateInfo("app:Step " + stepTime + " ms.");
                                stepTime = 0;
                                updateFrag(FRAG_INFO);
                                StoreInfo();
                                processData();
                                break;
                            case "DATA ERROR":
                            case "EXCEPTION":
                                updateFrag(FRAG_INFO);
                                StoreInfo();
                                break;
                            default:
                                if (runCollector) readLine(lineReceived);
                                break;
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
            long time_OBD = 600;
            long time_ms = currentTimeMillis() - previousTime;
            if (serviceSerial != null) {
                switch (serviceSerial.getState()) {
                    case BluetoothSerialService.STATE_CONNECTED:
                        if (runReset) {
                            if (time_ms > 4000) {
                                runReset = false;
                                runRestart = false;
                                upDateInfo("app:Reset timeout @ " + (cycleTime + time_ms) + " ms.");
                                upDateInfo("app:Please reset again.");
                                serviceSerial.timeoutReset();
                                updateFrag(FRAG_INFO);
                            }
                        } else if (runCollector) {
                            if (runBMU) {
                                if (time_ms > 2 * time_OBD && time_ms <= 3 * time_OBD) {
                                    serviceSerial.resetFlow();
                                }
                            } else {
                                if (time_ms > time_OBD && time_ms <= 2 * time_OBD) {
                                    serviceSerial.stepCollector();
                                    upDateInfo("app:Data collector stepped @ " + time_ms + " ms.");
                                }
                            }
                            if (time_ms > 10 * time_OBD && time_ms <= 11 * time_OBD) {
                                upDateInfo("app:Data collector restarted @ " + time_ms + " ms.");
                                updateFrag(FRAG_INFO);
                                serviceSerial.startCollector();
                            } else if (time_ms > 12 * time_OBD) {
                                stepTime = 0;
                                serviceSerial.disConnect();
                            }
                        }
                        break;
                    case BluetoothSerialService.STATE_CONNECTING:
                        break;
                    case BluetoothSerialService.STATE_FAILED:
                    case BluetoothSerialService.STATE_LOST:
                    case BluetoothSerialService.STATE_NONE:
                        if (runCollector) {
                            stepTime += time_ms;
                            cycleTime += time_ms;
                            previousTime = currentTimeMillis();
                            if (stepTime > 64 * time_OBD) {
                                stepTime = 0;
                                if (attemptNo < attemptLast) {
                                    upDateInfo("app:Reconnecting @ " + cycleTime + " ms.");
                                    updateFrag(FRAG_INFO);
                                    attemptNo++;
                                    serviceSerial.connect(connectedDevice);
                                } else {
                                    upDateInfo("app:Restarting @ " + cycleTime + " ms.");
                                    updateFrag(FRAG_INFO);
                                    doRestart();
                                }
                            }
                        }
                        break;
                }
                handlerMonitor.postDelayed(monitorOBD, time_OBD);
            }
        }
    };

    private void iniStorage() {
        if (okFileDir) {
            boolean noExceptions = true;
            boolean Ok = true;
            String currentDateTime = fileDate.format(new Date());

            String file = "Info_" + currentDateTime + ".txt";
            fileInfo = new File(fileDir, file);
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
                upDateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "PID_" + currentDateTime + ".txt";
            filePIDs = new File(fileDir, file);
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
                upDateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "PIDInt_" + currentDateTime + ".txt";
            filePIDInt = new File(fileDir, file);
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
                upDateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "Cells_" + currentDateTime + ".txt";
            fileCells = new File(fileDir, file);
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
                upDateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "CellTemperatures_" + currentDateTime + ".txt";
            fileSensors = new File(fileDir, file);
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
                upDateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "OBD_" + currentDateTime + ".txt";
            fileOBD = new File(fileDir, file);
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
                upDateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            file = "Calc_" + currentDateTime + ".txt";
            fileCalc = new File(fileDir, file);
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
                upDateInfo("app:" + e);
                updateFrag(FRAG_INFO);
                noExceptions = false;
            }

            if (noExceptions && Ok) {
                upDateInfo("app:Recording started");
                if (!runCollector) updateFrag(FRAG_INFO);
                iniRecording = false;
                runRecording = true;
                btnFive.setBackgroundColor(clrDarkGreen);
                StoreInfo();
            } else {
                upDateInfo("app:Recording failed for reasons unknown.");
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
                            upDateInfo("app:Error storing data in background");

                        }
                    }
                }.execute();
            }
    }

    private void StorePIDs(String datetime) {
        if (filePIDs != null && filePIDs.exists()) {
            String[] str = new String[1000];
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
                        upDateInfo("app:Error storing data in background");
                    }
                }
            }.execute();
        }
    }

    private void StorePIDIntegers(String datetime) {
        if (filePIDInt != null && filePIDInt.exists()) {
            String[] str = new String[100];
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
                        upDateInfo("app:Error storing data in background");
                    }
                }
            }.execute();
        }
    }

    private void StoreCells(String datetime) {
        if (fileCells != null && fileCells.exists()) {
            String[] str = new String[120];
            str[0] = fileCells.toString();
            int i = 1;
            for (Cell aCell : listCells)
                if (aCell.isNew) {
                    if ((cells88 || aCell.module != 6) && (cells88 || aCell.module != 12)) {
                        str[i] = datetime +
                                ";" + aCell.strModule() +
                                ";" + aCell.strCellLetter() +
                                ";" + aCell.strVoltage(3) +
                                ";" + aCell.strTemperature() +
                                ";" + aCell.strSoC() +
                                ";" + aCell.strAh1() +
                                ";" + aCell.strSoCsum() +
                                ";" + aCell.strAh2() +
                                "\r\n";
                        i++;
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
                        if (DEBUG) Log.e(TAG, "StoreCells " + e);
                        upDateInfo("app:Error storing data in background");
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
                        upDateInfo("app:Error storing data in background");
                    }
                }
            }.execute();
        }
    }

    private void StoreOBD(String datetime) {
        if (fileOBD != null && fileOBD.exists()) {
            ArrayList<String> strArray = new ArrayList<>();
            strArray.add(fileOBD.toString());
            strArray.add(datetime + ";Odometer;" + c_Odo.str() + "\r\n");
            strArray.add(datetime + ";Speed;" + c_Speed0.str() + "\r\n");
            strArray.add(datetime + ";SpeedShown;" + c_SpdShown.str() + "\r\n");
            strArray.add(datetime + ";Speed1;" + c_Speed1.str() + "\r\n");
            strArray.add(datetime + ";Speed2;" + c_Speed2.str() + "\r\n");
            strArray.add(datetime + ";Speed3;" + c_Speed3.str() + "\r\n");
            strArray.add(datetime + ";Speed4;" + c_Speed4.str() + "\r\n");
            strArray.add(datetime + ";Acceleration;" + c_Acceleration.str() + "\r\n");
            strArray.add(datetime + ";AccPedal;" + c_Pedal.str() + "\r\n");
            strArray.add(datetime + ";KeyOn/Off;" + c_KeyOn.str() + "\r\n");
            strArray.add(datetime + ";Brake;" + c_Brake.str() + "\r\n");
            strArray.add(datetime + ";BrakeOn/Off;" + c_BrakeOn.str() + "\r\n");
            strArray.add(datetime + ";Steering;" + c_Steering.str() + "\r\n");
            strArray.add(datetime + ";Rotation;" + c_Rotation.str() + "\r\n");
            strArray.add(datetime + ";MotorRPM;" + c_RPM.str() + "\r\n");
            strArray.add(datetime + ";BatteryV;" + b_Volts.str() + "\r\n");
            strArray.add(datetime + ";BatteryA;" + b_Amps.str() + "\r\n");
            strArray.add(datetime + ";BatACalOut;" + c_AmpsCal.str() + "\r\n");
            strArray.add(datetime + ";BatWCalOut;" + b_Watts.str() + "\r\n");
            strArray.add(datetime + ";BatteryT;" + b_Temp.str() + "\r\n");
            strArray.add(datetime + ";BatCapAh;" + c_CapAh.str() + "\r\n");
            strArray.add(datetime + ";BMUCapAh;" + b_CapAh.str() + "\r\n");
            strArray.add(datetime + ";RestRange;" + c_RestRange.str() + "\r\n");
            strArray.add(datetime + ";RangeShown;" + c_RRshown.str() + "\r\n");
            strArray.add(datetime + ";SoC1;" + c_SoC1.str() + "\r\n");
            strArray.add(datetime + ";SoC2;" + c_SoC2.str() + "\r\n");
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
            strArray.add(datetime + ";BatSoCmax;" + b_BatSoCmax.str() + "\r\n");
            strArray.add(datetime + ";BatSoCavg;" + b_BatSoCavg.str() + "\r\n");
            strArray.add(datetime + ";BatSoCmin;" + b_BatSoCmin.str() + "\r\n");
            strArray.add(datetime + ";CellVmaxMod;" + b_CellVmax.strModule() + "\r\n");
            strArray.add(datetime + ";CellVmaxCell;" + b_CellVmax.strCell() + "\r\n");
            strArray.add(datetime + ";CellVmaxVolt;" + b_CellVmax.strVoltage(3) + "\r\n");
            strArray.add(datetime + ";CellVmaxTemp;" + b_CellVmax.strTemperature() + "\r\n");
            strArray.add(datetime + ";CellVminMod;" + b_CellVmin.strModule() + "\r\n");
            strArray.add(datetime + ";CellVminCell;" + b_CellVmin.strCell() + "\r\n");
            strArray.add(datetime + ";CellVminVolt;" + b_CellVmin.strVoltage(3) + "\r\n");
            strArray.add(datetime + ";CellVminTemp;" + b_CellVmin.strTemperature() + "\r\n");
            strArray.add(datetime + ";CellVsum;" + b_CellVsum.str() + "\r\n");
            strArray.add(datetime + ";ChargeVDC;" + c_ChargeVDC.str() + "\r\n");
            strArray.add(datetime + ";ChargeVAC;" + c_ChargeVAC.str() + "\r\n");
            strArray.add(datetime + ";ChargeADC;" + c_ChargeADC.str() + "\r\n");
            strArray.add(datetime + ";ChargeAAC;" + c_ChargeAAC.str() + "\r\n");
            strArray.add(datetime + ";ChargeTemp1;" + c_ChargeTemp1.str() + "\r\n");
            strArray.add(datetime + ";ChargeTemp2;" + c_ChargeTemp2.str() + "\r\n");
            strArray.add(datetime + ";PIDCount;" + m_newPIDs + "\r\n");
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
                        upDateInfo("app:Error storing data in background");
                    }
                }
            }.execute();
        }
    }

    private void StoreCalc(String datetime) {
        if (fileCalc != null && fileCalc.exists()) {
            ArrayList<String> strArray = new ArrayList<>();
            strArray.add(fileCalc.toString());
            strArray.add(datetime + ";B W;" + b_Watts.str() + "\r\n");
            strArray.add(datetime + ";B WAvg;" + b_Wavg.str() + "\r\n");
            strArray.add(datetime + ";M W;" + m_Watts.str() + "\r\n");
            strArray.add(datetime + ";M WAvg;" + m_Wavg.str() + "\r\n");
            strArray.add(datetime + ";T W;" + t_W.str() + "\r\n");
            strArray.add(datetime + ";C Ah;" + c_AhRem.str() + "\r\n");
            strArray.add(datetime + ";B Ah;" + b_AhRem.str() + "\r\n");
            strArray.add(datetime + ";M Ah;" + m_AhRem.str() + "\r\n");
            strArray.add(datetime + ";C Wh;" + c_WhRem.str() + "\r\n");
            strArray.add(datetime + ";B Wh;" + b_WhRem.str() + "\r\n");
            strArray.add(datetime + ";M Wh;" + m_WhRem.str() + "\r\n");
            strArray.add(datetime + ";T Wh;" + t_WhReq.str() + "\r\n");
            strArray.add(datetime + ";C Wh/km;" + c_Whkm.str() + "\r\n");
            strArray.add(datetime + ";B Wh/km;" + b_Whkm.str() + "\r\n");
            strArray.add(datetime + ";B Wh/kmAux;" + b_WhkmAux.str() + "\r\n");
            strArray.add(datetime + ";M Wh/km;" + m_Whkm.str() + "\r\n");
            strArray.add(datetime + ";M Wh/kmAux;" + m_WhkmAux.str() + "\r\n");
            strArray.add(datetime + ";T Wh/km;" + t_Whkm.str() + "\r\n");
            strArray.add(datetime + ";M Odometer;" + m_Odo.str() + "\r\n");
            strArray.add(datetime + ";C kmTest;" + c_kmTest.str() + "\r\n");
            strArray.add(datetime + ";M km;" + m_km.str() + "\r\n");
            strArray.add(datetime + ";M kmTest;" + m_kmTest.str() + "\r\n");
            strArray.add(datetime + ";T km;" + t_km.str() + "\r\n");
            strArray.add(datetime + ";C RR;" + c_RR.str() + "\r\n");
            strArray.add(datetime + ";C RRtest;" + c_RRtest.str() + "\r\n");
            strArray.add(datetime + ";B RR;" + b_RR.str() + "\r\n");
            strArray.add(datetime + ";M RR;" + m_RR.str() + "\r\n");
            strArray.add(datetime + ";T RR;" + t_RR.str() + "\r\n");
            strArray.add(datetime + ";M Wind;" + m_Wind.str() + "\r\n");
            strArray.add(datetime + ";M Aux;" + m_AuxW.str() + "\r\n");
            strArray.add(datetime + ";E N;" + e_N.str() + "\r\n");
            strArray.add(datetime + ";E W;" + e_Watts.str() + "\r\n");
            strArray.add(datetime + ";M ekg/s;" + m_Error.str() + "\r\n");
            strArray.add(datetime + ";M eN;" + e_N.str() + "\r\n");
            strArray.add(datetime + ";M eW;" + e_Watts.str() + "\r\n");
            strArray.add(datetime + ";C Margin;" + c_Margin.str() + "\r\n");
            strArray.add(datetime + ";T Margin;" + i_Margin.str() + "\r\n");
            strArray.add(datetime + ";T RRChg;" + c_Margin.str() + "\r\n");
            strArray.add(datetime + ";T Speed;" + t_Speed.str() + "\r\n");
            strArray.add(datetime + ";Avg Speed;" + c_SpdAvg.str() + "\r\n");
            strArray.add(datetime + ";C AvgSpeed;" + c_SpdAvg.str() + "\r\n");
            strArray.add(datetime + ";B AhDis;" + b_AhRem.str() + "\r\n");
            strArray.add(datetime + ";B AhChg;" + b_AhRem.str() + "\r\n");
            strArray.add(datetime + ";C SoCDis;" + c_SoC2.str() + "\r\n");
            strArray.add(datetime + ";C SoCChg;" + c_SoC2.str() + "\r\n");
            strArray.add(datetime + ";B CapDisAh;" + b_CapAhCheck.str() + "\r\n");
            strArray.add(datetime + ";B CapChgAh;" + b_CapAhCheck.str() + "\r\n");
            strArray.add(datetime + ";B CapAhChk;" + b_CapAhCheck.str() + "\r\n");
            strArray.add(datetime + ";C Load;" + i_Load.str() + "\r\n");
            strArray.add(datetime + ";C Roll;" + c_Roll.str() + "\r\n");
            strArray.add(datetime + ";C Drag;" + c_Drag.str() + "\r\n");
            strArray.add(datetime + ";B A;" + c_AmpsCal.str() + "\r\n");
            strArray.add(datetime + ";C RegW;" + c_RegW.str() + "\r\n");
            strArray.add(datetime + ";M Acc;" + m_AccW.str() + "\r\n");
            strArray.add(datetime + ";M AccAvg;" + m_AccWavg.str() + "\r\n");
            strArray.add(datetime + ";M SoC;" + m_SoCavg.str() + "\r\n");
            strArray.add(datetime + ";M SoCavg;" + m_SoCavg.str() + "\r\n");
            strArray.add(datetime + ";M LowAmins;" + m_OCtimer.str() + "\r\n");
            strArray.add(datetime + ";M CapSoCsum;" + m_CellAhmin.strSoCsum() + "\r\n");
            strArray.add(datetime + ";M CapAhsum;" + m_CapAhsum.str() + "\r\n");
            strArray.add(datetime + ";M CapAh;" + m_CellAhmin.strAh2() + "\r\n");
            strArray.add(datetime + ";M CapTemp;" + m_CapTemp.str() + "\r\n");
            strArray.add(datetime + ";M CModule;" + m_CellAhmin.strModule() + "\r\n");
            strArray.add(datetime + ";M CCell;" + m_CellAhmin.strCell() + "\r\n");
            strArray.add(datetime + ";M CVolts;" + m_CellAhmin.strVoltage(3) + "\r\n");
            strArray.add(datetime + ";M CTemp;" + m_CellAhmin.strTemperature() + "\r\n");
            strArray.add(datetime + ";M CSoC;" + m_CellAhmin.strSoC() + "\r\n");
            strArray.add(datetime + ";M Cap1SoCUsed;" + m_CapSoCUsed.str() + "\r\n");
            strArray.add(datetime + ";M Cap1AhUsed;" + m_CapAhUsed.str() + "\r\n");
            strArray.add(datetime + ";M Cap1Ahmax;" + m_Cap1Ahmax.str() + "\r\n");
            strArray.add(datetime + ";M Cap1Ahavg;" + m_Cap1Ahavg.str() + "\r\n");
            strArray.add(datetime + ";M Cap1Ahmin;" + m_Cap1Ahmin.str() + "\r\n");
            strArray.add(datetime + ";M Cap2Ahmax;" + m_Cap2Ahmax.str() + "\r\n");
            strArray.add(datetime + ";M Cap2Ahavg;" + m_Cap2Ahavg.str() + "\r\n");
            strArray.add(datetime + ";M Cap2Ahmin;" + m_Cap2Ahmin.str() + "\r\n");
            strArray.add(datetime + ";M BatAh1max;" + m_BatAh1max.str() + "\r\n");
            strArray.add(datetime + ";M BatAh1avg;" + m_BatAh1avg.str() + "\r\n");
            strArray.add(datetime + ";M BatAh1min;" + m_BatAh1min.str() + "\r\n");
            strArray.add(datetime + ";M BatSummax;" + m_BatSummax.str() + "\r\n");
            strArray.add(datetime + ";M BatSumavg;" + m_BatSumavg.str() + "\r\n");
            strArray.add(datetime + ";M BatSummin;" + m_BatSummin.str() + "\r\n");
            strArray.add(datetime + ";M BatAh2max;" + m_BatAh2max.str() + "\r\n");
            strArray.add(datetime + ";M BatAh2avg;" + m_BatAh2avg.str() + "\r\n");
            strArray.add(datetime + ";M BatAh2min;" + m_BatAh2min.str() + "\r\n");
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
                        upDateInfo("app:Error storing data in background");
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
