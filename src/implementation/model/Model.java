package model;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.Stack;

import static model.ColorSet.getColor;
import static model.ColorSet.greyScale;

/**
 * This class stores the methods for mandelbrot set related operations and all variables used by the model components.
 */
public class Model {
    // used by FileOps.java
    public static File file;
    public static final int numberOfParameters = 10;
    // Delegate.canvas is a square
    public static final int drawSize = 1000;
    public static final double colorMaxValue = 255; // used by ColorSet.java
    public static double colorScale;
    public static Color currentColor;

    // used by pan & zoom operations
    public static double mouseDownX; // initial position of mouse click event
    public static double mouseDownY;
    public static double delta; // the 'length' of the line/ square
    public static double deltaX;
    public static double deltaY;

    // used by show zoom magnification operations
    public static double currentMagnification;

    // used by undo/ redo operations
    public static final Stack<String> undoStack = new Stack<>();
    public static final Stack<String> redoStack = new Stack<>();
    public static Boolean isUndo = false;   // new actions during undo/ redo can overwrite the stacks
    public static Boolean isRedo = false;
    public static Boolean isOverride = false;

    // the parameters for the mandelbrot set
    public static double currentMinReal;
    public static double currentMaxReal;
    public static double currentMinImaginary;
    public static double currentMaxImaginary;
    public static int currentMaxIterations;
    public static double currentRadiusSquared;

    public static final MandelbrotCalculator mandelCalc = new MandelbrotCalculator();
    public static int[][] graph = new int[drawSize][drawSize];

    /**
     * This sets the parameters to default settings.
     */
    public static void setParameters() {
        currentMinReal = MandelbrotCalculator.INITIAL_MIN_REAL;
        currentMaxReal = MandelbrotCalculator.INITIAL_MAX_REAL;
        currentMinImaginary = MandelbrotCalculator.INITIAL_MIN_IMAGINARY;
        currentMaxImaginary = MandelbrotCalculator.INITIAL_MAX_IMAGINARY;
        currentMaxIterations = MandelbrotCalculator.INITIAL_MAX_ITERATIONS;
        currentRadiusSquared = MandelbrotCalculator.DEFAULT_RADIUS_SQUARED;

        colorScale = colorMaxValue / currentMaxIterations;
        currentColor = greyScale;

        currentMagnification = 1;
    }

    /**
     * Another version of the draw method.
     * Used to draw the graph.
     * @param canvas The canvas to be drawn.
     */
    public static void draw(Canvas canvas) {
        draw(currentColor, drawSize, canvas);
    }

    /**
     * This draws the content of the graph to the canvas.
     * This version is used to clear the show magnification area
     * if it is disabled.
     * @param tone The original color used as reference.
     * @param yEnd The index of last horizontal line to be drawn.
     * @param canvas The canvas to be drawn.
     */
    public static void draw(Color tone, int yEnd, Canvas canvas) {
        PixelWriter writer = canvas.getGraphicsContext2D().getPixelWriter();
        for (int yIndex = 0; yIndex < yEnd; yIndex++) {
            for (int xIndex = 0; xIndex < drawSize; xIndex++) {
                int colorValue = (int) Math.round(graph[yIndex][xIndex] * colorScale);  // casting will have no effect to the data
                getColor(tone, colorValue);
                writer.setColor(xIndex, yIndex, currentColor);
            }
        }
        currentColor = tone;    // this sets 'currentColor' back to its original value after being used as temporary storage
    }

    /**
     * This logs the position of pressed mouse event
     * for pan and zoom operations.
     * @param mouseEvent The mouse event.
     * @param selectionContext The graphics context of the canvas.
     * @param pan The pan button indicating pan or zoom operations.
     * @param area The square used for zoom operations.
     */
    public static void logPressed(MouseEvent mouseEvent, GraphicsContext selectionContext, RadioButton pan, Rectangle area) {
        mouseDownX = mouseEvent.getX();
        mouseDownY = mouseEvent.getY();
        if (pan.isSelected()) {
            selectionContext.beginPath();
        } else {
            area.setX(mouseDownX);
            area.setY(mouseDownY);
        }
    }

