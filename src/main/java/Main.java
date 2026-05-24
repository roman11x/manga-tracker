import api.JikanClient;
import db.Database;
import db.MangaRepository;
import model.Manga;
import model.Status;
import ui.AppScreen;
import ui.Router;
import ui.screens.DetailScreen;
import ui.screens.ListScreen;
import ui.screens.MainMenuScreen;
import ui.screens.SearchScreen;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        AppScreen screen = null;

        try {
            Database.initialize();
            var connection = Database.getConnection();
            screen = new AppScreen();
            var router = new Router();
            var client = new JikanClient();
            var repo = new MangaRepository(connection);
            var mainMenu = new MainMenuScreen(screen, router);
            var  detailScreen = new DetailScreen(screen, router, repo);
            var searchScreen = new SearchScreen(screen, router, client, repo);
            var readingListScreen = new ListScreen(screen, router, repo, Status.READING);
            var completedListScreen = new ListScreen(screen, router, repo, Status.COMPLETED);
            var droppedListScreen = new ListScreen(screen, router, repo, Status.DROPPED);
            var planToListScreen = new ListScreen(screen, router, repo, Status.PLAN_TO_READ);
            router.goToMenu();
            while (router.getCurrentScreen() != Router.ScreenState.QUIT) {
                switch (router.getCurrentScreen()) {
                    case MAIN_MENU ->{
                        mainMenu.render();
                        mainMenu.handleKey( screen.readKey());
                    }
                    case SEARCH ->{
                        searchScreen.render();
                    }
                    case LIST ->{

                        var activeStatus = router.getActiveStatus();
                        ListScreen activeListScreen = null;
                         switch (activeStatus){
                             case READING -> activeListScreen = readingListScreen;
                             case COMPLETED -> activeListScreen = completedListScreen;
                             case DROPPED -> activeListScreen = droppedListScreen;
                             case PLAN_TO_READ -> activeListScreen = planToListScreen;
                         }
                         activeListScreen.render();
                         activeListScreen.handleKey(screen.readKey());
                    }
                    case DETAIL ->{
                        detailScreen.render();
                        detailScreen.handleKey(screen.readKey());
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            if (screen != null) {
                try {
                    screen.stop();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }


    }
}