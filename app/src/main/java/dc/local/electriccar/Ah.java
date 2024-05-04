package dc.local.electriccar;

import static dc.local.electriccar.MainActivity.m_CellsNo;
import static dc.local.electriccar.MainActivity.i_Chem;

import java.text.DecimalFormat;

class Ah {

    private final static DecimalFormat decFix0 = new DecimalFormat("##0");
    private final static DecimalFormat decFix1 = new DecimalFormat("##0.0");

    double cap;
    double rem;
    double Whkm;
    double sum;

    Ah(double cAh, double rAh, double wAh, double sAh) {
        cap = cAh;
        rem = rAh;
        Whkm = wAh;
        sum = sAh;
    }

    double used() {
        return cap - rem;
    }

    double SoC() {
        if (cap > 0) return 100.0 * rem / cap;
        else return -1;
    }
    double capWh() {
        if (m_CellsNo == 80 | m_CellsNo == 88) {
            if (i_Chem.equals("NMC")) {
                return cap * m_CellsNo * (4.1 + 3.33) / 2;
            } else {
                return cap * m_CellsNo * (4.1 + 3.65) / 2;
            }
        } else {
            return cap * m_CellsNo * 3.7;
        }
    }
    double remWh() {
        if ((m_CellsNo == 80 | m_CellsNo == 88) && SoC() > -1) {
            double xC = SoC() / 100.0;
            if (i_Chem.equals("NMC")) {
                return rem * m_CellsNo * (0.77 * xC + 3.33 + 3.33) / 2;
            } else {
                return rem * m_CellsNo * (0.45 * xC + 3.65 + 3.65) / 2;
            }
        } else {
            return rem * m_CellsNo * 3.7;
        }
    }

    double remWh10() {
        if (remWh() > 0.1 * capWh()){
            return remWh() - 0.1 * capWh();
        } else {
            return 0;
        }
    }

    double RR() {
        if (Whkm > 0 && remWh10() > 0) {
            return remWh10() / Whkm;
        } else {
            return 0;
        }
    }

    String capStr() {
        try {
            return decFix1.format(cap);
        } catch (Exception e) {
            return "0";
        }
    }

    String remStr() {
        try {
            return decFix1.format(rem);
        } catch (Exception e) {
            return "0";
        }
    }

    String usedStr() {
        try {
            return decFix1.format(cap - rem);
        } catch (Exception e) {
            return "0";
        }
    }

    String WhkmStr() {
        try {
            return decFix0.format(Whkm);
        } catch (Exception e) {
            return "0";
        }
    }

    String sumStr() {
        try {
            return decFix1.format(sum);
        } catch (Exception e) {
            return "0";
        }
    }

    String SoCStr() {
        try {
            return decFix1.format(SoC());
        } catch (Exception e) {
            return "0";
        }
    }

    String SoEStr() {
        double SoE = -1;
        if (capWh() > 0) SoE = 100.0 * remWh() / capWh();
        try {
            return decFix1.format(SoE);
        } catch (Exception e) {
            return "-1";
        }
    }
    String capWhStr() {
        try {
            return decFix0.format(capWh());
        } catch (Exception e) {
            return "-1";
        }
    }

    String remWhStr() {
        try {
            return decFix0.format(remWh());
        } catch (Exception e) {
            return "-1";
        }
    }

    String remWh10Str() {
        try {
            return decFix0.format(remWh10());
        } catch (Exception e) {
            return "0";
        }
    }

    String RRStr() {
        try {
            return decFix1.format(RR());
        } catch (Exception e) {
            return "-1";
        }
    }

    String capUnit() {
        try {
            return decFix1.format(cap) + " Ah";
        } catch (Exception e) {
            return "0";
        }
    }

    String remUnit() {
        try {
            return decFix1.format(rem) + " Ah";
        } catch (Exception e) {
            return "0 Ah";
        }
    }

    String SoCUnit() {
        try {
            return decFix1.format(SoC()) + " %";
        } catch (Exception e) {
            return "0 %";
        }
    }

    String RRUnit() {
        try {
            return decFix1.format(RR()) + " km";
        } catch (Exception e) {
            return "-1 km";
        }
    }

}