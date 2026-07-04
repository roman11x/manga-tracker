import api.JikanClient;
import api.MetadataSync;
import db.Database;
import db.MangaRepository;
import ui.AppScreen;
import ui.KittyRenderer;
import ui.Router;
import ui.screens.DetailScreen;
import ui.screens.LibraryScreen;
import ui.screens.SearchScreen;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        AppScreen screen = null;

        try {
            Database.initialize();
            var connection = Database.getConnection();
            screen = new AppScreen();
            var router = new Router();
            var client = new JikanClient();
            var repo = new MangaRepository(connection);
            var metadataSync = new MetadataSync(client);
            var libraryScreen = new LibraryScreen(screen, router, repo, metadataSync);
            var detailScreen = new DetailScreen(screen, router, repo);
            var searchScreen = new SearchScreen(screen, router, client, repo);

            // backfill volumes/demographic/genres for manga added before
            // those fields existed; runs quietly in the background
            metadataSync.start();

            while (router.getCurrentScreen() != Router.ScreenState.QUIT) {
                Router.ScreenState before = router.getCurrentScreen();

                switch (before) {
                    case LIBRARY -> {
                        libraryScreen.render();

                        // while the metadata backfill is running, re-render
                        // periodically so stats fill in live instead of
                        // blocking on input with stale numbers
                        var key = screen.pollKey();
                        while (key == null) {
                            if (metadataSync.unsyncedCount() > 0) {
                                Thread.sleep(300);
                                libraryScreen.render();
                                key = screen.pollKey();
                            } else {
                                key = screen.readKey();
                            }
                        }
                        libraryScreen.handleKey(key);
                    }
                    case SEARCH -> searchScreen.render();
                    case DETAIL -> {
                        detailScreen.render();
                        detailScreen.handleKey(screen.readKey());
                    }
                }

                // leaving a screen that drew a cover: delete any floating
                // Kitty images so they can't linger over the next screen
                if (router.getCurrentScreen() != before && KittyRenderer.isSupported()) {
                    KittyRenderer.clearImages();
                    detailScreen.invalidateCover();
                }
            }

        } catch (Exception e) {
            stopScreen(screen);
            screen = null;
            System.err.println("manga-tracker crashed: " + e.getMessage());
            System.exit(1);
        } finally {
            stopScreen(screen);
        }
    }

    // restore the terminal before printing anything, or the message is lost
    // inside the alternate screen buffer
    private static void stopScreen(AppScreen screen) {
        if (screen != null) {
            try {
                screen.stop();
            } catch (IOException e) {
                System.err.println("failed to restore the terminal: " + e.getMessage());
            }
        }
    }
}
