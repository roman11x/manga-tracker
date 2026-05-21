import model.*;

import java.util.ArrayList;

public class Main {

    public void main (String[] args) {

        var dorohedoro = new Manga(1, 165, Status.COMPLETED, "Dorohedoro");
        var ghoul = new Manga(2, 110, Status.PLAN_TO_READ, "Tokyo Ghoul");
        var mangaList = new ArrayList<Manga>();
        mangaList.add(dorohedoro);
        mangaList.add(ghoul);
        for ( Manga manga : mangaList) {
            System.out.println("Title " + manga.getTitle());
            System.out.println("Status " + manga.getStatus());
            System.out.println("Chapter count " + manga.getTotalChapters());
        }
        System.out.println("Before changing Dorohedoro chapters read");
        System.out.println(dorohedoro.getChaptersRead());
        System.out.println("After changing Dorohedoro chapters read");
        dorohedoro.setChaptersRead(165);
        System.out.println(dorohedoro.getChaptersRead());

    }
}
