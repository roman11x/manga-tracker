package ui.screens;

import com.googlecode.lanterna.TextColor;
import ui.AppScreen;
import ui.Router;

import java.io.IOException;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import model.Status;

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

    public void render() throws IOException {
        screen.clear();

        screen.generateTitle();

        int startRow = 4;
        int col = 4;

        for (int i = 0; i < labels.length; i++) {
            boolean isSelected = i == selected;

            String prefix = isSelected ? "> " : "  ";
            TextColor color = isSelected ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE;

            screen.draw(startRow + i, col, prefix + labels[i], color);
        }

        screen.draw(screen.getRows() - 2, 2, "↑/↓ move   enter select");
        screen.refresh();
    }

    public void handleKey(KeyStroke key) {
        if (key.getKeyType() == KeyType.ArrowUp) {
            selected = Math.max(selected - 1, 0);
        } else if (key.getKeyType() == KeyType.ArrowDown) {
            selected = Math.min(selected + 1, labels.length - 1);
        } else if (key.getKeyType() == KeyType.Enter) {
            handleEnter();
        }
    }

    private void handleEnter() {
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
