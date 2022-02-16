package model;

import javafx.scene.paint.Color;

import java.util.Arrays;

import static model.Model.colorMaxValue;
import static model.Model.currentColor;

/**
 * This class stores the list of colors used in the program and related methods.
 */
public class ColorSet {
    public static final Color blackWhite = Color.rgb(255, 255, 255);
    public static final Color greyScale = Color.rgb(0, 0, 0);
    public static final Color red = Color.rgb(255, 0, 0);
    public static final Color green = Color.rgb(0, 255, 0);
    public static final Color blue = Color.rgb(0, 0, 255);
    public static final Color yellow = Color.rgb(255, 255, 0);
    public static final Color cyan = Color.rgb(0, 255, 255);
    public static final Color pink = Color.rgb(255, 0, 255);
    public static final Color[] colorSet = {blackWhite, greyScale, red, green, blue, yellow, cyan, pink};


    /**
     * This changes the displayed color to the adjacent one.
     */
    public static void setColor() {
        int index = Arrays.asList(colorSet).indexOf(currentColor);
        index = (index + 1) % colorSet.length;
        currentColor = colorSet[index];
    }

    /**
     * This gets the color for each pixel following
     * different predefined rules.
     * @param tone The original color used as reference.
     * @param colorValue The value for the color (in sRGB format).
     */
    public static void getColor(Color tone, int colorValue) {
        if (tone.equals(blackWhite)) {
            colorValue = colorValue == colorMaxValue ? 0 : (int) colorMaxValue; // only pixels reaching max. iterations are black, else are white
            currentColor = Color.rgb(colorValue, colorValue, colorValue);
        }else if (tone.equals(greyScale)) {
            colorValue = colorValue == colorMaxValue ? 0 : colorValue; // pixels not reaching max. iterations are in grey scale
            currentColor = Color.rgb(colorValue, colorValue, colorValue);
        } else {
            colorValue = colorValue == colorMaxValue ? 0 : colorValue; // pixels not reaching max. iterations are in linear scale
            int rValue = tone.getRed() == 0.0 ? 0 : colorValue;
            int gValue = tone.getGreen() == 0.0 ? 0 : colorValue;
            int bValue = tone.getBlue() == 0.0 ? 0 : colorValue;
            currentColor = Color.rgb(rValue, gValue, bValue);
        }
    }
}
