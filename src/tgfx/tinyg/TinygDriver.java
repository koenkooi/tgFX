/**
 * tgFX Driver Class 
 * Copyright 2012-2013  Synthetos LLC
 * See license for terms.
 */
package tgfx.tinyg;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import tgfx.Main;
import tgfx.ResponseParser;
import tgfx.SerialDriver;
import tgfx.SerialWriter;
import tgfx.hardwarePlatforms.HardwarePlatform;
import tgfx.hardwarePlatforms.HardwarePlatformManager;
import tgfx.system.Axis;
import tgfx.system.Machine;
import tgfx.system.MachineFactory;
import tgfx.system.Motor;
import tgfx.ui.gcode.GcodeLine;

public class TinygDriver extends Observable {

    private double MINIMAL_BUILD_VERSIONS[] = {377.08, 13.01};
    static final Logger logger = Logger.getLogger(TinygDriver.class);
    private Machine machine = MachineFactory.getMachine();
    private QueueReport qr = QueueReport.getInstance();
    private MnemonicManager mneManager = new MnemonicManager();
    private ResponseManager resManager = new ResponseManager();
    private CommandManager cmdManager = new CommandManager();
    private String[] message = new String[2];
    private SimpleBooleanProperty connectionStatus = new SimpleBooleanProperty(false);
//    private String platformHardwareName = "";
    private HardwarePlatform hardwarePlatform = new HardwarePlatform();
    private HardwarePlatformManager hardwarePlatformManager = new HardwarePlatformManager();
    /**
     * Static commands for TinyG to get settings from the TinyG Driver Board
     */
    private ArrayList<String> connections = new ArrayList<>();
    private SerialDriver ser = SerialDriver.getInstance();
    private static ArrayBlockingQueue<String> jsonQueue = new ArrayBlockingQueue<>(10);
    private static ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(30);
    private static ArrayBlockingQueue<GcodeLine[]> writerQueue = new ArrayBlockingQueue<>(50000);
    private ResponseParser resParse = new ResponseParser(jsonQueue); // Our
    private SerialWriter serialWriter = new SerialWriter(writerQueue);
    private boolean PAUSED = false;
    public final static int MAX_BUFFER = 240;

    /**
     * Singleton Code for the Serial Port Object
     *
     * @return
     */
    public double[] getMINIMAL_BUILD_VERSIONS() {
        return MINIMAL_BUILD_VERSIONS;
    }

//    public voic addMin
//    
//    public void setMINIMAL_BUILD_VERSION(double MINIMAL_BUILD_VERSION) {
//        this.MINIMAL_BUILD_VERSION = MINIMAL_BUILD_VERSION;
//    }
    
    /**
     * @return the jsonQueue
     */
    public static ArrayBlockingQueue<String> getJsonQueue() {
        return jsonQueue;
    }

    /**
     * @param aJsonQueue the jsonQueue to set
     */
    public static void setJsonQueue(ArrayBlockingQueue<String> aJsonQueue) {
        jsonQueue = aJsonQueue;
    }

    /**
     * @return the queue
     */
    public static ArrayBlockingQueue<byte[]> getQueue() {
        return queue;
    }

    /**
     * @param aQueue the queue to set
     */
    public static void setQueue(ArrayBlockingQueue<byte[]> aQueue) {
        queue = aQueue;
    }

    /**
     * @return the writerQueue
     */
    public static ArrayBlockingQueue<GcodeLine[]> getWriterQueue() {
        return writerQueue;
    }
    
