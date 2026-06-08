package ui.screens;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import db.MangaRepository;
import model.Manga;
import model.Status;
import ui.AppScreen;
import ui.KittyRenderer;
import ui.Router;

import java.io.IOException;

public class DetailScreen {

    private final AppScreen screen;
    private final Router router;
    private final MangaRepository repo;

    public DetailScreen(AppScreen screen, Router router, MangaRepository repo) {
        this.screen = screen;
        this.router = router;
        this.repo = repo;
    }

    public void render() throws IOException {
        screen.clear();

        Manga manga = router.getSelectedManga();

        if (manga == null) {
            screen.draw(2, 2, "No manga selected.", TextColor.ANSI.RED);
            screen.draw(4, 2, "[esc] back");
            screen.refresh();
            return;
        }

        screen.draw(1, 2, manga.getTitle(), TextColor.ANSI.YELLOW);
        screen.draw(2, 2, "─".repeat(Math.min(manga.getTitle().length(), screen.getCols() - 4)));

        screen.draw(4, 2, "Status:   " + manga.getStatus());
        screen.draw(5, 2, "Progress: " + manga.getChaptersRead() + " / " + manga.getTotalChapters());

        String addedAt = manga.getAddedAt() == null ? "unknown" : manga.getAddedAt();
        screen.draw(6, 2, "Added:    " + addedAt);

        screen.draw(9, 2, "[+] next chapter");
        screen.draw(10, 2, "[e] set exact chapter");
        screen.draw(11, 2, "[d] drop");

        if (manga.getStatus() == Status.DROPPED) {
            screen.draw(12, 2, "[r] resume reading");
            screen.draw(13, 2, "[esc] back");
        } else {
            screen.draw(12, 2, "[esc] back");
        }

        screen.hideCursor();
        screen.refresh();

        if (KittyRenderer.isSupported() && manga.getCoverPath() != null) {
            KittyRenderer.render(manga.getCoverPath(), 45, 2, 24, 16);
        }

    }

    public void handleKey(KeyStroke key) throws IOException {
        Manga manga = router.getSelectedManga();

        if (manga == null) {
            if (key.getKeyType() == KeyType.Escape) {
                router.goToMenu();
            }
            return;
        }

        if (key.getKeyType() == KeyType.Escape) {
            router.goToList(router.getActiveStatus());
            return;
        }

        if (key.getCharacter() == null) {
            return;
        }

        char character = key.getCharacter();

        if (character == '+') {
            manga.setChaptersRead(manga.getChaptersRead() + 1);
            repo.update(manga);
            handleStartReadingIfNeeded(manga);
            handleCompletionIfNeeded(manga);

        } else if (character == 'e') {
            Integer chapter = readExactChapter(manga);

            if (chapter != null) {
                manga.setChaptersRead(chapter);
                repo.update(manga);
                handleStartReadingIfNeeded(manga);
                handleCompletionIfNeeded(manga);
            }
        } else if (character == 'd') {
            manga.setStatus(Status.DROPPED);
            repo.update(manga);
            router.goToList(Status.DROPPED);
        } else if (character == 'r' && manga.getStatus() == Status.DROPPED) {
            manga.setStatus(Status.READING);
            repo.update(manga);
            router.goToList(Status.READING);
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

                return Integer.parseInt(input.toString());
            }

            if (key.getKeyType() == KeyType.Backspace && !input.isEmpty()) {
                input.deleteCharAt(input.length() - 1);
            } else if (key.getCharacter() != null && Character.isDigit(key.getCharacter())) {
                input.append(key.getCharacter());
            }
        }
    }

    private void drawExactChapterPrompt(Manga manga, String input) throws IOException {
        screen.clear();

        screen.draw(2, 2, "Set exact chapter for " + manga.getTitle(), TextColor.ANSI.YELLOW);
        screen.draw(4, 2, "Chapter: " + input + "_");
        screen.draw(6, 2, "enter confirm   esc cancel");

        screen.hideCursor();
        screen.refresh();
    }

    private void handleCompletionIfNeeded(Manga manga) throws IOException {
        if (manga.getTotalChapters() > 0 && manga.getChaptersRead() == manga.getTotalChapters()) {
            boolean markCompleted = askCompletionPrompt(manga);

            if (markCompleted) {
                manga.setStatus(Status.COMPLETED);
                repo.update(manga);
                router.goToList(Status.COMPLETED);
            }
        }
    }
    private void handleStartReadingIfNeeded(Manga manga) throws IOException {
        if (manga.getStatus() == Status.PLAN_TO_READ) {

            boolean startedReading = askStartedReadingPrompt(manga);
            if(startedReading) {
                manga.setStatus(Status.READING);
                repo.update(manga);
                router.goToList(Status.READING);
            }
        }
    }
    private boolean askStartedReadingPrompt(Manga manga) throws IOException {
        while (true) {
            screen.clear();

            screen.draw(3, 2, "Have You Started reading Manga? " + manga.getTitle() + ".");
            screen.draw(4, 2, "[y] yes    [n] no");

            screen.hideCursor();
            screen.refresh();

            KeyStroke key = screen.readKey();

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



    private boolean askCompletionPrompt(Manga manga) throws IOException {
        while (true) {
            screen.clear();

            screen.draw(3, 2, "You've reached chapter " + manga.getTotalChapters() + ".");
            screen.draw(4, 2, "Mark " + manga.getTitle() + " as completed?");
            screen.draw(6, 2, "[y] yes    [n] keep reading");

            screen.hideCursor();
            screen.refresh();

            KeyStroke key = screen.readKey();

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
}
