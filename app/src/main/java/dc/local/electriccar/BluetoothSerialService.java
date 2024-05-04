/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dc.local.electriccar;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


/**
 * This class does all the work of setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected. pymasde.es
 */
class BluetoothSerialService {

    // Debugging
    private static final String TAG = "BluetoothReadService";
    private static final boolean DEBUG = true;

    private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final Handler handlerMessage;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    static final int STATE_NONE = 0;       // we're doing nothing
    static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    static final int STATE_CONNECTED = 3;  // now connected to a remote device
    static final int STATE_LOST = 4; // now initiating an outgoing connection
    static final int STATE_FAILED = 5;  // now connected to a remote device

    //Control parameters for the data collection loop David Cecil
    private final byte[] lineBuffer = new byte[2048];
    private int lineIndex = 0;
    private int lineCount = 0;
    private static int tickNo = 0;
    private int commandNo = 0;
    private boolean runReset = false;
    private boolean runCollect = false;
    private boolean runBMU = false;
    private boolean resetFlow = false;

    //Reset commands to the OBD dongle David Cecil
    private final static String[] strReset = {
            "ATZ", "ATPP FF OFF", "ATPP 0E SV DA", "ATPP 0E ON", "ATWS", "ATE1", "ATSP6", "ATH1", "ATL0",
            "ATS1", "ATCAF0", "ATCM D00"};

    //PID filters used to control the data flow from the car via the OBD dongle David Cecil
    private final static String[] filterPIDs =
            {"000", "400", "100", "400", "500", "400", "500", "400", "100", "400", "000"};
    private static int filterNo = 0;

    //Request for data from the BMU David Cecil
    private final static String[] strBMURequest = {
            "ATWS", "ATE1", "ATSP6", "ATH1", "ATL0", "ATS1",
            "ATFCSH761", "ATFCSD300000", "ATFCSM1", "ATSH761", "2101"};

