package dc.local.electriccar;

import java.text.DecimalFormat;

class Cell {

    private final static DecimalFormat decFix0 = new DecimalFormat("##0");
    private final static DecimalFormat decFix1 = new DecimalFormat("##0.0");
    private final static DecimalFormat decFix2 = new DecimalFormat("##0.00");
    private final static DecimalFormat decFix3 = new DecimalFormat("##0.000");
    private final static DecimalFormat decFix4 = new DecimalFormat("##0.0000");

    int module = 0;
    int cell = 0;
    double temperature = -50;
    double volts = 0;
    double SoC = 0;
    double p_SoC = 0;
    double SoCsum = 0;
    double capAh1 = 0;
    double capAh2 = 0;
    boolean isFound = false;
    boolean isNew = false;

    String strModule() {
        if (isFound) {
            return decFix0.format(module);
        } else {
            return "";
        }
    }

    String strCell() {
        if (isFound) {
            return decFix0.format(cell);
        } else {
            return "";
        }
    }

    String strTemperature() {
        if (isFound && temperature > -50) {
            return decFix1.format(temperature);
        } else {
            return "";
        }
    }

    String strVoltage(int dec) {
        if (isFound && volts > 0) {
            switch (dec) {
                case 1:
                    return decFix1.format(volts);
                case 2:
                    return decFix2.format(volts);
                case 3:
                    return decFix3.format(volts);
                case 4:
                    return decFix4.format(volts);
                default:
                    return decFix0.format(volts);
            }
        } else {
            return "";
        }
    }

    String strSoC() {
        if (isFound) {
            return decFix2.format(SoC);
        } else {
            return "";
        }
    }

    String strSoCsum() {
        if (isFound) {
            return decFix2.format(SoCsum);
        } else {
            return "";
        }
    }

    String strAh1() {
        if (isFound) {
            return decFix1.format(capAh1);
        } else {
            return "";
        }
    }

    String strAh2() {
        if (isFound) {
            return decFix1.format(capAh2);
        } else {
            return "";
        }
    }

    String strCellLetter() {
        if (isFound) {
            switch (cell) {
                case 1:
                    return "A";
                case 2:
                    return "B";
                case 3:
                    return "C";
                case 4:
                    return "D";
                case 5:
                    return "E";
                case 6:
                    return "F";
                case 7:
                    return "G";
                case 8:
                    return "H";
                default:
                    return "na";
            }
        } else {
            return "";
        }
    }
}
