package ui.screens;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import db.MangaRepository;
import model.Manga;
import model.Status;
import ui.AppScreen;
import ui.Router;
import ui.Theme;

import java.io.IOException;
import java.util.List;

/**
 * The home screen: a tab-bar dashboard in the classic ratatui style.
 *
 * ╭ MANGA TRACKER ─────────────────────────────────────╮
 * │  Reading │ Plan to Read │ Completed │ Dropped      │
 * ╰─────────────────────────────────────────────────────╯
 * ╭ Library ── 12 titles ──────────────── page 1/2 ────╮
 * │ ▌ One Piece            ▰▰▰▰▰▰▰▰▱▱  1044/1100       │
 * ╰─────────────────────────────────────────────────────╯
 *  ↑↓ move  ⇥ switch tab  ↵ open  s search  q quit
 *
 * One instance handles all four status lists; Tab/arrows switch between them.
 */
public class LibraryScreen {

    // The order the tabs appear in the header
    private static final Status[] TABS = {
            Status.READING,
            Status.PLAN_TO_READ,
            Status.COMPLETED,
            Status.DROPPED
    };

    private static final int HEADER_HEIGHT = 3;
    private static final int PROGRESS_WIDTH = 11;

    private final AppScreen screen;
    private final Router router;
    private final MangaRepository repo;

    private int selected = 0;
    private int page = 0;

    public LibraryScreen(AppScreen screen, Router router, MangaRepository repo) {
        this.screen = screen;
        this.router = router;
        this.repo = repo;
    }

    public void render() throws IOException {
        screen.clear();

        int cols = screen.getCols();
        int rows = screen.getRows();

        List<Manga> allManga = repo.findByStatus(router.getActiveTab());
        int totalTitles = allManga.size();

        // header panel with the app name and status tabs
        screen.drawBox(0, 0, cols, HEADER_HEIGHT, "MANGA TRACKER", null, Theme.ACCENT);
        screen.drawTabs(1, 2, tabLabels(), activeTabIndex());

        // library panel fills everything between the header and the status bar
        int panelRow = HEADER_HEIGHT;
        int panelHeight = Math.max(3, rows - HEADER_HEIGHT - 1);
        int pageSize = pageSize();

        int totalPages = getTotalPages(totalTitles, pageSize);
        page = clampPage(page, totalPages);
        List<Manga> currentPage = getCurrentPage(allManga, pageSize);

        if (selected >= currentPage.size()) {
            selected = Math.max(currentPage.size() - 1, 0);
        }

        String panelTitle = "Library · " + totalTitles + (totalTitles == 1 ? " title" : " titles");
        String pageLabel = totalPages > 1 ? "n/p page " + (page + 1) + "/" + totalPages : null;
        screen.drawBox(panelRow, 0, cols, panelHeight, panelTitle, pageLabel, Theme.ACCENT);

        if (currentPage.isEmpty()) {
            screen.drawCentered(panelRow + panelHeight / 2, cols / 2,
                    "Nothing here yet — press s to search for manga", Theme.DIM);
        } else {
            drawRows(currentPage, panelRow + 1, cols);
        }

        String flash = router.takeFlash();
        if (flash != null) {
            screen.drawStatusMessage(flash, Theme.ACCENT_BRIGHT);
        } else {
            screen.drawStatusBar("↑↓ move   ⇥ switch tab   ↵ open   s search   q quit");
        }

        screen.hideCursor();
        screen.refresh();
    }

