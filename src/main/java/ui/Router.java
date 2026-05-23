package ui;

import model.Manga;
import model.Status;
// tracks which screen the user is currently on
public class Router {

    public enum ScreenState {
        MAIN_MENU,
        LIST,
        DETAIL,
        SEARCH,
        QUIT
    }

    private ScreenState currentScreen;
    private Status activeStatus;
    private Manga manga;

    public Router() {
        this.currentScreen = ScreenState.MAIN_MENU;
        this.activeStatus = null;
        this.manga = null;
    }

    public void goToMenu(){
        this.currentScreen = ScreenState.MAIN_MENU;
        this.activeStatus = null;
        this.manga = null;
    }
    public void goToList(Status status){
        this.currentScreen = ScreenState.LIST;
        this.activeStatus = status;
        this.manga = null;
    }
    public void goToDetail(Manga manga){
        this.currentScreen = ScreenState.DETAIL;
        this.activeStatus = null;
        this.manga = manga;
    }
    public void goToSearch() {
        this.currentScreen = ScreenState.SEARCH;
        this.manga = null;
    }

    public void quit() {
        this.currentScreen = ScreenState.QUIT;
        this.activeStatus = null;
        this.manga = null;
    }

    public ScreenState getCurrentScreen() {
        return currentScreen;
    }

    public Manga getManga() {
        return manga;
    }

    public Manga getSelectedManga() {
        return manga;
    }

    public Status getActiveStatus() {
        return activeStatus;
    }
}
