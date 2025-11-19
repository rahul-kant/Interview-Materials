import java.time.Duration;
import java.util.*;

/**
 * Movie Class - Represents a movie
 * 
 * Interview Key Points:
 * - Immutable movie information
 * - Enum for ratings and genres
 * - Duration handling
 */
public class Movie {
    private final String id;
    private final String title;
    private final String description;
    private final Duration duration;
    private final Genre genre;
    private final Rating rating;
    private final List<String> cast;
    private final String director;
    
    public enum Genre {
        ACTION, COMEDY, DRAMA, HORROR, THRILLER, ROMANCE, SCI_FI, ANIMATED
    }
    
    public enum Rating {
        G, PG, PG_13, R, NC_17
    }
    
    public Movie(String id, String title, String description, int durationMinutes,
                 Genre genre, Rating rating, List<String> cast, String director) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.duration = Duration.ofMinutes(durationMinutes);
        this.genre = genre;
        this.rating = rating;
        this.cast = new ArrayList<>(cast);
        this.director = director;
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Duration getDuration() {
        return duration;
    }
    
    public Genre getGenre() {
        return genre;
    }
    
    public Rating getRating() {
        return rating;
    }
    
    public List<String> getCast() {
        return new ArrayList<>(cast);
    }
    
    public String getDirector() {
        return director;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - %d mins [%s]", 
            title, genre, duration.toMinutes(), rating);
    }
}