    private void drawRows(List<Manga> currentPage, int firstRow, int cols) {
        int innerWidth = cols - 2;
        int gaugeWidth = innerWidth >= 50 ? Math.min(14, innerWidth / 5) : 0;
        int titleWidth = innerWidth - gaugeWidth - PROGRESS_WIDTH - 6;

        for (int i = 0; i < currentPage.size(); i++) {
            Manga manga = currentPage.get(i);
            boolean isSelected = i == selected;
            int rowY = firstRow + i;

            String title = padRight(screen.truncate(manga.getTitle(), titleWidth), titleWidth);
            String progress = padLeft(formatProgress(manga), PROGRESS_WIDTH);
            String gauge = gaugeWidth > 0 ? " ".repeat(gaugeWidth) : "";
            String line = title + "  " + gauge + "  " + progress;

            screen.drawListRow(rowY, 1, innerWidth, line, isSelected, Theme.TEXT);

            // colored gauge + dim progress only make sense outside the
            // highlight bar; the selected row stays monochrome on accent
            if (!isSelected && gaugeWidth > 0) {
                int gaugeCol = 2 + titleWidth + 2;
                screen.drawGauge(rowY, gaugeCol, gaugeWidth,
                        manga.getChaptersRead(), manga.getTotalChapters(),
                        Theme.statusColor(manga.getStatus()));
                screen.draw(rowY, gaugeCol + gaugeWidth + 2, progress, Theme.DIM);
            }
        }
    }

    public void handleKey(KeyStroke key) {
        List<Manga> allManga = repo.findByStatus(router.getActiveTab());
        int pageSize = pageSize();
        List<Manga> currentPage = getCurrentPage(allManga, pageSize);
        int totalPages = getTotalPages(allManga.size(), pageSize);

        KeyType type = key.getKeyType();

        if (type == KeyType.ArrowUp) {
            selected = Math.max(selected - 1, 0);
        } else if (type == KeyType.ArrowDown) {
            selected = Math.min(selected + 1, Math.max(currentPage.size() - 1, 0));
        } else if (type == KeyType.Tab || type == KeyType.ArrowRight) {
            switchTab(1);
        } else if (type == KeyType.ReverseTab || type == KeyType.ArrowLeft) {
            switchTab(-1);
        } else if (type == KeyType.Enter) {
            if (!currentPage.isEmpty()) {
                router.goToDetail(currentPage.get(selected));
            }
        } else if (type == KeyType.Escape) {
            router.quit();
        } else if (key.getCharacter() != null) {
            handleCharacterKey(key.getCharacter(), totalPages);
        }
    }

    private void handleCharacterKey(char character, int totalPages) {
        switch (character) {
            case 'q' -> router.quit();
            case 's' -> router.goToSearch();
            case 'n' -> {
                if (page < totalPages - 1) {
                    page++;
                    selected = 0;
                }
            }
            case 'p' -> {
                if (page > 0) {
                    page--;
                    selected = 0;
                }
            }
            case '1', '2', '3', '4' -> {
                router.goToLibrary(TABS[character - '1']);
                resetPosition();
            }
            default -> {
            }
        }
    }

    private void switchTab(int direction) {
        int next = Math.floorMod(activeTabIndex() + direction, TABS.length);
        router.goToLibrary(TABS[next]);
        resetPosition();
    }

    private void resetPosition() {
        selected = 0;
        page = 0;
    }

    private int activeTabIndex() {
        for (int i = 0; i < TABS.length; i++) {
            if (TABS[i] == router.getActiveTab()) {
                return i;
            }
        }
        return 0;
    }

    private String[] tabLabels() {
        String[] labels = new String[TABS.length];
        for (int i = 0; i < TABS.length; i++) {
            labels[i] = Theme.statusLabel(TABS[i]);
        }
        return labels;
    }

    // how many list rows fit inside the library panel at the current size
    private int pageSize() {
        int panelHeight = Math.max(3, screen.getRows() - HEADER_HEIGHT - 1);
        return Math.max(1, panelHeight - 2);
    }

    private String formatProgress(Manga manga) {
        String total = manga.getTotalChapters() > 0 ? String.valueOf(manga.getTotalChapters()) : "?";
        return manga.getChaptersRead() + "/" + total;
    }

    private List<Manga> getCurrentPage(List<Manga> allManga, int pageSize) {
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allManga.size());

        if (fromIndex >= allManga.size()) {
            return List.of();
        }

        return allManga.subList(fromIndex, toIndex);
    }

    private int getTotalPages(int totalItems, int pageSize) {
        if (totalItems == 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalItems / pageSize);
    }

    private int clampPage(int currentPage, int totalPages) {
        return Math.max(0, Math.min(currentPage, totalPages - 1));
    }

    private String padRight(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }

    private String padLeft(String text, int width) {
        return " ".repeat(Math.max(0, width - text.length())) + text;
    }
}
