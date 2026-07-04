package ui.screens;

import api.MetadataSync;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import db.MangaRepository;
import model.Manga;
import model.Status;
import stats.LibraryStats;
import ui.AppScreen;
import ui.Router;
import ui.Theme;

import java.io.IOException;
import java.util.List;

/**
 * The home screen: a tab-bar dashboard in the classic ratatui style.
 *
 * ╭ MANGA TRACKER ─────────────────────────────────────╮
 * │  Reading │ Plan to Read │ Completed │ Dropped │ Stats
 * ╰─────────────────────────────────────────────────────╯
 * ╭ Library ── 12 titles ──────────────── page 1/2 ────╮
 * │ ▌ One Piece            ▰▰▰▰▰▰▰▰▱▱  1044/1100       │
 * ╰─────────────────────────────────────────────────────╯
 *  ↑↓ move  ⇥ switch tab  ↵ open  s search  q quit
 *
 * One instance handles the four status lists plus the Stats tab;
 * Tab/arrows/number keys switch between them.
 */
public class LibraryScreen {

    // The order the status tabs appear in the header; Stats is the 5th tab
    private static final Status[] TABS = {
            Status.READING,
            Status.PLAN_TO_READ,
            Status.COMPLETED,
            Status.DROPPED
    };
    private static final int STATS_TAB = TABS.length;
    private static final int TAB_COUNT = TABS.length + 1;

    private static final int HEADER_HEIGHT = 3;
    private static final int PROGRESS_WIDTH = 11;

    private final AppScreen screen;
    private final Router router;
    private final MangaRepository repo;
    private final MetadataSync metadataSync;

    private int selected = 0;
    private int page = 0;

    public LibraryScreen(AppScreen screen, Router router, MangaRepository repo, MetadataSync metadataSync) {
        this.screen = screen;
        this.router = router;
        this.repo = repo;
        this.metadataSync = metadataSync;
    }

    public void render() throws IOException {
        screen.clear();

        int cols = screen.getCols();

        // header panel with the app name and tabs
        screen.drawBox(0, 0, cols, HEADER_HEIGHT, "MANGA TRACKER", null, Theme.ACCENT);
        screen.drawTabs(1, 2, tabLabels(), activeTabIndex());

        if (router.isStatsTab()) {
            renderStats();
        } else {
            renderList();
        }

        screen.hideCursor();
        screen.refresh();
    }

    // ── the four status list tabs ──────────────────────────────────────────

