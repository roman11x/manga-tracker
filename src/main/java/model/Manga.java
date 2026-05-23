package model;
/**
 * Represents a manga tracked by the application.
 *
 * A Manga object stores the data needed by the tracker, including the
 * MyAnimeList ID, title, reading progress, total chapter count, reading
 * status, cover path, and the date it was added.
 *
 * Some fields, such as the title and MyAnimeList ID, are fixed after the
 * object is created. Other fields, such as chaptersRead and status, can
 * change as the user updates their progress.
 */
public class Manga {
    private final int malid;
    private final String title;
    private int chaptersRead;
    private  int totalChapters;
    private Status status;
    private String coverPath;
    private String addedAt;


    /**
     * Creates a new manga with zero chapters read by default.
     *
     * The manga's progress can be updated later using setChaptersRead().
     */
    public Manga(int malid, String title, int totalChapters, Status status) {
        this.malid = malid;
        this.totalChapters = totalChapters;
        this.status = status;
        this.title = title;
        this.chaptersRead = 0;
    }

    // getters
    public int getMalid(){
        return this.malid;
    }
    public String getTitle() {
        return this.title;
    }
    public int getChaptersRead() {
        return this.chaptersRead;
    }
    public int getTotalChapters() {
        return this.totalChapters;
    }
    public Status getStatus() {
        return this.status;
    }
    public String getCoverPath() {
        return  this.coverPath;
    }
    public String getAddedAt() {
        return this.addedAt;
    }

    //setters
    public void setChaptersRead(int chaptersRead) {
         this.chaptersRead = chaptersRead;
    }
    public void setTotalChapters(int totalChapters) {this.totalChapters = totalChapters;}
    public void setStatus(Status status) {
        this.status = status;
    }
    public void setCoverPath(String path) {
        this.coverPath = path;
    }
    public void setAddedAt(String date) {
        this.addedAt = date;
    }

    @Override
    public String toString() {
        return "Manga{title='" + title + "', status=" + status + ", chapters read=" + chaptersRead + ", total chapters=" + totalChapters + "}";
    }
}