    /**
     * This logs the position of dragged mouse event
     * and draw a line/ square for pan and zoom operations.
     * @param mouseEvent The mouse event.
     * @param selectionContext The graphics context of the canvas.
     * @param pan The pan button indicating pan or zoom operations.
     * @param area The square used for zoom operations.
     */
    public static void logDragged(MouseEvent mouseEvent, GraphicsContext selectionContext, RadioButton pan, Rectangle area) {
        selectionContext.clearRect(0, 0, selectionContext.getCanvas().getWidth(), selectionContext.getCanvas().getHeight()); // clear previous lines/ squares
        if (pan.isSelected()) {
            selectionContext.strokeLine(mouseDownX, mouseDownY, mouseEvent.getX(), mouseEvent.getY());  // draw a straight line
        } else {
            deltaX = Math.abs(mouseEvent.getX() - mouseDownX);
            deltaY = Math.abs(mouseEvent.getY() - mouseDownY);
            delta = Math.max(deltaX, deltaY); // the length of the square depends on the longer side
            area.setX(Math.min(mouseEvent.getX(), mouseDownX)); // does not matter even if out of bound
            area.setY(Math.min(mouseEvent.getY(), mouseDownY));
            selectionContext.strokeRect(area.getX(), area.getY(), delta, delta); // this just shows where the user selected
        }
    }

    /**
     * This logs the position of released mouse event
     * for pan and zoom operations.
     * @param mouseEvent The mouse event.
     * @param selectionContext The graphics context of the canvas.
     * @param pan The pan button indicating pan or zoom operations.
     * @param area The square used for zoom operations.
     */
    public static void logReleased(MouseEvent mouseEvent, GraphicsContext selectionContext, RadioButton pan, Rectangle area) {
        selectionContext.clearRect(0, 0, selectionContext.getCanvas().getWidth(), selectionContext.getCanvas().getHeight()); // clear previous lines/ squares
        deltaX = mouseEvent.getX() - mouseDownX; // negative value is needed by pan operations
        deltaY = mouseEvent.getY() - mouseDownY;
        if (pan.isSelected()) {
            calculatePan();
        } else { // resizing is done before calculation to exclude areas out of bound
            double tempX = Math.min(mouseEvent.getX(), mouseDownX) < 0 ? 0 : Math.min(mouseEvent.getX(), mouseDownX); // the lower limit is 0
            double tempY = Math.min(mouseEvent.getY(), mouseDownY) < 0 ? 0 : Math.min(mouseEvent.getY(), mouseDownY);
            area.setX(tempX);
            area.setY(tempY);
            deltaX = Math.abs(deltaX); // only non-negative values are accepted
            deltaY = Math.abs(deltaY);
            delta = Math.max(deltaX, deltaY); // the length of the square depends on the longer side
            delta = area.getX() + delta > drawSize ? drawSize - area.getX() : delta; // the upper limit is the draw size
            delta = area.getY() + delta > drawSize ? drawSize - area.getY() : delta;
            calculateZoom(area);
        }
    }

    /**
     * This calculates the mandelbrot set using current parameters, updates current magnification
     * and store the result in a 2D integer array.
     */
    public static void setGraph() {
        graph = mandelCalc.calcMandelbrotSet(drawSize, drawSize, currentMinReal, currentMaxReal,
                currentMinImaginary, currentMaxImaginary,
                currentMaxIterations, currentRadiusSquared);

        currentMagnification = (MandelbrotCalculator.INITIAL_MAX_REAL - MandelbrotCalculator.INITIAL_MIN_REAL) /(currentMaxReal - currentMinReal);
        currentMagnification *= (MandelbrotCalculator.INITIAL_MAX_IMAGINARY - MandelbrotCalculator.INITIAL_MIN_IMAGINARY) / (currentMaxImaginary - currentMinImaginary);
        // this calculates magnification using the areas of the original & new graph
    }

    /**
     * This calculates new parameters for 'setGraph' method and
     * thereby execute pan operation.
     */
    public static void calculatePan() {
        String undo = "Pan " + currentMinReal + " " + currentMaxReal + " " +
                currentMinImaginary  + " " + currentMaxImaginary  + " "; // this logs the parameters to the stacks for undo/ redo

        // 'xScale' & 'yScale' are declared locally to ensure they must be up-to-date
        double xScale = (currentMaxReal - currentMinReal) / drawSize; // used to calculate offset from pixel positions
        double yScale = (currentMaxImaginary - currentMinImaginary) / drawSize;
        currentMinReal += xScale * deltaX;
        currentMaxReal += xScale * deltaX;
        currentMinImaginary += yScale * deltaY;
        currentMaxImaginary += yScale * deltaY;

        undo +=  currentMinReal + " " + currentMaxReal + " " +
                currentMinImaginary  + " " + currentMaxImaginary; // this logs the new parameters
        undoStack.addElement(undo);
    }

