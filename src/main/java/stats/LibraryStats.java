package stats;

import model.Manga;
import model.Status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated statistics over the whole library, computed from one pass over
 * the manga list. Pure data — no UI or database code — so the Stats tab just
 * renders whatever this object reports.
 *
 * "Volumes read" counts completed manga only: finishing a manga is the only
 * point where we know exactly how many volumes were read.
 */
public class LibraryStats {

    public record TagCount(String name, int count) {
    }

    public record ReadingHighlight(String title, int percent) {
    }

    private final Map<Status, Integer> statusCounts = new EnumMap<>(Status.class);
    private final Map<String, Integer> demographicCounts = new LinkedHashMap<>();
    private final Map<String, Integer> tagCounts = new LinkedHashMap<>();

    private int totalTitles;
    private int chaptersRead;
    private int volumesRead;
    private int chaptersRemaining;
    private int averageReadingPercent;
    private ReadingHighlight closestToDone;

    public LibraryStats(List<Manga> library) {
        for (Status status : Status.values()) {
            statusCounts.put(status, 0);
        }

        int readingPercentSum = 0;
        int readingWithTotal = 0;

        for (Manga manga : library) {
            totalTitles++;
            statusCounts.merge(manga.getStatus(), 1, Integer::sum);
            chaptersRead += manga.getChaptersRead();

            if (manga.getStatus() == Status.COMPLETED) {
                volumesRead += manga.getTotalVolumes();
            }

            if (manga.getDemographic() != null) {
                demographicCounts.merge(manga.getDemographic(), 1, Integer::sum);
            }

            if (manga.getGenres() != null) {
                for (String tag : manga.getGenres().split(",")) {
                    String name = tag.strip();
                    if (!name.isEmpty()) {
                        tagCounts.merge(name, 1, Integer::sum);
                    }
                }
            }

            if (manga.getStatus() == Status.READING && manga.getTotalChapters() > 0) {
                int percent = 100 * manga.getChaptersRead() / manga.getTotalChapters();
                readingPercentSum += percent;
                readingWithTotal++;
                chaptersRemaining += Math.max(0, manga.getTotalChapters() - manga.getChaptersRead());

                if (percent < 100 && (closestToDone == null || percent > closestToDone.percent())) {
                    closestToDone = new ReadingHighlight(manga.getTitle(), percent);
                }
            }
        }

        averageReadingPercent = readingWithTotal > 0 ? readingPercentSum / readingWithTotal : 0;
    }

    public int getTotalTitles() {
        return totalTitles;
    }

    public int getChaptersRead() {
        return chaptersRead;
    }

    public int getVolumesRead() {
        return volumesRead;
    }

    public int getCompletedCount() {
        return statusCounts.get(Status.COMPLETED);
    }

    public int countFor(Status status) {
        return statusCounts.get(status);
    }

    public int getChaptersRemaining() {
        return chaptersRemaining;
    }

    public int getAverageReadingPercent() {
        return averageReadingPercent;
    }

    // null when nothing on the Reading list has a known total
    public ReadingHighlight getClosestToDone() {
        return closestToDone;
    }

    public List<TagCount> getDemographics() {
        return sortedByCount(demographicCounts, Integer.MAX_VALUE);
    }

    public List<TagCount> getTopTags(int limit) {
        return sortedByCount(tagCounts, limit);
    }

    private List<TagCount> sortedByCount(Map<String, Integer> counts, int limit) {
        var result = new ArrayList<TagCount>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .forEach(entry -> result.add(new TagCount(entry.getKey(), entry.getValue())));
        return result;
    }
}
