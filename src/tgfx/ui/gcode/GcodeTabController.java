/*
 * Copyright 2012-2013  Synthetos LLC
 * See license for terms.
 */
package tgfx.ui.gcode;

import com.google.common.base.Preconditions;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Duration;
import jfxtras.labs.scene.control.gauge.Lcd;
import org.apache.log4j.Logger;
import tgfx.render.CncMachinePreview;
import tgfx.render.Draw2d;
import tgfx.system.Machine;
import tgfx.tinyg.CommandManager;
import tgfx.tinyg.TinygDriver;
import tgfx.ui.tgfxsettings.TgfxSettingsController;

/**
 * FXML Controller class
 *
 * @author rileyporter
 */
public class GcodeTabController implements Initializable {

    private double scaleAmount;
    private AtomicBoolean taskActive = new AtomicBoolean(false);
    static final Logger logger = Logger.getLogger(GcodeTabController.class);
    public ObservableList data; //List to store the gcode file
    public static StackPane gcodePane = new StackPane(); //Holds CncMachinePreview  This needs to be before CncMachinePreview()
    private static CncMachinePreview machinePreview = new CncMachinePreview();
    private final EventHandler keyPress;
    private final EventHandler keyRelease;
    private String axis = new String();
    public static SimpleBooleanProperty isSendingFile = new SimpleBooleanProperty(false);  //This tracks to see if we are sending a file to tinyg.  This allows us to NOT try to jog while sending files
    private boolean isKeyPressed = false;
    private double jogDial = 0;
    private double FEED_RATE_PERCENTAGE = .05;  //%5
    private double TRAVERSE_FEED_RATE = 1;  //%100
    private double NUDGE_FEED_RATE = .05;  //%5
    private static int totalGcodeLines = 0;
    private static Date timeStartDt;
    private final Machine theMachine;
    private static Machine staticMachine;
    private final TinygDriver tinygDriver;
    /*  ######################## FXML ELEMENTS ############################*/
    @FXML
    private static Text timeElapsedTxt;
    @FXML
    private static Text timeLeftTxt;
    @FXML
    private Lcd xLcd, yLcd, zLcd, aLcd, velLcd; //DRO Lcds
    @FXML
    private Pane previewPane;
    @FXML
    StackPane machineWorkspace;
    @FXML
    private TableColumn<GcodeLine, String> gcodeCol;
    @FXML
    private static TableView gcodeView;
    @FXML
    private Text xAxisLocation, yAxisLocation;
    @FXML
    private static Text gcodeStatusMessage;  //Cursor location on the machinePreview Canvas
    @FXML
    private GridPane coordLocationGridPane;
    String cmd;
    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;
    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;
    @FXML // fx:id="zMoveScale"
    private ChoiceBox<?> zMoveScale; 
    @FXML
    private HBox gcodeTabControllerHBox;

