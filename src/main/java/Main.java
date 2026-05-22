import db.Database;
import db.DatabaseException;
import db.MangaRepository;
import model.Manga;
import model.Status;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Temporary entry point used for testing the basic Manga model behavior.
 *
 * This class will later be replaced or expanded with the terminal user
 * interface that lets users search for manga, add them to their list,
 * and update their reading progress.
 */
public class Main {

    public static void main(String[] args) {
        try {
            Database.initialize();

            try (Connection connection = Database.getConnection()) {
                MangaRepository mangaRepository = new MangaRepository(connection);

                Manga dorohedoro = new Manga(1, "Dorohedoro", 165, Status.READING);
                dorohedoro.setChaptersRead(20);

                Manga tokyoGhoul = new Manga(2, "Tokyo Ghoul", 143, Status.PLAN_TO_READ);

                // Clean up old test data if this program was run before.
                deleteIfExists(mangaRepository, dorohedoro.getMalid());
                deleteIfExists(mangaRepository, tokyoGhoul.getMalid());

                mangaRepository.insert(dorohedoro);
                mangaRepository.insert(tokyoGhoul);

                System.out.println("Manga with READING status:");
                var readingManga = mangaRepository.findByStatus(Status.READING);

                for (Manga manga : readingManga) {
                    printManga(manga);
                }

                System.out.println();
                System.out.println("Updating Dorohedoro chapters read to 45...");

                mangaRepository.updateChaptersRead(dorohedoro.getMalid(), 45);

                Optional<Manga> updatedManga = mangaRepository.findByMalId(dorohedoro.getMalid());

                if (updatedManga.isPresent()) {
                    System.out.println("Updated manga found:");
                    printManga(updatedManga.get());
                } else {
                    System.out.println("Dorohedoro was not found.");
                }
            }
        } catch (DatabaseException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Could not close the database connection.");
            e.printStackTrace();
        }
    }

    private static void deleteIfExists(MangaRepository mangaRepository, int malId) {
        if (mangaRepository.findByMalId(malId).isPresent()) {
            mangaRepository.delete(malId);
        }
    }

    private static void printManga(Manga manga) {
        System.out.println("ID: " + manga.getMalid());
        System.out.println("Title: " + manga.getTitle());
        System.out.println("Status: " + manga.getStatus());
        System.out.println("Chapters read: " + manga.getChaptersRead());
        System.out.println("Total chapters: " + manga.getTotalChapters());
        System.out.println();
    }
}