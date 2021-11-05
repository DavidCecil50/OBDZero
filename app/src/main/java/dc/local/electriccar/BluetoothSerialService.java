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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;


/**
 * This class does all the work for setting up and managing Bluetooth
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
    private final BluetoothAdapter mAdapter;
    private final Handler handlerMessage;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private boolean mAllowInsecureConnections;


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

    //Reset commands to the OBD dongle David Cecil
    private final static String[] strReset = {
            "ATZ", "ATPP FF OFF", "ATPP 0E SV DA", "ATPP 0E ON", "ATWS", "ATE1", "ATSP6", "ATH1", "ATL0",
            "ATS1", "ATCAF0", "ATCM D00"};

    //PID filters used to control the data flow from the car via the OBD dongle David Cecil
    private final static String[] filterPIDs =
            {"000", "400", "100", "400", "500", "400", "500", "400", "100", "400", "000"};
    private static int filterNo = 0;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param handler A Handler to send messages back to the UI Activity
     */
    BluetoothSerialService(IStaticHandler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        handlerMessage = IStaticHandlerFactory.create(handler);
        mAllowInsecureConnections = false;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (DEBUG) Log.i(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        handlerMessage.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    synchronized int getState() {
        return mState;
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

        // Send the name of the connecting device back to the UI Activity
        Message msg = handlerMessage.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handlerMessage.sendMessage(msg);

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
        Message msg = handlerMessage.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handlerMessage.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    synchronized void disconnect() {
        if (DEBUG) Log.i(TAG, "stop");

        if (runCollect) stopCollector();

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    // Reset the OBD prior to data collection David Cecil
    void startReset() {
        commandNo = 1;
        runReset = true;
        writeOBD(strReset[0]);
    }

    void timeoutReset() {
        runReset = false;
    }

    // This starts loop that collects the PIDs one line at a time reply David Cecil
    void startCollector() {
        runCollect = true;
        lineCount = 0;
        filterNo = 0;
        tickNo = 0;
        writeOBD("  ATDP");
    }

    // This advances the collection loop one step David Cecil
    void stepCollector() {
        if (runCollect) {
            if (tickNo == 0) {
                writeSpaces();
            } else {
                startCollector();
            }
        }
    }

    // This stops the collection loop David Cecil
    void stopCollector() {
        runCollect = false;
        writeSpaces();
    }

    // This adds the carriage return to a command and then sends it to the dongle. David Cecil
    private void writeOBD(String command) {
        command = command + "\r";
        write(command.getBytes());
    }

    // This stops the transmission of PIDs from the dongle to the app. David Cecil
    private void writeSpaces() {
        String command = "    ";
        write(command.getBytes());
    }

    // This sends one line of received data to the main activity also called the UI David Cecil
    private void msgLine(String strMessage) {
        Message msg = handlerMessage.obtainMessage(MainActivity.MESSAGE_RECEIVED);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.RECEIVED_LINE, strMessage);
        msg.setData(bundle);
        handlerMessage.sendMessage(msg);
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
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        Log.i(TAG, "connection failed");
        if (runCollect) stopCollector();
        setState(STATE_FAILED);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        Log.i(TAG, "connection lost");
        if (runCollect) stopCollector();
        setState(STATE_LOST);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (mAllowInsecureConnections) {
                    Method method;

                    //noinspection JavaReflectionMemberAccess
                    method = device.getClass().getMethod("createRfcommSocket", int.class);
                    tmp = (BluetoothSocket) method.invoke(device, 1);
                } else {
                    tmp = device.createRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
                }
            } catch (IOException | NoSuchMethodException e) {
                Log.i(TAG, "create() failed", e);
            } catch (IllegalAccessException e) {
                Log.i(TAG, "create() failed", e);
            } catch (InvocationTargetException e) {
                Log.i(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            if (mmSocket != null) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmSocket.connect();
                } catch (IOException e) {
                    connectionFailed();
                    // Close the socket
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {
                        Log.i(TAG, "unable to close() socket during connection failure", e2);
                    }
                    // Start the service over to restart listening mode
                    //BluetoothSerialService.this.start();
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
                } catch (IOException e) {
                    Log.i(TAG, "close() of connect socket failed", e);
                }
            } else {
                Log.i(TAG, "no socket was found");
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
            Log.i(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.i(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            int bytes;
            byte[] buffer = new byte[2048];

            /* Keep listening to the InputStream while connected
             * convert each line to a string and send the string to the UI
             */
            if (mmInStream != null) {
                while (true) {
                    try {
                        bytes = mmInStream.read(buffer);
                    } catch (IOException e) {
                        Log.i(TAG, "disconnected", e);
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
                                    if (lineReceived.length() > 0) msgLine(lineReceived);
                                    if (runCollect) {
                                        if (tickNo == 0 && lineCount % 20 == 1)
                                            writeSpaces();
                                        lineCount++;
                                    }
                                    break;
                                case 0x3E:
                                    if (runReset) {
                                        if (commandNo < strReset.length) {
                                            writeOBD(strReset[commandNo]);
                                        } else {
                                            runReset = false;
                                            msgLine("RESET OK");
                                        }
                                        commandNo++;
                                    } else if (runCollect) {
                                        switch (tickNo) {
                                            case 0:
                                                tickNo = 1;
                                                if (filterNo < filterPIDs.length) {
                                                    msgLine("STEP");
                                                } else {
                                                    filterNo = 0;
                                                    msgLine("PROCESS");
                                                }
                                                writeOBD(" ATCF " + filterPIDs[filterNo]);
                                                break;
                                            case 1:
                                                tickNo = 0;
                                                filterNo++;
                                                lineCount = 0;
                                                writeOBD(" ATMA");
                                                break;
                                            default:
                                                startCollector();
                                        }
                                    }
                                    break;
                                case 0x3F:
                                    if (runReset) {
                                        runReset = false;
                                        msgLine("RESET FAILED");
                                    } else if (runCollect) {
                                        if (tickNo == 0) {
                                            writeOBD(" ATMA");
                                        } else if (tickNo == 1) {
                                            writeOBD(" ATCF " + filterPIDs[filterNo]);
                                        } else {
                                            startCollector();
                                        }
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
            } catch (IOException e) {
                Log.i(TAG, "Exception during write", e);
            }
        }

        private void cancel() {
            if (mmSocket != null)
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.i(TAG, "close() of connect socket failed", e);
            }
        }
    }

    void setAllowInsecureConnections(boolean allowInsecureConnections) {
        mAllowInsecureConnections = allowInsecureConnections;
    }
}
