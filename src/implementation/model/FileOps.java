package model;

import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import static model.Model.*;

/**
 * This class stores the methods for file operations.
 */
public class FileOps {

    /**
     * This selects a file for save/ load operation.
     * @param title The title of the file chooser.
     * @param isSave The boolean flag to decide save/ load operation.
     */
    public static void selectFile(String title, boolean isSave) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text Files", "*.txt"));   // files are stored in text format
        if (isSave) {
            file = fileChooser.showSaveDialog(new Stage());
        } else {
            file = fileChooser.showOpenDialog(new Stage());
        }
    }

    /**
     * This creates a file and writes program's current parameters for save operation.
     */
    public static void writeFile() {
        try {
            if (!file.getName().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            file.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));   // buffered writer can reduce IO calls
            writer.write(drawSize + "\n");  // the draw size is for validation purpose
            writer.write(drawSize + "\n");
            writer.write(currentMinReal + "\n");
            writer.write(currentMaxReal + "\n");
            writer.write(currentMinImaginary + "\n");
            writer.write(currentMaxImaginary + "\n");
            writer.write(currentMaxIterations + "\n");
            writer.write(currentRadiusSquared + "\n");
            writer.write(currentColor + "\n");
            writer.write(currentMagnification + "\n");
            for (int[] line : graph) {
                for (int value : line) {
                    writer.write(value + "\n"); // one value per line
                }
            }
            writer.flush();
            file.setReadOnly(); // prevent careless manipulation
        }

        catch (IOException e) {
            alert("Cannot write data to the file");
        }
    }

    /**
     * This reads the parameters from the designated file for load operation.
     */
    public static void readFile() {
        String input;
        int index = 1;
        try {
            double tempMinReal = 0; // temporary variables used to store data before validation
            double tempMaxReal = 0;
            double tempMinImaginary = 0;
            double tempMaxImaginary = 0;
            int tempMaxIterations = 0;
            double tempRadiusSquared = 0;
            Color tempColor = ColorSet.blackWhite;
            double tempMagnification = 0;
            int[][] tempGraph = new int[drawSize][drawSize];
            int position;   // used to store the position of data at the graph

            Scanner reader = new Scanner(file);

            while (reader.hasNextLine()) {  // write data from file to temporary storage
                input = reader.nextLine();
                switch (index) {
                    case 1:
                    case 2:
                        if (drawSize != Integer.parseInt(input)) {  // validate the draw size
                            throw new UnsupportedOperationException();
                        }
                        break;
                    case 3:
                        tempMinReal = Double.parseDouble(input);
                        break;
                    case 4:
                        tempMaxReal = Double.parseDouble(input);
                        break;
                    case 5:
                        tempMinImaginary = Double.parseDouble(input);
                        break;
                    case 6:
                        tempMaxImaginary = Double.parseDouble(input);
                        break;
                    case 7:
                        tempMaxIterations = Integer.parseInt(input);
                        break;
                    case 8:
                        tempRadiusSquared = Double.parseDouble(input);
                        break;
                    case 9:
                        tempColor = Color.valueOf(input);
                        break;
                    case 10:
                        tempMagnification = Double.parseDouble(input);
                        break;
                    default:
                        if (index < drawSize * drawSize + numberOfParameters) { // ignore extra inputs
                            position = index - numberOfParameters;
                            if (Integer.parseInt(input) > tempMaxIterations || Integer.parseInt(input) < 0) {  // validate data
                                throw new Exception();
                            }
                            tempGraph[position / drawSize] [position % drawSize] = Integer.parseInt(input);
                        }
                        break;
                }
                index++;
            }

            if (index != drawSize * drawSize + numberOfParameters + 1) {    // validate length of file (mainly if it is shorter than expected)
                throw new Exception();
            }

            for (index = 3; index <= drawSize * drawSize + numberOfParameters; index++) {   // write from temporary storage to memory
                switch (index) {    // first two are ignored, no operations needed
                    case 3:
                        currentMinReal = tempMinReal;
                        break;
                    case 4:
                        currentMaxReal = tempMaxReal;
                        break;
                    case 5:
                        currentMinImaginary = tempMinImaginary;
                        break;
                    case 6:
                        currentMaxImaginary = tempMaxImaginary;
                        break;
                    case 7:
                        currentMaxIterations = tempMaxIterations;
                        break;
                    case 8:
                        currentRadiusSquared = tempRadiusSquared;
                        break;
                    case 9:
                        currentColor = tempColor;
                        break;
                    case 10:
                        currentMagnification = tempMagnification;
                        break;
                    default:
                        if (index < drawSize * drawSize + numberOfParameters) { // no checking is needed, previously done
                            position = index - numberOfParameters;
                            graph[position / drawSize] [position % drawSize] = tempGraph[position / drawSize] [position % drawSize];
                        }
                        break;
                }
            }
            colorScale = colorMaxValue / currentMaxIterations;  // refresh parameters
            undoStack.clear();
            redoStack.clear();
        }

        catch (UnsupportedOperationException e) {   // type of exception is not important, used just to ensure others won't throw it
            alert("Invalid draw sizes");
        }

        catch (Exception e) {   // for exceptions rather than different draw size
            e.printStackTrace();
            alert("Cannot read data from the file. File is corrupted/ invalid.");
        }
    }

    /**
     * This informs the user about exceptions using a pop-up.
     * @param message The details of the exception.
     */
    public static void alert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error is detected: " + message);
        alert.setContentText("Current action is aborted");
        alert.showAndWait();
    }
}
