package dc.local.electriccar;

class PID {
    String linePID = "";
    final String[] strPID = {"hx", "hx", "hx", "hx", "hx", "hx", "hx", "hx", "hx"};
    final int[] intPID = {-1, -1, -1, -1, -1, -1, -1, -1};
    int nBytes = 8;
    boolean isNew = false;
    boolean isFound = false;
}
