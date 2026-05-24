package ui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import javax.swing.*;
import java.io.IOException;

// This class is responsible for drawing the screen and handling user input.
// It is a thin wrapper around the TerminalScreen class from Lanterna.
public class AppScreen {

    private Screen screen; // manages hidden back buffer, watches for window resizes, and boots up your application space.
    private TextGraphics textGraphics; // This is our actual ink pen. Whenever we want to draw a border symbol or write text, we tell this pen what to do.

    public AppScreen() throws IOException {
        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        this.screen = new TerminalScreen(terminal);
        this.screen.startScreen(); // hide the normal cursor and enter fullscreen mode
        this.textGraphics = this.screen.newTextGraphics();
    }


    //Lifecycle methods

    // wipe our virtual scratchpad completely clean so we don't draw text on top of old data.
    public void clear() {
        this.screen.clear();
    }

    public void refresh() throws IOException {
        this.screen.doResizeIfNecessary(); // make sure the user didn't change their window boundaries
        this.screen.refresh(); // draw the screen to the real screen
    }

    public void stop() throws IOException {
        screen.close();
    }


    public KeyStroke readKey() throws IOException {
        return screen.readInput();
    }

    // Drawing methods

    public void draw(int row, int col, String text, TextColor color) {
        this.textGraphics.setForegroundColor(color);
        this.textGraphics.putString(col, row, text);
        this.textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
    }

    public void draw(int row, int col, String text) {
        draw(row, col, text, TextColor.ANSI.WHITE);
    }

    public void drawCentered(int row, int col, String text, TextColor color) {
        this.textGraphics.setForegroundColor(color);
        this.textGraphics.putString(col - (text.length() / 2), row, text);
        this.textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
    }

    public void generateTitle() {
        String title = "MANGA TRACKER";
        String underline = "=".repeat(title.length());
        int centerCol = getCols() / 2;
        drawCentered(0, centerCol, title, TextColor.ANSI.YELLOW_BRIGHT);
        drawCentered(1, centerCol, underline, TextColor.ANSI.YELLOW_BRIGHT);
    }

    public String readUntilEnter(int startRow, int startCol) throws IOException {
        StringBuilder inputBuffer = new StringBuilder();
        int currentCol = startCol;

        // Ensure the cursor is visible at the start position
        this.screen.setCursorPosition(new TerminalPosition(startCol, startRow));
        screen.refresh();

        while (true) {
            // readInput blocks until a key is pressed
            KeyStroke keyStroke = screen.readInput();

            if (keyStroke.getKeyType() == KeyType.Enter) {
                break;
            } else if (keyStroke.getKeyType() == KeyType.Character) {
                char c = keyStroke.getCharacter();

                inputBuffer.append(c);
                draw(startRow, startCol, inputBuffer.toString(), TextColor.ANSI.WHITE);

                currentCol++;
                screen.setCursorPosition(new TerminalPosition(currentCol, startRow));
                screen.refresh();
            } else if (keyStroke.getKeyType() == KeyType.Backspace && !inputBuffer.isEmpty()) {
                inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                currentCol--;

                // Clear the deleted character and move the cursor back
                draw(startRow, currentCol, " ", TextColor.ANSI.WHITE);
                screen.setCursorPosition(new TerminalPosition(currentCol, startRow));
                screen.refresh();
            } else if (keyStroke.getKeyType() == KeyType.Escape) {
                return null;
            }
        }

        return inputBuffer.toString();
    }




    public int getRows() {
        return this.screen.getTerminalSize().getRows();
    }

    public int getCols() {
        return this.screen.getTerminalSize().getColumns();
    }
}

