package ui.screens;

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
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detail view for a single manga: an info panel on the left (status,
 * progress gauge, actions) and the cover art framed in a panel on the right
 * when the terminal supports the Kitty graphics protocol.
 */
public class DetailScreen {

    private static final int MAX_CHAPTER_DIGITS = 6;

    private final AppScreen screen;
    private final Router router;
    private final MangaRepository repo;

    // one-shot status-bar message ("Moved back to Reading"), cleared after one render
    private String message;

    // which cover is currently on screen, so renders triggered by plain
    // keypresses don't re-upload (and blink) the same image
    private int renderedCoverId = -1;
    private int renderedCols = -1;
    private int renderedRows = -1;

    public DetailScreen(AppScreen screen, Router router, MangaRepository repo) {
        this.screen = screen;
        this.router = router;
        this.repo = repo;
    }

    public void render() throws IOException {
        screen.clear();

        Manga manga = router.getSelectedManga();

        if (manga == null) {
            screen.draw(2, 2, "No manga selected.", Theme.ERROR);
            screen.drawStatusBar("esc back");
            screen.refresh();
            return;
        }

        int cols = screen.getCols();
        int rows = screen.getRows();
        int panelHeight = Math.max(6, rows - 1);

        boolean showCover = KittyRenderer.isSupported() && cols >= 60;
        // a cover panel wide enough to hold a 2:3 poster given 1:2 cell aspect
        int coverWidth = showCover ? Math.min(cols / 2, (panelHeight - 4) * 4 / 3 + 4) : 0;
        int leftWidth = cols - coverWidth;

        screen.drawBox(0, 0, leftWidth, panelHeight, manga.getTitle(), null, Theme.ACCENT);

        int col = 3;
        screen.draw(2, col, "Status", Theme.DIM);
        screen.drawBold(2, col + 10, Theme.statusLabel(manga.getStatus()), Theme.statusColor(manga.getStatus()));

        screen.draw(4, col, "Progress", Theme.DIM);
        int gaugeWidth = Math.max(10, leftWidth - 30);
        screen.drawGauge(4, col + 10, gaugeWidth,
                manga.getChaptersRead(), manga.getTotalChapters(),
                Theme.statusColor(manga.getStatus()));
        String total = manga.getTotalChapters() > 0 ? String.valueOf(manga.getTotalChapters()) : "?";
        screen.draw(4, col + 10 + gaugeWidth + 2, manga.getChaptersRead() + "/" + total);

        String addedAt = manga.getAddedAt() == null ? "unknown" : manga.getAddedAt();
        screen.draw(6, col, "Added", Theme.DIM);
        screen.draw(6, col + 10, addedAt);

        if (manga.getDemographic() != null || manga.getGenres() != null) {
            String tags = manga.getDemographic() != null && manga.getGenres() != null
                    ? manga.getDemographic() + " · " + manga.getGenres()
                    : manga.getDemographic() != null ? manga.getDemographic() : manga.getGenres();
            screen.draw(7, col, "Tags", Theme.DIM);
            screen.draw(7, col + 10, screen.truncate(tags, leftWidth - col - 12));
        }

        int actionRow = 9;
        // a Completed manga has no next chapter; '-' stays because lowering
        // the count is how it moves back to Reading
        if (manga.getStatus() != Status.COMPLETED) {
            screen.draw(actionRow++, col, "[+] next chapter");
        }
        screen.draw(actionRow++, col, "[-] previous chapter");
        screen.draw(actionRow++, col, "[e] set exact chapter");
        if (manga.getStatus() == Status.DROPPED) {
            screen.draw(actionRow++, col, "[r] resume reading");
        } else {
            screen.draw(actionRow++, col, "[d] drop");
        }
        screen.draw(actionRow++, col, "[x] remove from tracker", Theme.ERROR);

        if (showCover) {
            screen.drawBox(0, leftWidth, coverWidth, panelHeight, "Cover", null, Theme.ACCENT);
            if (manga.getCoverPath() == null) {
                screen.drawCentered(panelHeight / 2, leftWidth + coverWidth / 2, "no cover", Theme.DIM);
            }
        }

        if (message != null) {
            screen.drawStatusMessage(message, Theme.ACCENT_BRIGHT);
            message = null;
        } else {
            String chapterKeys = manga.getStatus() == Status.COMPLETED ? "- chapter" : "+/- chapter";
            screen.drawStatusBar(chapterKeys + "   e exact   " +
                    (manga.getStatus() == Status.DROPPED ? "r resume" : "d drop") +
                    "   x remove   esc back");
        }
        screen.hideCursor();
        screen.refresh();

        if (showCover && manga.getCoverPath() != null) {
            // only re-upload the image when the manga or terminal size changed
            boolean coverStale = renderedCoverId != manga.getMalid()
                    || renderedCols != cols || renderedRows != rows;
            if (coverStale) {
                KittyRenderer.clearImages();
                // escape sequences are 1-based: inner area of the cover panel
                KittyRenderer.renderFit(manga.getCoverPath(),
                        leftWidth + 3, 3, coverWidth - 4, panelHeight - 4);
                renderedCoverId = manga.getMalid();
                renderedCols = cols;
                renderedRows = rows;
            }
        }
    }

