package ui;

import model.Manga;
import model.Status;

// tracks which screen the user is currently on
public class Router {

    public enum ScreenState {
        LIBRARY,
        DETAIL,
        SEARCH,
        QUIT
    }

    private ScreenState currentScreen;
    private Status activeTab; // which status tab the library is showing
    private boolean statsTab; // true when the library shows the Stats tab instead of a list
    private Manga manga;
    private String flashMessage; // one-shot message shown in the status bar

    public Router() {
        this.currentScreen = ScreenState.LIBRARY;
        this.activeTab = Status.READING;
        this.manga = null;
    }

    public void goToLibrary(Status tab) {
        this.currentScreen = ScreenState.LIBRARY;
        if (tab != null) {
            this.activeTab = tab;
        }
        this.statsTab = false;
        this.manga = null;
    }

    public void goToStats() {
        this.currentScreen = ScreenState.LIBRARY;
        this.statsTab = true;
        this.manga = null;
    }

    public boolean isStatsTab() {
        return statsTab;
    }

    public void goToDetail(Manga manga) {
        this.currentScreen = ScreenState.DETAIL;
        this.manga = manga;
    }

    public void goToSearch() {
        this.currentScreen = ScreenState.SEARCH;
        this.manga = null;
    }

    public void quit() {
        this.currentScreen = ScreenState.QUIT;
        this.manga = null;
    }

    public ScreenState getCurrentScreen() {
        return currentScreen;
    }

    public Manga getSelectedManga() {
        return manga;
    }

    public Status getActiveTab() {
        return activeTab;
    }

    // Flash messages survive exactly one read, so the next screen can show
    // them once in its status bar ("Added Blame!") and then drop them.
    public void setFlash(String message) {
        this.flashMessage = message;
    }

    public String takeFlash() {
        String message = flashMessage;
        flashMessage = null;
        return message;
    }
}
