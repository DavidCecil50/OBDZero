package dc.local.electriccar;


import java.text.DecimalFormat;

class CellSensor {
    private final static DecimalFormat decFix0 = new DecimalFormat("##0");

    int module = 0;
    int sensor = 0;
    double temperature = -50;
    boolean isNew = false;

    String strTemperature() {
        return decFix0.format(temperature);
    }

}