    // Forget the on-screen cover so the next render transmits it again.
    // Called after anything deletes the terminal's images (screen switches,
    // modals) behind this screen's back.
    public void invalidateCover() {
        renderedCoverId = -1;
    }

    public void handleKey(KeyStroke key) throws IOException {
        Manga manga = router.getSelectedManga();

        if (manga == null) {
            if (key.getKeyType() == KeyType.Escape) {
                router.goToLibrary(null);
            }
            return;
        }

        if (key.getKeyType() == KeyType.Escape) {
            router.goToLibrary(manga.getStatus());
            return;
        }

        if (key.getCharacter() == null) {
            return;
        }

        char character = key.getCharacter();

        if (character == '+') {
            if (manga.getStatus() == Status.COMPLETED) {
                return; // no next chapter on a completed manga
            }
            int next = manga.getChaptersRead() + 1;
            if (manga.getTotalChapters() > 0) {
                next = Math.min(next, manga.getTotalChapters());
            }
            if (next == manga.getChaptersRead()) {
                return; // already at the final chapter
            }

            manga.setChaptersRead(next);
            repo.update(manga);
            handleUncompleteIfNeeded(manga);
            handleStartReadingIfNeeded(manga);
            handleCompletionIfNeeded(manga);

        } else if (character == '-') {
            int next = Math.max(manga.getChaptersRead() - 1, 0);
            if (next == manga.getChaptersRead()) {
                return; // already at chapter 0
            }

            manga.setChaptersRead(next);
            repo.update(manga);
            handleUncompleteIfNeeded(manga);

        } else if (character == 'e') {
            Integer chapter = readExactChapter(manga);

            if (chapter != null) {
                manga.setChaptersRead(chapter);
                repo.update(manga);
                handleUncompleteIfNeeded(manga);
                handleStartReadingIfNeeded(manga);
                handleCompletionIfNeeded(manga);
            }
        } else if (character == 'd' && manga.getStatus() != Status.DROPPED) {
            manga.setStatus(Status.DROPPED);
            repo.update(manga);
            router.goToLibrary(Status.DROPPED);
        } else if (character == 'r' && manga.getStatus() == Status.DROPPED) {
            // a dropped manga that was already fully read resumes straight
            // back to Completed, not Currently Reading
            boolean fullyRead = manga.getTotalChapters() > 0
                    && manga.getChaptersRead() >= manga.getTotalChapters();
            Status resumed = fullyRead ? Status.COMPLETED : Status.READING;

            manga.setStatus(resumed);
            repo.update(manga);
            router.setFlash(fullyRead
                    ? "Restored " + manga.getTitle() + " to Completed"
                    : "Resumed reading " + manga.getTitle());
            router.goToLibrary(resumed);
        } else if (character == 'x') {
            removeFromTracker(manga);
        }
    }

    private void removeFromTracker(Manga manga) throws IOException {
        boolean confirmed = askConfirm(
                "Remove \"" + screen.truncate(manga.getTitle(), 40) + "\" from your tracker?",
                "[y] remove    [n] keep");

        if (!confirmed) {
            return;
        }

        Status previousTab = manga.getStatus();
        repo.delete(manga.getMalid());
        deleteCachedCover(manga);
        router.setFlash("Removed " + manga.getTitle());
        router.goToLibrary(previousTab);
    }

