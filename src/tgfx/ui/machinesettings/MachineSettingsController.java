/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.ui.machinesettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import org.apache.log4j.Logger;
import tgfx.Main;
import tgfx.system.Machine;
import tgfx.tinyg.CommandManager;
import tgfx.tinyg.TinygDriver;
import tgfx.tinyg.TinygDriverFactory;

/**
 * FXML Controller class
 *
 * @author rileyporter
 */
public class MachineSettingsController implements Initializable {

    private static final Logger logger = Logger.getLogger(MachineSettingsController.class);
    @FXML
    private static Label firmwareVersion;
    @FXML
    private Label hwVersion, buildNumb, hardwareId;
    @FXML
    private ListView configsListView;
    @FXML
    private static ChoiceBox machineSwitchType, machineUnitMode;
    private final TinygDriver tinygD;
    private final Machine theMachine;
    private static Machine staticMachine;

    private MachineSettingsController() {
        tinygD = TinygDriverFactory.getTinygDriver();
        theMachine = tinygD.getMachine();
        staticMachine = theMachine;
    }
    public static double getCurrentBuildNumber(){
        return(Double.valueOf(firmwareVersion.getText()));
    }
    
    public static void updateGuiMachineSettings() {
        machineUnitMode.getSelectionModel().select(staticMachine.getGcodeUnitModeAsInt());
        machineSwitchType.getSelectionModel().select(staticMachine.getSwitchType());
    }

    
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        populateConfigFiles();          //Populate all Config Files
        hardwareId.textProperty().bind(theMachine.getHardwareId()); //Bind the tinyg hardware id to the tg driver value
        hwVersion.textProperty().bind(theMachine.getHardwareVersion()); //Bind the tinyg version  to the tg driver value
        firmwareVersion.textProperty().bind(theMachine.getFirmwareVersion());
        buildNumb.textProperty().bind(theMachine.getFirmwareBuild().asString());
        
    }

    private void populateConfigFiles() {

        String path = "configs";

        String files;
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                files = listOfFiles[i].getName();
                if (files.endsWith(".config") || files.endsWith(".json")) {
                    configsListView.getItems().add(files);
                }
            }
        }
    }

    @FXML
    private void handleSaveCurrentSettings(ActionEvent event) throws Exception {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                FileChooser fc = new FileChooser();
                fc.setInitialDirectory(new File(System.getProperty("user.dir") + System.getProperty("file.separator") + "configs" + System.getProperty("file.separator")));
                fc.setTitle("Save Current TinyG Configuration");
                File f = fc.showSaveDialog(null);
                if (f.canWrite()) {
                }
            }
        });
    }

    @FXML
    private void handleImportConfig(ActionEvent event) throws Exception {
        //This function gets the config file selected and applys the settings onto tinyg.
        InputStream fis;
        BufferedReader br;
        String line;
        File selected_config = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + "configs" + System.getProperty("file.separator") + configsListView.getSelectionModel().getSelectedItem());
        fis = new FileInputStream(selected_config);
        br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

        while ((line = br.readLine()) != null) {
            if (tinygD.isConnected().get()) {
                if (line.startsWith("NAME:")) {
                    //This is the name of the CONFIG lets not write this to TinyG 
                    tgfx.Main.postConsoleMessage("[+]Loading " + line.split(":")[1] + " config into TinyG... Please Wait...");
                } else {
                    tinygD.write(line + "\n");    //Write the line to tinyG
                    Thread.sleep(100);      //Writing Values to eeprom can take a bit of time..
                    tgfx.Main.postConsoleMessage("[+]Writing Config String: " + line + "\n");
                }
            }
        }
    }

    @FXML
    private void handleApplyMachineSettings() {
        try {
            tinygD.getCmdManager().applyMachineSwitchMode(machineSwitchType.getSelectionModel().getSelectedIndex());
            tinygD.getCmdManager().applyMachineUnitMode(machineUnitMode.getSelectionModel().getSelectedIndex());
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handleQueryMachineSettings() {
        try {
            tinygD.getCmdManager().queryMachineSwitchMode();
            tinygD.getCmdManager().queryAllMachineSettings();
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    @FXML
    void handleApplyDefaultSettings(ActionEvent evt) {
        try {
            if (checkConectedMessage().equals("true")) {
                tinygD.write(CommandManager.CMD_APPLY_DEFAULT_SETTINGS);
            } else {
                logger.error(checkConectedMessage());
                tgfx.Main.postConsoleMessage(checkConectedMessage());
            }
        } catch (Exception ex) {
            logger.error("[!]Error Applying Default Settings", ex);
        }
    }

    private String checkConectedMessage() {
        if (tinygD.isConnected().get()) {
            return ("true");
        } else {
            return ("[!]TinyG is Not Connected");
        }
    }
}
