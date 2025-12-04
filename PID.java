package dc.local.electriccar;

class PID {
    String linePID = "";
    final String[] str = {"hx", "hx", "hx", "hx", "hx", "hx", "hx", "hx", "hx"};
    final int[] intr = {-1, -1, -1, -1, -1, -1, -1, -1};
    int nBytes = 8;
    boolean isNew = false;
    boolean isFound = false;
}