    public void notifyBuildChanged() throws IOException, JSONException {

//        int _size = this.getMINIMAL_BUILD_VERSIONS().length;
//        double _versions[] = this.getMINIMAL_BUILD_VERSIONS();
//
//
//        if (TinygDriver.getInstance().m.getFirmwareBuild() < 200 && TinygDriver.getInstance().m.getFirmwareBuild() > 0.0) {
//            //This is a bit of a hack at the moment.  If currently the Due port is no where near 200
//            //so this works.  However eventually?  This will break.
//            HardwarePlatform.getInstance().getPlatformByName("ArduinoDue");
//        }else{
//            HardwarePlatform.getInstance().getPlatformByName("TinyG");
//        }
        if(this.getHardwarePlatform().getMinimalBuildVersion() < getMachine().getFirmwareBuildValue()){
            //This checks to see if the current build version on TinyG is greater than what tgFX's hardware profile needs.
        
        }
        
       
        

        if (getMachine().getFirmwareBuildValue() < TinygDriver.getInstance().getHardwarePlatform().getMinimalBuildVersion() && 
                getMachine().getFirmwareBuildValue() != 0.0) {
            
            //too old of a build  we need to tell the GUI about this... This is where PUB/SUB will fix this 
            //bad way of alerting the gui about model changes.
            getMessage()[0] = "BUILD_ERROR";
            getMessage()[1] = Double.toString(getMachine().getFirmwareBuildValue());
            setChanged();
            notifyObservers(getMessage());
            logger.info("Build Version: " + getMachine().getFirmwareBuildValue() + " is NOT OK");
        } else {
            logger.info("Build Version: " + getMachine().getFirmwareBuild() + " is OK");
            getMessage()[0] = "BUILD_OK";
            getMessage()[1] = null;
            setChanged();
            notifyObservers(getMessage());
        }

    }

//    public String getPlatformHardwareName() {
//        return platformHardwareName;
//    }
//
//    public void setPlatformHardwareName(String platformHardwareName) {
//        this.platformHardwareName = platformHardwareName;
//    }
    public static TinygDriver getInstance() {
        return TinygDriverHolder.INSTANCE;
    }

    public void queryHardwareSingleAxisSettings(char c) {
        //Our queryHardwareSingleAxisSetting function for chars
        queryHardwareSingleAxisSettings(String.valueOf(c));
    }

    public void queryHardwareSingleAxisSettings(String _axis) {
        try {
            switch (_axis.toLowerCase()) {
                case "x":
                    getSerialWriter().write(CommandManager.CMD_QUERY_AXIS_X);
                    break;
                case "y":
                    getSer().write(CommandManager.CMD_QUERY_AXIS_Y);
                    break;
                case "z":
                    getSer().write(CommandManager.CMD_QUERY_AXIS_Z);
                    break;
                case "a":
                    getSer().write(CommandManager.CMD_QUERY_AXIS_A);
                    break;
                case "b":
                    getSer().write(CommandManager.CMD_QUERY_AXIS_B);
                    break;
                case "c":
                    getSer().write(CommandManager.CMD_QUERY_AXIS_C);
                    break;
            }
        } catch (Exception ex) {
            logger.error("[!]Error in queryHardwareSingleMotorSettings() ", ex);
        }
    }

    public void applyHardwareAxisSettings(Tab _tab) throws Exception {


        GridPane _gp = (GridPane) _tab.getContent();
        int size = _gp.getChildren().size();
        Axis _axis = this.getMachine().getAxisByName(String.valueOf(_gp.getId().charAt(0)));
        int i;
        for (i = 0; i < size; i++) {
            if (_gp.getChildren().get(i).getClass().toString().contains("TextField")) {
                //This ia a TextField... Lets get the value and apply it if it needs to be applied.
                TextField tf = (TextField) _gp.getChildren().get(i);
                applyHardwareAxisSettings(_axis, tf);

            } else if (_gp.getChildren().get(i) instanceof ChoiceBox) {
                //This ia a ChoiceBox... Lets get the value and apply it if it needs to be applied.
                @SuppressWarnings("unchecked")
                ChoiceBox<Object> cb = (ChoiceBox<Object>) _gp.getChildren().get(i);
                if (cb.getId().contains("AxisMode")) {
                    int axisMode = cb.getSelectionModel().getSelectedIndex();
                    String configObj = String.format("{\"%s%s\":%s}\n", _axis.getAxis_name().toLowerCase(), MnemonicManager.MNEMONIC_AXIS_AXIS_MODE, axisMode);
                    this.write(configObj);
                    continue;
                } else if (cb.getId().contains("switchModeMax")) {
                    int switchMode = cb.getSelectionModel().getSelectedIndex();
                    String configObj = String.format("{\"%s%s\":%s}\n", _axis.getAxis_name().toLowerCase(), MnemonicManager.MNEMONIC_AXIS_MAX_SWITCH_MODE, switchMode);
                    this.write(configObj);
                } else if (cb.getId().contains("switchModeMin")) {
                    int switchMode = cb.getSelectionModel().getSelectedIndex();
                    String configObj = String.format("{\"%s%s\":%s}\n", _axis.getAxis_name().toLowerCase(), MnemonicManager.MNEMONIC_AXIS_MIN_SWITCH_MODE, switchMode);
                    this.write(configObj);
                }
            }
        }


        Main.print("[+]Applying Axis Settings...");
    }