    private void renderList() {
        int cols = screen.getCols();
        int rows = screen.getRows();

        List<Manga> allManga = repo.findByStatus(router.getActiveTab());
        int totalTitles = allManga.size();

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

    // ── the Stats tab ───────────────────────────────────────────────────────

    private void renderStats() {
        int cols = screen.getCols();
        int rows = screen.getRows();

        LibraryStats stats = new LibraryStats(repo.findAll());

        int areaRow = HEADER_HEIGHT;
        int maxBottom = rows - 1; // the status bar owns the last row
        int leftWidth = cols / 2;
        int rightWidth = cols - leftWidth;

        // panels are sized to their content and stacked from the top; on a
        // tall terminal the leftover space simply stays empty instead of
        // stretching three lines of numbers across a giant box
        int overviewHeight = 5;                                    // 3 number lines
        int readingHeight = stats.countFor(Status.READING) == 0 ? 3 : 4;
        int breakdownHeight = TABS.length + 2;                     // one bar per status
        int demographicsCount = Math.max(1, Math.min(stats.getDemographics().size(), 6));
        int demographicsHeight = Math.min(demographicsCount + 2,
                Math.max(3, maxBottom - (areaRow + breakdownHeight)));

        drawOverviewPanel(stats, areaRow, 0, leftWidth, overviewHeight);
        drawReadingPanel(stats, areaRow + overviewHeight, 0, leftWidth, readingHeight);
        drawBreakdownPanel(stats, areaRow, leftWidth, rightWidth, breakdownHeight);
        drawDemographicsPanel(stats, areaRow + breakdownHeight, leftWidth, rightWidth, demographicsHeight);

        int tagsRow = Math.max(areaRow + overviewHeight + readingHeight,
                areaRow + breakdownHeight + demographicsHeight);
        int tagsHeight = Math.min(6, maxBottom - tagsRow);
        if (tagsHeight >= 3) {
            drawTagsPanel(stats, tagsRow, 0, cols, tagsHeight);
        }

        String flash = router.takeFlash();
        if (flash != null) {
            screen.drawStatusMessage(flash, Theme.ACCENT_BRIGHT);
        } else if (metadataSync.unsyncedCount() > 0) {
            screen.drawStatusBar("syncing metadata for " + metadataSync.unsyncedCount()
                    + " titles…   ⇥ switch tab   s search   q quit");
        } else {
            screen.drawStatusBar("⇥ switch tab   s search   q quit");
        }
    }

    private void drawOverviewPanel(LibraryStats stats, int row, int col, int width, int height) {
        screen.drawBox(row, col, width, height, "Overview", null, Theme.ACCENT);

        String[][] lines = {
                {formatNumber(stats.getChaptersRead()), "chapters read"},
                {formatNumber(stats.getCompletedCount()), "manga completed"},
                {formatNumber(stats.getVolumesRead()), "volumes read"},
        };

        int numberWidth = 8;
        for (int i = 0; i < lines.length && i < height - 2; i++) {
            screen.drawBold(row + 1 + i, col + 2, padLeft(lines[i][0], numberWidth), Theme.ACCENT_BRIGHT);
            screen.draw(row + 1 + i, col + 2 + numberWidth + 2,
                    screen.truncate(lines[i][1], width - numberWidth - 6));
        }
    }

    private void drawBreakdownPanel(LibraryStats stats, int row, int col, int width, int height) {
        screen.drawBox(row, col, width, height, "Library · " + stats.getTotalTitles(), null, Theme.ACCENT);

        int maxCount = 0;
        for (Status status : TABS) {
            maxCount = Math.max(maxCount, stats.countFor(status));
        }

        for (int i = 0; i < TABS.length && i < height - 2; i++) {
            Status status = TABS[i];
            drawBarRow(row + 1 + i, col + 2, width - 4,
                    Theme.statusLabel(status), stats.countFor(status), maxCount,
                    Theme.statusColor(status));
        }
    }

    private void drawReadingPanel(LibraryStats stats, int row, int col, int width, int height) {
        screen.drawBox(row, col, width, height, "Reading progress", null, Theme.ACCENT);

        if (stats.countFor(Status.READING) == 0) {
            screen.draw(row + 1, col + 2, "nothing on the reading list", Theme.DIM);
            return;
        }

        int line = row + 1;
        screen.draw(line, col + 2, "avg ", Theme.DIM);
        screen.drawBold(line, col + 6, stats.getAverageReadingPercent() + "%", Theme.ACCENT_BRIGHT);
        screen.draw(line, col + 6 + String.valueOf(stats.getAverageReadingPercent()).length() + 1,
                "  · " + formatNumber(stats.getChaptersRemaining()) + " chapters left", Theme.TEXT);

        if (height > 3 && stats.getClosestToDone() != null) {
            String closest = "closest to done: " + stats.getClosestToDone().title()
                    + "  " + stats.getClosestToDone().percent() + "%";
            screen.draw(line + 1, col + 2, screen.truncate(closest, width - 4), Theme.DIM);
        }
    }

    private void drawDemographicsPanel(LibraryStats stats, int row, int col, int width, int height) {
        screen.drawBox(row, col, width, height, "Demographics", null, Theme.ACCENT);

        List<LibraryStats.TagCount> demographics = stats.getDemographics();
        if (demographics.isEmpty()) {
            screen.draw(row + 1, col + 2, "no data yet", Theme.DIM);
            return;
        }

        int maxCount = demographics.get(0).count();
        for (int i = 0; i < demographics.size() && i < height - 2; i++) {
            LibraryStats.TagCount entry = demographics.get(i);
            drawBarRow(row + 1 + i, col + 2, width - 4,
                    entry.name(), entry.count(), maxCount, Theme.ACCENT_BRIGHT);
        }
    }

    private void drawTagsPanel(LibraryStats stats, int row, int col, int width, int height) {
        screen.drawBox(row, col, width, height, "Top tags", null, Theme.ACCENT);

        int innerLines = height - 2;
        List<LibraryStats.TagCount> tags = stats.getTopTags(innerLines * 2);
        if (tags.isEmpty()) {
            screen.draw(row + 1, col + 2, "no data yet", Theme.DIM);
            return;
        }

        // two columns of tag bars inside the full-width panel
        int maxCount = tags.get(0).count();
        int columnWidth = (width - 6) / 2;
        for (int i = 0; i < tags.size(); i++) {
            int line = row + 1 + (i % innerLines);
            int column = col + 2 + (i / innerLines) * (columnWidth + 2);
            LibraryStats.TagCount entry = tags.get(i);
            drawBarRow(line, column, columnWidth, entry.name(), entry.count(), maxCount, Theme.ACCENT);
        }
    }

    // one "label ▰▰▰▰ count" row, bar scaled against the largest count.
    // The bar is capped at 40 cells so counts stay next to the bars on
    // wide terminals instead of drifting to the far edge of the panel.
    private void drawBarRow(int row, int col, int width, String label, int count, int maxCount, com.googlecode.lanterna.TextColor color) {
        int labelWidth = Math.min(14, Math.max(8, width / 3));
        int countWidth = 4;
        int gaugeWidth = Math.min(40, Math.max(0, width - labelWidth - countWidth - 3));

        screen.draw(row, col, padRight(screen.truncate(label, labelWidth), labelWidth), Theme.TEXT);
        if (gaugeWidth > 0) {
            screen.drawGauge(row, col + labelWidth + 1, gaugeWidth, count, Math.max(maxCount, 1), color);
        }
        screen.draw(row, col + labelWidth + 1 + gaugeWidth + 1, padLeft(String.valueOf(count), countWidth), Theme.DIM);
    }

    private String formatNumber(int n) {
        return String.format("%,d", n);
    }

    // ── input handling ──────────────────────────────────────────────────────

    public void handleKey(KeyStroke key) {
        KeyType type = key.getKeyType();

        if (type == KeyType.Tab || type == KeyType.ArrowRight) {
            switchTab(1);
            return;
        }
        if (type == KeyType.ReverseTab || type == KeyType.ArrowLeft) {
            switchTab(-1);
            return;
        }
        if (type == KeyType.Escape) {
            router.quit();
            return;
        }

        Character character = key.getCharacter();
        if (character != null) {
            switch (character) {
                case 'q' -> {
                    router.quit();
                    return;
                }
                case 's' -> {
                    router.goToSearch();
                    return;
                }
                case '1', '2', '3', '4' -> {
                    router.goToLibrary(TABS[character - '1']);
                    resetPosition();
                    return;
                }
                case '5' -> {
                    router.goToStats();
                    resetPosition();
                    return;
                }
                default -> {
                }
            }
        }

        // everything below only applies to the list tabs
        if (router.isStatsTab()) {
            return;
        }

        List<Manga> allManga = repo.findByStatus(router.getActiveTab());
        int pageSize = pageSize();
        List<Manga> currentPage = getCurrentPage(allManga, pageSize);
        int totalPages = getTotalPages(allManga.size(), pageSize);

        if (type == KeyType.ArrowUp) {
            selected = Math.max(selected - 1, 0);
        } else if (type == KeyType.ArrowDown) {
            selected = Math.min(selected + 1, Math.max(currentPage.size() - 1, 0));
        } else if (type == KeyType.Enter) {
            if (!currentPage.isEmpty()) {
                router.goToDetail(currentPage.get(selected));
            }
        } else if (character != null) {
            if (character == 'n' && page < totalPages - 1) {
                page++;
                selected = 0;
            } else if (character == 'p' && page > 0) {
                page--;
                selected = 0;
            }
        }
    }

    private void switchTab(int direction) {
        int next = Math.floorMod(activeTabIndex() + direction, TAB_COUNT);
        if (next == STATS_TAB) {
            router.goToStats();
        } else {
            router.goToLibrary(TABS[next]);
        }
        resetPosition();
    }

    private void resetPosition() {
        selected = 0;
        page = 0;
    }

    private int activeTabIndex() {
        if (router.isStatsTab()) {
            return STATS_TAB;
        }
        for (int i = 0; i < TABS.length; i++) {
            if (TABS[i] == router.getActiveTab()) {
                return i;
            }
        }
        return 0;
    }

    private String[] tabLabels() {
        String[] labels = new String[TAB_COUNT];
        for (int i = 0; i < TABS.length; i++) {
            labels[i] = Theme.statusLabel(TABS[i]);
        }
        labels[STATS_TAB] = "Stats";
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
