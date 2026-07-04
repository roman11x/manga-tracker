package ui;

import com.googlecode.lanterna.TextColor;
import model.Status;

/**
 * Central color palette for the TUI so every screen looks consistent.
 *
 * ACCENT is used for borders, the active tab, and highlights.
 * DIM is used for secondary text such as key hints and inactive tabs.
 * Each reading status also has its own color, used for gauges and labels.
 */
public final class Theme {

    public static final TextColor ACCENT = TextColor.ANSI.CYAN;
    public static final TextColor ACCENT_BRIGHT = TextColor.ANSI.CYAN_BRIGHT;
    public static final TextColor TEXT = TextColor.ANSI.WHITE;
    public static final TextColor DIM = TextColor.ANSI.BLACK_BRIGHT;
    public static final TextColor ERROR = TextColor.ANSI.RED_BRIGHT;

    // Colors for the selected-row highlight bar
    public static final TextColor SELECTION_BG = TextColor.ANSI.CYAN;
    public static final TextColor SELECTION_FG = TextColor.ANSI.BLACK;

    private Theme() {
    }

    public static TextColor statusColor(Status status) {
        return switch (status) {
            case READING -> TextColor.ANSI.GREEN_BRIGHT;
            case PLAN_TO_READ -> TextColor.ANSI.YELLOW_BRIGHT;
            case COMPLETED -> TextColor.ANSI.BLUE_BRIGHT;
            case DROPPED -> TextColor.ANSI.RED_BRIGHT;
        };
    }

    public static String statusLabel(Status status) {
        return switch (status) {
            case READING -> "Reading";
            case PLAN_TO_READ -> "Plan to Read";
            case COMPLETED -> "Completed";
            case DROPPED -> "Dropped";
        };
    }
}
