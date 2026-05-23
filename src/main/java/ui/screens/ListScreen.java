package ui.screens;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import db.MangaRepository;
import model.Manga;
import model.Status;
import ui.AppScreen;
import ui.Router;

import java.io.IOException;
import java.util.List;

public class ListScreen {

    private static final int PAGE_SIZE = 10;

    private final AppScreen screen;
    private final Router router;
    private final MangaRepository repo;
    private final Status status;

    private int selected = 0;
    private int page = 0;

    public ListScreen(AppScreen screen, Router router, MangaRepository repo, Status status) {
        this.screen = screen;
        this.router = router;
        this.repo = repo;
        this.status = status;
    }

    public void render() throws IOException {
        screen.clear();

        List<Manga> allManga = repo.findByStatus(status);

        int totalTitles = allManga.size();
        int totalPages = getTotalPages(totalTitles);
        page = clampPage(page, totalPages);

        List<Manga> currentPage = getCurrentPage(allManga);

        if (selected >= currentPage.size()) {
            selected = Math.max(currentPage.size() - 1, 0);
        }

        String title = getTitleForStatus(status) + "  (" + totalTitles + " titles)";
        screen.draw(1, 2, title, TextColor.ANSI.YELLOW);
        screen.draw(2, 2, "─".repeat(Math.min(title.length(), screen.getCols() - 4)));

        if (currentPage.isEmpty()) {
            screen.draw(5, 4, "No manga found.");
        } else {
            for (int i = 0; i < currentPage.size(); i++) {
                Manga manga = currentPage.get(i);
                boolean isSelected = i == selected;

                String prefix = isSelected ? "> " : "  ";
                TextColor color = isSelected ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE;

                String line = prefix + formatMangaLine(manga);
                screen.draw(5 + i, 2, line, color);
            }
        }

        if (totalPages > 1) {
            screen.draw(screen.getRows() - 3, 2, "[n] next   [p] prev   page " + (page + 1) + "/" + totalPages);
        }

        screen.draw(screen.getRows() - 2, 2, "↑/↓ move   enter open   esc back");
        screen.refresh();
    }

    public void handleKey(KeyStroke key) {
        List<Manga> allManga = repo.findByStatus(status);
        List<Manga> currentPage = getCurrentPage(allManga);

        if (key.getKeyType() == KeyType.ArrowUp) {
            selected = Math.max(selected - 1, 0);
        } else if (key.getKeyType() == KeyType.ArrowDown) {
            selected = Math.min(selected + 1, Math.max(currentPage.size() - 1, 0));
        } else if (key.getKeyType() == KeyType.Enter) {
            if (!currentPage.isEmpty()) {
                router.goToDetail(currentPage.get(selected));
            }
        } else if (key.getKeyType() == KeyType.Escape) {
            router.goToMenu();
        } else if (key.getCharacter() != null) {
            handleCharacterKey(key.getCharacter(), allManga.size());
        }
    }

    private void handleCharacterKey(char character, int totalItems) {
        int totalPages = getTotalPages(totalItems);

        if (character == 'n' && page < totalPages - 1) {
            page++;
            selected = 0;
        } else if (character == 'p' && page > 0) {
            page--;
            selected = 0;
        }
    }

    private List<Manga> getCurrentPage(List<Manga> allManga) {
        int fromIndex = page * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allManga.size());

        if (fromIndex >= allManga.size()) {
            return List.of();
        }

        return allManga.subList(fromIndex, toIndex);
    }

    private int getTotalPages(int totalItems) {
        if (totalItems == 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalItems / PAGE_SIZE);
    }

    private int clampPage(int currentPage, int totalPages) {
        return Math.max(0, Math.min(currentPage, totalPages - 1));
    }

    private String formatMangaLine(Manga manga) {
        return manga.getTitle()
                + " [ch "
                + manga.getChaptersRead()
                + "/"
                + manga.getTotalChapters()
                + "]";
    }

    private String getTitleForStatus(Status status) {
        return switch (status) {
            case READING -> "Currently reading";
            case PLAN_TO_READ -> "Plan to read";
            case COMPLETED -> "Completed";
            case DROPPED -> "Dropped";
        };
    }
}
