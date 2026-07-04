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
import ui.KittyRenderer;
import ui.Router;
import ui.Theme;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Search flow: type a query, pick from the results with the arrow keys, and
 * press Enter to add the highlighted manga to the Plan-to-Read list.
 *
 * While browsing, the highlighted result's cover is downloaded in the
 * background and shown in a panel on the right (Kitty-protocol terminals
 * only), so the cover you see is the one that gets saved.
 */
public class SearchScreen {

    private static final int HEADER_HEIGHT = 3;
    private static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    private final AppScreen screen;
    private final Router router;
    private final JikanClient client;
    private final MangaRepository repo;
    private final CoverCache coverCache;

    // written by background download threads, read by the UI loop
    private final Map<Integer, String> coverPaths = new ConcurrentHashMap<>();
    private final Set<Integer> coverFailures = ConcurrentHashMap.newKeySet();
    private final Set<Integer> coverInFlight = ConcurrentHashMap.newKeySet();

    public SearchScreen(AppScreen screen, Router router, JikanClient client, MangaRepository repo) throws IOException {
        this.screen = screen;
        this.router = router;
        this.client = client;
        this.repo = repo;
        this.coverCache = new CoverCache();
    }

    public void render() throws Exception {
        String query = promptForQuery();

        // Escape or an empty query means the user doesn't want to search
        if (query == null || query.isBlank()) {
            router.goToLibrary(null);
            return;
        }

        List<MangaResult> results = runSearch(query);

        if (results == null) {
            // cancelled or failed; runSearch already showed the message
            router.goToLibrary(null);
            return;
        }

        if (results.isEmpty()) {
            drawSearchFrame(query);
            screen.drawCentered(screen.getRows() / 2, screen.getCols() / 2,
                    "No results for \"" + query + "\"", Theme.DIM);
            screen.drawStatusBar("press any key to go back");
            screen.hideCursor();
            screen.refresh();
            screen.readKey();
            router.goToLibrary(null);
            return;
        }

        browseResults(query, results);
    }

    // Phase 1: bordered input field at the top, cursor inside it
    private String promptForQuery() throws IOException {
        screen.clear();
        drawInputBox("");
        screen.drawStatusBar("type a title   ↵ search   esc cancel");
        screen.refresh();

        return screen.readUntilEnter(1, 5, Math.max(10, screen.getCols() - 8));
    }

    // Phase 2: spinner while the Jikan request runs; Escape cancels
    private List<MangaResult> runSearch(String query) throws Exception {
        CompletableFuture<List<MangaResult>> future = client.searchManga(query);

        int spinIndex = 0;
        while (!future.isDone()) {
            drawSearchFrame(query);
            screen.drawCentered(screen.getRows() / 2, screen.getCols() / 2,
                    SPINNER[spinIndex % SPINNER.length] + " Searching for \"" + query + "\"…", Theme.ACCENT_BRIGHT);
            screen.drawStatusBar("esc cancel");
            screen.hideCursor();
            screen.refresh();
            spinIndex++;

            KeyStroke key = screen.pollKey();
            if (key != null && key.getKeyType() == KeyType.Escape) {
                future.cancel(true);
                return null;
            }

            // sleep so the spinner doesn't spin the CPU out of control
            Thread.sleep(80);
        }

        try {
            return future.get();
        } catch (Exception e) {
            drawSearchFrame(query);
            screen.drawCentered(screen.getRows() / 2, screen.getCols() / 2,
                    "Search failed — check your connection", Theme.ERROR);
            screen.drawStatusBar("press any key to go back");
            screen.hideCursor();
            screen.refresh();
            screen.readKey();
            return null;
        }
    }

    // Phase 3: arrow-key browsing with async cover previews
    private void browseResults(String query, List<MangaResult> results) throws Exception {
        boolean[] tracked = new boolean[results.size()];
        for (int i = 0; i < results.size(); i++) {
            tracked[i] = repo.findByMalId(results.get(i).getMalId()).isPresent();
        }

        int selected = 0;
        int renderedCoverId = -1;
        String message = null;
        boolean dirty = true;

        startCoverDownload(results.get(selected));

        while (true) {
            MangaResult current = results.get(selected);

            if (dirty) {
                drawBrowse(query, results, tracked, selected, message);
                dirty = false;
            }

            // paint the cover once its background download lands
            if (coverPanelVisible() && renderedCoverId != current.getMalId()
                    && coverPaths.containsKey(current.getMalId())) {
                drawBrowse(query, results, tracked, selected, message);
                paintCover(coverPaths.get(current.getMalId()));
                renderedCoverId = current.getMalId();
            }

            KeyStroke key = screen.pollKey();
            if (key == null) {
                Thread.sleep(40);
                continue;
            }

            message = null;

            if (key.getKeyType() == KeyType.Escape) {
                clearCover();
                router.goToLibrary(null);
                return;
            }

            if (key.getKeyType() == KeyType.ArrowUp || key.getKeyType() == KeyType.ArrowDown) {
                int direction = key.getKeyType() == KeyType.ArrowUp ? -1 : 1;
                int next = Math.clamp(selected + direction, 0, results.size() - 1);

                if (next != selected) {
                    selected = next;
                    renderedCoverId = -1;
                    clearCover(); // drop the old cover so it can't sit next to the wrong result
                    startCoverDownload(results.get(selected));
                }
                dirty = true;
                continue;
            }

            if (key.getKeyType() == KeyType.Enter) {
                if (tracked[selected]) {
                    message = "Already in your tracker";
                    dirty = true;
                    continue;
                }

                if (addToLibrary(current)) {
                    return;
                }

                message = "Could not add " + current.getTitle();
                dirty = true;
            }
        }
    }

