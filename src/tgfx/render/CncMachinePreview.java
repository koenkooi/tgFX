/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.render;

import com.google.common.base.Preconditions;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import tgfx.Main;
import tgfx.system.Machine;
import tgfx.tinyg.CommandManager;
import tgfx.tinyg.TinygDriver;
import tgfx.tinyg.TinygDriverFactory;
import tgfx.ui.gcode.GcodeTabController;

/**
 *
 * @author rileyporter
 */
public class CncMachinePreview extends Pane {

    public static StackPane gcodePane = new StackPane(); //Holds CncMachinePreview
    private DecimalFormat df = new DecimalFormat("#.##");
    private final Circle cursorPoint = new Circle(2, javafx.scene.paint.Color.RED);
    private static double xPrevious;
    private static double yPrevious;
    private boolean _msgSent = false;
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CncMachinePreview.class);
    private final Machine machine;
    private final TinygDriver tinygD;

    public CncMachinePreview() {
        tinygD = TinygDriverFactory.getTinygDriver();
        machine = tinygD.getMachine();
        Preconditions.checkNotNull(machine);
        //Cursor point indicator
        cursorPoint.setRadius(1);

        this.setMaxSize(0, 0);  //hide this element until we connect
        //Set our machine size from tinyg travel max
        this.setVisible(false);
        this.setPadding(new Insets(10));
        this.setFocusTraversable(true);
        this.setFocused(true);

        /*####################################
         *CSS
         #################################### */

        this.setStyle("-fx-background-color: black; -fx-border-color: orange;  -fx-border-width: .5;");

        /*####################################
         *PositionCursor Set
         #################################### */

        final Text cursorText = new Text("None");
        cursorText.setFill(Color.YELLOW);
        cursorText.setFont(Font.font("Arial", 6));

        setupLayout(); //initial layout setup in constructor

        /*####################################
         *Event / Change Listeners
         *#################################### */

//ugh...
//
//
//        ChangeListener posChangeListener = new ChangeListener() {
//            @Override
//            public void changed(ObservableValue ov, Object t, Object t1) {
//                if (machine.getY().getMachinePosition() > heightProperty().get()
//                        || machine.getX().getMachinePosition() > widthProperty().get()) {
//                    hideOrShowCursor(false);
//                } else {
//                    hideOrShowCursor(true);
//                }
//            }
//        };



        this.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
//                gcodePane.getChildren().remove(c);
                getChildren().remove(cursorText);
                unFocusForJogging();
            }
        });


        this.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                setFocusForJogging();
                requestFocus();
            }
        });


        this.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(final MouseEvent me) {
                //This is so we can set our machine position when a machine does not have homing switches
                if (me.getButton().equals(MouseButton.SECONDARY)) {
                    //Right Clicked
                    ContextMenu cm = new ContextMenu();
                    MenuItem menuItem1 = new MenuItem("Set Machine Position");
                    menuItem1.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent t) {
                            Draw2d.setFirstDraw(true); //We do not want to draw a line from our previous position
                            tinygD.getCmdManager().setMachinePosition(getNormalizedX(me.getX()), getNormalizedY(me.getY()));
                            Draw2d.setFirstDraw(true); //This allows us to move our drawing to a new place without drawing a line from the old.
                            try {
                                tinygD.write(CommandManager.CMD_APPLY_SYSTEM_ZERO_ALL_AXES);
                                tinygD.write(CommandManager.CMD_QUERY_STATUS_REPORT);
                            } catch (Exception ex) {
                                logger.error(ex);
                            }
                            //G92 does not invoke a status report... So we need to generate one to have
                            //Our GUI update the coordinates to zero
                        }
                    });
                    cm.getItems().add(menuItem1);
                    cm.show((Node) me.getSource(), me.getScreenX(), me.getScreenY());
                }
            }
        });


        /*####################################
         *Bindings
         *#################################### */

        maxHeightProperty().bind(machine.getY().getTravelMaxSimple().multiply(machine.getGcodeUnitDivision()));
        maxWidthProperty().bind(machine.getX().getTravelMaxSimple().multiply(machine.getGcodeUnitDivision()));
        cursorPoint.translateYProperty().bind(this.heightProperty().subtract(machine.getY().getMachinePositionSimple()));
        cursorPoint.layoutXProperty().bind(machine.getX().getMachinePositionSimple());