    /**
     * Initializes the controller class.
     */
    public GcodeTabController(TinygDriver driver) {
        Preconditions.checkNotNull(driver);
        logger.setLevel(org.apache.log4j.Level.ERROR);
        logger.info("Gcode Controller Loaded");
        tinygDriver = driver;
        theMachine = tinygDriver.getMachine();
        staticMachine = theMachine;
        machinePreview.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                yAxisLocation.setText(machinePreview.getNormalizedYasString(me.getY()));
                xAxisLocation.setText(machinePreview.getNormalizedXasString(me.getX()));
            }
        });


        //TODO:  JOGGING NEEDS TO BE BROKEN INTO A NEW CLASS

        keyPress = new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (isSendingFile.get() == false) {  //If we are sending a file.. Do NOT jog right now
//                Main.postConsoleMessage("KEY PRESSED: " + keyEvent.getCode().toString());

                    //Do the jogging.
                    axis = " "; // Initialize to no valid axis set

                    if (!isKeyPressed) { //If this event has already sent a jog in need to pass this over.
                        KeyCode kc = keyEvent.getCode();
                        if (kc.equals(KeyCode.SHIFT)) {
                            return;   //This is going to toss out our initial SHIFT press for the z axis key combination.
                        }

                        if (keyEvent.isShiftDown()) {
                            //Alt is down so we make this into a Z movement
                            FEED_RATE_PERCENTAGE = TRAVERSE_FEED_RATE;
                        } else {
                            FEED_RATE_PERCENTAGE = NUDGE_FEED_RATE;
                        }

                        //Y Axis Jogging Movement
                        if (kc.equals(KeyCode.UP) || kc.equals(KeyCode.DOWN)) {
                            //This is and Y Axis Jog action
                            axis = "Y"; //Set the axis for this jog movment
                            if (keyEvent.getCode().equals(KeyCode.UP)) {
                                jogDial = theMachine.getJoggingIncrementByAxis(axis);
                            } else if (keyEvent.getCode().equals(KeyCode.DOWN)) {
                                jogDial = (-1 * theMachine.getJoggingIncrementByAxis(axis)); //Invert this value by multiplying by -1
                            }

                            //X Axis Jogging Movement
                        } else if (kc.equals(KeyCode.RIGHT) || kc.equals(KeyCode.LEFT)) {
                            //This is a X Axis Jog Action
                            axis = "X"; //Set the axis for this jog movment
                            if (keyEvent.getCode().equals(KeyCode.LEFT)) {
                                jogDial = (-1 * theMachine.getJoggingIncrementByAxis(axis));
                            } else if (keyEvent.getCode().equals(KeyCode.RIGHT)) {
                                jogDial = theMachine.getJoggingIncrementByAxis(axis); //Invert this value by multiplying by -1
                            }
                            //Z Axis Jogging Movement
                        } else if (kc.equals(KeyCode.MINUS) || (kc.equals(KeyCode.EQUALS))) {
                            axis = "Z";
                            if (keyEvent.getCode().equals(KeyCode.MINUS)) {
                                jogDial = (-1 * theMachine.getJoggingIncrementByAxis(axis));
                            } else if (keyEvent.getCode().equals(KeyCode.EQUALS)) {
                                jogDial = theMachine.getJoggingIncrementByAxis(axis); //Invert this value by multiplying by -1
                            }
                        }


                        try {
                            if (axis.equals("X") || axis.equals("Y") || axis.equals("Z")) {
                                // valid key pressed
                                CommandManager.setIncrementalMovementMode();
                                tinygDriver.write("{\"GC\":\"G1F" + (theMachine.getAxisByName(axis).getFeed_rate_maximum() * FEED_RATE_PERCENTAGE) + axis + jogDial + "\"}\n");
//                                tinygDriver.write("{\"GC\":\"G0" + axis + jogDial + "\"}\n");
                                isKeyPressed = true;
                            }

                        } catch (Exception ex) {
                            logger.error(ex);
                        }
                    }
                } //end if isSendingFile
                else {
                    //We are sending a file... We need to post a messages
                    setGcodeTextTemp("Jogging Disabled... Sending File.");
                }
            }
        };

        keyRelease = new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
//                Main.postConsoleMessage("Stopping Jog Action: " + keyEvent.getCode().toString());
                if (isSendingFile.get() == false) {
                    try {
                        setGcodeText("");
                        if (isKeyPressed) {  //We should find out of TinyG's distance mode is set to G90 before just firing this off.

                            CommandManager.stopJogMovement();
                            if (theMachine.getGcode_distance_mode().equals(theMachine.getGcode_distance_mode().INCREMENTAL)) {
                                //We are in incremental mode we now will enter ABSOLUTE mode
                                CommandManager.setAbsoluteMovementMode();
                            } //re-enable absolute mode
                            isKeyPressed = false; //reset the press flag
                        }
                    } catch (Exception ex) {
                        logger.error(ex);
                    }
                }
            }
        };

        machinePreview.setOnKeyPressed(keyPress);
        machinePreview.setOnKeyReleased(keyRelease);
    }

    public static void setGcodeTextTemp(String _text) {
        gcodeStatusMessage.setText(_text);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(3000), gcodeStatusMessage);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        fadeTransition.play();