    public void applyHardwareMotorSettings(Motor _motor, TextField tf) throws Exception {
        if (tf.getId().contains("StepAngle")) {
            if (_motor.getStep_angle() != Float.valueOf(tf.getText())) {
                this.write("{\"" + _motor.getId_number() + MnemonicManager.MNEMONIC_MOTOR_STEP_ANGLE + "\":" + tf.getText() + "}\n");
            }
        } else if (tf.getId().contains("TravelPer")) {
            if (_motor.getStep_angle() != Float.valueOf(tf.getText())) {
                this.write("{\"" + _motor.getId_number() + MnemonicManager.MNEMONIC_MOTOR_TRAVEL_PER_REVOLUTION + "\":" + tf.getText() + "}\n");
            }
        }
    }

    public void applyHardwareAxisSettings(Axis _axis, TextField tf) throws Exception {
        /**
         * Apply Axis Settings to TinyG from GUI
         */
        if (tf.getId().contains("maxVelocity")) {
            if (_axis.getVelocityMaximum() != Double.valueOf(tf.getText())) {
                //We check to see if the value passed was already set in TinyG 
                //To avoid un-needed EEPROM Writes.
                this.write("{\"" + _axis.getAxis_name().toLowerCase() + MnemonicManager.MNEMONIC_AXIS_VELOCITY_MAXIMUM + "\":" + tf.getText() + "}\n");
            }
        } else if (tf.getId().contains("maxFeed")) {
            if (_axis.getFeed_rate_maximum() != Double.valueOf(tf.getText())) {
                //We check to see if the value passed was already set in TinyG 
                //To avoid un-needed EEPROM Writes.
                this.write("{\"" + _axis.getAxis_name().toLowerCase() + MnemonicManager.MNEMONIC_AXIS_FEEDRATE_MAXIMUM + "\":" + tf.getText() + "}\n");
            }
        } else if (tf.getId().contains("maxTravel")) {
            if (_axis.getTravel_maximum() != Double.valueOf(tf.getText())) {
                //We check to see if the value passed was already set in TinyG 
                //To avoid un-needed EEPROM Writes.
                this.write("{\"" + _axis.getAxis_name().toLowerCase() + MnemonicManager.MNEMONIC_AXIS_TRAVEL_MAXIMUM + "\":" + tf.getText() + "}\n");
            }
        } else if (tf.getId().contains("maxJerk")) {
            if (_axis.getJerkMaximum() != Double.valueOf(tf.getText())) {
                //We check to see if the value passed was already set in TinyG 
                //To avoid un-needed EEPROM Writes.
                this.write("{\"" + _axis.getAxis_name().toLowerCase() + MnemonicManager.MNEMONIC_AXIS_JERK_MAXIMUM + "\":" + tf.getText() + "}\n");
            }
        } else if (tf.getId().contains("junctionDeviation")) {
            if (Double.valueOf(_axis.getJunction_devation()).floatValue() != Double.valueOf(tf.getText())) {
                //We check to see if the value passed was already set in TinyG 
                //To avoid un-needed EEPROM Writes.
                this.write("{\"" + _axis.getAxis_name().toLowerCase() + MnemonicManager.MNEMONIC_AXIS_JUNCTION_DEVIATION + "\":" + tf.getText() + "}\n");
            }
        } else if (tf.getId().contains("radius")) {
            if (_axis.getAxisType().equals(Axis.AXIS_TYPE.ROTATIONAL)) {
                //Check to see if its a ROTATIONAL AXIS... 
                if (_axis.getRadius() != Double.valueOf(tf.getText())) {
                    //We check to see if the value passed was already set in TinyG 
                    //To avoid un-needed EEPROM Writes.
                    this.write("{\"" + _axis.getAxis_name().toLowerCase() + MnemonicManager.MNEMONIC_AXIS_RADIUS + "\":" + tf.getText() + "}\n");
                }
            }
        } else if (tf.getId().contains("searchVelocity")) {
            if (_axis.getSearch_velocity() != Double.valueOf(tf.getText())) {
                //We check to see if the value passed was already set in TinyG 
                //To avoid un-needed EEPROM Writes.
                this.write("{\"" + _axis.getAxis_name().toLowerCase() + MnemonicManager.MNEMONIC_AXIS_SEARCH_VELOCITY + "\":" + tf.getText() + "}\n");
            }
        } else if (tf.getId().contains("latchVelocity")) {
            if (_axis.getLatch_velocity() != Double.valueOf(tf.getText())) {
                //We check to see if the value passed was already set in TinyG 
                //To avoid un-needed EEPROM Writes.
                this.write("{\"" + _axis.getAxis_name().toLowerCase() + MnemonicManager.MNEMONIC_AXIS_LATCH_VELOCITY + "\":" + tf.getText() + "}\n");
            }
        } else if (tf.getId().contains("latchBackoff")) {
            if (_axis.getLatch_backoff() != Double.valueOf(tf.getText())) {
                //We check to see if the value passed was already set in TinyG 
                //To avoid un-needed EEPROM Writes.
                this.write("{\"" + _axis.getAxis_name().toLowerCase() + MnemonicManager.MNEMONIC_AXIS_LATCH_BACKOFF + "\":" + tf.getText() + "}\n");
            }
        } else if (tf.getId().contains("zeroBackoff")) {
            if (_axis.getZero_backoff() != Double.valueOf(tf.getText())) {
                //We check to see if the value passed was already set in TinyG 
                //To avoid un-needed EEPROM Writes.
                this.write("{\"" + _axis.getAxis_name().toLowerCase() + MnemonicManager.MNEMONIC_AXIS_ZERO_BACKOFF + "\":" + tf.getText() + "}\n");
            }
        }
        Main.print("[+]Applying " + _axis.getAxis_name() + " settings");

    }

