package dc.local.electriccar;

public class OCV {
    static double model(double SoC, double volts, String chem, String calibration) {
    //based on the model and the fit in OCData2020_05_17Analyse.xlsx
    if (volts > 2.75 && volts < 4.2) {
        if (chem.equals("LEV")) {
            if (!(SoC > 0)) SoC = 20.779 * Math.pow((volts - 2.75), 5.3836);
            double a = 0.696993346051809;
            double b = 0.898293282771827;
            double c = 0.682268134515503;
            double d = 0.0176540718170829;
            double h = 0.00063556;
            double aP = 0.445338179227382;
            double bP = 4.28440915656331;
            double aN = 0.0832;
            double bN = 0.385;
            if (calibration.equals("new")) {
                //based on the model and the fit in ComputeSoCOCV.xlsx
                d = 0.00679833987304445;
                h = 0;
                aP = 0.36450458676377;
                bP = 4.25929966820228;
                aN = 0.08;
                bN = 0.539616037037016;
            }
            double xC = SoC / 100.0;
            double xP = b - a * xC;
            double xN = c * xC + d;
            double vModel = bP - aP * xP - aN * Math.pow(xN, -bN) - 10.0 * h;
            double errorV = volts - vModel;
            if (Math.abs(errorV) < 0.01)
                return 100 * xC * (1 + errorV);
            else if (errorV > 0) return 100 * xC * 1.01;
            else return 100 * xC * 0.99;
        } else {
            //based on piev's measurements of a NMC 93 cell 2022-10
            if (volts < 3.0) {
                return 4.082 * volts - 11.226;
            } else if (volts < 3.468) {
                return 33.497 * volts - 99.471;
            } else if (volts < 3.606) {
                return 132.143 * volts - 441.573;
            } else if (volts < 3.721) {
                return 183.199 * volts - 625.684;
            } else if (volts < 3.809) {
                return 89.213 * volts - 275.962;
            } else if (volts < 3.921) {
                return 131.098 * volts - 435.5;
            } else if (volts < 3.997) {
                return 100.031 * volts - 313.686;
            } else                                  //The SoC at 4.2v was computed using the slope and intercept for the line
                return 135.913 * volts - 457.106;   //from 3.997v to 4.099v. Therefore it wasn't necessary to include 4.099
        }
    } else return -1;
}

}
