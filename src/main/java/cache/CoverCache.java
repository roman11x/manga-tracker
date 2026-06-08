package cache;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// This class is responsible for downloading and caching covers for manga.

public class CoverCache {

    private final Path cacheDir; // This will hold the path to the cache directory
    private final HttpClient client = HttpClient.newHttpClient();

    public CoverCache() throws IOException {
        this.cacheDir = Paths.get(
                System.getProperty("user.home"), ".local", "share", "manga-tracker", "covers"
        );
        Files.createDirectories(cacheDir); // Create the cache directory if it doesn't exist'
    }

    // This method downloads a cover image from the given URL and saves it to the cache directory.
    public String download(int malId, String coverUrl) throws IOException, InterruptedException {
        Path dest = cacheDir.resolve(malId + ".jpg"); // Construct the destination path for the image
        if (Files.exists(dest)) { // If the image already exists, return its path
            return dest.toString();
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(coverUrl))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        Files.copy(response.body(), dest); // Save the image to the cache directory

        return dest.toString();

    }

    public void clear() throws IOException {
        Files.walk(cacheDir).forEach(path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


}
