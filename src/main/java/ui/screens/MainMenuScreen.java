package ui.screens;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import model.Status;
import ui.AppScreen;
import ui.Router;

import java.io.IOException;

public class MainMenuScreen {

    private final AppScreen screen;
    private final Router router;

    private final String[] labels = {
            "Add manga",
            "Currently reading",
            "Plan to read",
            "Completed",
            "Dropped",
            "Quit"
    };

    private int selected = 0;

    public MainMenuScreen(AppScreen screen, Router router) {
        this.screen = screen;
        this.router = router;
    }

 /*
 Render the main menu screen.
 Draw the title, and then draw each option in the labels array.
 The selected option is highlighted in yellow.
 On each keystroke, we rerender the screen.
 The highlighted option will only be highlighted when the loop index is equal to the selected index.
  */
    public void render() throws IOException {
        screen.clear();
        screen.generateTitle();
        int startRow = 4;
        int col = 4;
        for (int i = 0; i < labels.length; i++) {
            boolean isSelected = i == selected;

            String prefix = isSelected ? "> " : "  "; // put a > on the selected option
            TextColor color = isSelected ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE; // make the selected option yellow

            screen.draw(startRow + i, col, prefix + labels[i], color); // draw the option
        }
        screen.draw(screen.getRows() -2, 2,"↑/↓ move   enter select");
        screen.hideCursor();
        screen.refresh();
    }



    public void handleKey(KeyStroke key) throws IOException {
        /*
        * When the user goes up, the selected index is decremented by 1.
        * The minimum value of selected is 0, which is the first index in the labels array.
         */

        if (key.getKeyType() ==  KeyType.ArrowUp) {
            selected = Math.max(selected - 1, 0); // By moving up one, we decrement the selected index
        }
        /*
        * When the user goes down, the selected index is increased by 1.
        * The maximum value of selected is labels.length - 1, which is the last index in the labels array.
        * Therefore, if the user goes beyond the last index, the selected index will remain at the last index.
         */
        else if (key.getKeyType() == KeyType.ArrowDown) {
            selected = Math.min(selected + 1, labels.length - 1);
        }
        else if (key.getKeyType() == KeyType.Enter) {
            handleEnter();
        }
    }

// a method to handle the Enter key press
    private void handleEnter() throws IOException {
        switch (selected) {
            case 0 -> router.goToSearch();
            case 1 -> router.goToList(Status.READING);
            case 2 -> router.goToList(Status.PLAN_TO_READ);
            case 3 -> router.goToList(Status.COMPLETED);
            case 4 -> router.goToList(Status.DROPPED);
            case 5 -> router.quit();
        }
    }
}