    private void deleteCachedCover(Manga manga) {
        if (manga.getCoverPath() == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(manga.getCoverPath()));
        } catch (Exception e) {
            // a leftover cached file is harmless; don't fail the removal
        }
    }

    private Integer readExactChapter(Manga manga) throws IOException {
        StringBuilder input = new StringBuilder();

        while (true) {
            drawExactChapterPrompt(manga, input.toString());

            KeyStroke key = screen.readKey();

            if (key.getKeyType() == KeyType.Escape) {
                return null;
            }

            if (key.getKeyType() == KeyType.Enter) {
                if (input.isEmpty()) {
                    return null;
                }

                int chapter = Integer.parseInt(input.toString());
                if (manga.getTotalChapters() > 0) {
                    chapter = Math.min(chapter, manga.getTotalChapters());
                }
                return chapter;
            }

            if (key.getKeyType() == KeyType.Backspace && !input.isEmpty()) {
                input.deleteCharAt(input.length() - 1);
            } else if (key.getCharacter() != null && Character.isDigit(key.getCharacter())
                    && input.length() < MAX_CHAPTER_DIGITS) {
                input.append(key.getCharacter());
            }
        }
    }

    private void drawExactChapterPrompt(Manga manga, String input) throws IOException {
        drawModal("Set exact chapter",
                "Chapter: " + input + "_",
                "↵ confirm   esc cancel");
    }

    // A Completed manga whose progress drops below the final chapter is being
    // re-read (or the count was a misinput), so it moves back to Reading.
    private void handleUncompleteIfNeeded(Manga manga) {
        if (manga.getStatus() != Status.COMPLETED) {
            return;
        }
        if (manga.getTotalChapters() > 0 && manga.getChaptersRead() >= manga.getTotalChapters()) {
            return; // still at the final chapter
        }

        manga.setStatus(Status.READING);
        repo.update(manga);
        message = "Moved back to Reading";
    }

    private void handleCompletionIfNeeded(Manga manga) throws IOException {
        if (manga.getStatus() != Status.COMPLETED
                && manga.getTotalChapters() > 0 && manga.getChaptersRead() == manga.getTotalChapters()) {
            boolean markCompleted = askConfirm(
                    "You've reached chapter " + manga.getTotalChapters()
                            + ". Mark \"" + screen.truncate(manga.getTitle(), 40) + "\" as completed?",
                    "[y] yes    [n] keep reading");

            if (markCompleted) {
                manga.setStatus(Status.COMPLETED);
                repo.update(manga);
                router.setFlash("Completed " + manga.getTitle() + " 🎉");
                router.goToLibrary(Status.COMPLETED);
            }
        }
    }

    private void handleStartReadingIfNeeded(Manga manga) throws IOException {
        if (manga.getStatus() == Status.PLAN_TO_READ) {
            boolean startedReading = askConfirm(
                    "Move \"" + screen.truncate(manga.getTitle(), 40) + "\" to Reading?",
                    "[y] yes    [n] not yet");

            if (startedReading) {
                manga.setStatus(Status.READING);
                repo.update(manga);
            }
        }
    }

    // Generic centered y/n confirmation modal used by drop/complete/remove.
    private boolean askConfirm(String question, String hints) throws IOException {
        while (true) {
            drawModal("Confirm", question, hints);

            KeyStroke key = screen.readKey();

            if (key.getKeyType() == KeyType.Escape) {
                return false;
            }

            if (key.getCharacter() == null) {
                continue;
            }

            char character = Character.toLowerCase(key.getCharacter());

            if (character == 'y') {
                return true;
            }

            if (character == 'n') {
                return false;
            }
        }
    }

    private void drawModal(String title, String body, String hints) throws IOException {
        if (KittyRenderer.isSupported()) {
            KittyRenderer.clearImages(); // don't let the cover float over the modal
            invalidateCover();
        }
        screen.clear();

        int cols = screen.getCols();
        int rows = screen.getRows();
        int width = Math.min(Math.max(body.length() + 6, 40), cols - 4);
        int height = 5 + (body.length() + width - 5) / (width - 4); // grow with wrapped lines
        int row = Math.max(1, (rows - height) / 2);
        int col = (cols - width) / 2;

        screen.drawBox(row, col, width, height, title, null, Theme.ACCENT);

        // simple word-agnostic wrap of the body across the modal's inner width
        int innerWidth = width - 4;
        int line = 0;
        for (int i = 0; i < body.length(); i += innerWidth) {
            screen.draw(row + 2 + line, col + 2, body.substring(i, Math.min(i + innerWidth, body.length())));
            line++;
        }

        screen.draw(row + height - 2, col + 2, hints, Theme.DIM);
        screen.hideCursor();
        screen.refresh();
    }
}
