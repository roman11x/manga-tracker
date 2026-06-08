package ui.screens;

import api.JikanClient;
import api.MangaResult;
import cache.CoverCache;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import db.MangaRepository;
import model.Manga;
import model.Status;
import ui.AppScreen;
import ui.Router;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SearchScreen {

    private final AppScreen screen;
    private final Router router;
    private final JikanClient client;
    private final MangaRepository repo;

    public SearchScreen(AppScreen screen, Router router, JikanClient client, MangaRepository repo) {
        this.screen = screen;
        this.router = router;
        this.client = client;
        this.repo = repo;
    }
    public void render() throws Exception {
        screen.clear();

        screen.draw(2, 2, "Search for a manga: ");
        screen.refresh();
        //get the user's query
        String query = screen.readUntilEnter(2, 22);
        // if the user pressed escpade the query is null, and they do not want to search
        if (query == null) {
            router.goToMenu();
            return;
        }

        //start the network request in the background
        CompletableFuture<List<MangaResult>> future = client.searchManga(query);

        // the spinner loop
        // draw the spinner as long as the future is not done yet
        int spinIndex = 0;
        String[] spinners = new String[]{"|", "/", "-", "\\"};

        while (!future.isDone()) {
            screen.draw(2, 4, "Searching for: " + query + " " + spinners[spinIndex % spinners.length]);
            screen.refresh();
            spinIndex++;

            // sleep for 100 milliseconds so the thread doesn't go out of control
            Thread.sleep(100);
        }
        // the future is done, get the results
        List<MangaResult> results = future.get();
        // display the results
        screen.clear();
        screen.draw(2, 2, "Search results for: " + query);
        for (int i = 0; i < results.size(); i++) {
            MangaResult manga = results.get(i);
            screen.draw(4 + i , 2, (i + 1) + ". " + manga.getTitle() + " (" + manga.getTotalChapters() + " pages)");
        }

        screen.draw(16, 2, "Press 1-9 to select, or ESC to cancel.");
        screen.refresh();
        // Wait for the user to make a choice
       // screen.readUntilEnter(2, 22);screen.readUntilEnter(20, 2);screen.readUntilEnter(20, 2);
        handleSelection(results);


    }

    public void handleSelection(List<MangaResult> results) throws Exception {
        while (true) {
            KeyStroke key = screen.readKey();

            if (key.getKeyType() == KeyType.Escape) {
                router.goToMenu();
                break;
            }

            if (key.getKeyType() == KeyType.Character) {
                char c = key.getCharacter();
                //check if the user pressed a number between 1 and 9
                if(Character.isDigit(c)) {
                    int choiceIndex = Character.getNumericValue(c) - 1;

                    //ensure they didn't press a number higher than our list size
                    if (choiceIndex >= 0 && choiceIndex < results.size()){
                        //get the JSON manga result from the list provided by Jikan
                        MangaResult mangaResult = results.get(choiceIndex);
                        // map the temporary api result into a permanent database record
                        var manga = new Manga(mangaResult.getMalId(), mangaResult.getTitle(),
                                mangaResult.getTotalChapters(), Status.PLAN_TO_READ);
                        // download the cover image, save it to the cache, and get the path to the cached image
                        var cache = new CoverCache();
                        String imagePath = cache.download(mangaResult.getMalId(), mangaResult.getCoverPath());
                        manga.setCoverPath(imagePath);
                        // save the manga to SQLite
                        repo.insert(manga);
                        router.goToMenu();
                        break;
                    }
                }
            }

        }

    }







}
