package dc.local.electriccar;

import java.text.DecimalFormat;

/**
 * Created by ocanal on 13-01-2012.
 */
class OBD {
    private final static DecimalFormat decFix0 = new DecimalFormat("##0");
    private final static DecimalFormat decFix1 = new DecimalFormat("##0.0");
    private final static DecimalFormat decFix2 = new DecimalFormat("##0.00");
    private final static DecimalFormat decFix3 = new DecimalFormat("##0.000");
    private final static DecimalFormat decFix4 = new DecimalFormat("##0.0000");


    double dbl;
    private final String unit;
    private final int dec;


    OBD(double b, String u, int d) {
        dbl = b;
        unit = u;
        dec = d;
    }

    int in() {
        return (int) Math.round(dbl);
    }

    String str() {
        switch (dec) {
            case 1:
                return decFix1.format(dbl);
            case 2:
                return decFix2.format(dbl);
            case 3:
                return decFix3.format(dbl);
            case 4:
                return decFix4.format(dbl);
            default:
                return decFix0.format(dbl);
        }
    }

    String unit() {
        if (unit.length() > 0) {
            if (unit.equals("oC")) {
                return str() + unit;
            } else {
                return str() + " " + unit;
            }
        } else {
            return str();
        }
    }

    String strOnOff() {
        if (in() == 0) {
            return "off";
        } else {
            return "on";
        }
    }
}
