package ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;

// This class is responsible for drawing the screen and handling user input.
// It is a thin wrapper around the TerminalScreen class from Lanterna, plus
// the shared building blocks of the TUI: rounded panels, tab bars,
// selection highlight bars, progress gauges, and the bottom status bar.
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
    // Filled via textGraphics rather than screen.clear(): clear() sets Lanterna's
    // full-redraw hint, which makes the next refresh repaint the whole terminal
    // and causes visible flicker on every render.
    public void clear() {
        this.textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
        this.textGraphics.setBackgroundColor(TextColor.ANSI.DEFAULT);
        this.textGraphics.fill(' ');
    }

    public void refresh() throws IOException {
        this.screen.doResizeIfNecessary(); // make sure the user didn't change their window boundaries
        this.screen.refresh(); // draw the screen to the real screen
    }

    // Repaint every cell, not just the ones that changed. Needed after
    // something outside Lanterna (like a Kitty image) has touched the terminal.
    public void fullRefresh() throws IOException {
        this.screen.doResizeIfNecessary();
        this.screen.refresh(Screen.RefreshType.COMPLETE);
    }

    public void hideCursor() {
        this.screen.setCursorPosition(null);
    }

    public void stop() throws IOException {
        screen.close();
    }


    public KeyStroke readKey() throws IOException {
        return screen.readInput();
    }

    // Non-blocking read: returns null when no key is waiting. Used by screens
    // that need to keep rendering (spinners, async cover downloads) while
    // listening for input.
    public KeyStroke pollKey() throws IOException {
        return screen.pollInput();
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

    public void drawBold(int row, int col, String text, TextColor color) {
        this.textGraphics.setForegroundColor(color);
        this.textGraphics.putString(col, row, text, SGR.BOLD);
        this.textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
    }

    public void drawCentered(int row, int col, String text, TextColor color) {
        this.textGraphics.setForegroundColor(color);
        this.textGraphics.putString(col - (text.length() / 2), row, text);
        this.textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
    }

    /**
     * Draws a rounded-corner panel. The title is embedded in the top border
     * on the left, and rightLabel (may be null) is embedded on the right:
     *
     * ╭ Library ─────────────── page 1/2 ╮
     * │                                  │
     * ╰──────────────────────────────────╯
     */
    public void drawBox(int row, int col, int width, int height, String title, String rightLabel, TextColor borderColor) {
        if (width < 2 || height < 2) {
            return;
        }

        String horizontal = "─".repeat(width - 2);
        draw(row, col, "╭" + horizontal + "╮", borderColor);
        draw(row + height - 1, col, "╰" + horizontal + "╯", borderColor);

        for (int r = row + 1; r < row + height - 1; r++) {
            draw(r, col, "│", borderColor);
            // clear the panel interior so old content doesn't shine through
            draw(r, col + 1, " ".repeat(width - 2));
            draw(r, col + width - 1, "│", borderColor);
        }

        if (title != null && !title.isEmpty()) {
            String label = " " + truncate(title, width - 6) + " ";
            drawBold(row, col + 2, label, borderColor);
        }

        if (rightLabel != null && !rightLabel.isEmpty()) {
            String label = " " + rightLabel + " ";
            int labelCol = col + width - 2 - label.length();
            if (labelCol > col + 2) {
                draw(row, labelCol, label, Theme.DIM);
            }
        }
    }

    /**
     * Draws a tab bar like:  Reading │ Plan to Read │ Completed │ Dropped
     * The active tab is bold in the accent color, the rest are dimmed.
     */
    public void drawTabs(int row, int col, String[] labels, int activeIndex) {
        int cursor = col;
        for (int i = 0; i < labels.length; i++) {
            String label = " " + labels[i] + " ";
            if (i == activeIndex) {
                drawBold(row, cursor, label, Theme.ACCENT_BRIGHT);
            } else {
                draw(row, cursor, label, Theme.DIM);
            }
            cursor += label.length();

            if (i < labels.length - 1) {
                draw(row, cursor, "│", Theme.DIM);
                cursor += 1;
            }
        }
    }

    /**
     * Draws one row of a list, padded to the full panel width. The selected
     * row becomes a highlight bar (accent background, dark text) with a ▌
     * marker; unselected rows are plain text.
     */
    public void drawListRow(int row, int col, int width, String text, boolean selected, TextColor color) {
        String padded = " " + truncate(text, width - 2);
        padded = padded + " ".repeat(Math.max(0, width - padded.length()));

        if (selected) {
            textGraphics.setForegroundColor(Theme.SELECTION_FG);
            textGraphics.setBackgroundColor(Theme.SELECTION_BG);
            textGraphics.putString(col, row, padded);
            textGraphics.setBackgroundColor(TextColor.ANSI.DEFAULT);
            textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
            draw(row, col, "▌", Theme.ACCENT_BRIGHT);
        } else {
            draw(row, col, padded, color);
        }
    }

    /**
     * Draws a progress gauge like ▰▰▰▰▱▱▱▱▱▱. The filled part uses the given
     * color; the empty part is dimmed. A zero/unknown total draws all-empty.
     */
    public void drawGauge(int row, int col, int width, int value, int total, TextColor color) {
        if (width <= 0) {
            return;
        }

        int filled = 0;
        if (total > 0) {
            filled = (int) Math.round((double) Math.min(value, total) / total * width);
        }

        if (filled > 0) {
            draw(row, col, "▰".repeat(filled), color);
        }
        if (filled < width) {
            draw(row, col + filled, "▱".repeat(width - filled), Theme.DIM);
        }
    }

    // Bottom bar with contextual key hints, drawn on the last terminal row.
    public void drawStatusBar(String hints) {
        int row = getRows() - 1;
        draw(row, 0, " ".repeat(getCols()));
        draw(row, 1, truncate(hints, getCols() - 2), Theme.DIM);
    }

    // Same as drawStatusBar but for one-off messages ("Added Berserk"),
    // drawn in the accent color so it stands out from the key hints.
    public void drawStatusMessage(String message, TextColor color) {
        int row = getRows() - 1;
        draw(row, 0, " ".repeat(getCols()));
        drawBold(row, 1, truncate(message, getCols() - 2), color);
    }

    public String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (maxLength <= 0) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    /**
     * Reads a line of input at the given position, echoing as the user types.
     * maxLength caps how many characters can be entered so the text can't
     * overflow its input field. Returns null if the user presses Escape.
     */
    public String readUntilEnter(int startRow, int startCol, int maxLength) throws IOException {
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
                if (inputBuffer.length() >= maxLength) {
                    continue;
                }
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
                hideCursor();
                return null;
            }
        }

        hideCursor();
        return inputBuffer.toString();
    }




    public int getRows() {
        return this.screen.getTerminalSize().getRows();
    }

    public int getCols() {
        return this.screen.getTerminalSize().getColumns();
    }
}
