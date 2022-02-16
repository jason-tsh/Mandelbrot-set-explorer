package delegate;

import static model.Model.*;
import static model.ColorSet.*;
import static model.FileOps.*;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * This class stores the delegate component of the program.
 */
public class Delegate {

    @FXML public MenuItem save;
    @FXML public MenuItem load;

    @FXML public Canvas canvas;
    @FXML public Canvas selection;

    @FXML public RadioButton pan;
    @FXML public RadioButton showZoom;

    @FXML public Button reset;
    @FXML public Button undo;
    @FXML public Button redo;
    @FXML public Button changeIterations;
    @FXML public Button colorChoice;

    @FXML public TextField maxIterations;

    /**
     * This initializes the program using default settings.
     */
    @FXML
    public void initialize() {
        setParameters();
        setTool();
        setGraph();
        draw(canvas);
    }

    /**
     * This defines the interactions among the components of the program.
     */
    public void setTool() {
        GraphicsContext selectionContext = selection.getGraphicsContext2D(); // it is created here as it is primarily used here
        selectionContext.setStroke(Color.WHITE); // it is set here as the graphics context is created here
        selectionContext.setLineWidth(2.0);
        Rectangle area = new Rectangle();   // it is actually a square

        save.setOnAction(actionEvent -> {
            selectFile("Save", true);
            if (file != null) { // if a file is selected
                writeFile();
            }
        });

        load.setOnAction(actionEvent -> {
            selectFile("Load", false);
            if (file != null) {  // if a file is selected
                readFile();
                maxIterations.clear();
                maxIterations.setPromptText(String.valueOf(currentMaxIterations));
                draw(canvas);
            }
        });

        selection.setOnMousePressed(mouseEvent -> logPressed(mouseEvent, selectionContext, pan, area));

        selection.setOnMouseDragged(mouseEvent -> logDragged(mouseEvent, selectionContext, pan, area));

        selection.setOnMouseReleased(mouseEvent -> {
            logReleased(mouseEvent, selectionContext, pan, area);
            setGraph();
            draw(canvas);
            setShowZoom(canvas, showZoom);
            if (!isUndo && isOverride) { // if some actions are being undone while the user executed others
                redoStack.clear();
            } else {
                isOverride = true;
            }
        });

        showZoom.setOnAction(actionEvent -> {
            setShowZoom(canvas, showZoom);
            if (!isUndo && isOverride) { // if some actions are being undone while the user executed others
                redoStack.clear();
            } else {
                isOverride = true;
            }
            undoStack.addElement("ToggleZoom");
        });

        reset.setOnAction(actionEvent -> {
            setParameters();
            setGraph(); // update the graph
            draw(canvas); // display the graph
            pan.setSelected(false);
            selectionContext.setStroke(Color.WHITE); // it is set here as the graphics context is created here
            showZoom.setSelected(false);
            setShowZoom(canvas, showZoom); // clear magnification bar if needed
            maxIterations.clear();
            maxIterations.setPromptText(String.valueOf(currentMaxIterations));
            undoStack.clear(); // reset back to initial status
            redoStack.clear();
        });

        undo.setOnAction(actionEvent -> undoAndRedo(canvas, showZoom, maxIterations, true));

        redo.setOnAction(actionEvent -> undoAndRedo(canvas, showZoom, maxIterations, false));

        colorChoice.setOnAction(actionEvent -> {
            String undo = "Color " + currentColor + " ";
            setColor();
            draw(canvas);
            setShowZoom(canvas, showZoom);
            selectionContext.setStroke(currentColor.equals(blackWhite) ? Color.BLACK : Color.WHITE); // use black lines if the background is white
            if (!isUndo && isOverride) {  // if some actions are being undone while the user executed others
                redoStack.clear();
            } else {
                isOverride = true;
            }
            if (!isRedo && !isUndo) {  // if it is not an undo/ redo operation
                undo += currentColor;
                undoStack.addElement(undo);
            }
        });

        changeIterations.setOnAction(actionEvent -> {
            if (maxIterations.getText().length() > 0 &&
                    currentMaxIterations != Integer.parseInt(maxIterations.getText())) { // only invoked if a different value is entered
                String undo = "Iterations " + currentMaxIterations + " ";
                currentMaxIterations = Integer.parseInt(maxIterations.getText());
                colorScale = colorMaxValue / currentMaxIterations;
                maxIterations.clear();
                maxIterations.setPromptText(String.valueOf(currentMaxIterations));
                setGraph(); // update the graph
                draw(canvas); // display the graph
                setShowZoom(canvas, showZoom); // display magnification if needed
                if (!isUndo && isOverride) {  // if some actions are being undone while the user executed others
                    redoStack.clear();
                } else {
                    isOverride = true;
                }
                undo += currentMaxIterations;
                undoStack.addElement(undo);
            }
        });

        maxIterations.textProperty().addListener((observable, oldValue, newValue) -> { // exclude non-digit inputs
            if (!newValue.matches("\\d*")) {
                maxIterations.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }
}