    /**
     * Saves the manga (with its cached cover) and returns to the library on
     * the Plan-to-Read tab. Returns false if the insert failed so the user
     * can keep browsing instead of crashing the app.
     */
    private boolean addToLibrary(MangaResult result) {
        String coverPath = coverPaths.get(result.getMalId());
        if (coverPath == null && result.getCoverPath() != null) {
            try {
                coverPath = coverCache.download(result.getMalId(), result.getCoverPath());
            } catch (Exception e) {
                coverPath = null; // a missing cover shouldn't block adding the manga
            }
        }

        var manga = new Manga(result.getMalId(), result.getTitle(),
                result.getTotalChapters(), Status.PLAN_TO_READ);
        manga.setCoverPath(coverPath);

        try {
            repo.insert(manga);
        } catch (Exception e) {
            return false;
        }

        clearCover();
        router.setFlash("Added " + result.getTitle() + " to Plan to Read");
        router.goToLibrary(Status.PLAN_TO_READ);
        return true;
    }

    // kicks off a cover download for the given result if we don't have it yet
    private void startCoverDownload(MangaResult result) {
        if (!coverPanelVisible() || result.getCoverPath() == null) {
            return;
        }

        int malId = result.getMalId();
        if (coverPaths.containsKey(malId) || coverFailures.contains(malId) || !coverInFlight.add(malId)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String path = coverCache.download(malId, result.getCoverPath());
                if (path != null) {
                    coverPaths.put(malId, path);
                } else {
                    coverFailures.add(malId);
                }
            } catch (Exception e) {
                coverFailures.add(malId);
            } finally {
                coverInFlight.remove(malId);
            }
        });
    }

    // Drawing helpers

    private void drawInputBox(String query) {
        screen.drawBox(0, 0, screen.getCols(), HEADER_HEIGHT, "Search MyAnimeList", null, Theme.ACCENT);
        screen.draw(1, 3, "❯", Theme.ACCENT_BRIGHT);
        if (!query.isEmpty()) {
            screen.draw(1, 5, screen.truncate(query, screen.getCols() - 8));
        }
    }

    // input box + empty body; used as the backdrop for spinner/messages
    private void drawSearchFrame(String query) {
        screen.clear();
        drawInputBox(query);
    }

    private void drawBrowse(String query, List<MangaResult> results, boolean[] tracked, int selected, String message) throws IOException {
        screen.clear();
        drawInputBox(query);

        int cols = screen.getCols();
        int rows = screen.getRows();
        int panelRow = HEADER_HEIGHT;
        int panelHeight = Math.max(4, rows - HEADER_HEIGHT - 1);

        int coverWidth = coverPanelVisible() ? Math.min(cols / 2, (panelHeight - 4) * 4 / 3 + 4) : 0;
        int leftWidth = cols - coverWidth;

        screen.drawBox(panelRow, 0, leftWidth, panelHeight,
                "Results · " + results.size(), null, Theme.ACCENT);

        int innerWidth = leftWidth - 2;
        int visible = Math.min(results.size(), panelHeight - 2);

        for (int i = 0; i < visible; i++) {
            MangaResult result = results.get(i);

            String chapters = result.getTotalChapters() > 0 ? result.getTotalChapters() + " ch" : "? ch";
            String suffix = tracked[i] ? "  [tracked]" : "";
            int titleWidth = Math.max(8, innerWidth - 12 - suffix.length());

            String line = padRight(screen.truncate(result.getTitle(), titleWidth), titleWidth)
                    + padLeft(chapters, 8) + suffix;

            screen.drawListRow(panelRow + 1 + i, 1, innerWidth, line, i == selected,
                    tracked[i] ? Theme.DIM : Theme.TEXT);
        }

        if (coverWidth > 0) {
            screen.drawBox(panelRow, leftWidth, coverWidth, panelHeight, "Cover", null, Theme.ACCENT);

            int malId = results.get(selected).getMalId();
            String placeholder = null;
            if (coverFailures.contains(malId) || results.get(selected).getCoverPath() == null) {
                placeholder = "no cover";
            } else if (!coverPaths.containsKey(malId)) {
                placeholder = "fetching cover…";
            }
            if (placeholder != null) {
                screen.drawCentered(panelRow + panelHeight / 2, leftWidth + coverWidth / 2, placeholder, Theme.DIM);
            }
        }

        if (message != null) {
            screen.drawStatusMessage(message, Theme.ERROR);
        } else {
            screen.drawStatusBar("↑↓ move   ↵ add to library   esc back");
        }

        screen.hideCursor();
        screen.refresh();
    }

    private void paintCover(String path) throws IOException {
        int cols = screen.getCols();
        int rows = screen.getRows();
        int panelHeight = Math.max(4, rows - HEADER_HEIGHT - 1);
        int coverWidth = Math.min(cols / 2, (panelHeight - 4) * 4 / 3 + 4);
        int leftWidth = cols - coverWidth;

        KittyRenderer.clearImages();
        // escape sequences are 1-based: inner area of the cover panel
        KittyRenderer.renderFit(path, leftWidth + 3, HEADER_HEIGHT + 3, coverWidth - 4, panelHeight - 4);
    }

    private void clearCover() {
        if (KittyRenderer.isSupported()) {
            KittyRenderer.clearImages();
        }
    }

    private boolean coverPanelVisible() {
        return KittyRenderer.isSupported() && screen.getCols() >= 70;
    }

    private String padRight(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }

    private String padLeft(String text, int width) {
        return " ".repeat(Math.max(0, width - text.length())) + text;
    }
}
