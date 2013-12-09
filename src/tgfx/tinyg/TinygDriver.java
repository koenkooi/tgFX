/*
 * Copyright (C) 2013 Synthetos LLC. All Rights reserved.
 * see license.txt for terms.
 */
package tgfx.tinyg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import org.json.JSONException;
import tgfx.ResponseParser;
import tgfx.SerialDriver;
import tgfx.SerialWriter;
import tgfx.hardwarePlatforms.HardwarePlatform;
import tgfx.hardwarePlatforms.HardwarePlatformManager;
import tgfx.system.Axis;
import tgfx.system.Machine;
import tgfx.system.Motor;

/**
 * The <code> TinygDriver</code> class
 * @author pfarrell
 * Created on Dec 8, 2013 7:50:53 PM
 */
public interface TinygDriver {
    public final static int MAX_BUFFER = 240;
    
    void addObserver(Observer obsrvr);

    void appendJsonQueue(String line);

    void appendResponseQueue(byte[] val);

    void applyHardwareAxisSettings(Tab _tab) throws Exception;

    void applyHardwareAxisSettings(Axis _axis, TextField tf) throws Exception;

    void applyHardwareMotorSettings(Motor _motor, TextField tf) throws Exception;

    void applyHardwareMotorSettings(Tab _tab) throws Exception;

    void applyResponseCommand(responseCommand rc);

    void disconnect();

    /**
     * @return the cmdManager
     */
    CommandManager getCmdManager();

    /**
     * @return the connectionStatus
     */
    SimpleBooleanProperty getConnectionStatus();

    /**
     * @return the connections
     */
    ArrayList<String> getConnections();

    /**
     * @return the hardwarePlatform
     */
    HardwarePlatform getHardwarePlatform();

    /**
     * @return the hardwarePlatformManager
     */
    HardwarePlatformManager getHardwarePlatformManager();

    List<Axis> getInternalAllAxis();

    /**
     * @return the machine
     */
    Machine getMachine();

    /**
     * @return the message
     */
    String[] getMessage();

    /**
     * @return the mneManager
     */
    MnemonicManager getMneManager();

    void getMotorSettings(int motorNumber);

    String getPortName();

    /**
     * @return the qr
     */
    QueueReport getQr();

    /**
     * @return the resManager
     */
    ResponseManager getResManager();

    /**
     * @return the resParse
     */
    ResponseParser getResParse();

    /**
     * @return the ser
     */
    SerialDriver getSer();

    /**
     * @return the serialWriter
     */
    SerialWriter getSerialWriter();

    boolean initialize(String portName, int dataRate);

    SimpleBooleanProperty isConnected();

    boolean isPAUSED();

    /**
     *
     *
     * Utility Methods
     *
     */
    String[] listSerialPorts();

    void notifyBuildChanged() throws IOException, JSONException;

    void priorityWrite(Byte b) throws Exception;

    void priorityWrite(String msg) throws Exception;

    void queryHardwareSingleAxisSettings(char c);

    void queryHardwareSingleAxisSettings(String _axis);

    void queryHardwareSingleMotorSettings(int motorNumber);

    /**
     * @param cmdManager the cmdManager to set
     */
    void setCmdManager(CommandManager cmdManager);

    /**
     * Connection Methods
     */
    void setConnected(boolean choice);

    /**
     * @param connectionStatus the connectionStatus to set
     */
    void setConnectionStatus(SimpleBooleanProperty connectionStatus);

    /**
     * @param connections the connections to set
     */
    void setConnections(ArrayList<String> connections);

    /**
     * @param hardwarePlatform the hardwarePlatform to set
     */
    void setHardwarePlatform(HardwarePlatform hardwarePlatform);

    /**
     * @param hardwarePlatformManager the hardwarePlatformManager to set
     */
    void setHardwarePlatformManager(HardwarePlatformManager hardwarePlatformManager);

    /**
     * @param machine the machine to set
     */
    void setMachine(Machine machine);

    /**
     * @param message the message to set
     */
    void setMessage(String[] message);

    /**
     * @param mneManager the mneManager to set
     */
    void setMneManager(MnemonicManager mneManager);

    void setPAUSED(boolean choice) throws Exception;

    /**
     * @param qr the qr to set
     */
    void setQr(QueueReport qr);

    /**
     * @param resManager the resManager to set
     */
    void setResManager(ResponseManager resManager);

    /**
     * @param ser the ser to set
     */
    void setSer(SerialDriver ser);

    /**
     * All Methods involving writing to TinyG.. This messages will call the
     * SerialDriver write methods from here.
     */
    void write(String msg) throws Exception;
    
}
