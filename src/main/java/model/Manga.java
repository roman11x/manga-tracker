package model;
// This class represents how mangas are modeled in our data model
public class Manga {
    private final int malid;
    private final String title;
    private int chaptersRead;
    private final int totalChapters;
    private Status status;
    private String coverPath;
    private String addedAt;



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
    public void setStatus(Status status) {
        this.status = status;
    }
    public void setCoverPath(String path) {
        this.coverPath = path;
    }
    public void setAddedAt(String date) {
        this.addedAt = date;
    }


}