//        cncHeight.bind(this.heightProperty());
//        cncWidth.bind(this.widthProperty());
//        cursorPoint.layoutXProperty().addListener(posChangeListener); //When the x or y pos changes we see if we want to show or hide the cursor
//        cursorPoint.layoutYProperty().addListener(posChangeListener);
    }

    private void unFocusForJogging() {
        this.setFocused(true);
        //Main.postConsoleMessage("UnFocused");
        //GcodeTabController.hideGcodeText();
    }

    private void setFocusForJogging() {
        this.setFocused(true);
//        Main.postConsoleMessage("Focused");
        GcodeTabController.setGcodeText("Jogging Enabled");
    }

    public double getNormalizedX(double x) {
        return (Double.valueOf((x / machine.getGcodeUnitDivision().get())));
    }

    public double getNormalizedY(double y) {
        return (Double.valueOf((getHeight() - y) / machine.getGcodeUnitDivision().get()));
    }

    public String getNormalizedYasString(double y) {
        return (df.format(getNormalizedY(y)));
    }

    public String getNormalizedXasString(double x) {
        return (df.format(getNormalizedX(x)));
    }

    public boolean checkBoundsY(Line l) {
        if ((this.getHeight() - l.getEndY()) >=0 && (this.getHeight() - l.getEndY()) <= this.getHeight() + 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean checkBoundsX(Line l) {
        if (l.getEndX() >= 0 && l.getEndX() <= this.getWidth()) {
            return true;
        } else {
            return false;
        }
    }

    public void clearScreen() {
        this.getChildren().clear();
        Draw2d.setFirstDraw(true);  //We don't want to draw a line from where the previous point was when a clear screen is called.
        setupLayout();  //re-draw the needed elements.
    }

    /**
     *
     * @param moveType
     * @param vel
     */
    public void drawLine(String moveType, double vel) {
        Line l;
        l = new Line();
        l.setSmooth(true);
        double newX = machine.getX().getMachinePositionSimple().get();
        double newY = this.getHeight() - machine.getY().getMachinePositionSimple().get();
       
        if (Draw2d.isFirstDraw()) {
            //This is to not have us draw a line on the first connect.
            l = new Line(newX, this.getHeight(), newX, this.getHeight());
            Draw2d.setFirstDraw(false);
        } else {
            l = new Line(xPrevious, yPrevious, newX, newY);
            l.setStrokeWidth(.5);
        }
        
        xPrevious = newX;
        yPrevious = newY; //TODO Pull these out to CNC machine or Draw2d these are out of place

        if (machine.getMotionMode().get().equals("traverse")) {
            //G0 Moves
            l.getStrokeDashArray().addAll(1d, 5d);
            l.setStroke(Draw2d.TRAVERSE);
        } else {
//            l.setStroke(Draw2d.getLineColorFromVelocity(vel));
            l.setStroke(Draw2d.FAST);
        }



        if (l != null) {
            if (this.checkBoundsX(l) && this.checkBoundsY(l)) {
                //Line is within the travel max gcode preview box.  So we will draw it.
                this.getChildren().add(l);  //Add the line to the Pane 
//                cursorPoint.visibleProperty().set(true);
                _msgSent = false;
                if (!getChildren().contains(cursorPoint)) { //If the cursorPoint is not in the Group and we are in bounds
                    this.getChildren().add(cursorPoint);  //Adding the cursorPoint back
                }

            } else {
                logger.info("Outside of Bounds X");
                
                if (getWidth() != 21 && getHeight() != 21) { //This is a bug fix to avoid the cursor being hidden on the initial connect.
                    //This should be fairly harmless as it will always show the cursor if its the inital connect size 21,21
                    //its a bit of a hack but it works for now.
//                    cursorPoint.visibleProperty().set(false);
//                    Draw2d.setFirstDraw(true);
                    if (getChildren().contains(cursorPoint)) { //If cursor is in the group we are going to remove it util above is true
                        getChildren().remove(this.getChildren().indexOf(cursorPoint)); //Remove it.
                        if (!_msgSent) {
                            Main.postConsoleMessage("You are out of your TinyG machine working envelope.  You need to either move back in by jogging, homing"
                                    + "\n or you can right click on the Gcode Preview and click set position to set your estimated position.\n");
                            _msgSent = true; //We do this as to not continue to spam the user with out of bound errors.
                        }
                    }
                } else {
                    logger.error("how did we get here?");
                }
            }
        }
       
    }

    public void zeroSystem() {
        if (tinygD.isConnected().get()) {
            try {
                Draw2d.setFirstDraw(true); //This allows us to move our drawing to a new place without drawing a line from the old.
                tinygD.write(CommandManager.CMD_APPLY_SYSTEM_ZERO_ALL_AXES);
                //G92 does not invoke a status report... So we need to generate one to have
                //Our GUI update the coordinates to zero
                tinygD.write(CommandManager.CMD_QUERY_STATUS_REPORT);
                //We need to set these to 0 so we do not draw a line from the last place we were to 0,0
                resetDrawingCoords();
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }

    public static void resetDrawingCoords() {
        //After a reset has occured we call this ot reset the previous coords.
        xPrevious = 0;
        yPrevious = 0;
    }

    private void setupLayout() {
        //This draws the x axis text as well as grid etc
        Text xText = new Text("X Axis");
        Text yText = new Text("Y Axis");

        xText.setY(-10);
        xText.xProperty().bind(this.heightProperty().divide(2));
        xText.setRotate(0);
        xText.setFill(Color.YELLOW);
        xText.setFont(Font.font("Arial", 10));

        yText.setX(-25);
        yText.yProperty().bind(this.widthProperty().divide(2));
        yText.setRotate(-90);
        yText.setFill(Color.YELLOW);
        yText.setFont(Font.font("Arial", 10));

        this.getChildren().add(xText);
        this.getChildren().add(yText);

        this.setCursor(Cursor.CROSSHAIR);
        this.getChildren().add(cursorPoint);
    }

    public void autoScaleWorkTravelSpace(double scaleAmount) {
        //Get the axis with the smallest available space.  Think aspect ratio really

        double stroke = 2 / scaleAmount;
        this.setScaleX(scaleAmount);
        this.setScaleY(scaleAmount);
        Iterator ii = this.getChildren().iterator();

        while (ii.hasNext()) {
            if (ii.next().getClass().getName().endsWith("Line")) {
                Line l = (Line) ii.next();
                l.setStrokeWidth(stroke);
            }
        }
    }
}
