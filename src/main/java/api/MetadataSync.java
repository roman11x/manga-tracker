package api;

import db.Database;
import db.MangaRepository;
import model.Manga;

import java.util.List;

/**
 * Background backfill of Jikan metadata (volumes, demographic, genres) for
 * manga that were added before the tracker stored those fields.
 *
 * Runs once per launch on a daemon thread with its own database connection
 * (a sqlite-jdbc connection must not be shared across threads). Rows that
 * fail (network hiccup, rate limit) simply stay unsynced and are retried on
 * the next launch. The UI polls unsyncedCount() to show sync progress.
 */
public class MetadataSync {

    // Jikan allows ~3 requests/second; stay well under it
    private static final long REQUEST_INTERVAL_MS = 600;

    private final JikanClient client;
    private volatile int remaining = 0;

    public MetadataSync(JikanClient client) {
        this.client = client;
    }

    public void start() {
        Thread worker = new Thread(this::run, "metadata-sync");
        worker.setDaemon(true); // never keep the app alive on quit
        worker.start();
    }

    public int unsyncedCount() {
        return remaining;
    }

    private void run() {
        try (var connection = Database.getConnection()) {
            var repo = new MangaRepository(connection);
            List<Manga> unsynced = repo.findUnsynced();
            remaining = unsynced.size();

            for (Manga manga : unsynced) {
                try {
                    MangaResult result = client.fetchManga(manga.getMalid()).get();
                    repo.updateMetadata(manga.getMalid(),
                            result.getTotalVolumes(),
                            result.getDemographic(),
                            result.getGenres());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    // leave the row unsynced; it will be retried next launch
                }

                remaining--;
                Thread.sleep(REQUEST_INTERVAL_MS);
            }
        } catch (Exception e) {
            // background sync must never take the app down
        } finally {
            remaining = 0;
        }
    }
}