    //Return to normal operations David Cecil
    private final static String[] strResetFlow = {
            "ATFCSM0", "ATCAF0", "ATCM D00"};

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param handler A Handler to send messages back to the UI Activity
     */
    BluetoothSerialService(IStaticHandler handler) {
        mState = STATE_NONE;
        handlerMessage = IStaticHandlerFactory.create(handler);
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (DEBUG) Log.i(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        Message msg = handlerMessage.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE);
        Bundle bundle = new Bundle();
        // Give the new state to the Handler so the UI Activity can update
        //handlerMessage.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        bundle.putInt(MainActivity.STATE, mState);
        msg.setData(bundle);
        try {
            handlerMessage.sendMessage(msg);
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "send state change", e);
        }
    }

    /**
     * Return the current connection state.
     */
    synchronized int getState() {
        return mState;
    }

    @SuppressLint("MissingPermission")
    private void sendDeviceName(BluetoothDevice device) {
        // Send the name of the connecting device back to the UI Activity
        Message msg = handlerMessage.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        try {
            bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "device name failed", e);
            bundle.putString(MainActivity.DEVICE_NAME, "none");
        }
        msg.setData(bundle);
        try {
            handlerMessage.sendMessage(msg);
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "send device name", e);
        }
    }

    // This sends one line of received data to the main activity also called the UI David Cecil
    private void sendMessage(String strMessage) {
        Message msg = handlerMessage.obtainMessage(MainActivity.MESSAGE_RECEIVED);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.RECEIVED_LINE, strMessage);
        msg.setData(bundle);
        try {
            handlerMessage.sendMessage(msg);
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "send message", e);
        }
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    synchronized void start() {
        if (DEBUG) Log.i(TAG, "start");
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    synchronized void connect(BluetoothDevice device) {
        if (DEBUG) Log.i(TAG, "connect to: " + device);
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        sendDeviceName(device);
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (DEBUG) Log.i(TAG, "connected");
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        // Send the name of the connected device back to the UI Activity
        sendDeviceName(device);
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    synchronized void disconnect() {
        if (DEBUG) Log.i(TAG, "disconnect");
        stopCollector();
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        if (DEBUG) Log.i(TAG, "connection failed");
        setState(STATE_FAILED);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        if (DEBUG) Log.i(TAG, "connection lost");
        setState(STATE_LOST);
    }

    // Reset the OBD prior to data collection David Cecil
    void startReset() {
        runReset = true;
        runCollect = false;
        runBMU = false;
        resetFlow = false;
        lineCount = 0;
        commandNo = 0;
        writeOBD("ATDP");
    }

    void startBMU() {
        runReset = false;
        runCollect = false;
        runBMU = true;
        resetFlow = false;
        lineCount = 0;
        commandNo = 0;
        writeOBD("ATDP");
    }

    void resetFlow() {
        runReset = false;
        runCollect = false;
        runBMU = false;
        resetFlow = true;
        lineCount = 0;
        commandNo = 0;
        writeOBD("ATDP");
    }

    void timeoutReset() {
        runReset = false;
    }

    void wakeUp() {
        writeOBD("ATZ");
    }

    // This starts the loop that collects the PIDs one line at a time David Cecil
    void startCollector() {
        runReset = false;
        runCollect = true;
        runBMU = false;
        resetFlow = false;
        lineCount = 0;
        tickNo = 0;
        if (filterNo >= filterPIDs.length) filterNo = 0;
        writeOBD("ATDP");
    }

    // This advances the collection loop one step David Cecil
    void stepCollector() {
        writeOBD("ATDP");
    }

    // This stops the collection loop David Cecil
    void stopCollector() {
        runReset = false;
        runCollect = false;
        runBMU = false;
        resetFlow = false;
        writeSpaces();
    }

    // This adds the carriage return to a command and then sends it to the dongle. David Cecil
    private void writeOBD(String command) {
        command = "  " + command + "\r";
        write(command.getBytes());
    }

    // This stops the transmission of PIDs from the dongle to the app. David Cecil
    private void writeSpaces() {
        String command = "    ";
        write(command.getBytes());
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    private void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        try {
            r.write(out);
        } catch(Exception e){
            if (DEBUG) Log.e(TAG, "write to connected thread", e);
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    @SuppressLint("MissingPermission")
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "connect thread failed", e);
                sendMessage("Exception connect thread failed.");
            }
            mmSocket = tmp;
        }

        public void run() {
            if (DEBUG) Log.i(TAG, "run a connect thread");
            setName("ConnectThread");

            // Make a connection to the BluetoothSocket
            if (mmSocket != null) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an Exception
                    mmSocket.connect();
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "connection couldn't be made", e);
                    connectionFailed();
                    // Close the socket
                    try {
                        mmSocket.close();
                    } catch (Exception e2) {
                        if (DEBUG)
                            Log.e(TAG, "unable to close() socket during connection failure", e2);
                        sendMessage("Exception unable to close() socket during connection failure.");
                    }
                    return;
                }

                // Reset the ConnectThread because we're done
                synchronized (BluetoothSerialService.this) {
                    mConnectThread = null;
                }

                // Start the connected thread
                connected(mmSocket, mmDevice);
            } else {
                connectionFailed();
            }
        }

        private void cancel() {
            if (mmSocket != null) {
                try {
                    mmSocket.close();
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "close() of connect socket failed", e);
                    sendMessage("Exception close() of connect socket failed");
                }
            } else {
                if (DEBUG) Log.i(TAG, "no socket was found");
                sendMessage("no socket was found");
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        private ConnectedThread(BluetoothSocket socket) {
            if (DEBUG) Log.i(TAG, "create a connected thread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "temp sockets not created", e);
                sendMessage("Exception temp sockets not created");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            if (DEBUG) Log.i(TAG, "run the connected thread");
            int bytes;
            byte[] buffer = new byte[2048];

            /* Keep listening to the InputStream while connected
             * Convert each line to a string and send the string to the UI David Cecil
             * Start and stop PID flow, detect errors and direct the UI to process data David Cecil
             */
            if (mmInStream != null) {
                while (true) {
                    try {
                        bytes = mmInStream.read(buffer);
                    } catch (Exception e) {
                        if (DEBUG) Log.e(TAG, "disconnected", e);
                        connectionLost();
                        break;
                    }

                    if (bytes > 0) {
                        for (int i = 0; i < bytes; i++) {
                            lineBuffer[lineIndex] = buffer[i];
                            lineIndex++;
                            if (lineIndex == lineBuffer.length) buffer[i] = 0x0D;
                            switch (buffer[i]) {
                                case 0x0D:
                                    String lineReceived = new String(lineBuffer, 0, lineIndex - 1);
                                    lineIndex = 0;
                                    lineCount++;
                                    if (lineReceived.length() > 0) sendMessage(lineReceived);
                                    if (runCollect) {
                                        if (tickNo == 0 && lineCount % 20 == 2) writeSpaces();
                                    } else if (runBMU) {
                                        if (lineCount % 20 == 8) writeSpaces();
                                    }
                                    break;
                                case 0x3E:
                                    if (runReset) {
                                        if (commandNo < strReset.length) {
                                            writeOBD(strReset[commandNo]);
                                        } else {
                                            runReset = false;
                                            commandNo = 0;
                                            sendMessage("RESET OK");
                                        }
                                        commandNo++;
                                    } else if (runCollect) {
                                        switch (tickNo) {
                                            case 0:
                                                tickNo = 1;
                                                if (filterNo < filterPIDs.length) {
                                                    sendMessage("STEP");
                                                    writeOBD("ATCF " + filterPIDs[filterNo]);
                                                    filterNo++;
                                                } else {
                                                    filterNo = 0;
                                                    runCollect = false;
                                                    sendMessage("PROCESS");
                                                }
                                                break;
                                            case 1:
                                                tickNo = 0;
                                                lineCount = 0;
                                                writeOBD("ATMA");
                                                break;
                                            default:
                                                startCollector();
                                                break;
                                        }
                                    } else if (runBMU) {
                                        if (commandNo < strBMURequest.length) {
                                            writeOBD(strBMURequest[commandNo]);
                                            commandNo++;
                                            lineCount = 0;
                                        } else {
                                            runBMU = false;
                                            sendMessage("BMU OK");
                                        }
                                    } else if (resetFlow) {
                                        if (commandNo < strResetFlow.length) {
                                            writeOBD(strResetFlow[commandNo]);
                                            commandNo++;
                                        } else {
                                            resetFlow = false;
                                            sendMessage("FLOW OK");
                                        }
                                    }
                                    break;
                                case 0x3F:
                                    if (runReset) {
                                        runReset = false;
                                        sendMessage("RESET FAILED");
                                    } else if (runCollect) {
                                        if (tickNo == 0) {
                                            writeOBD("ATMA");
                                        } else if (tickNo == 1) {
                                            if (filterNo < filterPIDs.length)
                                                writeOBD("ATCF " + filterPIDs[filterNo]);
                                        } else {
                                            startCollector();
                                        }
                                    } else if (runBMU) {
                                        resetFlow();
                                    } else if (resetFlow) {
                                        resetFlow = false;
                                        sendMessage("FLOW FAILED");
                                    }
                                    break;
                            }
                        }
                    }
                }
            } else {
                connectionLost();
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        private void write(byte[] buffer) {
            if (mmOutStream != null)
                try {
                    mmOutStream.write(buffer);
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "Exception during write", e);
                    sendMessage("Exception during write.");
                }
        }

        private void cancel() {
            if (mmSocket != null)
                try {
                    mmSocket.close();
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "close() of connect socket failed", e);
                    sendMessage("Exception close() of connect socket failed.");
                }
        }
    }
}
