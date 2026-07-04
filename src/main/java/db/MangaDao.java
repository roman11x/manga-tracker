package db;

// This interface includes all the different methods for interacting with the manga table

import model.Manga;
import model.Status;

import java.util.List;
import java.util.Optional;

/**
 * Defines the database operations available for manga records.
 *
 * This interface separates the rest of the application from the actual
 * database implementation. Code that uses MangaDao does not need to know
 * whether the data is stored in SQLite, another database, or somewhere else.
 *
 * MangaRepository is the current SQLite implementation of this interface.
 */

public interface MangaDao {
    /**
     * Saves a new manga record.
     */
    void insert(Manga manga);
    /**
     * Updates the main editable fields of an existing manga.
     */
    void update(Manga manga);
    /**
     * Updates only the number of chapters the user has read.
     */
    void updateChaptersRead(int malId, int chaptersRead);
    /**
     * Updates only the reading status of a manga.
     */
    void updateStatus(int malId, Status status);
    /**
     * Updates only the total chapter count.
     *
     * This is useful for ongoing manga where the final chapter count
     * may change later.
     */
    void updateTotalChapters(int malId, int totalChapters);
    /**
     * Deletes a manga record by its MyAnimeList ID.
     */
    void delete(int malId);
    /**
     * Returns manga whose Jikan metadata (volumes, demographic, genres)
     * has not been fetched yet.
     */
    List<Manga> findUnsynced();
    /**
     * Stores the Jikan metadata for a manga and marks it as synced.
     *
     * Only touches the metadata columns so it cannot clobber reading
     * progress written concurrently from the UI.
     */
    void updateMetadata(int malId, int totalVolumes, String demographic, String genres);
    /**
     * Returns all manga stored in the database.
     */
    List<Manga> findAll();
    /**
     * Returns all manga with the given reading status.
     */
    List<Manga> findByStatus(Status status);
    /**
     * Finds a manga by its MyAnimeList ID.
     *
     * Returns Optional.empty() if no matching manga exists.
     */
    Optional<Manga> findByMalId(int malId);
}
