package db;

import model.Manga;
import model.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * SQLite implementation of the MangaDao interface.
 *
 * This class contains the SQL needed to insert, update, delete, and fetch
 * manga records from the database. It converts database rows into Manga
 * objects and wraps SQLExceptions in DatabaseException so higher layers of
 * the application do not need to deal directly with JDBC errors.
 */

public class MangaRepository implements MangaDao{


    private final Connection connection;

    /**
     * Creates a repository that uses the provided database connection.
     *
     * The connection is provided from outside so the application can decide
     * when to open and close it.
     */

    public MangaRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Converts the current row of a ResultSet into a Manga object.
     *
     * The ResultSet must already be positioned on a valid row before this
     * method is called.
     */
    private Manga createMangaFromResultSet(ResultSet resultSet) throws SQLException {
         var manga = new Manga(resultSet.getInt("mal_id"),
                resultSet.getString("title"),
                resultSet.getInt("total_chapters"),
                Status.valueOf(resultSet.getString("status")));

         manga.setChaptersRead(resultSet.getInt("chapters_read"));
         manga.setCoverPath(resultSet.getString("cover_path"));
         manga.setAddedAt(resultSet.getString("added_at"));
         manga.setTotalVolumes(resultSet.getInt("total_volumes"));
         manga.setDemographic(resultSet.getString("demographic"));
         manga.setGenres(resultSet.getString("genres"));
         return manga;
    }
     /**
     * Creates an exception for operations that target a manga ID
     * that does not exist in the database.
     */
    private DatabaseException createMangaNotFoundException(int malId) {
        return new DatabaseException("No manga found with id " + malId);
    }

    @Override
    public void insert(Manga manga) {
        String insertQuery = """
                INSERT INTO manga (
                    mal_id,
                    title,
                    chapters_read,
                    total_chapters,
                    status,
                    cover_path,
                    total_volumes,
                    demographic,
                    genres,
                    metadata_synced
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setInt(1, manga.getMalid());
            preparedStatement.setString(2, manga.getTitle());
            preparedStatement.setInt(3, manga.getChaptersRead());
            preparedStatement.setInt(4, manga.getTotalChapters());
            preparedStatement.setString(5, manga.getStatus().name());
            preparedStatement.setString(6, manga.getCoverPath());
            preparedStatement.setInt(7, manga.getTotalVolumes());
            preparedStatement.setString(8, manga.getDemographic());
            preparedStatement.setString(9, manga.getGenres());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert manga " + manga.getMalid() , e);
        }
    }

    @Override
    public void update(Manga manga) {

        String updateQuery = """
                UPDATE manga
                SET chapters_read = ?,
                total_chapters = ?,
                status = ?,
                cover_path = ?,
                total_volumes = ?,
                demographic = ?,
                genres = ?
                WHERE mal_id = ?
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setInt(1, manga.getChaptersRead());
            preparedStatement.setInt(2, manga.getTotalChapters());
            preparedStatement.setString(3, manga.getStatus().name());
            preparedStatement.setString(4, manga.getCoverPath());
            preparedStatement.setInt(5, manga.getTotalVolumes());
            preparedStatement.setString(6, manga.getDemographic());
            preparedStatement.setString(7, manga.getGenres());
            preparedStatement.setInt(8, manga.getMalid());

           int rowsUpdated = preparedStatement.executeUpdate();
           if (rowsUpdated == 0) {
               throw createMangaNotFoundException(manga.getMalid());
           }
        }
        catch (SQLException e) {
            throw new DatabaseException("Failed to update manga with id " + manga.getMalid(), e);
        }


    }

    @Override
    public void updateChaptersRead(int malId, int chaptersRead) {
        String updateQuery = """
                UPDATE manga 
                SET chapters_read = ? 
                WHERE mal_id = ?
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setInt(1, chaptersRead);
            preparedStatement.setInt(2, malId);

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated == 0) {
              throw  createMangaNotFoundException(malId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update chapters read for manga with id " + malId , e);
        }
        
    }

    @Override
    public void updateStatus(int malId, Status status) {
        String updateQuery = """
                UPDATE manga 
                SET status = ? 
                WHERE mal_id = ?
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setString(1, status.name());
            preparedStatement.setInt(2, malId);

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated == 0) {
                throw createMangaNotFoundException(malId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update status for manga with id " + malId , e);
        }

    }

    @Override
    public void updateTotalChapters(int malId, int totalChapters) {
        String updateQuery = """
                UPDATE manga 
                SET total_chapters = ? 
                WHERE mal_id = ?
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setInt(1, totalChapters);
            preparedStatement.setInt(2, malId);

           int rowsUpdated = preparedStatement.executeUpdate();
           if (rowsUpdated == 0) {
               throw createMangaNotFoundException(malId);
           }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update total chapters for manga with id " + malId , e);
        }

    }

    @Override
    public void delete(int malId) {
        String deleteQuery = """
                DELETE FROM manga 
                WHERE mal_id = ?
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
            preparedStatement.setInt(1, malId);

            int rowsDeleted = preparedStatement.executeUpdate();
            if (rowsDeleted == 0) {
                throw createMangaNotFoundException(malId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete manga with id " + malId , e);
        }

    }

    @Override
    public List<Manga> findUnsynced() {
        String selectQuery = "SELECT * FROM manga WHERE metadata_synced = 0";
        var mangaList = new ArrayList<Manga>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                mangaList.add(createMangaFromResultSet(resultSet));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch unsynced manga", e);
        }
        return mangaList;
    }

    @Override
    public void updateMetadata(int malId, int totalVolumes, String demographic, String genres) {
        String updateQuery = """
                UPDATE manga
                SET total_volumes = ?,
                demographic = ?,
                genres = ?,
                metadata_synced = 1
                WHERE mal_id = ?
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setInt(1, totalVolumes);
            preparedStatement.setString(2, demographic);
            preparedStatement.setString(3, genres);
            preparedStatement.setInt(4, malId);

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated == 0) {
                throw createMangaNotFoundException(malId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update metadata for manga with id " + malId, e);
        }
    }

    @Override
    public List<Manga> findAll() {
        String selectQuery = "SELECT * FROM manga";
        var mangaList = new ArrayList<Manga>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
        ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()) {

                mangaList.add(createMangaFromResultSet(resultSet));

            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all manga", e);
        }
        return mangaList;

    }

    @Override
    public List<Manga> findByStatus(Status status) {
        String selectQuery = "SELECT * FROM manga WHERE status = ?";
        var mangaList = new ArrayList<Manga>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
            preparedStatement.setString(1, status.name());

            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()) {

                mangaList.add(createMangaFromResultSet(resultSet));

            }


        }
        catch (SQLException e) {
            throw new DatabaseException("Failed to fetch manga with status " + status, e);
        }
        return mangaList;
    }

    @Override
    public Optional<Manga> findByMalId(int malId) {
        String selectQuery = "SELECT * FROM manga WHERE mal_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
            preparedStatement.setInt(1, malId);

            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.next()) {
                var manga = createMangaFromResultSet(resultSet);

                return Optional.of(manga);
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("Failed to fetch manga with id " + malId, e);
        }
        return Optional.empty();
    }
}
