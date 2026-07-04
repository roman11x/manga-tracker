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
                                // map the easy, top-level nodes to a MangaResult object
                                MangaResult manga = mapper.treeToValue(itemNode, MangaResult.class);

                                //manually dig down for the nested fields
                                JsonNode imagesNode = itemNode.get("images");
                                if(imagesNode != null) {
                                    JsonNode jpgNode = imagesNode.get("jpg");
                                    if (jpgNode != null && jpgNode.hasNonNull("image_url")) {
                                        String imageUrl = jpgNode.get("image_url").asText();
                                        manga.setCoverPath(imageUrl);
                                    }
                                }
                                results.add(manga);
                            }
                        }


                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    return results;
                });

    }


}
