package api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JikanClient {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public CompletableFuture <List<MangaResult>> searchManga(String query) {
        //build the http request
        // url encode the query
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        // limit=9 so every result fits on screen and stays selectable
        String url = "https://api.jikan.moe/v4/manga?q=" + encodedQuery + "&limit=9";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        //asynchronously send the request and return the response so that the ui won't freeze'
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(stringHttpResponse -> {
                    List<MangaResult> results = new ArrayList<>();

                    try  {
                        String rawJsonText = stringHttpResponse.body();

                        // parse the text into a navigable tree
                        JsonNode rootNode = mapper.readTree(rawJsonText);

                        //dig into the tree to get the data node
                        JsonNode dataNode = rootNode.get("data");

                        if (dataNode != null && dataNode.isArray()) {
                            for (JsonNode itemNode : dataNode) {
                                results.add(parseMangaNode(itemNode));
                            }
                        }


                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    return results;
                });

    }

    /**
     * Fetches a single manga by its MyAnimeList ID.
     *
     * Used by the metadata backfill for manga that were added before the
     * tracker stored volumes, demographic, and genres.
     */
    public CompletableFuture<MangaResult> fetchManga(int malId) {
        String url = "https://api.jikan.moe/v4/manga/" + malId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Jikan returned HTTP " + response.statusCode() + " for manga " + malId);
                    }

                    try {
                        // the single-manga endpoint wraps the same object shape
                        // in {"data": {...}} that search wraps in an array
                        JsonNode dataNode = mapper.readTree(response.body()).get("data");
                        if (dataNode == null || dataNode.isNull()) {
                            throw new RuntimeException("Jikan returned no data for manga " + malId);
                        }
                        return parseMangaNode(dataNode);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    // maps one manga JSON object (from search or single fetch) to a MangaResult
    private MangaResult parseMangaNode(JsonNode itemNode) throws JsonProcessingException {
        // map the easy, top-level nodes to a MangaResult object
        MangaResult manga = mapper.treeToValue(itemNode, MangaResult.class);

        //manually dig down for the nested fields
        JsonNode imagesNode = itemNode.get("images");
        if (imagesNode != null) {
            JsonNode jpgNode = imagesNode.get("jpg");
            if (jpgNode != null && jpgNode.hasNonNull("image_url")) {
                String imageUrl = jpgNode.get("image_url").asText();
                manga.setCoverPath(imageUrl);
            }
        }

        // demographics is an array; MAL manga effectively have at most one
        JsonNode demographicsNode = itemNode.get("demographics");
        if (demographicsNode != null && demographicsNode.isArray() && !demographicsNode.isEmpty()) {
            JsonNode nameNode = demographicsNode.get(0).get("name");
            if (nameNode != null && !nameNode.isNull()) {
                manga.setDemographic(nameNode.asText());
            }
        }

        // tags = genres + themes, stored as one comma-separated string
        List<String> tags = new ArrayList<>();
        collectNames(itemNode.get("genres"), tags);
        collectNames(itemNode.get("themes"), tags);
        if (!tags.isEmpty()) {
            manga.setGenres(String.join(", ", tags));
        }

        return manga;
    }

    private void collectNames(JsonNode arrayNode, List<String> into) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return;
        }
        for (JsonNode node : arrayNode) {
            JsonNode nameNode = node.get("name");
            if (nameNode != null && !nameNode.isNull()) {
                into.add(nameNode.asText());
            }
        }
    }


}