//        gcodeStatusMessage.setText(""); //clear it out
    }

    public static void setGcodeText(String _text) {
        gcodeStatusMessage.setText(_text);
        gcodeStatusMessage.setVisible(true);
//        FadeTransition fadeTransition  = new FadeTransition(Duration.millis(1000), gcodeStatusMessage);
//                fadeTransition.setFromValue(0.0);
//                fadeTransition.setToValue(1.0);
//                fadeTransition.play();
    }
    public static void hideGcodeText() {
//        gcodeStatusMessage.setVisible(false);
//        FadeTransition fadeTransition  = new FadeTransition(Duration.millis(500), gcodeStatusMessage);
//                fadeTransition.setFromValue(1.0);
//                fadeTransition.setToValue(0.0);
//                fadeTransition.play();
    }

    public static void drawCanvasUpdate() {
        if (TgfxSettingsController.isDrawPreview()) {
            machinePreview.drawLine(staticMachine.getMotionMode().get(), staticMachine.getVelocityValue());
        }
    }
    private void drawTable() {
        //TODO  We need to make this a message to subscribe to.
        if (!gcodePane.getChildren().contains(machinePreview)) {
            gcodePane.getChildren().add(machinePreview); // Add the cnc machine to the gcode pane
        }
    }

    @FXML
    private void handleHomeXYZ(ActionEvent evt) {
        if (tinygDriver.isConnected().get()) {
            try {
                tinygDriver.write(CommandManager.CMD_APPLY_SYSTEM_HOME_XYZ_AXES);
            } catch (Exception ex) {
                logger.error("Erroring HomingXYZ Command");
            }
        }
    }

    @FXML
    private void handleHomeAxisClick(ActionEvent evt) {
        MenuItem m = (MenuItem) evt.getSource();
        String _axis = String.valueOf(m.getId().charAt(0));
        if (tinygDriver.isConnected().get()) {
            try {
                switch (_axis) {
                    case "x":
                        tinygDriver.write(CommandManager.CMD_APPLY_HOME_X_AXIS);
                        break;
                    case "y":
                        tinygDriver.write(CommandManager.CMD_APPLY_HOME_Y_AXIS);
                        break;
                    case "z":
                        tinygDriver.write(CommandManager.CMD_APPLY_HOME_Z_AXIS);
                        break;
                    case "a":
                        tinygDriver.write(CommandManager.CMD_APPLY_HOME_A_AXIS);
                        break;
                }
            } catch (Exception ex) {
                logger.error("Exception in handleHomeAxisClick for Axis: " + _axis + " " + ex.getMessage());
            }
        }
        tgfx.Main.postConsoleMessage("[+]Homing " + _axis.toUpperCase() + " Axis...\n");
    }

    @FXML
    private void handleZeroAxisClick(ActionEvent evt) {
        MenuItem m = (MenuItem) evt.getSource();
        String _axis = String.valueOf(m.getId().charAt(0));
        if (tinygDriver.isConnected().get()) {
            Draw2d.setFirstDraw(true);  //We set this so we do not draw lines for the previous position to the new zero.
            try {
                switch (_axis) {
                    case "x":
                        tinygDriver.write(CommandManager.CMD_APPLY_ZERO_X_AXIS);
                        break;
                    case "y":
                        tinygDriver.write(CommandManager.CMD_APPLY_ZERO_Y_AXIS);
                        break;
                    case "z":
                        tinygDriver.write(CommandManager.CMD_APPLY_ZERO_Z_AXIS);
                        break;
                    case "a":
                        tinygDriver.write(CommandManager.CMD_APPLY_ZERO_A_AXIS);
                        break;
                }
            } catch (Exception ex) {
                logger.error("Exception in handleZeroAxisClick for Axis: " + _axis + " " + ex.getMessage());
            }
        }
        tgfx.Main.postConsoleMessage("[+]Zeroed " + _axis.toUpperCase() + " Axis...\n");

    }

    @FXML
    private void handleDroMouseClick(MouseEvent me) {
        if (me.isSecondaryButtonDown()) { //Check to see if its a Right Click
            String t;
            String _axis;
            Lcd l;
            l = (Lcd) me.getSource();
            t = String.valueOf(l.idProperty().get().charAt(0));
        }
    }

    public static void setCNCMachineVisible(boolean t) {
        machinePreview.setVisible(t);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        /* add support for zmove
         * 
         */
//        assert zMoveScale != null : "fx:id=\"zMoveScale\" was not injected: check your FXML file 'Position.fxml'.";
//
//        // Set up ChoiceBox selection handler
//        zMoveScale.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
//            @Override
//            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number result) {
//                switch ((int) result) {
//                    case 0:
//                        zScale = 10.0f;
//                        break;
//                    case 1:
//                        zScale = 1.0f;
//                        break;
//                    case 2:
//                        zScale = 0.1f;
//                        break;
//                }
//            }
//        });

        timeStartDt = new Date();

        setCNCMachineVisible(false);  //We default to NOT display the CNC machine pane.  Once the serial port is connected we will show this.
        //This adds our CNC Machine (2d preview) to our display window
        if (!gcodePane.getChildren().contains(machinePreview)) {
            gcodePane.getChildren().add(machinePreview); // Add the cnc machine to the gcode pane
        }

        coordLocationGridPane.visibleProperty().bind(machinePreview.visibleProperty());  //This shows the coords when the machinePreview is visible.

        xLcd.valueProperty().bind(theMachine.getX().getMachinePositionSimple().subtract(theMachine.getX().getOffset()).divide(theMachine.getGcodeUnitDivision()));
        yLcd.valueProperty().bind(theMachine.getY().getMachinePositionSimple().subtract(theMachine.getY().getOffset()).divide(theMachine.getGcodeUnitDivision()));
        zLcd.valueProperty().bind(theMachine.getZ().getMachinePositionSimple().subtract(theMachine.getZ().getOffset()).divide(theMachine.getGcodeUnitDivision()));
        aLcd.valueProperty().bind(theMachine.getA().getMachinePositionSimple().subtract(theMachine.getA().getOffset()));
        velLcd.valueProperty().bind(theMachine.getVelocity());


        /*######################################
         * BINDINGS CODE
         ######################################*/
        gcodeTabControllerHBox.disableProperty().bind(tinygDriver.getConnectionStatus().not());


        /*######################################
         * CHANGE LISTENERS
         ######################################*/

        xLcd.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue ov, Object oldValue, Object newValue) {
                double tmp = theMachine.getY().getWorkPosition().doubleValue() + 5;
            }
        });


        yLcd.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue ov, Object oldValue, Object newValue) {
                double tmp = theMachine.getY().getWorkPosition().doubleValue() + 5;
            }
        });

        theMachine.getGcodeUnitMode().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue ov, Object oldValue, Object newValue) {
                String tmp = theMachine.getGcodeUnitMode().get();

//                gcodeUnitMode.getSelectionModel().select(tinygDriver.m.getGcodeUnitModeAsInt());
                if (theMachine.getGcodeUnitModeAsInt() == 0) {
                    //A bug in the jfxtras does not allow for units to be updated.. we hide them if they are not mm
                    xLcd.lcdUnitVisibleProperty().setValue(false);
                    yLcd.lcdUnitVisibleProperty().setValue(false);
                    zLcd.lcdUnitVisibleProperty().setValue(false);
                    aLcd.lcdUnitVisibleProperty().setValue(false);
                    velLcd.lcdUnitVisibleProperty().setValue(false);
                } else {
                    xLcd.lcdUnitVisibleProperty().setValue(true);
                    yLcd.lcdUnitVisibleProperty().setValue(true);
                    zLcd.lcdUnitVisibleProperty().setValue(true);
                    aLcd.lcdUnitVisibleProperty().setValue(true);
                    velLcd.lcdUnitVisibleProperty().setValue(true);
                }
                tgfx.Main.postConsoleMessage("[+]Gcode Unit Mode Changed to: " + tmp + "\n");

                try {
                    tinygDriver.getSerialWriter().setThrottled(true);
                    tinygDriver.priorityWrite(CommandManager.CMD_QUERY_MOTOR_1_SETTINGS);
                    tinygDriver.priorityWrite(CommandManager.CMD_QUERY_MOTOR_2_SETTINGS);
                    tinygDriver.priorityWrite(CommandManager.CMD_QUERY_MOTOR_3_SETTINGS);
                    tinygDriver.priorityWrite(CommandManager.CMD_QUERY_MOTOR_4_SETTINGS);

                    tinygDriver.priorityWrite(CommandManager.CMD_QUERY_AXIS_X);
                    tinygDriver.priorityWrite(CommandManager.CMD_QUERY_AXIS_Y);
                    tinygDriver.priorityWrite(CommandManager.CMD_QUERY_AXIS_Z);
                    tinygDriver.priorityWrite(CommandManager.CMD_QUERY_AXIS_A);
                    tinygDriver.priorityWrite(CommandManager.CMD_QUERY_AXIS_B);
                    tinygDriver.priorityWrite(CommandManager.CMD_QUERY_AXIS_C);
                    Thread.sleep(400);
                    tinygDriver.getSerialWriter().setThrottled(false);
                } catch (Exception ex) {
                    logger.error("Error querying tg model state on gcode unit change.  GCodeTabController.java binding section.");
                }
            }
        });

        machinePreview.heightProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue o, Object oldVal,
                    Object newVal) {
                logger.info("cncHeightChanged: " + machinePreview.getHeight());
//                Main.print(cncHeightString 
            }
        });
        machinePreview.maxWidthProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue ov, Object oldValue, Object newValue) {
                handleMaxWithChange();
            }
        });
        machinePreview.maxHeightProperty()
                .addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue ov, Object oldValue, Object newValue) {
                handleMaxHeightChange();
            }
        });


        /*######################################
         * GCODE FILE CODE
         ######################################*/
        data = FXCollections.observableArrayList();

        gcodeCol.setCellValueFactory(
                new PropertyValueFactory<GcodeLine, String>("codeLine"));
        GcodeLine n = new GcodeLine("Click open to load..", 0);

        gcodeView.getItems().setAll(data);
        data.add(n);

        gcodeView.setItems(data);

        gcodeView.addEventHandler(MouseEvent.MOUSE_CLICKED,
                new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                if (me.getButton().equals(me.getButton().PRIMARY)) {
                    if (me.getClickCount() == 2) {
                        GcodeLine gcl = (GcodeLine) gcodeView.getSelectionModel().getSelectedItem();
                        if (tinygDriver.isConnected().get()) {
                            logger.info("Double Clicked gcodeView " + gcl.getCodeLine());
                            try {
                                tinygDriver.write(gcl.getGcodeLineJsonified());
                                tgfx.Main.postConsoleMessage(gcl.getGcodeLineJsonified());
                            } catch (Exception ex) {
                                logger.error(ex);
                            }
                        } else {
                            logger.info("TinyG Not Connected not sending: " + gcl.getGcodeLineJsonified());
                            tgfx.Main.postConsoleMessage("TinyG Not Connected not sending: " + gcl.getGcodeLineJsonified());
                        }
                    }
                }
            }
        });
    }
    static int test = 1;

    @FXML
    static void handleTestButton(ActionEvent evt) throws Exception {
        //logger.info("Test Button....");

        updateProgress(test);
        test += 5;

        //tgfx.Main.postConsoleMessage("Test!");
        //timeElapsedTxt.setText("hello");

//        Iterator ii = null;
//        Line l;
//        machinePreview.getChildren().iterator();
//        while(ii.hasNext()){
//            l = (Line) ii.next();
//            
//        }
    }

    public Task fileSenderTask() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                StringBuilder line = new StringBuilder();
                int gcodeCharLength = data.size();
                for (int i = 0; i < gcodeCharLength; i++) {
                    GcodeLine _gcl = (GcodeLine) data.get(i);

                    if (isTaskActive() == false) {
                        //Cancel Button was pushed
                        tgfx.Main.postConsoleMessage("[!]File Sending Task Killed....\n");
                        break;
                    } else {
                        if (_gcl.getCodeLine().equals("")) {
                            //Blank Line.. Passing.. 
                            continue;
                        }

                        if (_gcl.getCodeLine().toLowerCase().contains("(")) {
                            tinygDriver.write("Comment:" + _gcl.getCodeLine());
//                            tgfx.Main.postConsoleMessage("GCODE COMMENT:" + _gcl.getCodeLine());
                            continue;
                        }
                        line.setLength(0);
                        line.append("{\"gc\":\"").append(_gcl.getCodeLine()).append("\"}\n");
                        tinygDriver.write(line.toString());
                    }
                }
                tinygDriver.write("**FILEDONE**");
                return true;
            }
        };
    }

    public static void setIsFileSending(boolean flag) {
        isSendingFile.set(flag);
    }

    public boolean isTaskActive() {
        return taskActive.get();
    }

    public void setTaskActive(boolean boolTask) {
        taskActive.set(boolTask);
    }

    /*######################################
     * EVENT LISTENERS CODE
     ######################################*/
    public void handleMaxHeightChange() {
        if (gcodePane.getWidth() - theMachine.getX().getTravelMaxSimple().get() < gcodePane.getHeight() - theMachine.getY().getTravelMaxSimple().get()) {
            //X is longer use this code
            if (theMachine.getGcodeUnitModeAsInt() == 0) {  //INCHES
                scaleAmount = ((gcodePane.heightProperty().get() / (theMachine.getY().getTravelMaxSimple().get() * 25.4))) * .80;  //%80 of the scale;
            } else { //MM
                scaleAmount = ((gcodePane.heightProperty().get() / theMachine.getY().getTravelMaxSimple().get())) * .80;  //%80 of the scale;
            }
        } else {
            //Y is longer use this code
            if (theMachine.getGcodeUnitModeAsInt() == 0) {  //INCHES
                scaleAmount = ((gcodePane.heightProperty().get() / (theMachine.getY().getTravelMaxSimple().get() * 25.4))) * .80;  //%80 of the scale;
            } else { //MM
                scaleAmount = ((gcodePane.heightProperty().get() / theMachine.getY().getTravelMaxSimple().get())) * .80;  //%80 of the scale;
            }
            //  scaleAmount = ((gcodePane.heightProperty().get() / tinygDriver.m.getY().getTravelMaxSimple().get())) * .80;  //%80 of the scale;
        }
        machinePreview.autoScaleWorkTravelSpace(scaleAmount);
        //        widthSize.textProperty().bind( Bindings.format("%s",  
                     //machinePreview.widthProperty().divide(tinygDriver.m.gcodeUnitDivision).asString().concat(tinygDriver.m.getGcodeUnitMode())    ));
        //        heightSize.setText(decimalFormat.format(
            //             tinygDriver.m.getY().getTravel_maximum()) + " " + tinygDriver.m.getGcodeUnitMode().getValue());
    }

    public void handleMaxWithChange() {
        //This is for the change listener to call for Max Width Change on the CNC Machine
        if (gcodePane.getWidth() - theMachine.getX().getTravelMaxSimple().get() < gcodePane.getHeight() - theMachine.getY().getTravelMaxSimple().get()) {
            //X is longer use this code
            if (theMachine.getGcodeUnitModeAsInt() == 0) {  //INCHES
                scaleAmount = ((gcodePane.heightProperty().get() / (theMachine.getY().getTravelMaxSimple().get() * 25.4))) * .80;  //%80 of the scale;
            } else { //MM
                scaleAmount = ((gcodePane.heightProperty().get() / theMachine.getY().getTravelMaxSimple().get())) * .80;  //%80 of the scale;
            }
        } else {
            //Y is longer use this code
            if (theMachine.getGcodeUnitModeAsInt() == 0) {  //INCHES
                scaleAmount = ((gcodePane.heightProperty().get() / (theMachine.getY().getTravelMaxSimple().get() * 25.4))) * .80;  //%80 of the scale;
            } else { //MM
                scaleAmount = ((gcodePane.heightProperty().get() / theMachine.getY().getTravelMaxSimple().get())) * .80;  //%80 of the scale;
            }
        }
        machinePreview.autoScaleWorkTravelSpace(scaleAmount);
//        widthSize.setText(decimalFormat.format(tinygDriver.m.getX().getTravel_maximum()) + " " + tinygDriver.m.getGcodeUnitMode().getValue());

    }

    // Scroll Gcode table view to specified line, show elapsed and remaining time
    public static void updateProgress(int lineNum) {

        if (isSendingFile.get() && lineNum > 0) {
//            gcodeView.scrollTo(lineNum);

            // Show elapsed and remaining time
            Date currentTimeDt = new Date();  // Get current time
            long elapsed = (currentTimeDt.getTime() - timeStartDt.getTime());
            float rate = elapsed / lineNum;
            long remain = (long) ((totalGcodeLines - lineNum) * rate);  // remaining lines * secs per line

            timeElapsedTxt.setText(String.format("%02d:%02d", elapsed / 60000, (elapsed / 1000) % 60));
            timeLeftTxt.setText(String.format("%02d:%02d", remain / 60000, (remain / 1000) % 60));
        }
    }
}
