import db.Database;
import db.MangaRepository;
import model.Manga;
import model.Status;
import ui.AppScreen;
import ui.Router;
import ui.screens.DetailScreen;
import ui.screens.ListScreen;
import ui.screens.MainMenuScreen;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        AppScreen screen = null;

        try {
            Database.initialize();
            var connection = Database.getConnection();
            screen = new AppScreen();
            var router = new Router();
            var repo = new MangaRepository(connection);
            var fireForce = new Manga(25, "FireForce",250, Status.PLAN_TO_READ);
            var mainMenu = new MainMenuScreen(screen, router);
            var  detailScreen = new DetailScreen(screen, router, repo);
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
                        screen.clear();
                        screen.draw(2, 2, "Search screen is not implemented yet.");
                        screen.draw(4, 2, "Press any key to return to the main menu.");
                        screen.refresh();

                        screen.readKey();
                        router.goToMenu();

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