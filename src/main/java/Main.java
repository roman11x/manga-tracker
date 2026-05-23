import com.googlecode.lanterna.input.KeyStroke;
import db.Database;
import db.MangaRepository;
import model.Status;
import ui.AppScreen;
import ui.Router;
import ui.screens.DetailScreen;
import ui.screens.ListScreen;
import ui.screens.MainMenuScreen;

import java.sql.Connection;

public class Main {

    public static void main(String[] args) {
        AppScreen screen = null;

        try {
            Database.initialize();

            Connection connection = Database.getConnection();
            MangaRepository repo = new MangaRepository(connection);

            screen = new AppScreen();
            Router router = new Router();

            MainMenuScreen mainMenuScreen = new MainMenuScreen(screen, router);

            while (router.getCurrentScreen() != Router.ScreenState.QUIT) {
                switch (router.getCurrentScreen()) {
                    case MAIN_MENU -> {
                        mainMenuScreen.render();
                        KeyStroke key = screen.readKey();
                        mainMenuScreen.handleKey(key);
                    }
                    case LIST -> {
                        Status activeStatus = router.getActiveStatus();
                        ListScreen listScreen = new ListScreen(screen, router, repo, activeStatus);

                        listScreen.render();
                        KeyStroke key = screen.readKey();
                        listScreen.handleKey(key);
                    }
                    case DETAIL -> {
                        DetailScreen detailScreen = new DetailScreen(screen, router, repo);

                        detailScreen.render();
                        KeyStroke key = screen.readKey();
                        detailScreen.handleKey(key);
                    }
                    case SEARCH -> {
                        screen.clear();
                        screen.draw(2, 2, "Search screen is not implemented yet.");
                        screen.draw(4, 2, "Press any key to return to the main menu.");
                        screen.refresh();

                        screen.readKey();
                        router.goToMenu();
                    }
                    case QUIT -> {
                    }
                }
            }

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (screen != null) {
                try {
                    screen.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}