    /**
     * This calculates new parameters for 'setGraph' method and
     * thereby execute zoom operation.
     */
    public static void calculateZoom(Rectangle area) {
        String undo = "Zoom " + currentMinReal + " " + currentMaxReal + " " +
                currentMinImaginary  + " " + currentMaxImaginary  + " "; // this logs the parameters to the stacks for undo/ redo

        // 'xScale' & 'yScale' are declared locally to ensure they must be up-to-date
        double xScale = (currentMaxReal - currentMinReal) / drawSize;  // used to calculate offset from pixel positions
        double yScale = (currentMaxImaginary - currentMinImaginary) / drawSize;
        double tempMinReal = currentMinReal + xScale * area.getX();
        double tempMaxReal = currentMinReal + xScale * (area.getX() + delta);
        double tempMinImaginary = currentMinImaginary + yScale * area.getY();
        double tempMaxImaginary = currentMinImaginary + yScale * (area.getY() + delta);

        currentMinReal = tempMinReal;
        currentMaxReal = tempMaxReal;
        currentMinImaginary = tempMinImaginary;
        currentMaxImaginary = tempMaxImaginary;

        undo +=  currentMinReal + " " + currentMaxReal + " " + currentMinImaginary  + " " + currentMaxImaginary;  // this logs the new parameters
        undoStack.addElement(undo);
    }

    /**
     * This checks if show magnification function is enabled and draw correspondingly.
     * @param canvas The canvas to be drawn.
     * @param showZoom The button indicating the function is enabled or not.
     */
    public static void setShowZoom(Canvas canvas, RadioButton showZoom) {
        final int length = 350;
        final int height = 20;
        GraphicsContext graphContext = canvas.getGraphicsContext2D();
        if (showZoom.isSelected()) {
            graphContext.setFill(Color.BLACK); // this ensures the text is visible under any setting
            graphContext.fillRect(0, 0, length, height);
            graphContext.setFill(Color.WHITE);
            graphContext.fillText("Current magnification: " + currentMagnification + "x", 10, 15);
        } else {
            draw(currentColor, height, canvas); // this overwrites the content of the first 20 lines with the content of the graph
        }
    }

    /**
     * This undoes/ redoes the operations in the stacks.
     * It will not respond if there is nothing to do.
     * @param canvas The canvas to be drawn.
     * @param showZoom The button indicating show magnification function is enabled or not.
     * @param maxIterations The text field containing the value of max. iterations.
     * @param undoFlag The flag indicating it is an undo/ redo operation.
     */
    public static void undoAndRedo(Canvas canvas, RadioButton showZoom, TextField maxIterations, Boolean undoFlag) {
        Stack<String> targetStack = undoFlag ? undoStack : redoStack;
        if (!targetStack.isEmpty()) {
            if (undoFlag) {
                isUndo = true; // for functions' internal reference
            } else {
                isRedo = true; // for functions' internal reference
                isOverride = false; // temporary setting 'isOverride' to false
            }
            String item = targetStack.pop();
            String type = item.split(" ")[0];
            switch (type) {
                case "Pan":
                case "Zoom":
                    currentMinReal = undoFlag ? Double.parseDouble(item.split(" ")[1]) : Double.parseDouble(item.split(" ")[5]);
                    currentMaxReal = undoFlag ? Double.parseDouble(item.split(" ")[2]) : Double.parseDouble(item.split(" ")[6]);
                    currentMinImaginary = undoFlag ? Double.parseDouble(item.split(" ")[3]) : Double.parseDouble(item.split(" ")[7]);
                    currentMaxImaginary = undoFlag ? Double.parseDouble(item.split(" ")[4]) : Double.parseDouble(item.split(" ")[8]);
                    setGraph();
                    draw(canvas);
                    break;
                case "Color":
                    currentColor = undoFlag ? Color.valueOf(item.split(" ")[1]) : Color.valueOf(item.split(" ")[2]);
                    draw(canvas);
                    break;
                case "Iterations":
                    currentMaxIterations = undoFlag ? Integer.parseInt(item.split(" ")[1]) : Integer.parseInt(item.split(" ")[2]);
                    maxIterations.setPromptText(String.valueOf(currentMaxIterations));
                    colorScale = colorMaxValue / currentMaxIterations;
                    setGraph();
                    draw(canvas);
                    break;
                case "ToggleZoom":
                    showZoom.setSelected(!showZoom.isSelected());
                    break;
            }
            setShowZoom(canvas, showZoom); // need to draw again as it can be overwritten
            if (undoFlag) {
                isUndo = false;
                isOverride = true;
                redoStack.push(item);
            } else {
                isRedo = false;
                isOverride = !redoStack.isEmpty(); // if there is nothing to redo it is true
                undoStack.push(item);
            }
        }
    }

}