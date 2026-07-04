package api;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonProperty("volumes")
    private int totalVolumes;

    // set manually from the nested arrays; @JsonIgnore stops Jackson from
    // binding the JSON's "genres" array-of-objects onto this String field
    @JsonIgnore
    private String demographic;
    @JsonIgnore
    private String genres;

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
    public int getTotalVolumes() {
        return totalVolumes;
    }
    public String getDemographic() {
        return demographic;
    }
    public String getGenres() {
        return genres;
    }
    //setters for the manual fields as they are nested
    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }
    public void setDemographic(String demographic) {
        this.demographic = demographic;
    }
    public void setGenres(String genres) {
        this.genres = genres;
    }


}
