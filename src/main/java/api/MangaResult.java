package api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

//  Ignore any extra fields in the JSON we didn't explicitly map here
@JsonIgnoreProperties(ignoreUnknown = true)
public class MangaResult {

    @JsonProperty("mal_id")
    private int malId;

    // no annotation for coverPath as it is nested
    private String coverPath;

    @JsonProperty("title")
    private String title;

    @JsonProperty("chapters")
    private int totalChapters;

    public MangaResult() {}

    public int getMalId() {
        return malId;
    }
    public String getCoverPath() {
        return coverPath;
    }
    public String getTitle() {
        return title;
    }
    public int getTotalChapters() {
        return totalChapters;
    }
    //setter for the manual field as it is nested
    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }


}