    public void getMotorSettings(int motorNumber) {
        try {
            if (motorNumber == 1) {
                getSer().write(CommandManager.CMD_QUERY_MOTOR_1_SETTINGS);
            } else if (motorNumber == 2) {
                getSer().write(CommandManager.CMD_QUERY_MOTOR_2_SETTINGS);
            } else if (motorNumber == 3) {
                getSer().write(CommandManager.CMD_QUERY_MOTOR_3_SETTINGS);
            } else if (motorNumber == 4) {
                getSer().write(CommandManager.CMD_QUERY_MOTOR_4_SETTINGS);
            } else {
                logger.error("Invalid Motor Number.. Please try again..");
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    public void applyResponseCommand(responseCommand rc) {
        char _ax;
        switch (rc.getSettingKey()) {

            case (MnemonicManager.MNEMONIC_STATUS_REPORT_LINE):
                getMachine().setLineNumber(Integer.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_STATUS_REPORT_MOTION_MODE):
                logger.info("[DID NOT APPLY NEED TO CODE THIS IN:" + rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
//              machine.setMotionMode(Integer.valueOf(rc.getSettingValue()));
                break;

            case (MnemonicManager.MNEMONIC_STATUS_REPORT_POSA):
                _ax = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                getMachine().getAxisByName(String.valueOf(_ax)).setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_STATUS_REPORT_POSX):
                _ax = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                getMachine().getAxisByName(String.valueOf(_ax)).setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_STATUS_REPORT_POSY):
                _ax = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                getMachine().getAxisByName(String.valueOf(_ax)).setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_STATUS_REPORT_POSZ):
                _ax = rc.getSettingKey().charAt(rc.getSettingKey().length() - 1);
                getMachine().getAxisByName(String.valueOf(_ax)).setWorkPosition(Float.valueOf(rc.getSettingValue()));
                logger.info("[APPLIED:" + rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_STATUS_REPORT_STAT):
                logger.info("[APPLIED:" + rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            case (MnemonicManager.MNEMONIC_STATUS_REPORT_VELOCITY):
                getMachine().setVelocity(Double.valueOf(rc.getSettingValue()));
                TinygDriver.logger.info("[APPLIED:" + rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;

            default:
                logger.error("[ERROR] in ApplyResponseCommand:  Command Was:" + rc.getSettingParent() + " " + rc.getSettingKey() + ":" + rc.getSettingValue());
                break;
        }
    }

    public void applyHardwareMotorSettings(Tab _tab) throws Exception {
        /**
         * Apply Motor Settings to TinyG from GUI
         */
        Tab selectedTab = _tab.getTabPane().getSelectionModel().getSelectedItem();
        int _motorNumber = Integer.valueOf(selectedTab.getText().split(" ")[1].toString());
        Motor _motor = this.getMachine().getMotorByNumber(_motorNumber);

        GridPane _gp = (GridPane) _tab.getContent();
        int size = _gp.getChildren().size();
        int i;
        //Iterate though each gridpane child... Picking out text fields and choice boxes
        for (i = 0; i < size; i++) {

            if (_gp.getChildren().get(i).toString().contains("TextField")) {
                TextField tf = (TextField) _gp.getChildren().get(i);
                try {
                    applyHardwareMotorSettings(_motor, tf);
                } catch (Exception _ex) {
                    logger.error("[!]Exception in applyHardwareMotorSettings(Tab _tab)");
                }
            } else if (_gp.getChildren().get(i) instanceof ChoiceBox) {
                @SuppressWarnings("unchecked")
                ChoiceBox<Object> _cb = (ChoiceBox<Object>) _gp.getChildren().get(i);
                if (_cb.getId().contains("MapAxis")) {
                    int mapAxis;
                    switch (_cb.getSelectionModel().getSelectedItem().toString()) {
                        case "X":
                            mapAxis = 0;
                            break;
                        case "Y":
                            mapAxis = 1;
                            break;
                        case "Z":
                            mapAxis = 2;
                            break;
                        case "A":
                            mapAxis = 3;
                            break;
                        case "B":
                            mapAxis = 4;
                            break;
                        case "C":
                            mapAxis = 5;
                            break;
                        default:
                            mapAxis = 0;  //Defaults to map to X
                    }
                    String configObj = String.format("{\"%s\":{\"%s\":%s}}\n", _motorNumber, MnemonicManager.MNEMONIC_MOTOR_MAP_AXIS, mapAxis);
                    this.write(configObj);

                } else if (_cb.getId().contains("MicroStepping")) {
                    //This is the MapAxis Choice Box... Lets apply that
                    int microSteps;
                    switch (_cb.getSelectionModel().getSelectedIndex()) {
                        case 0:
                            microSteps = 1;
                            break;
                        case 1:
                            microSteps = 2;
                            break;
                        case 2:
                            microSteps = 4;
                            break;
                        case 3:
                            microSteps = 8;
                            break;
                        default:
                            microSteps = 1;
                    }
                    String configObj = String.format("{\"%s%s\":%s}\n", _motorNumber, MnemonicManager.MNEMONIC_MOTOR_MICROSTEPS, microSteps);
                    this.write(configObj);

                } else if (_cb.getId().contains("Polarity")) {
                    String configObj = String.format("{\"%s%s\":%s}\n", _motorNumber, MnemonicManager.MNEMONIC_MOTOR_POLARITY, _cb.getSelectionModel().getSelectedIndex());
                    this.write(configObj);

                } else if (_cb.getId().contains("PowerMode")) {
                    String configObj = String.format("{\"%s%s\":%s}\n", _motorNumber, MnemonicManager.MNEMONIC_MOTOR_POWER_MANAGEMENT, _cb.getSelectionModel().getSelectedIndex());
                    this.write(configObj);
                }
            }
        }
    }

    public void queryHardwareSingleMotorSettings(int motorNumber) {
        try {
            if (motorNumber == 1) {
                getSer().write(CommandManager.CMD_QUERY_MOTOR_1_SETTINGS);
            } else if (motorNumber == 2) {
                getSer().write(CommandManager.CMD_QUERY_MOTOR_2_SETTINGS);
            } else if (motorNumber == 3) {
                getSer().write(CommandManager.CMD_QUERY_MOTOR_3_SETTINGS);
            } else if (motorNumber == 4) {
                getSer().write(CommandManager.CMD_QUERY_MOTOR_4_SETTINGS);
            } else {
                Main.print("Invalid Motor Number.. Please try again..");
                setChanged();
            }
        } catch (Exception ex) {
            Main.print("[!]Error in queryHardwareSingleMotorSettings() " + ex.getMessage());


        }
    }

    private TinygDriver() {
    }

    /**
     * @return the machine
     */
    public Machine getMachine() {
        return machine;
    }

    /**
     * @param machine the machine to set
     */
    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    /**
     * @return the qr
     */
    public QueueReport getQr() {
        return qr;
    }

    /**
     * @param qr the qr to set
     */
    public void setQr(QueueReport qr) {
        this.qr = qr;
    }

    /**
     * @return the mneManager
     */
    public MnemonicManager getMneManager() {
        return mneManager;
    }

    /**
     * @param mneManager the mneManager to set
     */
    public void setMneManager(MnemonicManager mneManager) {
        this.mneManager = mneManager;
    }

    /**
     * @return the resManager
     */
    public ResponseManager getResManager() {
        return resManager;
    }

    /**
     * @param resManager the resManager to set
     */
    public void setResManager(ResponseManager resManager) {
        this.resManager = resManager;
    }

    /**
     * @return the cmdManager
     */
    public CommandManager getCmdManager() {
        return cmdManager;
    }

    /**
     * @param cmdManager the cmdManager to set
     */
    public void setCmdManager(CommandManager cmdManager) {
        this.cmdManager = cmdManager;
    }

    /**
     * @return the message
     */
    public String[] getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String[] message) {
        this.message = message;
    }

    /**
     * @return the connectionStatus
     */
    public SimpleBooleanProperty getConnectionStatus() {
        return connectionStatus;
    }

    /**
     * @param connectionStatus the connectionStatus to set
     */
    public void setConnectionStatus(SimpleBooleanProperty connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    /**
     * @return the hardwarePlatform
     */
    public HardwarePlatform getHardwarePlatform() {
        return hardwarePlatform;
    }

    /**
     * @param hardwarePlatform the hardwarePlatform to set
     */
    public void setHardwarePlatform(HardwarePlatform hardwarePlatform) {
        this.hardwarePlatform = hardwarePlatform;
    }

    /**
     * @return the hardwarePlatformManager
     */
    public HardwarePlatformManager getHardwarePlatformManager() {
        return hardwarePlatformManager;
    }

    /**
     * @param hardwarePlatformManager the hardwarePlatformManager to set
     */
    public void setHardwarePlatformManager(HardwarePlatformManager hardwarePlatformManager) {
        this.hardwarePlatformManager = hardwarePlatformManager;
    }

    /**
     * @return the connections
     */
    public ArrayList<String> getConnections() {
        return connections;
    }

    /**
     * @param connections the connections to set
     */
    public void setConnections(ArrayList<String> connections) {
        this.connections = connections;
    }

    /**
     * @return the ser
     */
    public SerialDriver getSer() {
        return ser;
    }

    /**
     * @param ser the ser to set
     */
    public void setSer(SerialDriver ser) {
        this.ser = ser;
    }

    /**
     * @return the resParse
     */
    public ResponseParser getResParse() {
        return resParse;
    }

    /**
     * @return the serialWriter
     */
    public SerialWriter getSerialWriter() {
        return serialWriter;
    }

    private static class TinygDriverHolder {
        private static final TinygDriver INSTANCE = new TinygDriver();
    }

    @Override
    public synchronized void addObserver(Observer obsrvr) {
        super.addObserver(obsrvr);
    }

    public void appendJsonQueue(String line) {
        // This adds full normalized json objects to our jsonQueue.
        TinygDriver.getJsonQueue().add(line);
    }

    public synchronized void appendResponseQueue(byte[] val) {
        // Add byte arrays to the buffer queue from tinyG's responses.
        try {
             queue.put((byte[]) val);
        } catch (Exception e) {
            Main.print("ERROR n shit");
            logger.error("error n shit");
        }
    }

    public boolean isPAUSED() {
        return PAUSED;
    }

    public void setPAUSED(boolean choice) throws Exception {
        PAUSED = choice;
        if (choice) { // if set to pause
            getSer().priorityWrite(CommandManager.CMD_APPLY_PAUSE);
        } else { // set to resume
            getSer().priorityWrite(CommandManager.CMD_QUERY_OK_PROMPT);
            getSer().priorityWrite(CommandManager.CMD_APPLY_RESUME);
            getSer().priorityWrite(CommandManager.CMD_QUERY_OK_PROMPT);
        }
    }

    /**
     * Connection Methods
     */
    public void setConnected(boolean choice) {
        this.getSer().setConnected(choice);
    }

    public boolean initialize(String portName, int dataRate) {
        return (this.getSer().initialize(portName, dataRate));
    }

    public void disconnect() {
        this.getSer().disconnect();
    }

    public SimpleBooleanProperty isConnected() {
        //Our binding to keep tabs in the us of if we are connected to TinyG or not.
        //This is mostly used to disable the UI if we are not connected.
        getConnectionStatus().set(this.getSer().isConnected());
        return (getConnectionStatus());
    }

    /**
     * All Methods involving writing to TinyG.. This messages will call the
     * SerialDriver write methods from here.
     */
    public synchronized void write(String msg) throws Exception {

        TinygDriver.getInstance().getSerialWriter().addCommandToBuffer(msg);
        logger.info("Send to Command Buffer >> " + msg);
    }

    public void priorityWrite(Byte b) throws Exception {
        this.getSer().priorityWrite(b);
        if(logger.getLevel() != Level.OFF){
            logger.info("+" + String.valueOf(b));
        }
    }

    public void priorityWrite(String msg) throws Exception {
        if (!msg.contains("\n")) {
            msg = msg + "\n";
        }
        getSer().write(msg);
        if(logger.getLevel() != Level.OFF){
            logger.info("+" + msg);
        }
        
    }

    /**
     *
     *
     * Utility Methods
     *
     */
    public String[] listSerialPorts() {
        // Get a listing current system serial ports
        String portArray[] = null;
        portArray = SerialDriver.listSerialPorts();
        return portArray;
    }

    public String getPortName() {
        // Return the serial port name that is connected.
        return getSer().serialPort.getName();
    }

    public List<Axis> getInternalAllAxis() {
        return getMachine().getAllAxis();
    }
}
