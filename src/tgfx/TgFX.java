/*
 * Copyright (c) 2013 Synthetos LLC
 * Rileyporter@gmail.com
 * www.synthetos.com
 */
package tgfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * This is the starting class, the real main of the tgFX application.
 * @author ril3y
 */
public class TgFX extends Application {

    private static final Logger logger = Logger.getLogger(TgFX.class);

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = (Parent) FXMLLoader.load(getClass().getResource("Main.fxml"));
        Scene scene = new Scene(root);

        scene.setRoot(root);

        FXMLLoader fxmlLoader = new FXMLLoader();
        Object obj = fxmlLoader.getController();
        if (obj instanceof TgFX) {
            logger.error("have object of " + obj.getClass().getName());
            TgFX ignored = (TgFX) obj;
        }

        stage.setMinHeight(800);
        stage.setMinWidth(1280);
        stage.setScene(scene);
        stage.show();
    }
/**
 * usual startup from the shell or GUI
 * @param args standard command line arguments.
 */
    public static void main(String[] args) {
        PropertyConfigurator.configure("log4j.properties");
        logger.info("tgFX started");
        Application.launch(TgFX.class, args);
    }
}
